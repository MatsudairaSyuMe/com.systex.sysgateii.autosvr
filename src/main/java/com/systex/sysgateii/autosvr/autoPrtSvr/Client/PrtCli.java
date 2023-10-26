package com.systex.sysgateii.autosvr.autoPrtSvr.Client;
import java.util.Calendar;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
//import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.systex.sysgateii.autosvr.autoPrtSvr.Server.FASSvr;
import com.systex.sysgateii.autosvr.autoPrtSvr.Server.PrnSvr;
import com.systex.sysgateii.autosvr.comm.Constants;
import com.systex.sysgateii.autosvr.comm.TXP;
import com.systex.sysgateii.autosvr.conf.DscptMappingTable;
import com.systex.sysgateii.autosvr.conf.MessageMappingTable;
import com.systex.sysgateii.autosvr.dao.GwDao;
import com.systex.sysgateii.autosvr.listener.ActorStatusListener;
import com.systex.sysgateii.autosvr.listener.EventListener;
import com.systex.sysgateii.autosvr.listener.EventType;
import com.systex.sysgateii.autosvr.prtCmd.Printer;
import com.systex.sysgateii.autosvr.prtCmd.Impl.CS4625Impl;
import com.systex.sysgateii.autosvr.prtCmd.Impl.CS5240Impl;
import com.systex.sysgateii.autosvr.telegram.P0080TEXT;
import com.systex.sysgateii.autosvr.telegram.P0880TEXT;
import com.systex.sysgateii.autosvr.telegram.P1885TEXT;
import com.systex.sysgateii.autosvr.telegram.P85TEXT;
import com.systex.sysgateii.autosvr.telegram.Q0880TEXT;
import com.systex.sysgateii.autosvr.telegram.Q98TEXT;
import com.systex.sysgateii.autosvr.telegram.TITATel;
import com.systex.sysgateii.autosvr.telegram.TOTATel;
import com.systex.sysgateii.autosvr.util.CharsetCnv;
import com.systex.sysgateii.autosvr.util.LogUtil;
import com.systex.sysgateii.autosvr.util.StrUtil;
import com.systex.sysgateii.autosvr.util.dataUtil;
import com.systex.sysgateii.autosvr.util.ipAddrPars;
import com.systex.sysgateii.comm.mdp.mdcliapi2;
//20220809 MatsudairaSyuMe
import com.systex.sysgateii.comm.sdk.RouteConnection;
//----
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
//20210627 MatsudairaSyuMe add Majordomo Protocol processing
import org.zeromq.ZMsg;
//----
@Sharable // 因為通道只有一組 handler instance 只有一個，所以可以 share
public class PrtCli extends ChannelDuplexHandler implements Runnable, EventListener {
	private static Logger log = LoggerFactory.getLogger(PrtCli.class);
	//20220819 add trace log
	private Logger trace = LoggerFactory.getLogger("trace");

	private Logger aslog = null;
	//2020115
//	public static Logger amlog = null;
	private Logger amlog = null;
//	public static Logger atlog = null;
	private Logger atlog = null;

	public String pid = "";

	private static final ByteBuf HEARTBEAT_SEQUENCE = Unpooled
			.unreleasableBuffer(Unpooled.copiedBuffer("hb_request", CharsetUtil.UTF_8));

	private String clientId = "";       // brno from set up XML file
	private String byDate = "";
	// for ChannelDuplexHandler function
	ChannelHandlerContext currentContext;
	Channel clientChannel;
	private CountDownLatch readLatch;
	private String idleStateHandlerName = "idleStateHandler";
	public ByteBuf clientMessageBuf = Unpooled.buffer(4096);
	Object readMutex = new Object();
	// end for ChannelDuplexHandler function

	private Bootstrap bootstrap = new Bootstrap();
	//20210404 MatsudairaSyuMe
	private NioEventLoopGroup group = null;
	//----
	private final static AtomicBoolean isConnected = new AtomicBoolean(false);

	private InetSocketAddress rmtaddr = null;
	private InetSocketAddress localaddr = null;
	//20210427 MatsudairaSyuMe
	private String remoteHostAddr = "";
	private String localHostAddr = "";
	//----
	private Channel channel_;
	private Timer timer_;
	private String brws = null;      // BRNO + WSNO from set up XML file
	private String type = null;      // printer type from set up XML file
	private String autoturnpage = null;
	private String getSeqStr = "";
	private int setSeqNo = 0;
	private File seqNoFile;

	private Printer prt = null;
	private int bufferSize = Integer.parseInt(System.getProperty("bufferSize", Constants.DEF_CHANNEL_BUFFER_SIZE + ""));
	private static final int MAXDELAY = 6;
	private static final int RECONNECT = 10;
	private int iRetry = 0;
	private boolean showStateMsg = false;

	// Signal Number
	private byte L1 = (byte) 0x01; // 1:0000001
	private byte L6 = (byte) 0x02; // 2:0000010
	private byte L5 = (byte) 0x04; // 4:0000100
	private byte L3 = (byte) 0x08; // 8:0001000
	private byte L0 = (byte) 0x10; // 16:0010000
	private byte L4 = (byte) 0x20; // 32:0100000
	private byte L2 = (byte) 0x40; // 64:1000000
	private byte IX = (byte) -1;
	private byte I0 = (byte) 0x00;
	private byte I1 = (byte) 0x01;
	private byte I2 = (byte) 0x02;
	private byte I3 = (byte) 0x04;
	private byte I4 = (byte) 0x08;
	private byte I5 = (byte) 0x10;
	private byte I6 = (byte) 0x20;
	private byte I7 = (byte) 0x40;
	private byte I8 = (byte) 0x80;

	// State Value
	public static final int SESSIONBREAK = -1; // 94補摺機斷線！.
	public static final int OPENPRINTER = 0;
//	public static final int CHECKPRINTER = 1;
//	public static final int CHECKPRINTERWAIT = 2;

	public static final int ENTERPASSBOOKSIG = 1; // 00請插入存摺... Capture Passbook
	public static final int CAPTUREPASSBOOK = 2;
	public static final int GETPASSBOOKSHOWSIG = 3; // Show Signal
	public static final int SETCPI = 4; // Set CPI
	public static final int SETLPI = 5; // Set LPI
	public static final int SETPRINTAREA = 6; // Set print area
	public static final int READMSR = 7; // Read MSR
	public static final int READMSRERR = 8; // 11磁條讀取失敗！Show Signal
	public static final int CHKACTNO = 9;// Check ACTNO(BOT or not) , PAGE, maybe line...
	public static final int CHKBARCODE = 10; // Get Passbook's Page Type=2
	public static final int SETSIGAFTERCHKBARCODE = 11; // Show Signal after get Passbook's Page Type=2
	public static final int EJECTAFTERPAGEERROR = 12; // Show Signal after get Passbook's Page error
	public static final int EJECTAFTERPAGEERRORWAIT = 13; // Show Signal after get Passbook's Page error
	public static final int READANDCHECKMSR = 14; // read MSR after write and check for previous constant 20211028 MatsudairaSyuMe MSR re-read
	public static final int SNDANDRCVTLM = 15; // compose TITA and send tita & Receive TOTA and check error
	public static final int SETREQSIG = 16; // Show Signal before send telegram to Host
	public static final int WAITSETREQSIG = 17; // wait Show Signal before send telegram to Host finished
	public static final int SENDTLM = 18; // start send TOTA and check error
	public static final int RECVTLM = 19; // Receive TOTA and check error
	public static final int STARTPROCTLM = 20; // start to send tita & Receive TOTA and check error
	
	public static final int PBDATAFORMAT = 21; //// Format 列印台幣存摺資料格式  print data
	public static final int FCDATAFORMAT = 22; //// Format 列印外匯存摺資料格式  print data
	public static final int GLDATAFORMAT = 23; //// Format 列印黃金存摺資料格式  print data
	public static final int FORMATPRTDATAERROR = 24; // 61存摺資料補登失敗！Show Signal
	public static final int WRITEMSR = 25; //// Write MSR
	public static final int WRITEMSRWAITCONFIRM = 26;  //// Write MSR ware confirm
	public static final int WRITEMSRERR = 27; //// Write MSR ERROR 71存摺磁條寫入有問題！
	public static final int READMSRERRAFTERWRITEMSRERR = 28; // 11磁條讀取失敗(1)！
	public static final int READMSRSUCAFTERWRITEMSRERR = 29; // 12存摺磁條讀取成功(1)！
	public static final int COMPMSRSUCAFTERWRITEMSRERR = 30; // 12存摺磁條比對正確(1)！
	public static final int COMPMSRERRAFTERWRITEMSRERR = 31; // 12存摺磁條比對失敗(1)！
	public static final int EJECTAFTERPAGEERRORWAITSTATUS = 32;
	public static final int WRITEMSRERRSHOWSIG = 33; // 71存摺磁條寫入失敗！ Show Signal
	public static final int SNDANDRCVDELTLM = 34; // 72存摺資料補登成功！
	public static final int SNDANDRCVDELTLMCHKEND = 35;        // 72存摺資料補登成功, 檢查翻頁及燈號開始！
	public static final int SNDANDRCVDELTLMCHKENDSETSIG = 36; // 72存摺資料補登成功, 檢查翻頁及燈號完成退摺開始！
	public static final int SNDANDRCVDELTLMCHKENDEJECTPRT = 37; // 72存摺資料補登成功, 檢查翻頁及燈號退摺！
	public static final int DELPASSBOOKREGCOMPERR = 38; // 73存摺資料補登刪除失敗！Show Signal
//	public static final int NOTFINISH = 38; // iEnd != 0 continue printing
//	public static final int NOTFINISHATP = 39; // iEnd != 0 continue printing, Auto turn page
//	public static final int NOTFINISHHTP = 40; // iEnd != 0 continue printing, Handy turn page, Show Reentry signal.
	public static final int FINISH = 39; // iEnd == 0 printing finished,
											// === 2 超過存摺頁次, 仍然顯示補登完成燈號
											// go to capture
	private int curState = SESSIONBREAK;
	//20200616
	private int lastState = SESSIONBREAK;
	private long durationTime = -1L;
	private long lastStateTime = -1L;
	//--
	private int iFirst = 0; // 0: start to print
							// 1: print after turn page
	private int iEnd = 0; // !< 繼續記號 0:開始 1:請翻下頁 2:頁次超過最大頁數
	private String actfiller = ""; // !< 帳號保留 MSR for PB/FC len 4
	private String msrbal = ""; // !< 磁條餘額 MSR for PB/FC len 14, GL len 12
	private String cline = ""; // !< 行次 MSR for PB/FC/GL len 2
	private String cpage = ""; // !< 頁次 MSR for PB/FC/GL len 2
	private String bkseq = ""; // !< 領用序號 MSR for PB len 1, FC len 2
	private String no = ""; // !< 存摺號碼 MSR for GL len 9
	private String pbver = ""; //!< MSR for FC 領用序號
	private int nline = 0;
	private int npage = 0;
	private int rpage = 0;
	private int iFig = 0; // type of passbook, AP type -- 1:PB / 2:FC / 3:GL
	private String total_con = ""; // total NB count
	private String org_mbal = ""; // original MSR's balance
	private int iCount = 0;
	private int iCon = 0;
	//20200724
	private String con = "";
	private String dCount = "";
	private int iLine = 0;
	private int pbavCnt = 999;
	byte[] fepdd = new byte[2];
	TITATel tital = null;
	TOTATel total = null;
//	private String cbkseq = "";
	private static final boolean firstOpenConn = true;
	private String account = "";
	private String catagory = "";   // working passbook workstation no
	private byte[] cusid = null;
	//20211028 MatsudairaSyuMe MSR re-read check buffer
	private byte[] reReadcusid = null;
	//----
	//20210627 mark
	private FASSvr dispatcher;
	private boolean alreadySendTelegram = false;
	//20210122
	private boolean PRT_CLI_TITA_TOTA_START = false;
	//----
	private byte[] resultmsg = null;
	private byte[] rtelem = null;
	private String msgid = "";
	ConcurrentHashMap<String, String> tx_area = new ConcurrentHashMap<String, String>();

	private List<byte[]> pb_arr = new ArrayList<byte[]>();
	private List<byte[]> fc_arr = new ArrayList<byte[]>();
	private List<byte[]> gl_arr = new ArrayList<byte[]>();
	private P0080TEXT p0080DataFormat = null;
	private Q0880TEXT q0880DataFormat = null;
	private P0880TEXT p0880DataFormat = null;
	private String pasname = "        ";
    //	"                                                     請翻下頁繼續補登\r\n";
	private byte[] chgpgary = {(byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0x20, (byte)0xbd, (byte)0xd0, (byte)0xc2, (byte)0xbd, (byte)0xa4, (byte)0x55, (byte)0xad, (byte)0xb6, (byte)0xc4, (byte)0x7e, (byte)0xc4, (byte)0xf2, (byte)0xb8, (byte)0xc9, (byte)0xb5, (byte)0x6e, (byte)0x0d, (byte)0x0a};
    private boolean passSNDANDRCVTLM = false; //202007114 check Send_Receive()

	private DscptMappingTable descm = null;
	//20200716 add for message table
	private MessageMappingTable m_Msg = null;
	private boolean Send_Recv_DATAInq = true;
	private CharsetCnv charcnv = new CharsetCnv();
	//20200506 receive time from Host default 60 seconds
	private int responseTimeout = 60 * 1000;// 毫秒
	private String curSockNm = "";
	//----
	//20200513
	private String statusfields = "";
	//20201222 add 'SYSTEM' to TB_AUDEVSTS.MODIFIER
	private String updValueptrn = "'%s','%s','%s','%s','0','%s','%s','1','SYSTEM','SYSTEM'";
	//分行設備分類0: 匯率顯示版 1:利率顯示版 2: AUTO46 自動補褶機 3: AUTO52 自動補褶機
	private String typeid = "2"; //default for AUTP46
	private GwDao jsel2ins = null;
	//----
	List<ActorStatusListener> actorStatusListeners = new ArrayList<ActorStatusListener>();

	private long startTime;
	//20200722
	private long stateStartTime;
	//----
	private long lastCheckTime;
	//20200906
	private EventType curMode = EventType.ACTIVE;
	//----
	//20201026 fir cmdhis
	private String sno = "";
	private String hisfldvalssptrn2 = "'%s','%s','%s','%s','%s','%s','%s','%s'";
	private GwDao cmdhiscon = null;
	//----
	//20201119
	GwDao amtbcon = null;
	private String amstatusptrn = "'%s','%s','%s','%s','%s'";

	//20210116 MataudairaSyuMe  for incoming TOTA telegram
	private String telegramKey = "";
	//----
	private boolean changeLightOnLastPage = false; //20220927 MatsudairaSyuMe when printer stop print job on last line and last page will show error light
	//----
	//20210414 MatsudairaSyuMe
	private final long connectTimeOut = 3l;
	private final TimeUnit connectTimeUnit = TimeUnit.SECONDS;
	//----
	//20210627 MatsudairaSyuMe add Majordomo Protocol processing
//	private mdcliapi2 clientSession = null;
	//----
	//20220809 MatsudairaSyuMe add RouteConnect
	private RouteConnection clientSession = null;
	//---
	//20220429  MatsudairaSyuMe
	private boolean startIdleMode = false;
	private long lastRequestTime = 0l;
	//----
	// 20230522 MasudairaSyuMe register last time read data from pass book print
	private long lastTimeFromPrt = 0l;

	public List<ActorStatusListener> getActorStatusListeners() {
		return actorStatusListeners;
	}

	public void setActorStatusListeners(List<ActorStatusListener> actorStatusListeners) {
		this.actorStatusListeners = actorStatusListeners;
	}

	//20210627 change to use MDP take off dispatcher
//	public PrtCli(ConcurrentHashMap<String, Object> map, FASSvr dispatcher, Timer timer)
	//20220905 MAtsudairaSyuMe using RouteConnection as dispatcher
	// public PrtCli(ConcurrentHashMap<String, Object> map, Timer timer)
	public PrtCli(ConcurrentHashMap<String, Object> map, RouteConnection prnsvrdispatcher, Timer timer)
	{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		this.setByDate(sdf.format(new Date()));

		this.brws = (String) map.get("brws");
		this.type = (String) map.get("type");
		this.autoturnpage = (String) map.get("autoturnpage");
		this.autoturnpage = this.autoturnpage.toLowerCase();
		this.timer_ = timer;
		this.clientId = this.brws.substring(0, 3);
		this.iRetry = 1;
		this.curState = SESSIONBREAK;
		this.iFirst = 0;
		//20210627 MatsudairaSyuMe add Majordomo Protocol processing
//		this.clientSession = new mdcliapi2("tcp://localhost:5555", false);//20220728 set debug flag to false , 20220803 turn off debug
//20220729		this.clientSession.setTimeout(PrnSvr.getReqTime());//20220613//, 20220718 MatsudairaSyuMe change from PrnSvr.getReqTime() to setResponseTimeout
		//----
		//20220809 MatsydairaSyuMe use RouteConnection
		//20220905 MatsudairaSyuMe using RouteConnection as dispatcher
		//this.clientSession = new RouteConnection("127.0.0.1", 5555, new Timer());
		this.clientSession = prnsvrdispatcher;
		//20230316 MatsudairaSyuMe mark up for directly read from TB_AUDM
		//this.descm = new DscptMappingTable();
		//----20230316 end
		//20200716 add for message table
		this.m_Msg = new MessageMappingTable();
		//----
		pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
		MDC.put("WSNO", this.brws.substring(3));
		MDC.put("PID", pid);
		responseTimeout = PrnSvr.setResponseTimeout;
		/*
		//20210324 MatsudairaSyume initialize sequence no. from 0 at first time build
        try {
            // 20210626 MatsudairaSyuMe check for digit character
	        final String regularExpression = "([\\w\\:\\\\w ./-]+\\w+(\\.)?\\w+)";
            String chkbrws = this.brws.trim();
            Pattern pattern = Pattern.compile(regularExpression);
            if (!pattern.matcher(chkbrws).matches()) {
                chkbrws = "12345678";
                log.error("warning !!! brws name is not digit type can not create seqno file please check dashboard!");
            }
			this.seqNoFile = new File("SEQNO", "SEQNO_" + chkbrws);
			//----
			// 20210217 MatsydairaSyuMe
			log.debug("seqNoFile local=" + this.seqNoFile.getAbsolutePath());
			if (seqNoFile.exists() == false) {
				File parent = seqNoFile.getParentFile();
				if (parent.exists() == false) {
					parent.mkdirs();
				}
				this.seqNoFile.createNewFile();
				FileUtils.writeStringToFile(this.seqNoFile, "0", Charset.defaultCharset());
			}
        } catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			// 20210204,20210428 MatsudairaSyuMe Log Forging
//			final String logStr = String.format("error!!! create or open seqno file SEQNO_%s error %s", this.brws, e.getMessage());
			log.error("error!!! create or open seqno file SEQNO_ error");
		}
		//----20210324
		*/
		//20201115
//		amlog = PrnSvr.amlog;
		//20211115 MatsudairasyuMe auto rolling start by Date
		//20201214
//		this.amlog = LogUtil.getDailyLogger(PrnSvr.logPath, this.clientId + "AM" + this.brws.substring(3) + byDate, "info", "[%d{yyyy/MM/dd HH:mm:ss:SSS}]%msg%n");
		this.amlog = LogUtil.getDailyLogger(PrnSvr.logPath, this.clientId + "AM" + this.brws.substring(3), "info", "[%d{yyyy/MM/dd HH:mm:ss:SSS}]%msg%n");
		//----
		//20211115 MatsudairasyuMe auto rolling start by Date
		//20201214
//		this.aslog = LogUtil.getDailyLogger(PrnSvr.logPath, this.clientId + "AS" + this.brws.substring(3) + byDate, "info", "TIME     [0000]:%d{yyyy.MM.dd HH:mm:ss:SSS} %msg%n");
		this.aslog = LogUtil.getDailyLogger(PrnSvr.logPath, this.clientId + "AS" + this.brws.substring(3), "info", "TIME     [0000]:%d{yyyy.MM.dd HH:mm:ss:SSS} %msg%n");
//		atlog = PrnSvr.atlog;
		//20211115 MatsudairasyuMe auto rolling start by Date
		//20201214
//		this.atlog = LogUtil.getDailyLogger(PrnSvr.logPath, this.clientId + "AT" + this.brws.substring(3) + byDate, "info", "[TID:%X{PID} %d{yyyy/MM/dd HH:mm:ss:SSS}]:[%X{WSNO}]:[%thread]:[%class{0} %M|%L]:%msg%n");
//20230704 MatsudairaSyuMe use ConsoleAppender		this.atlog = LogUtil.getDailyLogger(PrnSvr.logPath, this.clientId + "AT" + this.brws.substring(3), "info", "[TID:%X{PID} %d{yyyy/MM/dd HH:mm:ss:SSS}]:[%X{WSNO}]:[%thread]:[%class{0} %M|%L]:%msg%n");
		this.atlog = LoggerFactory.getLogger(ch.qos.logback.core.ConsoleAppender.class);
		atlog.info("=============[Start]=============");
		atlog.info("------MainThreadId={}------", pid);
		atlog.info("------Call MaintainLog OK------");

		if (this.type.equals("AUTO28")) {
			atlog.info("load Auto Printer type AUTO28");
			this.typeid = Constants.DEVAUTO28;
		} else if (this.type.equals("AUTO20")) {
			atlog.info("load Auto Printer type AUTO20");
			this.typeid = Constants.DEVAUTO28;
		} else if (this.type.equals("AUTO46")) {
			this.prt = new CS4625Impl(this, this.brws, this.type, this.autoturnpage);
			atlog.info("load Auto Printer type AUTO46");
			this.typeid = Constants.DEVAUTO46;
		} else if (this.type.equals("AUTO52")) {
			this.prt = new CS5240Impl(this, this.brws, this.type, this.autoturnpage);
			atlog.info("load Auto Printer type AUTO52");
			this.typeid = Constants.DEVAUTO52;
		} else {
			atlog.info("Auto Printer type define error!");
			return;
		}
		// 20210714 MatsudairaSyuMe Log Forging
		//String s1 = StrUtil.convertValidLog(this.brws.substring(0, 3));
		//String s2 = StrUtil.convertValidLog(this.brws.substring(3));
		log.info("PrtCli=======[Start]=============");  //20220525 MatsudairaSyuMe mark pid
		this.statusfields = PrnSvr.statustbfields;
		
		ipAddrPars nodePars = new ipAddrPars();
		nodePars.init();
		try {
			nodePars.CheckAddrT((String) map.get("ip"), "=", false);
			nodePars.list();
			if (nodePars.getCurrentParseResult()) {
				this.rmtaddr = nodePars.getCurrentRemoteNodeAddress();
				if (nodePars.getCurrentNodeType()) {
					Iterator<InetSocketAddress> iterator = new ArrayList<InetSocketAddress>(
							nodePars.getCurrentLocalNodeAddressMap().values()).iterator();
					if (iterator.hasNext()) {
						this.localaddr = iterator.next();
					}
				}
			}
			//20210427 MatsudairaSyuMe
			this.remoteHostAddr = nodePars.getCurrentRemoteHostAddress();
			this.localHostAddr = nodePars.getCurrentLocalHostAddress();
			/*** 20220525 MatsudaiarSyuMe mark for not delete status data */
			if (PrnSvr.dburl != null && PrnSvr.dburl.trim().length() > 0) {
				jsel2ins = new GwDao(PrnSvr.dburl, PrnSvr.dbuser, PrnSvr.dbpass, false);
				//20201119 add for make reset the status table
				jsel2ins.DELETETB(PrnSvr.statustbname, "BRWS",this.brws);
				//20210204,20210428 MatsudairaSyuMe Log Forging
				//final String logStr2 = String.format("reset %s on %s", this.brws, PrnSvr.statustbname);
				log.info("reset brws on status table");
				//----
			}
			//20210112 MatsudairaSyume
			jsel2ins.CloseConnect();
			jsel2ins = null;
			/**/ //----
		} catch (Exception e) {
			log.error("Address format error!!! {}", e.getMessage());
		}
		//20210630 MatsudairaSyuMe for Path Manipulation, 20210716 Often Misused: Authentication
        //String[] saddr = this.rmtaddr.getAddress().getHostAddress().split("\\.");
		String result = cnvIPv4Addr2Str(this.remoteHostAddr,this.rmtaddr.getPort());;
		log.debug("==>remote seqno result=[{}]", result);
		//20210324 MatsudairaSyume initialize sequence no. from 0 at first time build
		try {
			//20210723 MatsudairaSyuMe Manipulation
			this.seqNoFile = new File("SEQNO", StrUtil.cleanString("SEQNO" + result));
			//----
			log.debug("seqNoFile local=" + this.seqNoFile.getAbsolutePath());
			if (seqNoFile.exists() == false) {
				File parent = seqNoFile.getParentFile();
				if (parent.exists() == false) {
					parent.mkdirs();
				}
				this.seqNoFile.createNewFile();
				FileUtils.writeStringToFile(this.seqNoFile, "0", Charset.defaultCharset());
				this.setSeqNo = 0;  //202207089 MatsudairasyuMe
			}
		} catch (IOException e) {
			e.printStackTrace();
			log.error("error!!! create or open seqno file SEQNO_ error");
		}
		//----20210324
//		log.info("rmt addr {} port {} local addr {} port {}",this.rmtaddr.getAddress().getHostAddress(), this.rmtaddr.getPort(),this.localaddr.getAddress().getHostAddress(), this.localaddr.getPort());
//20200910 change to used new UPSERT
//		String updValue = String.format(updValueptrn,this.brws, this.rmtaddr.getAddress().getHostAddress(),
//				this.rmtaddr.getPort(),this.localaddr.getAddress().getHostAddress(), this.localaddr.getPort(), this.typeid, Constants.STSUSEDINACT);
		//20210827 MatsudairaSyuMe set to current mode to on init Constants.STSNOTUSED
		String updValue = String.format(updValueptrn,this.remoteHostAddr,//20210427 MatsudairaSyuMe Often Misused: Authentication
				this.rmtaddr.getPort(),this.localHostAddr, this.localaddr.getPort(), this.typeid, Constants.STSNOTUSED);
		//----
		try {
			if (jsel2ins == null) {
				jsel2ins = new GwDao(PrnSvr.dburl, PrnSvr.dbuser, PrnSvr.dbpass, false);
				String selfld = "";
				if (PrnSvr.svrtbsdytbfields.indexOf(',') > -1) {
					String[] fldary = PrnSvr.svrtbsdytbfields.split(",");
					selfld = fldary[1];
				} else
					selfld = PrnSvr.svrtbsdytbfields;
				jsel2ins.SELTBSDY_R(PrnSvr.svrtbsdytbname, selfld, PrnSvr.svrtbsdytbmkey, "?", true);  //20220613 MatsudairaSyuMe
				jsel2ins.UPSERT_R(PrnSvr.statustbname, PrnSvr.statustbfields, updValue, PrnSvr.statustbmkey, "'" + this.brws + "'" + "," + PrnSvr.svrid, true);
			}
			int row = jsel2ins.UPSERT(PrnSvr.statustbname, PrnSvr.statustbfields, updValue, PrnSvr.statustbmkey, "'" + this.brws + "'" + "," + PrnSvr.svrid);  //20220525 change this.brws to "'" + this.brws + "'"
//20230427			int row = jsel2ins.UPSERT_R(PrnSvr.statustbname, PrnSvr.statustbfields, updValue, PrnSvr.statustbmkey, "'" + this.brws + "'" + "," + PrnSvr.svrid, false);  //20220613 MatsudairaSyuMe
			//20210827 MatsudairaSyuMe set to current mode to on init Constants.STSNOTUSED
			log.debug("total {} records update  status [{}]", row, Constants.STSNOTUSED);
			//----
			jsel2ins.CloseConnect();
			jsel2ins = null;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.debug("update status table {} error:", PrnSvr.statustbname, e.getMessage());
		}
		PeriodDayEndSchedule();  //20211203 MatsudairasyuMe set day end check log schedule
		//20230314 MatsudairaSyuMe, 20230325 add svrid
		rmAbNomalAlart(PrnSvr.svrid.trim() + "_" + this.brws);
		//----
	}
	//20210217 MatsudairaSyume for brws name check
	boolean isValidBrws (String askbrws) {
		boolean rtn = true;
		Pattern FILTER_PATTERN = Pattern.compile("[0-9]+");
		if (askbrws == null || askbrws.trim().length() < 1 || !FILTER_PATTERN.matcher(this.brws).matches())
			rtn = false;
		return rtn;
	}

	//----
	public void sendBytes(byte[] msg) throws IOException {
		//20210723 MatsudairaSyuMe
		if (msg == null || msg.length <= 0) {
			log.error("input data buffer null");
			return;
		}
		//----
		if (channel_ != null && channel_.isActive()) {
			//20200827 converto to UTF-8 message
//			aslog.info(String.format("SEND %s[%04d]:%s", this.curSockNm, msg.length, new String(msg)));
			// 20210714 MatsudairaSyuMe Log Forging
			String cnvStr = "";
			try {
				cnvStr = charcnv.BIG5bytesUTF8str(msg);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (cnvStr == null || cnvStr.trim().length() == 0)
					cnvStr = new String(msg);
			}
			try {//20210803 MAtsydairaSyuMe change to use ESAPI for Log Forging, 20230314 change to use time stamp parameter
				if (((startIdleMode == true) && ((System.currentTimeMillis() - this.lastCheckTime) >= (PrnSvr.getChgidleTime() * 1000))) || ((startIdleMode == false)))
					aslog.info(String.format("SEND %s[%04d]:%s", this.curSockNm, msg.length, StrUtil.convertValidLog(cnvStr)));
			} catch (Exception e) {
				e.printStackTrace();
				log.error("aslog data format error");
			}
			//----
			cnvStr = null;
			//----
			ByteBuf buf = channel_.alloc().buffer().writeBytes(msg);
			//channel_.writeAndFlush(buf);
			//20230703 MatsudairaSyume make sure for write and flush synchronize mode an
			try {
				channel_.writeAndFlush(buf.retain()).sync();
			} catch (Exception e) {
				e.printStackTrace();
				log.error("Can't send message to Passbook Printer");
			}
			buf.release();
			buf = null;
			//20230703 MatsudairaSyuMe make sure for no direct memory leak
		} else {
			throw new IOException("Can't send message to inactive connection");
		}
	}

	public void close() {
//20210404		try {
			if (channel_ != null) {
				//20210404 MatsudairasyuMe
				////channel_.close().sync();
				channel_.close();
				channel_.closeFuture().syncUninterruptibly();
				group.shutdownGracefully();
				//----
				aslog.info(String.format("DIS  %s[%04d]:", this.curSockNm, 0));
				this.curSockNm = "";
				// 20230522 MasudairaSyuMe register last time read data from pass book print
				this.lastTimeFromPrt = 0l;
				// 20201004
				channel_ = null;
				// 20230522 MasudairaSyuMe register last time read data from pass book print
				this.lastTimeFromPrt = 0l;
				Sleep(3000);  //20230309 slower the reconnect time make sure the passbook printer as already reset
				// 20210415 MatsudairaSyuMe
				if (getCurMode() != EventType.SHUTDOWN && getCurMode() != EventType.RESTART) {
					bootstrap = new Bootstrap();
					group = new NioEventLoopGroup(1);
					bootstrap.group(group);
					bootstrap.channel(NioSocketChannel.class);
					bootstrap.option(ChannelOption.SO_REUSEADDR, true);
					bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
					bootstrap.option(ChannelOption.SO_LINGER, 0);
					bootstrap.option(ChannelOption.SO_REUSEADDR, true);
					bootstrap.option(ChannelOption.TCP_NODELAY, true);
					bootstrap.option(ChannelOption.ALLOW_HALF_CLOSURE, false);
					bootstrap.option(ChannelOption.SO_RCVBUF, bufferSize);
					bootstrap.option(ChannelOption.SO_SNDBUF, bufferSize);
					bootstrap.option(ChannelOption.RCVBUF_ALLOCATOR,
							new AdaptiveRecvByteBufAllocator(32768, 32768, 32768));
					prtcliFSM(firstOpenConn);
					bootstrap.handler(new ChannelInitializer<SocketChannel>() {
						@Override
						public void initChannel(SocketChannel ch) throws Exception {
							ch.pipeline().addLast("log", new LoggingHandler(PrtCli.class, LogLevel.INFO));
//20230314 mark up		ch.pipeline().addLast(new IdleStateHandler(((PrnSvr.getReqTime() > 110) ? (PrnSvr.getReqTime() - 10) : PrnSvr.getReqTime()), 0, 0, TimeUnit.MILLISECONDS)); //20220430 MatsudairaSyuMe 200 change to use getReadIdleTime()
							ch.pipeline().addLast(new IdleStateHandler(((PrnSvr.getReqTime() > 110) ? (PrnSvr.getReqTime() - 10) : PrnSvr.getReqTime()), (PrnSvr.getChgidleTime() * 3 * 1000), 0, TimeUnit.MILLISECONDS)); //20230314 MatsudairaSyuMe add write idle use 3 times of chgidleTime
							//ch.pipeline().addLast(new IdleStateHandler(100, 0, 0, TimeUnit.MILLISECONDS));  //20220425 MatsudairaSyuMe 200 changed to used getReadIdleTime()
							ch.pipeline().addLast(getHandler("PrtCli"));
						}
					});
					// ----
				}
			}
			//----
//20210404		} catch (InterruptedException e) {
//20210404			e.printStackTrace();
//20210404		}
	}

	public boolean connectStatus() {
		if (channel_ != null && channel_.isActive())
			return channel_.isActive();
		else
			return false;
	}

	private void doConnect(int _wait) {
		try {
			//20201004, 2001028 add RESTART test check
			if (getCurMode() != EventType.SHUTDOWN && getCurMode() != EventType.RESTART) {
				// ----
				ChannelFuture f = bootstrap.connect(rmtaddr, localaddr);
				f.addListener(new ChannelFutureListener() {
					@Override
					public void operationComplete(ChannelFuture future) throws Exception {
						if (!future.isSuccess()) {// if is not successful, reconnect
							future.channel().close();
							if (iRetry > MAXDELAY)
								iRetry = MAXDELAY;
							final int _newwait = iRetry * RECONNECT * 100;
							if (curState == SESSIONBREAK && !showStateMsg) {
								amlog.info("[{}][{}][{}]:99補摺機斷線，請檢查線路！", brws, "        ", "            ");
								showStateMsg = true;
								// 20230522 MasudairaSyuMe register last time read data from pass book print
								lastTimeFromPrt = 0l;
							}
							MDC.put("WSNO", brws.substring(3));
							MDC.put("PID", pid);
							atlog.info("Error , please check ... [{}:{}:{}]", rmtaddr.getAddress().toString(),
									rmtaddr.getPort(), localaddr.getPort());
							clientMessageBuf.clear();
//							if (!future.channel().isActive()) {
//							      prtcliFSM(firstOpenConn);
//							}
							Sleep(_newwait);
//							Sleep(10000);
							iRetry += 1;
							/*
							bootstrap = new Bootstrap();
							group = new NioEventLoopGroup(1);
							bootstrap.group(group);
							bootstrap.channel(NioSocketChannel.class);
							bootstrap.option(ChannelOption.SO_REUSEADDR, true);
							bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
							bootstrap.option(ChannelOption.SO_LINGER, 0);
							bootstrap.option(ChannelOption.SO_REUSEADDR, true);
							bootstrap.option(ChannelOption.TCP_NODELAY, true);
							bootstrap.option(ChannelOption.ALLOW_HALF_CLOSURE, false);
							bootstrap.option(ChannelOption.SO_RCVBUF, bufferSize);
							bootstrap.option(ChannelOption.SO_SNDBUF, bufferSize);
							bootstrap.option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(32768, 32768, 32768));
							prtcliFSM(firstOpenConn);
							bootstrap.handler(new ChannelInitializer<SocketChannel>() {
								@Override
								public void initChannel(SocketChannel ch) throws Exception {
									ch.pipeline().addLast("log", new LoggingHandler(PrtCli.class, LogLevel.INFO));
									ch.pipeline().addLast(new IdleStateHandler(200, 0, 0, TimeUnit.MILLISECONDS));
									ch.pipeline().addLast(getHandler("PrtCli"));
								}
							});
							*/
//							log.debug("try reconnect");
							bootstrap.connect(rmtaddr, localaddr).addListener(this);
//							log.debug("reconnect");
						} else {// good, the connection is ok
							showStateMsg = false;
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
								log.debug("addCloseDetectListener {}", iRetry);
								scheduleConnect(_wait);
							}
						});

					}
				});
				// 20201004
			}
			//----
		} catch (Exception ex) {

			scheduleConnect(_wait / 3);

		}
	}

	private void scheduleConnect(int millis) {
		timer_.schedule(new TimerTask() {
			@Override
			public void run() {
				doConnect(millis);
			}
		}, millis);
	}

	public void handleMessage(String msg) {
		log.debug("msg={}", msg);

	}

	public void connectionLost() {
		log.debug("connectionLost()");
		// 20230522 MasudairaSyuMe register last time read data from pass book print
		this.lastTimeFromPrt = 0l;
	}

	public void connectionEstablished() {
		log.debug("connectionEstablished()");
		this.clientMessageBuf.clear();
		// 20230522 MasudairaSyuMe register last time read data from pass book print
		this.lastTimeFromPrt = 0l;
	}

	@Override
	public void run() {
		//20210414
		////bootstrap.group(new NioEventLoopGroup());
		this.group = new NioEventLoopGroup(1);
		bootstrap.group(group);
		//----
		bootstrap.channel(NioSocketChannel.class);
		bootstrap.option(ChannelOption.SO_REUSEADDR, true);
		bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
		bootstrap.option(ChannelOption.SO_LINGER, 0);
		bootstrap.option(ChannelOption.SO_REUSEADDR, true);
		bootstrap.option(ChannelOption.TCP_NODELAY, true);
		bootstrap.option(ChannelOption.ALLOW_HALF_CLOSURE, false);
		bootstrap.option(ChannelOption.SO_RCVBUF, bufferSize);
		bootstrap.option(ChannelOption.SO_SNDBUF, bufferSize);
		bootstrap.option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(32768, 32768, 32768));
		prtcliFSM(firstOpenConn);

		bootstrap.handler(new ChannelInitializer<SocketChannel>() {
			@Override
			public void initChannel(SocketChannel ch) throws Exception {
				ch.pipeline().addLast("log", new LoggingHandler(PrtCli.class, LogLevel.INFO));
//20230314 mark up				ch.pipeline().addLast(new IdleStateHandler(((PrnSvr.getReqTime() > 110) ? (PrnSvr.getReqTime() - 10) : PrnSvr.getReqTime()), 0, 0, TimeUnit.MILLISECONDS)); //20220430 MatsudairaSyuMe 200 change to use getReadIdleTime()
				ch.pipeline().addLast(new IdleStateHandler(((PrnSvr.getReqTime() > 110) ? (PrnSvr.getReqTime() - 10) : PrnSvr.getReqTime()), (PrnSvr.getChgidleTime() * 3 * 1000), 0, TimeUnit.MILLISECONDS)); //20230314 MatsudairaSyuMe add write idle for 3 time of chgidletime
				ch.pipeline().addLast(getHandler("PrtCli"));
			}
		});
		scheduleConnect(3000);

	} // run

	public ChannelHandler getHandler(String _id) {
		clientId = _id;
		return this;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		log.debug(clientId + "channel active");
		this.iRetry = 1;
		this.currentContext = ctx;
		this.clientChannel = this.currentContext.channel();
		publishActiveEvent();
		super.channelActive(ctx);
		//20200719
		prt.getIsShouldShutDown().set(false);
		//----
		MDC.put("WSNO", this.brws.substring(3));
		MDC.put("PID", pid);
		showStateMsg = false;
		//20210217 MatsudairaSyuMe
		SecureRandom secureRandomGenerator = null;
		int tmpsockno = 0;
		try {
			secureRandomGenerator = SecureRandom.getInstance("SHA1PRNG");
			tmpsockno = secureRandomGenerator.nextInt(9999);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error("SecureRandom error:NoSuchAlgorithmException");
		} finally {
			if (tmpsockno == 0)
				tmpsockno = 1;
		}
//		this.curSockNm = String.format("%04d", (int) ((Math.random() * ((9999 - 4) + 1)) + 4));
		//----
		this.curSockNm = String.format("%04d", tmpsockno);
		aslog.info(String.format("CON  %s[%04d]:", this.curSockNm, 0));
//		String updValue = String.format(updValueptrn,this.brws, this.rmtaddr.getAddress().getHostAddress(),
//				this.rmtaddr.getPort(),this.localaddr.getAddress().getHostAddress(), this.localaddr.getPort(), this.typeid, Constants.STSUSEDACT);
//20200910 change to use new UPSERT
		String updValue = String.format(updValueptrn,this.remoteHostAddr,//20210427 MatsudairaSyuMe Often Misused: Authentication
				this.rmtaddr.getPort(),this.localHostAddr, this.localaddr.getPort(), this.typeid, Constants.STSUSEDACT);
		if (jsel2ins == null)
			jsel2ins = new GwDao(PrnSvr.dburl, PrnSvr.dbuser, PrnSvr.dbpass, false);
		int row = jsel2ins.UPSERT(PrnSvr.statustbname, PrnSvr.statustbfields, updValue, PrnSvr.statustbmkey, "'" + this.brws + "'" + "," + PrnSvr.svrid);  //20220525 MatsudairaSyuMe change this.brws to "'" + this.brws + "'"
//20240427		int row = jsel2ins.UPSERT_R(PrnSvr.statustbname, PrnSvr.statustbfields, updValue, PrnSvr.statustbmkey, "'" + this.brws + "'" + "," + PrnSvr.svrid, false);//20220613 MatsudairaSyuMe
//----
		log.debug("total {} records update status [{}]", row, Constants.STSUSEDACT);
		jsel2ins.CloseConnect();  //20230427
		jsel2ins =  null;
		// 20230522 MasudairaSyuMe register last time read data from pass book print
		this.lastTimeFromPrt = 0l;
		prtcliFSM(!firstOpenConn);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		log.debug(clientId + " channelInactive");
		publishInactiveEvent();
		this.clientChannel = null;
		super.channelInactive(ctx);
		prt.getIsShouldShutDown().set(true);
		prt.ClosePrinter();
		aslog.info(String.format("DIS  %s[%04d]:", this.curSockNm, 0));
//20200910 change to use new UPSERT
//		String updValue = String.format(updValueptrn,this.brws, this.rmtaddr.getAddress().getHostAddress(),
//				this.rmtaddr.getPort(),this.localaddr.getAddress().getHostAddress(), this.localaddr.getPort(), this.typeid, Constants.STSUSEDINACT);
		String updValue = String.format(updValueptrn,this.remoteHostAddr,  //20210427 MatsudairaSyuMe Often Misused: Authentication
				this.rmtaddr.getPort(),this.localHostAddr, this.localaddr.getPort(), this.typeid, Constants.STSUSEDINACT);
		if (jsel2ins == null)
			jsel2ins = new GwDao(PrnSvr.dburl, PrnSvr.dbuser, PrnSvr.dbpass, false);
		    int row = jsel2ins.UPSERT(PrnSvr.statustbname, PrnSvr.statustbfields, updValue, PrnSvr.statustbmkey, "'" + this.brws + "'" + "," + PrnSvr.svrid); //20220525 MatsudairasyuMe change this.brws to "'" + this.brws + "'"
//20230427		int row = jsel2ins.UPSERT_R(PrnSvr.statustbname, PrnSvr.statustbfields, updValue, PrnSvr.statustbmkey, "'" + this.brws + "'" + "," + PrnSvr.svrid, false); //20220613 MatsudairasyuMe
		log.debug("total {} records update  status [{}]", row, Constants.STSUSEDINACT);
		jsel2ins.CloseConnect(); //20230427
		jsel2ins = null;
		this.curSockNm = "";
		this.clientMessageBuf.clear();
		// 20230522 MasudairaSyuMe register last time read data from pass book print
		this.lastTimeFromPrt = 0l;
		prtcliFSM(firstOpenConn);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		log.debug(clientId + " channelRead");
		try {
			this.lastTimeFromPrt = System.currentTimeMillis();// 20230522 MasudairaSyuMe register last time read data from pass book printer
			if (msg instanceof ByteBuf) {
				ByteBuf buf = (ByteBuf) msg;
				if (buf.isReadable() && !buf.hasArray()) {
					// it is long raw telegram
					log.debug("readableBytes={} barray={}", buf.readableBytes(), buf.hasArray());
					byte[] asary = new byte[buf.readableBytes()];
					ByteBuf dup = buf.duplicate();
					dup.readBytes(asary);
					//20220429 MatsuairaSyuMe mark up log
					if (startIdleMode == true) {
						if ((System.currentTimeMillis() - this.lastCheckTime) > (PrnSvr.getChgidleTime() * 1000)) { //20230314 change to use parameter curTime
							aslog.info(String.format("RECV %s[%04d]:%s", this.curSockNm, buf.readableBytes(), new String(asary)));
							this.lastCheckTime = System.currentTimeMillis();
						}
					} else
						aslog.info(String.format("RECV %s[%04d]:%s", this.curSockNm, buf.readableBytes(), new String(asary)));
					//----
					if (clientMessageBuf.readerIndex() > (clientMessageBuf.capacity() / 2)) {
						clientMessageBuf.discardReadBytes();
						log.debug("adjustment clientMessageBuf readerindex ={}" + clientMessageBuf.readableBytes());
					}
					synchronized (this.readMutex) {
						clientMessageBuf.writeBytes(buf);
						prtcliFSM(!firstOpenConn);
					}
					log.debug("readableBytes={} barray={}", buf.readableBytes(), buf.hasArray());
				}
			} else // if
				log.warn("not ByteBuf");
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.toString());
		} finally {
			ReferenceCountUtil.release(msg);
			// 若是有配置等待鎖，則解鎖
			if (readLatch != null) {
				readLatch.countDown();
			}
		}
	}

	/**
	 * it's depends also on ChannelOption.MAX_MESSAGES_PER_READ which is 16 by
	 * default 當每一部份的訊息被讀取後會被呼叫 例如 buffer 中有 32 bytes，此功能會被呼叫 2 次
	 * .option(ChannelOption.MAX_MESSAGES_PER_READ, Integer.MAX_VALUE)
	 * 
	 * @throws Exception
	 */
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		log.debug(clientId + " channelReadComplete");
		super.channelReadComplete(ctx);
	}

	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		log.debug(clientId + " channelRegister");
		prt.getIsShouldShutDown().set(false);  //20200719
		// 20230522 MasudairaSyuMe register last time read data from pass book print
		this.lastTimeFromPrt = 0l;
		super.channelRegistered(ctx);
	}

	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		log.debug(clientId + " channelUnregistered");
		//20200910 change to use new UPSERT
