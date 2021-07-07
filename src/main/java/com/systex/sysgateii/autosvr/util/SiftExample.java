package com.systex.sysgateii.autosvr.util;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;

public class SiftExample {
	static Logger logger = null;
	public static void main(String[] args) throws JoranException {
/*		if (args.length != 1) {
			usage("Wrong number of arguments.");
		}

		String configFile = args[0];

		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
		JoranConfigurator configurator = new JoranConfigurator();
		lc.reset();
		configurator.setContext(lc);
		configurator.doConfigure(configFile);*/
		System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "/home/scotthong/botworkbts/BF20118030008/autoprt/sift2.xml");

		logger = LoggerFactory.getLogger(SiftExample.class);
		logger.debug("Application started");

		MDC.put("userid", "Alice");
		for (int i = 0; i < 10; i++)
			logger.debug("Alice says hello");
		// StatusPrinter.print(lc);
	}

	static void usage(String msg) {
		System.err.println(msg);
		System.err.println("Usage: java " + SiftExample.class.getName() + " configFile\n"
				+ "   configFile a logback configuration file");
		System.exit(1);
	}
}
