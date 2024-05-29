package com.systex.sysgateii.autosvr.prtCmd.Impl;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.systex.sysgateii.autosvr.autoPrtSvr.Client.PrtCli;
import com.systex.sysgateii.autosvr.autoPrtSvr.Server.PrnSvr;
import com.systex.sysgateii.autosvr.prtCmd.Printer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class CS2812Impl implements Printer {
	private static Logger log = LoggerFactory.getLogger(CS2812Impl.class);
//	public static Logger amlog = null;
//	public static Logger atlog = null;
	private Logger amlog = null;
	private Logger atlog = null;
	private byte  ESQ		= (byte)0x1b;
	private byte  ENQ		= (byte)0x05;
	private byte  ACK		= (byte)0x06;
	private byte  STX		= (byte)0x02;
	private byte  ETX		= (byte)0x03;
	private byte  NAK		= (byte)0x15;
	private byte  CR		= (byte)0x0d;
	private byte  LF		= (byte)0x0a;
	private byte  MI_DATA	= (byte)0x30;
	private byte  MI_STAT	= (byte)0x31;
	private byte  MI_INIT	= (byte)0x32;
	private byte 	CC		= (byte)0x1b;
	private byte  CC_MS_READ	= (byte)0x7b;
	private byte  EF_MS_WRITE	= (byte)0x57;
	private byte  PR_MS_WRITE = (byte)0X87;
	private byte  MS_WRITE_START	= (byte)0x2B;
	private byte  MS_WRITE_END	= (byte)0x2F;
	private byte 	CC_OPEN_INSERTER = (byte)0x75;
	private byte  CC_CLOSE_INSERTER = (byte)0x73;
	private byte  CPI10	= (byte)0x50;
	private byte  CPI12	= (byte)0x4D;
	private byte  LPI		= (byte)0x2F;
	private byte  CPI18	= (byte)0x69;
	private byte  STD		= (byte)0x76;
	private byte  HEL		= (byte)0x77;
	private byte  HVEL	= (byte)0x7E;
	private byte  VEL		= (byte)0x5A; //= (byte)0x5A //= (byte)0x8A
	private byte  MLF		= (byte)0x2D;
	private String MULTI_LINE_FEED = "1B2D";
	private String FINE_PITCH_LINE_FEED = "1B49";
	private String TRAIN_MSG  = "　　　　　　　　　　訓 練 用 本 單 作 廢\n";
	private byte  MSR_WRITE_ERR  = (byte)0x40;
	private byte  MSR_READ_ERR   = (byte)0x41;
	// Signal Number
	private byte  L1  = (byte)0x01; //  1:0000001 
	private byte  L6	= (byte)0x02; //  2:0000010 
	private byte  L5	= (byte)0x04; //  4:0000100
	private byte  L3	= (byte)0x08; //  8:0001000
	private byte  L0	= (byte)0x10; // 16:0010000
	private byte  L4	= (byte)0x20; // 32:0100000
	private byte  L2	= (byte)0x40; // 64:1000000
	//Hitach Barcode define 2008.01.22
	private byte  HITACH_PAGE1   = (byte)0x6d;
	private byte  HITACH_PAGE2   = (byte)0x5d;
	private byte  HITACH_PAGE3   = (byte)0x3d;
	private byte  HITACH_PAGE4   = (byte)0x3b;
	private byte  HITACH_PAGE5   = (byte)0x67;
	private byte  HITACH_PAGE6   = (byte)0x57;
	private byte  HITACH_PAGE7   = (byte)0x37;
	private byte  HITACH_PAGE8   = (byte)0x4f;
	private byte  HITACH_PAGE9   = (byte)0x2f;
	//Std Barcode define 2008.01.22
	private byte  STD_PAGE1    = (byte)0x18;
	private byte  STD_PAGE2    = (byte)0x14;
	private byte  STD_PAGE3    = (byte)0x0c;
	private byte  STD_PAGE4    = (byte)0x12;
	private byte  STD_PAGE5    = (byte)0x0a;
	private byte  STD_PAGE6    = (byte)0x06;
	private byte  STD_PAGE7    = (byte)0x11;
	private byte  STD_PAGE8    = (byte)0x09;
	private byte  STD_PAGE9    = (byte)0x05;
	private byte  STD_PAGE10   = (byte)0x03;

	private byte[] PEJT={STX,(byte)3,MI_DATA,CC,CC_OPEN_INSERTER,ETX,(byte)((byte)3+MI_DATA+CC+CC_OPEN_INSERTER+ETX)};
	private byte[] PENQ={ENQ};
	private byte[] PACK={ACK};
	private byte[] PENLARGE_OD={STX,(byte)3,MI_DATA,(byte)0x0D,(byte)0x0A,ETX,(byte)((byte)2+MI_DATA+(byte)0x0D+(byte)0x0A+ETX)};

	private byte[] PINIT={STX,(byte)1,MI_INIT,ETX,(byte)((byte)1+MI_INIT+ETX)};
	private byte[] PMS_READ={STX,(byte)3,MI_DATA,CC,CC_MS_READ,ETX,(byte)((byte)3+MI_DATA+CC+CC_MS_READ+ETX)};
	private byte[] PSTAT={STX,(byte)1,MI_STAT,ETX,(byte)((byte)1+MI_STAT+ETX)};
	private byte[] PSTD={STX,(byte)3,MI_DATA,CC,STD,ETX,(byte)((byte)3+MI_DATA+CC+STD+ETX)};
	private byte[] PHEL={STX,(byte)3,MI_DATA,CC,HEL,ETX,(byte)((byte)3+MI_DATA+CC+HEL+ETX)};
	private byte[] PHVEL={STX,(byte)3,MI_DATA,CC,HVEL,ETX,(byte)((byte)3+MI_DATA+CC+HVEL+ETX)};
	private byte[] PVEL={STX,(byte)3,MI_DATA,CC,VEL,ETX,(byte)((byte)3+MI_DATA+CC+VEL+ETX)};
	private byte[] PINS={STX,(byte)5,MI_DATA,CC,(byte)0x20,(byte)0x10,(byte)0x00,ETX,(byte)((byte)5+MI_DATA+CC+(byte)0x20+(byte)0x10+(byte)0x00+ETX)};
	private byte[] SET_SIGNAL={STX,(byte)5,MI_DATA,CC,(byte)0x20,(byte)0x00,(byte)0x00,ETX,(byte)0x00};
	private byte[] HITACH_BAR={STX,(byte)5,MI_DATA,CC,(byte)0x5f,(byte)0x04,(byte)0x00,ETX,(byte)((byte)5+MI_DATA+CC+(byte)0x5f+(byte)0x04+(byte)0x00+ETX)};
	private byte[] SET_HITACH_BAR={STX,(byte)0x11,MI_DATA,CC,(byte)0x5e,(byte)0x01,(byte)0x00,(byte)0x4c,(byte)0x00,(byte)0x23,(byte)0x00,(byte)0x47,(byte)0x09,(byte)0x02,(byte)0x07,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,ETX,
			(byte)((byte)0x11+MI_DATA+CC+(byte)0x5e+(byte)0x01+(byte)0x00+(byte)0x4c+(byte)0x00+(byte)0x23+(byte)0x00+(byte)0x47+(byte)0x09+(byte)0x02+(byte)0x07+(byte)0x00+(byte)0x00+(byte)0x00+(byte)0x00+ETX)};
	private byte[] STD_BAR={STX,(byte)5,MI_DATA,CC,(byte)0x5f,(byte)0x04,(byte)0x00,ETX,(byte)((byte)5+MI_DATA+CC+(byte)0x5f+(byte)0x04+(byte)0x00+ETX)};
	private byte[] SET_STD_BAR={STX,(byte)0x11,MI_DATA,CC,(byte)0x5e,(byte)0x04,(byte)0x00,(byte)0x38,(byte)0x00,(byte)0x39,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,ETX,
			(byte)((byte)0x11+MI_DATA+CC+(byte)0x5e+(byte)0x04+(byte)0x00+(byte)0x38+(byte)0x00+(byte)0x39+(byte)0x00+(byte)0x00+(byte)0x00+(byte)0x00+(byte)0x00+(byte)0x00+(byte)0x00+(byte)0x00+(byte)0x00+ETX)};
	private byte[] NOPERLINE = {STX,(byte)4,MI_DATA,CC,(byte)0x70,(byte)0x48,ETX,(byte)((byte)4+MI_DATA+CC+(byte)0x70+(byte)0x48+ETX)};
	private byte[] TURN_BACK = {STX,(byte)4,MI_DATA,CC,(byte)0x55,(byte)0x00,ETX,(byte)((byte)4+MI_DATA+CC+(byte)0x55+(byte)0x00+ETX)};  //0xa6 翻前一頁OK
	private byte[] TURN_NEXT = {STX,(byte)4,MI_DATA,CC,(byte)0x56,(byte)0x00,ETX,(byte)((byte)4+MI_DATA+CC+(byte)0x56+(byte)0x00+ETX)}; //0xa7 翻後一頁

	private byte[] inBuff = new byte[128];
	//20240520 Dead Code: Unused Field private byte[] curmsdata;
	//20240520 Dead Code: Unused Field private byte[] curbarcodedata;

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

//  State Value
	public static final int ResetPrinterInit_START    = 0;
	public static final int ResetPrinterInit          = 1;
	public static final int ResetPrinterInit_2        = 2;
	public static final int ResetPrinterInit_3        = 3;
	public static final int ResetPrinterInit_4        = 4;
	public static final int ResetPrinterInit_FINISH   = 5;
	public static final int OpenPrinter_START         = 6;
	public static final int OpenPrinter               = 7;
	public static final int OpenPrinter_START_2       = 8;
	public static final int OpenPrinter_2             = 9;
	public static final int OpenPrinter_FINISH        = 10;

	public static final int SetSignal                 = 11;
	public static final int SetSignal_START_2         = 12;
	public static final int SetSignal_2               = 13;
	public static final int SetSignal_START_3         = 14;
	public static final int SetSignal_3               = 15;
	public static final int SetSignal_START_4         = 16;
	public static final int SetSignal_4               = 17;
	public static final int SetSignal_FINISH          = 18;
	public static final int DetectPaper               = 19;
	public static final int DetectPaper_START         = 20;
	public static final int DetectPaper_FINISH        = 21;
	public static final int SetCPI                    = 22;
	public static final int SetCPI_START              = 23;
	public static final int SetCPI_FINISH             = 24;
	public static final int SetLPI                    = 25;
	public static final int SetLPI_START              = 26;
	public static final int SetLPI_FINISH             = 27;
	public static final int MS_Read                   = 28;
	public static final int MS_Read_START             = 29;
	public static final int MS_ReadRecvData           = 30;
	public static final int MS_Read_START_2           = 31;
	public static final int MS_Read_2                 = 32;
	public static final int MS_Read_FINISH            = 33;
	public static final int ReadBarcode               = 34;
	public static final int ReadBarcode_START         = 35;
	public static final int ReadBarcode_START_2       = 36;
	public static final int ReadBarcodeRecvData       = 37;
	public static final int ReadBarcode_FINISH        = 38;
	public static final int Prt_Text                  = 39;
	public static final int Prt_Text_START            = 40;
	public static final int Prt_Text_START_FINISH     = 41;
	public static final int Eject                     = 42;
	public static final int Eject_START               = 43;
	public static final int Eject_FINISH              = 44;
	public static final int MS_Write                  = 45;
	public static final int MS_Write_START            = 46;
	public static final int MS_Write_START_2          = 47;
	public static final int MS_WriteRecvData          = 48;
	public static final int MS_Write_FINISH           = 49;
	public static final int ADDFONT                   = 50;
	public static final int ADDFONT_START             = 51;


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

	public CS2812Impl(final PrtCli pc, final String brws, final String type, final String autoturnpage) {
		this.pc = pc;
		this.brws = brws;
		this.wsno = this.brws.substring(2);
		this.type = type;
		this.autoturnpage = autoturnpage;
		this.notSetCLPI.set(false);
		this.nCPI = 0;
		this.nLPI = 0;
		this.m_bColorRed.set(false);
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
		log.debug("OpenPrinter before {} curState={}", conOpen, curState);
		byte[] data = null;

		if (conOpen)
			this.curState = ResetPrinterInit_START;
		if (this.curState < OpenPrinter_START) {
			//20240529 Dead Code: Expression is Always true if (!ResetPrinter()) {
				atlog.info("ResetPrinter() Error ! ret=[-1]");
				return false;
			/*} else {
				log.debug("1 ===<><>{} chkChkState {} {}", this.curState, this.curChkState, data);
				this.curState = OpenPrinter_START;
				//Send_hData(S2812_PRESET);
			}*/
		}
		if (this.curState == OpenPrinter_START || this.curState == OpenPrinter) {
			if (this.curState == OpenPrinter_START) {
				this.curChkState = CheckStatus_START;
				this.curState = OpenPrinter;
			}
			data = CheckStatus();
			log.debug("2 ===<><>{} chkChkState {} {}", this.curState, this.curChkState, data);
			//20240527 Dead Code: Expression is Always true if (!CheckError(data)) {
				return false;
			/*} else {
				this.curState = OpenPrinter_START_2;
				Send_hData(PINIT);
			}*/
		}
		if (this.curState == OpenPrinter_START_2 || this.curState == OpenPrinter_2) {
			if (this.curState == OpenPrinter_START_2) {
				this.curState = OpenPrinter_2;
				this.curChkState = CheckStatus_START;
			}
			data = CheckStatus();
			log.debug("3 ===<><>{} chkChkState {} {}", this.curState, this.curChkState, data);
			//20240529 Dead Code: Expression is Always true if (!CheckError(data)) {
				return false;
			/*} else {
				this.curState = OpenPrinter_FINISH;
				log.debug("4 ===<><>{} chkChkState {}", this.curState, this.curChkState);
				return true;
			}*/
		}
		return false;

	}

	@Override
	public boolean ClosePrinter() {
		// TODO Auto-generated method stub
		return false;
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
		return 0;
	}

	@Override
	public int Send_hData(byte[] buff) {
		// TODO Auto-generated method stub
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
			if (pc.clientMessageBuf != null && pc.clientMessageBuf.readableBytes() > 2) {
				int size = pc.clientMessageBuf.readableBytes();
				byte[] buf = new byte[size];
				pc.clientMessageBuf.getBytes(pc.clientMessageBuf.readerIndex(), buf);
				if (buf[0] == (byte) 0x1b && buf[1] == (byte) 's') { // data from S4680 , msread,error status etc
					for (int i = 2; i < size; i++)
						if (buf[i] == (byte) 0x1c) {
							rtn = new byte[i + 1];
							log.debug("Rcv_Data rtn.length={}", rtn.length);
							pc.clientMessageBuf.readBytes(rtn, 0, rtn.length);
							return rtn;
						}
				} else if (buf[0] == (byte) 0x1b) {
					rtn = new byte[3];
					log.debug("Rcv_Data get 3 bytes");
					pc.clientMessageBuf.readBytes(rtn, 0, rtn.length);
					return rtn;
				}
			}
//20200330			Sleep(100);
			Sleep(50);
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
		return null;
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
					//atlog.info("[{}]-[{}]", rcv_len, new String(rtn));
					return rtn;
				}
			}
