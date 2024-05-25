package com.systex.sysgateii.autosvr.telegram;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
/**
 * TITATel
 * MatsudairaSyume
 *  20200123
 */
public class TITATel {
	private static Logger log = LoggerFactory.getLogger(TITATel.class);
    private int  BRNO = 3;
    private int  WSNO = 4;
    //20221123 private int  FTXSEQ = 3;   // 20051031 , ADD , 20060120 FOR UONLINE TEST , BRANCH TXSEQ IS 5 DIGITS , C11 WILL SYNC WITH HOST SCTL
    private int  TXSEQ = 8; //20221123 5->8
    private int  ORGKIN = 3;   // 20051031 , ADD
    private int  ORGTRM = 4;   // 20051031 , ADD
    private int  ORGTNO = 8;   // 20051031 , ADD
    private int  TRANCD = 2;
    private int  FWSTYPE = 1;  // 20051031 , ADD
    private int  WSTYPE = 1;
    private int  TLRNO = 2;
    private int  APTYPE = 1;
    private int  APCODE = 2;
    private int  STXNO = 2;    // 20051031 , ADD
    private int  RSTINQ = 1;   // TIMEOUT INQUERY
    private int  PTYPE = 1;
    private int  DSCPT = 5;
    private int  ACTNO = 14;
    private int  BACTNO = 2;   // 20051031 , ADD
    private int  TXTYPE = 2;
    private int  CRDB = 1;
    private int  SPCD = 1;
    private int  NBCD = 1;
    private int  HCODE = 1;
    private int  TRNMOD = 1;
    private int  SBTMOD = 1;
    private int  CURCD = 2;
    private int  TXAMT = 13;
    private int  FEPDD = 2;    // 20051031 , ADD
    private int  PREDO = 1;    // 20051031 , ADD
    private int  CALDY = 8;    // 20051031 , ADD
    private int  CALTM = 8;    // 20051031 , ADD
    private int  TOTAFG = 1;   // 20051031 , ADD
    private int  WARNFG = 1;   // 20051031 , ADD
    private int  SUPNO = 2;    // 20051031 , ADD
    private int  MTTPSEQ = 2;  // 20051031 , ADD
    private int  PSEUDO = 1;   // 20051031 , ADD
    private int  MKEY = 16;    // 20051031 , ADD
    private int  BKNO = 3;     // 20051031 , ADD
    private int  VER = 2;      // 20051031 , ADD
    private int  ACBRNO = 3;   // 20051031 , ADD
    private int  SECNO = 2;    // 20051031 , ADD
    private int  IBFFG = 1;    // 20051031 , ADD
    private int  JOBNO = 3;
    private int  FILLER = 1;   // 20051031 , ADD


    private String  brno = "brno";
    private String  wsno = "wsno";
    //20221123 private String  ftxseq = "ftxseq";    
    private String  txseq = "txseq";
    private String  orgkin = "orgkin";    
    private String  orgtrm = "orgtrm";    
    private String  orgtno = "orgtno";    
    private String  trancd = "trancd";
    private String  fwstype = "fwstype"; 
    private String  wstype = "wstype";
    private String  titalrno = "titalrno";
    private String  aptype = "aptype";
    private String  apcode = "apcode";
    private String  stxno  = "stxno";     
    private String  rstinq = "rstinq";    
    private String  ptype  = "ptype";
    private String  dscpt  = "dscpt";
    private String  actno  = "actno";
    private String  bactno = "bactno";    
    private String  txtype = "txtype";
    private String  crdb  = "crdb";
    private String  spcd  = "spcd";
    private String  nbcd  = "nbcd";
    private String  hcode = "hcode";
    private String  trnmod = "trnmod";
    private String  sbtmod = "sbtmod";
    private String  curcd  = "curcd";
    private String  txamt  = "txamt";
    private String  fepdd  = "fepdd";     
    private String  predo  = "predo";     
    private String  caldy  = "caldy";     
    private String  caltm  = "caltm";     
    private String  totafg = "totafg";    
    private String  warnfg = "warnfg";
    private String  supno = "supno";    
    private String  mttpseq = "mttpseq";
    private String  pseudo  = "pseudo";   
    private String  mkey    = "mkey";      
    private String  bkno    = "bkno";      
    private String  ver     = "ver";        
    private String  acbrno = "acbrno";    
    private String  secno  = "secno";     
    private String  ibffg  = "ibffg";     
    private String  jobno  = "jobno";
    private String  filler = "filler";    

