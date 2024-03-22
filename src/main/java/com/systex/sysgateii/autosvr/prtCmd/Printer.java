package com.systex.sysgateii.autosvr.prtCmd;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.systex.sysgateii.autosvr.autoPrtSvr.Client.PrtCli;

public interface Printer {
/* 58 functions */
	/******************************************************************
	*	OpenPrinter : Create Printer Com Port	        *
	*******************************************************************/
	public boolean OpenPrinter(boolean conOpen);

	/******************************************************************
	*	ClosePrinter : Close Printer Com Port	        *
	*				   Must be call.					        *
	*******************************************************************/
	public boolean ClosePrinter();

	/*******************************************************************
	*	CloseMsr : Close Msr Com Port       	        *
	*				   Must be call.					        *
	*******************************************************************/
	public boolean CloseMsr();

	/*******************************************************************
	*	Send_hDataBuf : send data to printer thru buffer
	*   1. buffer size 255
	*   2. Actual xmt to Printer situation
	*      2.1 Buffer full
	*      2.2 0x0a
	*      2.3 Eject
	*      2.4 Check status and INQ ( purge buffer )
	********************************************************************/
	public int Send_hDataBuf(byte[] buff, int length);

	public int PurgeBuffer();
	
	/*******************************************************************
	*	Send_hData : send data to printer		        *
	********************************************************************/
	public int Send_hData(byte[]  buff);

	/********************************************************************
	*	Rcv_Data : Receive data from printer	         *
	*********************************************************************/
	public byte[] Rcv_Data();

	/********************************************************************
	*	CheckStatus : Send State Control Code to printer*
	*********************************************************************/
	public byte[] CheckStatus();

	/*********************************************************************
	*	Rcv_Data : Receive data from printer	          *
	*			   data length follow the rcv_len          *
	**********************************************************************/
	public byte[] Rcv_Data(int rcv_len);

	/********************************************************************
	*	Prt_Text : Print the text data			         *
	*********************************************************************/
	public boolean Prt_Text(byte[]  buff);
	//20240311 MatsudairaSyuMe public boolean Prt_Text(byte[] skipline, byte[]  buff);
	
	/********************************************************************
	*	ChkAddFont : Check the additional chinese char	*
	*********************************************************************/
	public boolean ChkAddFont(int fontno);

	public boolean AddFont(int fontno);

	/**********************************************************************
	*	Prt_hCCode : Print hex control code		           *
	***********************************************************************/
	public byte[] Prt_hCCode(byte[] buff, int length);

	public void EndOfJob();

	public boolean GetPaper();

	public void TestCCode();

	/***********************************************************************
	*	Eject : Eject the paper						           *
	************************************************************************/
	public boolean Eject(boolean start);

	public boolean EjectNoWait();

	/************************************************************************
	*	MS_Read : Read the passbook's magentic data			*
	*************************************************************************/
	public byte[] MS_Read(boolean start, String brws);

	/************************************************************************
	*	INQ : Receive the printer state		 	            *
	*************************************************************************/
	public byte[] INQ();

	/************************************************************************
	*	DetectPaper : Detect if printer has paper			   *
	*************************************************************************/
	public boolean DetectPaper(boolean startDetect, int iTimeout);

	/************************************************************************
	*	ResetPrinter : Reset Error of the printer			   *
	*************************************************************************/
	public boolean ResetPrinter();

	/************************************************************************
	*	ResetPrinterInit : Reset printer and Init printer	*
	*************************************************************************/
	public boolean ResetPrinterInit();

	public int apatoi(byte[] szBuf,int iLen);

	/************************************************************************
	*	LogData : Log data from printer send & receive		*
	*************************************************************************/
	public void LogData(byte[] str,int type,int len);

	/***********************************************************************
	*	SetCPI : Set char per inch					           *
	************************************************************************/
	public boolean SetCPI(boolean start, int cpi);

	/************************************************************************
	*	SetCPI : Set line per inch					            *
	*************************************************************************/
	/*
	LF1     DB      4,ESQ,'xx'             ;1/4 吋
	LF2     DB      4,ESQ,'xx'             ;1/5 吋
	LF3     DB      4,ESQ,'xx'             ;1/6 吋
	LF4     DB      4,ESQ,'xx'             ;1/8 吋
	LF5     DB      4,ESQ,'xx'             ;1/10 吋
	LF6     DB      4,ESQ,'xx'             ;1/12 吋
	*/
	public boolean SetLPI(boolean start, int lpi);

