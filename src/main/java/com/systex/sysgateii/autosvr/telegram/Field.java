package com.systex.sysgateii.autosvr.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Field {
	String name = "";
	int offset = 0;
	int len = 0;
	private static Logger log = LoggerFactory.getLogger(Field.class);

	public Field() {
	}

	public void setData(String n, int o, int l) {
		name = n;
		offset = o;
		len = l;
	}

	public void showData() {
//		System.out.println(String.format("name %s offset %d len %d", name, offset, len));
		log.debug(String.format("name %s offset %d len %d", name, offset, len));
	}

}
