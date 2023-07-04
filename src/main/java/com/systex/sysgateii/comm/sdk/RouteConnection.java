package com.systex.sysgateii.comm.sdk;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Timer;
import java.util.TimerTask;
//20220905 MatsudairaSyuMe RouteConnection using as Dispatcher
import java.util.concurrent.ConcurrentHashMap;
//----

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.systex.sysgateii.autosvr.autoPrtSvr.Client.PrtCli;
import com.systex.sysgateii.autosvr.comm.Constants;
import com.systex.sysgateii.autosvr.comm.TXP;
import com.systex.sysgateii.autosvr.util.COMM_STATE;
import com.systex.sysgateii.autosvr.util.dataUtil;

public class RouteConnection {
	private static Logger log = LoggerFactory.getLogger(RouteConnection.class);
	private Bootstrap bootstrap = new Bootstrap();
	private EventLoopGroup group;
	private SocketAddress addr_;
	private Channel channel_;
	private Timer timer_;
	//20220923 private byte[] rtnmsg = null;
	private int bufferSize = Integer.parseInt(System.getProperty("bufferSize", Constants.DEF_CHANNEL_BUFFER_SIZE + ""));
	private ByteBuf clientSessionRecvMessageBuf = null;
	//20220905 MatsudairaSyuMe for RouteConnection as dispatcher
	private final ConcurrentHashMap<String, Object> incomingTelegramMap = new ConcurrentHashMap<String, Object>();
	//----20220905

	public RouteConnection(String host, int port, Timer timer) {
		this(new InetSocketAddress(host, port), timer);
		log.info("initial connect to [{}] port [{}]", host, port);
		//20220905 MatsudairaSyuMe
		this.incomingTelegramMap.clear();
		//----
	}

	public RouteConnection(SocketAddress addr, Timer timer) {
		this.addr_ = addr;
		this.timer_ = timer;
		//20220818 MatsudairasyuMe add for receive buffer for 2 byte length processing
		this.clientSessionRecvMessageBuf = Unpooled.buffer(bufferSize);
		this.clientSessionRecvMessageBuf.clear();
		//----
		group = new NioEventLoopGroup();
		bootstrap.group(group);
		bootstrap.channel(NioSocketChannel.class);
		bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
		bootstrap.option(ChannelOption.SO_RCVBUF, bufferSize);
		bootstrap.option(ChannelOption.SO_SNDBUF, bufferSize);
		//20220905 MatsudairaSyuMe
		this.incomingTelegramMap.clear();
		//----
		bootstrap.handler(new ChannelInitializer<SocketChannel>() {
			@Override
			public void initChannel(SocketChannel ch) throws Exception {
				ch.pipeline().addLast("log", new LoggingHandler(RouteConnection.class, LogLevel.INFO)).
				addLast(createNMMessageHandler());
			}
		});

		scheduleConnect(1000);
	}

	public boolean send(String msg) {
		log.debug("send string msg:[{}]", msg);
		return send(msg.getBytes());
	}

