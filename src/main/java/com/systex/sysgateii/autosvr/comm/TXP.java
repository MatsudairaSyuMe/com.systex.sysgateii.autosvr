package com.systex.sysgateii.autosvr.comm;

public class TXP {
	public static final int ACTNO_LEN = 12;    //!< 帳號 for PB/FC/GL
	public static final int ACFILLER_LEN = 4;  //!< 帳號保留 for for PB/FC
	public static final int MSRBAL_LEN = 14;   //!< 磁條餘額 for PB/FC
	public static final int MSRBALGL_LEN = 12; //!< 磁條餘額 for GL
	public static final int LINE_LEN = 2;      //!< 行次     for PB/FC/GL
	public static final int PAGE_LEN = 2;      //!< 頁次     for PB/FC/GL
	public static final int BKSEQ_LEN = 1;     //!< 領用序號 for PB
	public static final int PBVER_LEN = 2;     //!< 領用序號 for FC
	public static final int NO_LEN = 9;        //!< 存摺號碼 for GL
	public static final int INQ = 1;           // data function -- 1:INQ 
	public static final int DEL = 2;           // data function -- 2:DEL
	public static final int PBTYPE = 1;        // AP type -- 1:PB
	public static final int FCTYPE = 2;        // AP type -- 2:FC
	public static final int GLTYPE = 3;        // AP type -- 3:GL
	//20200810
	public static final int C0099TYPE = 4;     // AP type -- 4:C0099
	//----
	public static final int SENDTHOST = 1;     //Send to Host
	public static final int RECVFHOST = 2;     //Recv from Host
	public static final int CONTROL_BUFFER_SIZE = 12;
	public static final int PB_MAX_PAGE = 8;   //PB存摺 只有8頁
	public static final int FC_MAX_PAGE = 5;   //FC存摺 只有5頁
	public static final int GL_MAX_PAGE = 9;   //GL存摺 只有9頁
	public static final String AMOUNT =  "ZZZZZ,ZZZ,Z-9.99";
	public static final String AMOUNT1 = "ZZZZZZZ,ZZZ,Z-9.99";
	public static final String AMOUNT2 = "*ZZZZZZ,ZZZ,Z-9.99";
	public static final String GRAM1  =  "ZZZ,Z-9.99";
	public static final String GRAM2  = "Z,ZZZ,Z-9.99";
}
