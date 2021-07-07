package com.systex.sysgateii.autosvr.util;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.RollingPolicyBase;
import ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import ch.qos.logback.core.util.StatusPrinter;

import java.io.File;

//import ch.qos.logback.core.util.StatusPrinter;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;

public class LogTestMain {
//	public static Logger logger = (Logger) LoggerFactory.getLogger(LogTestMain.class);
/*	public LogTestMain () {
	    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

	    // Don't inherit root appender
//	    logger.setAdditive(false);
	    logger.setAdditive(true);
	    RollingFileAppender<ILoggingEvent> rollingFile = new RollingFileAppender<ILoggingEvent>();
	    rollingFile.setContext(context);
//	    rollingFile.setName("dynamic_logger_fileAppender");

	    // Optional
	    rollingFile.setFile("/home/scotthong/tmp"
	            + File.separator + "msg.log");
	    rollingFile.setAppend(true);

	    // set up pattern encoder
	    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
	    encoder.setContext(context);
	    encoder.setPattern("%msg%n");
	    encoder.start();

	    // Set up rolling policy
	    TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<ILoggingEvent>();
	    rollingPolicy.setMaxHistory(3);
	    rollingPolicy.setFileNamePattern("/home/scotthong/tmp"
	            + File.separator + "archive"
	            + File.separator + "msg_%d{yyyy-MM-dd_HH-mm-sss}.txt");
	    rollingPolicy.setCleanHistoryOnStart(true);
	    rollingPolicy.setContext(context);
	    rollingPolicy.setParent(rollingFile);
	    rollingPolicy.start();
		// Also impose a max size per file policy.
		SizeAndTimeBasedFNATP<ILoggingEvent> fnatp = new SizeAndTimeBasedFNATP<ILoggingEvent>();
		fnatp.setContext(context);
		rollingPolicy.setTimeBasedFileNamingAndTriggeringPolicy(fnatp);
		fnatp.setMaxFileSize(FileSize.valueOf(String.format("%s", 512l)));
		fnatp.setTimeBasedRollingPolicy(rollingPolicy);
		fnatp.start();

	    rollingFile.setRollingPolicy(rollingPolicy);
	    rollingFile.setTriggeringPolicy(rollingPolicy);
	    rollingFile.setEncoder(encoder);
	    rollingFile.start();

	    // Atach appender to logger
	    logger.addAppender(rollingFile);
	}*/
	public static void main(String[] args) {
/*		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

		RollingFileAppender<ILoggingEvent> rfAppender = new RollingFileAppender<ILoggingEvent>();
		rfAppender.setContext(loggerContext);
		rfAppender.setFile("testFile.log");
		
		FixedWindowRollingPolicy rollingPolicy = new FixedWindowRollingPolicy();
		rollingPolicy.setContext(loggerContext);
		// rolling policies need to know their parent
		// it's one of the rare cases, where a sub-component knows about its parent
		rollingPolicy.setParent(rfAppender);
		rollingPolicy.setFileNamePattern("testFile.%i.log.zip");
		rollingPolicy.start();

		SizeBasedTriggeringPolicy<ILoggingEvent> triggeringPolicy = new SizeBasedTriggeringPolicy<ILoggingEvent>();
		triggeringPolicy.setMaxFileSize(FileSize.valueOf("5MB"));
		triggeringPolicy.start();

		PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		encoder.setContext(loggerContext);
		encoder.setPattern("%-4relative [%thread] %-5level %logger{35} - %msg%n");
		encoder.start();

		rfAppender.setEncoder(encoder);
		rfAppender.setRollingPolicy(rollingPolicy);
		rfAppender.setTriggeringPolicy(triggeringPolicy);

		rfAppender.start();

		// attach the rolling file appender to the logger of your choice
		Logger logbackLogger = loggerContext.getLogger("LogTestMain");
		logbackLogger.addAppender(rfAppender);
*/
		// OPTIONAL: print logback internal status messages
//		StatusPrinter.print(loggerContext);

		// log something
		/*
		Logger logbackLogger = LogUtil.getDailyLogger("/home/scotthong/tmp", "LogTestMain", "info", "[%d{yyyy/MM/dd HH:mm:ss:SSS}]%msg%n");
		Logger logbackLogger2 = LogUtil.getDailyLogger("/home/scotthong/tmp", "LogTestMain2", "info", "[%d{yyyy/MM/dd HH:mm:ss:SSS}]%msg%n");
		for (int i = 0; i < 1000; i++) {
			logbackLogger.info("hello");
			logbackLogger.info("hello [{}]", i);
			logbackLogger2.info("hello");
			logbackLogger2.info("hello [{}]", i);
		}
		*/
		/*
		LogTestMain l = new LogTestMain();
		for (int i = 0; i < 1000; i++) {
			logger.info("hello [{}]", i);
		}
		*/
		System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "." + File.separator + "logback.xml");
	    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

	    FileAppender<ILoggingEvent> fileAppender = new FileAppender<ILoggingEvent>();
	    fileAppender.setContext(loggerContext);
	    fileAppender.setName("timestamp");
	    // set the file name
	    fileAppender.setFile("/home/scotthong/tmp/" + System.currentTimeMillis()+".log");

	    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
	    encoder.setContext(loggerContext);
	    encoder.setPattern("%r %thread %level - %msg%n");
	    encoder.start();

	    fileAppender.setEncoder(encoder);
	    fileAppender.start();

	    // attach the rolling file appender to the logger of your choice
	    Logger logbackLogger = loggerContext.getLogger("Main");
	    logbackLogger.addAppender(fileAppender);

	    // OPTIONAL: print logback internal status messages
	    StatusPrinter.print(loggerContext);

	    // log something
	    logbackLogger.debug("hello");
	}
}