//		String updValue = String.format(updValueptrn,this.brws, this.rmtaddr.getAddress().getHostAddress(),
//				this.rmtaddr.getPort(),this.localaddr.getAddress().getHostAddress(), this.localaddr.getPort(), this.typeid, Constants.STSUSEDINACT);
		//20210826 MatsudairaSyuMe if current mode CurMode SHUTDOWN/RESTART device stat set to Constants.STSNOTUSED or Constants.STSUSEDINACT
		String updValue = String.format(updValueptrn,this.remoteHostAddr, //20210427 MatsudairaSyuMe Often Misused: Authentication
				this.rmtaddr.getPort(),this.localHostAddr, this.localaddr.getPort(), this.typeid, (getCurMode() == EventType.SHUTDOWN || getCurMode() == EventType.RESTART) ? Constants.STSNOTUSED : Constants.STSUSEDINACT );
		//----
		try {
			if (jsel2ins == null)
				jsel2ins = new GwDao(PrnSvr.dburl, PrnSvr.dbuser, PrnSvr.dbpass, false);
			int row = jsel2ins.UPSERT(PrnSvr.statustbname, PrnSvr.statustbfields, updValue, PrnSvr.statustbmkey, "'" + this.brws + "'" + "," + PrnSvr.svrid); //20220525 MatsudairaSyuMe change this.brws to "'" + this.brws + "'"
//20240327			int row = jsel2ins.UPSERT_R(PrnSvr.statustbname, PrnSvr.statustbfields, updValue, PrnSvr.statustbmkey, "'" + this.brws + "'" + "," + PrnSvr.svrid, false); //20220613 MatsudairaSyuMe
			//20210827 MatsudairaSyuMe if current mode CurMode SHUTDOWN/RESTART device stat set to Constants.STSNOTUSED otherwise Constants.STSUSEDINACT
			log.debug("total {} records update  status [{}]", row, (getCurMode() == EventType.SHUTDOWN || getCurMode() == EventType.RESTART) ? Constants.STSNOTUSED : Constants.STSUSEDINACT );
			//----
			jsel2ins.CloseConnect();
			jsel2ins = null;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.debug("update status table {} error:", PrnSvr.statustbname, e.getMessage());
		}
		// 20230522 MasudairaSyuMe register last time read data from pass book print
		this.lastTimeFromPrt = 0l;
		ctx.close();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		log.debug(clientId + " exceptionCaught=" + cause.getMessage());
		// 20230522 MasudairaSyuMe register last time read data from pass book print
		this.lastTimeFromPrt = 0l;
		if (cause instanceof ConnectException) {
			publishInactiveEvent();
			ctx.close();
			prt.getIsShouldShutDown().set(true);
			prt.ClosePrinter();
			this.clientMessageBuf.clear();
			aslog.info(String.format("ERR  %s[%04d]:", this.curSockNm, 0));
		}
		//20210112 MatshdairaSyuMe
		else if (this.curState == RECVTLM && this.isTITA_TOTA_START() == true && this.alreadySendTelegram == true) {
			log.warn("already send telegram and wait to receive telegram or received telegram error !!!!!!!!");
		}
		//----
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		log.debug(clientId + " userEventTriggered=" + evt.toString());
		if (evt instanceof IdleStateEvent) {
			if (clientChannel.pipeline().get(idleStateHandlerName) != null) {
				log.debug("unload idle state handler");
				clientChannel.pipeline().remove(idleStateHandlerName);
			}
			//20211126 MatsudairaSyuMe check system date end
//			if (!new SimpleDateFormat("yyyyMMdd").format(new Date()).trim().equals(this.getByDate())) {
//				//20211126 date changed update am log
//				if (this.amlog != null)
//					amlog.info("[{}][{}][{}]:                            ", brws, "        ", "            ");
//				this.setByDate(new SimpleDateFormat("yyyyMMdd").format(new Date()).trim());
//				log.info("system date changed");
//			}
			//----
			IdleStateEvent e = (IdleStateEvent) evt;
			if (e.state() == IdleState.READER_IDLE) {
				//20230314 MattaudairaSyuMe registered the read time stamp
				log.debug(clientId + " READER_IDLE");
				//20220429 MatsudairaSyuMe ====
				if ((this.curState == CAPTUREPASSBOOK) && (this.iFirst == 0) && (PrnSvr.getChgidleTime() > 0)) {
					//20230314 change to use curTime
					if (((System.currentTimeMillis() - this.lastCheckTime) >= (PrnSvr.getChgidleTime() * 1000)) && (startIdleMode == false))
						startIdleMode = true;
				} else
					startIdleMode = false;
				prtcliFSM(!firstOpenConn);

			} else if (e.state() == IdleState.WRITER_IDLE) {
				//20230314 MatsudairaSyuMe writer idle happened !!!,
				//it is too long that no data had been send to pass book printer
				log.debug(clientId + " WRITER_IDLE");
				prtAbNomalAlart(PrnSvr.svrid.trim() + "_" + this.brws); //20230314 create or update pass book printer alart file, 20230325 add svrid
			} else if (e.state() == IdleState.ALL_IDLE) {
				log.debug(clientId + " ALL_IDLE");
			}
		}
	}

	public synchronized void addActorStatusListener(ActorStatusListener listener) {
		log.debug(clientId + " actor status listener add");
		actorStatusListeners.add(listener);
	}

	public synchronized void removeActorStatusListener(ActorStatusListener listener) {
		log.debug(clientId + " actor status listener remove");
		actorStatusListeners.remove(listener);
	}

	public void publishShutdownEvent() {
		log.debug(clientId + " publish shutdown event to listener");
		log.debug("-publish end-");
	}

	public void publishActiveEvent() {
		log.debug(clientId + " publish active event to listener");
		this.isConnected.set(true);
		log.debug("-publish end-");
	}

	public void publishInactiveEvent() {
		log.debug(clientId + " publish Inactive event to listener");
		this.isConnected.set(false);
		log.debug("-publish end-");
	}

	public void publishactorSendmessage(String actorId, Object eventObj) {
		log.debug(actorId + " publish message to listener");

		log.debug("-publish end-");
	}

	// end for ChannelDuplexHandler function
    //sleep function using milliseconds unit as parameter 
	private void Sleep(int s) {
		try {
			TimeUnit.MILLISECONDS.sleep(s);
		} catch (InterruptedException e1) { // TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	private boolean SetSignal(boolean firstSet, boolean sendReqFirst, String lightstr, String blinkstr) {
		byte l1 = (byte) 0x0, l2 = (byte) 0x0, b1 = (byte) 0x0, b2 = (byte) 0x0;
		char[] light = lightstr.toCharArray();
		char[] blink = blinkstr.toCharArray();
		boolean rtn = false;
		if (this.type.equals("AUTO28") || this.type.equals("AUTO20")) {
			// 顯示燈號
			if (light[1] == '1')
				l1 |= L0;
			if (light[2] == '1')
				b1 |= L0;
			if (light[3] == '1')
				l1 |= L1;
			if (light[4] == '1')
				l1 |= L2;
			if (light[5] == '1') {
				b1 |= L0;
				b1 |= L2;
			}
			if (light[6] == '1')
				l1 |= L3;
			if (light[7] == '1')
				l1 |= L5;
			if (light[8] == '1')
				l1 |= L6;
			if (light[9] == '1')
				l1 |= L4;
			// 閃爍燈號
			if (blink[1] == '1')
				b1 |= L0;
			if (blink[2] == '1')
				b1 |= L0;
			if (blink[3] == '1')
				b1 |= L1;
			if (blink[4] == '1')
				b1 |= L2;
			if (blink[5] == '1') {
				b1 |= L0;
				b1 |= L2;
			}
			if (blink[6] == '1')
				b1 |= L3;
			if (blink[7] == '1')
				b1 |= L5;
			if (blink[8] == '1')
				b1 |= L6;
			if (blink[9] == '1')
				b1 |= L4;
			rtn = prt.SetSignal(firstSet, sendReqFirst, l1, (byte) 0x0, b1, (byte) 0x0);
		} else if (this.type.equals("AUTO46") || this.type.equals("AUTO52")) {
			// 顯示燈號
			if (light[0] == '1')
				l1 |= I1;
			if (light[1] == '1')
				l2 |= I1;
			if (light[2] == '1')
				l2 |= I2;
			if (light[3] == '1')
				l2 |= I3;
			if (light[4] == '1')
				l2 |= I4;
			if (light[5] == '1')
				l2 |= I5;
			if (light[6] == '1')
				l2 |= I6;
			if (light[7] == '1')
				l2 |= I7;
			if (light[8] == '1')
				l2 |= I8;
			if (light[9] == '1')
				l2 |= I6;
			// 閃爍燈號
			if (blink[0] == '1')
				b1 |= I1;
			if (blink[1] == '1')
				b2 |= I1;
			if (blink[2] == '1')
				b2 |= I2;
			if (blink[3] == '1')
				b2 |= I3;
			if (blink[4] == '1')
				b2 |= I4;
			if (blink[5] == '1')
				b2 |= I5;
			if (blink[6] == '1')
				b2 |= I6;
			if (blink[7] == '1')
				b2 |= I7;
			if (blink[8] == '1')
				b2 |= I8;
			if (blink[9] == '1')
				b2 |= I6;
			rtn = prt.SetSignal(firstSet, sendReqFirst, l1, l2, b1, b2);
		}
		return rtn;
	}

	private boolean chk_Account(byte[] cussrc) {
		boolean rtn = false;
		short[] pair = { 6, 5, 4, 3, 2, 7, 6, 5, 4, 3, 2, -1 };
		short sum = 0, chkdg;
		for (int i = 0; i < (TXP.ACTNO_LEN - 1); i++)
			sum += (((short) cussrc[i] - 48) * pair[i]);
		chkdg = (short) (9 - (sum % 9));
		if (chkdg == (short) cussrc[TXP.ACTNO_LEN - 1] - 48)
			rtn = true;
		else {
			atlog.info("actno[{}] chkdg[{}] error!", new String(cussrc), chkdg);
		}
		return rtn;
	}

	//20200724
/*	private byte[] DataINQ(int iVal, int ifig, String dCount, String con) {
		return DataINQ(iVal, ifig, dCount, con, null);
	}
*/
	private byte[] DataINQ(int iVal, int ifig, String dCount) {
		return DataINQ(iVal, ifig, dCount, null);
	}
	//----

	private void setpasname(byte[] cussrc) {
		String chkcatagory = new String(cussrc, 3, 3);
		String chkcusid = "";
		boolean negiative = false;
		boolean allzero = true;
		int i = 0, j = 0;
		switch (chkcatagory) {
		// 台幣存摺
			case "001":
			case "002":
			case "003":
			case "004":
			case "005":
			case "006":
			case "008":
				pasname = "台幣存摺";
				//20230220 MatsudairaSyuMe check pb msr balance if <0000000000000 change to +0000000000000
				chkcusid = new String(cussrc, TXP.ACTNO_LEN + TXP.ACFILLER_LEN, TXP.MSRBAL_LEN);
				negiative = false;
				allzero = true;
				i = 0;
				j = 0;
				for ( char c : chkcusid.toCharArray()) {
					if (c == '-' || c == '<') {
						negiative = true;
						j = i;
					} else	if (c != '0') {
						allzero = false;
						break;
					}
					i += 1;
				}
				if (negiative && allzero) // balance is zero but sign is minus
					cusid[TXP.ACTNO_LEN + TXP.ACFILLER_LEN + j] = (byte)'0';
				log.info("WOW !!!! after check pb msr neiactive and all zero [{}] [{}] new cusid[{}] !!!", negiative, allzero, new String(cusid));
				//---- end 20230220
				break;
				// 外幣存摺
			case "007":
			case "021":
			case "701":
			case "702":
			case "703":
				pasname = "外幣存摺";
				break;
				// 黃金存摺
			case "071":
			case "072":
				pasname = "黃金存摺";
				break;
			default:
				pasname = "        ";
				break;
		}
		return;
	}
	private boolean MS_Check(byte[] cussrc) {
		boolean rtn = true;
		//20230323 MatsudairaSyuMe
		try {
		//----20230323
		//20200611
		if (iEnd == 0)
			this.account = new String(cussrc, 0, TXP.ACTNO_LEN);
		//----
		if (!chk_Account(cussrc)) {
			rtn = false;
			amlog.info("[{}][{}][{}]:13存摺帳號錯誤！", brws, "        ", this.account);
			
			SetSignal(firstOpenConn, !firstOpenConn, "0000000000", "0000000001");
			return rtn;
		}
		log.debug("[{}]", new String(cussrc));
		/*************** check MSR's apno ***************/
		if (iEnd == 1) {
			//20200611, 20220914 MatsudairaSyuMe add differential message
			if (!new String(cussrc, 0, TXP.ACTNO_LEN).equals(this.account)) {
				amlog.info("[{}][{}][{}]:13存摺帳號錯誤！翻頁後應置入帳號 [{}]", brws, pasname, new String(cussrc, 0, TXP.ACTNO_LEN), this.account);
				rtn = false;
				return rtn;
			}
		}
		/*************** check MSR's apno ***************/
		this.catagory = account.substring(3, 6);
		this.cpage = "";
		this.cline = "";
		switch (catagory) {
		// 台幣存摺
		case "001":
		case "002":
		case "003":
		case "004":
		case "005":
		case "006":
		case "008":
			this.actfiller = new String(cussrc, TXP.ACTNO_LEN, TXP.ACFILLER_LEN); // !< 帳號保留 MSR for PB/FC len 4
			this.msrbal = new String(cussrc, TXP.ACTNO_LEN + TXP.ACFILLER_LEN, TXP.MSRBAL_LEN); // !< 磁條餘額 MSR for PB/FC len 14, GL len 12, PB 13 + 1正負號 
			//20200709 add for atlog
			String atlogmsrbal = new String(cussrc, TXP.ACTNO_LEN + TXP.ACFILLER_LEN + 1, TXP.MSRBAL_LEN - 1); // !< 磁條餘額 MSR for PB/FC len 14, GL len 12, PB 13 + 1正負號 
			this.cline = new String(cussrc, TXP.ACTNO_LEN + TXP.ACFILLER_LEN + TXP.MSRBAL_LEN, TXP.LINE_LEN); // !< 行次 MSR for PB/FC/GL len 2
			this.cpage = new String(cussrc, TXP.ACTNO_LEN + TXP.ACFILLER_LEN + TXP.MSRBAL_LEN + TXP.LINE_LEN, TXP.PAGE_LEN); // !< 頁次 MSR for PB/FC/GL len 2
			this.bkseq = new String(cussrc,
					TXP.ACTNO_LEN + TXP.ACFILLER_LEN + TXP.MSRBAL_LEN + TXP.LINE_LEN + TXP.PAGE_LEN, TXP.BKSEQ_LEN); // !< 領用序號 MSR for PB len 1, FC len 2
			// 20200709 change to use atlogmsrbal for atlog
			atlog.info("台幣存摺 PB_MSR [{}]/[{}]/[{}]/[{}]/[{}]/[{}]", account, actfiller, atlogmsrbal, cline, cpage, bkseq);
			iFig = TXP.PBTYPE;
			break;
		// 外幣存摺
		case "007":
		case "021":
		case "701":
		case "702":
		case "703":
			this.actfiller = new String(cussrc, TXP.ACTNO_LEN, TXP.ACFILLER_LEN); // !< 帳號保留 MSR for PB/FC len 4
			this.msrbal = new String(cussrc, TXP.ACTNO_LEN + TXP.ACFILLER_LEN, TXP.MSRBAL_LEN); // !< 磁條餘額 MSR for PB/FC len 14, GL len 12
			this.cline = new String(cussrc, TXP.ACTNO_LEN + TXP.ACFILLER_LEN + TXP.MSRBAL_LEN, TXP.LINE_LEN); // !< 行次 MSR for PB/FC/GL len 2
			this.cpage = new String(cussrc, TXP.ACTNO_LEN + TXP.ACFILLER_LEN + TXP.MSRBAL_LEN + TXP.LINE_LEN,
					TXP.PAGE_LEN); // !< 頁次 MSR for PB/FC/GL len 2
			this.pbver = new String(cussrc,
					TXP.ACTNO_LEN + TXP.ACFILLER_LEN + TXP.MSRBAL_LEN + TXP.LINE_LEN + TXP.PAGE_LEN, TXP.PBVER_LEN); // !< 領用序號 MSR for PB len 1, FC len 2
			atlog.info("外幣存摺 FC_MSR [{}]/[{}]/[{}]/[{}]/[{}]/[{}]", account, actfiller, msrbal, cline, cpage, pbver);
			iFig = TXP.FCTYPE;
			break;
		// 黃金存摺
		case "071":
		case "072":
			this.msrbal = new String(cussrc, TXP.ACTNO_LEN, TXP.MSRBALGL_LEN); // !< 磁條餘額 MSR for PB/FC len 14, GL len 12
			this.cline = new String(cussrc, TXP.ACTNO_LEN + TXP.MSRBALGL_LEN, TXP.LINE_LEN); // !< 行次 MSR for PB/FC/GL len 2
			this.cpage = new String(cussrc, TXP.ACTNO_LEN + TXP.MSRBALGL_LEN + TXP.LINE_LEN, TXP.PAGE_LEN); // !< 頁次 MSR for PB/FC/GL len 2
			this.no = new String(cussrc, TXP.ACTNO_LEN + TXP.MSRBALGL_LEN + TXP.LINE_LEN + TXP.PAGE_LEN, TXP.NO_LEN); // !< 存摺號碼 MSR for GL len 9
			atlog.info("黃金存摺 GL_MSR [{}]/[{}]/[{}]/[{}]/[{}]", account, msrbal, cline, cpage, no);
			iFig = TXP.GLTYPE;
			break;
		default:
			amlog.info("[{}][{}][{}]:13存摺帳號錯誤！[{}](非台幣/外幣/黃金存摺)", brws, pasname, this.account);
			atlog.info("ERROR！！ PB_MSR [{}]/[{}]/[{}]/[{}]/[{}]/[{}]", account, "", "", cline, cpage, bkseq);
			iFig = 0;
			rtn = false;
			break;
		}
		if (iFig == 0)
			return rtn;
		this.nline = Integer.parseInt(this.cline);
		this.npage = Integer.parseInt(this.cpage);
		} catch (Exception e) {
			e.printStackTrace();
			amlog.info("[{}][{}][{}]:13存摺格式錯誤！", brws, "        ", this.account);
			atlog.info("MSR format ERROR！！[{}]", account);
			iFig = 0;
			rtn = false;
		}
		//----20230323
		return rtn;
	}

	/*********************************************************
	*  PbDataFormat() : Format TOTA Text to print            *
	*  function       : 列印台幣存摺資料格式                 *
	*  parameter 1    : tx_area data                         *
	*  parameter 2    : total NB count                       *
	*  return_code    : BOOL - TRUE                          *
	*                          FALSE               2008.01.24*
	*********************************************************/

	private boolean PbDataFormat() {
		boolean rtn = true;
		int tl,total;
		tl = this.iLine;
		//20210419 MatsudairaSyuMe total change to this.iCon
		//total = Integer.parseInt(this.dCount);
		total = this.iCon;
		String pbpr_date = String.format("%9s", " ");    //日期 9
		String pbpr_wsno = String.format("%7s", " ");    //櫃檯機編號 7
		String pbpr_crdblog = String.format("%36s", " ");   //摘要+支出收入金額 36
		String pbpr_crdb = String.format("%36s", " ");   //摘要+支出收入金額 36
		String pbpr_crdbT = String.format("%36s", " ");   //摘要+支出收入金額 36
		String pbpr_dscpt = String.format("%16s", " ");  //摘要 16 byte big
		String pbpr_balance = String.format("%18s", " ");//結存 18
		String pr_datalog = ""; //  80
		String pr_data = ""; //  80

		if (this.curState == STARTPROCTLM) {
			p0080DataFormat = new P0080TEXT();
			this.curState = PBDATAFORMAT;
		}
		log.debug("1--->p0080text=>{} {}", this.curState);
		try {
			//PB 日期(1+8)/空格(1)/櫃檯機編號(7)/摘要(16)/支出收入金額(20)/結存(18)/
			
			for (int i = 0; i < pb_arr.size(); i++) {
				//處理日期格式
				//20201216 add one space
				//20210420 MatsudairaSymMe reduse one space
//				pr_data = " ";
				pr_data = "";
				pbpr_date = new String (p0080DataFormat.getTotaTextValueSrc("date", pb_arr.get(i))).trim();
				if (Integer.parseInt(pbpr_date) > 1000000)
					pbpr_date = String.format("%9s", Integer.parseInt(pbpr_date));  //20200731 adjust local's Date format
				else {
					pbpr_date  = " " + pbpr_date.substring(0, 2) + "." + pbpr_date.substring(2, 4) + "." + pbpr_date.substring(4, 6);
				}
				pr_data = pr_data + pbpr_date;
				//處理櫃檯機編號
				pbpr_wsno = String.format("%5s%2s",new String (p0080DataFormat.getTotaTextValueSrc("trmno", pb_arr.get(i))).trim()
				,new String (p0080DataFormat.getTotaTextValueSrc("tlrno", pb_arr.get(i))).trim());
				pr_data = pr_data + " " + pbpr_wsno;
				//處理摘要
				byte dtype[] = p0080DataFormat.getTotaTextValueSrc("dsptype", pb_arr.get(i));
				byte[] dsptb = null;
				byte[] dsptbsnd = new byte[24];  //20220701 MatsudairaSyuMe
				if (dtype[0] == (byte)'9') {
					dsptb = p0080DataFormat.getTotaTextValueSrc("dsptext", pb_arr.get(i));
					dsptb = FilterBig5(dsptb);
				} else {
					String desc = new String(p0080DataFormat.getTotaTextValueSrc("dscpt", pb_arr.get(i))).trim();
					//20230316 MatsudairaSyuMe read Dscpt from table TB_AUDM
					byte[] tmpdsp = null;
					if (jsel2ins == null)
						jsel2ins = new GwDao(PrnSvr.dburl, PrnSvr.dbuser, PrnSvr.dbpass, false);
					tmpdsp = jsel2ins.SELONEFLDBary(PrnSvr.dmtbname, "NAME", PrnSvr.dmtbsearkey, "'" + desc + "'" , false);
					if (tmpdsp == null)
						dsptb = desc.getBytes();
					else {
						int s = 0;
						for (s = 0; s < tmpdsp.length; s++)
							if (tmpdsp[s] == (byte)0x0)
								break;
							else
								s++;
						if (s < 1)
							dsptb = desc.getBytes();
						else {
							dsptb = new byte[s];
							System.arraycopy(tmpdsp, 0, dsptb, 0, s);
						}
						tmpdsp = null;
					}
					/*20230316 mark up
					if (DscptMappingTable.m_Dscpt2.containsKey(desc))
////						dsptb = DscptMappingTable.m_Dscpt.get(desc).getBytes();
						dsptb = DscptMappingTable.m_Dscpt2.get(desc);
					else
						dsptb = desc.getBytes();
					*/
					//----20230316 end
				}
				//20220701 MatsudairaSyuMe
				//dsptbsnd = dsptb;
				Arrays.fill(dsptbsnd, (byte)' ');
				System.arraycopy(dsptb, 0, dsptbsnd, 0, dsptb.length);
				//---- 20220701
				//20220630 MatsudairaSyuMe dsptblen
				int dsptblen = dsptbsnd.length;
//				pbpr_dscpt = new String(FilterChi(pbpr_dscpt.getBytes()));
				//20100503 by Han 支出摘要第12位或若為中文碼時，轉為空白
				//20100503 by Han 存入摘要第17位或若為中文碼時，轉為空白
				byte[] tmpb1 = null;
////				log.debug("crdb=0 crdb=1 pbpr_dscpt[11]=[{}] dspt[16]=[{}] {} len={}",dsptb[11],dsptb[16], dsptb, dsptb.length);
				byte[] crdb = p0080DataFormat.getTotaTextValueSrc("crdb", pb_arr.get(i));
				if (crdb[0] == (byte)'0') {
					tmpb1 = new byte[24];
					Arrays.fill(tmpb1, (byte)' ');
					System.arraycopy(dsptb, 0, tmpb1, 0, dsptb.length);
					dsptb = tmpb1;
////					dsptb = FilterChi(dsptb, 12);
				}
				if (crdb[0] == (byte)'1') {
					tmpb1 = new byte[34];
////					dsptb = FilterChi(dsptb, 17);
					Arrays.fill(tmpb1, (byte)' ');
					System.arraycopy(dsptb, 0, tmpb1, 0, dsptb.length);
					dsptb = tmpb1;
				}
//				for (int ii = 0; ii < dsptb.length; ii++)
//					System.out.print(String.format("%x", dsptb[ii]));
//				System.out.println();
				log.debug("crdb=0 crdb=1 pbpr_dscpt[11]=[{}] dspt[16]=[{}] {} len={}",String.format("%x",(int)(dsptb[11] & 0xff)),String.format("%x",(int)(dsptb[16] & 0xff)), dsptb, dsptb.length);
				//20200826
				atlog.info("crdb=0 pbpr_dscpt[11]=[{}]", String.format("%x",(int)(dsptb[11] & 0xff)));
				atlog.info("crdb=1 pbpr_dscpt[16]=[{}]", String.format("%x",(int)(dsptb[16] & 0xff)));
				//----
				if (crdb[0] == (byte)'0') {
					pbpr_crdb = String.format("%12s", new String(dsptb, "BIG5"));
//					pbpr_crdblog = String.format("%12s", new String(dsptb));
					pbpr_crdblog = String.format("%12s", new String(dsptb, "BIG5"));
					dsptblen = 12;  //20220630 MatsudairaSyuMe dsptblen
				} else {
					pbpr_crdb = String.format("%17s", new String(dsptb, "BIG5"));
//					pbpr_crdblog = String.format("%17s", new String(dsptb));
					pbpr_crdblog = String.format("%17s", new String(dsptb, "BIG5"));
					dsptblen = 17;  //20220630 MatsudairaSyuMe dsptblen
				}
				//處理支出收入金額
				double dTxamt = 0.0;
				if (crdb[0] == (byte)'0') {
					//支出
					String samtbuf = "";
					samtbuf = new String(p0080DataFormat.getTotaTextValueSrc("stxamt", pb_arr.get(i)));
					dTxamt = Double.parseDouble(new String(p0080DataFormat.getTotaTextValueSrc("txamt", pb_arr.get(i))).trim()) / 100.0;
					if (samtbuf.equals("-"))
						dTxamt *= -1.0;
//					NumberFormat format =  new DecimalFormat("#####,###,##0.00        ");
//					pbpr_crdblog = pbpr_crdblog + String.format("%25s", format.format(dTxamt));
					/* 20200820 adjust print position
					pbpr_crdb = pbpr_crdb + String.format("%18s", dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT) + "       ");
					
					pbpr_crdbT = String.format("%18s", dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT) + "       ");
					
					pbpr_crdblog = pbpr_crdblog + String.format("%18s", dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT) + "       ");
					*/
					//20210419 MatsudairaSyume reduce one space
					//pbpr_crdb = pbpr_crdb + String.format("%18s", dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT) + "        ");
					pbpr_crdb = pbpr_crdb + String.format("          %18s", dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT) + "       ");//20220630 MatsudairaSyuMe dsptbsnd left shift two column
					//20220630 MatsudairaSyuMe dsptbsnd left shift two column
					pbpr_crdbT = String.format("          %18s", dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT) + "       ");
					//20220630 MatsudairaSyuMe dsptbsnd left shift two column
					pbpr_crdblog = pbpr_crdblog + String.format("          %18s", dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT) + "       ");
					//----
					//----
				} else {
					//收入
					String samtbuf = "";
					samtbuf = new String(p0080DataFormat.getTotaTextValueSrc("stxamt", pb_arr.get(i)));
					dTxamt = Double.parseDouble(new String(p0080DataFormat.getTotaTextValueSrc("txamt", pb_arr.get(i))).trim()) / 100.0;
					if (samtbuf.equals("-"))
						dTxamt *= -1.0;
//					NumberFormat format =  new DecimalFormat("#####,###,##0.00   ");
//					pbpr_crdb = pbpr_crdb + String.format("%19s", format.format(dTxamt));
//					pbpr_crdblog = pbpr_crdblog + String.format("%19s", format.format(dTxamt));
					/* 20200820 adjust print position
					pbpr_crdb = pbpr_crdb + String.format("%18s", dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT) + "  ");

					pbpr_crdbT = String.format("%18s", dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT) + "  ");

					pbpr_crdblog = pbpr_crdblog + String.format("%18s", dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT) + "  ");
					*/
					//20210419 MatsudairaSyume reduce one space
					//pbpr_crdb = pbpr_crdb + String.format("                 %18s", dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT));
					pbpr_crdb = pbpr_crdb + String.format("                 %18s", dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT));//20220630 MatsudairaSyuMe dsptbsnd left shift one column

					pbpr_crdbT = String.format("                 %18s", dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT));//20220630 MatsudairaSyuMe dsptbsnd left shift one column

					pbpr_crdblog = pbpr_crdblog + String.format("                 %18s", dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT));//20220630 MatsudairaSyuMe dsptbsnd left shift one column
					//----
				}
				pr_datalog = pr_data;
				//20210419 MatsudairaSyume reduce one space change from %35s to %34s
				pbpr_crdb = String.format("%35s", pbpr_crdb); //20220630 MatsudairaSyuMe dsptbsnd left shift two column
				//20210419 MatsudairaSyume reduce one space change from %35s to %34s
				pbpr_crdbT = String.format("%35s", pbpr_crdbT);//20220630 MatsudairaSyuMe dsptbsnd left shift two column

				log.debug("pbpr_crdb len={} pbpr_crdbT [{}] len={}", pbpr_crdb.length(), pbpr_crdbT, pbpr_crdbT.length());

				String pr_dataprev = pr_data;

				pr_data = pr_data + pbpr_crdb;
				//20210419 MatsudairaSyume reduce one space change from %35s to %34s
				pr_datalog = pr_datalog + String.format("%35s", pbpr_crdblog);
				//處理結存
				String sbalbuff = "";
				sbalbuff = new String(p0080DataFormat.getTotaTextValueSrc("spbbal", pb_arr.get(i)));
				dTxamt = Double.parseDouble(new String(p0080DataFormat.getTotaTextValueSrc("pbbal", pb_arr.get(i))).trim()) / 100.0;
				if (sbalbuff.equals("-"))
					dTxamt *= -1.0;
