package com.systex.sysgateii.autosvr.prtCmd.Impl;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.systex.sysgateii.autosvr.autoPrtSvr.Client.PrtCli;
import com.systex.sysgateii.autosvr.autoPrtSvr.Server.PrnSvr;
import com.systex.sysgateii.autosvr.prtCmd.Printer;
import com.systex.sysgateii.autosvr.util.CharsetCnv;
import com.systex.sysgateii.autosvr.util.LogUtil;
import com.systex.sysgateii.autosvr.util.TWCategory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.util.concurrent.atomic.AtomicBoolean;

public class CS4625Impl implements Printer {
	private static Logger log = LoggerFactory.getLogger(CS4625Impl.class);
	//20201214
//	public static Logger amlog = null;
	private Logger amlog = null;
	//----
//	private static Logger atlog = LoggerFactory.getLogger("atlog");
	//20201214
//	public static Logger atlog = null;
	private Logger atlog = null;
	//----
	private byte ESQ = (byte) 0x1b;
	private byte ENQ = (byte) 0x05;
	private byte ACK = (byte) 0x06;
	private byte STX = (byte) 0x02;
	private byte ETX = (byte) 0x03;
	private byte NAK = (byte) 0x15;
	private byte CR = (byte) 0x0d;
	private byte LF = (byte) 0x0a;
	private byte MI_DATA = (byte) 0x30;
	private byte MI_STAT = (byte) 0x31;
	private byte MI_INIT = (byte) 0x32;
	private byte CC = (byte) 0x1b;
	private byte FS = (byte) 0X1C;
	private byte CC_MS_READ = (byte) 0x7b;
	private byte EF_MS_WRITE = (byte) 0x57;
	private byte PR_MS_WRITE = (byte) 0X87;
	private byte MS_WRITE_START = (byte) 0x2B;
	private byte MS_WRITE_END = (byte) 0x2C;
	// private byte MS_WRITE_END = (byte) 0x3F;
	private byte MS_PR2800_WRITE_END = (byte) 0x3B;
	private byte CC_OPEN_INSERTER = (byte) 0x75;
	private byte CC_CLOSE_INSERTER = (byte) 0x73;
	private byte CPI10 = (byte) 0x50;
	private byte CPI12 = (byte) 0x4D;
	private byte LPI = (byte) 0x2F;
	private byte CPI18 = (byte) 0x69;
	private byte STD = (byte) 0x76;
	private byte HEL = (byte) 0x77;
	private byte HVEL = (byte) 0x7E;
	private byte UPPER_MLF = (byte) 0x2C;
	private byte VEL = (byte) 0x5A; // 0x5A //0x8A
	private byte MLF = (byte) 0x2D;
	private byte CANCEL = (byte) 0x18;

	private String TRAIN_MSG = "　　　　　　　　　　訓 練 用 本 單 作 廢\n";

	// private byte CC_UPPER_CLOSE_INSERTER = (byte) 0x72;
	// private byte CC_UPPER_OPEN_INSERTER = (byte) 0x74;
	// private byte CC_UPPER_SELECT_INSERTER = (byte) 0x6C;
	// private byte CC_LOWER_SELECT_INSERTER = (byte) 0x6D;

	private byte[] S4625_PEJT = { ESQ, (byte) 0x4f };
	private byte[] S4625_PENQ = { ESQ, (byte) 'j' };
	private byte[] S4625_PERRCODE_REQ = { ESQ, (byte) 'k' };
	// private byte[] S4625_PACK = {ACK,(byte)0};
	private byte[] S4625_PENLARGE_OD = { (byte) 0x0D };
	private byte[] S4625_PENLARGE_OA = { (byte) 0x0a};
	private byte[] S4625_PREVERSE_LINEFEED = { ESQ, (byte) '7' };
	// private byte[]
	// S4625_PINIT_PRT={ESQ,(byte)'4',ESQ,(byte)'h',(byte)'0',(byte)0};
	// 20060713 , pr2(-e) 有時會印出之前傳票資料, 故在 initialize printer 時, send del , 清除buffer
	// 20060721, pr2中文自會mess suspect ,
	// private byte[]
	// S4625_PINIT_PRT={(byte)0x7f,ESQ,(byte)'4',ESQ,(byte)'h',(byte)'0',(byte)0};
	// private byte[]
	// S4625_PINIT_PRT={ESQ,(byte)'4',ESQ,(byte)'h',(byte)'0',(byte)0};
	// uprivate byte[]
	// S4625_PINIT_PRT={ESQ,(byte)'[',(byte)'5',(byte)'1',(byte)'4',(byte)0};
	private byte[] S4625_PINIT_PRT = { ESQ, (byte) '[', (byte) '0', (byte) '0', (byte) '0' };
	// FS,'J','0','0','4','2',FS,'K','0','1','4','7','0','5','2','2',0};

	private byte[] S4625_PSI = { ESQ, (byte) '[', (byte) '5', (byte) '1', (byte) '8'};
	private byte[] S4625_PSO = { ESQ, (byte) '[', (byte) '0', (byte) '0', (byte) '0' };
	private byte[] S4625_PNON_PRINT_AREA = { ESQ, (byte) 0x78 };

	private byte[] S4625_PINIT = { ESQ, (byte) '0' };
	private byte[] S4625_PRESET = { ESQ, (byte) 'l' };
	private byte[] S4625_PMS_READ = { ESQ, (byte) ']', ESQ, (byte) 'j'};
	private byte[] S4625_PMS_WRITE = { ESQ, (byte) 0x5c, ESQ, (byte) 'j'};
	private byte[] S4625_PSTAT = { ESQ, (byte) 'j' };
	private byte[] S4625_PSTD = { ESQ, (byte) '4', ESQ, (byte) 'h', (byte) '0' };
	private byte[] S4625_PHEL = { ESQ, (byte) '3'};
	private byte[] S4625_PHVEL = { ESQ, (byte) '3', ESQ, (byte) 'h', (byte) '1'};
	private byte[] S4625_PVEL = { ESQ, (byte) 'h', (byte) '1'};
	// private byte[]
	// S4625_PFINE={STX,(byte)5,MI_DATA,ESQ,(byte)0x49,(byte)0,(byte)0,ETX,(byte)0,(byte)0};
//	typedef int (WINAPI *INTPROC)(char *);		// 定義pointer of Function
	// 20040129 7,STX,3,'0',ESQ,'l',ETX,0 上口
//	         11,STX,7,'0',ESQ,'m',ESQ,'I',00H,0AH,ETX,0 下口
	// unsigned char
	// S4625_UPTHRTOPEN[7]={STX,(byte)3,(byte)'0',ESQ,(byte)'l',ETX,(byte)0};
	// unsigned char
	// S4625_DNTHRTOPEN[7]={STX,(byte)3,(byte)'0',ESQ,(byte)'m',ETX,(byte)0};

	// unsigned char
	// S4625_PEJT_UPPER[8]={STX,(byte)3,MI_DATA,CC,CC_UPPER_OPEN_INSERTER,ETX,
//				    (3+MI_DATA+CC+CC_UPPER_OPEN_INSERTER+ETX),(byte)0};
	// Read BarCode
	private byte[] S4625_BAR_CODE = { ESQ, (byte) 'W', (byte) 'A' };
	private byte[] S4625_SET_SIGNAL = { ESQ, (byte) 'e', (byte) 0, (byte) 0};
	// Set Signal
	private byte[] S4625_OFF_SIGNAL = { ESQ, (byte) 'f', (byte) 0, (byte) 0};
	private byte[] S4625_SET_BLINK = { ESQ, (byte) 'g', (byte) 0, (byte) 0, (byte) 0, (byte) 0};
	// Turn Page
	private byte[] S4625_TURN_PAGE = { ESQ, (byte) 'W', (byte) 'B'};
	private byte[] S4625_REVS_PAGE = { ESQ, (byte) 'W', (byte) 'C' };
	private byte[] S4625_DET_PASS = { ESQ, (byte) 'Y', (byte) ESQ, (byte) 'j' };
	private byte[] S4625_CANCEL = { CANCEL};  //20220828, 20220923 change from ESQ, CANCEL to CANCEL
	private byte[] inBuff = new byte[128];
	private byte[] curmsdata;
	private byte[] curbarcodedata;

	private boolean sendINIT = false;
	private byte[] init = new byte[6 + 1];
	private String prName = ""; // max char [80];
	private PrtCli pc = null;
	private String brws = "";
	private String wsno = "";
	private String type = "";
	private String autoturnpage = "";
	private String outptrn1 = "%5.5s";
	private String outptrn2 = "%4.4s";
	private String outptrn3 = "%c";
	//20200730 send Chinese add and extend bitmap font data
	private byte[] command = new byte[79];
	//----

//  State Value
	public static final int ResetPrinterInit_START    = 0;
	public static final int ResetPrinterInit          = 1;
	public static final int ResetPrinterInit_FINISH   = 2;
	public static final int OpenPrinter_START         = 3;
	public static final int OpenPrinter               = 4;
	public static final int OpenPrinter_START_2       = 5;
	public static final int OpenPrinter_2             = 6;
	public static final int OpenPrinter_FINISH        = 7;

	public static final int SetSignal                 = 8;
	public static final int SetSignal_START_2         = 9;
	public static final int SetSignal_2               = 10;
	public static final int SetSignal_START_3         = 11;
	public static final int SetSignal_3               = 12;
	public static final int SetSignal_START_4         = 13;
	public static final int SetSignal_4               = 14;
	public static final int SetSignal_FINISH          = 15;
	public static final int DetectPaper               = 16;
	public static final int DetectPaper_START         = 17;
	public static final int DetectPaper_FINISH        = 18;
	public static final int SetCPI                    = 19;
	public static final int SetCPI_START              = 20;
	public static final int SetCPI_FINISH             = 21;
	public static final int SetLPI                    = 22;
	public static final int SetLPI_START              = 23;
	public static final int SetLPI_FINISH             = 24;
	public static final int MS_Read                   = 25;
	public static final int MS_Read_START             = 26;
	public static final int MS_ReadRecvData           = 27;
	public static final int MS_Read_START_2           = 28;
	public static final int MS_Read_2                 = 29;
	public static final int MS_Read_FINISH            = 30;
	public static final int ReadBarcode               = 31;
	public static final int ReadBarcode_START         = 32;
	public static final int ReadBarcode_START_2       = 33;
	public static final int ReadBarcodeRecvData       = 34;
	public static final int ReadBarcode_FINISH        = 35;
	public static final int Prt_Text                  = 36;
	public static final int Prt_Text_START            = 37;
	public static final int Prt_Text_START_FINISH     = 38;
	public static final int Eject                     = 39;
	public static final int Eject_START               = 40;
	public static final int Eject_FINISH              = 41;
	public static final int MS_Write                  = 42;
	public static final int MS_Write_START            = 43;
	public static final int MS_Write_START_2          = 44;
	public static final int MS_WriteRecvData          = 45;
	public static final int MS_Write_FINISH           = 46;
	public static final int ADDFONT                   = 47;
	public static final int ADDFONT_START             = 48;


	public static final int CheckStatus_START         = 100;
	public static final int CheckStatus               = 101;
	public static final int CheckStatusRecvData       = 102;
	public static final int CheckStatus_FINISH        = 103;

	public static final int PorgeStatus_START         = 200;
	public static final int PorgeStatus               = 201;
	public static final int PorgeStatusRecvData       = 202;
	public static final int PorgeStatus_FINISH        = 203;

	private int curState = ResetPrinterInit_START;
	private int curChkState = CheckStatus_START;
	private int curPurState = PorgeStatus_START;
	private int iCnt = 0;
	private int detectTimeout = 0;
	private long detectStartTimeout = 0;

	private AtomicBoolean isShouldShutDown = new AtomicBoolean(false);
	private AtomicBoolean notSetCLPI = new AtomicBoolean(false);
	private AtomicBoolean m_bColorRed = new AtomicBoolean(false);
	private AtomicBoolean p_fun_flag = new AtomicBoolean(false);
	private int nCPI = 0;
	private int nLPI = 0;
	//20200915 for keep skip control code data
	private ByteBuf skiplinebuf = Unpooled.buffer(16384);
	//--
	//20240605
	private String pn = "", act = ""; //20240605
 
	public CS4625Impl(final PrtCli pc, final String brws, final String type, final String autoturnpage) {
		this.pc = pc;
		this.brws = brws;
		this.wsno = this.brws.substring(2);
		this.type = type;
		this.autoturnpage = autoturnpage;
		this.notSetCLPI.set(false);
		this.nCPI = 0;
		this.nLPI = 0;
		this.m_bColorRed.set(false);
//20200628		this.p_fun_flag.set(false);
		this.p_fun_flag.set(PrnSvr.p_fun_flag.get());
		MDC.put("WSNO", this.brws.substring(3));
		MDC.put("PID", pc.pid);
		//20201115
//		amlog = PrnSvr.amlog;
		this.amlog = pc.getAmLog();
//		atlog = PrnSvr.atlog;
		this.atlog = pc.getAtLog();
		//----
	}

	@Override
	public boolean OpenPrinter(boolean conOpen) {
		// TODO Auto-generated method stub
//		if (LockPrinter() > 0)
//			return false;
		log.debug("OpenPrinter before {} curState={}", conOpen, curState);
		byte[] data = null;

		if (conOpen)
			this.curState = ResetPrinterInit_START;
		if (this.curState < OpenPrinter_START) {
			if (!ResetPrinterInit()) {
				atlog.info("ConnectToRemote failed ret=[-1]");
				return false;
			} else {
				log.debug("1 ===<><>{} chkChkState {} {}", this.curState, this.curChkState, data);
				this.curState = OpenPrinter_START;
				Send_hData(S4625_PRESET);
			}
		}
		if (this.curState == OpenPrinter_START || this.curState == OpenPrinter) {
			if (this.curState == OpenPrinter_START) {
				this.curChkState = CheckStatus_START;
				this.curState = OpenPrinter;
			}
			data = CheckStatus();
			log.debug("2 ===<><>{} chkChkState {} {}", this.curState, this.curChkState, data);
//			if (CheckDis(data) != 0) {
//				this.curState = ResetPrinterInit_START;
//				return false;
//			}
			if (!CheckError(data)) {
//				pc.close();
//				this.curState = ResetPrinterInit_START;
				return false;
			} else {
				this.curState = OpenPrinter_START_2;
				Send_hData(S4625_PINIT_PRT);
			}
		}
		if (this.curState == OpenPrinter_START_2 || this.curState == OpenPrinter_2) {
			if (this.curState == OpenPrinter_START_2) {
				this.curState = OpenPrinter_2;
				this.curChkState = CheckStatus_START;
			}
			data = CheckStatus();
			log.debug("3 ===<><>{} chkChkState {} {}", this.curState, this.curChkState, data);
//			if (CheckDis(data) != 0) {
//				this.curState = ResetPrinterInit_START;
//				return false;
//			}
			if (!CheckError(data)) {
//				this.curState = ResetPrinterInit_START;
//				pc.close();
				return false;
			} else {
				this.curState = OpenPrinter_FINISH;
				log.debug("4 ===<><>{} chkChkState {}", this.curState, this.curChkState);
				return true;
			}
		}
		return false;
	}

	
	@Override
	public boolean ClosePrinter() {
		// TODO Auto-generated method stub
		this.curState = ResetPrinterInit_START;
		return true;
	}

