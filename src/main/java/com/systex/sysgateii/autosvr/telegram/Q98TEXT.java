package com.systex.sysgateii.autosvr.telegram;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Q98TEXT
 * MatsudairaSyume
 *  2020310
 */
public class Q98TEXT {
	private static Logger log = LoggerFactory.getLogger(Q98TEXT.class);
	// Q98 TITA_TEXT
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
	
   private int  NBCNT  = 3;   //!< 刪除筆數
   private int  TXDAY  = 8;   //!< 交易日期
   private int  TXSEQ  = 6;   //!< 交易序號
   private int  PBBAL  = 13;  //!< 存摺餘額
   private int  PBCOL  = 2;   //!< 行次
   private int  PBPAGE = 2;   //!< 頁次

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

   private String  nbcnt   = "nbcnt";   //!< 刪除筆數
   private String  txday  = "txday";   //!< 交易日期
   private String  txseq  = "txseq";   //!< 交易序號
   private String  pbbal  = "pbbal";  //!< 存摺餘額
   private String  pbcol  = "pbcol";   //!< 行次
   private String  pbpage = "pbpage";   //!< 頁次

	private int q98titatext_lens[] = {
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

			 NBCNT,    //!< 刪除筆數
			 TXDAY,    //!< 交易日期
			 TXSEQ,    //!< 交易序號
			 PBBAL,   //!< 存摺餘額
			 PBCOL,    //!< 行次
			 PBPAGE    //!< 頁次
	};
	private String q98titatext_names[] = {
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
			 
			 nbcnt,    //!< 刪除筆數
			 txday,    //!< 交易日期
			 txseq,    //!< 交易序號
			 pbbal,   //!< 存摺餘額
			 pbcol,    //!< 行次
			 pbpage    //!< 頁次
	};

	public void setValueLtoRfill(String fieldN, String setsV, byte fillV) throws Exception {
		setValueLtoRfill(fieldN, setsV.getBytes(), fillV);
	}
	
	public void setValueLtoRfill(String fieldN, byte[] setbV, byte fillV) throws Exception {
		Field f = q98titatext.get(q98titatextname.get(fieldN));
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
		Field f = q98titatext.get(q98titatextname.get(fieldN));
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
		Field f = q98titatext.get(q98titatextname.get(fieldN));
//		log.debug(fieldN + ":");
		System.arraycopy(setbV, 0, q98titatextary, f.offset, f.len);
	}
	public boolean appendTitaText(String fieldN, byte[] srcValue) {
		Field f = q98titatext.get(q98titatextname.get(fieldN));
		if (this.q98titatext_len > 0 && srcValue != null && srcValue.length > 0) {
			if (this.q98titatextary == null || this.q98titatextary.length == 0) {
					this.q98titatextary = new byte[this.q98titatext_len];
			}
			int cplen = this.q98titatext_len - f.offset;
			if (srcValue.length < cplen)
				cplen = srcValue.length;
			log.debug("fieldName={} offset={} q98titatext_len={} setval.len={} cplen={}",
				f.name, f.offset ,this.q98titatext_len,srcValue.length,cplen);
			System.arraycopy(srcValue, 0, this.q98titatextary, f.offset, cplen);
			return true;
		} else
			return false;
	}

	public int getQ98TitatextLen() {
		return this.q98titatext_len;
	}

	public byte[] getQ98Titatext() {
		return q98titatextary;
	}

	public boolean initQ98TitaTEXT(byte initValue) {
		if (q98titatext_len > 0) {
			q98titatextary = new byte[q98titatext_len];
			Arrays.fill(q98titatextary, initValue);
			return true;
		} else
			return false;
	}

	private List<Field> q98titatext = new ArrayList<Field>();
	private Map<String, Integer> q98titatextname = new HashMap<String, Integer>();
	private int q98titatext_len = 0;
	private byte[] q98titatextary = null;

	public Q98TEXT() {
		log.debug("q98titatext_lens items=" + q98titatext_lens.length);
		int id = 0;
		for (int l : q98titatext_lens) {
			Field f = new Field();
//         log.debug(String.format("%d [%s] offset %d len %d",id, q98titatext_names[id], q98titatext_len, l));
			f.setData(q98titatext_names[id], q98titatext_len, l);
			q98titatextname.put(q98titatext_names[id], id);
			id += 1;
			q98titatext.add(f);
			q98titatext_len += l;
		}
		log.debug("q98titatext_lens={}", q98titatext_len);
	}
	/*public static void main(String[] args) throws Exception {
		boolean rtn;
		Q98TEXT tl = new Q98TEXT();
		rtn = tl.initQ98TitaTEXT((byte) '0');
		log.debug("tl.initQ98TitaTEXT rtn={}", rtn);
		byte[] result = tl.getQ98Titatext();
		// System.out.println("2--->" + Arrays.toString(result) + ":[" + new
		// String(result) + "]");
		log.debug("2--->[{}]len={}", new String(result), tl.getQ98TitatextLen());
	}*/

}
