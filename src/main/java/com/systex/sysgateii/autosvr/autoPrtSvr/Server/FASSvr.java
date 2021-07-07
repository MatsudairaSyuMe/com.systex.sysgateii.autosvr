package com.systex.sysgateii.autosvr.autoPrtSvr.Server;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.systex.sysgateii.autosvr.comm.Constants;
import com.systex.sysgateii.autosvr.comm.TXP;
import com.systex.sysgateii.autosvr.listener.MessageListener;
import com.systex.sysgateii.autosvr.ratesvr.Server.ServerProducer;
import com.systex.sysgateii.autosvr.util.CharsetCnv;
import com.systex.sysgateii.autosvr.util.dataUtil;
import com.systex.sysgateii.comm.pool.fas.FASSocketChannel;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;

/*
 * FAS socket connect servr
 * socket controller
 *    
 * MatsudairaSyuMe
 * Ver 1.0
 *  20200115
 */
public class FASSvr implements MessageListener<byte[]>, Runnable {
	private static Logger log = LoggerFactory.getLogger(FASSvr.class);
	private Logger faslog = LoggerFactory.getLogger("faslog");
	static FASSvr server;

	static String[] NODES = { "" };
	private FASSocketChannel ec2 = null;
	private static final int FAIL_EVERY_CONN_ATTEMPT = 10;
	private static final long TEST_TIME_SECONDS = 1;
	private Channel currConn;
	private boolean TITA_TOTA_START = false;
	private ConcurrentHashMap<Channel, File> currSeqMap = null;
	private File currSeqF = null;
	private String header1 = "\u000f\u000f\u000f";
	private String header2 = "";
	private int setSeqNo = 0;
	private String getSeqStr = "";
	private String fasSendPtrn = "-->FAS len %4d :[............%s]";
	private String fasRecvPtrn = "<--FAS len %4d :[............%s]";
	private CharsetCnv charcnv = new CharsetCnv();
	int bufferSize;
	int tsKeepAlive;
	int tsIdleTimeout;
	String bindAddr;
	ConcurrentHashMap<String, String> map;

	public FASSvr(ConcurrentHashMap<String, String> map) {
		log.info("FASSvr start");
		bufferSize = Constants.DEF_CHANNEL_BUFFER_SIZE;
		tsKeepAlive = Constants.DEF_KEEP_ALIVE;
		tsIdleTimeout = Constants.DEF_IDLE_TIMEOUT;
		bindAddr = Constants.DEF_SERVER_ADDRESS;
		this.map = map;
	}

	@Override
	public void messageReceived(String serverId, byte[] msg) {
		// TODO Auto-generated method stub
		log.debug(" ms");
	}

	public void run() {
		RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
		String jvmName = bean.getName();
		long pid = Long.valueOf(jvmName.split("@")[0]);
		log.info("FASSvr MainThreadId={}", pid);
		try {
			//----20200422 test
			bindAddr = map.get("system.ip");
			int port = Integer.parseInt(map.get("system.port"));
			log.info("port=" + port);
			String tmps = map.get("boards.board.ip");
			log.info("boards.board.ip=" + tmps);
			if (tmps.startsWith("["))
				tmps = tmps.substring(1);
			if (tmps.endsWith("]"))
				tmps = tmps.substring(0, tmps.length() - 1);
			String[] localary = tmps.split(",");
			ServerProducer producer = new ServerProducer(map.get("system.svrid").trim(), bindAddr, port, bufferSize,
			tsKeepAlive, tsIdleTimeout);
			List<String> wsnos = new ArrayList<String>();
			wsnos.add(addLeftZeroForNum(98, 4));
			for (String cs : localary) {
				producer.getIpList().add(cs.trim());
				String[] p = cs.trim().split("\\.");
				wsnos.add(addLeftZeroForNum(Integer.valueOf(p[3]), 4));
			}
			List<String> brnos = new ArrayList<String>();
			tmps = map.get("boards.board.brno");
			if (tmps.startsWith("["))
				tmps = tmps.substring(1);
			if (tmps.endsWith("]"))
				tmps = tmps.substring(0, tmps.length() - 1);
			localary = tmps.split(",");
			for (String cs : localary)
				brnos.add(cs.trim());
			//20200215
			producer.getBrnoList().addAll(brnos);
			for (int j = 0; j < brnos.size(); j++) {
				if (producer.getBrnoaddrGrp().containsKey(brnos.get(j)))
					producer.getBrnoaddrGrp().get(brnos.get(j)).add(producer.getIpList().get((j)));
				else {
					List<String> subbrnoList = new ArrayList<String>();
					subbrnoList.add(producer.getIpList().get((j)));
					producer.getBrnoaddrGrp().put(brnos.get(j), subbrnoList);
				}
			}
			//----
			//20200608
			if (!map.get("system.port").isEmpty() && !map.get("system.port").trim().equals("0")) {
				Thread thread = new Thread(producer);
				thread.start();
			}
			//----
			this.ec2 = new FASSocketChannel(NODES, producer, brnos, wsnos);
			//----
//			this.ec2 = new FASSocketChannel(NODES);
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}
	}