	@Override
	public boolean CloseMsr() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int Send_hDataBuf(byte[] buff, int length) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int PurgeBuffer() {
		// TODO Auto-generated method stub
//		byte[] data = null;
		log.debug("PurgeBuffer curState={} curPurState={}", this.curState, this.curPurState);
		//this.curPurState = PorgeStatus_FINISH;
		pc.clientMessageBuf.clear();
		return 0;
	}

	@Override
	public int Send_hData(byte[] buff) {
		// TODO Auto-generated method stub
		if (buff == null)
			return -3;
		try {
			pc.sendBytes(buff);
			//atlog.info("[{}]-[{}{}", buff.clone().length,new String(buff, "UTF-8"), "]");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//20240503 MatsudairaSyuMe mark for curChkState e.printStackTrace();
			return -1;
		}
		return 0;
	}

	@Override
	public byte[] Rcv_Data() {
		// TODO Auto-generated method stub
		int retry = 0;
		byte[] rtn = null;
		if (getIsShouldShutDown().get())
			return "DIS".getBytes();
		do {
			//log.debug("pc.clientMessageBuf={}", pc.clientMessageBuf.readableBytes());
			if (pc.clientMessageBuf != null && pc.clientMessageBuf.readableBytes() > 2) {
				int size = pc.clientMessageBuf.readableBytes();
				byte[] buf = new byte[size];
				pc.clientMessageBuf.getBytes(pc.clientMessageBuf.readerIndex(), buf);
				if ( buf[0]== (byte)0x1b && buf[1] == (byte)'s' ) { //data from S4680 , msread,error status etc
					for (int i = 2; i < size; i++)
						if (buf[i] == (byte)0x1c) {
							rtn = new byte[i + 1];
							log.debug("Rcv_Data rtn.length={}", rtn.length);
							pc.clientMessageBuf.readBytes(rtn, 0, rtn.length);
							return rtn;
						}
				} else if (buf[0]== (byte)0x1b) {
					//20201208 add
					if ((size > 3 ) && (buf[1] == (byte)'r') && (buf[2] != (byte)'A' && buf[2] != (byte)'P')) {
						rtn = new byte[size];
					} else {
					//20240605
						if (size > 3) {
							int start = 0;
							byte skipbary[] = new byte[3];
							while ((size > 3) && (buf[start] == (byte)0x1b) && (buf[start + 1] == (byte)'r') && (buf[start + 2] == (byte)'A' || buf[start + 2] == (byte)'P')) {
								pc.clientMessageBuf.readBytes(skipbary, 0, 3); //20240605 drop the previous redundancy ESC r A or ESC r P
								size -= 3;
								start += 3;
								log.atDebug().setMessage("drop the previous redundancy ESC r A or ESC r P result size={}").addArgument(size).log();
							}
							skipbary = null;
						}
						//----20240605
						//20201208 modify
						rtn = new byte[size];//20240605 change to sdjust size
					}
					log.debug("Rcv_Data get {} bytes", rtn.length);
					pc.clientMessageBuf.readBytes(rtn, 0, rtn.length);
					//20240403 mark atlog.info("[{}]-[{}]",rtn.length,new String(rtn, 0, rtn.length)); //20201002
					return rtn;
					//20240403 MatsudairaqSyuMe add for purge garbage data for CS4625
				} else {
					log.warn("Rcv_Data have some garbage data!!!!");
					//20240510 Poor Style: Value Never Read rtn = new byte[size];
					pc.clientMessageBuf.readBytes(buf, 0, buf.length);
					int starti = -1;
					for (int i = 0; i < buf.length; i++)
						if (buf[i] == (byte)0x1b) {
							starti = i;
							break;
						}
					log.warn("Rcv_Data get ESC data from idx {}!!!!{}", starti, buf);
					if (starti > -1) {
						rtn = new byte[buf.length - starti];
						log.warn("Rcv_Data get ESC data from idx {}!!!! rtn.length={}", starti, rtn.length);
						for (int idx = 0; starti < buf.length; idx++, starti++)
							rtn[idx] = buf[starti];
						return rtn;
					} else {
						rtn = new byte[buf.length];
						for (int idx = 0; idx < buf.length; idx++)
							rtn[idx] = buf[idx];
						return rtn;
					}
				}
				//----20240403
			}//20240403 test else if (pc.clientMessageBuf != null) log.debug("pc.clientMessageBuf.readableBytes()={}", pc.clientMessageBuf.readableBytes());
//20200330			Sleep(100);
			Sleep(30); //20220429 MatsudairaSyuMe change from 50 to 33
		} while (++retry < 10);
		if (getIsShouldShutDown().get())
			return "DIS".getBytes();
		if (pc.clientMessageBuf == null || !pc.clientMessageBuf.isReadable())
			rtn = null;
		return rtn;
	}

	@Override
	public byte[] CheckStatus() {
		// TODO Auto-generated method stub
		byte[] data = null;
		log.debug("CheckStatus curState={} curChkState={}", this.curState, this.curChkState);
		//20200402 make sure there already response
/*		if ((data = Rcv_Data(3)) != null) {
			this.curChkState = CheckStatus_FINISH;
			log.debug("CheckStatus check first get data curState={} curChkState={} {} [{}]", this.curState, this.curChkState, Arrays.toString(data), new String(data, 1, 2));
			return data;
		}*/
		//----
		if (this.curChkState == CheckStatus_START) {
			this.curChkState = CheckStatus;
			log.debug("CheckStatus curState={} curChkState={}", this.curState, this.curChkState);
			if (Send_hData(S4625_PSTAT) != 0)
				return (data);
		}
		if (this.curChkState == CheckStatus) {
			this.curChkState = CheckStatusRecvData;
//			Sleep(50);
			this.iCnt = 0;
			data = Rcv_Data(3);
		} else if (this.curChkState == CheckStatusRecvData) {
//			Sleep(100);
			this.iCnt++;
			/*20201208 mark
			data = Rcv_Data(3);
			*/
			data = Rcv_Data();log.debug("CheckStatus after Rcv_Data data len={}", data != null ? data.length:0);
			//20200330 change from 3 to 20
			if (data == null && iCnt > 20) {
				amlog.info("[{}][{}][{}]:95補摺機無回應！", brws, "        ", "            ");
				this.curChkState = CheckStatus_FINISH;
				pc.close();
			} else if (data != null && !new String(data, Charset.forName("UTF-8")).equals("DIS"))
				this.curChkState = CheckStatus_FINISH;
		}
		return data;
	}