    private int titalabel_lens[] = {
        BRNO,
        WSNO,
        //20221123 FTXSEQ,
        TXSEQ,
        ORGKIN,
        ORGTRM,
        ORGTNO,
        TRANCD,
        FWSTYPE,
        WSTYPE,
        TLRNO,
        APTYPE,
        APCODE,
        STXNO,
        RSTINQ,
        PTYPE,
        DSCPT,
        ACTNO,
        BACTNO,
        TXTYPE,
        CRDB,
        SPCD,
        NBCD,
        HCODE,
        TRNMOD,
        SBTMOD,
        CURCD,
        TXAMT,
        FEPDD,
        PREDO,
        CALDY,
        CALTM,
        TOTAFG,
        WARNFG,
        SUPNO,
        MTTPSEQ,
        PSEUDO,
        MKEY,
        BKNO,
        VER,
        ACBRNO,
        SECNO,
        IBFFG,
        JOBNO,
        FILLER
    };

    private String titalabel_names[] = {
        brno,
        wsno,
        //20221123 ftxseq,
        txseq,
        orgkin,
        orgtrm,
        orgtno,
        trancd,
        fwstype,
        wstype,
        titalrno,
        aptype,
        apcode,
        stxno,
        rstinq,
        ptype,
        dscpt,
        actno,
        bactno,
        txtype,
        crdb,
        spcd,
        nbcd,
        hcode,
        trnmod,
        sbtmod,
        curcd,
        txamt,
        fepdd,
        predo,
        caldy,
        caltm,
        totafg,
        warnfg,
        supno,
        mttpseq,
        pseudo,
        mkey,
        bkno,
        ver,
        acbrno,
        secno,
        ibffg,
        jobno,
        filler
    };
    public void setValueLtoRfill(String fieldN, String setsV, byte fillV) throws Exception {
        setValueLtoRfill(fieldN, setsV.getBytes(), fillV);
    }
    public void setValueLtoRfill(String fieldN, byte[] setbV, byte fillV) throws Exception {
        Field f = titalabel.get(titalname.get(fieldN));
        byte[] ntb = null;
        if (setbV.length < f.len) {
            ntb = new byte[f.len];
            Arrays.fill(ntb, fillV);
            System.arraycopy(setbV, 0, ntb, 0, setbV.length);
        } else
            ntb = setbV;
        setValue(fieldN, ntb);
      //20240517 Code Correctness: Call to System.gc()  System.gc();
    }
    public void setValueRtoLfill(String fieldN, String setsV, byte fillV) throws Exception {
        setValueRtoLfill(fieldN, setsV.getBytes(), fillV);
    }
    public void setValueRtoLfill(String fieldN, byte[] setbV, byte fillV) throws Exception {
        Field f = titalabel.get(titalname.get(fieldN));
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
        Field f = titalabel.get(titalname.get(fieldN));
        System.arraycopy(setbV, 0, titalabelary, f.offset, f.len);
    }
	public byte[] getValue(String fieldN) throws Exception {
		byte[] rtn = null;
		Field f = titalabel.get(titalname.get(fieldN));
		rtn = new byte[f.len];
		System.arraycopy(titalabelary, f.offset, rtn, 0, f.len);
		return rtn;
	}

    private List<Field> titalabel = new ArrayList<Field>();
    private Map<String, Integer> titalname = new HashMap<String, Integer>();
    private int titalabel_len = 0;
    private byte[] titalabelary = null;

    public TITATel() {
        log.debug("titalabel_lens items=" + titalabel_lens.length);
        int id = 0;
        for (int l: titalabel_lens) {
            Field f = new Field();
//            log.debug(String.format("%d offset %d len %d",id, titalabel_len, l));
            f.setData(titalabel_names[id], titalabel_len, l);
            titalname.put(titalabel_names[id], id);
            id += 1;
            titalabel.add(f);
            titalabel_len += l;
        }
      log.debug("titalabel_lens={}", titalabel_len);

    }

    public boolean initTitaLabel(byte initValue) {
        if (titalabel_len > 0) {
            titalabelary = new byte[titalabel_len];
            Arrays.fill(titalabelary, initValue);
            return true;
        } else
            return false;
    }
    public byte[] getTitalabel() {
        return titalabelary;
    }
    /*********************************************************
     *	FilterMsr()   : Convert less mark to minus           *
     *   function      : 磁條內容過濾
     *   parameter 1   : data                                 *
     *   parameter 2   : len                                  *
     *   parameter 3   : check char                           *
     *   parameter 4   : replace char                         *
     *   return_code   : data  - NORMAL                       *
     *                   no change data - ERROR               *
     *********************************************************/
    public String FilterMsr(String data, char check, char replase) {
        char[] rtn = data.toCharArray();
        for (int i = 0; i < rtn.length; i++)
            if (rtn[i] == check)
                rtn[i] = replase;
        return new String(rtn);
    }

