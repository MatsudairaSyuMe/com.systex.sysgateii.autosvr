package com.systex.sysgateii.autosvr.telegram;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * P0880TEXT
 * MatsudairaSyume
 *  20200311
 */
public class P0880TEXT {
	private static Logger log = LoggerFactory.getLogger(P0880TEXT.class);
	// P0880 TITA_TEXT

	private int  GLCOMM = 50;  //!< (共用)
	private int  PBCNT  = 6;   //!< 存摺剩餘可列印行數
	private int  REQCNT = 4;   //!< 要求筆數
	private int  BEGIN  = 4;   //!< 要補未登摺之第幾筆
	private int  NBNO   = 9;   //!< 磁條存摺號碼
	private int  LINENO = 2;   //!< 磁條行次
	private int  PAGENO = 2;   //!< 磁條頁次

	private String  glcomm  = "glcomm";  //!< (共用)
	private String  pbcnt   = "pbcnt";   //!< 存摺剩餘可列印行數
	private String  reqcnt  = "reqcnt";   //!< 要求筆數
	private String  begin   = "begin";   //!< 要補未登摺之第幾筆
	private String  nbno    = "nbno";   //!< 磁條存摺號碼
	private String  lineno  = "lineno";   //!< 磁條行次
	private String  pageno  = "pageno";   //!< 磁條頁次

	private int p0880titatext_lens[] = {
			GLCOMM,  //!< (共用)
			PBCNT,   //!< 存摺剩餘可列印行數
			REQCNT,   //!< 要求筆數
			BEGIN,   //!< 要補未登摺之第幾筆
			NBNO,      //!< 磁條存摺號碼
			LINENO,   //!< 磁條行次
			PAGENO   //!< 磁條頁次
	};
	
	private String p0880titatext_names[] = {
			glcomm,  //!< (共用)
			pbcnt,   //!< 存摺剩餘可列印行數
			reqcnt,   //!< 要求筆數
			begin,   //!< 要補未登摺之第幾筆
			nbno,      //!< 磁條存摺號碼
			lineno,   //!< 磁條行次
			pageno   //!< 磁條頁次
	};
	
	// P0880 TOTA_TEXT    _nbdtl_area_1
	private int TXDAY    =8;   //!< 交易日期
	private int KINBR    =3;   //!< 異動分行
	private int TRMSEQ   =4;   //!< 異動櫃台機
	private int TOTAPAGENO   =2;   //!< 頁次
	private int TOTALINENO   =2;   //!< 行次
	private int DSCPTX   =10;  //!< 摘要說明
	private int CURCD    =2;   //!< 交易幣別(NT/US)
	private int PRICE    =8;   //!< 單價
	private int WITHSIGN =1;   //!< 回售/提領黃金數正負號
	private int WITHDRAW =7;   //!< 回售/提領黃金數
	private int DEPOSIGN =1;   //!< 存入黃金數正負號
	private int DEPOSIT  =7;   //!< 存入黃金數
	private int AVEBAL   =9;   //!< 餘額(對應 P1885-NPBBAL)
	private int DSCPT    =2;   //!< 類別
	private int TLRNO    =2;   //!< 異動櫃員
	private int HCODE    =1;   //!< 更正記號
	private int NBSEQ    =8;   //!< 交易序號(對應於 P1885 最後一筆登摺序號 NBSEQ)

	// p0880_tota
	private int TOTAGLCOMM    =8;   //!< (共用)時間
	private int NBCNT     =3;   //!< 登摺總筆數
	private int ENDFLG    =1;   //!< 結束記號(0:尚有未登摺資料 1:無未登摺資料)
   //	_nbdtl_area_1 oc =6;

	private String txday          = "txday";   //!< 交易日期
	private String kinbr          = "kinbr";   //!< 異動分行
	private String trmseq         = "trmseq";   //!< 異動櫃台機
	private String totapageno    = "totapageno";   //!< 頁次
	private String totalineno    = "totalineno";   //!< 行次
	private String dscptx          = "dscptx";  //!< 摘要說明
	private String curcd           = "curcd";   //!< 交易幣別(NT/US)
	private String price           = "price";   //!< 單價
	private String withsign       = "withsign";   //!< 回售/提領黃金數正負號
	private String withdraw       = "withdraw";   //!< 回售/提領黃金數
	private String deposign       = "deposign";   //!< 存入黃金數正負號
	private String deposit        = "deposit";   //!< 存入黃金數
	private String avebal         = "avebal";   //!< 餘額(對應 P1885-NPBBAL)
	private String dscpt          = "dscpt";   //!< 類別
	private String tlrno          = "tlrno";   //!< 異動櫃員
	private String hcode          = "hcode";   //!< 更正記號
	private String nbseq          = "nbseq";   //!< 交易序號(對應於 P1885 最後一筆登摺序號 NBSEQ)

