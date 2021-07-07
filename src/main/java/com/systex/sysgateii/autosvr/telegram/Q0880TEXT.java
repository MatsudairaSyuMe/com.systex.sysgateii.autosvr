package com.systex.sysgateii.autosvr.telegram;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Q0880TEXT
 * MatsudairaSyume
 *  2020304
 */
public class Q0880TEXT {
	private static Logger log = LoggerFactory.getLogger(Q0880TEXT.class);
	// Q0880 TITA_TEXT
	// _fxcomm
	private int NEWSEQ = 5;
	private int OLDSEQ = 5;
	private int OLDWSNO= 2;
	private int DEPT   = 1;
	private int RETUR  = 1;
   private int OLDWSNOBUFG  = 1;
	private int RBRNO  = 3;
	private int ACBRNO = 3;
	private int APTYPE = 2;
	private int CIFKEY = 10;
	private int CORPNO = 2;
	private int ACTFG  = 1;
	private int TRCNT  = 2;
	private int STATCD = 1;
	private int FILLER = 1;

	private int  PBCNT  = 3;   //!< 存摺簿可列印筆數
	private int  PBVER  = 2;   //!< 領用序號
	private int  BEGIN  = 4;   //!< 輸出資料起始值(初值:1)
	private int  TXNOS  = 4;   //!< 要求筆數(6)
	private int  TXDAY  = 8;   //!< 交易日期(初值:0)
	private int  TXSEQ  = 6;   //!< 交易序號(初值:0)
	private int  PBCOL  = 2;   //!< 起印行
	private int  PBPAGE = 2;   //!< 起印頁

	private String newseq        = "newseq";
	private String oldseq        = "oldseq";
	private String oldwsno       = "oldwsno";
	private String dept           = "dept";
	private String retur          = "retur";
    private String oldwsnobufg  = "oldwsnobufg";
	private String rbrno          = "rbrno";
	private String acbrno        = "acbrno";
	private String aptype        = "aptype";
	private String cifkey        = "cifkey";
	private String corpno        = "corpno";
	private String actfg         = "actfg";        
	private String trcnt         = "trcnt";        
	private String statcd       = "statcd";       
	private String filler       = "filler";       

	private String  pbcnt       = "pbcnt";         //!< 存摺簿可列印筆數
	private String  pbver       = "pbver";         //!< 領用序號
	private String  begin       = "begin";         //!< 輸出資料起始值(初值:1)
	private String  txnos       = "txnos";         //!< 要求筆數(6)
	private String  txday       = "txday";         //!< 交易日期(初值:0)
	private String  txseq       = "txseq";         //!< 交易序號(初值:0)
	private String  pbcol       = "pbcol";         //!< 起印行
	private String  pbpage      = "pbpage";         //!< 起印頁

	private int q0880titatext_lens[] = {
			NEWSEQ,
			 OLDSEQ,
			 OLDWSNO,
			 DEPT,
			 RETUR,
		    OLDWSNOBUFG,
			 RBRNO,
			 ACBRNO,
			 APTYPE,
			 CIFKEY,
			 CORPNO,
			 ACTFG,
			 TRCNT,
			 STATCD,
			 FILLER,

			  PBCNT,
			  PBVER,
			  BEGIN,
			  TXNOS,
			  TXDAY,
			  TXSEQ,
			  PBCOL,
			  PBPAGE
	};
	private String q0880titatext_names[] = {
			 newseq,
			 oldseq,
			 oldwsno,
			 dept,
			 retur,
		     oldwsnobufg,
			 rbrno,
			 acbrno,
			 aptype,
			 cifkey,
			 corpno,
			 actfg,
			 trcnt,
			 statcd,
			 filler,

			  pbcnt,
			  pbver,
			  begin,
			  txnos,
			  txday,
			  txseq,
			  pbcol,
			  pbpage
	};

	//Q0880 TOTA_TEXT
	private int TOTATXDAY  = 8;   //!< 交易日期
	private int DATE   = 8;   //!< 無摺資料交易日
	private int TOTATXSEQ  = 6;   //!< 交易序號
	private int HCODE  = 1;   //!< 更正記號
	private int KINBR  = 3;   //!< 櫃員機號1
	private int TRMSEQ = 2;   //!< 櫃員機號2
	private int DSCPTX = 16;  //!< 備註
	private int CURCD  = 3;   //!< 幣別
	private int CRDB   = 1;   //!< 借貸別(1:借 2:貸)
	private int TXAMT  = 13;  //!< 交易金額
	private int PBBAL  = 13;  //!< 存摺餘額
	private int MARK   = 1;   //!< "+"

	//q0880 tota
	private int FXCOMM   = 13;  //!< (共用)
	private int NBCNT    = 3;   //!< 總筆數
	private int ENDFLG   = 1;   //!< 結束記號
//	_text_area  OC[6];