	@Override
	public byte[] Rcv_Data(int rcv_len) {
		// TODO Auto-generated method stub
		int retry = 0;
		byte[] rtn = null;
		if (getIsShouldShutDown().get())
			return "DIS".getBytes();
		do {
//			log.debug("pc.clientMessage={}", pc.clientMessage);
			if (pc.clientMessageBuf != null && pc.clientMessageBuf.isReadable()) {
				if (rcv_len <= pc.clientMessageBuf.readableBytes()) {
					rtn = new byte[rcv_len];
					pc.clientMessageBuf.readBytes(rtn);
					//atlog.info("[{}]-[{}]",rcv_len,new String(rtn));
					return rtn;
				}
			}
//20200330			Sleep(100);
			//20220430 MatsudairaSyuMe
		} while (++retry < 10);
		if (getIsShouldShutDown().get())
			return "DIS".getBytes();
		;

		if (pc.clientMessageBuf == null || !pc.clientMessageBuf.isReadable())
			rtn = null;
		return rtn;
	}
	@Override
	public boolean Prt_Text(byte[] buff) {
		// TODO Auto-generated method stub
		int i = 0, wlen = 0;
		if (buff == null || buff.length == 0)
			return (false);
		int len = buff.length;
		//20240510 Poor Style: Value Never Read byte bcc = 0x0;
		byte chrtmp = 0x0;
		byte chrtmp1 = 0x0;
		boolean dblword = false; // chinese character
		//20240510 Poor Style: Value Never Read byte[] data = null;
		//20240510 Poor Style: Value Never Read boolean bPrinterNoFont = false;

		//20240510 Poor Style: Value Never Read byte[] linefeed = null;
		int j; //20240510Poor Style: Value Never Read , offset = 0;
		//20240510 Poor Style: Value Never Read boolean bLineFeed;
		//20240510  boolean bHalf = false;
		//20211012 MatsudiaraSyuMe extend size of hBuf from 600 to 1500
		byte[] hBuf = new byte[1500];

		// filter space 91.10.09
		//20240510 Poor Style: Value Never Read bLineFeed = false;

		for (j = len - 1; j >= 0; j--) {
			if (buff[j] != (byte)0x0a && buff[j] != (byte)0x0d)
				break;
			//20240510 Poor Style: Value Never Read else
			//20240510 Poor Style: Value Never Read	bLineFeed = true;
		}
/*		if (bLineFeed) {
			linefeed = new byte[len - j - 1];
			System.arraycopy(buff, j + 1, linefeed, 0, len - j - 1);
			offset = j;
			linefeed = new byte[offset + 1];
			System.arraycopy(buff, 0, linefeed, 0, offset + 1);
			buff = linefeed;
		}*/
		len = buff.length;
		if (this.notSetCLPI.get() == true)
		{
			if (nCPI != 0)
				SetCPI(true, nCPI);
			else if (nLPI != 0)
				SetLPI(true, nLPI);
			notSetCLPI.set(false);
		}
		boolean bBeginRedSession=m_bColorRed.get();
		boolean bBeginSISession=false;
		this.curState = Prt_Text_START;
//		while (i < len ) {
//			chrtmp = buff[i];
//			chrtmp1 = buff[i+1];
/*			if((this.p_fun_flag.get() == true) && (int)(chrtmp & 0xff) >= (int)((byte)0x80 & 0xff))
			{
				log.debug("0 ===<><>{} Prt_Text check chinese Font chkChkState {} i={}", this.curState, this.curChkState, i, String.format("0x%02x%02x", chrtmp, chrtmp1));

				if (ChkAddFont((int)((chrtmp & 0x00ff)<<8)+(int)((chrtmp1 & 0xff))) == true)
				{
					AddFont((int)((chrtmp & 0x00ff)<<8)+(int)((chrtmp1 & 0xff)));
					log.debug("1 ===<><>{} Prt_Text enter S4625_PSI chkChkState {} i={} AddFont", this.curState, this.curChkState, i, String.format("0x%02x%02x", chrtmp, chrtmp1));
					i+=2;
					continue;
				}
				if (ChkExtFont((int)((chrtmp & 0x00ff)<<8)+((int)((chrtmp1 & 0xff)))) == true)
				{
					AddExtFont(chrtmp,chrtmp1);
					log.debug("2 ===<><>{} Prt_Text enter S4625_PSI chkChkState {} i={} AddExtFont", this.curState, this.curChkState, i, String.format("0x%02x%02x", chrtmp, chrtmp1));
					i+=2;
					continue;
				}
			}*/
//			log.debug("Prt_Text i={} len={}", i, len);
/*			if ( len > 1 ) {
				if ( buff[i] == (byte)0x0a &&
					 buff[i-1] != (byte)0x0d )
				{
					Send_hData(S4625_PNON_PRINT_AREA);
					Send_hData(S4625_PENLARGE_OD);
					//20060906 if addfont is the last char and follows a 0x0a , then slip won't catch newline
					if ( i == ( len -  1 ) )
						Send_hData(S4625_PENLARGE_OA);
					//Sleep(250);
//					PurgeBuffer();
					//see what happen in r8
					if (this.curState == Prt_Text_START || this.curState == Prt_Text) {
						if (this.curState == Prt_Text_START) {
							this.curChkState = CheckStatus_START;
							this.curState = Prt_Text;
						}
						data = CheckStatus();
						log.debug("2 ===<><>{} Prt_Text chkChkState {} {}", this.curState, this.curChkState, data);
						if (CheckDis(data) != 0) {
							this.curState = ResetPrinterInit_START;
							log.debug("{} {} {} {} 94補摺機狀態錯誤！", iCnt, brws, "", "");
							return false;
						}
						if (!CheckError(data)) {
							log.debug("{} {} {} {} 94存摺資料補登時發生錯誤！", iCnt, brws, "", "");
							return false;
						} else {
							this.curState = Prt_Text_START;
						}
					}
					i++;
				}
			}
			else {
				if ( buff[0] == (byte)0x0a ||
					 buff[0] == (byte)0x0d )
				{
					Send_hData(S4625_PNON_PRINT_AREA);
					Send_hData(S4625_PENLARGE_OD);
					Send_hData(S4625_PENLARGE_OA);
					//Sleep(250);
//					PurgeBuffer();
					//see what happen in r8
					//data=CheckStatus();
					//CheckError(data);
					i++;
				}
			}*/
/*			if (len > 1) {
				if (buff[i] == (byte) 0x0a && buff[i - 1] != (byte) 0x0d) {
					System.arraycopy(S4625_PNON_PRINT_AREA, 0, hBuf, wlen+3, S4625_PNON_PRINT_AREA.length);
					wlen += S4625_PNON_PRINT_AREA.length;
					System.arraycopy(S4625_PENLARGE_OD, 0, hBuf, wlen+3, S4625_PENLARGE_OD.length);
					wlen += S4625_PENLARGE_OD.length;
					System.arraycopy(S4625_PENLARGE_OA, 0, hBuf, wlen+3, S4625_PENLARGE_OA.length);
					i++;
					log.debug("2.1 ===<><>{} {} Prt_Text wlen={} i={}", this.curState, this.curChkState, wlen, i);
				}
			} else {
				if (buff[0] == (byte) 0x0a || buff[0] == (byte) 0x0d) {
					System.arraycopy(S4625_PNON_PRINT_AREA, 0, hBuf, wlen+3, S4625_PNON_PRINT_AREA.length);
					wlen += S4625_PNON_PRINT_AREA.length;
					System.arraycopy(S4625_PENLARGE_OD, 0, hBuf, wlen+3, S4625_PENLARGE_OD.length);
					wlen += S4625_PENLARGE_OD.length;
					System.arraycopy(S4625_PENLARGE_OA, 0, hBuf, wlen+3, S4625_PENLARGE_OA.length);
					i++;
					log.debug("2.2 ===<><>{} {} Prt_Text wlen={} i={}", this.curState, this.curChkState, wlen, i);
				}
			}*/
			{
				Arrays.fill(hBuf, (byte)0x0);
				while (i < len)
				{
					if ( bBeginRedSession == true ) {
						//set to red
					}
//					log.debug("3 ===<><>{} Prt_Text chkChkState {} wlen={} i={} len={} {} {} {}", this.curState, this.curChkState,wlen,i,len, (char)buff[i], Byte.toString(buff[i]), Byte.toString(buff[i+1]));
					hBuf[wlen+3] = buff[i];
					hBuf[wlen+4] = buff[i+1];
					chrtmp = buff[i];
					chrtmp1 = buff[i+1];
//					log.debug("3 ===<><>Prt_Text chkChkState {} {}", (int)(chrtmp & 0xff), (int)(0x80 & 0xff));
//20200729					if ( (int)(chrtmp & 0xff) >= (int)(0x80 & 0xff))
					if((this.p_fun_flag.get() == true) && (int)(chrtmp & 0xff) >= (int)((byte)0x80 & 0xff))
					{
						dblword = true;
						// check only , BNE 6319 , break
	/*					if (ChkAddFont(((chrtmp<<8)+(chrtmp1))) == true)
						{
							dblword = false;
							break;
						}
						if (ChkExtFont(((chrtmp<<8)+(chrtmp1))) == true)
						{
							dblword = false;
							break;
						}*/

						log.debug("0 ===<><>{} {} Prt_Text check chinese Font i={} [{}]", this.curState, this.curChkState, i, String.format("0x%02x%02x", chrtmp, chrtmp1));
						if (ChkAddFont((int)((chrtmp & 0x00ff)<<8)+(int)((chrtmp1 & 0xff))) == true)
						{
							log.debug("1 ===<><>{} {} Prt_Text i={} AddFont [{}]", this.curState, this.curChkState, i, String.format("0x%02x%02x", chrtmp, chrtmp1));
							if (AddFont((int)((chrtmp & 0x00ff)<<8)+(int)((chrtmp1 & 0xff)))) {
								//20230728 MatsudairaSyuMe check if PSI has been sent before and not yet send PSO, if it is the situation then send PSO firstly
								if ( bBeginSISession == true ) {
									log.debug("1.01 ===<><>{} {} Prt_Text leave S4625_PSO wlen={} i={}", this.curState, this.curChkState, wlen, i);
									System.arraycopy(S4625_PSO, 0, hBuf, wlen+3, 5);
									wlen+=5;
									hBuf[wlen+3] = buff[i];
								}
								//---- 20230728
								System.arraycopy(this.command, 0, hBuf, wlen+3, this.command.length);
								wlen+=this.command.length;
								bBeginSISession=false;
							} else
								log.error("1 ===<><>{} {} Prt_Text i={} AddFont [{}] error", this.curState, this.curChkState, i, String.format("0x%02x%02x", chrtmp, chrtmp1));								
							i+=2;
							dblword = false;
							continue;
						}
						if (ChkExtFont((int)((chrtmp & 0x00ff)<<8)+((int)((chrtmp1 & 0xff)))) == true)
						{
							log.debug("2 ===<><>{} {} Prt_Text i={} AddExtFont [{}]", this.curState, this.curChkState, i, String.format("0x%02x%02x", chrtmp, chrtmp1));
							if (AddExtFont(chrtmp,chrtmp1)) {
								//20230728 MatsudairaSyuMe check if PSI has been sent before and not yet send PSO, if it is the situation then send PSO firstly
								if ( bBeginSISession == true ) {
									log.debug("2.01 ===<><>{} {} Prt_Text leave S4625_PSO wlen={} i={}", this.curState, this.curChkState, wlen, i);
									System.arraycopy(S4625_PSO, 0, hBuf, wlen+3, 5);
									wlen+=5;
									hBuf[wlen+3] = buff[i];
								}
								//---- 20230728

								System.arraycopy(this.command, 0, hBuf, wlen+3, this.command.length);
								wlen+=this.command.length;
								bBeginSISession=false;
							} else
								log.error("2.1 ===<><>{} {} Prt_Text i={} AddExtFont error [{}] ", this.curState, this.curChkState, i, String.format("0x%02x%02x", chrtmp, chrtmp1));								
							i+=2;
							dblword = false;
							continue;
						}

//----
						if ( bBeginSISession == false ) {
							log.debug("4 ===<><>{} {} Prt_Text enter S4625_PSI wlen={} i={}", this.curState, this.curChkState, wlen, i);

//							memcpy(&hBuf[wlen+3],S4625_PSI,5);
							System.arraycopy(S4625_PSI, 0, hBuf, wlen+3, 5);
							bBeginSISession=true;
							wlen+=5;
							hBuf[wlen+3] = buff[i];
							hBuf[wlen+4] = buff[i+1];
						}
					}
					if (dblword == true)
					{
						//bcc = bcc + buff[i] + buff[i+1];
						i+=2;
						wlen+=2;
						dblword = false;
					}
					else
					{
						if ( bBeginSISession == true ) {
							log.debug("5 ===<><>{} {} Prt_Text leave S4625_PSO wlen={} i={}", this.curState, this.curChkState, wlen, i);
							System.arraycopy(S4625_PSO, 0, hBuf, wlen+3, 5);
							bBeginSISession=false;
							wlen+=5;
							hBuf[wlen+3] = buff[i];
						}
						if (((int)(buff[i] & 0xff) < (int)(0x20 & 0xff)) && buff[i] != (byte)0x0d && buff[i] != (byte)0x0a)
						{
							buff[i] = 0x20;
							hBuf[wlen+3] = buff[i];
						}
						if ( buff[i] == (byte)0x0d &&
							 buff[i+1] == (byte)0x0a )
						{
							i++;
							System.arraycopy(S4625_PNON_PRINT_AREA, 0, hBuf, wlen+3, S4625_PNON_PRINT_AREA.length);
							wlen += S4625_PNON_PRINT_AREA.length;
							System.arraycopy(S4625_PENLARGE_OD, 0, hBuf, wlen+3, S4625_PENLARGE_OD.length);
							wlen += S4625_PENLARGE_OD.length;
							System.arraycopy(S4625_PENLARGE_OA, 0, hBuf, wlen+3, S4625_PENLARGE_OA.length);

							log.debug("5.2 ===<><>{} {} Prt_Text wlen={} i={}", this.curState, this.curChkState, wlen, i);
						}
						if ( buff[i] == (byte)0x0a &&
							 buff[i-1] != (byte)0x0d )
						{
							log.debug("5.3 ===<><>{} {} Prt_Text wlen={} i={}", this.curState, this.curChkState, wlen, i);
							break;
						}
						if ( buff[i] == (byte)0x0d &&
							 buff[i-1] != (byte)0x0a )
						{
							log.debug("5.4 ===<><>{} {} Prt_Text wlen={} i={}", this.curState, this.curChkState, wlen, i);
							break;
						}
						i++;
						wlen++;
					}
				}
				log.debug("6 ===<><>{} {} Prt_Text leave S4625_PSO chkChkState wlen={} i={}", this.curState, this.curChkState, wlen, i);
				if ( wlen > 0 ) {
					hBuf[0]=hBuf[1]=hBuf[2]=' ';
					if ( bBeginSISession == true ) {
						log.debug("6.1 ===<><>{} {} Prt_Text leave S4625_PSO wlen={} i={}", this.curState, this.curChkState, wlen, i);
						System.arraycopy(S4625_PSO, 0, hBuf, wlen+3, 5);
						//20240510 Poor Style: Value Never Read bBeginSISession=false;
						wlen+=5;
					}
					//20060905 , to compromise the bne at last digit, 0x0a set in upper logic
					hBuf[wlen+3]=(byte)0x0;
					//20060905
//					wlen = 0;
					byte[] sendhBuf = new byte[wlen];
					System.arraycopy(hBuf, 3, sendhBuf, 0, sendhBuf.length);
					Send_hData(sendhBuf);
					log.debug("6.2 ===<><>{} {} Prt_Text leave S4625_PSO wlen={} i={}", this.curState, this.curChkState, wlen, i);
				}
			}
//		}
		return true;
	}

	/*20240311 MatsudairaSyuMe mark up
	@Override
	public boolean Prt_Text(byte[] skipbuff, byte[] buff) {
		// TODO Auto-generated method stub
	}
	*/

	@Override
	public boolean ChkAddFont(int fontno) {
		// TODO Auto-generated method stub
		return CharsetCnv.ChkAddFont(fontno);
	}

	@Override
	public boolean AddFont(int fontno) {
		// TODO Auto-generated method stub
//20200730 use class define parameter		byte[] command = new byte[79];
		//20200730
		Arrays.fill(command, (byte)0x0);
		//----
		command[0] = (byte)0x1b;
		command[1] = (byte)0x25;
		command[2] = (byte)0x43;
		command[3] = (byte)'0';
		command[4] = (byte)'0';
		command[5] = (byte)'7';
		command[6] = (byte)'2';
		try {
			System.arraycopy(PrnSvr.big5funt.getFontImageData((long) fontno), 0, command, 7, 72);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//20240503 MatsudairaSyuMe mark for System Information Leak e.printStackTrace();
			log.error("AddFont fontno=[{}] error ===<><>curState {} chkChkState {}", fontno, this.curState, this.curChkState); //20240503 change log message
			return false;
		}
		//20200730
//		Send_hData(command);
		//----
/*		if (this.curState == ADDFONT_START || this.curState == ADDFONT) {
			if (this.curState == ADDFONT_START) {
				this.curChkState = CheckStatus_START;
				this.curState = ADDFONT;
			}
			byte[] data = CheckStatus();
			log.debug("2 ===<><>{} chkChkState {} {}", this.curState, this.curChkState, data);
			if (!CheckError(data)) {
			} else {
				this.curState = ADDFONT;
				Send_hData(S4625_PINIT_PRT);
			}
		}*/
		return true;
	}

	@Override
	public byte[] Prt_hCCode(byte[] buff, int length) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void EndOfJob() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean GetPaper() {
		// TODO Auto-generated method stub
		byte[] Pdata = {ESQ,(byte)'l',ESQ,(byte)'L',(byte)'0',(byte)'0',(byte)'0'};
		Send_hData(Pdata);
//		Sleep(200);
		return true;
	}

	@Override
	public void TestCCode() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean Eject(boolean start) {
		// TODO Auto-generated method stub
		log.debug("{} {} {} Eject curState={}", brws, "", "", this.curState);
		if (start)
			this.curState = Eject_START;

		if (this.curState == Eject_START) {
			log.debug("{} {} {} 存摺退出...", brws, "", "");
			//20200430 for clean buffer
			PurgeBuffer();

			//20200331 modify for command
//			Send_hData(S4625_PINIT)
			if (Send_hData(S4625_PEJT) != 0)
				return false;
		}
		byte[] data = null;
		if (this.curState == Eject_START || this.curState == Eject) {
			if (this.curState == Eject_START) {
				this.curState = Eject;
				this.curChkState = CheckStatus_START;
			}
			data = CheckStatus();
			log.debug("1 ===<><>{} chkChkState {} {}", this.curState, this.curChkState, data);
			if (CheckDis(data) == -2) 
				return false;

			if (!CheckError(data)) {
				if (data != null  && this.curState != 39 && this.curChkState != CheckStatus_START)
					amlog.info("[{}][{}][{}]:95補摺機硬體錯誤！(EJT)", brws, "        ", "            ");
				return false;
			} else {
				log.debug("2 ===<><>{} chkChkState {} {}", this.curState, this.curChkState, data);
				this.curState = Eject_FINISH;
			}
		}
		log.debug("3 ===<><>{} chkChkState {}", this.curState, this.curChkState);
		if (curState == Eject_FINISH) {
			amlog.info("[{}][{}][{}]:06存摺退出成功！", brws, "        ", "            ");
			//20231116
			pc.InsertAMStatus(brws, this.pn, this.act, "06存摺退出成功");//20240506 add pasname and account
			//----
			return true;
		} else
			return false;

	}

