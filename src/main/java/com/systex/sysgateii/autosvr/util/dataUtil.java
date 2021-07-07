package com.systex.sysgateii.autosvr.util;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class dataUtil {
	private static Logger log = LoggerFactory.getLogger(dataUtil.class);
	private final static String STAR = "**********************";

	public int ArraySearchIndexOf(final byte[] outerArray, final byte[] smallerArray) {
		for (int i = 0; i < outerArray.length - smallerArray.length + 1; ++i) {
			boolean found = true;
			for (int j = 0; j < smallerArray.length; ++j) {
				if (outerArray[i + j] != smallerArray[j]) {
					found = false;
					break;
				}
			}
			if (found)
				return i;
		}
		return -1;
	}

	public static byte[] remove03(byte[] source) {
		if (source[source.length - 1] == 0x03) {
			source = ArrayUtils.subarray(source, 0, source.length - 1);
			log.debug("remove03");
		}
		return source;
	}

	public static int fromByteArray(byte[] bytes) {
		int r = 0;
		for (byte b : bytes)
			r = (r * 100) + ((((b >> 4) & 0xf) * 10 + (b & 0xf)));
		return r;
	}

	public static byte[] to3ByteArray(int l) {
		byte[] rtn = new byte[3];
		int tl = l;
		byte b1 = (byte) 0x0;
		byte b2 = (byte) 0x0;
		for (int i = rtn.length - 1; i >= 0; --i) {
			b1 = (byte) (tl % 10);
			tl = tl / 10;
			b2 = (byte) (tl % 10);
			tl = tl / 10;
			rtn[i] = (byte) (((b2 << 4) & 0xf0) | (b1 & 0xf));
		}
		return rtn;
	}

	/*********************************************************
	*  rfmtdbl()   : 金額格式化                              *
	*  parameter 1 : input data                              *
	*  parameter 2 : input data format                       *
	*  return_code : ouput data buff (參考TPCOMM) 2008.01.25*
	*********************************************************/
	public static String rfmtdbl(double idbl, String ifmt) {
		String STAR = "**********************";
		boolean Ldollarsign = false;
		boolean Lstarsign = false;
		boolean Lminus = false;
		int outlen = ifmt.length();
		String Lifmt = "";
		String Lfmt = "";
		Lifmt = ifmt.trim();
		double Lidbl;

		Lidbl = idbl;

		if (Lidbl < 0)
			Lidbl *= -1;

		char ckc[] = Lifmt.toCharArray();
		for (int i = 0; i < ckc.length; i++) {
			if (ckc[i] == '$') {
				Ldollarsign = true;
				ckc[i] = 'Z';
			} else if (ckc[i] == '*') {
				Lstarsign = true;
				ckc[i] = 'Z';
			} else if (ckc[i] == '-') {
				Lminus = true;
				ckc[i] = 'Z';
			}
		}
		Lfmt = new String(ckc);
		String[] pary = Lfmt.trim().split("\\.");

		if (pary.length == 0) {
			return STAR.substring(0, outlen);
		}
		String p1 = "", p2 = "";
		p1 = pary[0];
		if (pary.length == 1 && pary[0].length() == 0)
			p1 = " ";
		if (pary.length > 1)
			p2 = pary[1];

		int Ldec = 0;
		Ldec = p1.length();
		int Ldot = 0;
		if (p2.length() != 0)
			Ldot = p2.length();
		else
			Ldot = 0;
		int Lcommacnt = 0;
		for (char c : p1.toCharArray()) {
			if (c == ',')
				Lcommacnt++;
		}

		String Ltfmt = "";
		if (Ldot == 0)
			Ltfmt = String.format("%%%d.%df", Ldec - Lcommacnt, Ldot);
		else
			Ltfmt = String.format("%%%d.%df", Ldec - Lcommacnt + Ldot + 1, Ldot);

		String Ltbuf = "";

		Ltbuf = String.format(Ltfmt, Lidbl);

		pary = Ltbuf.split("\\.");

		if (pary.length == 0) {
			return STAR.substring(0, outlen);
		}
		String p3 = "", p4 = "";
		p3 = pary[0];

		int LIfmt = p3.length();
		if (pary.length > 1)
			p4 = pary[1];

		if (LIfmt > Ldec) {
			return STAR.substring(0, outlen);
		}
		int p5 = p3.length() - 1; // index of p3
		char[] p5ary = p3.toCharArray();
		int p6 = Ldec - 1; // index of p1
		char[] p6ary = p1.toCharArray();
		int p7 = Ldec; // index of obuf
		char[] p7ary = new char[Ldec];
		for (int j = 0; j < p7ary.length; j++)
			p7ary[j] = (char) ' ';
		int i = 0;
		for (i = 0; i < Ldec; i++) {
			switch ((char) p6ary[p6]) {
			case (char) '9':
				if (p5 < 0)
					p7ary[--p7] = (char) ' ';
				else {
					p7ary[--p7] = p5ary[p5];

					if (p7ary[p7] == (char) '-')
						p7ary[p7] = (char) '0';
					else if (p7ary[p7] == (char) ' ')
						p7ary[p7] = (char) '0';
					p5--;
				}

				break;
			case (char) '-':
			case (char) 'Z':
				if (p5 < 0)
					p7ary[--p7] = (char) ' ';
				else {
					p7ary[--p7] = p5ary[p5];
					p5--;
				}
				if (p7ary[p7] == (char) '+')
					p7ary[p7] = (char) ' ';
				break;
			case (char) ',':
				if (p6ary[p6 - 1] == (char) '9')
					p7ary[--p7] = (char) ',';
				else if ((p5 > -1) && p5ary[p5] == (char) ' ')
					p7ary[--p7] = (char) ' ';
				else
					p7ary[--p7] = (char) ',';
				break;
			default:
				if (p5 < 0) {
					p7ary[--p7] = (char) ' ';
				} else {
					p7ary[--p7] = p5ary[p5];
					p5--;
				}
				break;
			}
			p6--;
		}

		if (Lminus) {
			if (idbl < 0) {
				for (i = 1; i < Ldec; i++) {
					if (p7ary[i] != ' ') {
						p7ary[i - 1] = (p7ary[i - 1] != (char) ' ') ? p7ary[i - 1] : (char) '-';
						break;
					}
				}
			}
		}
		if (Ldollarsign) {
			for (i = 1; i < Ldec; i++) {
				if (p7ary[i] != ' ') {
					p7ary[i - 1] = (char) '$';
					break;
				}
			}
		} else if (Lstarsign) {
			for (i = 1; i < Ldec; i++) {
				if (p7ary[i] != ' ') {
					p7ary[i - 1] = (char) '*';
					break;
				}
			}
		}
		String obuf = new String(p7ary);
		if (p2.length() > 0) {
			obuf = obuf + '.' + p4;
		}
		return obuf;
	}

	public static <T> T[] concatArray(T[] first, T[] second) {
		T[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}
	private static int keyOffset = 0;
	private static int keyLength = 15;

	//20210116 MatsudairaSyuMe for incoming TOTA telegram
	public static String getTelegramKey(byte[] bytes) {
		if (bytes == null) {
			return "";
		}
		if (bytes.length < keyLength) {
			return "";
		}

		bytes = ArrayUtils.subarray(bytes, keyOffset, keyOffset + keyLength);
		return new String(bytes, StandardCharsets.UTF_8);
	}
}