	private String totatxday  = "totatxday";   //!< 交易日期
	private String date   = "date";   //!< 無摺資料交易日
	private String totatxseq  = "totatxseq";   //!< 交易序號
	private String hcode  = "hcode";   //!< 更正記號
	private String kinbr  = "kinbr";   //!< 櫃員機號1
	private String trmseq = "trmseq";   //!< 櫃員機號2
	private String dscptx = "dscptx";  //!< 備註
	private String curcd  = "curcd";   //!< 幣別
	private String crdb   = "crdb";   //!< 借貸別(1:借 2:貸)
	private String txamt  = "txamt";  //!< 交易金額
	private String pbbal  = "pbbal";  //!< 存摺餘額
	private String mark   = "mark";   //!< "+"

	private String fxcomm   = "fxcomm";  //!< (共用)
	private String nbcnt    = "nbcnt";   //!< 總筆數
	private String endflg   = "endflg";   //!< 結束記號
//	_text_area  oc[6];

	private int q0880totatext_lens[] = {
			TOTATXDAY,   //!< 交易日期
			DATE,   //!< 無摺資料交易日
			TOTATXSEQ,   //!< 交易序號
			HCODE,   //!< 更正記號
			KINBR,   //!< 櫃員機號1
			TRMSEQ,   //!< 櫃員機號2
			DSCPTX,  //!< 備註
			CURCD,   //!< 幣別
			CRDB,   //!< 借貸別(1:借 2:貸)
			TXAMT,  //!< 交易金額
			PBBAL,  //!< 存摺餘額
			MARK   //!< "+"
	};
	
	private int q0880totaheadtext_lens[] = {
			FXCOMM,  //!< (共用)
			NBCNT,   //!< 總筆數
			ENDFLG   //!< 結束記號
//			OC[6],		
	};

	private String q0880totatext_names[] = {
			totatxday,   //!< 交易日期
			date,   //!< 無摺資料交易日
			totatxseq,   //!< 交易序號
			hcode,   //!< 更正記號
			kinbr,   //!< 櫃員機號1
			trmseq,   //!< 櫃員機號2
			dscptx,  //!< 備註
			curcd,   //!< 幣別
			crdb,   //!< 借貸別(1:借 2:貸)
			txamt,  //!< 交易金額
			pbbal,  //!< 存摺餘額
			mark   //!< "+"
	};
	
	private String q0880totaheadtext_names[] = {
			fxcomm,  //!< (共用)
			nbcnt,   //!< 總筆數
			endflg   //!< 結束記號
//			oc[6],	
	};

	public void setValueLtoRfill(String fieldN, String setsV, byte fillV) throws Exception {
		setValueLtoRfill(fieldN, setsV.getBytes(), fillV);
	}
	
	public void setValueLtoRfill(String fieldN, byte[] setbV, byte fillV) throws Exception {
		Field f = q0880titatext.get(q0880titatextname.get(fieldN));
		byte[] ntb = null;
		if (setbV.length < f.len) {
//     int diff = f.len - setbV.length;
			ntb = new byte[f.len];
			Arrays.fill(ntb, fillV);
			System.arraycopy(setbV, 0, ntb, 0, setbV.length);
		} else
			ntb = setbV;
		setValue(fieldN, ntb);
		System.gc();
	}
	
	public void setValueRtoLfill(String fieldN, String setsV, byte fillV) throws Exception {
		setValueRtoLfill(fieldN, setsV.getBytes(), fillV);
	}
	
	public void setValueRtoLfill(String fieldN, byte[] setbV, byte fillV) throws Exception {
		Field f = q0880titatext.get(q0880titatextname.get(fieldN));
		byte[] ntb = null;
		if (setbV.length < f.len) {
			int diff = f.len - setbV.length;
			ntb = new byte[f.len];
			Arrays.fill(ntb, fillV);
			System.arraycopy(setbV, 0, ntb, diff, setbV.length);
		} else
			ntb = setbV;
		setValue(fieldN, ntb);
		System.gc();
	}

	public void setValue(String fieldN, String setsV) throws Exception {
		setValue(fieldN, setsV.getBytes());
	}

