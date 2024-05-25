package com.systex.sysgateii.autosvr.telegram;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * C0099TEXT
 * MatsudairaSyume
 *  20200317
 */
public class C0099TEXT {
	private static Logger log = LoggerFactory.getLogger(C0099TEXT.class);
	// C0099 TOTA_TEXT
	private int CALDY  = 8;  //!< 日曆日
	private int STNDTM = 6;  //!< 中心時間
	private int TBSDY  = 8;  //!< 本營業日
	
	private String caldy  = "caldy";  //!< 日曆日
	private String stndtm = "stndtm";  //!< 中心時間
	private String tbsdy  = "tbsdy";  //!< 本營業日
	
	private int c0099totaheadtext_lens[] = {
			CALDY,
			STNDTM,
			TBSDY
	};
	private String c0099totaheadtext_names[] = {
			caldy,
			stndtm,
			tbsdy
	};

	public int getC0099TotaheadtextLen() {
		return this.c0099totaheadtext_len;
	}

	public byte[] getC0099Totaheadtext() {

		return c0099totaheadtextary;
	}

	public boolean initC0099TotaheadTEXT(byte initValue) {
		if (c0099totaheadtext_len > 0) {
			c0099totaheadtextary = new byte[c0099totaheadtext_len];
			Arrays.fill(c0099totaheadtextary, initValue);
			return true;
		} else
			return false;
	}

	public boolean copyTotaHead(byte[] srcValue) {
		if (this.c0099totaheadtext_len > 0 && srcValue != null && srcValue.length > 0) {
			this.c0099totaheadtextary = new byte[this.c0099totaheadtext_len];
			System.arraycopy(srcValue, 0, this.c0099totaheadtextary, 0, this.c0099totaheadtext_len);
			return true;
		} else
			return false;
	}

	public byte[] getHeadValue(String fieldN) throws Exception {
		byte[] rtn = null;
		Field f = c0099totaheadtext.get(c0099totaheadtextname.get(fieldN));
		rtn = new byte[f.len];
		System.arraycopy(c0099totaheadtextary, f.offset, rtn, 0, f.len);
		return rtn;
	}

	private List<Field> c0099totaheadtext = new ArrayList<Field>();
	private Map<String, Integer> c0099totaheadtextname = new HashMap<String, Integer>();
	private int c0099totaheadtext_len = 0;
	private byte[] c0099totaheadtextary = null;

	public C0099TEXT() {
		log.debug("c0099totaheadtext_lens items=" + c0099totaheadtext_lens.length);
		int id = 0;
		for (int l : c0099totaheadtext_lens) {
			Field f = new Field();
//            log.debug(String.format("%d offset %d len %d",id, c0099titatext_len, l));
			f.setData(c0099totaheadtext_names[id], c0099totaheadtext_len, l);
			c0099totaheadtextname.put(c0099totaheadtext_names[id], id);
			id += 1;
			c0099totaheadtext.add(f);
			c0099totaheadtext_len += l;
		}
		log.debug("c0099totaheadtext_lens={}", c0099totaheadtext_len);
	}
/*
	public static void main(String[] args) throws Exception {
		//20240510 Poor Style: Value Never Read boolean rtn;
		C0099TEXT tl = new C0099TEXT();
		tl.initC0099TotaheadTEXT((byte) '0');//20240510 Poor Style: Value Never Read take out rtn 
		byte[] result = tl.getC0099Totaheadtext();
		// System.out.println("2--->" + Arrays.toString(result) + ":[" + new
		// String(result) + "]");
		log.debug("3--->[{}] len={}", new String(result), tl.getC0099TotaheadtextLen());
	}
*/
}