	public boolean send(byte[] msg) {  //20220819 MatsudairaSyuMe send TRANSFER data add COMM_STATE.TRANSF
		//20220923 this.rtnmsg = null;
		if (channel_ != null && channel_.isActive() && (msg != null || msg.length == 0)) {
			//20220818 MatsudairaSyuMe add 2 bytes length before msg as real send message
			byte[] sndmsg = new byte [msg.length + 3];  //total send msgary is 2 byte length + 1 byte COMM_STATE.TRANSF + msg
			sndmsg[0] = (byte) (sndmsg.length / 256);
			sndmsg[1] = (byte) (sndmsg.length  % 256);
			sndmsg[2] = (byte) COMM_STATE.TRANSF.Getid();
			System.arraycopy(msg, 0, sndmsg, 3, msg.length);
			ByteBuf buf = channel_.alloc().buffer().writeBytes(sndmsg);
			log.debug("send to RouteServer len=[{}] sndmsg:[{}]", sndmsg.length, new String(sndmsg));
			//20230217 MatsudairaSyume make sure for write and flush synchronize mode
			try {
				channel_.writeAndFlush(buf.retain()).sync();
			} catch (Exception e) {
				e.printStackTrace();
				log.error("Can't send message to RouteSvrHandler");
			}
			//20230703 MatsudairaSyuMe make sure for no direct memory leak
			finally {
				buf.release();
				buf = null;
			}
			//----
			sndmsg = null;
			//20230704 MatsudairaSyuMebuf = null;
			//******
			return true;
		} else {
			if (msg == null || msg.length == 0)
				log.error("send message null or length == 0");
			else
				log.error("Can't send message to inactive connection");
			return false;
		}
	}
	public boolean sendCANCEL(byte[] msg) {  //20220819 MatsudairaSyuMe send COMM_STATE.CANCEL command and telegramKey
		//20220923 this.rtnmsg = null;
		if (channel_ != null && channel_.isActive() && (msg != null || msg.length == 0)) {
			//20220905 MatsudairaSyuMe
			String telegramKey = dataUtil.getTelegramKey(msg);
			if (this.incomingTelegramMap.containsKey(telegramKey)) {
				this.incomingTelegramMap.remove(telegramKey);
				log.debug("!!! cancel incomming telegram remove [{}] from incomingTelegramMap after remove size=[{}]", telegramKey, this.incomingTelegramMap.size());
			}
			//----
			//20220818 MatsudairaSyuMe add 2 bytes length before msg as real send message
			byte[] sndmsg = new byte [msg.length + 3];  //total send msgary is 2 byte length + 1 byte COMM_STATE.TRANSF + msg
			sndmsg[0] = (byte) (sndmsg.length / 256);
			sndmsg[1] = (byte) (sndmsg.length % 256);
			sndmsg[2] = (byte) COMM_STATE.CANCEL.Getid();
			System.arraycopy(msg, 0, sndmsg, 3, msg.length);
			ByteBuf buf = channel_.alloc().buffer().writeBytes(sndmsg);
			log.debug("send CANCEL command to RouteServer len=[{}] sndmsg:[{}]", sndmsg.length, new String(sndmsg));
			//20230217 MatsudairaSyume make sure for write and flush synchronize mode
			try {
				channel_.writeAndFlush(buf.retain()).sync();
			} catch (Exception e) {
				e.printStackTrace();
				log.error("Can't sendCANCEL message to RouteSvrHandler");
			}
			//20230703 MatsudairaSyuMe make sure for no direct memory leak
			finally {
				buf.release();
				buf = null;
			}
			//----
			sndmsg = null;
			//20230704 MatsudairaSyuMebuf = null;
			//******
			return true;
		} else {
			if (msg == null || msg.length == 0)
				log.error("send CANCEL command message null or length == 0");
			else
				log.error("Can't send CANCEL command message to inactive connection");
			return false;
		}
	}
	/*20220923
	public byte[] recv() {
		log.debug("try to get return TRANSFE message rtnmsg.length=[{}]", this.rtnmsg != null ? this.rtnmsg.length: 0);
		return this.rtnmsg;
	}
	*/

	//20220905 MatsudairaSyuMe
	public byte[] recv(String telegramKey) {
		synchronized (this.incomingTelegramMap) {
			//20220923 mark this.rtnmsg = null;
			if (this.incomingTelegramMap.containsKey(telegramKey)) {
				// 20220923 this.rtnmsg = (byte[]) this.incomingTelegramMap.remove(telegramKey);
				byte[] rcvary = (byte[]) this.incomingTelegramMap.remove(telegramKey);
				log.debug("get incomming telegram remove [{}] from incomingTelegramMap after remove size=[{}] TRANSFE message rcvary.length=[{}]",
						telegramKey, this.incomingTelegramMap.size(), rcvary != null ? rcvary.length: 0);
				return rcvary;
			} else {
				log.debug("not yet get incomming telegram from incomingTelegramMap");
				return null; //20220923 add 
			}
			//20220923 mark log.debug("try to get return TRANSFE message rtnmsg.length=[{}]", this.rtnmsg != null ? this.rtnmsg.length: 0);
			//return this.rtnmsg;
		}
	}
	//----

