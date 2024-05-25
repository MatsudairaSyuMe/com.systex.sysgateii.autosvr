package com.systex.sysgateii.autosvr.telegram;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Arrays;

/**
 * TOTATel MatsudairaSyume 20200131
 */
public class TOTATel {
	private static Logger log = LoggerFactory.getLogger(TOTATel.class);
	private int BRNO = 3;
	// private int FWSNO = 2; // 20051031 , add
	// private int WSNO = 2;
	// 20061219 , 2位放大成4位
	private int WSNO = 4;
	//20221123 private int FTXSEQ = 3; // 20051031 , add
	private int TXSEQ = 8; //20221123 5->8
	private int TRANCD = 2;
	private int FWSTYPE = 1; // 20051031 , add
	private int WSTYPE = 1;
	private int TXTSK = 1;
	private int MSGEND = 1;
	private int TOTASEQ = 4; // 20051031 , add
	private int APTYPE = 1; // 20051031 , add
	private int MTYPE = 1;
	private int MSGNO = 3;
	private int MSGLNG = 4; // 20051031 , fix
	private int WARNCNT = 1; // 20051125 , add

	private String brno = "brno";
	// private String fwsno = ; // 20051031 , add
	// private String wsno = ;
	// 20061219 , 2位放大成4位
	private String wsno = "wsno";
	//20221123 private String ftxseq = "ftxseq"; // 20051031 , add
	private String txseq = "txseq";
	private String trancd = "trancd";
	private String fwstype = "fwstype"; // 20051031 , add
	private String wstype = "wstype";
	private String txtsk = "txtsk";
	private String msgend = "msgend";
	private String totaseq = "totaseq"; // 20051031 , add
	private String aptype = "aptype"; // 20051031 , add
	private String mtype = "mtype";
	private String msgno = "msgno";
	private String msglng = "msglng"; // 20051031 , fix
	private String warncnt = "warncnt"; // 20051125 , add

	private int totalabel_lens[] = { BRNO, WSNO, //20221123 FTXSEQ, // 20051031 , add
			TXSEQ, TRANCD, FWSTYPE, // 20051031 , add
			WSTYPE, TXTSK, MSGEND, TOTASEQ, // 20051031 , add
			APTYPE, // 20051031 , add
			MTYPE, MSGNO, MSGLNG, // 20051031 , fix
			WARNCNT // 20051125 , add
	};

	private String totalabel_names[] = { brno, wsno, /* 20221123 ftxseq,*/ txseq, trancd, fwstype, wstype, txtsk, msgend, totaseq,
			aptype, mtype, msgno, msglng, warncnt };

	public void setValueLtoRfill(String fieldN, String setsV, byte fillV) throws Exception {
		setValueLtoRfill(fieldN, setsV.getBytes(), fillV);
	}

	public void setValueLtoRfill(String fieldN, byte[] setbV, byte fillV) throws Exception {
		Field f = totalabel.get(totalname.get(fieldN));
		byte[] ntb = null;
		if (setbV.length < f.len) {
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
		Field f = totalabel.get(totalname.get(fieldN));
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
		Field f = totalabel.get(totalname.get(fieldN));
		System.arraycopy(setbV, 0, totalabelary, f.offset, f.len);
	}

	public byte[] getValue(String fieldN) throws Exception {
		byte[] rtn = null;
		Field f = totalabel.get(totalname.get(fieldN));
		rtn = new byte[f.len];
		System.arraycopy(totalabelary, f.offset, rtn, 0, f.len);
		return rtn;
	}

	private List<Field> totalabel = new ArrayList<Field>();
	private Map<String, Integer> totalname = new HashMap<String, Integer>();
	private int totalabel_len = 0;
	private byte[] totalabelary = null;

	public TOTATel() {
		log.debug("totalabel_lens items=" + totalabel_lens.length);
		this.totalabel_len =0;
		int id = 0;
		for (int l : totalabel_lens) {
			Field f = new Field();
//            log.debug(String.format("%d offset %d len %d",id, totalabel_len, l));
			f.setData(totalabel_names[id], totalabel_len, l);
			totalname.put(totalabel_names[id], id);
			id += 1;
			totalabel.add(f);
			totalabel_len += l;
		}
		log.debug("totalabel_lens={}", totalabel_len);
	}

	public boolean initTotaLabel(byte initValue) {
		if (totalabel_len > 0) {
			totalabelary = new byte[totalabel_len];
			Arrays.fill(totalabelary, initValue);
			return true;
		} else
			return false;
	}

	public boolean copyTotaLabel(byte[] srcValue) {
		if (this.totalabel_len > 0 && srcValue != null && srcValue.length > 0) {
			if (this.totalabelary == null || this.totalabelary.length == 0)
				this.totalabelary = new byte[this.totalabel_len];
			System.arraycopy(srcValue, 0, this.totalabelary, 0, this.totalabel_len);
			return true;
		} else
			return false;
	}

	public byte[] getTotalabel() {
		return totalabelary;
	}

	public byte[] mkTOTAmsg(byte[] label, byte[] text) throws Exception {
		if (label == null || text == null)
			throw new Exception("total_label or tota_text null");
		byte[] totamsg = new byte[label.length + text.length];
		System.arraycopy(label, 0, totamsg, 0, label.length);
		System.arraycopy(text, 0, totamsg, label.length, text.length);
		return totamsg;
	}
	
	public int getTotalLabelLen() {
		return this.totalabel_len;
	}
/*Poor Style: Value Never Read
	public static void main(String[] args) throws Exception {
		boolean rtn;
		String msgid = "";
		String cMsg = "";
		String totabasicerrmsg1 = "983043500000105DU0011    PE0860077 ";
		String totatexterrmsg1 = "\u0004登摺金額超過限制\u0007                        \u0003";
		TOTATel total = new TOTATel();
		rtn = total.copyTotaLabel(totabasicerrmsg1.getBytes());
		log.debug("total.copyTotaLabel rtn={} mtype={} msgno={}", rtn, new String(total.getValue("mtype")),
				new String(total.getValue("msgno")));
//		P0080TEXT p0080text = new P0080TEXT();
//		boolean p0080titatextrtn = p0080text.initP0080TitaTEXT((byte)'0');
		msgid = new String(total.getValue("mtype")) + new String(total.getValue("msgno"));
//		byte[] totatext = totatexterrmsg1.getBytes(Charset.defaultCharset());
		byte[] p0080tota = total.mkTOTAmsg(total.getTotalabel(), totatexterrmsg1.getBytes(Charset.defaultCharset()));
		log.debug("4--->[{}] len={}", new String(p0080tota), p0080tota.length);
		byte[] totatext = Arrays.copyOfRange(p0080tota, total.getTotalLabelLen(), p0080tota.length);
		log.debug("5--->totatexterrmsg1 len={} {} ", totatext.length, Charset.defaultCharset());
		String mt = new String(total.getValue("mtype"));
		if (mt.equals("E") || mt.equals("A") || mt.equals("X")) {
			if (total.getValue("mtype").equals("A"))
				msgid = "E" + new String(total.getValue("msgno"));
			for (int i = 0; i < totatext.length; i++)
				if (totatext[i] == 0x7 || totatext[i] == 0x4 || totatext[i] == 0x3)
					totatext[i] = 0x20;
			cMsg = "-" + new String(totatext).trim();
			log.debug("cMsg=[{}] {}", cMsg, Arrays.toString(totatext));
		}
	}*/
}