//				NumberFormat format =  new DecimalFormat("*####,###,##0.00");
//				pbpr_balance = String.format("%19s", format.format(dTxamt));
				pbpr_balance = dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT2);
				if (this.type.equals("AUTO20")) {
					
				}
				byte[] nl = new byte[2];
				nl[0] = (byte)0x0d;
				nl[1] = (byte)0x0a;
				pr_data = pr_data + " " + pbpr_balance + new String(nl);
				
				pbpr_crdbT = pbpr_crdbT + " " +  pbpr_balance + new String(nl);
				
				pr_datalog = pr_datalog + " " + pbpr_balance;
				log.debug("pbpr_date=[{}] pbpr_wsno=[{}] pbpr_dscpt=[{}] pbpr_crdb=[{}] pbpr_balance=[{}] pr_data=[{}] pbpr_crdbT=[{}]", pbpr_date, pbpr_wsno, pbpr_dscpt, pbpr_crdb, pbpr_balance, pr_data, pbpr_crdbT);
				log.debug("pr_datalog=[{}]", pr_datalog);
				//20200826
				// 20210723 MatsudairaSyuMe Log Forging, 20210802
				//String chkpr_datalog = "";
				try {
					atlog.info(": PbDataFormat() -- All Data=[{}]",StrUtil.convertValidLog(pr_datalog));
				} catch (Exception ce) {
					ce.printStackTrace();
					log.error(": PbDataFormat() -- error",ce.toString()); //20210730 add
					atlog.info(": PbDataFormat() -- All Data=[]"); //20210730 add
				}
				//----
				//Print Data
				//20200915
				prt.PrepareSkipBuffer();
				//----
				if ( i == 0 )
				{
					for (int k=1; k <= (tl-1); k++)
					{
						if ( k == 12 && tl >= 13)
						{
							// tl 起始行數 > 12
//							prt.Parsing(firstOpenConn, "SKIP=3".getBytes());
//							prt.SkipnLine(3);
							//20200915
							prt.SkipnLineBuf(3);
							//----
						}
						else
//							prt.Parsing(firstOpenConn, "SKIP=1".getBytes());
//							prt.SkipnLine(1);
							//20200915
							prt.SkipnLineBuf(1);
							//----
					}
				}
				else
				{
					if ( (tl+i) == 13 )
					{
						// tl 起始行數 < 13
//						prt.Parsing(firstOpenConn, "SKIP=2".getBytes());
//						prt.SkipnLine(2);
						//20200915
						prt.SkipnLineBuf(2);
						//----
					}
					
				}
				log.debug("after skip line------------tl+i=[{}] total=[{}] i + 1=[{}] pb_arr.size()=[{}] Integer.parseInt(con)=[{}]", tl+i, total, i + 1, pb_arr.size(), Integer.parseInt(con));   //20200603 test
				//20200915
				byte[] skipbytes =	prt.GetSkipLineBuf();
				byte[] sndbary = new byte[pr_dataprev.getBytes().length + pbpr_crdbT.getBytes().length];
				System.arraycopy(pr_dataprev.getBytes(), 0, sndbary, 0, pr_dataprev.getBytes().length);
				System.arraycopy(pbpr_crdbT.getBytes(), 0, sndbary, pr_dataprev.getBytes().length, pbpr_crdbT.getBytes().length);
				System.arraycopy(dsptbsnd, 0, sndbary, pr_dataprev.getBytes().length, dsptblen);  //20220630 MatsudairaSyuMe use dsptblen
