package com.systex.sysgateii.autosvr.util;

//20210702 MatsudairasyuMe import for Log Forging
import java.text.Normalizer;
import java.text.NumberFormat;
import java.text.ParsePosition;
//20210702 MatsudairasyuMe import for Log Forging
import java.util.ArrayList;
import java.util.Arrays;
//20210702 MatsudairasyuMe import for Log Forging
import java.util.List;

public class StrUtil {
	public static boolean isNotEmpty(String s) {
		if (s == null || s.isEmpty()) {
			return false;
		}
		return true;
	}

	public static boolean isEmpty(String s) {
		return !isNotEmpty(s);
	}

	public static boolean isNumeric(String s) {
		NumberFormat formatter = NumberFormat.getInstance();
		ParsePosition pos = new ParsePosition(0);
		formatter.parse(s, pos);
		return s.length() == pos.getIndex();
	}

	public static String padOnRight(String org, byte pad, int newLength) {
		if (org.length() > newLength) {
			return org;
		}

		byte[] newArr = new byte[newLength];

		Arrays.fill(newArr, pad);

		byte[] orgByteArr = org.getBytes();
		System.arraycopy(orgByteArr, 0, newArr, 0, orgByteArr.length);

		return new String(newArr);
	}

	public static String padSpace(String org, int len) {
		String org_text = padOnRight(org, (byte) 0x20, len);
		if (org_text.length() > len) {
			org_text = org_text.substring(0, len);
		}
		return org_text;
	}

	/**
	 * 左補零
	 * 
	 * @param org
	 * @param newLength
	 * @return
	 */
	public static String padZeroLeft(String org, int newLength) {
		return padOnLeft(org, (byte) 0x30, newLength);
	}

	/**
	 * 左補滿 當 newLength 的值小於 輸入字串長度時，回傳原有字串
	 * 
	 * @param org       原有的字串
	 * @param pad       要補滿的字元(byte)
	 * @param newLength 長度
	 * @return 補滿的字串
	 */
	public static String padOnLeft(String org, byte pad, int newLength) {
		if (org.length() > newLength) {
			return org;
		}

		byte[] newArr = new byte[newLength];

		Arrays.fill(newArr, pad);

		byte[] orgByteArr = org.getBytes();
		System.arraycopy(orgByteArr, 0, newArr, newArr.length - orgByteArr.length, orgByteArr.length);

		return new String(newArr);
	}

	//20210702 MatsudairasyuMe function for Log Forging
    public static String convertValidLog(String log){
        List<String> list = new ArrayList<String>();
        list.add("%0d");
        list.add("\r");
        list.add("%0a");
        list.add("\n");

        // normalize the log content
        String encode = Normalizer.normalize(log, Normalizer.Form.NFKC);
        for(String toReplaceStr : list)
            encode = encode.replace(toReplaceStr, "");
        return encode;
    }

	public static void main(String args[]) {
		System.out.println(padSpace("12345678901", 10) + "*");
	}

}