	public void close() {
		try {
			log.info("close connect from Router");
			channel_.close().sync();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void shutdown() {
		try {
			log.info("shutdown connect from Router");
			channel_.close().sync();
			group.shutdownGracefully();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private void doConnect(long _wait) {
		try {
			ChannelFuture f = bootstrap.connect(addr_);
			f.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					if (!future.isSuccess()) {// if is not successful, reconnect
						future.channel().close();
						log.info("seceduleConnect");
						try {
							Thread.sleep(_wait);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						bootstrap.connect(addr_).addListener(this);
					} else {// good, the connection is ok
						channel_ = future.channel();
						// add a listener to detect the connection lost
						addCloseDetectListener(channel_);
						connectionEstablished();

					}
				}

				private void addCloseDetectListener(Channel channel) {
					// if the channel connection is lost, the
					// ChannelFutureListener.operationComplete() will be called
					channel.closeFuture().addListener(new ChannelFutureListener() {
						@Override
						public void operationComplete(ChannelFuture future) throws Exception {
							connectionLost();
							scheduleConnect(3000);
						}
					});

				}
			});
		} catch (Exception ex) {
			scheduleConnect(1000);

		}
	}

	private void scheduleConnect(long millis) {
		timer_.schedule(new TimerTask() {
			@Override
			public void run() {
				doConnect(millis);
			}
		}, millis);
	}

	private ChannelHandler createNMMessageHandler() {
		return new ChannelInboundHandlerAdapter() {
			@Override
			public void channelRead(ChannelHandlerContext ctx, Object msg) {
				log.info("get message from RouterServer");
				//20230703 MatsudairaSyuMe check Direct Memory LEAK
				try {
				//20230703----
					ByteBuf buf = (ByteBuf) msg;
				//20220818 MatsudairasyuMe add for receive buffer for 2 byte length processing
				log.debug("capacity=" + buf.capacity() + " readableBytes=" + buf.readableBytes() + " barray="
						+ buf.hasArray() + " nio=  " + buf.nioBufferCount());
				if (clientSessionRecvMessageBuf.readerIndex() > (clientSessionRecvMessageBuf.capacity() / 2)) {
					clientSessionRecvMessageBuf.discardReadBytes();
					log.debug("adjustment clientSessionRecvMessageBuf readerindex ={}" + clientSessionRecvMessageBuf.readableBytes());
				}
				log.debug("readableBytes={} barray={}", buf.readableBytes(), buf.hasArray());
				if (buf.isReadable() && !buf.hasArray()) {
					log.debug("readableBytes={} barray={}", buf.readableBytes(), buf.hasArray());
//					if (clientSessionRecvMessageBuf.readerIndex() > (clientSessionRecvMessageBuf.capacity() / 2)) {
//						clientSessionRecvMessageBuf.discardReadBytes();
//						log.debug("adjustment clientMessageBuf readerindex ={}" + clientSessionRecvMessageBuf.readableBytes());
//					}
					clientSessionRecvMessageBuf.writeBytes(buf);
					log.debug("clientMessageBuf.readableBytes={}",clientSessionRecvMessageBuf.readableBytes());
					if (clientSessionRecvMessageBuf.readableBytes() >= 2) {
						byte[] lenbary = new byte[2];
						clientSessionRecvMessageBuf.getBytes(clientSessionRecvMessageBuf.readerIndex(), lenbary);
						int rcvmsglen = ((int)(lenbary[0] & 0xff)  * 256 + (int)(lenbary[1] & 0xff));
						if (rcvmsglen >= 2 && rcvmsglen <= clientSessionRecvMessageBuf.readableBytes()) {
							log.debug("clientMessageBuf.readableBytes={} rcvmsglen={}",clientSessionRecvMessageBuf.readableBytes(), rcvmsglen);
							//20220819 MatsudairaSyuMe add COMM_STATE
							//rtnmsg = cnvResultTelegram();
							byte[] rtnTmpary = cnvResultTelegram();
							switch (COMM_STATE.ById(rtnTmpary[0])) {
							case TRANSF: // <==接收傳送電文
								byte[] rtnmsg = new byte[rtnTmpary.length - 1];  //20220923 rtnmsg change from class parameter to method  parameter
								System.arraycopy(rtnTmpary, 1, rtnmsg, 0, (rtnTmpary.length - 1));
								//20220905 MatsudairaSyuMe
								String telegramKey = dataUtil.getTelegramKey(rtnmsg);
								if (incomingTelegramMap.containsKey(telegramKey)) {
									if (incomingTelegramMap.replace(telegramKey, rtnmsg) == null)
										log.error("new incoming update by telegramKey [{}] into map table error!!!!", telegramKey);
									else
										log.debug("new incoming already update by telegramKey [{}] into map table", telegramKey);
								} else {
									incomingTelegramMap.put(telegramKey, rtnmsg);
									log.debug("new incoming telegram put into map table by telegramKey [{}]", telegramKey);
								}
								log.debug("new incoming telegram map table size=[{}]", incomingTelegramMap.size());
								rtnmsg = null; //clear this parameter !!!!!!
								//----20220905
								break;
							case CANCEL: // <==刪除記錄
								log.info("RouteServerHandler--> RouteConnection 刪除記錄 ignore");
								break;
							case CHECK: // <==詢問
								log.info("RouteServerHandler--> RouteConnection 詢問 ignore");
								break;
							case ACK: // <==回覆成功
								log.info("RouteServerHandler--> RouteConnection 回覆成功 ignore");
								break;
							case NAK: // <==回覆失敗
								log.info("RouteServerHandler--> RouteConnection 回覆失敗 ignore");
								break;
							}
							rtnTmpary = null;
							//if (rtnmsg != null && rtnmsg.length > 0) {
							//	handleMessage(new String(rtnmsg));
							//}
						} else
							log.debug("clientMessageBuf.readableBytes={} rcvmsglen={}",clientSessionRecvMessageBuf.readableBytes(), rcvmsglen);
					} else
						log.debug("clientMessageBuf.readableBytes lower to 2 bytes wait next incomming");
					//----
				}
				//20230703 MatsudairaSyuMe use direct memory Leak check
				} finally {
					io.netty.util.ReferenceCountUtil.release(msg);
				}
				//----20230703
			}

			@Override
			public void channelActive(ChannelHandlerContext ctx) throws Exception {
				//20220818 MatsudairasyuMe add for receive buffer for 2 byte length processing
				clientSessionRecvMessageBuf.clear();
				//----
				log.info("channelActive");
			}

			@Override
			public void channelInactive(ChannelHandlerContext ctx) throws Exception {
				log.info("channelInActive");
			}
			/*  20220818 MatsudairasyuMe add for receive buffer for 2 byte length processing
			 * cnvResultTelegram() no need to check if clientSessionRecvMessageBuf.hasArray() && clientSessionRecvMessageBuf.readableBytes() > 0
			 *                                      or if clientSessionRecvMessageBuf.readableBytes() >= 2
			 *                                      or if two bytes length array convert to size
			 *                                            size >= 2 && size <= clientSessionRecvMessageBuf.readableBytes()
			 */
			private byte[] cnvResultTelegram() {
				byte[] rtn = null;
				byte[] lenbary = new byte[2];
				int size = 0;
				clientSessionRecvMessageBuf.getBytes(clientSessionRecvMessageBuf.readerIndex(), lenbary);
				size = ((int)(lenbary[0] & 0xff)  * 256 + (int)(lenbary[1] & 0xff));
				log.debug("clientSessionRecvMessageBuf.readableBytes={} size={}", clientSessionRecvMessageBuf.readableBytes(), size);
				byte[] recvrtnmsg = new byte[size];
				clientSessionRecvMessageBuf.readBytes(recvrtnmsg);
				log.debug("read {} byte(s) from clientMessageBuf after {}", size,
						clientSessionRecvMessageBuf.readableBytes());
				if (recvrtnmsg.length - 2 > 0) {
					rtn = new byte[recvrtnmsg.length - 2];
					System.arraycopy(recvrtnmsg, 2, rtn, 0, (recvrtnmsg.length - 2));
					recvrtnmsg = null;
				}
				log.debug("get rtn len= {}", (rtn == null) ? 0 : rtn.length);
				lenbary = null;
				return rtn;
			}
			//----
		};
	}

	public void handleMessage(String msg) {
		//20220818 MatsudairasyuMe add for receive buffer for 2 byte length processing
		log.info("receive from RouteServer [{}]", msg);
	}

	public void connectionLost() {
		log.info("connectionLost()");
	}

	public void connectionEstablished() {
		//20220905 MatsudairaSyuMe
		this.incomingTelegramMap.clear();
		//----
		//20220818 MatsudairasyuMe add for receive buffer for 2 byte length processing
		this.clientSessionRecvMessageBuf.clear();
		//----
		log.info("connectionEstablished()");
		/*
		 * try { send( "hello"); } catch (IOException e) { e.printStackTrace(); }
		 */
	}

}