	private String totaglcomm   = "totaglcomm";   //!< (共用)時間
	private String nbcnt          = "nbcnt";   //!< 登摺總筆數
	private String endflg         = "endflg";   //!< 結束記號(0:尚有未登摺資料 1:無未登摺資料)
//	_nbdtl_area_1 oc            = "";

	private int p0880totatext_lens[] = {
			TXDAY,         //!< 交易日期
			KINBR,         //!< 異動分行
			TRMSEQ,        //!< 異動櫃台機
			TOTAPAGENO,   //!< 頁次
			TOTALINENO,   //!< 行次
			DSCPTX,      //!< 摘要說明
			CURCD,        //!< 交易幣別(NT/US)
			PRICE,        //!< 單價
			WITHSIGN,    //!< 回售/提領黃金數正負號
			WITHDRAW,    //!< 回售/提領黃金數
			DEPOSIGN,    //!< 存入黃金數正負號
			DEPOSIT,     //!< 存入黃金數
			AVEBAL,       //!< 餘額(對應 P1885-NPBBAL)
			DSCPT,         //!< 類別
			TLRNO,         //!< 異動櫃員
			HCODE,         //!< 更正記號
			NBSEQ         //!< 交易序號(對應於 P1885 最後一筆登摺序號 NBSEQ)
	};

	private int p0880totaheadtext_lens[] = {
			TOTAGLCOMM,   //!< (共用)時間
			NBCNT,         //!< 登摺總筆數
			ENDFLG        //!< 結束記號(0:尚有未登摺資料 1:無未登摺資料)
//			_NBDTL_AREA_1 OC,
	};

	private String p0880totatext_names[] = {
			txday,         //!< 交易日期
			kinbr,         //!< 異動分行
			trmseq,        //!< 異動櫃台機
			totapageno,   //!< 頁次
			totalineno,   //!< 行次
			dscptx,      //!< 摘要說明
			curcd,        //!< 交易幣別(NT/US)
			price,        //!< 單價
			withsign,    //!< 回售/提領黃金數正負號
			withdraw,    //!< 回售/提領黃金數
			deposign,    //!< 存入黃金數正負號
			deposit,     //!< 存入黃金數
			avebal,       //!< 餘額(對應 P1885-NPBBAL)
			dscpt,         //!< 類別
			tlrno,         //!< 異動櫃員
			hcode,         //!< 更正記號
			nbseq         //!< 交易序號(對應於 P1885 最後一筆登摺序號 NBSEQ)
	};
	
	private String p0880totaheadtext_names[] = {
			totaglcomm,   //!< (共用)時間
			nbcnt,         //!< 登摺總筆數
			endflg        //!< 結束記號(0:尚有未登摺資料 1:無未登摺資料)
//			_nbdtl_area_1 oc,
	};

	public void setValueLtoRfill(String fieldN, String setsV, byte fillV) throws Exception {
		setValueLtoRfill(fieldN, setsV.getBytes(), fillV);
	}
	
	public void setValueLtoRfill(String fieldN, byte[] setbV, byte fillV) throws Exception {
		Field f = p0880titatext.get(p0880titatextname.get(fieldN));
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
		Field f = p0880titatext.get(p0880titatextname.get(fieldN));
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
		Field f = p0880titatext.get(p0880titatextname.get(fieldN));
		System.arraycopy(setbV, 0, p0880titatextary, f.offset, f.len);
	}

	public void setTotaTextValue(String fieldN, byte[] setbV, int oc) throws Exception {
		Field f = p0880totatext.get(p0880totatextname.get(fieldN));
		System.arraycopy(setbV, 0, p0880totatextary, (oc * this.p0880totatext_len) + f.offset, f.len);
		byte[] mdytext = this.p0880totatextlist.get(oc);
		System.arraycopy(setbV, 0, mdytext, f.offset, f.len);
		this.p0880totatextlist.set(oc, mdytext);
	}
	