//				prt.Prt_Text(pr_data.getBytes());
				//20200915
				if (skipbytes != null && skipbytes.length > 0) 
					prt.Prt_Text(skipbytes, sndbary);
				else
					prt.Prt_Text(sndbary);
				//----
				//若印滿 24 筆且尚有補登資料，加印「請翻下頁繼續補登」
				if ( (tl+i) == 24 && (total > (i+1)) )   //20210401 total >= (i+1) change to total > (i+1))
				{
					// 因為存摺會補到滿, PB 只有8頁, 如果是第8頁則不進行換頁流程
					// 20180518 , add
					if (this.npage >= TXP.PB_MAX_PAGE) {
						this.iEnd = 2;
						return true;
					}
//					pr_data = "                                                     請翻下頁繼續補登\n"
					this.iEnd = 1;
					amlog.info("[{}][{}][{}]:62請翻下頁繼續補登...", brws, pasname, this.account);
//					if (prt.Prt_Text(pr_data.getBytes()) == false)
//						return false;
					sndbary = chgpgary;
					prt.Prt_Text(sndbary);
				}
				else
					this.iEnd = 0;
				if ((tl+i) == 24) //20210401
					break;
			}
		} catch (Exception e) {
			log.debug("error--->p0080text convert error", e.getMessage());
			rtn = false;
			this.curState = FORMATPRTDATAERROR;
		}
		return rtn;
	}

	/*********************************************************
	*  FcDataFormat() : Format TOTA Text to print            *
	*  function       : 列印外匯存摺資料格式                 *
	*  parameter 1    : tx_area data                         *
	*  parameter 2    : total NB count                       *
	*  return_code    : BOOL - TRUE                          *
	*                          FALSE               2008.08.25*
	*********************************************************/
	private boolean FcDataFormat() {
		boolean rtn = true;
		int tl, total;
		tl = this.iLine;
//20200603  test		total = this.iCon;
		//20210401 MatsudairaSyuMe total count not data count
//		total = Integer.parseInt(this.dCount);
		total = Integer.parseInt(con);
		//----
		String pbpr_date = String.format("%9s", " "); // 日期 9
		String pbpr_wsno = String.format("%5s", " "); // 櫃檯機編號 5
		String pbpr_crdblog = String.format("%36s", " "); // 摘要+支出收入金額 36
		String pbpr_crdb = String.format("%36s", " "); // 摘要+支出收入金額 36
		String pbpr_crdbT = String.format("%16s", " "); // 摘要+支出收入金額 36
		String pbpr_dscpt = String.format("%16s", " "); // 摘要 16 byte big
		String pbpr_balance = String.format("%18s", " ");// 結存 18
		String pr_datalog = ""; // 80
		String pr_data = ""; // 80

		if (this.curState == STARTPROCTLM) {
			q0880DataFormat = new Q0880TEXT();
			this.curState = PBDATAFORMAT;
		}
		log.debug("1--->q0880text=>{} {}", this.curState);
		try {
			// PB 日期(1+8)/空格(1)/櫃檯機編號(7)/摘要(16)/支出收入金額(20)/結存(18)/
			// FC 日期(1+8)/空格(1)/櫃檯機編號(5)/摘要(16)/幣別(3)/支出收入金額(21)/結存(18)

			for (int i = 0; i < fc_arr.size(); i++) {
				//處理日期格式
				pr_data = "";
				pbpr_date = String.format("%8s", (Integer.parseInt(new String (q0880DataFormat.getTotaTextValueSrc("date", fc_arr.get(i))).trim()) - 19110000));
				pr_data = pbpr_date;
				//處理櫃檯機編號
				pr_data = pr_data + " " + new String(q0880DataFormat.getTotaTextValueSrc("kinbr", fc_arr.get(i)))
					+ new String(q0880DataFormat.getTotaTextValueSrc("trmseq", fc_arr.get(i)));
				byte[] dsptb = q0880DataFormat.getTotaTextValueSrc("dscptx", fc_arr.get(i));
				String pr_dataprev = pr_data;
				dsptb = FilterBig5(dsptb);  //摘要
				pbpr_crdbT = String.format("%16s"," "); // 摘要 template(16 bytes)
				pr_data = pr_data + String.format("%-16s", new String(dsptb, "BIG5"));
				//處理幣別
				pbpr_crdbT = pbpr_crdbT + new String(q0880DataFormat.getTotaTextValueSrc("curcd", fc_arr.get(i)));
				pr_data = pr_data + new String(q0880DataFormat.getTotaTextValueSrc("curcd", fc_arr.get(i)));

				//處理支出收入金額
				double dTxamt = 0.0;
				byte[] crdb = q0880DataFormat.getTotaTextValueSrc("crdb", fc_arr.get(i));
				if (crdb[0] == (byte)'1') {
					//支出
					String samtbuf = "";
					samtbuf = new String(q0880DataFormat.getTotaTextValueSrc("hcode", fc_arr.get(i)));
					dTxamt = Double.parseDouble(new String(q0880DataFormat.getTotaTextValueSrc("txamt", fc_arr.get(i))).trim()) / 100.0;
					if (samtbuf.equals("1"))
						dTxamt *= -1.0;
					//20200903 add for convert dTxamt
					String dTxamtcnvStr = dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT1);
//					pr_data = pr_data + String.format("%19s   ", dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT));
//20200903					pr_data = pr_data + dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT1) + "  ";
					pr_data = pr_data + dTxamtcnvStr + "  ";
					
//					pbpr_crdbT = pbpr_crdbT + String.format("  %19s", dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT));
//20200903			pbpr_crdbT = pbpr_crdbT + dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT1) + "  ";
					pbpr_crdbT = pbpr_crdbT + dTxamtcnvStr + "  ";
					
//20200903			pbpr_crdblog = pbpr_crdblog + dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT1) + "  ";
					pbpr_crdblog = pbpr_crdblog + dTxamtcnvStr + "  ";
					//20200903
					atlog.info(": FcDataFormat() -- 支出 obuff=[{}]", dTxamtcnvStr);
					//----
				} else {
					//收入
					String samtbuf = "";
					samtbuf = new String(q0880DataFormat.getTotaTextValueSrc("hcode", fc_arr.get(i)));
					dTxamt = Double.parseDouble(new String(q0880DataFormat.getTotaTextValueSrc("txamt", fc_arr.get(i))).trim()) / 100.0;
					if (samtbuf.equals("1"))
						dTxamt *= -1.0;
					//20200903 add for convert dTxamt
					String dTxamtcnvStr = dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT1);

					//20200903
//					pr_data = pr_data + "  " + dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT1);

//					pbpr_crdbT = pbpr_crdbT + "  " + dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT1);

//					pbpr_crdblog = pbpr_crdblog + "   " + dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT1);
					pr_data = pr_data + "  " + dTxamtcnvStr;

					pbpr_crdbT = pbpr_crdbT + "  " + dTxamtcnvStr;

					pbpr_crdblog = pbpr_crdblog + "   " + dTxamtcnvStr;
					//20200903
					atlog.info(": FcDataFormat() -- 收入 obuff=[{}]", dTxamtcnvStr);
					//----
				}
				pr_datalog = pr_data;
				//處理結存
				log.debug("--->q0880text pbbal src [{}]", new String(q0880DataFormat.getTotaTextValueSrc("pbbal", fc_arr.get(i))).trim());
				dTxamt = Double.parseDouble(new String(q0880DataFormat.getTotaTextValueSrc("pbbal", fc_arr.get(i))).trim()) / 100.0;
				//20200903
				atlog.info(": FcDataFormat() -- dbal=[{}]",dTxamt);
				//----
				log.debug("--->q0880text pbbal float [{}]", dTxamt);
				pbpr_balance = dataUtil.rfmtdbl(dTxamt, TXP.AMOUNT2);
				//20200903
				atlog.info(": FcDataFormat() -- obalbuff=[{}]",pbpr_balance);
				//----
				log.debug("--->q0880text pbbal convert=[{}]", pbpr_balance);
				byte[] nl = new byte[2];
				nl[0] = (byte)0x0d;
				nl[1] = (byte)0x0a;
				pr_data = pr_data + pbpr_balance + new String(nl);
				
				pbpr_crdbT = pbpr_crdbT + pbpr_balance + new String(nl);
			
				pr_datalog = pr_datalog + pbpr_balance;
				log.debug("pbpr_date=[{}] pbpr_wsno=[{}] pbpr_dscpt=[{}] pbpr_crdb=[{}] pbpr_balance=[{}] pr_data=[{}] pbpr_crdbT=[{}]", pbpr_date, pbpr_wsno, pbpr_dscpt, pbpr_crdb, pbpr_balance, pr_data, pbpr_crdbT);
				log.debug("pr_datalog=[{}]", pr_datalog);
				//20200826
				atlog.info(": FcDataFormat() -- All Data=[{}]", pr_datalog);
				//----

				//Print Data
				//20200915
				prt.PrepareSkipBuffer();
				//----
				if ( i == 0 )
				{
					for (int k=1; k <= (tl-1); k++)
					{
						if ( k == 12 && tl >= 13)
						{
							// tl 起始行數 > 12
//							prt.Parsing(firstOpenConn, "SKIP=3".getBytes());
//							prt.SkipnLine(3);
							//20200915
							prt.SkipnLineBuf(3);
							//----
						}
						else
//							prt.Parsing(firstOpenConn, "SKIP=1".getBytes());
//							prt.SkipnLine(1);
							//20200915
							prt.SkipnLineBuf(1);
							//----
					}
				}
				else
				{
					if ( (tl+i) == 13 )
					{
						// tl 起始行數 < 13
//						prt.Parsing(firstOpenConn, "SKIP=2".getBytes());
						//20200915
						prt.SkipnLineBuf(2);
						//----
					}
					
				}
				log.debug("after skip line------------tl+i=[{}] total=[{}] i+1=[{}] fc_arr.size()=[{}]", tl+i, total, i+1, fc_arr.size());   //20200603 test
				//20200915
				byte[] skipbytes =	prt.GetSkipLineBuf();
				byte[] sndbary = new byte[pr_dataprev.getBytes().length + pbpr_crdbT.getBytes().length];
				System.arraycopy(pr_dataprev.getBytes(), 0, sndbary, 0, pr_dataprev.getBytes().length);
				System.arraycopy(pbpr_crdbT.getBytes(), 0, sndbary, pr_dataprev.getBytes().length, pbpr_crdbT.getBytes().length);
				System.arraycopy(dsptb, 0, sndbary, pr_dataprev.getBytes().length, dsptb.length);
				//20200915
				if (skipbytes != null && skipbytes.length > 0) 
					prt.Prt_Text(skipbytes, sndbary);
				else
					prt.Prt_Text(sndbary);
				//若印滿 24 筆且尚有補登資料，加印「請翻下頁繼續補登」
				if ( (tl+i) == 24 && (total > (i+1)) )  //20210401 change to total > (i+1))
				{
					// 因為存摺會補到滿, FC 只有5頁, 如果是第5頁則不進行換頁流程
					// 20180518 , add
					if (this.npage >= TXP.FC_MAX_PAGE) {
						this.iEnd = 2;
						return true;
					}
//					pr_data = "                                                     請翻下頁繼續補登\n"
					this.iEnd = 1;
					amlog.info("[{}][{}][{}]:62請翻下頁繼續補登...", brws, pasname, this.account);
//					if (prt.Prt_Text(pr_data.getBytes()) == false)
//						return false;
					sndbary = chgpgary;
					prt.Prt_Text(sndbary);
				}
				else
					this.iEnd = 0;
			}

		} catch (Exception e) {
			log.debug("error--->q0880text convert error", e.getMessage());
			rtn = false;
			this.curState = FORMATPRTDATAERROR;
		}
		return rtn;
	}
	
	/*********************************************************
	*  GlDataFormat() : Format TOTA Text to print            *
	*  function       : 列印黃金存摺資料格式                 *
	*  parameter 1    : tx_area data                         *
	*  parameter 2    : total NB count                       *
	*  return_code    : BOOL - TRUE                          *
	*                          FALSE               2008.06.01*
	*********************************************************/
	private boolean GlDataFormat() {
		boolean rtn = true;
		int tl, total;
		tl = this.iLine;
		//20200603  test		total = this.iCon;
		//20210401 MatsudairaSyuMe total count not data count
//		total = Integer.parseInt(this.dCount);
		total = Integer.parseInt(con);
		//----
		String pbpr_date = String.format("%8s", " "); // 日期 8
		String pbpr_wsno = String.format("%5s", " "); // 櫃檯機編號 5
		String pbpr_crdblog = String.format("%36s", " "); // 摘要+支出收入金額 36
		String pbpr_crdb = String.format("%36s", " "); // 摘要+支出收入金額 36
		String pbpr_crdbT = String.format("%10s", " "); // 摘要
		String pbpr_dscpt = String.format("%10s", " "); // 摘要 10 byte big
		String pbpr_balance = String.format("%12s", " ");// 結存 12
		String pr_datalog = ""; // 80
		String pr_data = ""; // 列印資料 80
		//20200904
		atlog.info(": GlDataFormat() -- m_gArr.Count=[{}]", gl_arr.size());
		//----

		if (this.curState == STARTPROCTLM) {
			p0880DataFormat = new P0880TEXT();
			this.curState = PBDATAFORMAT;
		}
		log.debug("1--->p0880text=>{}", this.curState);
		try {
			// PB 日期(1+8)/空格(1)/櫃檯機編號(7)/摘要(16)/支出收入金額(20)/結存(18)/
			// FC 日期(1+8)/空格(1)/櫃檯機編號(5)/摘要(16)/幣別(3)/支出收入金額(21)/結存(18)
			// GL 空格(1)/日期(8)/空格(1)/櫃檯機編號(7)/空格(1)/摘要(8)/幣別(2)/單價(4.2)/空格(1)/支出(S5.2)/空格(1)/收入(S5.2)/空格(1)/結存(7.2)/空格(1)/更正記號(1)

			for (int i = 0; i < gl_arr.size(); i++) {
				//空格(1)+日期
				//20210127 MatsudairaSyume add back 1 space
				pr_data = " ";
				//----
				pbpr_date = pr_data + new String(p0880DataFormat.getTotaTextValueSrc("txday", gl_arr.get(i)));
				pr_data = pr_data + pbpr_date;

				//空格(2)+櫃台機編號
				pr_data = pr_data + " " + new String(p0880DataFormat.getTotaTextValueSrc("kinbr", gl_arr.get(i)))
					+ new String(p0880DataFormat.getTotaTextValueSrc("trmseq", gl_arr.get(i)));
				byte[] dsptb = p0880DataFormat.getTotaTextValueSrc("dscptx", gl_arr.get(i));
				String pr_dataprev = pr_data;
				dsptb = FilterBig5(dsptb);

				//空格(1)+摘要
				//20200821 摘要 10
				pbpr_crdbT = String.format("%10s", " "); // " " + 摘要 template(11 bytes)
				pr_data = pr_data + " " + String.format("%-10s", new String(dsptb, "BIG5"));

				//空格(1)+幣別(NT/US)(2)+單價(7)
				//單價(4.2)
				//20210127 MatshdairaSyume cut back to 1 space
				pbpr_crdbT = pbpr_crdbT + " " + new String(p0880DataFormat.getTotaTextValueSrc("curcd", gl_arr.get(i)));
				pr_data = pr_data + " " + new String(p0880DataFormat.getTotaTextValueSrc("curcd", gl_arr.get(i)));
				//----

				//20200731 Matsudaira check for null value!!!!!!
				byte[] pricechk = p0880DataFormat.getTotaTextValueSrc("price", gl_arr.get(i));
				int chkidx = 0;
				boolean filled = false;
				for (final byte cb : pricechk) {
					if (cb == (byte)0x0) {
						pricechk[chkidx] = (byte)'0';
						filled = true;
					}
					chkidx++;
				}
				log.debug("--->p0880text price src [{}] and filled [{}]", new String(pricechk).trim(), filled ? "yes":"no");
//				double price = Double.parseDouble(new String(p0880DataFormat.getTotaTextValueSrc("price", gl_arr.get(i))).trim()) / 100.0;
				double price = Double.parseDouble(new String(pricechk).trim()) / 100.0;
				//----
				pbpr_crdbT = pbpr_crdbT + dataUtil.rfmtdbl(price, "ZZZ9.99");
				pr_data = pr_data + dataUtil.rfmtdbl(price, "ZZZ9.99");

				//處理支出(回售/提領)黃金數
				
				String wamtbuff = new String(p0880DataFormat.getTotaTextValueSrc("withsign", gl_arr.get(i)))
					+ new String(p0880DataFormat.getTotaTextValueSrc("withdraw", gl_arr.get(i)));

				//處理存入黃金數
				String damtbuff = new String(p0880DataFormat.getTotaTextValueSrc("deposign", gl_arr.get(i)))
						+ new String(p0880DataFormat.getTotaTextValueSrc("deposit", gl_arr.get(i)));

				//處理支出收入金額
				double dTxamt = 0.0;
				if (Double.parseDouble(wamtbuff) != 0) {
					//支出
					dTxamt = Double.parseDouble(wamtbuff) / 100.0;
					/*20200821
					pr_data = pr_data +  " " + dataUtil.rfmtdbl(dTxamt, TXP.GRAM1) + "           ";
					
					pbpr_crdbT = pbpr_crdbT + " " + dataUtil.rfmtdbl(dTxamt, TXP.GRAM1) + "           ";
					
					pbpr_crdblog = pbpr_crdblog + " " + dataUtil.rfmtdbl(dTxamt, TXP.GRAM1) + "           ";
					*/
					pr_data = pr_data +  " " + dataUtil.rfmtdbl(dTxamt, TXP.GRAM1) + "         ";//20210324 MatsudairaSyume delete 1 space
					
					pbpr_crdbT = pbpr_crdbT + " " + dataUtil.rfmtdbl(dTxamt, TXP.GRAM1) + "         ";//20210324 MatsudairaSyume delete 1 space
					
					pbpr_crdblog = pbpr_crdblog + " " + dataUtil.rfmtdbl(dTxamt, TXP.GRAM1) + "         ";//20210324 MatsudairaSyume delete 1 space
					//----
				} else {
					//收入
					dTxamt = Double.parseDouble(damtbuff) / 100.0;
					/*20200821
					pr_data = pr_data +  "            " + dataUtil.rfmtdbl(dTxamt, TXP.GRAM1);
					
					pbpr_crdbT = pbpr_crdbT + "            " + dataUtil.rfmtdbl(dTxamt, TXP.GRAM1);
					
					pbpr_crdblog = pbpr_crdblog + "            " + dataUtil.rfmtdbl(dTxamt, TXP.GRAM1);
					*/
					pr_data = pr_data +  "          " + dataUtil.rfmtdbl(dTxamt, TXP.GRAM1);//20210324 MatsudairaSyume delete 1 space
					
					pbpr_crdbT = pbpr_crdbT + "          " + dataUtil.rfmtdbl(dTxamt, TXP.GRAM1);//20210324 MatsudairaSyume delete 1 space
					
					pbpr_crdblog = pbpr_crdblog + "          " + dataUtil.rfmtdbl(dTxamt, TXP.GRAM1);//20210324 MatsudairaSyume delete 1 space
					//----

				}
				pr_datalog = pr_data;
				//處理結存
//				log.debug("--->p0880text avebal src [{}]", new String(p0880DataFormat.getTotaTextValueSrc("avebal", gl_arr.get(i))).trim());
				//20200731 Matsudaira check for null value!!!!!!
				byte[] avebalchk = p0880DataFormat.getTotaTextValueSrc("avebal", gl_arr.get(i));
				chkidx = 0;
				filled = false;
				for (final byte cb : avebalchk) {
					if (cb == (byte)0x0) {
						avebalchk[chkidx] = (byte)'0';
						filled = true;
					}
					chkidx++;
				}
				log.debug("--->p0880text avebal src [{}] and filled [{}]", new String(avebalchk).trim(), filled ? "yes":"no");
//				dTxamt = Double.parseDouble(new String(p0880DataFormat.getTotaTextValueSrc("avebal", gl_arr.get(i))).trim()) / 100.0;
				//----
				dTxamt = Double.parseDouble(new String(avebalchk).trim()) / 100.0;
				log.debug("--->p0880text avebal float [{}]", dTxamt);
				pbpr_balance = dataUtil.rfmtdbl(dTxamt, TXP.GRAM2);
				log.debug("--->p0880text avebal convert=[{}]", pbpr_balance);
				//20200731 Matsudaira check for null value!!!!!!
//				tx_area.put("avebal", new String(p0880DataFormat.getTotaTextValueSrc("avebal", gl_arr.get(i))));
				tx_area.put("avebal", new String(avebalchk));
				//----
				
				tx_area.put("nbday", new String(p0880DataFormat.getTotaTextValueSrc("txday", gl_arr.get(i))));
				tx_area.put("nbseq", new String(p0880DataFormat.getTotaTextValueSrc("nbseq", gl_arr.get(i))));
				tx_area.put("kinbr", new String(p0880DataFormat.getTotaTextValueSrc("kinbr", gl_arr.get(i))));
				
				byte[] nl = new byte[2];
				nl[0] = (byte)0x0d;
				nl[1] = (byte)0x0a;
 			   pr_data = pr_data + " " + pbpr_balance + new String(nl);
				
				pbpr_crdbT = pbpr_crdbT + " " +  pbpr_balance + new String(nl);
				
				pr_datalog = pr_datalog + " " + pbpr_balance;
				log.debug("pbpr_date=[{}] pbpr_wsno=[{}] pbpr_dscpt=[{}] pbpr_crdb=[{}] pbpr_balance=[{}] pr_data=[{}] pbpr_crdbT=[{}]", pbpr_date, pbpr_wsno, pbpr_dscpt, pbpr_crdb, pbpr_balance, pr_data, pbpr_crdbT);
				log.debug("pr_datalog=[{}]", pr_datalog);
				//20200826
				atlog.info(": GlDataFormat() -- All Data=[{}]", pr_datalog);
				//----
				//Print Data
				//20200915
				prt.PrepareSkipBuffer();
				//----
				if ( i == 0 )
				{
					for (int k=1; k <= (tl-1); k++)
					{
						if ( k == 12 && tl >= 13)
						{
							// tl 起始行數 > 12
//							prt.Parsing(firstOpenConn, "SKIP=3".getBytes());
//							prt.SkipnLine(3);
							//20200915
							prt.SkipnLineBuf(3);
							//----
						}
						else
//							prt.Parsing(firstOpenConn, "SKIP=1".getBytes());
//							prt.SkipnLine(1);
							//20200915
							prt.SkipnLineBuf(1);
							//----
					}
				}
				else
				{
					if ( (tl+i) == 13 )
					{
						// tl 起始行數 < 13
//						prt.Parsing(firstOpenConn, "SKIP=2".getBytes());
//						prt.SkipnLine(2);
						//20200915
						prt.SkipnLineBuf(2);
						//----
					}
					
				}
				log.debug("after skip line------------tl+i=[{}] total=[{}] i+1=[{}] gl_arr.size()=[{}]", tl+i, total, i+1, gl_arr.size());   //20200603 test
				//20200915
				byte[] skipbytes =	prt.GetSkipLineBuf();
				byte[] sndbary = new byte[pr_dataprev.getBytes().length + pbpr_crdbT.getBytes().length];
				System.arraycopy(pr_dataprev.getBytes(), 0, sndbary, 0, pr_dataprev.getBytes().length);
				System.arraycopy(pbpr_crdbT.getBytes(), 0, sndbary, pr_dataprev.getBytes().length, pbpr_crdbT.getBytes().length);
				System.arraycopy(dsptb, 0, sndbary, pr_dataprev.getBytes().length, dsptb.length);  //20220630 MatsudairaSyuMe dsptbsnd left shift one column
				//20200915
				if (skipbytes != null && skipbytes.length > 0) 
					prt.Prt_Text(skipbytes, sndbary);
				else
					prt.Prt_Text(sndbary);
				//若印滿 24 筆且尚有補登資料，加印「請翻下頁繼續補登」
				if ( (tl+i) == 24 && (total > (i+1)) )  //20210401 total >= (i+1)  change to total > (i+1))
				{
					// 因為存摺會補到滿, GL 只有9頁, 如果是第9頁則不進行換頁流程
					// 20180518 , add
					if (this.npage >= TXP.GL_MAX_PAGE) {
						this.iEnd = 2;
						return true;
					}
//					pr_data = "                                                     請翻下頁繼續補登\n";
					this.iEnd = 1;
					amlog.info("[{}][{}][{}]:62請翻下頁繼續補登...", brws, pasname, this.account);
//					if (prt.Prt_Text(pr_data.getBytes()) == false)
//						return false;
					sndbary = chgpgary;
					prt.Prt_Text(sndbary);
				}
				else
					this.iEnd = 0;
				if ((tl+i) == 24) //20210401
					break;
			}

		} catch (Exception e) {
			log.debug("error--->p0880text convert error", e.getMessage());
			rtn = false;
			this.curState = FORMATPRTDATAERROR;
		}
		return rtn;
	}

	/*********************************************************
	*  WMSRFormat() : Format the new MSR                     *
	*  paramater 1  : tx area data                           *
	*  paramater 2  : AP flag                                *
	*  return_code  : BOOL - TRUE                            *
	*                        FALSE                 2008.01.30*
	*********************************************************/
	private boolean WMSRFormat(boolean start)
	{
		boolean rtn = false;
		int l = 0, p = 0, iCnt = 0;
		byte wline[] = new byte[2];
		byte wpage[] = new byte[2];
		Arrays.fill(wline, (byte) 0x0);
		Arrays.fill(wpage, (byte) 0x0);
		byte c_Msr[] = tx_area.get("c_Msr").getBytes();
		log.debug("{} {} {} WMSRFormat before to write flag={} PBTYPE {} MSR [{}]", brws, catagory, account, start, this.iFig, new String(c_Msr));
		if (start) {
			if (this.iFig == TXP.PBTYPE) {
				if (p0080DataFormat == null)
					p0080DataFormat = new P0080TEXT();
				System.arraycopy(c_Msr, 30, wline, 0, 2);
				System.arraycopy(c_Msr, 32, wpage, 0, 2);
				iCnt = pb_arr.size();
			}
			if (this.iFig == TXP.FCTYPE) {
				if (q0880DataFormat == null)
					q0880DataFormat = new Q0880TEXT();
				System.arraycopy(c_Msr, 30, wline, 0, 2);
				System.arraycopy(c_Msr, 32, wpage, 0, 2);
				iCnt = fc_arr.size();
			}
			if (this.iFig == TXP.GLTYPE) {
				if (p0880DataFormat == null)
					p0880DataFormat = new P0880TEXT();
				System.arraycopy(c_Msr, 24, wline, 0, 2);
				System.arraycopy(c_Msr, 26, wpage, 0, 2);
				iCnt = gl_arr.size();
			}
			l = Integer.parseInt(new String(wline));
			p = Integer.parseInt(new String(wpage));
			if ((l - 1) + iCnt >= 24) {  //20210401 if ((l - 1) + iCnt == 24) change to if ((l - 1) + iCnt >= 24)
				if ((l - 1) + iCnt > 24) {
					log.debug("WMSRFormat (l - 1) + iCnt > 24 before {}", iCnt);
					iCnt = iCnt - ((l - 1) + iCnt - 24);
					log.debug("WMSRFormat (l - 1) + iCnt > 24 after {}", iCnt);
				}
				//----
				l = 1;
				p = p + 1;
			} else
				l = l + iCnt;
		}
		try {
			switch (this.iFig) {
			case TXP.PBTYPE:
				if (start) {
					//20221102 MSR new format negative symbol '<' immediately before the number characters
					byte[] spbbal = p0080DataFormat.getTotaTextValueSrc("spbbal", pb_arr.get(iCnt - 1));
					/*if (new String(spbbal).equals("-"))
						System.arraycopy(spbbal, 0, c_Msr, 16, 1);
					else
						System.arraycopy("0".getBytes(), 0, c_Msr, 16, 1);
					*/
					System.arraycopy(p0080DataFormat.getTotaTextValueSrc("pbbal", pb_arr.get(iCnt - 1)), 0, c_Msr, 17,
							13);
					System.arraycopy(String.format("%02d", l).getBytes(), 0, c_Msr, 30, 2);
					System.arraycopy(String.format("%02d", p).getBytes(), 0, c_Msr, 32, 2);
log.debug(" before transfer write new PBTYPE line={} page={} MSR {}", l, p, new String(c_Msr));

					System.arraycopy("0".getBytes(), 0, c_Msr, 16, 1);
					if (new String(spbbal).equals("-")) {
						for (int tidx = 17; tidx < (TXP.ACTNO_LEN + TXP.ACFILLER_LEN + TXP.MSRBAL_LEN); tidx++) {
							if (c_Msr[tidx] != (byte)'0') {
								c_Msr[tidx - 1] = (byte)'-';
								break;
							}
						}
					}
					//---- 20221102
					tx_area.put("c_Msr", new String(c_Msr));
				}
				rtn = prt.MS_Write(start, brws, account, c_Msr);//20200712 add for test
				if (p > TXP.PB_MAX_PAGE) this.changeLightOnLastPage = true; //20220927
//				log.debug(" after to write new PBTYPE line={} page={} MSR {} changeLight=[{}]", l, p, tx_area.get("c_Msr"), (this.changeLightOnLastPage ? "Yes": "No")); //20220927
				log.debug(" after to write new PBTYPE line={} page={} MSR {} changeLight=[{}]", l, p, new String(tx_area.get("c_Msr")), (this.changeLightOnLastPage ? "Yes": "No")); //20220927
				break;
			case TXP.FCTYPE:
				if (start) {
					System.arraycopy("0".getBytes(), 0, c_Msr, 16, 1);
					System.arraycopy(q0880DataFormat.getTotaTextValueSrc("pbbal", fc_arr.get(iCnt - 1)), 0, c_Msr, 17,
							13);
					System.arraycopy(String.format("%02d", l).getBytes(), 0, c_Msr, 30, 2);
					System.arraycopy(String.format("%02d", p).getBytes(), 0, c_Msr, 32, 2);
					tx_area.put("c_Msr", new String(c_Msr));
					//20200528
					tx_area.put("pbcol", String.format("%02d", l));
					tx_area.put("pbpage", String.format("%02d", p));
					//----
				}
				rtn = prt.MS_Write(start, brws, account, c_Msr);  //20200712 add for test
				if (p > TXP.FC_MAX_PAGE) this.changeLightOnLastPage = true; //20220927
				log.debug(" after to write new FCTYPE line={} page={} MSR {} changeLight=[{}]", l, p, tx_area.get("c_Msr"), (this.changeLightOnLastPage ? "Yes": "No")); //20220927
				break;
			case TXP.GLTYPE:
				if (start ) {
					System.arraycopy("000".getBytes(), 0, c_Msr, 16, 3);
					System.arraycopy(p0880DataFormat.getTotaTextValueSrc("avebal", gl_arr.get(iCnt - 1)), 0, c_Msr, 15,
							9);
					System.arraycopy(String.format("%02d", l).getBytes(), 0, c_Msr, 24, 2);
					System.arraycopy(String.format("%02d", p).getBytes(), 0, c_Msr, 26, 2);
					tx_area.put("c_Msr", new String(c_Msr));
					//20200528
					tx_area.put("lineno", String.format("%02d", l));
					tx_area.put("pageno", String.format("%02d", p));
					//----
				}
				rtn = prt.MS_Write(start, brws, account, c_Msr);  //20200712 add for test
				if (p > TXP.GL_MAX_PAGE) this.changeLightOnLastPage = true; //20220927
				log.debug(" after to write new GLTYPE line={} page={} MSR {} changeLight=[{}]", l, p, tx_area.get("c_Msr"), (this.changeLightOnLastPage ? "Yes": "No")); //20220927
				break;
			default:
				rtn = false;
				break;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.debug("{} {} {} WMSRFormat exception {}", brws, catagory, account, e.getMessage());
		}

		return rtn;
	}

	private boolean WMSRFormat(boolean start, int setPage)
	{
		boolean rtn = false;
		int l = 0, p = 0;
		byte wline[] = new byte[2];
		byte wpage[] = new byte[2];
		Arrays.fill(wline, (byte) 0x0);
		Arrays.fill(wpage, (byte) 0x0);
		byte c_Msr[] = tx_area.get("c_Msr").getBytes();
		log.debug("{} {} {} WMSRFormat before to write flag={} PBTYPE {} MSR [{}] change page [{}]", brws, catagory, account, start, this.iFig, new String(c_Msr), p);
		p = setPage;
		try {
			switch (this.iFig) {
			case TXP.PBTYPE:
				System.arraycopy(String.format("%02d", p).getBytes(), 0, c_Msr, 32, 2);
				rtn = prt.MS_Write(start, brws, account, c_Msr);
				log.debug("{} {} {} WMSRFormat after to write new PBTYPE line={} page={} MSR {}", brws, catagory, account, l, p, tx_area.get("c_Msr"));
				break;
			case TXP.FCTYPE:
				System.arraycopy(String.format("%02d", p).getBytes(), 0, c_Msr, 32, 2);
				rtn = prt.MS_Write(start, brws, account, c_Msr);
				log.debug("{} {} {} WMSRFormat after to write new FCTYPE line={} page={} MSR {}", brws, catagory, account, l, p, tx_area.get("c_Msr"));
				break;
			case TXP.GLTYPE:
				System.arraycopy(String.format("%02d", p).getBytes(), 0, c_Msr, 26, 2);
				rtn = prt.MS_Write(start, brws, account, c_Msr);
				log.debug("{} {} {} WMSRFormat after to write new GLTYPE line={} page={} MSR {}", brws, catagory, account, l, p, tx_area.get("c_Msr"));
				break;
			default:
				rtn = false;
				break;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.debug("{} {} {} WMSRFormat exception {}", brws, catagory, account, e.getMessage());
		}

		return rtn;
	}
	/*********************************************************
	*	FilterBig5() : filter the chinese control code       *
	*   function     : 去除中文控制碼                        *
	*   parameter 1  : dsptext included the control code     *
	*   parameter 2  : dsptext length                        *
	*   return_code  : dsptext filter the control code       *
	*********************************************************/
	
	private byte[] FilterBig5(byte dsptext[]) {
		int iChin = 0, j = 0;
		for (int i = 0; i < dsptext.length; i++) {
			if (dsptext[i] == 0x04) {
				iChin = 1;
				continue;
			}
			if (dsptext[i] == 0x07) {
				if (iChin==1) {
					//dsptext[j++] = ' ';
					//dsptext[j++] = ' ';
				}
				iChin = 0;
				continue;
			}
			dsptext[j++] = dsptext[i];
		}
		for (int i = j; i < dsptext.length; i++)
			dsptext[j++] = ' ';
		// 20081104 , fix for chinese char cut half.
		if (dsptext[dsptext.length - 1] >= 0x80)
			dsptext[dsptext.length - 1] = 0x20;

		return dsptext;
	}

	/*********************************************************
	*	  FilterChi()  : filter the chinese control code       *
	*   function     : 去除文字全半形參雜(尾碼避免中文一半)  *
	*   parameter 1  : dsptext                               *
	*   parameter 2  : dsptext length                        *
	*   return_code  : dsptext                               *
	*********************************************************/

	private byte[] FilterChi(byte[] dsptext, int dsplen) {
		int pt = 0;

		for (int i = 0; i < dsplen; i++) {
			if ((int)dsptext[i] >= (int)(0x80 & 0xff)) {
				pt = pt + 2;
				i++;
			} else {
				pt = pt + 1;
			}
		}

		if (pt > dsplen)
			dsptext[dsplen - 1] = 0x20;
		return dsptext;
	}
//20200902 	private byte[] DataDEL(int iVal, int ifig, String mbal) {
	private byte[] DataDEL(int iVal, int currentifig, String mbal) {
		byte[] rtn = null;
		P85TEXT p85text = null;
		Q98TEXT q98text = null;
		P1885TEXT p1885text = null;
		//20200902 change to currentifig
		log.debug("1--->ifig={} mbal={}", currentifig, mbal);
		try {
			if (iVal == TXP.SENDTHOST) { // send to host
				//***** Compose P85 TITA *****//
				//20200902 change to currentifig
				if (currentifig == TXP.PBTYPE)
				{
					p85text = new P85TEXT();
					boolean p85titatextrtn = p85text.initP85TitaTEXT((byte) '0');
					log.debug("p85titatextrtn.initP85TitaTEXT p0080titatextrtn={}", p85titatextrtn);
					tital.setValue("aptype", "P");
					tital.setValue("apcode", "85");
					tital.setValue("stxno", "00");
					tital.setValue("ptype", "0");
					tital.setValue("dscpt", "     ");
					tital.setValueLtoRfill("actno", tx_area.get("account"), (byte) ' ');
					if (tital.ChkCrdb(mbal) > 0)
						tital.setValue("crdb", "1");
					else
						tital.setValue("crdb", "0");
					String sm = this.msrbal.substring(1);
					//20200827
//					atlog.info("pArr[0]=[{}]",sm);
					atlog.info("-- pArr[0]=[{}]",charcnv.BIG5bytesUTF8str(pb_arr.get(0)));
					//----
					sm = tital.FilterMsr(sm, '-', '0');
					tital.setValue("txamt", sm);
					atlog.info("TITA_BASIC.txamt=[{}]",sm);
					tital.setValue("ver", "02");
					p85text.setValue("bkseq", this.bkseq);
					//20200523
					String set_str = this.bkseq;

//					p85text.appendTitaText("date", pb_arr.get(0));
					//----
					/* 20230221 MatsudairaSyuMe mark for check PB snpbbal from last printed telegram
					if (tital.ChkCrdb(this.msrbal) > 0) {    ///check 20200224
						p85text.setValue("snpbbal", "+");
						//20200523
						set_str = set_str +  "+";
						//----
					}else {
						p85text.setValue("snpbbal", "-");
						//20200523
						set_str = set_str + "-";
						//----
					}
					20230221 mark */
					String cursnpbbal = tx_area.get("snpbbal");
					String curnpbbal = tx_area.get("npbbal");   //20230221 MatsudairaSyume
					log.debug("1 check PB current snpbbal= [{}]", cursnpbbal);
					log.debug("2 check PB current npbbal= [{}]", curnpbbal);//20230221 MatsudairaSyume
					try {
				        if (Long.parseLong(curnpbbal) == 0) {  // 20230221 15:15change to Long
					        cursnpbbal = "0";
					        log.debug("2.1 check PB current npbbal= [{}] so snpbbal change to=[{}]", curnpbbal, cursnpbbal);//20230221 MatsudairaSyume
				        }
					} catch( NumberFormatException e) {
						log.error("Input string Error");
					}
//					p85text.setValue("npbbal", this.msrbal.substring(1));//20230221 MatsudairaSyume mark up
//					p85text.setValue("npbbal", tx_area.get("npbbal"));//20230221 MatsudairaSyume mark up
					p85text.setValue("snpbbal", cursnpbbal);
					p85text.setValue("npbbal", curnpbbal);//20230221 MatsudairaSyume
//					String scnt = String.format("%04d", pb_arr.size());
					p85text.setValue("delcnt", String.format("%04d", pb_arr.size()));
//					p85text.setValue("fnbdtl", pb_arr.get(pb_arr.size() - 1)); 20200523 change to pb_arr = 0
					log.debug("3 pb_arr {} [{}]", 0, new String(pb_arr.get(0)));
					//20200523
					set_str = set_str + tx_area.get("npbbal") + String.format("%04d", pb_arr.size());
					byte[] set_arr = new byte[p85text.getP85TitatextLen() - set_str.getBytes().length];
					System.arraycopy(pb_arr.get(0), 0, set_arr, 0, p85text.getP85TitatextLen() - set_str.getBytes().length);
					log.debug("set_arr length [{}]", set_arr.length);
					log.debug(" len [{}]", p85text.getP85TitatextLen());
					p85text.appendTitaText("date", set_arr);
					//----
					rtn = tital.mkTITAmsg(tital.getTitalabel(), p85text.getP85Titatext());
//					log.debug("P85 tita len={} [{}] len={} [{}]len={} [{}]", rtn.length, new String(rtn), tital.getTitalabel().length, new String(tital.getTitalabel()),p85text.getP85Titatext().length, new String(p85text.getP85Titatext()));
					log.debug("P85 tita len={} [{}]", rtn.length, new String(rtn));
				}
				//***** Compose Q98 TITA *****//
				//20200902 change to currentifig
				else if (currentifig == TXP.FCTYPE)
				{
					q98text = new Q98TEXT();
					boolean q98titatextrtn = q98text.initQ98TitaTEXT((byte) ' ');
					log.debug("q98titatextrtn.initQ98TitaTEXT q98titatextrtn={}", q98titatextrtn);
					tital.setValue("aptype", "Q");
					tital.setValue("apcode", "98");
					tital.setValue("stxno", "00");
					tital.setValue("ptype", "0");
					tital.setValue("dscpt", "     ");
					tital.setValueLtoRfill("actno", tx_area.get("account"), (byte) ' ');
					tital.setValue("crdb", "0");
					tital.setValue("nbcd", "3");
					//20200528
//					log.debug("fc_arr size() - 1={} [{}]", fc_arr.size() - 1, new String(fc_arr.get(fc_arr.size() - 1)));
					log.debug("fc_arr {} [{}]", 0, new String(fc_arr.get(0)));
					String sm = this.msrbal.substring(1);
					//20200827
//					atlog.info("fArr[0]=[{}]",sm);
					atlog.info("-- fArr[0]=[{}]",charcnv.BIG5bytesUTF8str(fc_arr.get(0)));
					//----
					tital.setValue("txamt", sm);
					atlog.info("TITA_BASIC.txamt=[{}]",sm);
					q98text.setValue("newseq", tx_area.get("txseq"));
					q98text.setValue("oldseq", tx_area.get("txseq"));
					q98text.setValue("oldwsno", "00000");
					q98text.setValue("retur", "0");
					q98text.setValue("rbrno", tital.getValue("brno"));
					q98text.setValue("acbrno", tital.getValue("brno"));
					q98text.setValue("aptype", "14");
					q98text.setValue("corpno", "00");
					q98text.setValue("actfg", "1");
					q98text.setValue("nbcnt", String.format("%03d", fc_arr.size()));
					q98text.setValue("txday", tx_area.get("txday"));
					q98text.setValue("txseq", tx_area.get("txseq"));
					q98text.setValue("pbbal", tx_area.get("pbbal"));
					q98text.setValue("pbcol", tx_area.get("pbcol"));
					q98text.setValue("pbpage", tx_area.get("pbpage"));

					rtn = tital.mkTITAmsg(tital.getTitalabel(), q98text.getQ98Titatext());
					log.debug("Q98 tita [{}]", new String(rtn));
				}
				//***** Compose Pxx TITA *****//
				//20200902 change to currentifig
				else if (currentifig == TXP.GLTYPE)
				{
					p1885text = new P1885TEXT();
					boolean p1885titatextrtn = p1885text.initP1885TitaTEXT((byte) ' ');
					log.debug("p1885titatextrtn.initP1885TitaTEXT p1885titatextrtn={}", p1885titatextrtn);
					tital.setValue("aptype", "P");
					tital.setValue("apcode", "18");
					tital.setValue("stxno", "85");
					tital.setValue("ptype", "0");
					tital.setValue("dscpt", "     ");
					tital.setValueLtoRfill("actno", tx_area.get("account"), (byte) ' ');
					if (tital.ChkCrdb(mbal) > 0)
						tital.setValue("crdb", "1");
					else
						tital.setValue("crdb", "0");
					tital.setValue("nbcd", "8");
					log.debug("gl_arr size() - 1={} [{}]", gl_arr.size() - 1, new String(gl_arr.get(gl_arr.size() - 1)));
					log.debug("gl_arr {} [{}]", 0, new String(gl_arr.get(0)));
					String sm = "0" + this.msrbal;
					//20200827
//					atlog.info("gArr[0]=[{}]",sm);
					atlog.info("-- gArr[0]=[{}]",charcnv.BIG5bytesUTF8str(gl_arr.get(0)));
					//----
					tital.setValue("txamt", sm);
					log.debug("txamt[{}]", sm);
					atlog.info("TITA_BASIC.txamt=[{}]",sm);
					p1885text.setValueLtoRfill("glcomm", "00", (byte)' ');
					if (tital.ChkCrdb(tx_area.get("avebal")) > 0)
						p1885text.setValue("snpbbal", "+");
					else
						p1885text.setValue("snpbbal", "-");
					sm = tx_area.get("avebal");

					sm = "000" + tital.FilterMsr(sm, '-', '0');
					p1885text.setValue("npbbal", sm);
					p1885text.setValue("delcnt", String.format("%04d", gl_arr.size()));
					p1885text.setValue("nbday", tx_area.get("nbday"));
					p1885text.setValue("nbseq", tx_area.get("nbseq"));
					p1885text.setValue("kinbr", tx_area.get("kinbr"));
					p1885text.setValue("nbno", this.no);
					p1885text.setValue("lineno", tx_area.get("lineno"));
					p1885text.setValue("pageno", tx_area.get("pageno"));
					p1885text.setValue("end", "$");

					rtn = tital.mkTITAmsg(tital.getTitalabel(), p1885text.getP1885Titatext());
					log.debug("P1885 tita [{}]", new String(rtn));
				//20200810
					//20200902 change to currentifig
				} else if (currentifig == TXP.C0099TYPE) {
//					memcpy(TITA_BASIC.aptype,"C00",sizeof(TITA_BASIC.aptype)+sizeof(TITA_BASIC.apcode));
//					memcpy(TITA_BASIC.stxno,"99",sizeof(TITA_BASIC.stxno));
//					memcpy(TITA_BASIC.ptype,"0",sizeof(TITA_BASIC.ptype));
//					memcpy(TITA_BASIC.dscpt,"S99  ",sizeof(TITA_BASIC.dscpt));
//					memset(TITA_BASIC.actno,'0',sizeof(TITA_BASIC.actno));
//					memcpy(&TITA_TEXT,"        ",8);
					tital.setValue("apcode", "00");
					tital.setValue("crdb", "0");
					tital.setValue("nbcd", "0");

					tital.setValue("aptype", "C00");
					tital.setValue("stxno", "99");
					tital.setValue("ptype", "0");
					tital.setValue("dscpt", "S99  ");
					tital.setValueLtoRfill("actno", "0", (byte) '0');
					tital.setValue("dscpt", "S99  ");
					String tita_text = "        ";
					rtn = tital.mkTITAmsg(tital.getTitalabel(), tita_text.getBytes());
					log.debug("TxFlow : DataINQ() -- rtn=[{}]", new String(rtn));
				}
				//----
			} else { //iVal == RECVFHOST
				//***** Receive P001 TOTA and check error *****//
				if (currentifig == TXP.PBTYPE)
				{
				}
				//***** Receive Q980 TOTA and check error *****//
				else if (currentifig == TXP.FCTYPE)
				{
				}
				//***** Receive P885 TOTA and check error *****//
				else if (currentifig == TXP.GLTYPE)
				{
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error("telegram  DataDEL error on tita label: {}", e.getMessage());
		}
		return rtn;
	}

	//20200723
//	private byte[] DataINQ(int iVal, int ifig, String dCount, String con, byte[] opttotatext)
//20200902	private byte[] DataINQ(int iVal, int ifig, String dCount, byte[] opttotatext)
	private byte[] DataINQ(int iVal, int currentifig, String dCount, byte[] opttotatext)
	{
		// optotatext only used while iVal == TXP.RECVFHOST mode
		byte[] rtn = null;
		P0080TEXT p0080text = null;
		Q0880TEXT q0880text = null;
		P0880TEXT p0880text = null;
		int begin = Integer.parseInt(dCount);
		int totCnt = Integer.parseInt(con);
		int inqiLine = Integer.parseInt(tx_area.get("cline").trim());
		log.debug("1--->iVal={} ifig={} begin=>{} totCnt={} inqiLine={}", iVal, currentifig, begin, totCnt, inqiLine);
		if (opttotatext != null && opttotatext.length > 0)
			log.debug("1.1--->opttotatext.length=>{}", opttotatext.length);
		try {
			if (iVal == TXP.SENDTHOST) { // send to host
				if (currentifig == TXP.PBTYPE) {
					p0080text = new P0080TEXT();
					boolean p0080titatextrtn = p0080text.initP0080TitaTEXT((byte) '0');
					log.debug("p0080titatextrtn.initP0080TitaTEXT p0080titatextrtn={}", p0080titatextrtn);
					tital.setValue("aptype", "P");
					tital.setValue("apcode", "00");
					tital.setValue("stxno", "80");
					tital.setValue("dscpt", "S80  ");
					tital.setValueLtoRfill("actno", tx_area.get("account"), (byte) ' ');
					if (tital.ChkCrdb(this.msrbal) > 0)
						tital.setValue("crdb", "1");
					else
						tital.setValue("crdb", "0");
					String sm = this.msrbal.substring(1);
					sm = tital.FilterMsr(sm, '-', '0');
					tital.setValue("txamt", sm);
					tital.setValue("ver", "02");
					this.pbavCnt = 999;
//					this.pbavCnt = 1;
					p0080text.setValueRtoLfill("pbcnt", String.format("%d", this.pbavCnt), (byte) '0');
					p0080text.setValue("bkseq", this.bkseq);
					// 要求筆數(若該頁剩餘筆數 < 6，則為"剩餘筆數")
					if ((inqiLine - 1 + begin) + 6 > 24) {
						int reqcnt = 24 - (inqiLine - 1 + begin);
						atlog.info("reqcnt = [{}]",reqcnt);
						p0080text.setValueRtoLfill("reqcnt", Integer.toString(reqcnt), (byte) '0');
					} else {
						// 若剩餘要求之未登摺筆數 < 6，則為"剩餘之未登摺筆數"，否則為6
						if (totCnt > 0 && begin + 6 > totCnt) {
							log.debug("TxFlow : () -- reqcnt 2 ={}", Integer.toString(totCnt - begin));
							p0080text.setValueRtoLfill("reqcnt", Integer.toString(totCnt - begin), (byte) '0');
						} else {
							log.debug("TxFlow : () -- reqcnt 3 =6");
							p0080text.setValueRtoLfill("reqcnt", Integer.toString(6), (byte) '0');
						}
					}
					//未登摺之第幾筆
					log.debug("--->begin=>{}", begin);
					if (begin == 0)
						p0080text.setValueRtoLfill("begin", Integer.toString(1), (byte) '0');
					else
						p0080text.setValueRtoLfill("begin", Integer.toString(begin + 1), (byte) '0');
					rtn = tital.mkTITAmsg(tital.getTitalabel(), p0080text.getP0080Titatext());
				} else if (currentifig == TXP.FCTYPE) {  //20200902 change to currentifig
					q0880text = new Q0880TEXT();
					boolean q0880titatextrtn = q0880text.initQ0880TitaTEXT((byte) '0');
					log.debug("q0880titatextrtn.initQ0880TitaTEXT q0880titatextrtn={}", q0880titatextrtn);
					tital.setValue("aptype", "Q");
					tital.setValue("apcode", "08");
					tital.setValue("stxno", "80");
					tital.setValue("dscpt", "S80  ");
					tital.setValueLtoRfill("actno", tx_area.get("account"), (byte) ' ');
					tital.setValue("crdb", "0");
					tital.setValue("nbcd", "3");
					tital.setValue("txamt", this.msrbal.substring(1));
					q0880text.appendTitaText("newseq", "                              ".getBytes());
					q0880text.setValue("retur", "0");
					q0880text.setValue("rbrno", this.brws.substring(0, 3));
					q0880text.setValue("acbrno", this.brws.substring(0, 3));
					q0880text.setValue("aptype", "14");
					q0880text.setValue("corpno", "00");
					q0880text.setValue("actfg", "1");
					this.pbavCnt = 999;
					q0880text.setValueRtoLfill("pbcnt", String.format("%d", this.pbavCnt), (byte) '0');
					q0880text.setValue("pbver", this.pbver);
					q0880text.setValue("txnos", String.format("%04d",6));
					if (begin == 0)
						q0880text.setValue("begin",String.format("%04d",1));
					else
						q0880text.setValue("begin",String.format("%04d", begin + 1));

					if (begin == 0) {
						q0880text.setValue("txday","00000000");
						q0880text.setValue("txseq","000000");
					}
					else {
						q0880text.setValue("txday",tx_area.get("txday"));
						q0880text.setValue("txseq",tx_area.get("txseq"));
					}
					q0880text.setValue("pbcol",this.cline);
					q0880text.setValue("pbpage",this.cpage);
					tx_area.put("pbcol", this.cline);
					tx_area.put("pbpage", this.cpage);
					rtn = tital.mkTITAmsg(tital.getTitalabel(), q0880text.getQ0880Titatext());
				} else if (currentifig == TXP.GLTYPE) {  //20200902 change to currentifig
					p0880text = new P0880TEXT();
					boolean p0880titatextrtn = p0880text.initP0880TitaTEXT((byte) '0');
					log.debug("p0880titatextrtn.initP0880TitaTEXT p0880titatextrtn={}", p0880titatextrtn);
					tital.setValue("aptype", "P");
					tital.setValue("apcode", "08");
					tital.setValue("stxno", "80");
					tital.setValue("dscpt", "S80  ");
					tital.setValueLtoRfill("actno", tx_area.get("account"), (byte) ' ');

					if (tital.ChkCrdb(this.msrbal) > 0)
						tital.setValue("crdb", "1");
					else
						tital.setValue("crdb", "0");

					tital.setValue("nbcd", "8");
					// 20080905 , prepare txamt
					String sm = "0" + this.msrbal;

					sm = tital.FilterMsr(sm, '-', '0');
					tital.setValue("txamt", sm);
					this.pbavCnt = 999;
					p0880text.setValueRtoLfill("pbcnt", String.format("%d", this.pbavCnt), (byte) '0');
					//GL-COMM共用(前兩位為0, 後48位為空白)
					p0880text.setValueLtoRfill("glcomm", "00".getBytes(), (byte) ' ');
					//要求筆數(若該頁剩餘筆數 < 6，則為"剩餘筆數")
					if ((inqiLine - 1 + begin) + 6 > 24) {
						int reqcnt = 24 - (inqiLine - 1 + begin);
						log.debug("TxFlow : DataINQ() -- reqcnt 1 ={}", Integer.toString(reqcnt));
						p0880text.setValueRtoLfill("reqcnt", Integer.toString(reqcnt), (byte) '0');
					} else {
						// 若剩餘要求之未登摺筆數 < 6，則為"剩餘之未登摺筆數"，否則為6
						if (totCnt > 0 && begin + 6 > totCnt) {
							log.debug("TxFlow : () -- reqcnt 2 ={}", Integer.toString(totCnt - begin));
							p0880text.setValueRtoLfill("reqcnt", Integer.toString(totCnt - begin), (byte) '0');
						} else {
							log.debug("TxFlow : () -- reqcnt 3 =6");
							p0880text.setValueRtoLfill("reqcnt", Integer.toString(6), (byte) '0');
						}
					}
					//未登摺之第幾筆
					log.debug("GL <><><><>--->begin=>{} cline=[{}]", begin, this.cline);
					if (begin == 0)
						p0880text.setValueRtoLfill("begin", Integer.toString(1), (byte) '0');
					else
						p0880text.setValueRtoLfill("begin", Integer.toString(begin + 1), (byte) '0');
					p0880text.setValue("nbno",this.no);
					//20200701
	//				this.cline = tx_area.get("cline").trim();
					//---
					p0880text.setValue("lineno",this.cline);
					p0880text.setValue("pageno",this.cpage);
					tx_area.put("nbno", this.no);
					tx_area.put("lineno", this.cline);
					tx_area.put("pageno", this.cpage);
					rtn = tital.mkTITAmsg(tital.getTitalabel(), p0880text.getP0880Titatext());
					log.debug("TxFlow : DataINQ() -- rtn={}", new String(rtn));
				//20200810
				} else if (currentifig == TXP.C0099TYPE) {   //20200902 change to currentifig
//					memcpy(TITA_BASIC.aptype,"C00",sizeof(TITA_BASIC.aptype)+sizeof(TITA_BASIC.apcode));
//					memcpy(TITA_BASIC.stxno,"99",sizeof(TITA_BASIC.stxno));
//					memcpy(TITA_BASIC.ptype,"0",sizeof(TITA_BASIC.ptype));
//					memcpy(TITA_BASIC.dscpt,"S99  ",sizeof(TITA_BASIC.dscpt));
//					memset(TITA_BASIC.actno,'0',sizeof(TITA_BASIC.actno));
//					memcpy(&TITA_TEXT,"        ",8);

					tital.setValue("apcode", "00");
					tital.setValue("crdb", "0");
					tital.setValue("nbcd", "0");

					tital.setValue("aptype", "C00");
					tital.setValue("stxno", "99");
					tital.setValue("ptype", "0");
					tital.setValue("dscpt", "S99  ");
					tital.setValueLtoRfill("actno", "0", (byte) '0');
					tital.setValue("dscpt", "S99  ");
					String tita_text = "        ";
					rtn = tital.mkTITAmsg(tital.getTitalabel(), tita_text.getBytes());
					log.debug("TxFlow : DataINQ() -- rtn=[{}]", new String(rtn));
				}
				//----
			} else { //iVal == RECVFHOST
				if (currentifig == TXP.PBTYPE) {   //20200902 change to currentifig
					rtn = new byte[0];
					p0080text = new P0080TEXT();
					byte[] texthead = Arrays.copyOfRange(opttotatext, 0, p0080text.getP0080TotaheadtextLen());
					p0080text.copyTotaHead(texthead);
					con = new String(p0080text.getHeadValue("nbcnt"));
					log.debug("P0080totahead rtn={} tota.nbcnt={} tota.nbdelcnt={}",
							new String(texthead), con,
							new String(p0080text.getHeadValue("nbdelcnt")));
					if (Integer.parseInt(con) > this.pbavCnt) {
						//if (全部未登摺之資料筆數 > 存摺總剩餘可列印之資料筆數) Eject!
						SetSignal(firstOpenConn, !firstOpenConn, "0000000000","0000000001");
						Sleep(1000);
						amlog.info("[{}][{}][{}]:54全部未登摺之資料筆數[{}] > 存摺總剩餘可列印之資料筆數[{}]！", brws, pasname, this.account, con, this.pbavCnt);
						rtn = new byte[0];
					} else {
						int nCnt = Integer.parseInt(new String(p0080text.getHeadValue("nbdelcnt")));
						int iCnt = Integer.parseInt(dCount);
						atlog.info("iCnt=[{}] nCnt=[{}]", iCnt, nCnt);
						//20200523
						tx_area.put("bkseq", new String(p0080text.getHeadValue("totabkseq")));
						//----
						if (opttotatext.length > texthead.length) {
							int j = 0;
							byte[] text = Arrays.copyOfRange(opttotatext, p0080text.getP0080TotaheadtextLen(), opttotatext.length);
//							log.debug("{} {} {} :TxFlow : () -- iCnt=[{}] nCnt=[{}] text.length={}", brws, catagory, account, iCnt, nCnt, text.length);
							if (text.length % p0080text.getP0080TotatextLen() == 0)
								j = text.length / p0080text.getP0080TotatextLen();
							log.debug("{} {} {} :TxFlow : () -- iCnt=[{}] nCnt=[{}] text.length={} j={}", brws, catagory, account, iCnt, nCnt, text.length, j);
							if (j == nCnt) {
								p0080text.copyTotaText(text, j);
								byte[] plus = {'+'};
								for (int i = 0; i < j; i++) {
									double dTxamt = Double.parseDouble(new String(p0080text.getTotaTextValue("txamt", i))) / 100.0;
									if (dTxamt == 0)
										p0080text.setTotaTextValue("stxamt", plus, i);
									atlog.info("m_pArr[{}]=[{}]", i, new String(p0080text.getTotaTexOc(i)));
									log.info("i = [{}] txamt={} dTxamt={} stxamt={} text=[{}]", i, new String(p0080text.getTotaTextValue("txamt", i)), dTxamt, new String(p0080text.getTotaTextValue("stxamt", i)), new String(p0080text.getTotaTexOc(i)));
															}
								this.pb_arr.addAll(p0080text.getTotaTextLists());
								rtn = text;
								log.debug("{} {} {} :TxFlow : () -- pb_arr.size={}", brws, catagory, account, pb_arr.size());
								iCnt = iCnt + nCnt;
								this.dCount = String.format("%03d", iCnt);
								log.debug("{} {} {} :TxFlow : after () -- dCount=[{}]", brws, catagory, account, this.dCount);
								//Print Data
								// 20200523
								tx_area.put("snpbbal", new String(p0080text.getTotaTextValue("spbbal", nCnt - 1)));
								tx_area.put("npbbal", new String(p0080text.getTotaTextValue("pbbal", nCnt - 1)));
								log.debug("{} {} {} :TxFlow : () -- bkseq=[{}] snbbal=[{}] nbbal=[{}]", brws, catagory, account,
										tx_area.get("bkseq"), tx_area.get("snpbbal"), tx_area.get("npbbal"));
								// -----
							} else
								rtn = new byte[0];
						} else
							rtn = new byte[0];
					}
				} else if (currentifig == TXP.FCTYPE) { //20200902 change to currentifig
					rtn = new byte[0];
					q0880text = new Q0880TEXT();
					byte[] texthead = Arrays.copyOfRange(opttotatext, 0, q0880text.getQ0880TotaheadtextLen());
					q0880text.copyTotaHead(texthead);
					con = new String(q0880text.getHeadValue("nbcnt"));
					log.debug("Q0880totahead rtn={} tota.nbcnt={}",
							new String(texthead), con);
					if (Integer.parseInt(con) > this.pbavCnt) {
						//if (全部未登摺之資料筆數 > 存摺總剩餘可列印之資料筆數) Eject!
						SetSignal(firstOpenConn, !firstOpenConn, "0000000000","0000000001");
						Sleep(1000);
						amlog.info("[{}][{}][{}]:54全部未登摺之資料筆數[{}] > 存摺總剩餘可列印之資料筆數[{}]！", brws, pasname, this.account, con, this.pbavCnt);
						rtn = new byte[0];
					} else {
						totCnt = Integer.parseInt(con);
						atlog.info("begin=[{}] totCnt=[{}]", begin,	totCnt);
						if (opttotatext.length > texthead.length) {
							int j = 0;
							byte[] text = Arrays.copyOfRange(opttotatext, q0880text.getQ0880TotaheadtextLen(),
									opttotatext.length);
//							log.debug("{} {} {} :TxFlow : () -- totCnt=[{}] text.length={} [{}]", brws, catagory, account,
//									totCnt, text.length, new String(text));
							if (text.length % q0880text.getQ0880TotatextLen() == 0)
								j = text.length / q0880text.getQ0880TotatextLen();
							log.debug("{} {} {} :TxFlow : () -- totCnt=[{}] text.length={} j={}", brws, catagory,
									account, totCnt, text.length, j);
							//20200523
							if (j >= (totCnt % 6)) {
								q0880text.copyTotaText(text, j);
								//----
								if (begin < totCnt) {
									int dataCnt = 0;
									if ((totCnt - begin) >= 6)
										dataCnt = 6;
									else
										dataCnt = totCnt - begin;
									// 20080828 , 滿24筆時 dataCnt <= 6
									int iCur, iLeft;
									iCur = iLine + begin;
									iLeft = 25 - iCur;
									dataCnt = (dataCnt < iLeft) ? dataCnt : iLeft;
									atlog.info("dataCnt=[{}]",dataCnt);
									int i = 0;
									for (i = 0; i < dataCnt; i++) {
										// 20080923 , txday[0] == '0'
										if (q0880text.getTotaTextValue("totatxday", i) == null
												|| new String(q0880text.getTotaTextValue("totatxday", i)).trim()
														.length() == 0
												|| Integer.parseInt(
														new String(q0880text.getTotaTextValue("totatxday", i))) == 0)
											break;
										this.fc_arr.add(q0880text.getTotaTexOc(i));
										//20200903 convert to utf8 charcnv.BIG5bytesUTF8str
//										atlog.info("m_fArr[{}]=[{}]", begin + i, new String(q0880text.getTotaTexOc(i)));
										atlog.info("m_fArr[{}]=[{}]", begin + i, charcnv.BIG5bytesUTF8str(q0880text.getTotaTexOc(i)));
										//----
										//20200523
										log.info("fc_arr [{}]=[{}]", begin + i, new String(q0880text.getTotaTexOc(i)));
										//----
									}
									if (i == 0) {
										atlog.info("m_fArr data null");
										//20200523
										log.info("i == 0");
										//----
									} else {
										tx_area.put("txday", new String(q0880text.getTotaTextValue("totatxday", i - 1)));
										tx_area.put("txseq", new String(q0880text.getTotaTextValue("totatxseq", i - 1)));
										tx_area.put("pbbal", new String(q0880text.getTotaTextValue("pbbal", i - 1)));
										atlog.info("tx_area->txday=[{}] tx_area->txseq=[{}]",tx_area.get("txday"), tx_area.get("txseq"));
										rtn = text;
										log.debug("{} {} {} :TxFlow : () -- fc_arr.size={}", brws, catagory, account,
												fc_arr.size());
										this.dCount = String.format("%03d", begin + i);
										log.debug("{} {} {} :TxFlow : () -- this.dCount={}", brws, catagory, account,this.dCount);
									}
								} else {
									rtn = new byte[0];
								}
							}
						} else {
							rtn = new byte[0];
						}
					}
				} else if (currentifig == TXP.GLTYPE) {  //20200902 change to currentifig
					rtn = new byte[0];
					p0880text = new P0880TEXT();
					byte[] texthead = Arrays.copyOfRange(opttotatext, 0, p0880text.getP0880TotaheadtextLen());
					p0880text.copyTotaHead(texthead);
					con = new String(p0880text.getHeadValue("nbcnt"));
					log.debug("P0880totahead rtn={} tota.nbcnt={}",
							new String(texthead), con);
					if (Integer.parseInt(con) > this.pbavCnt) {
						//if (全部未登摺之資料筆數 > 存摺總剩餘可列印之資料筆數) Eject!
						SetSignal(firstOpenConn, !firstOpenConn, "0000000000","0000000001");
						Sleep(1000);
						amlog.info("[{}][{}][{}]:54全部未登摺之資料筆數[{}] > 存摺總剩餘可列印之資料筆數[{}]！", brws, pasname, this.account, con, this.pbavCnt);
						rtn = new byte[0];
					} else {
						totCnt = Integer.parseInt(con);
						atlog.info("begin=[{}] totCnt=[{}]",begin,totCnt);
						if (opttotatext.length > texthead.length) {
							int j = 0;
							byte[] text = Arrays.copyOfRange(opttotatext, p0880text.getP0880TotaheadtextLen(),
									opttotatext.length);
//							log.debug("{} {} {} :TxFlow : () -- totCnt=[{}] text.length={} getP0880TotatextLen()=[{}]", brws, catagory, account,
//									totCnt, text.length, p0880text.getP0880TotatextLen());
//							if (text.length % p0880text.getP0880TotatextLen() == 0)
							j = text.length / p0880text.getP0880TotatextLen();
							
							log.debug("{} {} {} :TxFlow : () -- totCnt=[{}] text.length={} j={}", brws, catagory,
									account, totCnt, text.length, j);
							//20200723
							if (j >= (totCnt % 6)) {
								p0880text.copyTotaText(text, j);
							//----
								if (begin < totCnt) {
									int dataCnt = 0;
									if ((totCnt - begin) >= 6)
										dataCnt = 6;
									else
										dataCnt = totCnt - begin;
									// 20080828 , 滿24筆時 dataCnt <= 6
									int iCur, iLeft;
									iCur = iLine + begin;
									iLeft = 25 - iCur;
									dataCnt = (dataCnt < iLeft) ? dataCnt : iLeft;
									atlog.info("dataCnt=[{}]",dataCnt);
									int i = 0;
									for (i = 0; i < dataCnt; i++) {
										// 20080923 , txday[0] == '0'
										if (p0880text.getTotaTextValue("txday", i) == null
												|| new String(p0880text.getTotaTextValue("txday", i)).trim()
														.length() == 0
												|| Integer.parseInt(
														new String(p0880text.getTotaTextValue("txday", i))) == 0)
											break;
										this.gl_arr.add(p0880text.getTotaTexOc(i));
										//20200903 convert to utf8 charcnv.BIG5bytesUTF8str
//										atlog.info("m_gArr[{}]=[{}]",begin + i, new String(p0880text.getTotaTexOc(i)));
										atlog.info("m_gArr[{}]=[{}]",begin + i, charcnv.BIG5bytesUTF8str(p0880text.getTotaTexOc(i)));
										//20200523
										log.info("gl_arr [{}]=[{}]",begin + i, new String(p0880text.getTotaTexOc(i)));
										//----
									}
									if (i == 0) {
										atlog.info("m_gArr data null");
										//20200523
										log.info("i == 0");
										//----
									} else {
										tx_area.put("txday", new String(p0880text.getTotaTextValue("txday", i - 1)));
										tx_area.put("nbseq", new String(p0880text.getTotaTextValue("nbseq", i - 1)));
										tx_area.put("avebal", new String(p0880text.getTotaTextValue("avebal", i - 1)));
										log.debug("[{} {} {} : DataINQ() -- tx_area->txday=[{}] tx_area->nbseq=[{}] tx_area->avebal=[{}]",
												brws, catagory, account, tx_area.get("txday"), tx_area.get("nbseq"), tx_area.get("avebal"));
										rtn = text;
										log.debug("{} {} {} :TxFlow : () -- gl_arr.size={}", brws, catagory, account,
												gl_arr.size());
										this.dCount = String.format("%03d", begin + i);
										log.debug("{} {} {} :TxFlow : () -- this.dCount={}", brws, catagory, account,this.dCount);
									}
								} else {
									rtn = new byte[0];
								}
							}
						} else {
							rtn = new byte[0];
						}
					}
				}
			}
			//20200902 change to currentifig
			if (currentifig == TXP.PBTYPE)
				log.debug("4.1--->pb_arr.size=[{}]", pb_arr.size());
			else if (currentifig == TXP.FCTYPE)
				log.debug("4.2--->fc_arr.size=[{}]", fc_arr.size());
			else if (currentifig == TXP.GLTYPE)
				log.debug("4.3--->gl_arr.size=[{}]", gl_arr.size());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error("telegram   error on tita label: {}", e.getMessage());
		}
		return rtn;
	}

	/************************************************************************
	*	Send_Recv() : Send TX to Host /                             *
	*                Receive Data from Host                        *
	*   function    : 傳送INQ/UPD交易上中心/接收中心資料                  * 
	*   parameter 1 : AP type -- 1:PB / 2:FC / 3:GL                *
	*   parameter 2 : data function -- 1:INQ / 2:DEL               *
	*   parameter 3 : total NB count                               *
	*   parameter 4 : original MSR's balance                       *
	*   return_code : = 0 - NORMAL                                 *
	*                 < 0 - ERROR                                  *
	*   delete data field error -2                                 *              
	*  send message length == 0 -1                                 *
	*  receive TITA/TOTA telegram KEY or message format error -3   *
	*************************************************************************/
	private int Send_Recv(int iflg, int ifun, String _con, String mbal)
	{
		//20200724
		this.con = "000";
		if (ifun == TXP.INQ) {
			pb_arr.clear();
			fc_arr.clear();
			gl_arr.clear();
		}
		//----
		int rtn = 0;

		do {
			log.debug("------------------------------------this.curState=[{}] this.Send_Recv_DATAInq=[{}] this.passSNDANDRCVTLM=[{}] alreadySendTelegram=[{}] isTITA_TOTA_START()=[{}]" , this.curState, this.Send_Recv_DATAInq, this.passSNDANDRCVTLM, alreadySendTelegram, this.isTITA_TOTA_START());

			if (this.curState == SNDANDRCVTLM || this.curState == SNDANDRCVDELTLM) {
				this.iLine = Integer.parseInt(tx_area.get("cline").trim());
//20200724				con = "000";
//				this.con = "000";
				this.iCon = Integer.parseInt(this.con.trim());
/*				if (ifun == TXP.INQ) {
					pb_arr.clear();
					fc_arr.clear();
					gl_arr.clear();
				}*/
				tital = new TITATel();
				boolean titalrtn = tital.initTitaLabel((byte) '0');
				log.debug("tital.initTitaLabel rtn={}", titalrtn);

				try {
					tital.setValue("brno", this.brws.substring(0, 3));
					tital.setValue("wsno", this.brws.substring(3));
					try {
						this.setSeqNo = Integer
								.parseInt(FileUtils.readFileToString(this.seqNoFile, Charset.defaultCharset())) + 1;
						//20210630 MatsudairaSyuMe make sure seqno Exceed the maximum 
						if (this.setSeqNo > 99999999) //20221123 99999->99999999
							this.setSeqNo = 1;
						FileUtils.writeStringToFile(this.seqNoFile, Integer.toString(this.setSeqNo),
								Charset.defaultCharset());
					} catch (Exception e) {
						log.error("ERROR!!! update new seq number string {} error {}", this.setSeqNo, e.getMessage());
						 //20220708 MatsudairaSyuMe
						FileUtils.writeStringToFile(this.seqNoFile, "0",	Charset.defaultCharset());
						this.setSeqNo = 0;
						//----
					}
					tital.setValueRtoLfill("txseq", String.format("%d", this.setSeqNo), (byte) '0');
					tital.setValue("trancd", "CB");
					tital.setValue("wstype", "0");
					tital.setValue("titalrno", "00");
					tital.setValueLtoRfill("txtype", " ", (byte) ' ');
					tital.setValue("spcd", "0");
					tital.setValue("nbcd", "0");
					tital.setValue("hcode", "0");
					tital.setValue("trnmod", "0");
					tital.setValue("sbtmod", "0");
					tital.setValue("curcd", "00");
					tital.setValue("pseudo", "1");
					if (!new String(this.fepdd).equals("  ")) {
						tital.setValue("fepdd", this.fepdd);
					}
					atlog.info("fepdd=[{}]",new String(this.fepdd));
					tital.setValue("acbrno", this.brws.substring(0, 3));
					if (ifun == TXP.INQ) {
						if (this.iCount == 0) {
							amlog.info("[{}][{}][{}]:03中心存摺補登資料讀取中...", brws, pasname, this.account);
						}
						//20200506
						this.startTime = System.currentTimeMillis();
						//----

						// Send Inquiry Request
						this.resultmsg = null;
						//20200724
//						resultmsg = DataINQ(TXP.SENDTHOST, iflg, this.dCount, con);
						resultmsg = DataINQ(TXP.SENDTHOST, iflg, this.dCount);
						//----
						if (resultmsg == null || resultmsg.length == 0) {
							atlog.info("iMsgLen = 0");
							amlog.info("[{}][{}][{}]:31傳送之訊息長度為０！", brws, pasname, this.account);							
							rtn = -1;
						}
					} else {
						amlog.info("[{}][{}][{}]:04中心存摺已補登資料刪除中...", brws, pasname, this.account);
						//20200506
						this.startTime = System.currentTimeMillis();
						//----
						this.resultmsg = null;
						resultmsg = DataDEL(TXP.SENDTHOST, iflg, mbal);
						if (resultmsg == null || resultmsg.length == 0) {
							atlog.info("iMsgLen = 0");
							amlog.info("[{}][{}][{}]:31傳送之訊息長度為０！", brws, pasname, this.account);							
							rtn = -1;
						}
					}
					this.curState = SETREQSIG;
					//20200403
//20200619					SetSignal(firstOpenConn, firstOpenConn, "0000000000", "0010000000");
					//----
					atlog.info("TITA_TEXT=[{}]",new String(resultmsg));
					//20210116 MataudairaSyuMe  for incoming TOTA telegram
					this.telegramKey = dataUtil.getTelegramKey(resultmsg);
					log.info("telegramKey=[{}]", this.telegramKey);
					//----
					//20200724
					if (this.iCount > 0 && ifun == TXP.INQ) {
						this.curState = SENDTLM;
						//20210112 mark by MatsudairaSyuMe TITA_TOTA_START flag checking change to PrtCli
						log.debug("{} {} {} AutoPrnCls : --change start process telegram dispatcher.isTITA_TOTA_START()={} alreadySendTelegram ={} this.iCount=[{}]", brws, catagory, account, this.isTITA_TOTA_START(), this.alreadySendTelegram, this.iCount);
						//----
					} else {
						if (SetSignal(firstOpenConn, !firstOpenConn, "0000000000", "0010000000")) {
							this.curState = RECVTLM;
							log.debug("{} {} {} AutoPrnCls : --change start process telegram", brws, catagory, account);
						} else {
							this.curState = WAITSETREQSIG;
							log.debug("{} {} {} AutoPrnCls : --change wait Set Signal for request data", brws, catagory,
									account);
						}
					}
					//----
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					log.error("telegram compose error on tita label: {}", e.getMessage());
				}
			} else if (this.curState == WAITSETREQSIG) {
				if (SetSignal(!firstOpenConn, !firstOpenConn, "0000000000", "0010000000")) {
					this.curState = SENDTLM;
					//20210112 mark by MatsudairaSyuMe TITA_TOTA_START flag checking change to PrtCli
					log.debug("{} {} {} AutoPrnCls : --change start process telegram dispatcher.isTITA_TOTA_START()={} alreadySendTelegram ={} ", brws, catagory, account, this.isTITA_TOTA_START(), this.alreadySendTelegram);
					//----
				} else {
					log.debug("{} {} {} AutoPrnCls : --change wait Set Signal for request data", brws, catagory,
							account);
				}
			} else if (this.curState == SENDTLM || this.curState == RECVTLM) {
				//20210112 mark by MatsudairaSyuMe TITA_TOTA_START flag checking change to PrtCli
				if (this.curState == SENDTLM  && !this.isTITA_TOTA_START() && !alreadySendTelegram) {
					//20210628 change to use MDP
					//----
					//not yet send telegram send firstly
					//alreadySendTelegram = dispatcher.sendTelegram(resultmsg);
					//20220809 MatsudairaSyuMe
					//alreadySendTelegram = true;
//					ZMsg request = new ZMsg();
//					request.append(resultmsg);
//					clientSession.send("fas", request);
					//20210112 add by MatsudairaSyuMe TITA_TOTA_START flag checking change to PrtCli
//					if (alreadySendTelegram == false && this.dispatcher.isCurrConnNull())
					//20210628 change to use MDP take off this.dispatcher.isCurrConnNull()
					//20220809 change to use RouteConnection
					this.rtelem = null; //20220809 MatsudairaSyuMe reset the receive buffer
					alreadySendTelegram = clientSession.send(resultmsg);
					if (alreadySendTelegram == false)
					{
						log.debug("can not get connect from pool !!!!!!!!!!!!!!!!!");
						this.curState = EJECTAFTERPAGEERROR;
					} else {
						//----
						if (alreadySendTelegram)
							this.setTITA_TOTA_START(true);
						// ----
						if (ifun == 1 && iCount == 0) {
							amlog.info("[{}][{}][{}]:05中心存摺補登資料接收中...", brws, pasname, this.account);
						}
						this.curState = RECVTLM;
					}
					//20200506
//					this.startTime = System.currentTimeMillis();
					//----
					//20210112 mark by MatsudairaSyuMe TITA_TOTA_START flag checking change to PrtCli
				} else if (this.isTITA_TOTA_START() && alreadySendTelegram) {
					//----
					//20210116 MatshdairaSyuMe
//					this.rtelem = dispatcher.getResultTelegram();
					//20210628 change to use MDP
					//this.rtelem = dispatcher.getResultTelegram(this.telegramKey);
					log.debug("{} {} {} AutoPrnCls : clientSession.recv() start DEBUG-TEST!!! iFirst=[{}] iEnd=[{}]!!!", brws, catagory, account, this.iFirst, this.iEnd); //20220923 debug test
					/* 20220809 MatsudairaSyuMe
					ZMsg reply = null;
					reply = clientSession.recv();
					if (reply != null) {
						this.rtelem = reply.pop().getData();
						reply.destroy();
					}
					*/
					//20220905 MatsudairaSyuMe change to us clientSession as dispatcher
					//this.rtelem = clientSession.recv();
					this.rtelem = clientSession.recv(this.telegramKey);
					//20220923 debug-test if (this.iFirst > 0 && this.rtelem != null) {this.rtelem = null;  log.debug("!!!DEBUG-TEST!!! iFirst=[{}] this.relem set to null for timeout testing ", this.iFirst);}//20220923 DEBUG-TEST
					//----20220905
					if (this.rtelem != null) {
						//20210112 mark by MatsudairaSyuMe TITA_TOTA_START flag checking change to PrtCli
						this.setTITA_TOTA_START(false);
						log.debug(
								"{} {} {} :AutoPrnCls : process telegram isTITA_TOTA_START={} alreadySendTelegram={} get {} [{}]",
								brws, catagory, account, this.isTITA_TOTA_START(), alreadySendTelegram,
								rtelem.length, new String(this.rtelem));
						//----
						//20220818 MatsudairaSyuMe check receive telegram transaction key
						if (this.rtelem.length > 15) {
							String recvTelegramKey = new String(Arrays.copyOfRange(rtelem, 0, this.telegramKey.length()));
							if (!recvTelegramKey.equals(this.telegramKey)) {
								//received TITA/TOTA transaction sequence no. not equals
								log.error("TITA/TOTA  transaction sequence no not equal TITA=[{}] TOTA=[{}]", this.telegramKey, recvTelegramKey);
								amlog.info("[{}][{}][{}]:05中心存摺補登資料接收電文傳輸編號錯誤錯誤 TITA[{}] TOTA [{}]", brws, pasname, this.account, this.telegramKey, recvTelegramKey);
								this.rtelem = null;
								this.resultmsg = null;
								SetSignal(firstOpenConn, firstOpenConn, "0000000000", "0000000001");
								if (SetSignal(!firstOpenConn, firstOpenConn, "0000000000", "0000000001")) {
									log.debug(
											"{} {} {} AutoPrnCls : --keep cheak barcode after Set Signal after check telegramKey",
											brws, catagory, account);
								} else {
									log.debug(
											"{} {} {} AutoPrnCls : --keep cheak barcode after Set Signal after check telegramKey",
											brws, catagory, account);
								}
								this.curState = EJECTAFTERPAGEERROR;
								rtn = -3;
								//20220819 MAtsudairaSyuMe cancel the Constants.outgoingTelegramKeyMap on RouteServerHandler
								clientSession.sendCANCEL(this.telegramKey.getBytes());
								log.debug("[{}][{}][{}] 中心存摺{}資料接收電文傳輸編號錯誤錯誤 TITA Key=[{}] TOTA Key=[{}] send cancel to RouteServer", brws, pasname, this.account, (ifun == TXP.INQ) ? "補登" : "刪除", this.telegramKey, recvTelegramKey);
								trace.error("[{}][{}][{}] 中心存摺{}資料接收電文傳輸編號錯誤錯誤 TITA Key=[{}] TOTA Key=[{}] send cancel to RouteServer", brws, pasname, this.account, (ifun == TXP.INQ) ? "補登" : "刪除", this.telegramKey, recvTelegramKey);
								//----
								break;
							} else
								log.info("TITA/TOTA  transaction sequence TITA=[{}] TOTA=[{}] matched !!!", this.telegramKey, recvTelegramKey);
						} else {
							//received telegram error
							log.error("received TOTA message format error");
							amlog.info("[{}][{}][{}]:05中心存摺補登資料接收電文格式錯誤！！", brws, pasname, this.account);
							this.rtelem = null;
							this.resultmsg = null;
							SetSignal(firstOpenConn, firstOpenConn, "0000000000", "0000000001");
							if (SetSignal(!firstOpenConn, firstOpenConn, "0000000000", "0000000001")) {
								log.debug(
										"{} {} {} AutoPrnCls : --keep cheak barcode after Set Signal after received message format error",
										brws, catagory, account);
							} else {
								log.debug(
										"{} {} {} AutoPrnCls : --keep cheak barcode after Set Signal after received message format error",
										brws, catagory, account);
							}
							this.curState = EJECTAFTERPAGEERROR;
							//20220819 MAtsudairaSyuMe cancel the Constants.outgoingTelegramKeyMap on RouteServerHandler
							clientSession.sendCANCEL(this.telegramKey.getBytes());
							log.debug("[{}][{}][{}] 中心存摺{}資料接收電文格式錯誤！！ TITA Key=[{}] send cancel to RouteServer", brws, pasname, this.account, (ifun == TXP.INQ) ? "補登" : "刪除", this.telegramKey);
							trace.error("[{}][{}][{}] 中心存摺{}資料接收電文格式錯誤！！ TITA Key=[{}] send cancel to RouteServer", brws, pasname, this.account, (ifun == TXP.INQ) ? "補登" : "刪除", this.telegramKey);
							//----
							rtn = -3;
							break;
						}
						//---
						total = new TOTATel();
						boolean totalrtn = total.copyTotaLabel(Arrays.copyOfRange(rtelem, 0, total.getTotalLabelLen()));
						log.debug("total.initTotaLabel rtn={} getTotalLabelLen={} {}", totalrtn,
								total.getTotalLabelLen(), this.rtelem.length);

						byte[] totatext = Arrays.copyOfRange(rtelem, total.getTotalLabelLen(), this.rtelem.length);
						log.debug("totatext len={}", totatext.length);
						try {
							String mt = new String(total.getValue("mtype"));
							String cMsg = "";
							//20200523
							String mnostr = new String(total.getValue("msgno"));
							//20200819 change TOTA_TEXT to UTF-8 messge
//							atlog.info(" -- [{}] TOTA_TEXT=[{}]", mt + mnostr, new String(totatext));
							if (totatext.length > 1)
								atlog.info(" -- [{}] TOTA_TEXT=[{}]", mt + mnostr, charcnv.BIG5bytesUTF8str(totatext));
							else
								atlog.info(" -- [{}] TOTA_TEXT=[{}]", mt + mnostr, new String(totatext));
							//----
							//----
							//20200810
							if ((mt + mnostr).trim().equalsIgnoreCase("C000")) {
//								memcpy(fepdd,tota_c0099.tbsdy+6,2);
//								ODSTrace(NULL,"set fepdd=[%2.2s]",fepdd);
//								"2020081014070020200810"
								log.debug("!!!!! copy fepdd");
								if (totatext.length >= 22) {
									System.arraycopy(totatext, 20, this.fepdd, 0, 2);
									atlog.info("set fepdd=[{}]", new String(this.fepdd));
									//20200910 change to use new UPSERT
//									String updTBSDY = PrnSvr.svrid + ",'" + new String(totatext, 14, 8) + "',";
									String updTBSDY = "'" + new String(totatext, 14, 8) + "'";
									if (jsel2ins == null)
										jsel2ins = new GwDao(PrnSvr.dburl, PrnSvr.dbuser, PrnSvr.dbpass, false);
									//20201115
								//	int row = jsel2ins.UPSERT(PrnSvr.svrtbsdytbname, PrnSvr.svrtbsdytbfields, updTBSDY, PrnSvr.svrtbsdytbmkey, PrnSvr.svrid);
									int row = jsel2ins.UPSERT(PrnSvr.svrtbsdytbname, PrnSvr.svrtbsdytbfields, updTBSDY, PrnSvr.svrtbsdytbmkey, PrnSvr.bkno);
									//----
									log.debug("total {} records update [{}]", row, updTBSDY);
//20220525									jsel2ins.CloseConnect();
//									jsel2ins = null;
								} else
									log.error("!!!!! totatext len={} too short !!!", totatext.length);
								this.curState = SNDANDRCVTLM;
								this.Send_Recv_DATAInq = true;
								this.passSNDANDRCVTLM = true;
								this.alreadySendTelegram = false;
								continue;
							}
							//----
							if (mt.equals("E") || mt.equals("A") || mt.equals("X")) {
								//20200716 modify get message from message table
								if (mt.equals("E"))
									msgid = "A" + mnostr;
								else
									msgid = mt + mnostr;
									//20200523
//									msgid = "E" + new String(total.getValue("msgno"));
//									msgid = "E" + mnostr;
								//----
								for (int i = 0; i < totatext.length; i++)
									if (totatext[i] == 0x7 || totatext[i] == 0x4 || totatext[i] == 0x3)
										totatext[i] = 0x20;
//								cMsg = "-" + new String(totatext).trim();
//								cMsg = "-" + charcnv.BIG5bytesUTF8str(totatext).trim();
								//20200716 modify for get message from message table
								//20200809 modify for check totatext length > 0
								if (totatext.length > 0)
								    cMsg = charcnv.BIG5bytesUTF8str(totatext).trim();
								else
									cMsg = "";
								int mno = Integer.parseInt(new String(total.getValue("msgno")));
								//20200819 get A/E 622 message
								if (cMsg != null && cMsg.length() > 0)
									cMsg = m_Msg.m_Message.get(msgid) + "－" + cMsg;
								else
									cMsg = m_Msg.m_Message.get(msgid);
								log.debug("cMsg=[{}]", cMsg);
								//---
								// 20100913 , E622:本次日不符 send C0099
								if (mno == 622) {
//									return 622;
									amlog.info("[{}][{}][{}]:52[{}{}]{}！", brws, pasname,this.account, mt,mnostr, cMsg);
									//20200810
//									this.curState = SNDANDRCVTLM;
									this.Send_Recv_DATAInq = true;
									this.passSNDANDRCVTLM = true;
									this.alreadySendTelegram = false;
									this.setTITA_TOTA_START(false);
									resultmsg = DataINQ(TXP.SENDTHOST, TXP.C0099TYPE, this.dCount);
									//20210628 change to use MDP
									//not yet send telegram send firstly
									//alreadySendTelegram = dispatcher.sendTelegram(resultmsg);
									//20220809 MatsudairaSyuMe change to use RouteConnection
									//alreadySendTelegram = true;
									/* 20220809 MatsudairaSyuMe change to use RouteConnection
									ZMsg request = new ZMsg();
									request.append(resultmsg);
									clientSession.send("fas", request);
									*/
									//20220809 MatsudairaSyuMe change to use RouteConnection
									this.rtelem = null;  //20220809 reset previous receive buffer
									alreadySendTelegram = clientSession.send(resultmsg);
									//20210112 add by MatsudairaSyuMe TITA_TOTA_START flag checking change to PrtCli
									if (alreadySendTelegram)
										this.setTITA_TOTA_START(true);
									//----
									this.curState = RECVTLM;
									//20200810
									rtn = 622;
//									break;
									continue;
								}
								//20200428 add for receive TOTA ERROR message
								this.curState = EJECTAFTERPAGEERROR;
								// "A665" & "X665" 無補登摺資料、"A104" 該戶無未登摺資料
								if (mno == 665 || mno == 104) {
									SetSignal(firstOpenConn, firstOpenConn, "0000000000", "0000000100");
									if (SetSignal(!firstOpenConn, firstOpenConn, "0000000000", "0000000100")) {
//										amlog.info("[{}][{}][{}]:52[{}]{}{}!", brws, pasname, this.account,mt,mno, cMsg);
									} else {
										log.debug("{} {} {} {} {} {} AutoPrnCls : --change ", brws, catagory, account,
												mt, mnostr, cMsg);
									}
									amlog.info("[{}][{}][{}]:52[{}{}]{}！", brws, pasname, this.account,mt,mnostr, cMsg);

								}
								// E194 , 補登資料超過可印行數, 應至服務台換摺
								else if (mno == 194) {
									SetSignal(firstOpenConn, firstOpenConn, "0000000000", "0000001000");
									if (SetSignal(!firstOpenConn, firstOpenConn, "0000000000", "0000001000")) {
										;
									} else {
										log.debug("{} {} {} {} {} {} AutoPrnCls : --change ", brws, catagory, account,
												mt, mnostr, cMsg);
									}
									amlog.info("[{}][{}][{}]:52[{}{}]{}！", brws, pasname, this.account,mt,mnostr, cMsg);
								} else {
									SetSignal(firstOpenConn, firstOpenConn, "0000000000", "0000000001");
									if (SetSignal(!firstOpenConn, firstOpenConn, "0000000000", "0000000001")) {
//										amlog.info("[{}][{}][{}]:52[{}]{}{}!", brws, pasname, this.account,mt,mno, cMsg);
									} else {
										log.debug("{} {} {} {} {} {} AutoPrnCls : --change ", brws, catagory, account,
												mt, mnostr, cMsg);
									}
									amlog.info("[{}][{}][{}]:53[{}{}]{}！", brws, pasname, this.account,mt,mnostr, charcnv.BIG5UTF8str(cMsg));  //20200714 change 52 to 53
									//20231026 add check for duplicate seq. no. if mt + mnostr == "E692" then increment 10 to setSeq and save toe locale reg. file
									if (new String(mt + mnostr).equals("E692")) {
										try {
											this.setSeqNo = Integer
													.parseInt(FileUtils.readFileToString(this.seqNoFile, Charset.defaultCharset())) + 10;
											if (this.setSeqNo > 99999999)
												this.setSeqNo = 1;
											FileUtils.writeStringToFile(this.seqNoFile, Integer.toString(this.setSeqNo),
													Charset.defaultCharset());
										} catch (Exception e) {
											log.error("ERROR!!! update new seq number string {} error {}", this.setSeqNo, e.getMessage());
											FileUtils.writeStringToFile(this.seqNoFile, "0", Charset.defaultCharset());
											this.setSeqNo = 0;
										}
										log.debug("debug!!! E692 change setSeqNo to {}", this.setSeqNo);
									}
									//----
								}
								if (ifun == 1) {
									log.debug("[{}]:TxFlow : Send_Recv() -- INQ Data Failed ! msgid=[{}{}]", brws, mt,
											mnostr);
									atlog.info("INQ Data Failed ！ msgid=[{}{}]", mt,mnostr); //20200718 take out cMsg
								} else {
									log.debug("[{}]:TxFlow : Send_Recv() -- DEL Data Failed ! msgid=[{}{}", brws, mt,
											mnostr);
									atlog.info("DEL Data Failed ！ msgid=[{}{}]", mt,mnostr);
								}
//								return (-2);
								rtn = -2;
								break;
							}
							if (ifun == TXP.INQ) {
								// Receive Inquiry Data
								// 20080923 , Check return value
								//20200724
//								resultmsg = DataINQ(TXP.RECVFHOST, iflg, this.dCount, con, totatext);
								resultmsg = DataINQ(TXP.RECVFHOST, iflg, this.dCount, totatext);
								//----
								if (resultmsg == null || resultmsg.length == 0) {
									if (SetSignal(firstOpenConn, firstOpenConn, "0000000000", "0000000001")) {
										amlog.info("[{}][{}][{}]:34接收資料錯誤！", brws, pasname, this.account);
									} else {
										log.debug("{} {} {} AutoPrnCls : --change ", brws, catagory, account);
									}
									atlog.info("Failed ！ iRtncd=[-1]");
									rtn = -1;
									break;
								}
								iCount = Integer.parseInt(this.dCount);
								iCon = Integer.parseInt(con);
								log.debug("iCont={} iCon={} iLine={} (iLine - 1 + iCount)={}", iCount, iCon, iLine,
										(iLine - 1 + iCount));
								if ((iLine - 1 + iCount) >= 24) {
									//20200918
//									atlog.info("[{}] TOTA_TEXT=[{}]", resultmsg.length, new String(resultmsg));
									atlog.info("[{}] TOTA_TEXT=[{}]", resultmsg.length, charcnv.BIG5bytesUTF8str(resultmsg));
									amlog.info("[{}][{}][{}]:55存摺補登資料接收成功！", brws, pasname, this.account);
									this.curState = STARTPROCTLM;
									break;
								} //20200724
								else if (this.iCount < iCon) {
									this.curState = SNDANDRCVTLM;
									this.Send_Recv_DATAInq = true;
									this.passSNDANDRCVTLM = true;
									this.alreadySendTelegram = false;
								} else
									this.curState = STARTPROCTLM;
								//----
							} else {
								// Receive Delete Result
								DataDEL(TXP.RECVFHOST, iflg, "");
								amlog.info("[{}][{}][{}]:56存摺已補登資料刪除成功！", brws, pasname, this.account);
								this.curState = SNDANDRCVDELTLMCHKEND;
								break;
							}
//20200724							this.curState = STARTPROCTLM;
						} catch (Exception e) {
							e.getStackTrace();
							log.error("ERROR while get total label mtype {}" + e.getMessage());
						}
					} else {
						long now = System.currentTimeMillis();
						if ((now - startTime) > responseTimeout) {
							//20210112 MatsudairaSyume
							/* 20210116 MatsudairaSyuMe
							if (!this.dispatcher.getFASSvr().isCurrConnNull())
								this.dispatcher.releaseConn();
								*/
							//----
							// 20200504
							this.curState = EJECTAFTERPAGEERROR;
							log.error("ERROR!!! received data from host timeout {}", responseTimeout);
							//20211126 MatsudairaSyuMe change AMlog timeout message 20211209 add colon word
//							amlog.info("[{}][{}][{}]:21存摺頁次錯誤！[{}]接電文逾時{}", brws, pasname, this.account, rpage, responseTimeout);
							amlog.info("[{}][{}][{}]:05中心存摺補登資料接收電文逾時{}", brws, pasname, this.account, responseTimeout);
							//20220819 MAtsudairaSyuMe cancel the Constants.outgoingTelegramKeyMap on RouteServerHandler
							clientSession.sendCANCEL(this.telegramKey.getBytes());
							//20220819 MAtsudairaSyuMe
							trace.error("中心存摺{}資料接收電文逾時 {} sendCANCEL {} to RouteServer", (ifun == TXP.INQ) ? "補登" : "刪除", responseTimeout, this.telegramKey);
							//----
							SetSignal(firstOpenConn, firstOpenConn, "0000000000", "0000000001");
							// ----
							if (SetSignal(!firstOpenConn, firstOpenConn, "0000000000", "0000000001")) {
								log.debug(
										"{} {} {} AutoPrnCls : --ckeep cheak barcode after Set Signal after check barcode",
										brws, catagory, account);
							} else {
								log.debug(
										"{} {} {} AutoPrnCls : --keep cheak barcode after Set Signal after check barcode",
										brws, catagory, account);
							}
							rtn = -1;
							//20230406 MatsudairaSyuMe add for Timeout
							break;
							//20230406 MattsudairaSyuMe
						} else {
							//20210112 mark by MatsudairaSyuMe TITA_TOTA_START flag checking change to PrtCli
							log.warn("WARN!!! not yet received data from host {} dispatcher.isTITA_TOTA_START()={} alreadySendTelegram={}", now - startTime, this.isTITA_TOTA_START(), alreadySendTelegram);
							//----
							rtn = 0;
						}
					}
//					this.alreadySendTelegram = false;
				}
			}
			log.debug("====================================this.curState=[{}] this.Send_Recv_DATAInq=[{}] this.passSNDANDRCVTLM=[{}] alreadySendTelegram=[{}] isTITA_TOTA_START()=[{}]" , this.curState, this.Send_Recv_DATAInq, this.passSNDANDRCVTLM, alreadySendTelegram , this.isTITA_TOTA_START());
			//20230331 MatsudairaSyuMe check timeout
			//20230406 MatudiraSyuMe mark Up for always check timeout
			//if (this.curState == RECVTLM && this.Send_Recv_DATAInq == true && this.passSNDANDRCVTLM == true && alreadySendTelegram == false) {
				long now = System.currentTimeMillis();
				if ((now - startTime) > responseTimeout) {
					this.curState = EJECTAFTERPAGEERROR;
					log.error("2 ERROR!!! received data from host timeout {}", responseTimeout);
					amlog.info("[{}][{}][{}]:05中心存摺補登資料接收電文逾時2 {}", brws, pasname, this.account, responseTimeout);
					clientSession.sendCANCEL(this.telegramKey.getBytes());
					trace.error("中心存摺{}資料接收電文逾時2 {} sendCANCEL {} to RouteServer", (ifun == TXP.INQ) ? "補登" : "刪除", responseTimeout, this.telegramKey);
					SetSignal(firstOpenConn, firstOpenConn, "0000000000", "0000000001");
					if (SetSignal(!firstOpenConn, firstOpenConn, "0000000000", "0000000001")) {
						log.debug(
							"{} {} {} AutoPrnCls : --ckeep cheak barcode after Set Signal after check barcode 2 ",
							brws, catagory, account);
					} else {
						log.debug(
							"{} {} {} AutoPrnCls : --keep cheak barcode after Set Signal after check barcode 2",
							brws, catagory, account);
					}
					rtn = -1;
					break;
				} else
					log.debug("2 not yet received data from host Waiting <<{}>>", now - startTime);				
			//20230406 MatsudairaSyuMe Mark Up for always check timeout }
			//20230331 MatsudairaSyuMe end
		} while (this.iCount < iCon);

		//20200428 add for receive error TOTA  ERROR message set to this.curState == EJECTAFTERPAGEERROR
		//20210112 mark by MatsudairaSyuMe TITA_TOTA_START flag checking change to PrtCli
		if ((this.curState == STARTPROCTLM || this.curState == SNDANDRCVDELTLMCHKEND || this.curState == EJECTAFTERPAGEERROR)
				&& !this.isTITA_TOTA_START() && alreadySendTelegram)
			//----
			//relese channel
			this.alreadySendTelegram = false;

		if (ifun == TXP.DEL) {
			pb_arr.clear();
			fc_arr.clear();
			gl_arr.clear();
		}
		log.debug("{} {} {} this.curState={}", brws, catagory, account, this.curState);
		return rtn;
	}

	private void resetPassBook() {
		this.alreadySendTelegram = false;
		//20210112 mark by MatsudairaSyuMe TITA_TOTA_START flag checking change to PrtCli
		this.setTITA_TOTA_START(false);
		//----
		this.iFirst = 0;
		this.iEnd = 0;
		this.dCount = "000";
		this.iCount = Integer.parseInt(this.dCount);
		this.catagory = "";
		this.account = "";
		this.iFirst = 0;
		this.iEnd = 0;
		this.pb_arr.clear();
		this.fc_arr.clear();
		this.gl_arr.clear();
		this.pasname = "        ";
		this.changeLightOnLastPage = false; //20220927
		this.curState = ENTERPASSBOOKSIG;
		this.passSNDANDRCVTLM = false;  //20200714
		SetSignal(firstOpenConn, firstOpenConn, "1100000000", "0000000000");
		log.debug("{}=====resetPassBook prtcliFSM", this.curState);
		// 20230522 MasudairaSyuMe register last time read data from pass book print
		this.lastTimeFromPrt = 0l;
		return;
	}

	private void prtcliFSM(boolean isInit) {
		if (isInit) {
			this.curState = SESSIONBREAK;
			//20200616
			this.lastState = SESSIONBREAK;
			this.lastStateTime = -1l;			
			//----
			this.alreadySendTelegram = false;
			//20210108 mark by MatsudairaSyuMe
			////this.setTITA_TOTA_START(false);
			//----
			this.iFirst = 0;
			this.iEnd = 0;
			this.dCount = "000";
			this.iCount = Integer.parseInt(this.dCount);
			this.catagory = "";
			this.account = "";
			this.pasname = "        ";
			this.passSNDANDRCVTLM = false;  //20200714
			this.changeLightOnLastPage = false; //20220927
			log.debug("=======================check prtcliFSM init");
			//20230314 MatsudairaSyuMe, 20230325 add svrid
			rmAbNomalAlart(PrnSvr.svrid.trim() + "_" + this.brws);
			//----
			// 20230522 MasudairaSyuMe register last time read data from pass book print
			this.lastTimeFromPrt = 0l;
			return;
		}
        //20200616 add this.lastState and check duration time
/*		if (this.lastState != this.curState) {
			this.lastState = this.curState;
			this.lastStateTime = System.currentTimeMillis(); //re-new lastStateTime
			this.durationTime = -1l;
		} else {
			if (this.lastState == CAPTUREPASSBOOK)
				this.lastStateTime = System.currentTimeMillis(); //re-new lastStateTime for CAPTUREPASSBOOK
			else {
				this.durationTime = System.currentTimeMillis() - this.lastStateTime;
				if (this.durationTime > responseTimeout) {
					// 20200504
					this.curState = EJECTAFTERPAGEERROR;
					log.error("WORN!!! state timeout {} start reset", responseTimeout);
					amlog.info("[{}][{}][{}]:21狀態錯誤！[{}] 逾時{}", brws, pasname, this.account, rpage, responseTimeout);
					SetSignal(firstOpenConn, firstOpenConn, "0000000000", "0000000001");
					// ----
					if (SetSignal(!firstOpenConn, firstOpenConn, "0000000000", "0000000001")) {
						log.debug("{} {} {} AutoPrnCls : --state error time out", brws, catagory, account);
					} else {
						log.debug("{} {} {} AutoPrnCls : --state error time out", brws, catagory, account);
					}
				}
			}
		}*/
		//----
		log.debug("before {} last {} duration {} =======================check prtcliFSM", this.curState, this.lastState, this.durationTime);
		int before = this.curState;
		//20200906, 20201028 RESTART
		if (getCurMode() == EventType.SHUTDOWN || getCurMode() == EventType.RESTART) {
			if (this.curState <= CAPTUREPASSBOOK) {
				log.info("CurMode {} curState == [{}] start shutdown now", getCurMode(), this.curState);
				try {
					publishInactiveEvent();
					super.channelInactive(this.currentContext);
					prt.getIsShouldShutDown().set(true);
					prt.ClosePrinter();
					aslog.info(String.format("DIS  %s[%04d]:", this.curSockNm, 0));
//20200910 change to use new UPSERT
//					String updValue = String.format(updValueptrn, this.brws, this.rmtaddr.getAddress().getHostAddress(),
//							this.rmtaddr.getPort(), this.localaddr.getAddress().getHostAddress(),
//							this.localaddr.getPort(), this.typeid, Constants.STSUSEDINACT);
					//20201028 modify for RESTART
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
					String t = sdf.format(new java.util.Date());
					if (getCurMode() == EventType.SHUTDOWN) {
						String updValue = String.format(updValueptrn, this.rmtaddr.getAddress().getHostAddress(),
							this.rmtaddr.getPort(), this.localaddr.getAddress().getHostAddress(),
							//20210827 MatsudairaSyuMe if current mode CurMode SHUTDOWN/RESTART device stat set to Constants.STSNOTUSED otherwise Constants.STSUSEDINACT
							this.localaddr.getPort(), this.typeid, Constants.STSNOTUSED);
						//----
						if (jsel2ins == null)
							jsel2ins = new GwDao(PrnSvr.dburl, PrnSvr.dbuser, PrnSvr.dbpass, false);
						//20230517 MatsudairaSyuMe take out the mark for connect once
						int row = jsel2ins.UPSERT(PrnSvr.statustbname, PrnSvr.statustbfields, updValue, PrnSvr.statustbmkey,
							"'" + this.brws + "'" + "," + PrnSvr.svrid);  //20220525 MatsudairaSyuMe change this.brws to "'" + this.brws + "'"
						/*20230517 MatsudairaSyuMe mark up for connect once
						int row = jsel2ins.UPSERT_R(PrnSvr.statustbname, PrnSvr.statustbfields, updValue, PrnSvr.statustbmkey,
								"'" + this.brws + "'" + "," + PrnSvr.svrid, false);  //20220613 MatsudairaSyuMe
						*/
						//20210826 MatsudairaSyuMe if current mode CurMode SHUTDOWN/RESTART device stat set to Constants.STSNOTUSED or Constants.STSUSEDINACT
						log.debug("total {} records update status [{}]", row, Constants.STSNOTUSED);
						//----
					// 20200909 update cmd table
//	mark for RESTART				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
//	mark for RESTART				String t = sdf.format(new java.util.Date());
						row = jsel2ins.UPDT(PrnSvr.cmdtbname, "CMD, CMDRESULT,CMDRESULTTIME", "'','STOP','" + t + "'",
							"SVRID,BRWS", PrnSvr.svrid + ",'" + this.brws + "'");
						log.debug("total {} records update status [{}]", row, this.curMode);
						jsel2ins.CloseConnect();
						jsel2ins = null;
					}
					//20201026
					cmdhiscon = new GwDao(PrnSvr.dburl, PrnSvr.dbuser, PrnSvr.dbpass, false);
					//20201028 modify for RESTART
					String fldvals2 = "";
					if (getCurMode() == EventType.SHUTDOWN)
//					String fldvals2 = String.format(hisfldvalssptrn2, "", "STOP", t, this.rmtaddr.getAddress().getHostAddress(),
//							this.rmtaddr.getPort(),this.localaddr.getAddress().getHostAddress(), this.localaddr.getPort(),"0");
						//20201218 add original cmd to devcmdhis
						fldvals2 = String.format(hisfldvalssptrn2, "STOP", "STOP", t, this.rmtaddr.getAddress().getHostAddress(),
							this.rmtaddr.getPort(),this.localaddr.getAddress().getHostAddress(), this.localaddr.getPort(),"0");
					else
						fldvals2 = String.format(hisfldvalssptrn2, "RESTART", "STOP", t, this.rmtaddr.getAddress().getHostAddress(),
								this.rmtaddr.getPort(),this.localaddr.getAddress().getHostAddress(), this.localaddr.getPort(),"0");
					//----
//					sno = cmdhiscon.INSSELChoiceKey(PrnSvr.devcmdhistbname, "SVRID,AUID,BRWS,CMD,CMDCREATETIME,CMDRESULT,CMDRESULTTIME,RESULTSTUS", "1,1,'9838901','','2020-10-21 09:46:38.368000','START','2020-10-21 09:46:38.368000','0','2'", "SNO", "31", false, true);
					String[] rsno = cmdhiscon.INSSELChoiceKey(PrnSvr.devcmdhistbname, "CMD,CMDRESULT,CMDRESULTTIME,DEVIP,DEVPORT,SVRIP,SVRPORT,RESULTSTUS", fldvals2, PrnSvr.devcmdhistbsearkey, sno, false, true);
					if (rsno != null) {
						for (int i = 0; i < rsno.length; i++)
							log.debug("rsno[{}]=[{}]",i,rsno[i]);
					} else
						log.error("rsno null");
					cmdhiscon.CloseConnect();
					cmdhiscon = null;
					//----

				} catch (Exception e) {
					e.printStackTrace();
				}
				// 20201004 test
				if (this.lastState != SESSIONBREAK) { //20230204 Reset the printer and set the signal to no service mode
					amlog.info("[{}][{}][{}]:99接收指令停止與補摺機連線...", brws, "        ", "            ");
					//20230310 Reset the printer and set the signal  to no service mode
					SetSignal(firstOpenConn, firstOpenConn, "0000000000", "0000000010");
//					SetSignal(!firstOpenConn, firstOpenConn, "0000000000", "0000000010");
					byte[] PINIT = { (byte) 0x1b, (byte) '0' };
					prt.Send_hData(PINIT);
					Sleep(500);
					//----
				}
//				this.curState = SESSIONBREAK;
				this.curSockNm = "";
				this.clientMessageBuf.clear();
				this.clientChannel = null;
				/* 20220905 MatsudairaSyuMe use RouteConnection as dispatcher no need to shutdown anyway!!!
				//20210628 use MDP
				if (clientSession != null)
					//20220809 MatsudairaSyuMe change to use RouteConnection
//					clientSession.destroy();
					clientSession.shutdown();
				//----
				*/
				close();
				// 20201006
				if (this.lastState != SESSIONBREAK) {
					log.info("CurMode {} curState == [{}] stop the thread", getCurMode(), this.curState);
					PrnSvr.closeNode(this.brws, true);
					//20210423 MatsudairaSyuMe stop thread dynamic log
					LogUtil.stopLog((ch.qos.logback.classic.Logger) amlog);
					LogUtil.stopLog((ch.qos.logback.classic.Logger) aslog);
					LogUtil.stopLog((ch.qos.logback.classic.Logger) atlog);
					amlog = null;
					aslog = null;
					atlog = null;
					timer.cancel();  //20211203 MatsudairasyuMe
					Thread.currentThread().interrupt();
				}
				this.curState = SESSIONBREAK;
				return;
				// 20201006----
			} else
				log.info("CurMode {} curState == [{}] prepare to shutdown", getCurMode(), this.curState);
		}
		// 20230522 MasudairaSyuMe register last time read data from pass book print
		if ((this.curState >= ENTERPASSBOOKSIG) && ((System.currentTimeMillis() - this.lastTimeFromPrt) > (this.responseTimeout / 2))) {
			amlog.info("[{}][{}][{}]:97補摺機超過{}秒無回應，開始嘗試重啟連線", brws, "        ", "            ", (this.responseTimeout / 2000));									
			log.error("[{}][{}][{}]補摺機超過{}秒無回應，開始嘗試重啟連線", brws, "        ", "            ", (this.responseTimeout / 2000));									
			close();
			return;
		}/* else
			amlog.info("[{}][{}][{}]:97-1補摺機 {} 回應MAX {}", brws, "        ", "            ", (System.currentTimeMillis() - this.lastTimeFromPrt), (this.responseTimeout / 2));									
			*/
		//----20230522
		//----
		switch (this.curState) {
		case SESSIONBREAK:
			prt.OpenPrinter(firstOpenConn);
			this.curState = OPENPRINTER;
			//20200718
			lastCheck(before);
			log.debug("after {}=>{}===check prtcliFSM", before, this.curState);
			break;

		case OPENPRINTER:
			this.alreadySendTelegram = false;
			//20210112 mark by MatsudairaSyuMe TITA_TOTA_START flag checking change to PrtCli
			this.setTITA_TOTA_START(false);
			//----
			this.iFirst = 0;
			this.iEnd = 0;
			this.dCount = "000";
			this.iCount = Integer.parseInt(this.dCount);
			this.catagory = "";
			this.account = "";
			this.pasname = "        ";
			this.changeLightOnLastPage = false; //20220927
			//20230314 MatsudairaSyuMe, 20230325 add svrid
			rmAbNomalAlart(PrnSvr.svrid.trim() + "_" + this.brws);
			//----
			if ((this.iFirst == 0) && prt.OpenPrinter(!firstOpenConn)) {
				this.curState = ENTERPASSBOOKSIG;
				SetSignal(firstOpenConn, firstOpenConn, "1100000000", "0000000000");
				this.iFirst = 0;
				this.iEnd = 0;
				this.catagory = "";
				this.account = "";
				this.pb_arr.clear();
				this.fc_arr.clear();
				this.gl_arr.clear();
				log.debug("{}=====SetSignal prtcliFSM", this.curState);
			}
			//20200718
			lastCheck(before);
			log.debug("after {}=>{}=====check prtcliFSM", before, this.curState);
			break;

		case ENTERPASSBOOKSIG:
			//20200611 for turn page processing
			String sig1 = "1100000000", sigb = "0000000000";
			if (this.iFirst == 1) {
				sig1 = "1100000000";
				sigb = "0000010000";
			}
			if (SetSignal(!firstOpenConn, firstOpenConn, sig1, sigb)) {
				this.curState = CAPTUREPASSBOOK;
				if (this.iFirst == 1) {
					amlog.info("[{}][{}][{}]:62等待請翻下頁繼續補登...", brws, pasname, account);
					log.debug("DetectPaper [{}][{}][{}]:62等待請翻下頁繼續補登...", brws, pasname, account);
				} else {
					amlog.info("[{}][{}][{}]:****************************", StrUtil.convertValidLog(brws), "        ", "            "); //20230112 change brws to StrUtil.convertValidLog(brws)
					amlog.info("[{}][{}][{}]:00請插入存摺...", brws, pasname, "            ");
					//20220429 MatsudairaSyuMe
					this.startIdleMode = false;
					//--
					log.debug("DetectPaper [{}][{}][{}]:00請插入存摺...", brws, pasname, "            ");
					/*//20230321 MatsudairaSyuMe update status of device table
					try {
						String updValue = String.format(updValueptrn,this.remoteHostAddr,
								this.rmtaddr.getPort(),this.localHostAddr, this.localaddr.getPort(), this.typeid, Constants.STSUSEDACT);
						if (jsel2ins == null)
							jsel2ins = new GwDao(PrnSvr.dburl, PrnSvr.dbuser, PrnSvr.dbpass, false);
						int row = jsel2ins.UPSERT_R(PrnSvr.statustbname, PrnSvr.statustbfields, updValue, PrnSvr.statustbmkey, "'" + this.brws + "'" + "," + PrnSvr.svrid, false);
						log.debug("total {} records update status [{}] ENTERPASSBOOKSIG", row, Constants.STSUSEDACT);
					} catch (Exception e) {
						e.printStackTrace();
						log.error("update state table {} in ENTERPASSBOOKSIG error:{}", PrnSvr.statustbname, e.getMessage());
					}
					//---- 20230321 end*/
				}
				//----
				this.lastCheckTime = System.currentTimeMillis();
				//20220429 MatsudairaSyuMe use fix Request command Time
				this.lastRequestTime = this.lastCheckTime;
				//----
				/* 20200427 mark up for  performance*/
//20200910 change to use UPSERT
//				String updValue = String.format(updValueptrn,this.brws, this.rmtaddr.getAddress().getHostAddress(),
//						this.rmtaddr.getPort(),this.localaddr.getAddress().getHostAddress(), this.localaddr.getPort(), this.typeid, Constants.STSUSEDACT);
				String updValue = String.format(updValueptrn,this.rmtaddr.getAddress().getHostAddress(),
						this.rmtaddr.getPort(),this.localaddr.getAddress().getHostAddress(), this.localaddr.getPort(), this.typeid, Constants.STSUSEDACT);
				try {
					if (jsel2ins == null)
						jsel2ins = new GwDao(PrnSvr.dburl, PrnSvr.dbuser, PrnSvr.dbpass, false);
					int row = jsel2ins.UPSERT(PrnSvr.statustbname, PrnSvr.statustbfields, updValue, PrnSvr.statustbmkey, this.brws + "," + PrnSvr.svrid);
					log.debug("total {} records update status [{}]", row, Constants.STSUSEDACT);
					jsel2ins.CloseConnect();
					jsel2ins = null;
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					log.error("update state table {} error:{}", PrnSvr.statustbname, e.getMessage());
				}
				/*20200427  */
//20200925				prt.DetectPaper(firstOpenConn, 0);
				prt.DetectPaper(firstOpenConn, responseTimeout);
			}
			//20200718
			lastCheck(before);
			log.debug("after {}=>{}=====check prtcliFSM", before, this.curState);
			break;

		case CAPTUREPASSBOOK:
			//20200611 for turn page processing
/*			if (this.iFirst == 0 || this.autoturnpage.equals("false")) {
				if (this.iFirst == 1) {
					SetSignal(firstOpenConn, firstOpenConn, "1100000000", "0000010000");
					amlog.info("[{}][{}][{}]:62等待請翻下頁繼續補登...", brws, pasname, account);
					//20200610
					SetSignal(!firstOpenConn, firstOpenConn, "1100000000", "0000010000");
					prt.DetectPaper(firstOpenConn, 0);
					//----
				} else {*/

			//20220429 MatsudairaSyuMe use fix Request command Time
			long cur = System.currentTimeMillis();
			log.debug("check CAPTUREPASSBOOK {}=>{} {}", cur, (cur - this.lastRequestTime), (PrnSvr.getReqTime() * 2));
////20220430if ((cur - this.lastRequestTime) >= (PrnSvr.getReqTime())) {
				this.lastRequestTime = cur;
			//

//20200925			if (prt.DetectPaper!firstOpenConn, 0))
					if (prt.DetectPaper(!firstOpenConn, responseTimeout))
					{
						this.curState = GETPASSBOOKSHOWSIG;
						log.debug("{} {} {} AutoPrnCls : --start Show Signal", brws, catagory, account);
//20200821 test						SetSignal(firstOpenConn, !firstOpenConn, "0000000000", "0010000000");
						SetSignal(firstOpenConn, firstOpenConn, "0000000000", "0010000000");
						//20220429 MatsudairaSyuMe
						this.startIdleMode = false;
							//----
					} else {
					//	if ((System.currentTimeMillis() - this.lastCheckTime) > 10 * 1000) {
							/* 20220429  MatsudairaSyuMe Maru up for  performance
//20200910 change to use new UPSERT
//							String updValue = String.format(updValueptrn,this.brws, this.rmtaddr.getAddress().getHostAddress(),
//									this.rmtaddr.getPort(),this.localaddr.getAddress().getHostAddress(), this.localaddr.getPort(), this.typeid, Constants.STSUSEDACT);
							String updValue = String.format(updValueptrn, this.rmtaddr.getAddress().getHostAddress(),
									this.rmtaddr.getPort(),this.localaddr.getAddress().getHostAddress(), this.localaddr.getPort(), this.typeid, Constants.STSUSEDACT);
							try {
								if (jsel2ins == null)
									jsel2ins = new GwDao(PrnSvr.dburl, PrnSvr.dbuser, PrnSvr.dbpass, false);
								int row = jsel2ins.UPSERT(PrnSvr.statustbname, PrnSvr.statustbfields, updValue, PrnSvr.statustbmkey, this.brws + "," + PrnSvr.svrid);
								log.debug("total {} records update status [{}]", row, Constants.STSUSEDACT);
								jsel2ins.CloseConnect();
								jsel2ins = null;
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								log.error("update state table {} error:{}", PrnSvr.statustbname, e.getMessage());
							}
							*/
					//		this.lastCheckTime = System.currentTimeMillis();
					//	}//20220221 change "Deteect Error" to "Detect No Passbook Insert"
							log.debug("{} {} {} {} {} AutoPrnCls : Parsing() -- Detect No Passbook Insert!", brws, catagory, account, startIdleMode, this.lastCheckTime);
					}

		//20200611 for turn page processing
/*				}
			}*/
					//20220429 MatsudairaSyuMe use fix Request command Time		
////20220430} else
////20220430	log.debug("current - lastRequestTime={} =====check prtcliFSM", before, this.curState, (cur - this.lastRequestTime));
			//----
			//20200718
			lastCheck(before);
			//20200427 test
			log.debug("after {}=>{} {} =====check prtcliFSM", before, this.curState, this.lastCheckTime);
			break;

		case GETPASSBOOKSHOWSIG:
			log.debug("{} {} {} :AutoPrnCls : Show Signal", brws, catagory, account);
			//2020021 test
			//----
			if (SetSignal(!firstOpenConn, firstOpenConn, "0000000000", "0010000000"))
			{
				this.curState = SETCPI;
				prt.SetCPI(firstOpenConn, 6);
				log.debug("{} {} {} AutoPrnCls : --start Set CPI", brws, catagory, account);
			}
			//20200718
			lastCheck(before);
			log.debug("after {}=>{}=====check prtcliFSM", before, this.curState);
			break;

		case SETCPI:
			atlog.info("Set CPI");
			if (prt.SetCPI(!firstOpenConn, 6)) {
				this.curState = SETLPI;
				prt.SetLPI(firstOpenConn, 5);
				log.debug("{} {} {} AutoPrnCls : --start Set LPI", brws, catagory, account);
			}
			//20200718
			lastCheck(before);
			log.debug("after {}=>{}=====check prtcliFSM", before, this.curState);
			break;

		case SETLPI:
			atlog.info("Set LPI");
			if (prt.SetLPI(!firstOpenConn, 5)) {
				this.curState = SETPRINTAREA;
				prt.Parsing(firstOpenConn, "AREA".getBytes());
				log.debug("{} {} {} AutoPrnCls : --start Set LPI", brws, catagory, account);
			}
			//20200718
			lastCheck(before);
			log.debug("after {}=>{}=====check prtcliFSM", before, this.curState);
			break;
		case SETPRINTAREA:
			atlog.info("Set PRINT Area");
			if (prt.Parsing(firstOpenConn, "AREA".getBytes())) {
				this.curState = READMSR;
				cusid = null;
				if (null != (cusid = prt.MS_Read(firstOpenConn, brws))) {
					if (cusid.length == 1) {
						this.curState = EJECTAFTERPAGEERROR;
					} else {
					this.curState = CHKACTNO;
					for (int i = 0; i < cusid.length; i++)
						cusid[i] = cusid[i] == (byte) '<' ? (byte) '-' : cusid[i];
					setpasname(cusid);
					log.debug("{} {} {} 12存摺磁條讀取成功！", brws, catagory, new String(cusid, 0, TXP.ACTNO_LEN));
					amlog.info("[{}][{}][{}]:12存摺磁條讀取成功！", brws, pasname, new String(cusid, 0, TXP.ACTNO_LEN));
				}
				log.debug("{} {} {} AutoPrnCls : --start Read MSR", brws, catagory, account);
				}
			}
			//20200718
			lastCheck(before);
			log.debug("after {}=>{}=====check prtcliFSM", before, this.curState);
			break;

		case READMSR:
			log.debug("{} {} {} :AutoPrnCls : Read MSR", brws, catagory, account);
			cusid = null;
			if (null != (cusid = prt.MS_Read(!firstOpenConn, brws))) {
				if (cusid.length == 1) {
					this.curState = EJECTAFTERPAGEERROR;
					SetSignal(firstOpenConn, firstOpenConn, "0000000000", "0000001000");
					SetSignal(!firstOpenConn, firstOpenConn, "0000000000", "0000001000");
					Sleep(2 * 1000); //20220914 MatsudairaSyuMe for signal delay
					amlog.info("[{}][{}][{}]:11磁條讀取失敗！", brws, "        ", "            ");
					//20201119
					InsertAMStatus(brws, "", "", "11磁條讀取失敗！");
					//----
					log.debug("{} {} {} AutoPrnCls : read MSR ERROR", brws);
				} else {
					this.curState = CHKACTNO;
					for (int i = 0; i < cusid.length; i++)
						cusid[i] = cusid[i] == (byte) '<' ? (byte) '-' : cusid[i];
					setpasname(cusid);
					amlog.info("[{}][{}][{}]:12存摺磁條讀取成功！", brws, pasname, new String(cusid, 0, TXP.ACTNO_LEN));
					log.debug("{} {} {} AutoPrnCls : --start check Account", brws, catagory, account);
				}
			}
			//20200718
			lastCheck(before);
			log.debug("after {}=>{}=====check prtcliFSM", before, this.curState);
			break;

		case CHKACTNO:
			log.debug("{} {} {} :========<<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>AutoPrnCls : check Account", brws, catagory,
					account);
			if (MS_Check(cusid)) {
				tx_area.clear();
				tx_area.put("brws", brws);
				tx_area.put("account", account);
				tx_area.put("c_Msr", new String(cusid));
				tx_area.put("cpage", this.cpage);
				tx_area.put("cline", this.cline);
				tx_area.put("mbal", this.msrbal);
				tx_area.put("txday", "");
				tx_area.put("txseq", "");
				tx_area.put("keepacc", "");
				Arrays.fill(fepdd, (byte) ' ');
				updatefepdd();
				this.iEnd = 0;
				this.dCount = "000";
				this.iCount = Integer.parseInt(this.dCount);
				tx_area.put("iEnd", Integer.toString(this.iEnd));
				this.curState = CHKBARCODE;
				log.debug("{} {} {} tx_area {} iFig={} AutoPrnCls : --start check barcode", brws, catagory, account,
						tx_area, iFig);
				amlog.info("[{}][{}][{}]:02檢查存摺頁次...", brws, pasname, account);
				if ((this.rpage = prt.ReadBarcode(firstOpenConn, (short) 2)) > 0) {
					log.debug("{} {} {} AutoPrnCls : --start telegram get rpage={} npage={}", brws, catagory, account,
							this.rpage, this.npage);
					if (npage == rpage) {
						amlog.info("[{}][{}][{}]:02檢查存摺頁次正確...正確頁次={} 插入頁次={} 行次={}", brws, pasname, account, npage, rpage, nline);

						if (SetSignal(firstOpenConn, firstOpenConn, "0000000000", "0010000000")) {
							//20210112 mark by MatsudairaSyuMe TITA_TOTA_START flag checking change to PrtCli
							this.curState = SNDANDRCVTLM;this.setTITA_TOTA_START(false);//20210108
							//20210112 mark by MatsudairaSyuMe TITA_TOTA_START flag checking change to PrtCli
							log.debug("{} {} {} AutoPrnCls : --change process telegram", brws, catagory, account);
						} else {
							this.curState = SETSIGAFTERCHKBARCODE;
							log.debug("{} {} {} AutoPrnCls : --change Set Signal after check barcode", brws, catagory,
									account);
						}
					} else {
						amlog.info("[{}][{}][{}]:21存摺頁次錯誤！[{}]", brws, pasname, account, rpage);
						
						if (SetSignal(firstOpenConn, firstOpenConn, "0000000000","0000100000")) {
							this.curState = SETSIGAFTERCHKBARCODE;
							log.debug(
									"{} {} {} AutoPrnCls : --eject passbook set signal after check barcode page error!!",
									brws, catagory, account);
						} else {
							this.curState = SETSIGAFTERCHKBARCODE;
							log.debug("{} {} {} AutoPrnCls : --keep cheak barcode after Set Signal after check barcode",
									brws, catagory, account);
						}
					}
				}
			} else {
				//20220914 MatsudairaSyuMe
				SetSignal(firstOpenConn, firstOpenConn, "0000000000", "0000001000");
				SetSignal(!firstOpenConn, firstOpenConn, "0000000000", "0000001000");
				Sleep(2 * 1000);
				this.curState = EJECTAFTERPAGEERROR;//20220914 MatsudairaSyuMe this.curState = SESSIONBREAK;
				log.debug("{} {} {} AutoPrnCls : --check Account error", brws, catagory, account);
			}
			log.debug("{} {} {} AutoPrnCls : --Read MSR error", brws, catagory, account);
			//20200718
			lastCheck(before);
			log.debug("after {}=>{}=====check prtcliFSM", before, this.curState);
			break;

		case CHKBARCODE:
			log.debug("{} {} {} :AutoPrnCls : process check barcode", brws, catagory, account);
			if ((this.rpage = prt.ReadBarcode(!firstOpenConn, (short) 2)) > 0) {
				log.debug("{} {} {} AutoPrnCls : --start telegram get rpage={} npage={}", brws, catagory, account,
						this.rpage, this.npage);
				atlog.info("MS_Check() -- (1)Insert Page=[{}]", rpage);  //20200806
				if (npage == rpage) {
					amlog.info("[{}][{}][{}]:02檢查存摺頁次正確...正確頁次={} 插入頁次={} 行次={}", brws, pasname, account, npage, rpage, nline);
					if (SetSignal(firstOpenConn, !firstOpenConn, "0000000000", "0010000000")) {
						//20210112 mark by MatsudairaSyuMe TITA_TOTA_START flag checking change to PrtCli
						this.curState = SNDANDRCVTLM;this.setTITA_TOTA_START(false);//20200108
						//----
						log.debug("{} {} {} AutoPrnCls : --change process telegram", brws, catagory, account);
					} else {
						this.curState = SETSIGAFTERCHKBARCODE;
						log.debug("{} {} {} AutoPrnCls : --change Set Signal after check barcode", brws, catagory,
								account);
					}
				} else {
//					atlog.info("MS_Check() -- (1)Insert Page=[{}]", rpage);  mark for 20200806
//					WMSRFormat(true, rpage);
//					WMSRFormat(true, rpage);
					if (SetSignal(firstOpenConn, firstOpenConn, "0000000000","0000100000")) {
						this.curState = SETSIGAFTERCHKBARCODE;
						log.debug(
								"{} {} {} AutoPrnCls : --eject passbook set signal after check barcode page error!!",
								brws, catagory, account);
					} else {
						this.curState = SETSIGAFTERCHKBARCODE;
						log.debug("{} {} {} AutoPrnCls : --keep cheak barcode after Set Signal after check barcode",
								brws, catagory, account);
					}
				}
			}
			//20200718 modify for return return no barcode
			if (this.rpage < 0) {
				if (this.rpage == -2) {
					this.rpage = 0;
//					atlog.info("MS_Check() -- (1)Insert Page=[{}]", rpage);  mark 20200806
					if (SetSignal(firstOpenConn, firstOpenConn, "0000000000", "0000100000")) {
						this.curState = SETSIGAFTERCHKBARCODE;
						log.debug("{} {} {} AutoPrnCls : --eject passbook set signal after check barcode page error!!",
								brws, catagory, account);
					} else {
						this.curState = SETSIGAFTERCHKBARCODE;
						log.debug("{} {} {} AutoPrnCls : --keep cheak barcode after Set Signal after check barcode",
								brws, catagory, account);
					}
				} else {
					SetSignal(firstOpenConn, firstOpenConn, "0000000000", "0000001000");
					SetSignal(!firstOpenConn, firstOpenConn, "0000000000", "0000001000");
					this.curState = EJECTAFTERPAGEERROR;
					log.debug("check barcode error rpage={}", this.rpage);
				}
			}
			//------
			//20200718
			lastCheck(before);
			log.debug("after {}=>{}=====check prtcliFSM", before, this.curState);
			break;

		case SETSIGAFTERCHKBARCODE:
			log.debug("{} {} {} :AutoPrnCls : process setsignal after checkbcode", brws, catagory, account);
			if (npage == rpage && rpage > 0) {  //20200718 for print with page bar code exist
				if (SetSignal(!firstOpenConn, !firstOpenConn, "0000000000", "0010000000")) {
					//20210112 mark by MatsudairaSyuMe TITA_TOTA_START flag checking change to PrtCli
					this.curState = SNDANDRCVTLM;this.setTITA_TOTA_START(false);//20210108
					//----
					log.debug("{} {} {} AutoPrnCls : --change process telegram", brws, catagory, account);
				}
			} else {
//				amlog.info("[{}][{}][{}]:22存摺頁次不符...正確頁次={} 插入頁次={}", brws, pasname, account, npage, rpage);
				if (SetSignal(!firstOpenConn, firstOpenConn, "0000000000","0000100000")) {
					amlog.info("[{}][{}][{}]:22存摺頁次不符...正確頁次={} 插入頁次={}", brws, pasname, account, npage, rpage);
					atlog.info(" -- Error Page!! Correct Page=[{}] Insert Page=[{}]", npage, rpage);
					this.curState = EJECTAFTERPAGEERROR;
					log.debug(
							"{} {} {} AutoPrnCls : --eject passbook after check barcode page error!!",
							brws, catagory, account);
				}
			}
			if (this.rpage < 0) {
				SetSignal(!firstOpenConn, firstOpenConn, "0000000000","0000100000");
				this.curState = SESSIONBREAK;
				amlog.info("[{}][{}][{}]:21存摺頁次錯誤！[{}]", brws, pasname, account, rpage);
				close();
			}
			//20200718
			lastCheck(before);			
			log.debug("after {}=>{}=====check prtcliFSM", before, this.curState);
			break;

		case EJECTAFTERPAGEERROR:
			log.debug("{} {} {} :AutoPrnCls : process EJECTAFTERPAGEERROR", brws,catagory, account);
			if (!this.passSNDANDRCVTLM)  //20201002 add for log
				atlog.info((this.cusid.length > 1 ? " -- ACTNO or PAGE ... ERROR！":"MS_Read() -- Read MSR Error !"));
			else
				atlog.info("after Send_Recv() -- ... ERROR！");
			if (prt.Eject(firstOpenConn))
				resetPassBook();
			else
				this.curState = EJECTAFTERPAGEERRORWAIT;
			//20200718
			lastCheck(before);
			log.debug("after {}=>{} =====check prtcliFSM", before, this.curState);
			break;

		case EJECTAFTERPAGEERRORWAIT:
			log.debug("{} {} {} :AutoPrnCls : process EJECTAFTERPAGEERRORWAIT", brws,catagory, account);
			if (prt.Eject(!firstOpenConn))
				resetPassBook();
			//20200718
			lastCheck(before);
			log.debug("after {}=>{} =====check prtcliFSM", before, this.curState);
			break;

		case SNDANDRCVTLM:
			//20210112 mark by MatsudairaSyuMe TITA_TOTA_START flag checking change to PrtCli
			log.debug("{} {} {} :AutoPrnCls : process telegram isTITA_TOTA_START={} alreadySendTelegram={}", brws,
					catagory, account, this.isTITA_TOTA_START(), alreadySendTelegram);
			//----
			int r = 0;
			this.Send_Recv_DATAInq = true;
			this.passSNDANDRCVTLM = true;  //20200714
			if ((r = Send_Recv(this.iFig, TXP.INQ, "0", "0")) != 0) {
				//20200506 modify for receive TOTA ERROR message and can't received TOTA message
				// 20220818 add select for r != -3 format or TITA/TOTA tran. seq. key error
				if (r < 0 && r != -2 && r != -1 && r != -3) {
					this.curState = SESSIONBREAK;
					amlog.info("[{}][{}][{}]:61存摺資料補登失敗！", brws, pasname, account);
				}
			}
			switch (this.iFig) {
				case TXP.PBTYPE:
					log.debug("SNDANDRCVTLM r = {} pb_arr.size()=>{}=====check prtcliFSM", r, pb_arr.size());
					break;
				case TXP.FCTYPE:
					log.debug("SNDANDRCVTLM r = {} fc_arr.size()=>{}=====check prtcliFSM", r, fc_arr.size());
					break;
				case TXP.GLTYPE:
					log.debug("SNDANDRCVTLM r = {} gl_arr.size()=>{}=====check prtcliFSM", r, gl_arr.size());
					break;
				default:
					log.debug("SNDANDRCVTLM r = {}  unknow passbook type=[{}]=====check prtcliFSM", r, this.iFig);
					break;
			}
			//20200718
			lastCheck(before);
			log.debug("after {}=>{} r={} =====check prtcliFSM", before, this.curState, r);
			break;

		case SETREQSIG:
		case WAITSETREQSIG:
		case SENDTLM:
		case RECVTLM:
			//20210112 mark by MatsudairaSyuMe TITA_TOTA_START flag checking change to PrtCli
			log.debug(
					"{} {} {} :AutoPrnCls : process set req signal before send telegram isTITA_TOTA_START={} alreadySendTelegram={} this.Send_Recv_DATAInq={}",
					brws, catagory, account, this.isTITA_TOTA_START(), alreadySendTelegram, this.Send_Recv_DATAInq);
			//----
			r = 0;
			if (this.Send_Recv_DATAInq) {
				if ((r = Send_Recv(this.iFig, TXP.INQ, "0", "0")) != 0) {
					//20200506 modify for receive TOTA ERROR message and can't received TOTA message
					// 20220818 add select for r != -3 format or TITA/TOTA tran. seq. key error
					if ((r < 0 && r != -2 && r != -1 && r != -3)) {
						this.curState = SESSIONBREAK;
						amlog.info("[{}][{}][{}]:61存摺資料補登失敗！", brws, pasname, account);
					}
				} else {
					//20210112 mark by MatsudairaSyuMe TITA_TOTA_START flag checking change to PrtCli
					if ((r == 0 && this.isTITA_TOTA_START() == false && this.alreadySendTelegram == false)
							|| (this.curState == EJECTAFTERPAGEERROR)) {
						//----
						long now = System.currentTimeMillis();
						//20210112 mark by MatsudairaSyuMe TITA_TOTA_START flag checking change to PrtCli
						if (((now - startTime) > responseTimeout) || (this.curState == EJECTAFTERPAGEERROR)) {
						//----
							//this.curState = EJECTAFTERPAGEERROR;
							if (this.curState == EJECTAFTERPAGEERROR) {
								log.error("ERROR!!! received data from host timeout {} can't get connection!!!!", responseTimeout);
								amlog.info("[{}][{}][{}]:62存摺資料補登失敗！與中心連線逾時", brws, pasname, this.account);
							} else {
								log.error("ERROR!!! received data from host timeout {} release connection!!!!", responseTimeout);
								this.curState = EJECTAFTERPAGEERROR;
								amlog.info("[{}][{}][{}]:62存摺資料補登失敗！[{}]接電文逾時", brws, pasname, this.account,
									responseTimeout);
								//20210112
								//20210116 MAtsudairaSyuMe this.dispatcher.releaseConn();
								//----
							}
							SetSignal(firstOpenConn, firstOpenConn, "0000000000", "0000000001");
							// ----
							if (SetSignal(!firstOpenConn, firstOpenConn, "0000000000", "0000000001")) {
								log.debug(
										"{} {} {} AutoPrnCls : --reset printer after receive telegram error",
										brws, catagory, account);
							} else {
								log.debug(
										"{} {} {} AutoPrnCls : --reset printer after receive telegram error",
										brws, catagory, account);
							}
						}
						log.debug("startTime={} new={} (now - startTime) ={}", this.startTime, now, (now - startTime));
					}
				}
			} else {
				if (Send_Recv(this.iFig, TXP.DEL, "", tx_area.get("mbal")) != 0) {
					if (r < 0 && r != -3) { //20200619 for connect error
						// 20220818 add select for r != -3 format or TITA/TOTA tran. seq. key error
						this.curState = SESSIONBREAK;
//						amlog.info("[{}][{}][{}]:61存摺資料補登失敗！", brws, pasname, account);
						amlog.info("[{}][{}][{}]:61存摺資料刪除失敗！", brws, pasname, account);
					} else {
						//20210112 mark by MatsudairaSyuMe TITA_TOTA_START flag checking change to PrtCli
						if ((r == 0 && this.isTITA_TOTA_START() == false && this.alreadySendTelegram == false)
								|| (this.curState == EJECTAFTERPAGEERROR)) {
							//----
							long now = System.currentTimeMillis();
							//20210112 mark by MatsudairaSyuMe TITA_TOTA_START flag checking change to PrtCl
							if (((now - startTime) > responseTimeout) || (this.curState == EJECTAFTERPAGEERROR)) {
							//----
								this.curState = EJECTAFTERPAGEERROR;
								//20210108
								//20210116 MatsudairaSyume this.dispatcher.releaseConn();
								//----
								//20220121 MatsudairaSyuMe
								if ((now - startTime) > responseTimeout) {
								    log.error("ERROR!!! received data from host timeout {}", responseTimeout);
								    amlog.info("[{}][{}][{}]:62存摺刪除補登資料失敗！[{}]接電文逾時", brws, pasname, this.account,
										responseTimeout);
								} else {
								    log.error("ERROR!!! received data from host error");
								    amlog.info("[{}][{}][{}]:62存摺刪除補登資料失敗！", brws, pasname, this.account);									
								}
								SetSignal(firstOpenConn, firstOpenConn, "0000000000", "0000000001");
								// ----
								if (SetSignal(!firstOpenConn, firstOpenConn, "0000000000", "0000000001")) {
									log.debug(
											"{} {} {} AutoPrnCls : --reset printer after receive telegram error",
											brws, catagory, account);
								} else {
									log.debug(
											"{} {} {} AutoPrnCls : --reset printer after receive telegram error",
											brws, catagory, account);
								}
							}
							log.debug("startTime={} new={} (now - startTime) ={}", this.startTime, now, (now - startTime));
						}
					}
				}
			}
			switch (this.iFig) {
				case TXP.PBTYPE:
					//20210112 mark by MatsudairaSyuMe TITA_TOTA_START flag checking change to PrtCli
					log.debug(
						"SENDTLM/RECVTLM r = {} pb_arr.size=>{} isTITA_TOTA_START={} alreadySendTelegram={}=====check prtcliFSM",
						r, pb_arr.size(), this.isTITA_TOTA_START(), this.alreadySendTelegram);
					//----
					break;
				case TXP.FCTYPE:
					//20210112 mark by MatsudairaSyuMe TITA_TOTA_START flag checking change to PrtCli
					log.debug(
						"SENDTLM/RECVTLM r = {} fc_arr.size=>{} isTITA_TOTA_START={} alreadySendTelegram={}=====check prtcliFSM",
						r, fc_arr.size(), this.isTITA_TOTA_START(), this.alreadySendTelegram);
					//----
					break;
				case TXP.GLTYPE:
					//20210112 mark by MatsudairaSyuMe TITA_TOTA_START flag checking change to PrtCli
					log.debug(
						"SENDTLM/RECVTLM r = {} gl_arr.size=>{} isTITA_TOTA_START={} alreadySendTelegram={}=====check prtcliFSM",
						r, gl_arr.size(), this.isTITA_TOTA_START(), this.alreadySendTelegram);
					//----
					break;
				default:
					//20210112 mark by MatsudairaSyuMe TITA_TOTA_START flag checking change to PrtCli
					log.debug(
						"SENDTLM/RECVTLM r = {} unonow passbook type=[{}] isTITA_TOTA_START={} alreadySendTelegram={}=====check prtcliFSM",
						r, this.iFig, this.isTITA_TOTA_START(), this.alreadySendTelegram);
					//----
					break;
			}
			//20200718
			lastCheck(before);
			log.debug("after {}=>{}=====check prtcliFSM", before, this.curState);
			break;

		case STARTPROCTLM:
			amlog.info("[{}][{}][{}]:06存摺資料補登中...", brws, pasname, account);
			switch (this.iFig) {
				case TXP.PBTYPE:
					log.debug("STARTPROCTLM pb_arr.size=>{}=====check prtcliFSM", pb_arr.size());
					if (PbDataFormat()) {
						this.curState = WRITEMSR;
					}
					break;
				case TXP.FCTYPE:
					log.debug("STARTPROCTLM fc_arr.size=>{}=====check prtcliFSM", fc_arr.size());
					if (FcDataFormat()) {
						this.curState = WRITEMSR;
					}
					break;
				case TXP.GLTYPE:
					log.debug("STARTPROCTLM gl_arr.size=>{}=====check prtcliFSM", gl_arr.size());
					if (GlDataFormat()) {
						this.curState = WRITEMSR;
					}
					break;
			}
			atlog.info("補登... {}", this.iFig == TXP.PBTYPE ? "台幣存摺" : (this.iFig == TXP.FCTYPE ? "外幣存摺" : "黃金存摺"));
			//20200718
			lastCheck(before);
			log.debug("after {}=>{}=====check prtcliFSM", before, this.curState);
			//20230302 sent PSTAT beofre WRITMSR
			/* 20230809 MatsudairaSyuMe mark up for not receive data so don't send ESC, j
			if ((before != this.curState) && (this.curState == WRITEMSR)) {
				byte[] PSTAT = { (byte) 0x1b, (byte) 'j' };
				if (prt.Send_hData(PSTAT) == 0)
					log.debug("write PSTAT before WRITEMSR");
				else
					log.error("write PSTAT before WRITEMSR ERROR!!!!");
			}
			*/
			//----
			break;

		case PBDATAFORMAT:
			switch (this.iFig) {
				case TXP.PBTYPE:
					log.debug("PBDATAFORMAT pb_arr.size=>{}=====check prtcliFSM", pb_arr.size());
					if (PbDataFormat()) {
						this.curState = WRITEMSR;
					}
					break;
				case TXP.FCTYPE:
					log.debug("PBDATAFORMAT fc_arr.size=>{}=====check prtcliFSM", fc_arr.size());
					if (FcDataFormat()) {
						this.curState = WRITEMSR;
					}
					break;
				case TXP.GLTYPE:
					log.debug("PBDATAFORMAT gl_arr.size=>{}=====check prtcliFSM", gl_arr.size());
					if (GlDataFormat()) {
						this.curState = WRITEMSR;
					}
					break;
			}
			log.debug("{} {} {} AutoPrnCls : 補登... {}", brws, catagory, account, this.iFig == TXP.PBTYPE ? "台幣存摺" : (this.iFig == TXP.FCTYPE ? "外幣存摺" : "黃金存摺"));
			//20200718
			lastCheck(before);
			log.debug("after {}=>{}=====check prtcliFSM", before, this.curState);

		case FORMATPRTDATAERROR:
			log.debug("{} {} {} ORMATPRTDATAERROR :AutoPrnCls : XXDataFormat() -- Print Data Error!", brws, catagory, account);
			amlog.info("[{}][{}][{}]:61存摺資料補登失敗！", brws, pasname, account);
			SetSignal(firstOpenConn, firstOpenConn, "0000000000","0000000001");
			prt.Eject(firstOpenConn);
			Sleep(2 * 1000);
			this.iEnd = 0;
			this.iFirst = 0;
			this.curState = OPENPRINTER;
			//20200718
			lastCheck(before);
			log.debug("after {}=>{}=====check prtcliFSM", before, this.curState);
			break;

		case WRITEMSR:
			log.debug("{} {} {} :AutoPrnCls : process WRITEMSR", brws, catagory, account);
			if (WMSRFormat(firstOpenConn)) {
				/*20211028 MatsudairaSyuMe read MSR again after write MSR and check the result with previous write constant*/
				this.curState = READANDCHECKMSR;
				//amlog.info("[{}][{}][{}]:07存摺磁條寫入成功！", brws, pasname, account);//20211028 MatsudairaSyuMe read MSR again after write MSR and check the result with previous write constant
				log.debug("07存摺磁條寫入成功！ 1");
				reReadcusid = null;
				if (null != (reReadcusid = prt.MS_CheckAndRead(firstOpenConn, brws))) {//20211123 change to use MS_CheckAndRead
					if (reReadcusid.length == 1) {
						amlog.info("[{}][{}][{}]:11磁條讀取失敗(1)！", brws, "        ", "            ");
						InsertAMStatus(brws, catagory, account, "11磁條讀取失敗(1)！");
						log.debug("{} {} {} AutoPrnCls : read MSR ERROR after write: from WRITEMSR", brws, catagory, account);
						atlog.info("[{}]:AutoPrnCls : MS_Read() -- Read MSR Error(1) !", brws);
						//amlog.info("[{}][{}][{}]:13磁條比對不符", brws, pasname, account);  20211203 MatsudairaSyuMe
						//InsertAMStatus(brws, pasname, account, "13磁條比對不符");  20211203 MatsudairaSyuMe
						SetSignal(firstOpenConn, firstOpenConn, "0000000000", "0000000001");
						prt.Eject(firstOpenConn);
						Sleep(2 * 1000);
						resetPassBook();
					} else {
						amlog.info("[{}][{}][{}]:12存摺磁條讀取成功(1)！", brws, pasname, account);
						for (int i = 0; i < reReadcusid.length; i++)
							reReadcusid[i] = reReadcusid[i] == (byte) '<' ? (byte) '-' : reReadcusid[i];
						String reReadsid = new String(reReadcusid);
						atlog.info("[{}]:AutoPrnCls : MS_Read() -- c_Msr(1)=[{}]", brws, reReadsid);
						amlog.info("[{}][{}][{}]:13寫入資料：{}", brws, pasname, account, tx_area.get("c_Msr"));
						amlog.info("[{}][{}][{}]:13讀取資料：{}", brws, pasname, account, reReadsid);
						if (tx_area.get("c_Msr").equals(reReadsid)) {
							// amlog.info("[{}][{}][{}]:12存摺磁條比對正確(1)！", brws, pasname, account); 20211203 MatsudairaSyuMe
							this.curState = SNDANDRCVDELTLM;
							amlog.info("[{}][{}][{}]:13磁條比對成功", brws, pasname, account);
							log.debug("{} {} {} AutoPrnCls : --re-read and check Account: from WRITEMSR", brws, catagory, account);
						} else {
							// amlog.info("[{}][{}][{}]:12存摺磁條比對失敗(1)！{} {}", brws, pasname, account, tx_area.get("c_Msr"), reReadsid);20211203 MatsudairaSyuMe
							atlog.info("[{}]:AutoPrnCls : WMSRFormat() ERR -- c_Msr=[{}][{}]", brws, tx_area.get("c_Msr"),reReadsid);
							// InsertAMStatus(brws, pasname, account, "12存摺磁條比對失敗(1)！");20211203 MatsudairaSyuMe
							amlog.info("[{}][{}][{}]:13磁條比對不符", brws, pasname, account);
							InsertAMStatus(brws, pasname, account, "13磁條比對不符");
							log.debug("{} {} {} AutoPrnCls : read MSR ERROR [{}] after write: from WRITEMSR", brws, catagory, account,
									reReadsid);
							SetSignal(firstOpenConn, firstOpenConn, "0000000000", "0000000001");
							prt.Eject(firstOpenConn);
							Sleep(2 * 1000);
							resetPassBook();
						}
					}
				}
				//----
			} else
				this.curState = WRITEMSRWAITCONFIRM;
			log.debug("{} {} {} :AutoPrnCls : WMSRFormat() -- c_Msr=[{}]",brws, catagory, account, this.tx_area.get("c_Msr"));
			//20200522
			atlog.info("c_Msr=[{}]",this.tx_area.get("c_Msr"));
			//----
			//20200718
			lastCheck(before);
			log.debug("after {}=>{}=====check prtcliFSM", before, this.curState);
			break;

		case WRITEMSRWAITCONFIRM:
			log.debug("{} {} {} :AutoPrnCls : process WRITEMSRWAITCONFIRM", brws, catagory, account);
			if (WMSRFormat(!firstOpenConn)) {
				// 20211028 MatsudairaSyuMe read MSR again after write MSR and check the result
				// with previous write constant
				//amlog.info("[{}][{}][{}]:07存摺磁條寫入成功！", brws, pasname, account);
				this.curState = READANDCHECKMSR;
				log.debug("07存摺磁條寫入成功！ 2");
				reReadcusid = null;
				if (null != (reReadcusid = prt.MS_CheckAndRead(firstOpenConn, brws))) {//20211123 change to use MS_CheckAndRead
					if (reReadcusid.length == 1) {
						amlog.info("[{}][{}][{}]:11磁條讀取失敗(1)！", brws, "        ", "            ");
						atlog.info("[{}]:AutoPrnCls : MS_Read() -- Read MSR Error(1) !", brws);
						InsertAMStatus(brws, catagory, account, "11磁條讀取失敗(1)！");
						log.debug("{} {} {} AutoPrnCls : read MSR ERROR after write: from WRITEMSRWAITCONFIRM", brws, catagory, account);
						//amlog.info("[{}][{}][{}]:13磁條比對不符", brws, pasname, account); 20211203 MatsudairaSyuMe
						//InsertAMStatus(brws, pasname, account, "13磁條比對不符"); 20211203 MatsudairaSyuMe
						SetSignal(firstOpenConn, firstOpenConn, "0000000000", "0000000001");
						prt.Eject(firstOpenConn);
						Sleep(2 * 1000);
						resetPassBook();
					} else {
						amlog.info("[{}][{}][{}]:12存摺磁條讀取成功(1)！", brws, pasname, account);
						for (int i = 0; i < reReadcusid.length; i++)
							reReadcusid[i] = reReadcusid[i] == (byte) '<' ? (byte) '-' : reReadcusid[i];
						String reReadsid = new String(reReadcusid);
						atlog.info("[{}]:AutoPrnCls : MS_Read() -- c_Msr(1)=[{}]", brws, reReadsid);
						amlog.info("[{}][{}][{}]:13寫入資料：{}", brws, pasname, account, tx_area.get("c_Msr"));
						amlog.info("[{}][{}][{}]:13讀取資料：{}", brws, pasname, account, reReadsid);
						if (tx_area.get("c_Msr").equals(reReadsid)) {
							//amlog.info("[{}][{}][{}]:12存摺磁條比對正確(1)！", brws, pasname, account); 20211203 MatsudairaSyuMe
							this.curState = SNDANDRCVDELTLM;
							amlog.info("[{}][{}][{}]:13磁條比對成功", brws, pasname, account);
							log.debug("{} {} {} AutoPrnCls : --re-read and check Account: from WRITEMSRWAITCONFIRM", brws, catagory, account);
						} else {
							//amlog.info("[{}][{}][{}]:12存摺磁條比對失敗(1)！{} {}", brws, pasname, account, tx_area.get("c_Msr"), reReadsid); 20211203 MatsudairaSyuMe
							atlog.info("[{}]:AutoPrnCls : WMSRFormat() ERR -- c_Msr=[{}][{}]", brws, tx_area.get("c_Msr"),reReadsid);
							//InsertAMStatus(brws, pasname, account, "12存摺磁條比對失敗(1)！"); 20211203 MatsudairaSyuMe
							amlog.info("[{}][{}][{}]:13磁條比對不符", brws, pasname, account);
							InsertAMStatus(brws, pasname, account, "13磁條比對不符");
							log.debug("{} {} {} AutoPrnCls : read MSR ERROR [{}] after write: from WRITEMSRWAITCONFIRM", brws, catagory, account,
									reReadsid);
							SetSignal(firstOpenConn, firstOpenConn, "0000000000", "0000000001");
							prt.Eject(firstOpenConn);
							Sleep(2 * 1000);
							resetPassBook();
						}
					}
				}
			}
			//20200718
			lastCheck(before);
			log.debug("after {}=>{}=====check prtcliFSM", before, this.curState);
			break;
/*20211028 MatsudairaSyuMe read MSR again after write MSR and check the result with previous write constant*/
        case READANDCHECKMSR:
			log.debug("{} {} {} :AutoPrnCls : process READANDCHECKMSR", brws, catagory, account);
			reReadcusid = null;
			if (null != (reReadcusid = prt.MS_CheckAndRead(!firstOpenConn, brws))) {//20211123 change to use MS_CheckAndRead
				if (reReadcusid.length == 1) {
					amlog.info("[{}][{}][{}]:11磁條讀取失敗(1)！", brws, "        ", "            ");
					atlog.info("[{}]:AutoPrnCls : MS_Read() -- Read MSR Error(1) !", brws);
					InsertAMStatus(brws, catagory, account, "11磁條讀取失敗(1)！");
					log.debug("{} {} {} AutoPrnCls : read MSR ERROR after write: from READANDCHECKMSR", brws, catagory, account);
					//amlog.info("[{}][{}][{}]:13磁條比對不符", brws, pasname, account); 20211203 MatsudairaSyuMe
					//InsertAMStatus(brws, pasname, account, "13磁條比對不符"); 20211203 MatsudairaSyuMe
					SetSignal(firstOpenConn, firstOpenConn, "0000000000", "0000000001");
					prt.Eject(firstOpenConn);
					Sleep(2 * 1000);
					resetPassBook();
				} else {
					amlog.info("[{}][{}][{}]:12存摺磁條讀取成功(1)！", brws, pasname, account);
					for (int i = 0; i < reReadcusid.length; i++)
						reReadcusid[i] = reReadcusid[i] == (byte) '<' ? (byte) '-' : reReadcusid[i];
					String reReadsid = new String(reReadcusid);
					atlog.info("[{}]:AutoPrnCls : MS_Read() -- c_Msr(1)=[{}]", brws, reReadsid);
					amlog.info("[{}][{}][{}]:13寫入資料：{}", brws, pasname, account, tx_area.get("c_Msr"));
					amlog.info("[{}][{}][{}]:13讀取資料：{}", brws, pasname, account, reReadsid);
					if (tx_area.get("c_Msr").equals(reReadsid)) {
						//amlog.info("[{}][{}][{}]:12存摺磁條比對正確(1)！", brws, pasname, account); 20211203 MatsudairaSyuMe
						this.curState = SNDANDRCVDELTLM;
						amlog.info("[{}][{}][{}]:13磁條比對成功", brws, pasname, account);
						amlog.info("[{}][{}][{}]:72存摺資料補登成功！", brws, pasname, account);
						log.debug("{} {} {} AutoPrnCls : --re-read and check Account: from READANDCHECKMSR", brws, catagory, account);
					} else {
						//amlog.info("[{}][{}][{}]:12存摺磁條比對失敗(1)！{} {}", brws, pasname, account, tx_area.get("c_Msr"), reReadsid); 20211203 MatsudairaSyuMe
						atlog.info("[{}]:AutoPrnCls : WMSRFormat() ERR -- c_Msr=[{}][{}]", brws, tx_area.get("c_Msr"),reReadsid);
						//InsertAMStatus(brws, pasname, account, "12存摺磁條比對失敗(1)！"); 20211203 MatsudairaSyuMe
						amlog.info("[{}][{}][{}]:13磁條比對不符", brws, pasname, account);
						InsertAMStatus(brws, pasname, account, "13磁條比對不符");
						log.debug("{} {} {} AutoPrnCls : read MSR ERROR [{}] after write: from READANDCHECKMSR", brws, catagory, account, reReadsid);
						SetSignal(firstOpenConn, firstOpenConn, "0000000000", "0000000001");
						prt.Eject(firstOpenConn);
						Sleep(2 * 1000);
						resetPassBook();
					}
				}
			}
			lastCheck(before);
			log.debug("after {}=>{}=====check prtcliFSM", before, this.curState);
			break;
			/**/
		case SNDANDRCVDELTLM:
			//20210112 mark by MatsudairaSyuMe TITA_TOTA_START flag checking change to PrtCli
			log.debug("{} {} {} :AutoPrnCls : process SNDANDRCVDELTLM isTITA_TOTA_START={} alreadySendTelegram={}", brws,
					catagory, account, this.isTITA_TOTA_START(), alreadySendTelegram);
			//----
			r = 0;
			this.Send_Recv_DATAInq = false;
			if ((r = Send_Recv(this.iFig, TXP.DEL, "", tx_area.get("mbal"))) != 0) {
				if (r < 0) {
					SetSignal(firstOpenConn, firstOpenConn, "0000000000","0000000001");
					prt.Eject(firstOpenConn);
					Sleep(2 * 1000);
					this.iEnd = 0;
					this.iFirst = 0;

					this.curState = SESSIONBREAK;resetPassBook();//20220914 MatsudairaSyuMe
					amlog.info("[{}][{}][{}]:73存摺資料補登刪除失敗！", brws, pasname, account);				
				}
			}
			log.debug("SNDANDRCVDELTLM r = {} pb_arr.size()=>{}=====check prtcliFSM", r, pb_arr.size());
			//20200718
			lastCheck(before);
			log.debug("after {}=>{} r={} =====check prtcliFSM", before, this.curState, r);
			break;

		case SNDANDRCVDELTLMCHKEND:
			log.debug("{} {} {} :AutoPrnCls : process SNDANDRCVDELTLMCHKEND", brws, catagory, account);
			if (iEnd == 1) {
				if (!this.autoturnpage.equals("false")){
					
				} else {
/*//20200403					if (SetSignal(firstOpenConn, firstOpenConn, "0000000000","0101010000")) {
						this.curState = SNDANDRCVDELTLMCHKENDEJECTPRT;
					}
				*/
					SetSignal(firstOpenConn, firstOpenConn, "0000000000","0101010000");
					//20200610
//					this.curState = SNDANDRCVDELTLMCHKENDEJECTPRT;
					this.curState = SNDANDRCVDELTLMCHKENDSETSIG;
					//----
				}
			} else {
				// Show Signal
/*//20200403				if (SetSignal(firstOpenConn, firstOpenConn, "0000000000","0001000000")) {
					this.curState = SNDANDRCVDELTLMCHKENDSETSIG;
				}*/
				//20220927
				if (this.changeLightOnLastPage)
					SetSignal(firstOpenConn, firstOpenConn, "0000000000","0000000001");
				else
				//----
				SetSignal(firstOpenConn, firstOpenConn, "0000000000","0001000000");
				this.curState = SNDANDRCVDELTLMCHKENDSETSIG;
			}
			//20200718
			lastCheck(before);
			log.debug("after {}=>{} iEnd={} changeLight=[{}]=====check prtcliFSM", before, this.curState, iEnd, (this.changeLightOnLastPage ? "Yes": "No")); //20220927
			break;

		case SNDANDRCVDELTLMCHKENDSETSIG:
			log.debug("{} {} {} :AutoPrnCls : process SNDANDRCVDELTLMCHKENDSETSIG", brws, catagory, account);
			if (iEnd == 1) {
				if (!this.autoturnpage.equals("false")){
					
				} else {
					if (SetSignal(!firstOpenConn, firstOpenConn, "0000000000","0101010000")) {
						this.curState = SNDANDRCVDELTLMCHKENDEJECTPRT;
						if (prt.Eject(firstOpenConn)) {
							//20200611 turn page processing
//							this.curState = CAPTUREPASSBOOK;
							this.alreadySendTelegram = false;
							//20210112 mark by MatsudairaSyuMe TITA_TOTA_START flag checking change to PrtCli
							this.setTITA_TOTA_START(false);
							//----
							this.curState = ENTERPASSBOOKSIG;
							SetSignal(firstOpenConn, firstOpenConn, "1100000000", "0000010000");
							log.debug("{}=====resetPassBook for turn page prtcliFSM", this.curState);
							iFirst = 1;
//20200401							Sleep(2 * 1000);
							atlog.info("翻頁...");
							//----
						}
					}
				}
			} else {
				// Show Signal
				if (this.changeLightOnLastPage) {
					if (SetSignal(!firstOpenConn, firstOpenConn, "0000000000", "0000000001")) {
						this.curState = SNDANDRCVDELTLMCHKENDEJECTPRT;
						if (prt.Eject(firstOpenConn)) {
	//20200401						Sleep(2 * 1000);
							this.curState = FINISH;
						}
					}				
				} else {
					if (SetSignal(!firstOpenConn, firstOpenConn, "0000000000", "0001000000")) {
						this.curState = SNDANDRCVDELTLMCHKENDEJECTPRT;
						if (prt.Eject(firstOpenConn)) {
//20200401						Sleep(2 * 1000);
							this.curState = FINISH;
						}
					}
				}
			}
			//20200718
			lastCheck(before);
			log.debug("after {}=>{} iEnd={} changeLight=[{}]=====check prtcliFSM", before, this.curState, iEnd, (this.changeLightOnLastPage ? "Yes": "No")); //20220927
			break;

		case SNDANDRCVDELTLMCHKENDEJECTPRT:
			log.debug("{} {} {} :AutoPrnCls : process SNDANDRCVDELTLMCHKENDEJECTPRT", brws, catagory, account);
			if (iEnd == 1) {
				if (!this.autoturnpage.equals("false")){
					
				} else {
					if (prt.Eject(!firstOpenConn)) {
						//20200611 turn page processing
//						this.curState = CAPTUREPASSBOOK;
						this.alreadySendTelegram = false;
						//20210112 mark by MatsudairaSyuMe TITA_TOTA_START flag checking change to PrtCli
						this.setTITA_TOTA_START(false);
						//----
						this.curState = ENTERPASSBOOKSIG;
						SetSignal(firstOpenConn, firstOpenConn, "1100000000", "0000010000");
						log.debug("{}=====resetPassBook for turn page prtcliFSM", this.curState);
//20200401						Sleep(2 * 1000);
						atlog.info("翻頁...");
						iFirst = 1;
					}
				}
			} else {
				// Eject Priner
				if (prt.Eject(!firstOpenConn)) {
					this.curState = FINISH;
//20200401					Sleep(2 * 1000);
				}
			}
			//20200718
			lastCheck(before);
			log.debug("after {}=>{} iEnd={} =====check prtcliFSM", before, this.curState, iEnd);
			break;
			
		case FINISH:
			log.debug("{} {} {} :AutoPrnCls : process FINISH", brws, catagory, account);
			iFirst = 0;
			this.changeLightOnLastPage = false; //20220927
			if (iEnd == 2)
				iEnd = 0;
			resetPassBook();
			atlog.info("完成！！.");
			//20200718
			lastCheck(before);
			log.debug("after {}=>{} iEnd={} =====check prtcliFSM", before, this.curState, iEnd);
			break;

		default:
			//20200718
			lastCheck(before);
			log.debug("unknow status after {}=>{} iEnd={} =====check prtcliFSM", before, this.curState, iEnd);
			break;
		}
		return;
	}
	public String getClientId() {
		return this.clientId;
	}
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getByDate() {
		return byDate;
	}

	public void setByDate(String byDate) {
		this.byDate = byDate;
	}
	public Logger getAmLog() {
		return amlog;
	}
	public Logger getAtLog() {
		return atlog;
	}

	/**
	 * @return the responseTimeout
	 */
	public int getResponseTimeout() {
		return responseTimeout;
	}

	/**
	 * @param responseTimeout the responseTimeout to set
	 */
	public void setResponseTimeout(int responseTimeout) {
		this.responseTimeout = responseTimeout;
	}
    //20210324 adjust stateStartTime
	public void adjustStart() {
		if (this.durationTime > 0l)
			this.stateStartTime = System.currentTimeMillis() - this.durationTime;
		else {
			this.stateStartTime = System.currentTimeMillis();
			this.durationTime = 0l;
		}
	}
	//----
	
    //20200718 status check
	private void lastCheck(int before) {
		long now = System.currentTimeMillis();
		if (before != this.curState || (before == this.curState && (this.curState == CAPTUREPASSBOOK && this.iFirst == 0))) {
			this.lastState = before;
//20200722			this.startTime = now;
			this.stateStartTime = now;
			//----
			this.durationTime = 0l;
		} else {
//20200722			this.durationTime = now - this.startTime;
			this.durationTime = now - this.stateStartTime;
		}
//		log.error("this.durationTime={}", this.durationTime);
		if (this.durationTime > responseTimeout) {
			// 20200722
			if (before == this.curState && (this.curState == CAPTUREPASSBOOK && this.iFirst == 1)) { // 翻頁列印逾時
				//20200925
				amlog.info("[{}][{}][{}]:96超過時間尚未重新插入存摺！", brws, "        ", "            ");
				amlog.info("[{}][{}][{}]:63等待逾時或發生錯誤...[{}]", brws, pasname, String.format("%12s", this.account), responseTimeout);
				//----
				resetPassBook();
				this.curState = ENTERPASSBOOKSIG;
				log.error("ERROR!!! eject print host timeout {}", responseTimeout);
				if (this.account == null)
					this.account = "";
			} else {
				//20210108
				if (this.curState == RECVTLM) {
//	20210116 MAtsudairaSyuMe				this.dispatcher.releaseConn();
					log.error("ERROR!!! RECVTLM timeout release connect");
				}
				// ----
				this.curState = EJECTAFTERPAGEERROR;
				log.error("ERROR!!! eject print host timeout {}", responseTimeout);
				if (this.account == null)
					this.account = "";
				amlog.info("[{}][{}][{}]:90補摺機動作失敗！狀態等待逾時[{}]", brws, pasname, String.format("%12s", this.account), responseTimeout);
				SetSignal(firstOpenConn, firstOpenConn, "0000000000", "0000000001");
				// ----
				if (SetSignal(!firstOpenConn, firstOpenConn, "0000000000", "0000000001")) {
					log.debug("{} {} {} AutoPrnCls : --reset printer after receive telegram error", brws, catagory,
							account);
				} else {
					log.debug("{} {} {} AutoPrnCls : --reset printer after receive telegram error", brws, catagory,
							account);
				}
				//20211028 MatsudairaSyuMe
				prt.Eject(firstOpenConn);
				Sleep(2 * 1000);
				resetPassBook();
                //----
//			close();
//			this.curState = SESSIONBREAK;
			}
		}
		// log.debug("now={} durationTime ={}", now, durationTime);
	}
	private void updatefepdd() {
		try {
//			String tbsdy = jsel2ins.SELTBSDY("BISAP.TB_AUSVRPRM", "TBSDY", "SVRID", 1).trim();
			String selfld = "";
			if (PrnSvr.svrtbsdytbfields.indexOf(',') > -1) {
				String[] fldary = PrnSvr.svrtbsdytbfields.split(",");
				selfld = fldary[1];
			} else
				selfld = PrnSvr.svrtbsdytbfields;
			if (jsel2ins == null) {
				jsel2ins = new GwDao(PrnSvr.dburl, PrnSvr.dbuser, PrnSvr.dbpass, false);
				jsel2ins.SELTBSDY_R(PrnSvr.svrtbsdytbname, selfld, PrnSvr.svrtbsdytbmkey, "?", true);  //20220613 MatsudairaSyuMe
			}
			//20201115
//			String tbsdy = jsel2ins.SELONEFLD(PrnSvr.svrtbsdytbname, selfld, PrnSvr.svrtbsdytbmkey, PrnSvr.bkno, true).trim();  20220613
			String tbsdy = jsel2ins.SELTBSDY_R(PrnSvr.svrtbsdytbname, selfld, PrnSvr.svrtbsdytbmkey, PrnSvr.bkno, false).trim();
			log.debug("current tbsdy [{}]", tbsdy);
			if (tbsdy != null && tbsdy.length() >= 7)
				this.fepdd = tbsdy.substring(tbsdy.length() - 2).getBytes();
		} catch (Exception e) {
			e.printStackTrace();
			log.error("update state table {} error:{}", PrnSvr.svrtbsdytbname, e.getMessage());
		} finally {
/*20220525 ,20230517 MatsudairaSyuMe take out mark for connect once*/
			try {
				jsel2ins.CloseConnect();
			} catch (Exception e) {
				e.printStackTrace();
				log.error("close connect to table {} error:{}", PrnSvr.svrtbsdytbname, e.getMessage());
			}
			jsel2ins = null;
			/* 20230517 MatsudairaSyuMe take out mark for connect once */
		}
		return;
	}

	//20200906
	public int getCurState() {
		return this.curState;
	}
	/**
	 * 做為事件的被通知者
	 * 20201026 for cmdhis
	 */
	@Override
	public void onEvent(String id, EventType evt, String onsno) {
		// TODO Auto-generated method stub
		sno = onsno;
		switch (evt) {
		case ACTIVE:// 被通知要開啟
			//20201004
			if (this.curMode == evt.ACTIVE) {
				log.debug("current mode already in ACTIVE [{}]",this.curMode);
				return;
			}
			//----
			log.debug(getId() + ">>> ACTIVE sno=[{}]", sno);
			this.curMode = evt;
			//20200909 update cmd table
			try {
				if (jsel2ins == null)
					jsel2ins = new GwDao(PrnSvr.dburl, PrnSvr.dbuser, PrnSvr.dbpass, false);
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
				String t = sdf.format(new java.util.Date());
				int row = jsel2ins.UPDT(PrnSvr.cmdtbname, "CMD, CMDRESULT,CMDRESULTTIME", "'','START','" + t + "'",
						"SVRID,BRWS", PrnSvr.svrid + ",'" + this.brws + "'");
				log.debug("total {} records update status [{}]", row, this.curMode);
//20220525				jsel2ins.CloseConnect();
//				jsel2ins = null;
				//----
				//20201026
				cmdhiscon = new GwDao(PrnSvr.dburl, PrnSvr.dbuser, PrnSvr.dbpass, false);
				//20201218 add original cmd to devcmdhis
				String fldvals2 = String.format(hisfldvalssptrn2, "START", "START", t, this.remoteHostAddr,//20210427 MatsudairaSyuMe Often Misused: Authentication
						this.rmtaddr.getPort(),this.localHostAddr, this.localaddr.getPort(),"2");
//				sno = cmdhiscon.INSSELChoiceKey(PrnSvr.devcmdhistbname, "SVRID,AUID,BRWS,CMD,CMDCREATETIME,CMDRESULT,CMDRESULTTIME,CURSTUS", "1,1,'9838901','','2020-10-21 09:46:38.368000','START','2020-10-21 09:46:38.368000','0','2'", "SNO", "31", false, true);
				String[] rsno = cmdhiscon.INSSELChoiceKey(PrnSvr.devcmdhistbname, "CMD,CMDRESULT,CMDRESULTTIME,DEVIP,DEVPORT,SVRIP,SVRPORT,RESULTSTUS", fldvals2, PrnSvr.devcmdhistbsearkey, sno, false, true);
				if (rsno != null) {
					for (int i = 0; i < rsno.length; i++)
						log.debug("update =======> rsno[{}]=[{}]",i,rsno[i]);
				} else
					log.error("rsno null");
				cmdhiscon.CloseConnect();
				cmdhiscon = null;
				//----

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				log.debug("set event errot error e:[{}]", e.toString());
			}
			//--
			//20201004
			doConnect(3000);
			//----
			break;

		case INACTIVE:
			log.debug(getId() + " INACTIVE");
			break;

		case SHUTDOWN: // 被通知要關閉
			log.debug(getId() + ">>> SHUTDOWN");
			this.curMode = evt;
			break;

		case RESTART: // 被通知重開前先要關閉
			log.debug(getId() + ">>> RESTART");
			this.curMode = evt;
			break;

		default:
			log.debug(getId() + " default");
			break;
		}
	}
	public String getId() {
		return this.brws;
	}
	//----

	//20200906
	/**
	 * @return the curMode
	 */
	public EventType getCurMode() {
		return curMode;
	}

	/**
	 * @param curMode the curMode to set
	 */
	public void setCurMode(EventType curMode) {
		this.curMode = curMode;
	}
	//20201026
	public InetSocketAddress getRmtaddr() {
		return this.rmtaddr;
	}
	public InetSocketAddress getLocaladdr() {
		return this.localaddr;
	}

	//20201119 insert AM error status data
	public void InsertAMStatus(String brws, String passname, String act, String desc) {
		try {
			if (amtbcon == null)
				amtbcon = new GwDao(PrnSvr.dburl, PrnSvr.dbuser, PrnSvr.dbpass, false);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
			String t = sdf.format(new java.util.Date());
			//20211028 MatsudairaSyuMe change field position
			String fldam = String.format(amstatusptrn, t, brws, passname, act, desc);
			String[] rsno = amtbcon.INSSELChoiceKey(PrnSvr.devamtbname, PrnSvr.devamtbfields, fldam, PrnSvr.devamtbsearkey, "-1", false, false);
			if (rsno != null) {
				for (int i = 0; i < rsno.length; i++)
					log.debug("update =======> rsno[{}]=[{}]",i,rsno[i]);
			} else
				log.error("rsno null");
			amtbcon.CloseConnect();
			amtbcon = null;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.debug("insert am error status table error e:[{}]", e.toString());
		}
	}
	//----
	//20210112
	public boolean isTITA_TOTA_START() {
		return PRT_CLI_TITA_TOTA_START;
	}

	public void setTITA_TOTA_START(boolean tITA_TOTA_START) {
		PRT_CLI_TITA_TOTA_START = tITA_TOTA_START;
		log.info("setTITA_TOTA_START=[{}]", PRT_CLI_TITA_TOTA_START);
	}
	//----
	//20210716 MatsudairaSyuMe
	// convert remote socket IPv4 address to string
	private String cnvIPv4Addr2Str(String sIP, int sPort) {
		String rtn = "";
		if (sIP == null || sIP.trim().length() == 0)
			rtn = "000000000000";
		else {
			if (sIP.trim().equals("localhost"))
				sIP = "127.0.0.1";
			String[] sIPary = sIP.split("\\.");
			//System.out.println(sIP + ":" + sIPary.length);
			for (String s : sIPary)
				rtn = rtn + String.format("%03d", Integer.parseInt(s));
		}
		if (sPort <= 0)
			rtn = rtn + "00000";
		else
			rtn = rtn + String.format("%05d", sPort);
		return rtn;
	}
	//----
	//20210427 MatsudairaSyuMe
	/**
	 * @return the remoteHostAddr
	 */
	public String getRemoteHostAddr() {
		return this.remoteHostAddr;
	}
	/**
	 * @return the localHostAddr
	 */
	public String getLocalHostAddr() {
		return this.localHostAddr;
	}
	// 20211203 MatsudairaSyuMe
	private Timer timer = null;
	private void PeriodDayEndSchedule() {
		this.timer = new Timer();
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 1);
		Date firstTime = calendar.getTime();
		log.info("設定執行 Date 為：{} , Period：86400秒", firstTime);
//		timer.schedule(new DateTask(), firstTime, 86400 *1000);
		timer.scheduleAtFixedRate(new DateTask(), firstTime, 86400 *1000);
//        timer.cancel();
	}

	public class DateTask extends TimerTask {
		@Override
		public void run() {
			if (amlog != null)
				amlog.info("[{}][{}][{}]:                            ", brws, "        ", "            ");
			if (aslog != null)
				aslog.info(String.format("SCH  %s[%04d]:", curSockNm, 0));
			log.info("Task 執行時間：" + new Date());
		}
	}
	
	//20230314 create register pass book printer abnormal alart file
	private void prtAbNomalAlart(String brws) {
		try {
			File alartf = new File("Mon", StrUtil.cleanString(brws + "_alt"));
			//og.debug("alart file=[{}]", alartf.getAbsolutePath());
			if (alartf.exists() == false) {
				File parent = alartf.getParentFile();
				if (alartf.exists() == false) {
					parent.mkdirs();
				}
				alartf.createNewFile();
				FileUtils.writeStringToFile(alartf, "0", Charset.defaultCharset());
				log.debug("create alart file=[{}] seq=0", alartf.getAbsolutePath());
				log.info("stable already trigger OSM please restart [{}] asap!!!", this.brws);
				amlog.info("[{}][{}][{}]:97補摺機太久無回應，開始嘗試重啟連線", brws, "        ", "            ");									
				close();
			} else {
				try {
					String f = FileUtils.readFileToString(alartf, Charset.defaultCharset());
					log.debug("alart file=[{}] flag=[{}]", alartf.getAbsolutePath(), f);
					if (f != null && f.trim().length() > 0 && f.charAt(0) == (char) '0') {
						log.warn("stable still not trigger OSM please check stable and restart [{}]", this.brws);
						amlog.info("[{}][{}][{}]:97補摺機太久無回應且stable沒有發出警訊，請儘快重啟連線並同時檢查stable程式", brws, "        ", "            ");
					} else {
						log.info("stable already trigger OSM please restart [{}] asap!!!", this.brws);
						amlog.info("[{}][{}][{}]:97補摺機太久無回應，請儘快重啟連線", brws, "        ", "            ");
					}
				} catch (Exception e) {
					log.error("ERROR!!! check alart file  error {}", e.getMessage());
					FileUtils.writeStringToFile(alartf, "0",	Charset.defaultCharset());
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
			log.error("error!!! create or open brws alart file error");
		}
	}
	private void rmAbNomalAlart(String brws) {
		try {
			File alartf = new File("Mon", StrUtil.cleanString(brws + "_alt"));
			log.debug("alart file=[{}]", alartf.getAbsolutePath());
			if (alartf.exists() == false) {
				log.debug("alart file=[{}] not exist no need to delete", alartf.getAbsolutePath());
			} else {
				if (alartf.delete())
					log.debug("alart file=[{}] delete successfully", alartf.getAbsolutePath());
				else
					log.debug("alart file=[{}] delete failly", alartf.getAbsolutePath());
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("error!!! delete brws alart file error");
		}
	}
}
