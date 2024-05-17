package com.systex.sysgateii.autosvr.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import static java.lang.System.out;

/*
 * Convert tool between UTF8 and BIG5
 * MatsudairaSyuMe
 * 
 * Demonstrate default Charset-related details.
 * 20190902
 * 
 */
public class CharsetCnv {
	private static final Charset BIG5 = Charset.forName("BIG5");
	private static final Charset UTF8 = Charset.forName("UTF-8");

	public static boolean ChkAddFont(int fontno) {
		// use another check function
		long fontnum;
		long fontnum1, fontnum2;

		if (fontno < (long)0x8140)
			return false;
		if (fontno < (long)0xC980 && fontno > (long)0xC69E) // 斷層 C980-C69E
			return false;
		if (fontno < (long)0xAE8A && fontno > (long)0x9ACF) // 斷層 AE8A-9ACF
			return false;

		if (fontno >= (long)0xAE8A) // while > 8E8A
		{
			fontnum = fontno - (fontno & (long)0xFF);
//			out.println("fontno=" + fontno + ":" + (fontno & (long)0xFF) + ": fontnum=" + fontnum);
//			out.println("fontnum + (long)0x9E=" + (fontnum + (long)0x9E) + ": fontnum + (long)0x80=" + (fontnum + (long)0x80));
			if (fontno > fontnum + (long)0x9E || fontno < fontnum + (long)0x80)
				return false;
		} else // while < 8E8A
		{
			fontnum = fontno - (fontno & (long)0xFF);
			fontnum1 = fontnum + (long)0x7E;
			fontnum2 = fontnum + (long)0xA1;
			fontnum = fontnum + (long)0x40;
			if ((fontno & 0xFF) == (long)0x00FF)
				return false;
			if (fontno < fontnum || (fontno > fontnum1 && fontno < fontnum2))
				return false;
		}

		return true;
	}
	
	public static boolean ChkExtFont(int fontno)
	{
		long fontnum1,fontnum2;

		fontnum1 = ((fontno & 0xFF00)) >> 8;
		fontnum2 = (fontno & 0x00FF);
		if ( ((long)0x81 <= fontnum1 && fontnum1 <= (long)0xD7) &&
			 ((long)0x21 <= fontnum2 && fontnum2 <= (long)0x3F) )
			 return true;

		if ( fontnum1 == (long)0xD8 &&
			 ((long)0x21 <= fontnum2 && fontnum2 <= (long)0x24 ) )
			 return true;

		if ( ((long)0x81 <= fontnum1 && fontnum1 <= (long)0xAD) &&
			 ((long)0x81 <= fontnum2 && fontnum2 <= (long)0x9F) )
			 return true;

		/*
		fontnum  = fontno - (fontno&0xFF);
		fontnum1 = fontnum + 0x01;
		fontnum2 = fontnum + 0x3F;
		if (fontno < fontnum1 || fontno > fontnum2)
			return false;
		*/
		return false;
	}
	
	/**
	 * Supplies the default encoding without using Charset.defaultCharset() and
	 * without accessing System.getProperty("file.encoding").
	 *
	 * @return Default encoding (default charset).
	 */
	public static String getEncoding() {
		final byte[] bytes = { 'D' };
		final InputStream inputStream = new ByteArrayInputStream(bytes);
		final InputStreamReader reader = new InputStreamReader(inputStream);
		final String encoding = reader.getEncoding();
		return encoding;
	}

	public String BIG5UTF8str(String big5input) throws Exception {
		if (big5input == null)
			throw new Exception("input data buffer null");
		byte[] utf8Encoded = null;
		byte[] big5Encoded = big5input.getBytes(BIG5);
		String decoded = new String(big5Encoded, BIG5);
		utf8Encoded = decoded.getBytes(UTF8);
		return new String(utf8Encoded, UTF8);
	}

	public String BIG5bytesUTF8str(byte[] big5input) throws Exception {
		if (big5input == null || big5input.length == 0)
			throw new Exception("input data buffer null");
		byte[] utf8Encoded = null;
		byte[] big5Encoded = big5input;
		String decoded = new String(big5Encoded, BIG5);
		utf8Encoded = decoded.getBytes(UTF8);
		return new String(utf8Encoded, UTF8);
	}