	public void stop() {
		log.debug("Enter stop");
	}

	public static void createServer(ConcurrentHashMap<String, String> _map) {
		log.debug("Enter createServer");
		if (_map != null) {
			NODES = _map.get("svrsubport.svrip").split(",");
			for (int i = 0; i < NODES.length; i++) {
				NODES[i] = NODES[i].trim();
				log.debug("Enter createServer {}", NODES[i]);
			}
			//20210116 MatsudairaSyuMe for imcoming TOTA telegram Map
			Constants.incomingTelegramMap.clear();
			//----
			log.debug("Enter createServer size={}", NODES.length);
			server = new FASSvr(_map);
		}
	}

	public static void startServer() {
		log.debug("Enter startServer");
		if (server != null) {
			server.run();
		}

	}

	public static void stopServer() {
		log.debug("Enter stopServer");
		if (server != null) {
			server.stop();
		}
	}

	public static void sleep(int t) {
		try {
			TimeUnit.SECONDS.sleep(t);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static FASSvr getFASSvr() {
		return server;
	}

	public boolean sendTelegram(byte[] telmsg) {
		//20210116 MatsydairaSyuMe
		synchronized (NODES) {
			boolean rtn = false;
			if (telmsg == null) {
				log.debug("sendTelegram error send telegam null");
				return rtn;
			}
//20210112 MatsudairaSyuMe			int attempt = 0;
			log.debug("1 start sendTelegram [{}]", this.currConn);
			// 20210112 MatsudairaSyuMe
			/*
			 * int attempt = 0;
			 * while (null == (this.currConn = this.ec2.getConnPool().lease())) {
			 *    if (++attempt < FAIL_EVERY_CONN_ATTEMPT) {
			 * //20210112 MatsudairaSyuMe
			 *        log.warn("WORNING!!! poll busy re-try after {} second(s)",
			 *        TEST_TIME_SECONDS); Thread.sleep(TEST_TIME_SECONDS * 1000);
			 *     } else
			 *        break;
			 *  }
			 */
			try {
				this.currConn = this.ec2.getConnPool().lease(FAIL_EVERY_CONN_ATTEMPT, TEST_TIME_SECONDS);
				InetSocketAddress localsock = (InetSocketAddress) this.currConn.localAddress();
				InetSocketAddress remotsock = (InetSocketAddress) this.currConn.remoteAddress();
				MDC.put("SERVER_ADDRESS", (String) remotsock.getAddress().toString());
				MDC.put("SERVER_PORT", String.valueOf(remotsock.getPort()));
				MDC.put("LOCAL_ADDRESS", (String) localsock.getAddress().toString());
				MDC.put("LOCAL_PORT", String.valueOf(localsock.getPort()));
				int sendlen = TXP.CONTROL_BUFFER_SIZE + telmsg.length;
				this.currSeqMap = this.ec2.getseqfMap();
				this.currSeqF = this.currSeqMap.get(this.currConn);
				int seqno = 0;
				try {
					// 20210107 mark for use local parameter
					/*
					 * this.setSeqNo = Integer.parseInt(FileUtils.readFileToString(this.currSeqF,
					 * Charset.defaultCharset())) + 1; //20200910 sdjust setSeqNO from 2 ~ 999 if
					 * (this.setSeqNo > 999 || this.setSeqNo == 1) this.setSeqNo = 2;
					 * FileUtils.writeStringToFile(this.currSeqF, Integer.toString(this.setSeqNo),
					 * Charset.defaultCharset()); header2 =
					 * String.format("\u0001%03d\u000f\u000f",setSeqNo);
					 */
					seqno = Integer.parseInt(FileUtils.readFileToString(this.currSeqF, Charset.defaultCharset()))
							+ 1;
					//20210630 MatsudairaSyuMe make sure seqno Exceed the maximum
					if (seqno >= 999) {
						seqno = 0;
					}
					FileUtils.writeStringToFile(this.currSeqF, Integer.toString(seqno), Charset.defaultCharset());
					header2 = String.format("\u0001%03d\u000f\u000f", seqno);
				} catch (Exception e) {
					log.warn("WORNING!!! update new seq number string {} error {}", seqno, e.getMessage());
				}

				ByteBuf req = Unpooled.buffer();
				req.clear();
				req.writeBytes(header1.getBytes());
				req.writeBytes(dataUtil.to3ByteArray(sendlen));
				req.writeBytes(header2.getBytes());
				req.writeBytes(telmsg);
				this.currConn.writeAndFlush(req.retain()).sync();
				rtn = true;
				try {
					log.debug(String.format(fasSendPtrn,
							header1.getBytes().length + dataUtil.to3ByteArray(sendlen).length
									+ header2.getBytes().length + telmsg.length,
							charcnv.BIG5bytesUTF8str(telmsg)) + " isCurrConnNull=[" + isCurrConnNull() + "]");
					faslog.debug(
							String.format(fasSendPtrn,
									header1.getBytes().length + dataUtil.to3ByteArray(sendlen).length
											+ header2.getBytes().length + telmsg.length,
									charcnv.BIG5bytesUTF8str(telmsg)));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// ----
			} catch (final InterruptedException e) {
				log.debug("get connect from pool error {}", e.getMessage());
			} catch (final Throwable cause) {
				log.debug("get connect from pool error {}", cause.getMessage());
			} finally {
				log.debug("2 end sendTelegram isCurrConnNull=[{}]", isCurrConnNull());
				releaseConn();
			}
			log.debug("3 end sendTelegram isCurrConnNull=[{}]", isCurrConnNull());
			return rtn;
		}
		//----
	}


	//20210107
	public void releaseConn() {
		//20210116 MatsydairaSyuMe
		synchronized (NODES) {
			try {
			//20210112 MatshdairaSyume release connection error handling
				if (this.ec2.getConnPool() != null && this.currConn != null) {
					this.ec2.getConnPool().release(this.currConn);
					this.currConn = null;
					log.debug("return connect to pool");
				} else
					log.warn("connection poll is [{}] or current connection is [{}]", this.ec2.getConnPool(), this.currConn);
			} catch (final Throwable cause) {
				log.debug("free connect from pool error {}", cause.getMessage());
			}
		}
//		log.debug("return connect to pool");
	}
	//20210116 MatshdairaSyume
	public byte[] getResultTelegram(String telegramKey) {
		synchronized (Constants.incomingTelegramMap) {
			log.debug("look by telegramKey=[{}] size={}", telegramKey, Constants.incomingTelegramMap.size());
			byte[] rtn = null;
			if (null == (rtn = (byte[]) Constants.incomingTelegramMap.remove(telegramKey))) {
				log.debug("not yet get incomming telegram");
			} else {
				log.debug("get incomming telegram");
//				releaseConn();
			}
			return rtn; 
		}
	}
	//----
	public byte[] getResultTelegram() {
		byte[] rtn = null;
		byte[] lenbary = new byte[3];
		byte [] telmbyteary = null;
		int size = 0;
		//20210112 mark by MatsudairaSyuMe TITA_TOTA_START flag checking change to PrtCli
		////if (isTITA_TOTA_START()) {
			if (this.ec2.getrcvBuf().hasArray() && this.ec2.getrcvBuf().readableBytes() > 0) {
				if (this.ec2.getrcvBuf().readableBytes() >= 12) {
					this.ec2.getrcvBuf().getBytes(this.ec2.getrcvBuf().readerIndex() + 3, lenbary);
					size = dataUtil.fromByteArray(lenbary);
					log.debug("clientMessageBuf.readableBytes={} size={}",this.ec2.getrcvBuf().readableBytes(), size);
					if (size > 0 && size <= this.ec2.getrcvBuf().readableBytes()) {
						telmbyteary = new byte[size];
						this.ec2.getrcvBuf().readBytes(telmbyteary);
						log.debug("read {} byte(s) from clientMessageBuf after {}", size, this.ec2.getrcvBuf().readableBytes());
						//this.getSeqStr = new String(telmbyteary, 7, 3);
						//this.currSeqMap = this.ec2.getseqfMap();
						//this.currSeqF = this.currSeqMap.get(this.currConn);
						try {
							//20200910 mark by Scott Hong for not write back seqno
//							FileUtils.writeStringToFile(this.currSeqF, this.getSeqStr, Charset.defaultCharset());
							//----
							rtn = new byte[telmbyteary.length - TXP.CONTROL_BUFFER_SIZE];
							System.arraycopy(telmbyteary, TXP.CONTROL_BUFFER_SIZE, rtn, 0,
									telmbyteary.length - TXP.CONTROL_BUFFER_SIZE);
							// ----
							//20200903 convert 0x00 to byte ' '
							byte [] faslogary = new byte[rtn.length];
							System.arraycopy(rtn, 0, faslogary, 0, rtn.length);
							for (int _tmpidx = 0; _tmpidx < rtn.length; _tmpidx++)
								faslogary[_tmpidx] = (rtn[_tmpidx] == (byte)0x0 ? (byte)' ': rtn[_tmpidx]);
							faslog.debug(String.format(fasRecvPtrn, telmbyteary.length, charcnv.BIG5bytesUTF8str(faslogary)));
//							faslog.debug(String.format(fasRecvPtrn, telmbyteary.length, charcnv.BIG5bytesUTF8str(rtn)));
							// ----
							rtn = remove03(rtn);
							log.debug("get rtn len= {}", rtn.length);
						} catch (Exception e) {
							log.warn("WORNING!!! update new seq number string {} error {}",this.getSeqStr, e.getMessage());
						}
						//20210112 mark by MatsudairaSyuMe TITA_TOTA_START flag checking change to PrtCli
						////this.setTITA_TOTA_START(false);
						//----
						//200210111 MatsudairaSyuMe 
						// exclusive channel for TITA/TOTA processing return connect to pool only receive correct TOTA telegram
						if (rtn != null && rtn.length > 0)
							releaseConn();
						//----
//						break;
					}// else
//						break;
				}
//				rtn = new byte[this.ec2.getrcvBuf().readableBytes()];
//				log.debug("get rtn len= {}", rtn.length);
//				this.ec2.getrcvBuf().readBytes(rtn);
//				this.setTITA_TOTA_START(false);
				/*20210111 MatsudairaSyuMe mark for exclusive channel for TITA/TOTA processing 
				try {
					if (rtn != null && rtn.length > 0)
						this.ec2.getrcvBuf().clear();
					this.ec2.getConnPool().release(this.currConn);
				} catch (final Throwable cause) {
					log.debug("free connect from pool error {}", cause.getMessage());
				}
				log.debug("return connect to pool");
				*/
			}
		//20210112 mark by MatsudairaSyuMe TITA_TOTA_START flag checking change to PrtCli
		////}
		return rtn;
	}
	
	private byte[] remove03(byte[] source) {
		if (source[source.length - 1] == 0x03) {
			source = ArrayUtils.subarray(source, 0, source.length - 1);
			log.debug("remove03");
		}
		return source;
	}
	private String addLeftZeroForNum(int num, int strLength) {
		return addLeftZeroForNum(Integer.toString(num), strLength);
	}

	private String addLeftZeroForNum(String str, int strLength) {
		int strLen = str.length();
		if (strLen < strLength) {
			while (strLen < strLength) {
				StringBuffer sb = new StringBuffer();
				// sb.append(appn).append("0");// 左補0
				sb.append(str).append("0");// 右補0
				str = sb.toString();
				strLen = str.length();
			}
		}

		return str;
	}

	public boolean isTITA_TOTA_START() {
		return TITA_TOTA_START;
	}

	public void setTITA_TOTA_START(boolean tITA_TOTA_START) {
		TITA_TOTA_START = tITA_TOTA_START;
		log.info("setTITA_TOTA_START=[{}]", TITA_TOTA_START);
	}

	//20210112 add by MatsudairaSyuMe TITA_TOTA_START flag checking change to PrtCli
	public boolean isCurrConnNull() {
		return this.currConn == null;
	}

}