	@Override
	public boolean EjectNoWait() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public byte[] MS_Read(boolean start, String brws) {
		// TODO Auto-generated method stub
		byte[] data = null;

		if (start)
			this.curState = MS_Read_START;
		log.debug("MS_Read curState={} curChkState={}", this.curState, this.curChkState);
		if (this.curState == MS_Read_START) {
			this.curState = MS_Read;
			log.debug("MS_Read curState={} curChkState={}", this.curState, this.curChkState);
			amlog.info("[{}][{}][{}]:01讀取存摺磁條中...", brws, "        ", "            ");
			if (Send_hData(S4625_PMS_READ) != 0)
				return (data);
		}
		if (this.curState == MS_Read) {
			this.curState = MS_ReadRecvData;
// 202090819 test for speed			Sleep(1500);
			Sleep(500);
			//----
			this.iCnt = 0;
			this.curmsdata = null;
			data = Rcv_Data();
		} else if (this.curState == MS_ReadRecvData) {
			Sleep(200);
			this.iCnt++;
			data = Rcv_Data();
			if (data == null && iCnt > 40) {
				amlog.info("[{}][{}][{}]:94補摺機狀態錯誤！(MSR-2)", brws, "        ", "            ");
				//20201119
				pc.InsertAMStatus(brws, "", "", "94補摺機狀態錯誤！(MSR)");
				//----
				this.curState = ResetPrinterInit_START;
				ResetPrinterInit();
				pc.close();
			} else if (data != null && !new String(data, Charset.forName("UTF-8")).equals("DIS")) {
				if (data[1] == (byte)'s') {
					if (data[2] == (byte)(0x7f & 0xff)) {
						iCnt = 0;
						amlog.info("[{}][{}][{}]:94補摺機狀態錯誤！(MSR)", brws, "        ", "            ");
						//20201119
						pc.InsertAMStatus(brws, "", "", "94補摺機狀態錯誤！(MSR-1格式錯誤)");
						//----
//						this.curState = ResetPrinterInit_START;
						//20201208 add
						this.curState = MS_Read_2;
						this.curChkState = CheckStatusRecvData;
						this.iCnt = 0;
						//----
						/*mark 20201208
						this.curState = MS_Read_FINISH;
//						ResetPrinterInit();
						byte[] nr = new byte[1];
						nr[0] = (byte)'X';
						this.curmsdata = nr;
						*/
//						pc.close();
//						return null;
						return this.curmsdata;
					} else if (data.length >= 38) {
						int lastidx = data.length - 1;
						for (; lastidx > 1; lastidx--)
							if (data[lastidx] == (byte) 0x1c)
								break;
						byte[] tmpb = new byte[lastidx - 2];
						System.arraycopy(data, 2, tmpb, 0, tmpb.length);
						this.curmsdata = tmpb;
						//20240517 Code Correctness: Call to System.gc() System.gc();
					} else {
						iCnt = 0;
						amlog.info("[{}][{}][{}]:94補摺機狀態錯誤！(MSR-1格式錯誤)", brws, "        ", "            ");
						//20201119
						pc.InsertAMStatus(brws, "", "", "94補摺機狀態錯誤！(MSR-1)");
						//----
						//20200929
						/*
						this.curState = ResetPrinterInit_START;
						ResetPrinterInit();
						pc.close();
						this.curmsdata = null;
						return null;
						*/
						byte[] nr = new byte[1];
						nr[0] = (byte)'X';
						this.curmsdata = nr;
						return this.curmsdata;
					}
				} else {
					iCnt = 0;
					amlog.info("[{}][{}][{}]:94補摺機狀態錯誤！(MSR-1)", brws, "        ", "            ");
					//20201119, 20240420 add private toAsciiStr
					pc.InsertAMStatus(brws, "", "", "94補摺機狀態錯誤！(MSR-1)磁條讀取回傳硬體錯誤代碼:" + toAsciiStr(data));
					//----
					//20220828 MatsudairaSyuMe
					/*this.curState = ResetPrinterInit_START;
					ResetPrinterInit();
					pc.close();
					this.curmsdata = null;
					return null;
					*/
					if (data.length > 4 && data[1] == (byte)'r' && data[2] == (byte)'6' && data[3] == (byte)'2' && data[4] == (byte)'3') {
						Send_hData(S4625_CANCEL);
						amlog.info("[{}][{}][{}]:95硬體錯誤代碼4[{}]", brws, "        ", "            ", new String(data, 1, data.length - 1, Charset.forName("UTF-8")));		
						ResetPrinter();
					}
					byte[] nr = new byte[1];
					nr[0] = (byte)'X';
					this.curmsdata = nr;
					return this.curmsdata;
					//----
				}
				this.curState = MS_Read_START_2;
				log.debug("MS_Read 1 ===<><>{} chkChkState {} curmsdata={}", this.curState, this.curChkState, this.curmsdata);
			}
		}
		if (this.curState == MS_Read_START_2 || this.curState == MS_Read_2) {
			if (this.curState == MS_Read_START_2) {
				this.curState = MS_Read_2;
				this.curChkState = CheckStatus_START;
			}
			data = CheckStatus();
			log.debug("MS_Read 2 ===<><>{} chkChkState {}", this.curState, this.curChkState);
			if (!CheckError(data)) {
				return null;
			} else {
				this.curState = MS_Read_FINISH;
				log.debug("MS_Read 3 ===<><>{} chkChkState {}", this.curState, this.curChkState);
				//atlog.info("ms_read data=[{}]",new String(this.curmsdata));
				return this.curmsdata;
			}
		}

		log.debug("{} {} {} {} final data.length={}", iCnt, brws, "", "", (data == null? 0: data.length));//20210413 MatsudairaSyuMe prevent Null Dereference
		return curmsdata;
	}

	@Override
	public byte[] INQ() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean DetectPaper(boolean startDetect, int iTimeout) {
		// TODO Auto-generated method stub
		long currentTime = 0;
		if (startDetect) {
			this.curState = DetectPaper_START;
			if (iTimeout > 0) {
				this.detectTimeout = iTimeout;
				this.detectStartTimeout = System.currentTimeMillis();
			} else {
				this.detectTimeout = 0;
				this.detectStartTimeout = 0;
			}
		}
		log.debug("{} {} {} DetectPaper curState={} iCnt={}", brws, "", "", this.curState, this.iCnt);
		if (this.curState == DetectPaper_START) {
			//20200325 clear buffer before send data
			//clearBuffer();
			PurgeBuffer();
			GetPaper();
			log.debug("{} {} {} DetectPaper GetPaper curState={}", brws, "", "", this.curState);
		}
		byte[] data = null;
		if (this.curState == DetectPaper_START || this.curState == DetectPaper) {
			if (this.curState == DetectPaper_START) {
				this.curState = DetectPaper;
				this.curChkState = CheckStatus_START;
			}
			data = CheckStatus();
			if (this.detectTimeout > 0) {
				currentTime = System.currentTimeMillis();
				if (((currentTime - this.detectStartTimeout) / 1000) > this.detectTimeout) {
					amlog.info("[{}][{}][{}]:96超過時間尚未重新插入存摺！", brws, "        ", "            ");
					this.curState = DetectPaper_FINISH;
					return false;
				}
			}
//			if (CheckDis(data) == -2) 
//				return false;
			if (!CheckError(data)) {
				if (!pc.connectStatus()) {
					log.debug("{} {} {} channel break", brws, "", "");
					return false;
				}
			} else {
				//20210409 MatsudairaSyuMe already gat response data reset detectStartTimeout
				this.detectStartTimeout = System.currentTimeMillis();
				log.debug("{} {} {} DetectPaper curState={} iCnt={}, reset detectStartTimeout", brws, "", "", this.curState, this.iCnt);
				//----
//20200710				atlog.info("first data is {}",Arrays.toString(data));
				//20060714 V116 , In p201/p101/p80 case , if read msr but the pr2-(e) replies '1' --> paper jame
				// and Driver send ESC '0' to reset printer , but in vain.
				// So after Open printer , if meed some errors like '1' -- paper jam , '8' -- command error, 'a' -- hw error ,
				// show the related msg to warn teller to take some action.
				// S4265
				// ESC 'r' '2' --> DOCUMENT EMPTY nopaper
				//         'P' --> paper in
				//         '8' --> command error
				//         '1' --> paper jam
				//         '4' --> paper in chasis
				//         'q' --> msr error
				//         'r' --> blank msr
				//         'X' --> print area out of paper
				//         'a' --> Receive Error
				//         0x90 --> hw error
				//         0x80 --> S4265 can not operate
				//         0xd0 --> S4265 , status not in initial config

				if (data[2] == (byte)'P') {
					amlog.info("[{}][{}][{}]:01偵測到存摺插入！", brws, "        ", "            ");
					//20231116
					pc.InsertAMStatus(brws, "", "", "01偵測到存摺插入！");
					//----
					this.curState = DetectPaper_FINISH;
					return true;
				} else if (data[2] == (byte)'A' || data[2] == (byte)'0') {  //20230322 MatsudairaSyuMe add for ESC r 0 0 0 accept normalcy data
					this.curChkState = CheckStatus_START;
					log.debug("{} {} {} get 'A' or '0 curState={} change to curChkState={} ", brws, wsno, "",this.curState,  this.curChkState);
					this.iCnt = 0;
				} else if (data[2] == (byte)'4') {
					this.curChkState = CheckStatus_START;
					//clearBuffer();
					PurgeBuffer();
					log.debug("{} {} {} get '4' paper in chasis curState={}  curChkState={} ", brws, wsno, "",this.curState,  this.curChkState);
					this.iCnt = 0;
					//20230309 reset connection
					log.debug("{} {} eject paper", brws, wsno);
					Send_hData(S4625_PEJT);  //eject paper
					Sleep(500);
					log.debug("{} {} reset printer", brws, wsno);
					ResetPrinter();
					////20240311 MatsudairaSyuMe TEST
					/*
					Sleep(500);
					log.debug("{} {} close connection", brws, wsno);
					pc.close();   //close
					*/
					//----20240311
				} else {
					switch (data[2]) {
					case (byte) '1': // 20060619 paper jam
						amlog.info("[{}][{}][{}]:94請重試一下,否則有卡紙現象", brws, "        ", "            ");
						this.curChkState = CheckStatus_START;
						this.iCnt = 0;
						break;
					case (byte) '8':
						amlog.info("[{}][{}][{}]:94指令錯誤", brws, "        ", "            ");
						break;
					case (byte) 'q':
						amlog.info("[{}][{}][{}]:94寫磁條錯檢查磁頭", brws, "        ", "            ");
						//20201119
						pc.InsertAMStatus(brws, "", "", "94寫磁條錯檢查磁頭");
						//----
						break;
					case (byte) 'r':
						amlog.info("[{}][{}][{}]:94空白磁條,請重建磁條", brws, "        ", "            ");
						//20201119
						pc.InsertAMStatus(brws, "", "", "94空白磁條,請重建磁條");
						//----
						break;
					case (byte) 'X':
						amlog.info("[{}][{}][{}]:94傳票稍短,超出可列印範圍", brws, "        ", "            ");
						break;
					case (byte) 'a':
						amlog.info("[{}][{}][{}]:94紙張插歪 或 錯誤資料格式", brws, "        ", "            ");
						break;
					case (byte) 0x90:
						amlog.info("[{}][{}][{}]:94硬體媒介故障", brws, "        ", "            ");
						break;
					case (byte) 0x80:
						amlog.info("[{}][{}][{}]:94補摺機無法運作", brws, "        ", "            ");
						break;
					default:
						amlog.info("[{}][{}][{}]:94硬體故障", brws, "        ", "            ");
						break;
					}
					//atlog.info("first data is {}",new String(data));
				}
			}
		}
		return false;
	}
	

	@Override
	public boolean ResetPrinter() {
		// TODO Auto-generated method stub
		log.debug("ResetPrinter ===<><>{}", this.curState);
		if (Send_hData(S4625_PRESET) != 0)
			return false;
		PurgeBuffer();  //20220826===============
		return true;
	}

	@Override
	public boolean ResetPrinterInit() {
		// TODO Auto-generated method stub
		log.debug("{} {} {} ResetPrinterInit curState={}", brws, "", "", this.curState);
		if (this.curState == ResetPrinterInit_START) {
			amlog.info("[{}][{}][{}]:00補摺機重置中...", brws, "        ", "            ");		
			if (Send_hData(S4625_PINIT) != 0)
				return false;
		}
		byte[] data = null;
		if (this.curState == ResetPrinterInit_START || this.curState == ResetPrinterInit) {
			if (this.curState == ResetPrinterInit_START) {
				this.curState = ResetPrinterInit;
				this.curChkState = CheckStatus_START;
			}
			data = CheckStatus();
			log.debug("1 ===<><>{} chkChkState {} {}", this.curState, this.curChkState, data);
			if (CheckDis(data) == -2) 
				return false;

			if (!CheckErrorReset(data)) {
				return false;
			} else {
				log.debug("2 ===<><>{} chkChkState {} {}", this.curState, this.curChkState, data);
				this.curState = ResetPrinterInit_FINISH;
			}
		}
		log.debug("5 ===<><>{} chkChkState {}", this.curState, this.curChkState);
		if (curState == ResetPrinterInit_FINISH) {
			amlog.info("[{}][{}][{}]:00補摺機重置完成！", brws, "        ", "            ");		
			return true;
		} else
			return false;
	}

	@Override
	public int apatoi(byte[] szBuf, int iLen) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void LogData(byte[] str, int type, int len) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean SetCPI(boolean start, int cpi) {
		// TODO Auto-generated method stub
		byte[] Pdata = {ESQ, (byte)0x0};
		if (start)
			this.curState = SetCPI_START;
		log.debug("{} {} {} SetCPI curState={}", brws, "", "", this.curState);
		if (this.curState == SetCPI_START) {
			log.debug("{} {} {} SetCPI {}...", brws, "", "", cpi);
			switch (cpi)
			{
			case 5:
				Pdata[1] = (byte)0x3c;
				break;
			case 6:
				Pdata[1] = (byte)0x3d;
				break;
			default:
				Pdata[1] = (byte)0x36;
				break;
			}

			if (Send_hData(Pdata) != 0)
				return false;
		}
		byte[] data = null;
		if (this.curState == SetCPI_START || this.curState == SetCPI) {
			if (this.curState == SetCPI_START) {
				this.curState = SetCPI;
				this.curChkState = CheckStatus_START;
			}
			data = CheckStatus();
			log.debug("1 ===<><>{} SetCPI {} {} iCnt={}", this.curState, this.curChkState, data, this.iCnt);
			if (CheckDis(data) != 0) {
				if (!pc.connectStatus()) {
					amlog.info("[{}][{}][{}]:94補摺機斷線！", brws, "        ", "            ");		
					return false;
				}
				return false;
			}else {
				log.debug("2 ===<><>{} SetCPI {} {}", this.curState, this.curChkState, data);
				this.curState = SetCPI_FINISH;
			}
		}
		log.debug("3 ===<><>{} SetCPI {}", this.curState, this.curChkState);
		if (curState == SetCPI_FINISH) {
			log.debug("{} {} {} SetCPI fiinish！", brws, "", "");
			return true;
		} else
			return false;
	}

