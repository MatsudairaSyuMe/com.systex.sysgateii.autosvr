package com.systex.sysgateii.autosvr.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import ch.qos.logback.classic.util.ContextInitializer;

public class HeadLog implements Runnable {

//  static Logger logger = LoggerFactory.getLogger(HeadLog.class);
	public static Logger logger = null;

	private String name;

	@Override
	public void run() {

		MDC.put("logFileName", getName());
		//System.out.println(getName());
		logger.info("hello");

		// remember remove this
		MDC.remove("logFileName");

	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public static void main(String[] args) {
		/*
		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		JoranConfigurator configurator = new JoranConfigurator();
		lc.reset();
		configurator.setContext(lc);
		try {
			configurator.doConfigure("/home/scotthong/botworkbts/BF20118030008/autoprt/shift.xml");
		} catch (JoranException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "/home/scotthong/botworkbts/BF20118030008/autoprt/sift.xml");
		logger = LoggerFactory.getLogger("HeadLog");
		int count = 1;
		ExecutorService threadPools = Executors.newFixedThreadPool(5);// creating a pool of 5 threads
		while (count <= 20) {
			HeadLog head = new HeadLog();
			head.setName("head-" + count);
			threadPools.execute(head);
			count++;
		}
		count = 1;
		while (count <= 10) {
			HeadLog head = new HeadLog();
			head.setName("head-" + count);
			threadPools.execute(head);
			count++;
		}

		threadPools.shutdown();
		while (!threadPools.isTerminated()) {
		}
		System.out.println("Finished all threads " + count);
	}
}
