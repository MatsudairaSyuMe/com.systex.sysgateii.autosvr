package com.systex.sysgateii.autosvr.telegram;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
	 * P1885TEXT
	 * MatsudairaSyume
	 *  2020312
	 */
public class P1885TEXT {
	private static Logger log = LoggerFactory.getLogger(P1885TEXT.class);
	// P1885 TITA_TEXT
	private int GLCOMM =50;  //!< (共用)
	private int SNPBBAL=1;   //!< 最新存摺餘額正負號
	private int NPBBAL =12;  //!< 最新存摺餘額(對應 P0880-AVEBAL)
	private int DELCNT =4;   //!< 本次共登摺筆數
	private int NBDAY  =8;   //!< 交易日期(對應 P0880-TXDAY)
	private int NBSEQ  =8;   //!< 登摺最後一筆序號(對應 P0880-NBSEQ)
	private int KINBR  =3;   //!< 異動分行
	private int TRMSEQ =4;   //!< 異動櫃台機
	private int NBNO   =9;   //!< 存摺號碼
	private int LINENO =2;   //!< 最新存摺行次
	private int PAGENO =2;   //!< 最新存摺頁次
	private int END     =1;   //!< "$"

	private String glcomm = "glcomm";  //!< (共用)
	private String snpbbal= "snpbbal";   //!< 最新存摺餘額正負號
	private String npbbal = "npbbal";  //!< 最新存摺餘額(對應 P0880-AVEBAL)
	private String delcnt = "delcnt";   //!< 本次共登摺筆數
	private String nbday  = "nbday";   //!< 交易日期(對應 P0880-TXDAY)
	private String nbseq  = "nbseq";   //!< 登摺最後一筆序號(對應 P0880-NBSEQ)
	private String kinbr  = "kinbr";   //!< 異動分行
	private String trmseq = "trmseq";   //!< 異動櫃台機
	private String nbno    = "nbno";   //!< 存摺號碼
	private String lineno = "lineno";   //!< 最新存摺行次
	private String pageno = "pageno";   //!< 最新存摺頁次
	private String end     = "end";   //!< "$"

	private int p1885titatext_lens[] = {
			GLCOMM,   //!< (共用)
			SNPBBAL,   //!< 最新存摺餘額正負號
			NPBBAL,   //!< 最新存摺餘額(對應 P0880-AVEBAL)
			DELCNT,   //!< 本次共登摺筆數
			NBDAY,     //!< 交易日期(對應 P0880-TXDAY)
			NBSEQ,     //!< 登摺最後一筆序號(對應 P0880-NBSEQ)
			KINBR,     //!< 異動分行
			TRMSEQ,   //!< 異動櫃台機
			NBNO,      //!< 存摺號碼
			LINENO,   //!< 最新存摺行次
			PAGENO,   //!< 最新存摺頁次
			END        //!< "$"
	};

	private String p1885titatext_names[] = {
			glcomm,   //!< (共用)
			snpbbal,   //!< 最新存摺餘額正負號
			npbbal,   //!< 最新存摺餘額(對應 P0880-AVEBAL)
			delcnt,   //!< 本次共登摺筆數
			nbday,     //!< 交易日期(對應 P0880-TXDAY)
			nbseq,     //!< 登摺最後一筆序號(對應 P0880-NBSEQ)
			kinbr,     //!< 異動分行
			trmseq,   //!< 異動櫃台機
			nbno,      //!< 存摺號碼
			lineno,   //!< 最新存摺行次
			pageno,   //!< 最新存摺頁次
			end        //!< "$"
	};

	public void setValueLtoRfill(String fieldN, String setsV, byte fillV) throws Exception {
		setValueLtoRfill(fieldN, setsV.getBytes(), fillV);
	}
	
	public void setValueLtoRfill(String fieldN, byte[] setbV, byte fillV) throws Exception {
		Field f = p1885titatext.get(p1885titatextname.get(fieldN));
		byte[] ntb = null;
		if (setbV.length < f.len) {
//     int diff = f.len - setbV.length;
			ntb = new byte[f.len];
			Arrays.fill(ntb, fillV);
			System.arraycopy(setbV, 0, ntb, 0, setbV.length);
		} else
			ntb = setbV;
		setValue(fieldN, ntb);
		//20240517 Code Correctness: Call to System.gc() System.gc();
	}
	