    /*******************************************************
     *	ChkCrdb()     : Check Txamt                          *
     *   function      : 判斷借貸
     *   parameter 1   : txmat                                *
     *   return_code   : 0  - 負                              *
     *                   1  - 正                              *
     *********************************************************/

    public int ChkCrdb (String txmat) {
        for (char c: txmat.toCharArray())
            if (c == '-')
                return 0;
        return 1;
    }
    public byte[] mkTITAmsg (byte[] label, byte[] text) throws Exception {
        if (label == null || text == null)
            throw new Exception("tital_label or tita_text null");
        byte[] titamsg = new byte[label.length + text.length];
        System.arraycopy(label, 0, titamsg, 0, label.length);
        System.arraycopy(text, 0, titamsg, label.length, text.length);
        return titamsg;
    }
    /*Poor Style: Value Never Read
    public static void main(String[] args) throws Exception {
        int iLine = 0;
        int begin = 0;
        int totCnt = 0;
        boolean rtn;
        boolean p0080titatextrtn;
        byte[] fepdd = new byte[2];
        String marbal = "00000021040800";
        String bkseq = "0";
        String cline = "13";
//        String cpage = "07";
        iLine = Integer.parseInt(cline.trim());
        begin = Integer.parseInt("000".trim());
        Arrays.fill(fepdd, (byte)' ');
        TITATel tital = new TITATel();
        rtn = tital.initTitaLabel((byte)'0');
        log.debug("tital.initTitaLabel rtn={}",rtn);
        P0080TEXT p0080text = new P0080TEXT();
        p0080titatextrtn = p0080text.initP0080TitaTEXT((byte)'0');
        tital.setValue("brno", "983");
        tital.setValue("wsno", "0403");
        tital.setValue("txseq", "12345");
        tital.setValue("trancd", "CB");
        tital.setValue("wstype", "0");
        tital.setValue("titalrno", "00");
        tital.setValueLtoRfill("txtype", " ", (byte)' ');
        tital.setValue("spcd", "0");
        tital.setValue("nbcd", "0");
        tital.setValue("hcode", "0");
        tital.setValue("trnmod", "0");
        tital.setValue("sbtmod", "0");
        tital.setValue("curcd", "00");
        tital.setValue("pseudo", "1");
        if (!new String(fepdd).equals("  "))
            tital.setValue("fepdd", fepdd);
        tital.setValue("aptype", "P");
        tital.setValue("apcode", "00");
        tital.setValue("stxno", "80");
        tital.setValue("dscpt", "S80  ");
        tital.setValueLtoRfill("actno", "983004149692", (byte)' ');
        if (tital.ChkCrdb(marbal) > 0)
            tital.setValue("crdb", "1");
        else
            tital.setValue("crdb", "0");
        String sm = marbal.substring(1);
        sm = tital.FilterMsr(sm, '-', '0');
        tital.setValue("txamt",sm);
        tital.setValue("ver","02");
        p0080text.setValueRtoLfill("pbcnt", "999", (byte)'0');
        p0080text.setValue("bkseq", bkseq);
        //要求筆數(若該頁剩餘筆數 < 6，則為"剩餘筆數")
        if ((iLine - 1 + begin) + 6 > 24) {
        	   int reqcnt = 24 - (iLine - 1 + begin);
            System.out.println("TxFlow : DataINQ() -- reqcnt =" + reqcnt);
            p0080text.setValueRtoLfill("reqcnt", Integer.toString(reqcnt), (byte)'0');
            }
        else
          {
            //若剩餘要求之未登摺筆數 < 6，則為"剩餘之未登摺筆數"，否則為6
            if (totCnt >0 && begin + 6 > totCnt)
                p0080text.setValueRtoLfill("reqcnt", Integer.toString(totCnt - begin), (byte)'0');
            else
                p0080text.setValueRtoLfill("reqcnt", Integer.toString(6), (byte)'0');
           }
        if (begin == 0)
            p0080text.setValueRtoLfill("begin", Integer.toString(1), (byte)'0');
        else
            p0080text.setValueRtoLfill("begin", Integer.toString(begin + 1), (byte)'0');

        byte[] p0080tita = tital.mkTITAmsg(tital.getTitalabel(), p0080text.getP0080Titatext());
        log.debug("4--->[{}] len={}",new String(p0080tita), p0080tita.length);

    }*/
}