	private List<Field> p0880titatext = new ArrayList<Field>();
	private Map<String, Integer> p0880titatextname = new HashMap<String, Integer>();
	private int p0880titatext_len = 0;
	private byte[] p0880titatextary = null;
	private List<Field> p0880totaheadtext = new ArrayList<Field>();
	private Map<String, Integer> p0880totaheadtextname = new HashMap<String, Integer>();
	private List<Field> p0880totatext = new ArrayList<Field>();
	private Map<String, Integer> p0880totatextname = new HashMap<String, Integer>();
	private int p0880totaheadtext_len = 0;
	private int p0880totatext_len = 0;
	private byte[] p0880totaheadtextary = null;
	private byte[] p0880totatextary = null;
	private ArrayList<byte[]> p0880totatextlist = null;

	public P0880TEXT() {
		log.debug("p0880titatext_lens items=" + p0880titatext_lens.length);
		int id = 0;
		for (int l : p0880titatext_lens) {
			Field f = new Field();
//            log.debug(String.format("%d offset %d len %d",id, p0880titatext_len, l));
			f.setData(p0880titatext_names[id], p0880titatext_len, l);
			p0880titatextname.put(p0880titatext_names[id], id);
			id += 1;
			p0880titatext.add(f);
			p0880titatext_len += l;
		}
		log.debug("p0880titatext_lens={}", p0880titatext_len);
		log.debug("p0880totaheadtext_lens items=" + p0880totaheadtext_lens.length);
		id = 0;
		for (int l : p0880totaheadtext_lens) {
			Field f = new Field();
//            log.debug(String.format("%d offset %d len %d",id, p0880titatext_len, l));
			f.setData(p0880totaheadtext_names[id], p0880totaheadtext_len, l);
			p0880totaheadtextname.put(p0880totaheadtext_names[id], id);
			id += 1;
			p0880totaheadtext.add(f);
			p0880totaheadtext_len += l;
		}
		log.debug("p0880totaheadtext_lens={}", p0880totaheadtext_len);
		log.debug("p0880totatext_lens items=" + p0880totatext_lens.length);
		id = 0;
		for (int l : p0880totatext_lens) {
			Field f = new Field();
//            log.debug(String.format("%d offset %d len %d",id, p0880titatext_len, l));
			f.setData(p0880totatext_names[id], p0880totatext_len, l);
			p0880totatextname.put(p0880totatext_names[id], id);
			id += 1;
			p0880totatext.add(f);
			p0880totatext_len += l;
		}
		log.debug("p0880totatext_lens={}", p0880totatext_len);
	}

	public int getP0880TitatextLen() {
		return this.p0880titatext_len;
	}

	public int getP0880TotaheadtextLen() {
		return this.p0880totaheadtext_len;
	}

	public int getP0880TotatextLen() {
		return this.p0880totatext_len;
	}

	public boolean initP0880TitaTEXT(byte initValue) {
		if (p0880titatext_len > 0) {
			p0880titatextary = new byte[p0880titatext_len];
			Arrays.fill(p0880titatextary, initValue);
			return true;
		} else
			return false;
	}

	public byte[] getP0880Titatext() {
		
		return p0880titatextary;
	}

	public boolean initP0880TotaheadTEXT(byte initValue) {
		if (p0880totaheadtext_len > 0) {
			p0880totaheadtextary = new byte[p0880totaheadtext_len];
			Arrays.fill(p0880totaheadtextary, initValue);
			return true;
		} else
			return false;
	}

	public boolean initP0880TotaTEXT(byte initValue) {
		if (p0880totatext_len > 0) {
			p0880totatextary = new byte[p0880totatext_len];
			Arrays.fill(p0880totatextary, initValue);
			return true;
		} else
			return false;
	}

	public byte[] getP0880Totaheadtext() {

		return p0880totaheadtextary;
	}

	public byte[] getP0880Totatext() {

		return p0880totatextary;
	}
	public boolean copyTotaHead(byte[] srcValue) {
		if (this.p0880totaheadtext_len > 0 && srcValue != null && srcValue.length > 0) {
			this.p0880totaheadtextary = new byte[this.p0880totaheadtext_len];
			System.arraycopy(srcValue, 0, this.p0880totaheadtextary, 0, this.p0880totaheadtext_len);
			return true;
		} else
			return false;
	}