	public void setValueRtoLfill(String fieldN, String setsV, byte fillV) throws Exception {
		setValueRtoLfill(fieldN, setsV.getBytes(), fillV);
	}
	
	public void setValueRtoLfill(String fieldN, byte[] setbV, byte fillV) throws Exception {
		Field f = p1885titatext.get(p1885titatextname.get(fieldN));
		byte[] ntb = null;
		if (setbV.length < f.len) {
			int diff = f.len - setbV.length;
			ntb = new byte[f.len];
			Arrays.fill(ntb, fillV);
			System.arraycopy(setbV, 0, ntb, diff, setbV.length);
		} else
			ntb = setbV;
		setValue(fieldN, ntb);
		//20240517 Code Correctness: Call to System.gc() System.gc();
	}

	public void setValue(String fieldN, String setsV) throws Exception {
		setValue(fieldN, setsV.getBytes());
	}

	public void setValue(String fieldN, byte[] setbV) throws Exception {
		Field f = p1885titatext.get(p1885titatextname.get(fieldN));
//		log.debug(fieldN + ":");
		System.arraycopy(setbV, 0, p1885titatextary, f.offset, f.len);
	}
	public boolean appendTitaText(String fieldN, byte[] srcValue) {
		Field f = p1885titatext.get(p1885titatextname.get(fieldN));
		if (this.p1885titatext_len > 0 && srcValue != null && srcValue.length > 0) {
			if (this.p1885titatextary == null || this.p1885titatextary.length == 0) {
					this.p1885titatextary = new byte[this.p1885titatext_len];
			}
			int cplen = this.p1885titatext_len - f.offset;
			if (srcValue.length < cplen)
				cplen = srcValue.length;
			log.debug("fieldName={} offset={} p1885titatext_len={} setval.len={} cplen={}",
				f.name, f.offset ,this.p1885titatext_len,srcValue.length,cplen);
			System.arraycopy(srcValue, 0, this.p1885titatextary, f.offset, cplen);
			return true;
		} else
			return false;
	}

	public int getP1885TitatextLen() {
		return this.p1885titatext_len;
	}
	public byte[] getP1885Titatext() {
		return p1885titatextary;
	}
	public boolean initP1885TitaTEXT(byte initValue) {
		if (p1885titatext_len > 0) {
			p1885titatextary = new byte[p1885titatext_len];
			Arrays.fill(p1885titatextary, initValue);
			return true;
		} else
			return false;
	}

	private List<Field> p1885titatext = new ArrayList<Field>();
	private Map<String, Integer> p1885titatextname = new HashMap<String, Integer>();
	private int p1885titatext_len = 0;
	private byte[] p1885titatextary = null;

	public P1885TEXT() {
		log.debug("p1885titatext_lens items=" + p1885titatext_lens.length);
		int id = 0;
		for (int l : p1885titatext_lens) {
			Field f = new Field();
//         log.debug(String.format("%d [%s] offset %d len %d",id, p1885titatext_names[id], p1885titatext_len, l));
			f.setData(p1885titatext_names[id], p1885titatext_len, l);
			p1885titatextname.put(p1885titatext_names[id], id);
			id += 1;
			p1885titatext.add(f);
			p1885titatext_len += l;
		}
		log.debug("p1885titatext_lens={}", p1885titatext_len);
	}
	/*public static void main(String[] args) throws Exception {
		boolean rtn;
		P1885TEXT tl = new P1885TEXT();
		rtn = tl.initP1885TitaTEXT((byte) '0');
		log.debug("tl.initP1885TitaTEXT rtn={}", rtn);
		byte[] result = tl.getP1885Titatext();
		// System.out.println("2--->" + Arrays.toString(result) + ":[" + new
		// String(result) + "]");
		log.debug("2--->[{}]len={}", new String(result), tl.getP1885TitatextLen());
	}*/
}
