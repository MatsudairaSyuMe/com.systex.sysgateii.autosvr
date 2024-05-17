package com.systex.sysgateii.autosvr.conf;

import java.io.BufferedReader;
import java.io.FileInputStream;
//20210413 MatsudairaSyuMe prevent Unreleased Resource
import java.io.IOException;
//----
import java.io.InputStreamReader;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageMappingTable {
	private static Logger log = LoggerFactory.getLogger(MessageMappingTable.class);
	private String defname = "MESSAGE.INI";
	public static ConcurrentHashMap<String, String> m_Message = new ConcurrentHashMap<String, String>();

	public MessageMappingTable() {
		new MessageMappingTable(this.defname);
	}
	//20210413 MatsudairaSyuMe prevent Unreleased Resource
	public static void closeQuietly(InputStreamReader isr) {
		try {
			if (isr != null) {
				isr.close();
			}
		} catch (IOException ioe) {
			// ignore
		}
	}
	//----
	public MessageMappingTable(String filename) {
		BufferedReader reader;
		//20210413 MatsudairaSyuMe prevent Unreleased Resource
		InputStreamReader isr = null;
		int total = 0;
		m_Message.clear();
		try {
			//20210413 MatsudairaSyuMe prevent Unreleased Resource
			isr = new InputStreamReader(new FileInputStream(filename));
			reader = new BufferedReader(isr);
			String line = reader.readLine();
			while (line != null) {
				line = line.trim();
				if (line.length() > 0 && !line.substring(0, 1).equals("#") && line.contains("=")) {
					if (line.lastIndexOf('=') != (line.length() - 1)) {
						String[] sar = line.split("=");
//						log.debug("item=[" + sar[0].trim() + "] message=[" + sar[1].trim());
						m_Message.put(sar[0].trim(), sar[1].trim());
					} else {
						m_Message.put(line.substring(0, line.lastIndexOf('=')), "");
					}
					total += 1;
				}
				//20240510 Poor Style: Value Never Read line = "";
				line = reader.readLine();
				// read next line
			}
			//20210413 MatsudairaSyuMe prevent Unreleased Resource
			if (reader != null)
				reader.close();
			//----
		} catch (Exception e) {
			//20240503 MatsudairaSyuMe mark for System Information Leak e.getStackTrace();
			log.error("ERROR!! i/o exception");//20240503 change log message
		}
		//20210413 MatsudairaSyuMe prevent Unreleased Resource
		 finally {
			 closeQuietly(isr);
		 }
		log.debug("total {} records", total);
	}

	public static void main(String[] args) {
		MessageMappingTable d = new MessageMappingTable("MESSAGE.INI");
		System.out.println(d.m_Message.size());
		System.out.println(d.m_Message.get("E104"));
		System.out.println(d.m_Message.get("A104"));
	}

}