	public byte[] BIG5UTF8bytes(String big5input) throws Exception {
		if (big5input == null)
			throw new Exception("input data buffer null");
		byte[] utf8Encoded = null;
		byte[] big5Encoded = big5input.getBytes(BIG5);
		String decoded = new String(big5Encoded, BIG5);
		utf8Encoded = decoded.getBytes(UTF8);
		return utf8Encoded;
	}

	public String UTF8BIG5str(String utfinput) throws Exception {
		if (utfinput == null)
			throw new Exception("input data buffer null");
		byte[] big5Encoded = null;
//        byte[] utf8Encoded = utfinput.getBytes(UTF8); 
		String decoded = utfinput;
		big5Encoded = decoded.getBytes(BIG5);
		return new String(big5Encoded);
	}

	public String UTF8bytesBIG5str(byte[] utfinput) throws Exception {
		if (utfinput == null || utfinput.length == 0)
			throw new Exception("input data buffer null");
		byte[] big5Encoded = null;
		String decoded = new String(utfinput, UTF8);
		big5Encoded = decoded.getBytes(BIG5);
		return new String(big5Encoded, BIG5);
	}

	public byte[] UTF8BIG5bytes(String utfinput) throws Exception {
		if (utfinput == null)
			throw new Exception("input data buffer null");
		byte[] big5Encoded = null;
		byte[] utf8Encoded = utfinput.getBytes(UTF8);
		String decoded = new String(utf8Encoded, UTF8);
		big5Encoded = decoded.getBytes(BIG5);
		return big5Encoded;
	}

	public static void main(final String[] arguments) {
		CharsetCnv cc = new CharsetCnv();
		CharsetCnv.getEncoding();
		out.println("Default Encoding: " + CharsetCnv.getEncoding());
		try {
			String src = "AB 摩根太證息 12AB";
			out.println("UTF8 " + src);
			byte[] srcb = src.getBytes();
			for (int i = 0; i < srcb.length; i++)
				out.println(String.format("%02x ", srcb[i] & 0xff));
			/*
			 * String r = cc.UTF8BIG5str(src); out.println("摩根太證息 Encoding UTF8BIG5str: " +
			 * r);
			 */
			byte[] b = cc.UTF8BIG5bytes(src);
			out.println("UTF8 摩根太證息 Encoding BIG5UTF8bytes: ");
			for (int i = 0; i < b.length; i++)
				out.println(String.format("%02x ", b[i] & 0xff));
			/*
			 * out.println("big5 摩根太證息 Encoding BIG5UTF8str: " + cc.BIG5UTF8str(r));
			 */
			out.println("摩根太證息 Encoding bytes: " + new String(b));
			out.println("big5 摩根太證息 Encoding BIG5bytesUTF8str: " + cc.BIG5bytesUTF8str(b));
			byte s[] = {(byte)0x81, (byte)0x39};
			out.println("s=" + cc.ChkAddFont((((int)((s[0] & 0xff))<<8)+((int)(s[1] & 0xff)))));
			s[0] = (byte)0x81; s[1] = (byte)0x40;
			out.println("s=" + cc.ChkAddFont((((int)((s[0] & 0xff))<<8)+((int)(s[1] & 0xff)))));
			s[0] = (byte)0xC9; s[1] = (byte)0x80;
			out.println("s=" + cc.ChkAddFont((((int)((s[0] & 0xff))<<8)+((int)(s[1] & 0xff)))));
			s[0] = (byte)0xC9; s[1] = (byte)0x7f;
			out.println("s=" + cc.ChkAddFont((((int)((s[0] & 0xff))<<8)+((int)(s[1] & 0xff)))));
			s[0] = (byte)0xAE; s[1] = (byte)0x8A;
			out.println("s=" + cc.ChkAddFont((((int)((s[0] & 0xff))<<8)+((int)(s[1] & 0xff)))));
			s[0] = (byte)0xA4; s[1] = (byte)0xA4;
			out.println("s=" + cc.ChkAddFont((((int)((s[0] & 0xff))<<8)+((int)(s[1] & 0xff)))));
		} catch (Exception e) {
			System.err.println("convert Error");//20240503 MatsudairaSyuMe change log message
		}
	}
}