	@Override
	public boolean SetLPI(boolean start, int lpi) {
		// TODO Auto-generated method stub
		/*
		LF1     DB      4,ESQ,'&30'             ;1/4 吋
		LF2     DB      4,ESQ,'&24'             ;1/5 吋
		LF3     DB      4,ESQ,'&20'             ;1/6 吋
		LF4     DB      4,ESQ,'&15'             ;1/8 吋
		LF5     DB      4,ESQ,'&12'             ;1/10 吋
		LF6     DB      4,ESQ,'&10'             ;1/12 吋
		*/

		byte[] Pdata = {ESQ, (byte)'&',(byte)0x0,(byte)0x0 };
		if (start)
			this.curState = SetLPI_START;
		log.debug("{} {} {} SetLPI curState={}", brws, "", "", this.curState);
		if (this.curState == SetLPI_START) {
			log.debug("{} {} {} SetLPI {}...", brws, "", "", lpi);
			switch (lpi)
			{
			case 3:
				Pdata[2]=(byte)0x34;
				Pdata[3]=(byte)0x30;
				break;
			case 4:
				Pdata[2]=(byte)0x33;
				Pdata[3]=(byte)0x30;
				break;
			case 5:
				Pdata[2]=(byte)0x32;
				Pdata[3]=(byte)0x34;
				break;
			case 6:
				Pdata[2]=(byte)0x32;
				Pdata[3]=(byte)0x30;
				break;
			case 8:
				Pdata[2]=(byte)0x31;
				Pdata[3]=(byte)0x35;
				break;
			case 10:
				Pdata[2]=(byte)0x31;
				Pdata[3]=(byte)0x32;
				break;
			case 12:
				Pdata[2]=(byte)0x31;
				Pdata[3]=(byte)0x30;
				break;
			default:
				Pdata[2]=(byte)0x32;
				Pdata[3]=(byte)0x30;
				break;
			}

			if (Send_hData(Pdata) != 0)
				return false;
		}
		byte[] data = null;
		if (this.curState == SetLPI_START || this.curState == SetLPI) {
			if (this.curState == SetLPI_START) {
				this.curState = SetLPI;
				this.curChkState = CheckStatus_START;
			}
			data = CheckStatus();
			log.debug("1 ===<><>{} SetLPI {} {} iCnt={}", this.curState, this.curChkState, data, this.iCnt);
			if (CheckDis(data) != 0) {
				if (!pc.connectStatus()) {
					amlog.info("[{}][{}][{}]:94補摺機斷線！", brws, "        ", "            ");		
					return false;
				}
				return false;
			}else {
				log.debug("2 ===<><>{} SetLPI {} {}", this.curState, this.curChkState, data);
				this.curState = SetLPI_FINISH;
			}
		}
		log.debug("3 ===<><>{} SetLPI {}", this.curState, this.curChkState);
		if (curState == SetLPI_FINISH) {
			log.debug("{} {} {} SetLPI fiinish！", brws, "", "");
			return true;
		} else
			return false;
	}

	@Override
	public boolean SetEnlarge(int type) {
		// TODO Auto-generated method stub
		return false;
	}
	//20200915 for keep skip control code data
	public void PrepareSkipBuffer() {
		this.skiplinebuf.clear();
	}
	public boolean SkipnLineBuf(int nLine) {
		//20240529 Dead Code: Expression is Always false never used in "autosvr" if (nLine == 0)
		//	return true;
		//20240529 Dead Code: Expression is Always false never used in "autosvr" if (nLine < 0) {
		//	nLine = nLine * -1;
		//	for(int i = 0;i < nLine; i++)
		//		this.skiplinebuf.writeBytes(S4625_PREVERSE_LINEFEED);
		//} else {
			byte[] pData = {ESQ, (byte)0x49, 0x0, 0x0, 0x0};
			String sptrn = String.format("%03d",nLine);
			int i = 0;
			for (final byte b: sptrn.getBytes())
				pData[2 + i++] = b;
			this.skiplinebuf.writeBytes(pData);
		//}
		return true;

	}
	public byte[] GetSkipLineBuf() {
		byte[] rtn = null;
		if (this.skiplinebuf.readableBytes() > 0) {
			rtn = new byte[this.skiplinebuf.readableBytes()];
			this.skiplinebuf.readBytes(rtn);	
		}
		return rtn;
	}
	//----
	@Override
	public boolean SkipnLine(int nLine) {
		// TODO Auto-generated method stub
		if (nLine == 0)
			return true;
		if (nLine < 0) {
			nLine = nLine * -1;
			for(int i = 0;i < nLine; i++)
				Send_hData(S4625_PREVERSE_LINEFEED);
		} else {
			byte[] pData = {ESQ, (byte)0x49, 0x0, 0x0, 0x0};
			String sptrn = String.format("%03d",nLine);
			int i = 0;
			for (final byte b: sptrn.getBytes())
				pData[2 + i++] = b;
			Send_hData(pData);
		}
		return true;
	}

