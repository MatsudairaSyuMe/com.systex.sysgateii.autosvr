package com.systex.sysgateii.autosvr.util;
import java.io.PrintWriter;
import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.Reader;
import java.io.InputStreamReader;

public class FileUtils {
	public static boolean setCurrentDirectory(String directory_name)
	{
		boolean result = false;  // Boolean indicating whether directory was set
		File    directory;       // Desired current working directory

		directory = new File(directory_name).getAbsoluteFile();
		if (directory.exists() || directory.mkdirs())
		{
			result = (System.setProperty("user.dir", directory.getAbsolutePath()) != null);
		}

		return result;
	}

	public static PrintWriter openOutputFile(String file_name)
	{
		PrintWriter output = null;  // File to open for writing

		try {
			output = new PrintWriter(new File(file_name).getAbsoluteFile());
		} catch (Exception e) {}
		return output;
	}
	public static boolean writeFile(String path, byte[] content, boolean append) throws IOException
	{
		boolean res = false;
		File f = new File(path);
		RandomAccessFile raf = null;
		try {
			if (f.exists()) {
				if (!append) {
					//20240515 MatsudairaSyuMe Unchecked Return Value
					if (!f.delete())
						throw new IOException(String.format("delete ERROR %s!!!", path));
					//----20240515
					f.createNewFile();
				}
			} else {
				if ((path.lastIndexOf(File.separatorChar) == 1 && path.indexOf((char)'.') != 0) ||
						path.lastIndexOf(File.separator) > 1) {
					//20240510 Poor Style: Value Never Read boolean result = false;  // Boolean indicating whether directory was set
					String chkpath = path.substring(0, path.lastIndexOf(File.separator));
					File directory;       // Desired current working directory
					directory = new File(chkpath).getAbsoluteFile();
					if (!directory.exists())
						//20240515 MAtsudairaSyuMe Unchecked Return Value
						if (!directory.mkdirs()) {//20240510 Poor Style: Value Never Read take out result
							throw new IOException(String.format("mkdir ERROR %s!!!", path));
						}
						//----20240515
				}
				//System.out.println("create file " + f.getName());
				f.createNewFile();
			}
			if (f.canWrite()) {
				raf = new RandomAccessFile(f, "rw");
				raf.seek(f.length());
				raf.write(content);
				res = true;
			}
		} catch (Exception e) {
			throw new IOException(String.format("ERROR write %s -->%s !!!", path, e.getMessage()));
		} finally {
			if (raf != null) {
				try {
					raf.close();
				} catch (IOException ie) {
					throw new IOException(String.format("ERROR close %s -->%s !!!", path, ie.getMessage()));
				}
			}
		}
		return res;
	}
	public static boolean writeStringToFile(String path, String content, boolean append) throws IOException
	{
		return writeFile(path, content.getBytes(), append);
	}

	public static boolean writeFile(String path, byte[] content) throws IOException
	{
		return writeFile(path, content, false);
	}

	public static boolean writeStringToFile(String path, String content) throws IOException
	{
		return writeFile(path, content.getBytes(), false);
	}
	public static String readFileToString(String filePath, String enCoding) throws IOException
	{
		String streamString = "";
		if (filePath == null || filePath.trim().length() == 0)
			return streamString;
		FileInputStream fis = null;
		File f = new File(filePath);
		try {
			if (!f.exists() || f.isFile())
				f.createNewFile();
			fis = new FileInputStream(f);
			StringBuilder sb = new StringBuilder();
			Reader r = new InputStreamReader(fis, enCoding);  //or whatever encoding
			char[] buf = new char[1024];
			int amt = r.read(buf);
			while(amt > 0) {
				sb.append(buf, 0, amt);
				amt = r.read(buf);
			}
			streamString =  sb.toString();
		} catch (IOException ioe) {
			throw new IOException(String.format("ERROR read %s !!!", filePath));
		} finally {
			fis.close();
		}
		return streamString;
	}
}
