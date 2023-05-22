package com.systex.sysgateii.autosvr.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/*
 * Get Big5 72 bytes image font data utility
 * MatsudairaSyuMe
 * 
 * 20200502
 * 
 */

public class Big5FontImg {
	private FileInputStream fontkeyis = null;
	private FileInputStream fontis = null;
	private long fsize = 0l;
	private ConcurrentHashMap<Long, Long> keyadr_map = new ConcurrentHashMap<Long, Long>();
	//20211004 MatsudairaSyuMe change to open key file and glyph file
	private static String keyname = "";
	private static String glyphname = "";
	//20211004 MatsudairaSyuMe read key and glyph file every time load plyph
	public Big5FontImg(String filenamekey, String filename) throws IOException, Exception {
		if (filenamekey == null || filename.trim().length() == 0)
			throw new IOException("filenamekey null or length == 0");
		if (filename == null || filename.trim().length() == 0)
			throw new IOException("filename null or length == 0");
		keyname = filenamekey;
		glyphname = filename;
		//20230522 MatsudairaSyuMe change the tns path from /biscon/tns to /tns
		File testf = new File(keyname);
		if(!testf.exists())
			throw new IOException(String.format("file %s not exist", keyname));
		testf = new File(glyphname);
		if(!testf.exists())
			throw new IOException(String.format("file %s not exist", glyphname));
		testf = null;
		//----
		/*
		try {
			// create new file input stream
			fontkeyis = new FileInputStream(filenamekey);
			fontis = new FileInputStream(filename);
			this.fsize = fontis.getChannel().size();
			// read bytes to the buffer
			int i = 0;
			int idx = 0;
			byte[] bs = new byte[6];
			long key = 0l, addr = 0l;
			while ((i = fontkeyis.read(bs)) != -1) {
				// prints
//                System.out.println("Number of bytes read: "+i);
//                System.out.print("     Bytes read: ");
				idx += 1;

				// for each byte in buffer
//                for(byte b:bs)
//                {
				// print
//                    System.out.print(String.format("%02x ",b));
//                }
				key = 0l;
				addr = 0l;
				key = (long) (((bs[0] & 0xFF) << 8) | (bs[1] & 0xFF));
				addr = (long) (((bs[2] & 0xFF) << 24) | ((bs[3] & 0xFF) << 16) | ((bs[4] & 0xFF) << 8)
						| (bs[5] & 0xFF));
//                System.out.println("key=" + key + " addr=" + addr);
				keyadr_map.put(key, addr);
			}
//			System.out.println("Big5 table key total " + idx + " records create map table size=" + keyadr_map.size());
			if (fontkeyis != null)
				fontkeyis.close();
		} catch (Exception ex) {
			// if any error occurs
			ex.printStackTrace();
			throw new Exception(ex.toString());
		}*/
	}
	//----

	public byte[] toByteArray(long start, int count) throws IOException, Exception {
		// skip bytes from file input stream
		byte[] rtn = new byte[count];
		Arrays.fill(rtn, (byte) 0x0);
		try {
			long l = keyadr_map.get(start);
 //          System.out.println("key=" + start + " addr=" + l + ": addr * 72 = " + (l * 72) + " size=" + this.fsize);
			fontis.skip(l);
			if (fontis.read(rtn) != count) {
				Arrays.fill(rtn, (byte) 0x0);
//				throw new Exception("read length error");
			}
		} catch (Exception ex) {
			// if any error occurs
			ex.printStackTrace();
			Arrays.fill(rtn, (byte) 0x0);
//			throw new Exception("read file error:" + ex.toString());
		}
		return rtn;
	}

