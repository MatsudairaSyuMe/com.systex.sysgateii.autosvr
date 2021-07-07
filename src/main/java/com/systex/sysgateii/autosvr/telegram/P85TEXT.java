package com.systex.sysgateii.autosvr.telegram;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * P0080TEXT
 * MatsudairaSyume
 *  20200224
 */
public class P85TEXT {
	private static Logger log = LoggerFactory.getLogger(P85TEXT.class);
	// P85 TITA_TEXT
	private int   BKSEQ   =  1;   //!< 領用序號
	private int   SNPBBAL = 1;   //!< 新存摺餘額正負號
	private int   NPBBAL  = 13;  //!< 新存摺餘額
	private int   DELCNT  = 4;   //!< 刪除筆數
	
	private int DATE   = 8;  //!< 交易日期
	private int TRMNO  =  5;  //!< 櫃台機編號
	private int TLRNO  =  2;  //!< 櫃員編號
	private int DSCPT  =  5;  //!< 交易摘要
	private int STXAMT = 1;  //!< 交易金額正負號
	private int TXAMT  = 13; //!< 交易金額
	private int CRDB   =  1;  //!< 借貸記號
	private int DSPTYPE = 1;  //!< 備註種類
	private int DSPTKD  = 3;  //!< 備註型式
	private int DSPTEXT = 18; //!< 備註內容

	
	String bkseq    = "bkseq";   //!< 領用序號
	String snpbbal  = "snpbbal";   //!< 新存摺餘額正負號
	String npbbal   = "npbbal";  //!< 新存摺餘額
	String delcnt   = "delcnt";   //!< 刪除筆數
	
	String  date   =  "date";  //!< 交易日期
	String  trmno  =  "trmno";  //!< 櫃台機編號
	String  tlrno  =  "tlrno";  //!< 櫃員編號
	String  dscpt  =  "dscpt";  //!< 交易摘要
	String  stxamt = "stxamt";  //!< 交易金額正負號
	String  txamt  =  "txamt"; //!< 交易金額
	String  crdb   =  "crdb";  //!< 借貸記號
	String  dsptype = "dsptype";  //!< 備註種類
	String  dsptkd  = "dsptkd";  //!< 備註型式
	String  dsptext = "dsptext"; //!< 備註內容


	private int p85titatext_lens[] = {
		BKSEQ,  
		SNPBBAL,
		NPBBAL, 
		DELCNT,
		DATE,
		TRMNO,
		TLRNO,
		DSCPT,
		STXAMT,
		TXAMT,
		CRDB,
		DSPTYPE,
		DSPTKD,
		DSPTEXT
	};
	private String p85titatext_names[] = {
		bkseq,
		snpbbal,
		npbbal,
		delcnt,
		date,
		trmno,
		tlrno,
		dscpt,
		stxamt,
		txamt,
		crdb,
		dsptype,
		dsptkd,
		dsptext
	};
	public void setValueLtoRfill(String fieldN, String setsV, byte fillV) throws Exception {
		setValueLtoRfill(fieldN, setsV.getBytes(), fillV);
	}
	
	public void setValueLtoRfill(String fieldN, byte[] setbV, byte fillV) throws Exception {
		Field f = p85titatext.get(p85titatextname.get(fieldN));
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
		Field f = p85titatext.get(p85titatextname.get(fieldN));
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
		Field f = p85titatext.get(p85titatextname.get(fieldN));
//		log.debug(fieldN + ":");
		System.arraycopy(setbV, 0, p85titatextary, f.offset, f.len);
	}
	public boolean appendTitaText(String fieldN, byte[] srcValue) {
		log.debug(fieldN + ":");

		Field f = p85titatext.get(p85titatextname.get(fieldN));
		if (this.p85titatext_len > 0 && srcValue != null && srcValue.length > 0) {
			if (this.p85titatextary == null || this.p85titatextary.length == 0) {
					this.p85titatextary = new byte[this.p85titatext_len];
			}
			int cplen = this.p85titatext_len - f.offset;
			if (srcValue.length < cplen)
				cplen = srcValue.length;
			log.debug("fieldName={} offset={} p85titatext_len={} setval.len={} cplen={}",
				f.name, f.offset ,this.p85titatext_len,srcValue.length,cplen);
			System.arraycopy(srcValue, 0, this.p85titatextary, f.offset, cplen);
			return true;
		} else
			return false;
	}

	public int getP85TitatextLen() {
		return this.p85titatext_len;
	}
	public byte[] getP85Titatext() {
		return p85titatextary;
	}
	public boolean initP85TitaTEXT(byte initValue) {
		if (p85titatext_len > 0) {
			p85titatextary = new byte[p85titatext_len];
			Arrays.fill(p85titatextary, initValue);
			return true;
		} else
			return false;
	}

	private List<Field> p85titatext = new ArrayList<Field>();
	private Map<String, Integer> p85titatextname = new HashMap<String, Integer>();
	private int p85titatext_len = 0;
	private byte[] p85titatextary = null;

	public P85TEXT() {
//		log.debug("p85titatext_lens items=" + p85titatext_lens.length);
		int id = 0;
		for (int l : p85titatext_lens) {
			Field f = new Field();
//         log.debug(String.format("%d [%s] offset %d len %d",id, p85titatext_names[id], p85titatext_len, l));
			f.setData(p85titatext_names[id], p85titatext_len, l);
			p85titatextname.put(p85titatext_names[id], id);
			id += 1;
			p85titatext.add(f);
			p85titatext_len += l;
		}
		log.debug("p85titatext_lens={}", p85titatext_len);
	}
	public static void main(String[] args) throws Exception {
		boolean rtn;
		P85TEXT tl = new P85TEXT();
		rtn = tl.initP85TitaTEXT((byte) '0');
		log.debug("tl.initP85TitaTEXT rtn={}", rtn);
		byte[] result = tl.getP85Titatext();
		// System.out.println("2--->" + Arrays.toString(result) + ":[" + new
		// String(result) + "]");
		log.debug("2--->[{}]len={}", new String(result), tl.getP85TitatextLen());
	}

}