	/*************************************************************************
	*	SetEnlarge : Set vertical large or horizontal large *
	*                or normal char							    *
	**************************************************************************/
	public boolean SetEnlarge(int type);

	//20200915
	public void PrepareSkipBuffer();
	public boolean SkipnLineBuf(int nLine);
	public byte[] GetSkipLineBuf();
	//----
	/*************************************************************************
	*	SkipnLine : Skip n Line						             *
	**************************************************************************/
	public boolean SkipnLine(int nLine);

	/*************************************************************************
	*	MoveFine : Set Fine pitch line feed					    *
	*  MoveFine(-10) backward ten ,MoveFine(10) forward ten*
	**************************************************************************/
	public boolean MoveFine(byte[] value);

	public byte[] PrtCCode(byte[] buff);

	public char C2H(char buf1, char buf2);

	/************************************************************************
	*	MS_Write : Write data to passsbook's magentic		*
	*************************************************************************/
	public boolean MS_Write(boolean start, String brws, String account, byte[] buff);

	/**********************************************************************
	*	Parsing : parse the data or control code			  *
	***********************************************************************/
	public boolean Parsing(boolean start, byte[] str);

	/***********************************************************************
	*	DeParam : de-param 							           *
	************************************************************************/
	public int DeParam(byte[] str);

	public byte[] cstrchr(byte[] str, byte c);

	/************************************************************************
	*	change_big5_ser : change big5 sequence by big5 code*
	*************************************************************************/
	public int change_big5_ser(byte first, byte second);

	/***********************************************************************
	*	Prt_Big5 : print chinese use bmp					     *
	************************************************************************/
	public int Prt_Big5(byte high, byte low);

	/***********************************************************************
	*	search_24 : search chinese bmp font 				  *
	************************************************************************/
	public int search_24(int sernum, byte[] pattern);

	public int LockPrinter();

	public int ReleasePrinter();

	public boolean SetInkColor(int color);

	public boolean SetTrain();

	/***********************************************************************
	*	ChkAddFont : Check the extend chinese char  		  *
	************************************************************************/
	public boolean ChkExtFont(int fontno);

	public boolean AddExtFont(byte high,byte low);

	public boolean CheckPaper(boolean startDetect, int iTimeout);

	public boolean CheckError(byte[] data);

	public boolean CheckErrorReset(byte[] data);

	/**************************************************************************
	*	Prt_Transverse : 中文字橫印					  		          *
	***************************************************************************/
	public boolean Prt_Transverse(byte high,byte low);

	public boolean SetTran(byte[] buff);

	public int my_pow(int x, int y);

	public void SendHalf(byte c);

	public int WebBranchSystem(byte[] pszCmd);

	/****************************************************************************
	*  ReadBarcode() : Get the passbook's barcode data       *
	*  function      : 讀取條碼(頁次)內容                               *
	*  parameter 1   : 1- HITACH_BARCODE                     *
	*                  2- STD25_BARCODE(台銀)                 *
	*  return_code   : <= 0 - FALSE                          *
	*                  >  0 - PAGE NUMBER          2008.01.22*
	*****************************************************************************/
	public short ReadBarcode(boolean first, short type);

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
	public boolean SetSignal(boolean firstOpen, boolean sendReqFirst, byte light1,byte light2,byte blink1,byte blink2);

	/*****************************************************************************
	*  AutoTurnPage() : auto turn page                        *
	*  parameter 1    : tx_area data                          *
	*  parameter 2    : type (0:翻前一頁 / 1:翻後一頁)            *
	*  return_code    : BOOL - TRUE                           *
	*                          FALSE               2008.02.12 *
	******************************************************************************/
	public boolean AutoTurnPage(String brws, String account, short type);

	public boolean SetAutoInfo(Map<String, String> tid);

	public int CheckDis(byte[] data);

	public AtomicBoolean getIsShouldShutDown();

	//20211124 MAtsudairaSyuMe
	public byte[] MS_CheckAndRead(boolean b, String brws);

}