	//20211004 MatsudairaSyuMe read key and glyph file every time load plyph
//	public byte[] getFontImageData(long start) throws IOException, Exception {
//		return toByteArray(start, 72);
//	}
	public byte[] getFontImageData(long start) throws IOException, Exception {
		try {
			// create new file input stream
			CloseFontFile();
			fontkeyis = new FileInputStream(keyname);
			fontis = new FileInputStream(glyphname);
			this.fsize = fontis.getChannel().size();
			// read bytes to the buffer
			long i = 0;
			long idx = 0;
			byte[] bs = new byte[6];
			long key = 0l, addr = 0l;
			while ((i = fontkeyis.read(bs)) != -1) {
				idx += 1;
				key = 0l;
				addr = 0l;
				key = (long) (((bs[0] & 0xFF) << 8) | (bs[1] & 0xFF));
				addr = (long) (((bs[2] & 0xFF) << 24) | ((bs[3] & 0xFF) << 16) | ((bs[4] & 0xFF) << 8)
						| (bs[5] & 0xFF));
				keyadr_map.put(key, addr);
			}
			if (fontkeyis != null)
				fontkeyis.close();
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new Exception(ex.toString());
		}
		return toByteArray(start, 72);
	}
	//----

	public void CloseFontFile() throws IOException, Exception {
		try {
//			if (fontkeyis != null)
//				fontkeyis.close();
			if (keyadr_map != null && keyadr_map.size() > 0)
				keyadr_map.clear();
			if (fontis != null)
				fontis.close();
		} catch (Exception ex) {
			// if any error occurs
			ex.printStackTrace();
			throw new Exception("close file error:" + ex.toString());
		}
	}

	//20211004 MatsudairaSyuMe read key and glyph file every time load plyph
	//----

	//public static void main(String[] args) throws IOException {
//
	//	try {
	//		Big5FontImg fd = new Big5FontImg("FontTable_low.bin", "FontData_All.bin");
//
	//		byte[] aa = fd.getFontImageData((long) 0x8140);
	//		System.out.println("len " + aa.length + ":" + (long) 0x8140);
	//		for (byte b : aa)
	//			System.out.print(String.format("%02x ", b));
	//		System.out.println();
//
	//		aa = fd.getFontImageData((long) 0x8141);
	//		System.out.println("len " + aa.length + ":" + (long) 0x8141);
	//		for (byte b : aa)
	//			System.out.print(String.format("%02x ", b));
	//		System.out.println();
	//		aa = fd.getFontImageData((long) 0xfe9d);
	//		System.out.println("len " + aa.length + ":" + (long) 0xfe9d);
	//		for (byte b : aa)
	//			System.out.print(String.format("%02x ", b));
	//		System.out.println();
	//		aa = fd.getFontImageData((long) 0xfe9e);
	//		System.out.println("0xfe9e len " + aa.length + ":" + (long) 0xfe9e);
	//		for (byte b : aa)
	//			System.out.print(String.format("%02x ", b));
	//		System.out.println();
//			aa = fd.getFontImageData((long) 0xfe9e);
	//		aa = fd.getFontImageData((long) 0x95b0);
	//		System.out.println("0x95b0 len " + aa.length + ":" + (long) 0x95b0);
	//		for (byte b : aa)
	//			System.out.print(String.format("%02x ", b));
	//		System.out.println();
	//		for (int i = 0; i < 20; i++) {
	//			aa = fd.getFontImageData((long) 38320);
	//			System.out.println("38320 len " + aa.length + ":" + 38320 + ": i=" +i);
	//			for (byte b : aa)
	//				System.out.print(String.format("%02x ", b));
	//			System.out.println();
	//		}
	//		/*
	//		 * aa = fd.getFontImageData((long)0xf9d6); System.out.println("0xf9d6 len " +
	//		 * aa.length + ":" + (long)0xf9d6); for (byte b : aa)
	//		 * System.out.print(String.format("%02x ", b)); System.out.println();
	//		 */
	//		fd.CloseFontFile();
	//	} catch (Exception ex) {
	//		// if any error occurs
	//		ex.printStackTrace();
	//	}
	//}
}