//20200330			Sleep(100);
			Sleep(50);
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
		return false;
	}

	/* 20240311 MatsudairaSyuMe mark up
	@Override
	public boolean Prt_Text(byte[] skipbuff, byte[] buff) {
		// TODO Auto-generated method stub
		return false;
	}
	*/
	
	@Override
	public boolean ChkAddFont(int fontno) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean AddFont(int fontno) {
		// TODO Auto-generated method stub
		return false;
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
		return false;
	}

	@Override
	public void TestCCode() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean Eject(boolean start) {
		// TODO Auto-generated method stub
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
		return null;
	}

	@Override
	public byte[] INQ() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean DetectPaper(boolean startDetect, int iTimeout) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean ResetPrinter() {
		// TODO Auto-generated method stub
		log.debug("{} {} {} ResetPrinterInit curState={}", brws, "", "", this.curState);
		/*if (this.curState == ResetPrinterInit_START) {
			amlog.info("[{}][{}][{}]:00補摺機重置中..", brws, "        ", "            ");
			if (Send_hData(PENQ) != 0)
				return false;
			else {
				this.curState = ResetPrinterInit;
				this.curChkState = CheckStatus_START;
				this.iCnt = 50;
			}
		}
		byte[] data = null;
		if (this.curState == ResetPrinterInit) {
			data = Rcv_Data();
			log.debug("1 ===<><>{} chkChkState {} {}", this.curState, this.curChkState, data);
			if (CheckDis(data) == -2)
				return false;
			if (data == null) {
				if (this.iCnt > 0)
					Send_hData(PENQ);
				else
					this.iCnt -= 1;
				return false;
			} else {
				this.curState = ResetPrinterInit_2;
				this.curChkState = CheckStatus_START;
				this.iCnt = 0;				
			}
		}
		if (this.curState == ResetPrinterInit_2) {
			log.debug("2 ===<><>{} chkChkState {} {}", this.curState, this.curChkState, data);
			if (data != null && data[0] == (byte) STX) //20210413 MatsudairaSyuMe prevent Null Dereference
				Send_hData(PACK);
			if (Send_hData(PINIT) != 0)
				return false;
			data = Rcv_Data();
			if (CheckDis(data) < 0)
				return false;
			else {
				this.curState = ResetPrinterInit_3;
				this.curChkState = CheckStatus_START;
				this.iCnt = 200;
			}
		}
		//20240510 Poor Style: Value Never Read data = null;
		if (this.curState == ResetPrinterInit_3) {
			Send_hData(PENQ);
			data = Rcv_Data();
			log.debug("3 ===<><>{} chkChkState {} {}", this.curState, this.curChkState, data);
			if (data != null && CheckDis(data) == -2)
				return false;
			if (data[0] == (byte) ACK || data[0] == (byte) STX || --this.iCnt < 0) {
				if (this.iCnt >= 0 && data[0] == (byte) STX)
					Send_hData(PACK);
				this.curState = ResetPrinterInit_4;
				this.curChkState = CheckStatus_START;
				this.iCnt = 200;
				Sleep(2000);
			}
		}
		if (this.curState == ResetPrinterInit_4) {
			Send_hData(PENQ);
			data = Rcv_Data();
			log.debug("4 ===<><>{} chkChkState {} {}", this.curState, this.curChkState, data);
			if (data != null && CheckDis(data) == -2)
				return false;
			if (data[0] == (byte) ACK || data[0] == (byte) STX || --this.iCnt < 0) {
				if (this.iCnt >= 0 && data[0] == (byte) STX)
					Send_hData(PACK);
				this.curState = ResetPrinterInit_FINISH;
				this.curChkState = CheckStatus_START;
				this.iCnt = 0;
			}			
		}
		log.debug("5 ===<><>{} chkChkState {}", this.curState, this.curChkState);
		if (curState == ResetPrinterInit_FINISH) {
			amlog.info("[{}][{}][{}]:00補摺機重置完成！", brws, "        ", "            ");
			return true;
		} else*/
			return false;
	}

	@Override
	public boolean ResetPrinterInit() {
		// TODO Auto-generated method stub
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
		return false;
	}

	@Override
	public boolean SetLPI(boolean start, int lpi) {
		// TODO Auto-generated method stub
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

	@Override
	public boolean SkipnLine(int nLine) {
		// TODO Auto-generated method stub
		return false;
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

	@Override
	public boolean MS_Write(boolean start, String brws, String account, byte[] buff) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean Parsing(boolean start, byte[] str) {
		// TODO Auto-generated method stub
		return false;
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
		return false;
	}

	@Override
	public boolean AddExtFont(byte high, byte low) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean CheckPaper(boolean startDetect, int iTimeout) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean CheckError(byte[] data) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean CheckErrorReset(byte[] data) {
		// TODO Auto-generated method stub
		return false;
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

	@Override
	public short ReadBarcode(boolean first, short type) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean SetSignal(boolean firstOpen, boolean sendReqFirst, byte light1, byte light2, byte blink1,
			byte blink2) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean AutoTurnPage(String brws, String account, short type) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean SetAutoInfo(Map<String, String> tid) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int CheckDis(byte[] data) {
		// TODO Auto-generated method stub
		if (data == null || data.length == 0)
			return -1;
		if (new String(data, Charset.forName("US-ASCII")).contains("DIS")) {
			amlog.info("[{}][{}][{}]:94補摺機斷線！", brws, "        ", "            ");
			return -2;
		}
		return 0;
	}

	@Override
	public AtomicBoolean getIsShouldShutDown() {
		// TODO Auto-generated method stub
		return new AtomicBoolean(true); //20210413 MatsudairaSyuMe prevent Null Dereference
	}
	private void Sleep(int s) {
		try {
			TimeUnit.MILLISECONDS.sleep(s);
		} catch (InterruptedException e1) { // TODO Auto-generated catch block
			//20240503 MatsudairaSyuMe mark for curChkState e1.printStackTrace();
		}
	}
	//20211124 MatsudairaSyuMe
	public static final int MS_Read_Check_Start             = 49;
	public static final int MS_Read_Check                   = 50;
	public byte[] MS_CheckAndRead(boolean start, String brws) {
		byte[] data = null;
		return data;
	}
}