	public void setValue(String fieldN, byte[] setbV) throws Exception {
		Field f = q0880titatext.get(q0880titatextname.get(fieldN));
//		log.debug(fieldN + ":");
		System.arraycopy(setbV, 0, q0880titatextary, f.offset, f.len);
	}
	public boolean appendTitaText(String fieldN, byte[] srcValue) {
		Field f = q0880titatext.get(q0880titatextname.get(fieldN));
		if (this.q0880titatext_len > 0 && srcValue != null && srcValue.length > 0) {
			if (this.q0880titatextary == null || this.q0880titatextary.length == 0) {
					this.q0880titatextary = new byte[this.q0880titatext_len];
			}
			int cplen = this.q0880titatext_len - f.offset;
			if (srcValue.length < cplen)
				cplen = srcValue.length;
			log.debug("fieldName={} offset={} q0880titatext_len={} setval.len={} cplen={}",
				f.name, f.offset ,this.q0880titatext_len,srcValue.length,cplen);
			System.arraycopy(srcValue, 0, this.q0880titatextary, f.offset, cplen);
			return true;
		} else
			return false;
	}
	public void setTotaTextValue(String fieldN, byte[] setbV, int oc) throws Exception {
		Field f = q0880totatext.get(q0880totatextname.get(fieldN));
		System.arraycopy(setbV, 0, q0880totatextary, (oc * this.q0880totatext_len) + f.offset, f.len);
		byte[] mdytext = this.q0880totatextlist.get(oc);
		System.arraycopy(setbV, 0, mdytext, f.offset, f.len);
		this.q0880totatextlist.set(oc, mdytext);
	}

	public int getQ0880TitatextLen() {
		return this.q0880titatext_len;
	}
	public byte[] getQ0880Titatext() {
		return q0880titatextary;
	}

	public boolean initQ0880TitaTEXT(byte initValue) {
		if (q0880titatext_len > 0) {
				q0880titatextary = new byte[q0880titatext_len];
			Arrays.fill(q0880titatextary, initValue);
			return true;
		} else
			return false;
	}

	public boolean initQ0880TotaheadTEXT(byte initValue) {
		if (q0880totaheadtext_len > 0) {
			q0880totaheadtextary = new byte[q0880totaheadtext_len];
			Arrays.fill(q0880totaheadtextary, initValue);
			return true;
		} else
			return false;
	}

	public boolean initQ0880TotaTEXT(byte initValue) {
		if (q0880totatext_len > 0) {
			q0880totatextary = new byte[q0880totatext_len];
			Arrays.fill(q0880totatextary, initValue);
			return true;
		} else
			return false;
	}

	public byte[] getQ0880Totaheadtext() {

		return q0880totaheadtextary;
	}

	public byte[] getQ0880Totatext() {

		return q0880totatextary;
	}
	public int getQ0880TotaheadtextLen() {
		return this.q0880totaheadtext_len;
	}

	public int getQ0880TotatextLen() {
		return this.q0880totatext_len;
	}

	private List<Field> q0880titatext = new ArrayList<Field>();
	private Map<String, Integer> q0880titatextname = new HashMap<String, Integer>();
	private int q0880titatext_len = 0;
	private byte[] q0880titatextary = null;
	private List<Field> q0880totaheadtext = new ArrayList<Field>();
	private Map<String, Integer> q0880totaheadtextname = new HashMap<String, Integer>();
	private List<Field> q0880totatext = new ArrayList<Field>();
	private Map<String, Integer> q0880totatextname = new HashMap<String, Integer>();
	private int q0880totaheadtext_len = 0;
	private int q0880totatext_len = 0;
	private byte[] q0880totaheadtextary = null;
	private byte[] q0880totatextary = null;
	private ArrayList<byte[]> q0880totatextlist = null;

	public Q0880TEXT() {
		log.debug("q0880titatext_lens items=" + q0880titatext_lens.length);
		int id = 0;
		for (int l : q0880titatext_lens) {
			Field f = new Field();
//         log.debug(String.format("%d [%s] offset %d len %d",id, p85titatext_names[id], p85titatext_len, l));
			f.setData(q0880titatext_names[id], q0880titatext_len, l);
			q0880titatextname.put(q0880titatext_names[id], id);
			id += 1;
			q0880titatext.add(f);
			q0880titatext_len += l;
		}
		log.debug("q0880titatext_lens={}", q0880titatext_len);
		log.debug("q0880totaheadtext_lens items=" + q0880totaheadtext_lens.length);
		id = 0;
		for (int l : q0880totaheadtext_lens) {
			Field f = new Field();
//            log.debug(String.format("%d offset %d len %d",id, q0880titatext_len, l));
			f.setData(q0880totaheadtext_names[id], q0880totaheadtext_len, l);
			q0880totaheadtextname.put(q0880totaheadtext_names[id], id);
			id += 1;
			q0880totaheadtext.add(f);
			q0880totaheadtext_len += l;
		}
		log.debug("q0880totaheadtext_lens={}", q0880totaheadtext_len);
		log.debug("q0880totatext_lens items=" + q0880totatext_lens.length);
		id = 0;
		for (int l : q0880totatext_lens) {
			Field f = new Field();
//            log.debug(String.format("%d offset %d len %d",id, q0880titatext_len, l));
			f.setData(q0880totatext_names[id], q0880totatext_len, l);
			q0880totatextname.put(q0880totatext_names[id], id);
			id += 1;
			q0880totatext.add(f);
			q0880totatext_len += l;
		}
		log.debug("q0880totatext_lens={}", q0880totatext_len);
	}
	public boolean copyTotaHead(byte[] srcValue) {
		if (this.q0880totaheadtext_len > 0 && srcValue != null && srcValue.length > 0) {
			this.q0880totaheadtextary = new byte[this.q0880totaheadtext_len];
			System.arraycopy(srcValue, 0, this.q0880totaheadtextary, 0, this.q0880totaheadtext_len);
			return true;
		} else
			return false;
	}