	@Override
	public boolean MoveFine(byte[] value) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public byte[] PrtCCode(byte[] buff) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public char C2H(char buf1, char buf2) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	private String setpasname(String cussrc) {
		String pasname = "        ";
		String chkcatagory = cussrc.substring(3, 6);;
		// 20240412 add TW passbook's category from DB
		Set<String> twCategories = new HashSet<>(Arrays.asList(TWCategory.getTwCategories()));
		if (twCategories.contains(chkcatagory)) {
			pasname = "台幣存摺";
		} else {
			switch (chkcatagory) {
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
		}
		return pasname;
	}
		
//		switch (chkcatagory) {
//		// 台幣存摺
//			case "001":
//			case "002":
//			case "003":
//			case "004":
//			case "005":
//			case "006":
//			case "008":
//				pasname = "台幣存摺";
//				break;
//				// 外幣存摺
//			case "007":
//			case "021":
//			case "701":
//			case "702":
//			case "703":
//				pasname = "外幣存摺";
//				break;
//				// 黃金存摺
//			case "071":
//			case "072":
//				pasname = "黃金存摺";
//				break;
//			default:
//				pasname = "        ";
//				break;
//		}
//		return pasname;
//	}
	@Override
	public boolean MS_Write(boolean start, String brws, String account, byte[] buff) {
		// TODO Auto-generated method stub
		boolean rtn = false;
		String pasname = setpasname(account);this.pn = pasname;this.act = account; //20240605
		if (start) {
			if ((buff == null || buff.length == 0)) {
				log.debug("MS_Write ERROR!!! msr dat null on initial status");
				return rtn;
			} else {
				this.curState = MS_Write_START;
				this.curmsdata = null;
				//20240517 Code Correctness: Call to System.gc() System.gc();
				int i = 0;
				for (i = 0; i < buff.length; i++)
					if (buff[i] == (byte) 0x0)
						break;
				if (i == buff.length)
					i = buff.length - 1;
				log.debug("i={} buff.length={}", i, buff.length);
				this.curmsdata = new byte[i + 1 + 3];
				Arrays.fill(this.curmsdata, (byte) 0x0);
				this.curmsdata[0] = ESQ;
				this.curmsdata[1] = (byte) 0x74;
				for (i = 0; i < (this.curmsdata.length - 3); i++) {
					switch (buff[i]) {
					case (byte) 0x20:
						this.curmsdata[i + 2] = (byte) '0';
						break;
					case (byte) '+':
						this.curmsdata[i + 2] = (byte) '>';
						break;
					case (byte) '-':
						this.curmsdata[i + 2] = (byte) '<';
						break;
					default:
						this.curmsdata[i + 2] = buff[i];
						break;
					}
				}
				this.curmsdata[this.curmsdata.length - 1] = (byte) 0x1d;
			}
		}
		log.debug("MS_Write curState={} curChkState={} curmsdata={}", this.curState, this.curChkState, this.curmsdata);
		byte[] data = null;
		if (this.curState == MS_Write_START || this.curState == MS_Write) {
			if (this.curState == MS_Write_START) {
				this.curState = MS_Write;
				this.curChkState = CheckStatus_START;
			}
			//20230809 MatsudairaSyuMe mark up for not receive data data = Rcv_Data();  //20220828 data = CheckStatus() change to data = Rcv_Data()
			log.debug("1 ===<><>{} MS_Write {} {} iCnt={}", this.curState, this.curChkState, data, this.iCnt);
			if (CheckDis(data) != 0) {
				this.curChkState = CheckStatus_FINISH;
				if (!pc.connectStatus()) {
					amlog.info("[{}][{}][{}]:94補摺機斷線！", brws, pasname, account);		
					return false;
				}
			}
			if (CheckError(data)) {//20240528 mark if (data != null && !CheckError(data))
				amlog.info("[{}][{}][{}]:95補摺機硬體錯誤！(EJT)", brws, pasname, account);		
				this.curChkState = CheckStatus_FINISH;
				return false;
			} else {
				log.debug("2 ===<><>{} chkChkState {} {}", this.curState, this.curChkState, data);
				this.curChkState = CheckStatus_FINISH;
				this.curState = MS_Write_START_2;
			}
		}
		if (this.curState == MS_Write_START_2) {
			this.curChkState = CheckStatus_FINISH;
			amlog.info("[{}][{}][{}]:07存摺磁條寫入中...", brws, pasname, account);		
			Send_hData(this.curmsdata);

			// actual ms write
			Send_hData(S4625_PMS_WRITE);

		}
		if (this.curState == MS_Write_START_2) {
			this.curState = MS_WriteRecvData;
//20200821			Sleep(1500);
			Sleep(500);
			this.iCnt = 0;
			data = null;
			log.debug("3 ===<><>{} chkChkState {} {}", this.curState, this.curChkState, data);
			data = Rcv_Data();
		} else if (this.curState == MS_WriteRecvData) {
			Sleep(200);
			this.iCnt++;
			log.debug("4 ===<><>{} chkChkState {} {}", this.curState, this.curChkState, data);
			while (null != (data = Rcv_Data()) && !new String(data, Charset.forName("UTF-8")).equals("DIS")) {
				if (data[2] == (byte) 'P') {
					log.debug("MS_Write 5 ===<><>{} chkChkState {}", this.curState, this.curChkState);
					this.curState = MS_Write_FINISH;
					rtn = true;
					break;
				} else if (data[2] == (byte) 's') {
					amlog.info("[{}][{}][{}]:94補摺機狀態錯誤！(MSW)", brws, pasname, account);		
					//20201119
					//20240110 mark pc.InsertAMStatus(brws, pasname, account, "94補摺機狀態錯誤！(MSW)");
					//----
					//20211203 MatsudairaSyuMe
					amlog.info("[{}][{}][{}]:71存摺磁條寫入失敗！", brws, pasname, account);
					pc.InsertAMStatus(brws, pasname, account, "71存摺磁條寫入失敗！");
					//----
					this.curState = ResetPrinterInit_START;
					ResetPrinterInit();
					pc.close();
					break;
				} else if (data.length >= 3) {
					switch (data[2]) {
					case (byte) 'a': // receive error on hardware error
					case (byte) '9': // MAGNETIC STRIPE READ/write error
					case (byte) 'r': // MAGNETIC STRIPE READ error
					case (byte) 'E': // Obstacles with media
						//20200618
//						if (Send_hData(S4625_PERRCODE_REQ) != 0)
//							return false;
//						Sleep(50);
//						data = Rcv_Data(5);
						amlog.info("[{}][{}][{}]:95硬體錯誤代碼3(MSW)[{}]", brws, pasname, account, new String(data, Charset.forName("UTF-8")));
						//20201119
						//20240110 mark pc.InsertAMStatus(brws, pasname, account, "95硬體錯誤代碼3(MSW)"+new String(data));
						//----
						//20211203 MatsudairaSyuMe
						amlog.info("[{}][{}][{}]:71存摺磁條寫入失敗！", brws, pasname, account);
						pc.InsertAMStatus(brws, pasname, account, "71存摺磁條寫入失敗！");
						//----
						Send_hData(S4625_PERRCODE_REQ);
						if (Send_hData(S4625_PSTAT) == 0)
							return false;
//						this.curState = ResetPrinterInit_START;
//						ResetPrinterInit();
						break;
					case (byte) 'q':
						if (Send_hData(S4625_PERRCODE_REQ) == 0)
							return false;
						Sleep(50);
						data = Rcv_Data(5);
						amlog.info("[{}][{}][{}]:94補摺機狀態錯誤！(MSW) ERROR:[{}]", brws, pasname, account, data);		
						//20201119
						//20240110 mark pc.InsertAMStatus(brws, pasname, account, "94補摺機狀態錯誤！(MSW) ERROR:"+new String(data));
						//----
						// 20060706 , if write eorror , retry 3 times
						/*
						 * iRetryCnt++; if ( iRetryCnt < 1 ) { unsigned char S4625_PCLEAR[2]={0x7f,0};
						 * 
						 * Send_hData(S4625_PRESET); Send_hData(S4625_PCLEAR); goto S4265_MSRW_Retry; }
						 */
						//20211203 MatsudairaSyuMe
						amlog.info("[{}][{}][{}]:71存摺磁條寫入失敗！", brws, pasname, account);
						pc.InsertAMStatus(brws, pasname, account, "71存摺磁條寫入失敗！");
						//----
						this.curState = ResetPrinterInit_START;
						ResetPrinterInit();
						return false;
					case (byte) '8':
						amlog.info("[{}][{}][{}]:94補摺機指令錯誤！(MSW)", brws, pasname, account);		
						//20201119
						//20240110 mark pc.InsertAMStatus(brws, pasname, account, "94補摺機指令錯誤！(MSW)");
						//----
						//20211203 MatsudairaSyuMe
						amlog.info("[{}][{}][{}]:71存摺磁條寫入失敗！", brws, pasname, account);
						pc.InsertAMStatus(brws, pasname, account, "71存摺磁條寫入失敗！");
						//----
						this.curState = ResetPrinterInit_START;
						//20210414 MatsudairaSyuMe
						ResetPrinterInit();
						pc.close();
						//----
						return false;
					//20200618  for get paper overload process
					case (byte) 'X':
						if (Send_hData(S4625_PERRCODE_REQ) == 0)
							return false;
					//----
					//20220827 MatsudairaSyuMe
					case (byte) '4':
						if (data[2] == (byte) '4' && data[3] == (byte) '7' && data[4] == (byte) '5') {
							ResetPrinter();
							this.curChkState = CheckStatus_START;
							amlog.info("[{}][{}][{}]:95硬體錯誤代碼3(r475)", brws, pasname, account);		
						}
						//----
						return true;
					//20240420 MatsudairaSyuMe
					case (byte) '6': // read error response
						/*20240605 mark up for ignore 6xx status code
						byte[] nr = new byte[1];
						nr[0] = (byte)'W';
						this.curmsdata = nr;
						amlog.info("[{}][{}][{}]:95硬體錯誤代碼5[{}]", brws, "        ", "            ", new String(data, 1, data.length - 1, Charset.forName("UTF-8")));
						String s = "94補摺機狀態錯誤！磁條寫入時回傳硬體錯誤碼代碼:" + toAsciiStr(data);
						pc.InsertAMStatus(brws, "", "", s);
						*/
						amlog.info("[{}][{}][{}]:95硬體錯誤代碼5[{}]", brws, "        ", "            ", new String(data, 1, data.length - 1, Charset.forName("UTF-8")));
						Send_hData(S4625_CANCEL);
						ResetPrinter();
						this.curState = MS_Write_FINISH;
						rtn = true;
						break;
						/*20240605 mark up for ignore 6xx status code and just ignore and reset
						this.curState = ResetPrinterInit_START;
						ResetPrinterInit();
						data = CheckStatus();
						if (data != null && data.length > 0) {
							this.curmsdata = data;
							this.curState = ResetPrinterInit_FINISH;
							amlog.info("[{}][{}][{}]:00補摺機重置完成！", brws, "        ", "            ");	
						}
						return true;*/
					//----20240420
					default:
						// 20060713 , RECEVIE UNKNOWN ERROR , JUST RESET
						this.curState = ResetPrinterInit_START;
						ResetPrinter();
						return false;
					}
				} else if (iCnt > 40) {
					amlog.info("[{}][{}][{}]:94補摺機狀態錯誤！(MSR-2)", brws, pasname, account);		
					//20201119
					//20240110 mark pc.InsertAMStatus(brws, pasname, account, "94補摺機狀態錯誤！(MSR-2)");
					//----
					//20211203 MatsudairaSyuMe
					amlog.info("[{}][{}][{}]:71存摺磁條寫入失敗！", brws, pasname, account);
					pc.InsertAMStatus(brws, pasname, account, "71存摺磁條寫入失敗！");
					//----
					this.curState = ResetPrinterInit_START;
					ResetPrinterInit();
					pc.close();
				}
				log.debug("MS_Write 4 ===<><>{} chkChkState {}", this.curState, this.curChkState);
				this.iCnt++;
			}
		}
		//----
		log.debug("{} {} {} {} final data.length={} write msr{}", iCnt, brws, "", "", (data == null) ? 0: data.length, this.curmsdata);
		return rtn;
	}

	@Override
	public boolean Parsing(boolean start, byte[] str) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public int DeParam(byte[] str) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public byte[] cstrchr(byte[] str, byte c) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int change_big5_ser(byte first, byte second) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int Prt_Big5(byte high, byte low) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int search_24(int sernum, byte[] pattern) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int LockPrinter() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int ReleasePrinter() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean SetInkColor(int color) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean SetTrain() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean ChkExtFont(int fontno) {
		// TODO Auto-generated method stub
		return CharsetCnv.ChkExtFont(fontno);
	}

	@Override
	public boolean AddExtFont(byte high, byte low) {
		// TODO Auto-generated method stub
		//20200730 use class define parameter		byte[] command = new byte[79];
		//20200730
		Arrays.fill(command, (byte)0x0);
		//----

		command[0] = (byte)0x1b;
		command[1] = (byte)0x25;
		command[2] = (byte)0x43;
		command[3] = (byte)'0';
		command[4] = (byte)'0';
		command[5] = (byte)'7';
		command[6] = (byte)'2';

		long fontno = (long)((high & 0x00ff) << 8)+((long)((low & 0xff)));
		try {
			System.arraycopy(PrnSvr.big5funt.getFontImageData((long) fontno), 0, command, 7, 72);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//20240503 MatsudairaSyuMe mark for curChkState e.printStackTrace();
			log.error("AddExtFont fontno=[{}] error ===<><>curState {} chkChkState {} {}", fontno, this.curState, this.curChkState);//20240503 change log message
			return false;
		}
		//20200730
//		Send_hData(command);
		//----
		return true;
	}

	@Override
	public boolean CheckPaper(boolean startDetect, int iTimeout) {
		//20240327 MatsudairaSyuMe
		long currentTime = 0;
		if (startDetect) {
			this.curState = DetectPaper_START;
			if (iTimeout > 0) {
				this.detectTimeout = iTimeout;
				this.detectStartTimeout = System.currentTimeMillis();
			} else {
				this.detectTimeout = 0;
				this.detectStartTimeout = 0;
			}
		}
		log.debug("{} {} {} CheckPaper curState={} iCnt={}", brws, "", "", this.curState, this.iCnt);
		if (this.curState == DetectPaper_START) {
			PurgeBuffer();
			log.debug("{} {} {} CheckPaper PurgeBuffer curState={}", brws, "", "", this.curState);
		}
		byte[] data = null;
		if (this.curState == DetectPaper_START || this.curState == DetectPaper) {
			if (this.curState == DetectPaper_START) {
				this.curState = DetectPaper;
				this.curChkState = CheckStatus_START;
			}
			//20240327 MatsudairaSyuMe
			else if (this.curState == DetectPaper && this.iCnt > 0) {
				this.curChkState = CheckStatusRecvData;
//				log.debug("{} {} {} CheckPaper curState={} iCnt={} curChkState={}", brws, "", "", this.curState, this.iCnt, this.curChkState);
			}
			//----20240327
			data = CheckStatus();
			if (this.detectTimeout > 0) {
				currentTime = System.currentTimeMillis();
				if (((currentTime - this.detectStartTimeout) / 1000) > this.detectTimeout) {
					log.info("[{}][{}][{}]:96超過時間尚偵測到存摺！", brws, "        ", "            ");
					this.curState = DetectPaper_FINISH;
					return false;
				}
			}
			if (!CheckError(data)) {
				if (!pc.connectStatus()) {
					log.debug("{} {} {} channel break", brws, "", "");
					return false;
				}
				//20240327 MatsydairaSyuMe markup ----return true;  ////test
			} else {
				//20210409 MatsudairaSyuMe already gat response data reset detectStartTimeout
				this.detectStartTimeout = System.currentTimeMillis();
				log.debug("{} {} {} CheckPaper curState={} iCnt={}, reset detectStartTimeout", brws, "", "", this.curState, this.iCnt);
				//----
				//20060714 V116 , In p201/p101/p80 case , if read msr but the pr2-(e) replies '1' --> paper jame
				// and Driver send ESC '0' to reset printer , but in vain.
				// So after Open printer , if meed some errors like '1' -- paper jam , '8' -- command error, 'a' -- hw error ,
				// show the related msg to warn teller to take some action.
				// S4265
				// ESC 'r' '2' --> DOCUMENT EMPTY nopaper
				//         'P' --> paper in
				//         '8' --> command error
				//         '1' --> paper jam
				//         '4' --> paper in chasis
				//         'q' --> msr error
				//         'r' --> blank msr
				//         'X' --> print area out of paper
				//         'a' --> Receive Error
				//         0x90 --> hw error
				//         0x80 --> S4265 can not operate
				//         0xd0 --> S4265 , status not in initial config
				if (data[2] == (byte)'P') {
					log.info("[{}][{}][{}]:偵測到存摺", brws, "        ", "            ");
					this.curState = DetectPaper_FINISH;
					return true;
				} else if (data[2] == (byte)'A' || data[2] == (byte)'0') {  //20230322 MatsudairaSyuMe add for ESC r 0 0 0 accept normalcy data
					this.curChkState = CheckStatus_START;
					log.debug("{} {} {} get 'A' or '0 curState={} change to curChkState={} ", brws, wsno, "",this.curState,  this.curChkState);
					this.iCnt = 0;
				} else if (data[2] == (byte)'4') {
					this.curChkState = CheckStatus_START;
					PurgeBuffer();
					log.debug("{} {} {} get '4' paper in chasis curState={}  curChkState={} ", brws, wsno, "",this.curState,  this.curChkState);
					this.iCnt = 0;
					log.debug("{} {} reset printer curState={}  curChkState={}", brws, wsno, this.curState,  this.curChkState);
					//20240327 MatsudairaSyuMe mark ResetPrinter();
					return true;
					//----20240327
					//----
				} else {
					switch (data[2]) {
					case (byte) '1': // 20060619 paper jam
						amlog.info("[{}][{}][{}]:94請重試一下,否則有卡紙現象", brws, "        ", "            ");
						this.curChkState = CheckStatus_START;
						this.iCnt = 0;
						break;
					case (byte) '8':
						amlog.info("[{}][{}][{}]:94指令錯誤", brws, "        ", "            ");
						break;
					case (byte) 'q':
						amlog.info("[{}][{}][{}]:94寫磁條錯檢查磁頭", brws, "        ", "            ");
						//20201119
						pc.InsertAMStatus(brws, "", "", "94寫磁條錯檢查磁頭");
						//----
						break;
					case (byte) 'r':
						amlog.info("[{}][{}][{}]:94空白磁條,請重建磁條", brws, "        ", "            ");
						//20201119
						pc.InsertAMStatus(brws, "", "", "94空白磁條,請重建磁條");
						//----
						break;
					case (byte) 'X':
						//20240327 MatsudairasyuMe send 
						PurgeBuffer();
					    /*20240408 MatsudairaSyuMe get paper lower change to reset
						Send_hData(S4625_PERRCODE_REQ);
						*/
						log.debug("{} {} paper lower send perrcode_req curState={}  curChkState={}", brws, wsno, this.curState,  this.curChkState);
						amlog.info("[{}][{}][{}]:94傳票稍短,超出可列印範圍", brws, "        ", "            ");
						//----20240327
						//20240408 MatsudairaSyuMe get paper lower change to reset
						return true; //break;
					case (byte) 'a':
						amlog.info("[{}][{}][{}]:94紙張插歪 或 錯誤資料格式", brws, "        ", "            ");
						break;
					case (byte) 0x90:
						amlog.info("[{}][{}][{}]:94硬體媒介故障", brws, "        ", "            ");
						break;
					case (byte) 0x80:
						amlog.info("[{}][{}][{}]:94補摺機無法運作", brws, "        ", "            ");
						break;
					default:
						amlog.info("[{}][{}][{}]:94硬體故障", brws, "        ", "            ");
						break;
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean CheckError(byte[] data) {
		if (data == null || data.length == 0)
			return false;
		//20230322 MatsudairaSyuMe ignore the data not start with ESQ
		if (data.length > 0 && data[0] != ESQ) {
			log.warn("{} {} ignore receive data not start with ESC [{}]", brws, wsno, data);
			//20240510 Poor Style: Value Never Read data = null;
			PurgeBuffer();
			return false;
		}
		//----20230322 end
		// TODO Auto-generated method stub
		// S4265
		// ESC 'r' '2' --> DOCUMENT EMPTY nopaper
		// 'P' --> paper in
		// '8' --> command error
		// '1' --> paper jam
		// '4' --> paper in chasis
		// '6' --> ???
		// 'q' --> msr error
		// 'r' --> blank msr
		// 'X' --> Warning , print area out of paper
		// 'a' --> Hard error conditions
		// 'b' --> Receive Error
		// 'A' --> Warning waiting passbook insert
		// 0x90 --> hw error
		// 0x80 --> S4265 can not operate
		// 0xd0 --> S4265 , status not in initial config
		switch (data[2]) {
		case (byte) '2':
		case (byte) '4':
			//20200401
			if (this.curState == Eject || this.curState == SetSignal_4) {
				this.curChkState = CheckStatus_START;
				//20200827
//				amlog.info("[{}][{}][{}]:95硬體錯誤代碼3[{}]", brws, "        ", "            ", new String(data));
				//----
				//20220923 special for r427
				if (data[2] == (byte) '4' && data[3] == (byte) '2' && data[4] == (byte) '7') {
					Send_hData(S4625_CANCEL);
					ResetPrinter();
					return true;
				} else
					//----
					return false;
			}
			//20220827 MatsudairaSyuMe for ESC,r475, 20240403 Special for CS4625 ignore the last code
			if (data.length > 3 && data[2] == (byte) '4' && data[3] == (byte) '7') {
				ResetPrinter();
				this.curChkState = CheckStatus_START;
				//20240403 Special for CS4625 ignore the last code
				if (data.length > 4)
					amlog.info("[{}][{}][{}]:95硬體錯誤代碼3[{}]", brws, "        ", "            ", "r475");
				else
					amlog.info("[{}][{}][{}]:95硬體錯誤代碼3[{}]不完整!!請儘快維修機器", brws, "        ", "            ", "r47");
				//----20240403
				log.error("[{}][{}][{}]:95硬體錯誤代碼3[{}]", brws, "        ", "            ", "r475");
			}  //20230309 MatsudairaSyuMe for ESC,r434
			//20240327 MAtsudairaSyuME TEST
			else if (data.length > 3 && (data[2] == (byte) '4' && data[3] == (byte) '3'))
//			else if (data.length > 3 && (data[2] == (byte) '4'))
			{
				//20240403 Special for CS4625 ignore the last code
				if (data.length > 4)
					amlog.info("[{}][{}][{}]:95硬體錯誤代碼3[{}]", brws, "        ", "            ", "r434");
				else
					amlog.info("[{}][{}][{}]:95硬體錯誤代碼3[{}]不完整!!請儘快維修機器", brws, "        ", "            ", "r43");
				//----20240403
				log.error("[{}][{}][{}]:95硬體錯誤代碼3[{}] reset printer", brws, "        ", "            ", "r434");
				this.curChkState = CheckStatus_START;
				log.debug("{} {} eject paper", brws, wsno);
				Send_hData(S4625_PEJT);  //eject paper
				Sleep(500);
				log.debug("{} {} reset printer", brws, wsno);
				ResetPrinter();
				Sleep(500);
				log.debug("{} {} close connection", brws, wsno);
				pc.close();   //close
				return false;		
			}
			//--20240327
			//20240327 MatsudairaSyuME TEST
			else if (data.length > 3 && (data[2] == (byte) '4') && (data[3] == ESQ) && (data[4] == (byte)'r') && (data[5] == (byte)'4')) {
				//20240327
				log.error("[{}][{}][{}]:95硬體錯誤代碼3[{}] paper in printer purge buffer", brws, "        ", "            ", "r4.r4");
				this.curChkState = CheckStatus_START;
				PurgeBuffer();
				//----2024031
			}

			//----20230309
			//----
			return true;
		//----
		case (byte) 'P':
		case (byte) 'A':
		case (byte) 0xd0:
			return true;
		case (byte) '1': // 20060619 paper jam
			Send_hData(S4625_PERRCODE_REQ);
			Sleep(50);
			data = Rcv_Data(5);
			// 20091002 , show error code
			amlog.info("[{}][{}][{}]:95硬體錯誤代碼1[{}]", brws, "        ", "            ", String.format(outptrn1, data));		

			ResetPrinter();
			//20230204 Test
			if (this.curState == DetectPaper) {
				this.curChkState = CheckStatus_START;
				pc.close();
				return false;
			}
			//-----
			this.curState = ResetPrinterInit_START;
			ResetPrinterInit();
			return false;
		case (byte) 'a': // 20060801 hardware error ,ESCra , this may need resetInit()
		case (byte) 'b':
			// 20170116 , fix
			// ResetPrinterInit();
			/* 20230314 MatsudairaSyuMe change to close and reset connection
			ResetPrinter();
			this.curState = ResetPrinterInit_START;
			ResetPrinterInit();
			*/
			//20230314 MatsudairaSyuMe change to close and reset connection
			this.curChkState = CheckStatus_START;
			log.debug("{} {} eject paper", brws, wsno);
			Send_hData(S4625_PEJT);  //eject paper
			Sleep(500);
			log.debug("{} {} reset printer", brws, wsno);
			ResetPrinter();
			Sleep(500);
			log.debug("{} {} close connection", brws, wsno);
			pc.close();   //close
			//----20230314 end

			return false;
		case (byte) '8':
			// command error,
			Send_hData(S4625_PERRCODE_REQ);
			Sleep(50);
			data = Rcv_Data(5);
			// 20091002 , show error code
			amlog.info("[{}][{}][{}]:95硬體錯誤代碼2[{}]]", brws, "        ", "            ", String.format(outptrn1, data));		

			//2030309 add for reset conection
			this.curChkState = CheckStatus_START;
			log.debug("{} {} eject paper", brws, wsno);
			Send_hData(S4625_PEJT);  //eject paper
			Sleep(500);
			log.debug("{} {} reset printer", brws, wsno);
			ResetPrinter();
			Sleep(500);
			log.debug("{} {} close connection", brws, wsno);
			pc.close();   //close
			//----20230309 end

			return false;
		case (byte) 'X': // Warning , paper lower
			//20240327 MatsudairaSyuMe TEST
			PurgeBuffer();
		    /*20240408 MatsudairaSyuMe paper lower change to reprinter directly
			Send_hData(S4625_PERRCODE_REQ);
			*/
			log.debug("{} {} paper lower send perrcode_req curState={}  curChkState={}", brws, wsno, this.curState,  this.curChkState);
			amlog.info("[{}][{}][{}]:94-1傳票稍短,超出可列印範圍", brws, "        ", "            ");
			////Sleep(50);
			////data = Rcv_Data(5);
			// 20091002 , show error code

			////ResetPrinter();
			//----20240327
			//20240408 MatsudairaSyuMe paper lower change to reprinter directly
			//return false;
			ResetPrinter();
			return true;
		case (byte) 'q': // read/write error of MS
		case (byte) 'r': // read error of MS
			//20200729
			if (this.curState == Eject) {
				log.error("get read msr error status reset printer");
				ResetPrinter();
				this.curChkState = CheckStatus_START;
				if (Send_hData(S4625_PEJT) != 0)
					return false;
				return false;
			}
		//----
		/*20201208 mark
			this.curState = CheckStatus_START;
			*/
			CheckStatus();//20240510 Poor Style: Value Never Read take of data
			Send_hData(S4625_PERRCODE_REQ);
			
			//20201208 add
			this.curChkState = CheckStatusRecvData;
			this.iCnt = 0;
			//----

			Sleep(50);
			data = Rcv_Data(5);
			// 20091002 , show error code
			amlog.info("[{}][{}][{}]:95硬體錯誤代碼4[{}]", brws, "        ", "            ", new String(data, 1, data.length - 1, Charset.forName("UTF-8")));		
			/*20201208 mark
			ResetPrinter();
			this.curState = ResetPrinterInit_START;
			ResetPrinterInit();
			*/
			return false;
		case (byte) 0x21:
		case (byte) 0x22:
			Send_hData(S4625_PERRCODE_REQ);
			Sleep(50);
			data = Rcv_Data(5);
			// 20091002 , show error code
			amlog.info("[{}][{}][{}]:95硬體錯誤代碼5{}]", brws, "        ", "            ", String.format(outptrn1, data));		

			/*
			 * 20230309  mark up
			ResetPrinter();
			this.curState = ResetPrinterInit_START;
			ResetPrinterInit();
			*/
			//2030309 add for reset conection
			this.curChkState = CheckStatus_START;
			log.debug("{} {} eject paper", brws, wsno);
			Send_hData(S4625_PEJT);  //eject paper
			Sleep(500);
			log.debug("{} {} reset printer", brws, wsno);
			ResetPrinter();
			Sleep(500);
			log.debug("{} {} close connection", brws, wsno);
			pc.close();   //close
			//----20230309 end
			return false;
		case 0x00:
			atlog.info("Error [0x00]");
			return false;
		case (byte) '6': // read error response
			//20200728
			if (this.curState == Eject) {
				this.curChkState = CheckStatus_START;
				return false;
			} else {
				//20201208 add, 20240420 add if this.curState == MS_Read_Check change to nr[0] == 'W'
				byte[] nr = new byte[1];
				if (this.curState == MS_Read_Check)
					nr[0] = (byte)'W';
				else
					nr[0] = (byte)'X';
				this.curmsdata = nr;
				amlog.info("[{}][{}][{}]:95硬體錯誤代碼5[{}]", brws, "        ", "            ", new String(data, 1, data.length - 1, Charset.forName("UTF-8")));
				//20240420 add private toAsciiStr
				//				String s = "95硬體錯誤代碼" + new String(data, 1, data.length - 1);
				String s = "";
				if (this.curState == MS_Read_Check)
					s = "94補摺機狀態錯誤！磁條寫入時回傳硬體錯誤碼代碼:" + toAsciiStr(data);
				else
					s = "94硬體錯誤代碼:" + toAsciiStr(data);
				pc.InsertAMStatus(brws, "", "", s);
				//----20240420
				//20220923 
				Send_hData(S4625_CANCEL);
				//----
				//20201216
				ResetPrinter();
				//----
				this.curState = ResetPrinterInit_START;
				ResetPrinterInit();
				if (this.curState == ResetPrinterInit &&  this.curChkState == CheckStatusRecvData) {
					data = CheckStatus();
					if (data != null && data.length > 0)
						this.curmsdata = data;
					this.curState = ResetPrinterInit_FINISH;
					amlog.info("[{}][{}][{}]:00補摺機重置完成！", brws, "        ", "            ");	
				} else {
					//20240220 MatsudairaSyuMe
					log.debug("return false curState=[{}]", this.curState);
					//----20240220
					return false;
				}
				//----
				return true;
			//----
			}
		//----
		//20230306 MatsudairasyuMe for Normalcy system response ESC r 0 0 0
		case (byte) '0':
			//20230322 MatsudairaSyuMe ignore the other data after data.length > 3
			log.warn("receive Normalcy system response r000 ignoreit");
			return true;
			/*20230322 MatsudairaSyuMe ignore the other data after data.length > 3
			this.curChkState = CheckStatus_START;
			log.error("receive un-normal code reset priner");
			pc.close();
			return false;
			*/
		//----
		default:
			atlog.info("Error Reset[{}]", String.format(outptrn3, data[2]));
			ResetPrinter();
			return false;
		}
	}

	@Override
	public boolean CheckErrorReset(byte[] data) {
		// TODO Auto-generated method stub
		if (data == null || data.length == 0)
			return false;
		// S4265
		// ESC 'r' '2' --> DOCUMENT EMPTY nopaper
		// 'P' --> paper in
		// '8' --> command error
		// '1' --> paper jam
		// '4' --> paper in chasis
		// '6' --> ???
		// 'q' --> msr error
		// 'r' --> blank msr
		// 'X' --> Warning , print area out of paper
		// 'a' --> Hard error conditions
		// 'b' --> Receive Error
		// 'A' --> Warning waiting passbook insert
		// 0x90 --> hw error
		// 0x80 --> S4265 can not operate
		// 0xd0 --> S4265 , status not in initial config
		switch (data[2]) {
		case (byte) '2':
		case (byte) '4':
		case (byte) 'P':
		case (byte) 'A':
		case (byte) 0xd0:
			return true;
		case (byte) '1': // 20060619 paper jam
			Send_hData(S4625_PERRCODE_REQ);
			Sleep(50);
			data = Rcv_Data(5);
			amlog.info("[{}][{}][{}]:95硬體錯誤代碼Reset-1[{}]", brws, "        ", "            ", String.format(outptrn1, data));		
			return false;
		case (byte) 'a': // 20060801 hardware error ,ESCra , this may need resetInit()
		case (byte) 'b':
			return false;
		case (byte) '8':
			// command error,
			Send_hData(S4625_PERRCODE_REQ);
			Sleep(50);
			data = Rcv_Data(5);
			amlog.info("[{}][{}][{}]:95硬體錯誤代碼Reset-2[{}]", brws, "        ", "            ", String.format(outptrn1, data));		
			return false;
		case (byte) 'X': // Warning , paper lower
			Send_hData(S4625_PERRCODE_REQ);
			Sleep(50);
			data = Rcv_Data(5);
			amlog.info("[{}][{}][{}]:95硬體錯誤代碼Reset-3[{}]", brws, "        ", "            ", String.format(outptrn1, data));		
			return true;
		case (byte) 'q': // read/write error of MS
		case (byte) 'r': // read error of MS
			Send_hData(S4625_PERRCODE_REQ);
			Sleep(50);
			data = Rcv_Data(5);
			amlog.info("[{}][{}][{}]:95硬體錯誤代碼Reset-4[{}]", brws, "        ", "            ", String.format(outptrn1, data));		
			return false;
		case (byte) 0x21:
		case (byte) 0x22:
			Send_hData(S4625_PERRCODE_REQ);
			Sleep(50);
			data = Rcv_Data(5);
			amlog.info("[{}][{}][{}]:95硬體錯誤代碼Reset-5[{}]", brws, "        ", "            ", String.format(outptrn1, data));		
			return false;
		case (byte) 0x00:
			atlog.info("Error [0x00]");
			return false;
		default:
			atlog.info("Error Reset[{}]", String.format(outptrn3, data[2]));
			return false;
		}
	}

	@Override
	public boolean Prt_Transverse(byte high, byte low) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean SetTran(byte[] buff) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int my_pow(int x, int y) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void SendHalf(byte c) {
		// TODO Auto-generated method stub

	}

	@Override
	public int WebBranchSystem(byte[] pszCmd) {
		// TODO Auto-generated method stub
		return 0;
	}

	/*********************************************************
	*  ReadBarcode() : Get the passbook's barcode data       *
	*  function      : 讀取條碼(頁次)內容                    *
	*  parameter 1   : 1- HITACH_BARCODE                     *
	*                  2- STD25_BARCODE(台銀)                *
	*  return_code   : <= 0 - FALSE                          *
	*                  >  0 - PAGE NUMBER          2008.01.22*
	*********************************************************/
	@Override
	public short ReadBarcode(boolean first, short type) {
		// TODO Auto-generated method stub
		byte[] data = null;
		if (first) {
			this.curState = ReadBarcode;
			this.curChkState = CheckStatus_START;
			log.debug("ReadBarcode 1 ===<><>{} curChkState {}", this.curState, this.curChkState);
		}
		if (this.curState == ReadBarcode || this.curState == ReadBarcode_START) {
			log.debug("ReadBarcode 2 ===<><>{} curChkState {}", this.curState, this.curChkState);
			if (this.curState == ReadBarcode)
				this.curState = ReadBarcode_START;
			data = CheckStatus();
			if (data == null) {
//				log.debug("{} {} {} 94補摺機無回應", brws, "", "");
				return 0;
			}
			if (data[2] != (byte)'4' && data[2] != (byte)'P' && data[2] != (byte)'2') {
				if (new String(data, Charset.forName("UTF-8")).equals("DIS")) {
					amlog.info("[{}][{}][{}]:94補摺機斷線", brws, "        ", "            ");		
					this.curChkState = CheckStatus_FINISH;
					return 0;
				}
				if (!CheckError(data)) {
					amlog.info("[{}][{}][{}]:95補摺機硬體錯誤！(SIG)", brws, "        ", "            ");		
					this.curChkState = CheckStatus_FINISH;
					ResetPrinter();
					return 0;
				}
			} else {
				this.curChkState = CheckStatus_FINISH;
				this.curState = ReadBarcode_START_2;
				Send_hData(S4625_BAR_CODE);
			}
		}
		if (this.curState == ReadBarcode_START_2) {
			this.curState = ReadBarcodeRecvData;
//20200819 speed up			Sleep(1500);
			Sleep(500);
			this.iCnt = 0;
			this.curbarcodedata = null;
			data = Rcv_Data();
		} else if (this.curState == ReadBarcodeRecvData) {
			Sleep(200);
			this.iCnt++;
//			if (data != null && !new String(data).equals("DIS")) {
			while (null != (data = Rcv_Data()) && !new String(data, Charset.forName("UTF-8")).equals("DIS")) {
//				log.debug("ReadBarcode 2.2 ===<><> data={} s={}", data, (byte)'s');

				if ( data[1] == 'r' && data[2] == (byte)'P') {
					log.debug("ReadBarcode 3 ===<><>{} chkChkState {}", this.curState, this.curChkState);
				} else if (data[1] == (byte)'s') {
					if (data[2] == (byte)0x7f) { // barcode error , blank paper etc
						amlog.info("[{}][{}][{}]:95補摺機硬體錯誤！(讀空白頁)", brws, "        ", "            ");		
						this.curState = ResetPrinterInit_START;
						return -1;
//						ResetPrinterInit();
//						pc.close();
					} else {
						this.curbarcodedata = new byte[3];
						System.arraycopy(data, 3, this.curbarcodedata, 0, 3);
						this.curState = ReadBarcode_FINISH;
						log.debug("ReadBarcode 4 ===<><>{} chkChkState {} {}", this.curState, this.curChkState, (short)(this.curbarcodedata[0] - 0x30));
						//20200718 add for no barcode
						if ((short)(this.curbarcodedata[0] - 0x30) == 0)
							return -2;
						//----
						else
							return (short)(this.curbarcodedata[0] - 0x30);
					}
				} else if (iCnt > 40) {
					amlog.info("[{}][{}][{}]:95補摺機硬體錯誤！(MSR-2)", brws, "        ", "            ");		
					this.curState = ResetPrinterInit_START;
					ResetPrinterInit();
					pc.close();
				}
				log.debug("ReadBarcode 4 ===<><>{} chkChkState {}", this.curState, this.curChkState);
				this.iCnt++;
			}
		}

		log.debug("ReadBarcode {} {} {} {} final data.length={}", iCnt, brws, "", "", data != null ? data.length : 0);
		return 0;
	}

	/****************************************************************************
	*	SetSignal() : Set Priner Signal 設定顯示燈號                *
	*                 ex:(L1|L3,L4|L5|L6)                    *
	*   parameter 1 : light : 某一燈號亮燈(L0 ~ L6)                 *
	*   parameter 2 : blink : 某一燈號閃爍(L0 ~ L6)                 *
	*                                                                           *
	* # SetSignal[5]=Light   SetSignal[6]=Blink              *
	* # L0:0x10(16/0010000)  請翻到最近一頁/                           *
	* #                      請插入存摺後，稍待                                *
	* # L1:0x01( 1/0000001)  請取出存摺						        *
	* # L2:0x40(64/1000000)  請重新插入存摺					        *
	* # L3:0x08( 8/0001000)  請洽服務台換摺					        *
	* # L4:0x20(32/0100000)  異常狀態，請洽服務台			            *
	* # L5:0x04( 4/0000100)  無補登資料						        *
	* # L6:0x02( 2/0000010)  暫停服務              2008.01.18          *
	*****************************************************************************/
	@Override
	public boolean SetSignal(boolean firstSet, boolean sendReqFirst, byte light1, byte light2, byte blink1, byte blink2) {
		// TODO Auto-generated method stub
		if (firstSet) {
			if (sendReqFirst) {
				this.curState = SetSignal;
				this.curChkState = CheckStatus_START;
			} else
				this.curState = SetSignal_START_2;
		}
//		this.curState = SETSIGNAL;
		byte[] data = null;
		if (this.curState == SetSignal) {
			//20200331 test for speed
			/*
			data = CheckStatus();
			log.debug("SetSignal 1 ===<><>{} {}", this.curState, data);
			if (this.curChkState == CheckStatus_FINISH) {
				if (data == null) {
					log.debug("{} {} {} 94補摺機無回應", brws, "", "");
					return false;
					} else if (new String(data).equals("DIS")) {
						log.debug("{} {} {} 94補摺機斷線", brws, "", "");
						return false;
					}
				if (!CheckError(data)) {
					log.debug("{} {} {} 95補摺機硬體錯誤！(SIG)", brws, "", "");
					return false;
				} else
					this.curState = SetSignal_START_2;
			}
			*/
			//20200331 test for speed
			this.curChkState = CheckStatus_FINISH;
			this.curState = SetSignal_START_2;
		}
		if (this.curState == SetSignal_START_2 || this.curState == SetSignal_2) {
			log.debug("SetSignal 2 ===<><>{} curChkState {}", this.curState, this.curChkState);
			if (this.curState == SetSignal_START_2) {
				// Lamp OFF
				S4625_OFF_SIGNAL[2] = (byte)0;
				S4625_OFF_SIGNAL[3] = (byte)0xff;
				if ( Send_hData(S4625_OFF_SIGNAL) < 0 ) {
					log.debug("[{}]:S4625 : SetSignal() -- OFF Signal Failed!!", String.format(outptrn2, wsno));
					atlog.info("OFF Signal Failed!!");
					return false;
				}
				this.curState = SetSignal_2;
				this.curChkState = CheckStatus_START;
			}
			//20200331 test for speed
			/*
			data = CheckStatus();
			if (CheckDis(data) != 0) 
				return false;
			if (!CheckError(data)) {
				log.debug("{} {} {} 95補摺機硬體錯誤！(SIG)", brws, "", "");
				return false;
			} else 
				this.curState = SetSignal_START_3;
			*/
			//20200331 test for speed
			this.curState = SetSignal_START_3;
		}
		if (this.curState == SetSignal_START_3 || this.curState == SetSignal_3) {
			log.debug("SetSignal 3 ===<><>{} curChkState {}", this.curState, this.curChkState);
			if (this.curState == SetSignal_START_3) {
//20200331 test for speed				Sleep(100);
				S4625_SET_SIGNAL[2] = light1;
				S4625_SET_SIGNAL[3] = light1;
				if ( Send_hData(S4625_SET_SIGNAL) < 0 ) {
					log.debug("[{}]:S4625 : SetSignal() -- OFF Signal Failed!!", String.format(outptrn2, wsno));
					atlog.info("OFF Signal Failed!!");
					return false;
				}
//20200331 test for speed				Sleep(100);
				this.curState = SetSignal_3;
				this.curChkState = CheckStatus_START;
			}
			//20200331 test for speed
			/*
			data = CheckStatus();
			if (CheckDis(data) != 0) 
				return false;
			if (!CheckError(data)) {
				log.debug("{} {} {} 95補摺機硬體錯誤！(SIG)", brws, "", "");
				return false;
			} else 
				this.curState = SetSignal_START_4;
			*/
			this.curState = SetSignal_START_4;
		}
		if (this.curState == SetSignal_START_4 || this.curState == SetSignal_4) {
			log.debug("SetSignal 4 ===<><>{} curChkState {}", this.curState, this.curChkState);
			if (this.curState == SetSignal_START_4) {
				if ((int)blink1 != 0 || (int)blink2 != 0) {
					S4625_SET_BLINK[2] = blink1;
					S4625_SET_BLINK[3] = blink2;
					//20200403  test
//					clearBuffer();
					PurgeBuffer();
					//----
					if ( Send_hData(S4625_SET_BLINK) < 0 ) {
						log.debug("[{}]:S4625 : SetSignal() -- OFF Signal Failed!!", String.format(outptrn2, wsno));
						atlog.info("OFF Signal Failed!!");
						return false;
					}
//20200403  test	Sleep(1000);
				}
				this.curState = SetSignal_4;
				this.curChkState = CheckStatus_START;
			}
			data = CheckStatus();
			if (CheckDis(data) != 0) 
				return false;
			if (!CheckError(data)) {
				amlog.info("[{}][{}][{}]:95補摺機硬體錯誤！(SIG)", brws, "        ", "            ");		
				return false;
			} else 
				this.curState = SetSignal_FINISH;
		}
		log.debug("SetSignal 5 ===<><>{} curChkState {}", this.curState, this.curChkState);

		return true;
	}

	@Override
	public boolean AutoTurnPage(String brws, String account, short type) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean SetAutoInfo(Map<String, String> tid) {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public int CheckDis(byte[] data) {
		// TODO Auto-generated method stub
		if (data == null || data.length == 0)
			return -1;
		if (new String(data, Charset.forName("UTF-8")).contains("DIS")) {
			amlog.info("[{}][{}][{}]:94補摺機斷線！", brws, "        ", "            ");		
			return -2;
		}
		return 0;
	}

	private void Sleep(int s) {
		try {
			TimeUnit.MILLISECONDS.sleep(s);
		} catch (InterruptedException e1) { // TODO Auto-generated catch block
			//20240503 MatsudairaSyuMe mark for System Information Leak e1.printStackTrace();
		}
	}

	@Override
	public AtomicBoolean getIsShouldShutDown() {
		return isShouldShutDown;
	}

	//20211124 MatsudairaSyuMe
	public static final int MS_Read_Check_Start             = 49;
	public static final int MS_Read_Check                   = 50;
	public byte[] MS_CheckAndRead(boolean start, String brws) {
		byte[] data = null;
		if (start)
			this.curState = MS_Read_Check_Start;
		log.debug("MS_CheckAndRead curState={} curChkState={}", this.curState, this.curChkState);

		if (this.curState == MS_Read_Check_Start || this.curState == MS_Read_Check) {
			if (this.curState == MS_Read_Check_Start) {
				this.curState = MS_Read_Check;
				this.curChkState = CheckStatus_START;PurgeBuffer(); //20211210 MatsudairasyuMe purge buffer before start to check status
			}
			data = CheckStatus();
			log.debug("MS_CheckAndRead 1 ===<><>{} chkChkState {}", this.curState, this.curChkState);
			if (!CheckError(data)) {
				return null;
			} else
			//20240420 MatsudairaSyuMe check for MSW error
			{
				if(this.curmsdata.length == 1)
					return this.curmsdata;
				else
			//---->20240420
					this.curState = MS_Read_START;	
			}
			//----20240420
		}
		log.debug("MS_CheckAndRead 2 curState={} curChkState={}", this.curState, this.curChkState);
		if (this.curState == MS_Read_START) {
			this.curState = MS_Read;
			log.debug("MS_CheckAndRead 3 curState={} curChkState={}", this.curState, this.curChkState);
			amlog.info("[{}][{}][{}]:01讀取存摺磁條中...", brws, "        ", "            ");
			if (Send_hData(S4625_PMS_READ) != 0)
				return (data);
		}
		if (this.curState == MS_Read) {
			this.curState = MS_ReadRecvData;
			Sleep(500);
			this.iCnt = 0;
			this.curmsdata = null;
			data = Rcv_Data();
		} else if (this.curState == MS_ReadRecvData) {
			Sleep(200);
			this.iCnt++;
			data = Rcv_Data();
			if (data == null && iCnt > 40) {
				amlog.info("[{}][{}][{}]:94補摺機狀態錯誤！(MSR-2)", brws, "        ", "            ");
				pc.InsertAMStatus(brws, "", "", "94補摺機狀態錯誤！(MSR)");
				this.curState = ResetPrinterInit_START;
				ResetPrinterInit();
				pc.close();
			} else if (data != null && !new String(data, Charset.forName("UTF-8")).equals("DIS")) {
				if (data[1] == (byte)'s') {
					if (data[2] == (byte)(0x7f & 0xff)) {
						iCnt = 0;
						amlog.info("[{}][{}][{}]:94補摺機狀態錯誤！(MSR)", brws, "        ", "            ");
						pc.InsertAMStatus(brws, "", "", "94補摺機狀態錯誤！(MSR-1格式錯誤)");
						this.curState = MS_Read_2;
						this.curChkState = CheckStatusRecvData;
						this.iCnt = 0;
						return this.curmsdata;
					} else if (data.length >= 38) {
						int lastidx = data.length - 1;
						for (; lastidx > 1; lastidx--)
							if (data[lastidx] == (byte) 0x1c)
								break;
						byte[] tmpb = new byte[lastidx - 2];
						System.arraycopy(data, 2, tmpb, 0, tmpb.length);
						this.curmsdata = tmpb;
						//20240517 Code Correctness: Call to System.gc() System.gc();
					} else {
						iCnt = 0;
						amlog.info("[{}][{}][{}]:94補摺機狀態錯誤！(MSR-1格式錯誤)", brws, "        ", "            ");
						pc.InsertAMStatus(brws, "", "", "94補摺機狀態錯誤！(MSR-1)");
						byte[] nr = new byte[1];
						nr[0] = (byte)'X';
						this.curmsdata = nr;
						return this.curmsdata;
					}
				//20211124 get ESP rP while read MSR
				} else if (data.length > 2 && ((data[0] == ESQ) && (data[1] == (byte)'r') && (data[2] == (byte)'P'))) {
//					Send_hData(S4625_PSTAT);
					this.curmsdata = null;
					return null;
				//----
				} else {
					iCnt = 0;
					amlog.info("[{}][{}][{}]:94補摺機狀態錯誤！(MSR-1)", brws, "        ", "            ");
					//20240420 add private toAsciiStr
					pc.InsertAMStatus(brws, "", "", "94補摺機狀態錯誤！磁條寫入後讀取回傳硬體錯誤碼:" + toAsciiStr(data));
					//----20240420
					this.curState = ResetPrinterInit_START;
					ResetPrinterInit();
					pc.close();
					this.curmsdata = null;
					return null;
				}
				this.curState = MS_Read_START_2;
				log.debug("MS_CheckAndRead 4 ===<><>{} chkChkState {} curmsdata={}", this.curState, this.curChkState, this.curmsdata);
			}
		}
		if (this.curState == MS_Read_START_2 || this.curState == MS_Read_2) {
			if (this.curState == MS_Read_START_2) {
				this.curState = MS_Read_2;
				this.curChkState = CheckStatus_START;
			}
			data = CheckStatus();
			log.debug("MS_CheckAndRead 5 ===<><>{} chkChkState {}", this.curState, this.curChkState);
			if (!CheckError(data)) {
				return null;
			} else {
				this.curState = MS_Read_FINISH;
				log.debug("MS_CheckAndRead 6 ===<><>{} chkChkState {}", this.curState, this.curChkState);
				//atlog.info("ms_read data=[{}]",new String(this.curmsdata));
				return this.curmsdata;
			}
		}
		log.debug("{} {} {} {} MS_CheckAndRead final data.length={}", iCnt, brws, "", "", (data == null? 0: data.length));
		return curmsdata;
	}

	//20240420 MatsudairaSyuMe convert byte array to ascii string
	private String toAsciiStr(byte[] b) {
		String rtn = "";
		if (b == null || b.length == 0)
			return rtn;
		for (int i = 0; i < b.length; i++)
			if ((int)(b[i] & 0xff) < 32 || (int)(b[i] & 0xff) > 126)
                b[i] = (byte)'.';
		rtn = new String(b, Charset.forName("US-ASCII"));
		return rtn;
	}
	//----20240420
}
