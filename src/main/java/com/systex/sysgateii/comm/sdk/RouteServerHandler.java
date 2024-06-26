package com.systex.sysgateii.comm.sdk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.systex.sysgateii.autosvr.autoPrtSvr.Server.FASSvr;
import com.systex.sysgateii.autosvr.comm.Constants;
import com.systex.sysgateii.autosvr.comm.TXP;
import com.systex.sysgateii.autosvr.util.COMM_STATE;
import com.systex.sysgateii.autosvr.util.dataUtil;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
//20230703 MatsudairaSyuMe test for Direct Memory Leak
import io.netty.util.IllegalReferenceCountException;
//----

public class RouteServerHandler extends ChannelDuplexHandler {
	private static Logger log = LoggerFactory.getLogger(RouteServerHandler.class);
	private Logger trace = LoggerFactory.getLogger("trace");
	private FASSvr dispatcher;
	private int retryInterval = 2500;
	private int totalReTryTime;
	private int timeout = 60000;
	//20220818 MatsudairasyuMe add for receive buffer for 2 byte length processing
	private int bufferSize = Integer.parseInt(System.getProperty("bufferSize", Constants.DEF_CHANNEL_BUFFER_SIZE + ""));
	private ByteBuf serverSessionRecvMessageBuf = null;
	//----

	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		log.info("channelActived");
		super.channelActive(ctx);
		dispatcher = FASSvr.getFASSvr();
		this.timeout = RouteServer.getTimeout();
		this.totalReTryTime = (int) (this.timeout / this.retryInterval);
		//20220818 MatsudairasyuMe add for receive buffer for 2 byte length processing
		this.serverSessionRecvMessageBuf = Unpooled.buffer(bufferSize);
		this.serverSessionRecvMessageBuf.clear(); 
		//----
		log.debug("{} timeout [{}] retyrinterval=[{}] totalReTryTime=[{}]", ctx.channel().id(), this.timeout, this.retryInterval, this.totalReTryTime);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		//20230703 MatsudairaSyuMe check Direct Memory LEAK
		try {
		//20230703----
			if (msg instanceof ByteBuf) {
				log.info("get message from RouterConnection");
				//----
				ByteBuf buf = (ByteBuf) msg;
				//20220818 MatsudairasyuMe add for receive buffer for 2 byte length processing
				log.debug("capacity=" + buf.capacity() + " readableBytes=" + buf.readableBytes() + " barray="
						+ buf.hasArray() + " nio=  " + buf.nioBufferCount());
				if (this.serverSessionRecvMessageBuf.readerIndex() > (this.serverSessionRecvMessageBuf.capacity() / 2)) {
					this.serverSessionRecvMessageBuf.discardReadBytes();
					log.debug("adjustment clientSessionRecvMessageBuf readerindex ={}" + this.serverSessionRecvMessageBuf.readableBytes());
				}
				log.debug("readableBytes={} barray={}", buf.readableBytes(), buf.hasArray());
				//----
				if (buf.isReadable() && !buf.hasArray()) { //20220818 add check for !buf.hasArray()
					//20220818 MatsudairasyuMe add for receive buffer for 2 byte length processing
					//byte[] req = new byte[buf.readableBytes()];
					//buf.readBytes(req);
					//byte[] req = null;
					log.debug("readableBytes={} barray={}", buf.readableBytes(), buf.hasArray());
					//				if (this.serverSessionRecvMessageBuf.readerIndex() > (this.serverSessionRecvMessageBuf.capacity() / 2)) {
					//					this.serverSessionRecvMessageBuf.discardReadBytes();
					//					log.debug("adjustment serverSessionRecvMessageBuf readerindex ={}" + this.serverSessionRecvMessageBuf.readableBytes());
					//				}
					this.serverSessionRecvMessageBuf.writeBytes(buf);
					log.debug("serverSessionRecvMessageBuf.readableBytes={}",this.serverSessionRecvMessageBuf.readableBytes());
					if (this.serverSessionRecvMessageBuf.readableBytes() >= 2) {
						byte[] lenbary = new byte[2];
						log.debug("this.serverSessionRecvMessageBuf.readerIndex()={}", this.serverSessionRecvMessageBuf.readerIndex());
						this.serverSessionRecvMessageBuf.getBytes(this.serverSessionRecvMessageBuf.readerIndex(), lenbary);
						//log.debug("this.serverSessionRecvMessageBuf.readerIndex()={} [{}] [{}]", this.serverSessionRecvMessageBuf.readerIndex(), lenbary[0], lenbary[1]);
						int rcvmsglen = ((int)(lenbary[0] & 0xff)  * 256 + (int)(lenbary[1] & 0xff));
						if (rcvmsglen >= 2 && rcvmsglen <= this.serverSessionRecvMessageBuf.readableBytes()) {
							//log.debug("serverSessionRecvMessageBuf.readableBytes={} rcvmsglen={}",this.serverSessionRecvMessageBuf.readableBytes(), rcvmsglen);
							byte[] reqTmpary = cnvResultTelegram();
							byte[] req = null;
							switch (COMM_STATE.ById(reqTmpary[0])) {
							case TRANSF: // <==傳送電文
								req = new byte[reqTmpary.length - 1];
								System.arraycopy(reqTmpary, 1, req, 0, (reqTmpary.length - 1));
								log.info("RouteConnection--> RouteServerHandler 傳送電文  Constants.outgoingTelegramKeyMap.size()=[{}]", Constants.outgoingTelegramKeyMap.size());
								break;
							case CANCEL: // <==刪除記錄
								byte[] reqDataary = new byte[reqTmpary.length - 1];
								System.arraycopy(reqTmpary, 1, reqDataary, 0, (reqTmpary.length - 1));
								//							log.info("RouteConnection--> RouteServerHandler 刪除記錄  Constants.outgoingTelegramKeyMap.size()=[{}]", Constants.outgoingTelegramKeyMap.size());
								String canceltelegramKey = dataUtil.getTelegramKey(reqDataary);
								if (canceltelegramKey.trim().length() > 0 && Constants.outgoingTelegramKeyMap.containsKey(canceltelegramKey))
									Constants.outgoingTelegramKeyMap.remove(canceltelegramKey);
								log.info("RouteConnection--> RouteServerHandler 刪除記錄 [{}] result Constants.outgoingTelegramKeyMap.size()=[{}]", canceltelegramKey, Constants.outgoingTelegramKeyMap.size());
								trace.error("RouteConnection--> RouteServerHandler 刪除記錄 [{}] result Constants.outgoingTelegramKeyMap.size()=[{}]", canceltelegramKey, Constants.outgoingTelegramKeyMap.size());
								canceltelegramKey = "";
								reqDataary = null;
								break;
							case CHECK: // <==詢問
								log.info("RouteConnection--> RouteServerHandler 詢問 ignore");
								break;
							case ACK: // <==回覆成功
								log.info("RouteConnection--> RouteServerHandler 回覆成功 ignore");
								break;
							case NAK: // <==回覆失敗
								log.info("RouteConnection--> RouteServerHandler 回覆失敗 ignore");
								break;
							}
							reqTmpary = null;
							if (req != null && req.length > 0) {
								//log.info("receive from RouteClient [{}]", new String(req));
								String telegramKey = "";
								boolean alreadySendTelegram = false;
								telegramKey = dataUtil.getTelegramKey(req);
								//20230217 mark for not use UTF8
								//String body = new String(req, CharsetUtil.UTF_8).substring(0, req.length);
								//20220819 MatsudairaSyuMe change to use new outgoingTelegramKeyMap
								//							alreadySendTelegram = dispatcher.sendTelegram(req);
								alreadySendTelegram = dispatcher.sendTelegram(req, ctx);
								//							log.info("get request from RouteClient [{}]: telegramKey=[{}] [{}] start send to FAS [{}]", ctx.channel().id(), telegramKey, body, alreadySendTelegram);
								log.info("get request from RouteClient [{}]: telegramKey=[{}] len={{}] start send to FAS [{}]", ctx.channel().id(), telegramKey, req.length, alreadySendTelegram);
								// 20220819 int reTry = 0;
								byte[] resultmsg = null;
								/* 20220810 MatsudairaSyuMe change to send telegram back by FASClientChannelHandler
							if (alreadySendTelegram)
								do {
									resultmsg = dispatcher.getResultTelegram(telegramKey);
									if (resultmsg != null) {
										log.debug("{} getResultTelegram telegramKey=[{}] resultmsg=[{}]", ctx.channel().id(), telegramKey, new String(resultmsg));
										//20220818 MatsudairasyuMe add for receive buffer for 2 byte length processing, ctx.writeAndFlush(Unpooled.wrappedBuffer(resultmsg));
										break;
									} else {
										try {
											Thread.sleep(this.retryInterval);
										} catch (InterruptedException e) {
										}
									}
								} while (++reTry < this.totalReTryTime);
							else {
								resultmsg = dispatcher.mkE002(telegramKey);
								log.error("{} FAS connection break!!! send E002 telegramKey=[{}] resultmsg=[{}]", ctx.channel().id(), telegramKey, new String(resultmsg));
								//20220818 MatsudairasyuMe add for receive buffer for 2 byte length processing ctx.writeAndFlush(Unpooled.wrappedBuffer(resultmsg));
							}*/
								if (!alreadySendTelegram) {
									resultmsg = dispatcher.mkE002(telegramKey);
									//20220819 abolish		if (resultmsg == null || reTry >= this.totalReTryTime) {
									//20220819 abolish			log.error("{} getResultTelegram FAS timeout !!!! [{}]", ctx.channel().id(), (resultmsg == null) ? "": new String(resultmsg));
									//20220819 abolish		} else {
									//20220819 MatsudairasyuMe add for receive buffer for 2 byte length processing + 1 byte COMM_STATE.TRANSFE
									if (resultmsg != null) {
										byte[] sndmsg = new byte [resultmsg.length + 3];  //total send msgary is 2 byte length + msg
										sndmsg[0] = (byte) (sndmsg.length / 256);
										sndmsg[1] = (byte) (sndmsg.length % 256);
										sndmsg[2] = (byte) COMM_STATE.TRANSF.Getid();
										System.arraycopy(resultmsg, 0, sndmsg, 3, resultmsg.length);
										log.debug("send to RouteConnection sndmsg:[{}]", new String(sndmsg));
										//20230703 MatsudairaSyume make sure for write and flush synchronize mode
										//ctx.writeAndFlush(Unpooled.wrappedBuffer(sndmsg));
										ByteBuf intbuf = Unpooled.wrappedBuffer(sndmsg);
										try {
											ctx.writeAndFlush(buf.retain()).sync();
										}  catch (Exception e) {
											e.printStackTrace();
											log.error("Can't send E002 message to RouteConnection");
										}
										//20230703 MatsudairaSyuMe make sure for no direct memory leak
										finally {
											intbuf.release();
											intbuf = null;
										}
										//----
										if (telegramKey.trim().length() > 0 && Constants.outgoingTelegramKeyMap.containsKey(telegramKey))
											Constants.outgoingTelegramKeyMap.remove(telegramKey);
										sndmsg = null;
										resultmsg = null;
									}
									//20230703 MatsudairaSyuMe mark for use direct memory leak check
									//buf = null;
									//----20230703	
								}
							}
						} else
							log.debug("serverSessionRecvMessageBuf.readableBytes={} rcvmsglen={}",this.serverSessionRecvMessageBuf.readableBytes(), rcvmsglen);
					} else
						log.debug("serverSessionRecvMessageBuf.readableBytes lower to 2 bytes wait next incomming");
					//----
					/*20220818 MatsudairasyuMe add for receive buffer for 2 byte length processing
				String telegramKey = "";
				boolean alreadySendTelegram = false;
				telegramKey = dataUtil.getTelegramKey(req);
				String body = new String(req, CharsetUtil.UTF_8).substring(0, req.length);
				alreadySendTelegram = dispatcher.sendTelegram(req);
				log.info("get request from [{}]: telegramKey=[{}] [{}] start send to FAS [{}]", ctx.channel().id(), telegramKey, body, alreadySendTelegram);
				int reTry = 0;
				byte[] resultmsg = null;
				if (alreadySendTelegram)
					do {
						resultmsg = dispatcher.getResultTelegram(telegramKey);
						if (resultmsg != null) {
							log.debug("{} getResultTelegram telegramKey=[{}] resultmsg=[{}]", ctx.channel().id(), telegramKey, new String(resultmsg));
							//20220818 MatsudairasyuMe add for receive buffer for 2 byte length processing, ctx.writeAndFlush(Unpooled.wrappedBuffer(resultmsg));
							break;
						} else {
							try {
								Thread.sleep(this.retryInterval);
							} catch (InterruptedException e) {
							}
						}
					} while (++reTry < this.totalReTryTime);
				else {
					resultmsg = dispatcher.mkE002(telegramKey);
					log.error("{} FAS connection break!!! send E002 telegramKey=[{}] resultmsg=[{}]", ctx.channel().id(), telegramKey, new String(resultmsg));
					//20220818 MatsudairasyuMe add for receive buffer for 2 byte length processing ctx.writeAndFlush(Unpooled.wrappedBuffer(resultmsg));
				}
				if (resultmsg == null || reTry >= this.totalReTryTime) {
					log.error("{} getResultTelegram FAS timeout !!!! [{}]", ctx.channel().id());
				} else {
					//20220818 MatsudairasyuMe add for receive buffer for 2 byte length processing
					if (resultmsg != null) {
						byte[] sndmsg = new byte [resultmsg.length + 2];  //total send msgary is 2 byte length + msg
						System.arraycopy(resultmsg, 0, sndmsg, 2, resultmsg.length);
						sndmsg[0] = (byte) (sndmsg.length / 256);
						sndmsg[1] = (byte) (sndmsg.length % 256);
						log.debug("send to RouteConnection sndmsg:[{}]", new String(sndmsg));
						ctx.writeAndFlush(Unpooled.wrappedBuffer(sndmsg));
						sndmsg = null;
						resultmsg = null;
					}
					buf = null;
					//----	
				}
					 */
				} else {
					log.error("unknow message type");
					trace.error("unknow message type");
				}
			}
			//ByteBuf req = Unpooled.wrappedBuffer("Welcome to Netty.$_".getBytes(CharsetUtil.UTF_8));
			//ctx.writeAndFlush(req);
			//20230703 MatsudairaSyuMe use direct memory Leak check
		} finally {
			io.netty.util.ReferenceCountUtil.release(msg);
		}
		//----20230703
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		//20220818 MatsudairasyuMe add for receive buffer for 2 byte length processing
		this.serverSessionRecvMessageBuf.clear(); 
		//----
		log.info("Channel disconnected!");
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		//20220818 MatsudairasyuMe add for receive buffer for 2 byte length processing
		this.serverSessionRecvMessageBuf.clear(); 
		//----
		log.info("Exception caught" + cause);
	}
	/*  20220818 MatsudairasyuMe add for receive buffer for 2 byte length processing
	 * cnvResultTelegram() no need to check if serverSessionRecvMessageBuf.hasArray() && serverSessionRecvMessageBuf.readableBytes() > 0
	 *                                      or if serverSessionRecvMessageBuf.readableBytes() >= 2
	 *                                      or if two bytes length array convert to size
	 *                                            size >= 2 && size <= serverSessionRecvMessageBuf.readableBytes()
	 */
	private byte[] cnvResultTelegram() {
		byte[] rtn = null;
		byte[] lenbary = new byte[2];
		int size = 0;
		this.serverSessionRecvMessageBuf.getBytes(this.serverSessionRecvMessageBuf.readerIndex(), lenbary);
		size = ((int)(lenbary[0] & 0xff)  * 256 + (int)(lenbary[1] & 0xff));
		log.debug("clientSessionRecvMessageBuf.readableBytes={} size={}", this.serverSessionRecvMessageBuf.readableBytes(), size);
		byte[] recvrtnmsg = new byte[size];
		this.serverSessionRecvMessageBuf.readBytes(recvrtnmsg);
		log.debug("read {} byte(s) from clientMessageBuf after {}", size,
				this.serverSessionRecvMessageBuf.readableBytes());
		if (recvrtnmsg.length - 2 > 0) {
			rtn = new byte[recvrtnmsg.length - 2];
			System.arraycopy(recvrtnmsg, 2, rtn, 0, (recvrtnmsg.length - 2));
			//20240510 Poor Style: Value Never Read recvrtnmsg = null;
		}
		log.debug("get rtn len= {}", (rtn == null) ? 0 : rtn.length);
		//202405610 Poor Style: Value Never Read lenbary = null;
		return rtn;
	}

}
