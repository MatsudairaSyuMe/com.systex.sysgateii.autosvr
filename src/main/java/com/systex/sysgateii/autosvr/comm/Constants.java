package com.systex.sysgateii.autosvr.comm;
//20210116 MatsudairaSyume
import java.util.concurrent.ConcurrentHashMap;
//20220723 add for serial no
import java.util.concurrent.atomic.AtomicLong;
import java.time.LocalDate;
//----
import java.util.regex.Pattern;
//----
public class Constants {
	public static String LABEL_KEY_SYNC = "syncCheck";
	public static final String DEF_ENCODING = "UTF-8";

	public static final String CFG_SYS_FILE = "gw.properties";
	public static String CFG_FILE_LOCATION = "/resources/"; // 可以修改
	public static final String CFG_FILE = "memberserver.properties";
	public static final String CFG_MEMBER_ID = "mid";
	public static final String CFG_KEEP_ALIVE = "ts.keep.alive";
	public static final String CFG_IDLE_TIMEOUT = "ts.idle.timeout";
	public static final String CFG_TX_TIMEOUT = "ts.tx.timeout";
	public static final String CFG_CLIENT_ADDRESS = "client.ip.address";
	public static final String CFG_SERVER_ADDRESS = "system.ip";
	public static final String CFG_CHANNEL_NO_MIN = "channel.no.min";
	public static final String CFG_CHANNEL_NO_MAX = "channel.no.max";
	public static final String CFG_CHANNEL_BUFFER_SIZE = "channel.buffer.size";

	public static final int DEF_TIME_TO_LIVE = 30;
	public static final int DEF_CONNECT_TIMEOUT = 10;

	public static final String DEF_MEMBER_ID = "000";
	public static final int DEF_KEEP_ALIVE = 120;
	public static final int DEF_IDLE_TIMEOUT = 600;
	public static final int DEF_TX_TIMEOUT = 40;
//	public static final String DEF_CLIENT_ADDRESS = "192.168";
	public static final String DEF_CLIENT_ADDRESS = "127.0";
	public static final String DEF_SERVER_ADDRESS = "0.0.0.0";
	public static final int DEF_CHANNEL_NO_MIN = 11;
	public static final int DEF_CHANNEL_NO_MAX = 12;
	public static final int DEF_CHANNEL_BUFFER_SIZE = 8192;

	public static String SCRIPT_NAME = "TelecomCfg.java";
	public static String SCRIPT_FUNC = "getConfig()";
	public static String SCRIPT_PATH = System.getProperty("file.separator") + "script"
			+ System.getProperty("file.separator");

	public static String DEF_PCODE_R = "01001300";
	public static String DEF_PCODE_S = "01101300";

	public static final String RET_CD_TIMEOUT = "E1"; // 逾時未回應
	public static final String RET_CD_INVALID = "E2"; // 應用系統未開啟
	public static final String RET_CD_DATE_ERROR = "E3"; // 交易日期時間錯誤

//	public static final int MSG_HDR_SIZE = 102; //20150326 與文件同步扣除TextSize的4個byte
	public static final int MSG_HDR_SIZE = 98;
	public static final int MSG_ID_OFFSET = 4;
	public static final int MSG_ID_SIZE = 39;
	
	//PrtCli printer status
	public static final int READ_IDLE = 0; //00請插入存摺...
	public static final int OpenPrinterResetPrinterInitCheckStatus = 1; //00補摺機重置中...
	public static final int OpenPrinterPRESETCheckStatus = 2;
	public static final int PurgeBuffer = 2;
	public static final int CheckStatus = 3;       //95補摺機無回應！
	public static final int PRESET = 4;
	//0: 匯率顯示版 1:利率顯示版 2: AUTO46 自動補褶機 3: AUTO52 自動補褶機
	public static final String DEVRATE = "0";
	public static final String DEVINTER = "1";
	public static final String DEVAUTO46 = "2";
	public static final String DEVAUTO52 = "3";
	public static final String DEVAUTO28 = "4";
	public static final String DEVAUTO20 = "5";
	// '目前連線狀況, 0: 未啟用 1:啟用未連線 2: 啟用已連線 3: 備援狀態連線'
	public static final String STSNOTUSED = "0";
	public static final String STSUSEDINACT = "1";
	public static final String STSUSEDACT = "2";
	public static final String STSBACKACT = "3";
	//20210116  MatsudairaSyume add for incoming TOTA Telegram
	public static final ConcurrentHashMap<String, Object> incomingTelegramMap = new ConcurrentHashMap<String, Object>();
	//20220719 MatsudairaSyuMe register the outgoing time of telegram 
	public static final ConcurrentHashMap<String, Object> outgoingTelegramKeyMap = new ConcurrentHashMap<String, Object>();
	//----
	//20210122 MatsudairaSyume default sync. key
	public static final String DEFKNOCKING = "9588028820109132570743325311898426347857298773549468758875018579537757772163084478873699447306034466200616411960574122434059469100235892702736860872901247123456";
	//----
	//20210426 MatsudairaSyuMe Conduct command state
	public static final int START = 1;  //command "START"
	public static final int STOP = 2;   //command "STOP"
	public static final int RESTART = 3;  //command "RESTART"
	public static final int UNKNOWN = 0;  //command "UNKNOWN"
	//----
	//20210427 MatsudairaSyuMe Log Forging
	public static Pattern FilterNewlinePattern = Pattern.compile("\n");
	//20210628 MatsudairaSyuMe Log Forging
	public static Pattern SingleWordPattern = Pattern.compile("([\\w\\:\\\\w ./-]+\\w+(\\.)?\\w+)");

	//20220723 elegram serial no
	public static final ConcurrentHashMap<String, AtomicLong> teleSeqNoMap = new ConcurrentHashMap<String, AtomicLong>();

    public static int incrementAndGetS(String telegramKey) {
		synchronized (teleSeqNoMap) {
			if (Constants.teleSeqNoMap.containsKey(telegramKey)) {
				long r = Constants.teleSeqNoMap.get(telegramKey).getAndIncrement() % 10000;
				if (r == 0l)
					Constants.teleSeqNoMap.get(telegramKey).getAndSet(1l);
			} else {
				AtomicLong counter = new AtomicLong();
				counter.getAndSet(1l);
				Constants.teleSeqNoMap.put(telegramKey, counter);
			}
			return (int) Constants.teleSeqNoMap.get(telegramKey).get();
		}
    }

    //FAS channel seq hash map
	public static final ConcurrentHashMap<String, AtomicLong> chlSeqNoMap = new ConcurrentHashMap<String, AtomicLong>();

    public static int incrementChlAndGetS(String chlKey) {
		synchronized (chlSeqNoMap ) {
			if (Constants.chlSeqNoMap.containsKey(chlKey)) {
				long r = Constants.chlSeqNoMap.get(chlKey).getAndIncrement() % 1000;
				if (r == 0l)
					Constants.chlSeqNoMap.get(chlKey).getAndSet(1l);
			} else {
				AtomicLong counter = new AtomicLong();
				counter.getAndSet(1l);
				Constants.chlSeqNoMap.put(chlKey, counter);
			}
			return (int) Constants.chlSeqNoMap.get(chlKey).get();
		}
    }
}
