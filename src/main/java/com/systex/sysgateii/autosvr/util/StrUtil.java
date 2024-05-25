package com.systex.sysgateii.autosvr.util;

import java.nio.charset.Charset;
//20210702 MatsudairasyuMe import for Log Forging
import java.text.Normalizer;
import java.text.NumberFormat;
import java.text.ParsePosition;
//20210702 MatsudairasyuMe import for Log Forging
import java.util.ArrayList;
import java.util.Arrays;
//20210702 MatsudairasyuMe import for Log Forging
import java.util.List;
//20210802 MatsudairaSyuMe import for Log forging
//20240223 MatsudairaSyuMe mark up don't use OWASP ESAPI
//import org.owasp.esapi.ESAPI;
//----
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

		return new String(newArr, Charset.forName("UTF-8"));
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

		return new String(newArr, Charset.forName("UTF-8"));
	}

	//20210702 MatsudairasyuMe function for Log Forging
/*    public static String convertValidLog(String log){
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
    }*/
	//20210803 MatsudairaSyuMe function use ESPI for Log Forging
	//20240223 MatsudairaSyuMe change to use 
	@SuppressWarnings("deprecation")
	public static String convertValidLog(String message){
		message = message.replace('\n', '_').replace('\r', '_').replace('\t', '_');
		/*
		try {
			//message = ESAPI.encoder().encodeForHTML(message);
		} catch(Exception e) {
			;//e.printStackTrace();
		}
		return message;
		*/
		return org.apache.commons.lang3.StringEscapeUtils.escapeXml(message);
    }
	//----
    //20210723 MatshdairaSyuMe add for Path Manipulation
	public static String cleanString(String aString) {
		if (aString == null) return null;
		String cleanString = "";
		for (int i = 0; i < aString.length(); ++i) {
			cleanString += cleanChar(aString.charAt(i));
		}
		return cleanString;
	}
	private static char cleanChar(char aChar) {
		// 0 - 9
		for (int i = 48; i < 58; ++i) {
			if (aChar == i) return (char) i;
		}
		// 'A' - 'Z'
		for (int i = 65; i < 91; ++i) {
			if (aChar == i) return (char) i;
		}
		// 'a' - 'z'
		for (int i = 97; i < 123; ++i) {
			if (aChar == i) return (char) i;
		}
    // other valid characters
        /*
       switch (aChar) {
            case '/':
                return '/';
            case '.':
                return '.';
            case '-':
                return '-';
            case '_':
                return '_';
            case ' ':
                return ' ';
        }
        */
       return '_';
    }

	public static void main(String args[]) {
		System.out.println(padSpace("12345678901", 10) + "*");
	}

}