	public boolean copyTotaText(byte[] srcValue, int ocn) {
		if (this.q0880totatext_len > 0 && srcValue != null && srcValue.length > 0 && ocn > 0) {
			this.q0880totatextary = new byte[this.q0880totatext_len * ocn];
			System.arraycopy(srcValue, 0, this.q0880totatextary, 0, this.q0880totatext_len * ocn);
			this.q0880totatextlist = new ArrayList<byte[]>();
			for (int i = 0;i < ocn; i++)
				this.q0880totatextlist.add(Arrays.copyOfRange(this.q0880totatextary, i * this.q0880totatext_len, (i * this.q0880totatext_len) + this.q0880totatext_len));
			return true;
		} else
			return false;
	}

	public boolean appendTotaText(byte[] srcValue) {
		if (this.q0880totatext_len > 0 && srcValue != null && srcValue.length > 0) {
			if (this.q0880totatextary == null || this.q0880totatextary.length == 0) {
					this.q0880totatextary = new byte[this.q0880totatext_len];
					System.arraycopy(srcValue, 0, this.q0880totaheadtextary, 0, this.q0880totatext_len);
			} else {
				byte[] newary = new byte[this.q0880totatextary.length + this.q0880totatext_len];
				System.arraycopy(this.q0880totatextary, 0, newary, 0, this.q0880totatextary.length);
				System.arraycopy(srcValue, 0, newary, this.q0880totatextary.length, this.q0880totatext_len);
				this.q0880totatextary = newary;
			}
			if (this.q0880totatextlist == null)
				this.q0880totatextlist = new ArrayList<byte[]>();
			this.q0880totatextlist.add(srcValue);
			return true;
		} else
			return false;
	}

	public byte[] getHeadValue(String fieldN) throws Exception {
		byte[] rtn = null;
		Field f = q0880totaheadtext.get(q0880totaheadtextname.get(fieldN));
		rtn = new byte[f.len];
		System.arraycopy(q0880totaheadtextary, f.offset, rtn, 0, f.len);
		return rtn;
	}

	public byte[] getTotaTextValue(String fieldN, int oc) throws Exception {
		byte[] rtn = null;
		Field f = q0880totatext.get(q0880totatextname.get(fieldN));
		rtn = new byte[f.len];
		System.arraycopy(q0880totatextary, (oc * this.q0880totatext_len) + f.offset, rtn, 0, f.len);
		return rtn;
	}

	public byte[] getTotaTextValueSrc(String fieldN, byte[] totasrc) throws Exception {
		byte[] rtn = null;
		Field f = q0880totatext.get(q0880totatextname.get(fieldN));
		rtn = new byte[f.len];
		System.arraycopy(totasrc, f.offset, rtn, 0, f.len);
		return rtn;
	}

	public ArrayList<byte[]> getTotaTextLists() throws Exception {
		return this.q0880totatextlist;
	}

	public byte[] getTotaTexOc(int oc) throws Exception {
		return this.q0880totatextlist.get(oc);
	}


	public static void main(String[] args) throws Exception {
		boolean rtn;
		Q0880TEXT tl = new Q0880TEXT();
		rtn = tl.initQ0880TitaTEXT((byte) '0');
		log.debug("tl.initQ0880TitaTEXT rtn={}", rtn);
		byte[] result = tl.getQ0880Titatext();
		// System.out.println("2--->" + Arrays.toString(result) + ":[" + new
		// String(result) + "]");
		log.debug("2--->[{}]len={}", new String(result), tl.getQ0880TitatextLen());
		rtn = tl.initQ0880TotaheadTEXT((byte) '0');
		result = tl.getQ0880Totaheadtext();
		// System.out.println("2--->" + Arrays.toString(result) + ":[" + new
		// String(result) + "]");
		log.debug("3--->[{}] len={}", new String(result), tl.getQ0880TotaheadtextLen());
		rtn = tl.initQ0880TotaTEXT((byte) '0');
		result = tl.getQ0880Totatext();
		// System.out.println("2--->" + Arrays.toString(result) + ":[" + new
		// String(result) + "]");
		log.debug("4--->[{}] len={}", new String(result), tl.getQ0880TotatextLen());

	}
}