	public boolean copyTotaText(byte[] srcValue, int ocn) {
		if (this.p0880totatext_len > 0 && srcValue != null && srcValue.length > 0 && ocn > 0) {
			this.p0880totatextary = new byte[this.p0880totatext_len * ocn];
			System.arraycopy(srcValue, 0, this.p0880totatextary, 0, this.p0880totatext_len * ocn);
			this.p0880totatextlist = new ArrayList<byte[]>();
			for (int i = 0;i < ocn; i++)
				this.p0880totatextlist.add(Arrays.copyOfRange(this.p0880totatextary, i * this.p0880totatext_len, (i * this.p0880totatext_len) + this.p0880totatext_len));
			return true;
		} else
			return false;
	}

	public boolean appendTotaText(byte[] srcValue) {
		if (this.p0880totatext_len > 0 && srcValue != null && srcValue.length > 0) {
			if (this.p0880totatextary == null || this.p0880totatextary.length == 0) {
					this.p0880totatextary = new byte[this.p0880totatext_len];
					System.arraycopy(srcValue, 0, this.p0880totaheadtextary, 0, this.p0880totatext_len);
			} else {
				byte[] newary = new byte[this.p0880totatextary.length + this.p0880totatext_len];
				System.arraycopy(this.p0880totatextary, 0, newary, 0, this.p0880totatextary.length);
				System.arraycopy(srcValue, 0, newary, this.p0880totatextary.length, this.p0880totatext_len);
				this.p0880totatextary = newary;
			}
			if (this.p0880totatextlist == null)
				this.p0880totatextlist = new ArrayList<byte[]>();
			this.p0880totatextlist.add(srcValue);
			return true;
		} else
			return false;
	}

	public byte[] getHeadValue(String fieldN) throws Exception {
		byte[] rtn = null;
		Field f = p0880totaheadtext.get(p0880totaheadtextname.get(fieldN));
		rtn = new byte[f.len];
		System.arraycopy(p0880totaheadtextary, f.offset, rtn, 0, f.len);
		return rtn;
	}

	public byte[] getTotaTextValue(String fieldN, int oc) throws Exception {
		byte[] rtn = null;
		Field f = p0880totatext.get(p0880totatextname.get(fieldN));
		rtn = new byte[f.len];
		System.arraycopy(p0880totatextary, (oc * this.p0880totatext_len) + f.offset, rtn, 0, f.len);
		return rtn;
	}

	public byte[] getTotaTextValueSrc(String fieldN, byte[] totasrc) throws Exception {
		byte[] rtn = null;
		Field f = p0880totatext.get(p0880totatextname.get(fieldN));
		rtn = new byte[f.len];
		System.arraycopy(totasrc, f.offset, rtn, 0, f.len);
		return rtn;
	}

	public ArrayList<byte[]> getTotaTextLists() throws Exception {
		return this.p0880totatextlist;
	}

	public byte[] getTotaTexOc(int oc) throws Exception {
		return this.p0880totatextlist.get(oc);
	}

	public static void main(String[] args) throws Exception {
		boolean rtn = false;
		P0880TEXT tl = new P0880TEXT();
		rtn = tl.initP0880TitaTEXT((byte) '0');
		tl.setValueLtoRfill("glcomm", "00".getBytes(), (byte) ' ');
		tl.setValueRtoLfill("pbcnt", String.format("%d", 999), (byte) '0');
		tl.setValueRtoLfill("reqcnt", Integer.toString(1), (byte) '0');
		tl.setValueRtoLfill("begin", Integer.toString(1), (byte) '0');
		tl.setValue("nbno","000000304");
		tl.setValue("lineno","24");
		tl.setValue("pageno","01");
		log.debug("tl.initP0880TitaTEXT rtn={}", rtn);
		byte[] result = tl.getP0880Titatext();
		// System.out.println("2--->" + Arrays.toString(result) + ":[" + new
		// String(result) + "]");
		log.debug("2--->[{}]len={}", new String(result), tl.getP0880TitatextLen());
		rtn = tl.initP0880TotaheadTEXT((byte) '0');
		result = tl.getP0880Totaheadtext();
		// System.out.println("2--->" + Arrays.toString(result) + ":[" + new
		// String(result) + "]");
		log.debug("3--->[{}] len={}", new String(result), tl.getP0880TotaheadtextLen());
		rtn = tl.initP0880TotaTEXT((byte) '0');
		result = tl.getP0880Totatext();
		// System.out.println("2--->" + Arrays.toString(result) + ":[" + new
		// String(result) + "]");
		log.debug("4--->[{}] len={}", new String(result), tl.getP0880TotatextLen());
	}

}
