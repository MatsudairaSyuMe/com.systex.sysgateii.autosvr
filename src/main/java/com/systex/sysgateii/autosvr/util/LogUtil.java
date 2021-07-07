package com.systex.sysgateii.autosvr.util;

import ch.qos.logback.classic.Level;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
/**
 * <pre>
 * Change log4j log file name at initial.
 * 會影響全部的使用者
 * sample:
 *   LogUtil.getDailyLogger("pathname", "logfilename", "debug", "TIME     [0000]:%d{yyyy.MM.dd HH:mm:ss:SSS} %msg%n");
 * </pre>
 * @param logPathName
 * @param logFileName
 * @parm logLevel
 * @param logMessagePattrn
 */

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedFNATP;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.rolling.TimeBasedFileNamingAndTriggeringPolicy;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.util.FileSize;
import ch.qos.logback.core.util.OptionHelper;
import ch.qos.logback.core.util.StatusPrinter;


public class LogUtil {
	public static Logger getDailyLoggerorig(String pathname, String logName, String level, String ptrn) {
		Logger logbackLogger = (Logger) LoggerFactory.getLogger(logName);
//		RollingFileAppender<ILoggingEvent> a = (RollingFileAppender<ILoggingEvent>) ((AppenderAttachable<ILoggingEvent>) logbackLogger).getAppender(logName);
/*		if (a != null)
		{
//			System.out.println("Log Appender already exist");
			return logbackLogger;
		}		
*/
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

//		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
//		String byDate = sdf.format(new Date());

		RollingFileAppender<ILoggingEvent> rfAppender = new RollingFileAppender<ILoggingEvent>();
		rfAppender.setContext(loggerContext);
//		rfAppender.setFile(logName + byDate + ".log");
		String fpn = "";
		if (pathname != null && pathname.trim().length() > 0)
			fpn = pathname + File.separator + logName + ".log";
		else
			fpn = "." + File.separator + logName + ".log";
		rfAppender.setFile(fpn);
		//20201221
		rfAppender.setAppend(true);
		//----

//mark20200430		TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();
		FixedWindowRollingPolicy rollingPolicy = new FixedWindowRollingPolicy();
		rollingPolicy.setContext(loggerContext);
		// rolling policies need to know their parent
		// it's one of the rare cases, where a sub-component knows about its parent
		rollingPolicy.setParent(rfAppender);
//		rollingPolicy.setFileNamePattern(logName + byDate + ".%i.log.zip");
		if (pathname != null && pathname.trim().length() > 0)
			fpn = pathname + File.separator + "archive" + File.separator + logName + "-%d{yyyyMMddHHmmss}.%i.log.zip";
		else
			fpn = "." + File.separator + "archive" + File.separator + logName + "-%d{yyyyMMddHHmmss}.%i.log.zip";

		rollingPolicy.setFileNamePattern(fpn );
//mark20200430		rollingPolicy.setMaxHistory(5);
//mark20200430		rollingPolicy.setCleanHistoryOnStart(true);
		//20201221
		if (rollingPolicy.isStarted())
			rollingPolicy.stop();
		//----

		rollingPolicy.start();

		SizeBasedTriggeringPolicy<ILoggingEvent> triggeringPolicy = new SizeBasedTriggeringPolicy<ILoggingEvent>();
//mark20200430		SizeAndTimeBasedFNATP<ILoggingEvent> triggeringPolicy = new SizeAndTimeBasedFNATP<ILoggingEvent>();
		triggeringPolicy.setContext(loggerContext);
		triggeringPolicy.setMaxFileSize(FileSize.valueOf("30MB"));
//mark20200430		triggeringPolicy.setTimeBasedRollingPolicy(rollingPolicy);
		//20201221
		if (triggeringPolicy.isStarted())
			triggeringPolicy.stop();
		//----

		triggeringPolicy.start();

		//---
		//add for SizeAndTimeBasedFNATP
//mark20200430		rollingPolicy.setTimeBasedFileNamingAndTriggeringPolicy(triggeringPolicy);
//mark20200430		rollingPolicy.start();
		//---

		PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		encoder.setContext(loggerContext);
//		encoder.setPattern("%-4relative [%thread] %-5level %logger{35} - %msg%n");
		encoder.setPattern(ptrn);
		//20201221
		if (encoder.isStarted())
			encoder.stop();
		//----
		encoder.start();

		rfAppender.setEncoder(encoder);
		rfAppender.setRollingPolicy(rollingPolicy);
		rfAppender.setTriggeringPolicy(triggeringPolicy);
		//20201221
		if (rfAppender.isStarted())
			rfAppender.stop();
		//----

		rfAppender.start();

		// attach the rolling file appender to the logger of your choice
		logbackLogger = loggerContext.getLogger(logName);
		logbackLogger.addAppender(rfAppender);
		if (level.equalsIgnoreCase("debug"))
		{
			logbackLogger.setLevel(Level.DEBUG);			
		}
		else if (level.equalsIgnoreCase("info"))
		{
			logbackLogger.setLevel(Level.INFO);						
		}
		else if (level.equalsIgnoreCase("error"))
		{
			logbackLogger.setLevel(Level.ERROR);						
		}
		else
		{
			logbackLogger.setLevel(Level.ALL);												
		}

		return logbackLogger;
	}
	public static Logger getDailyLogger(String pathname, String logName, String level, String ptrn) {
		Logger logbackLogger = (Logger) LoggerFactory.getLogger(logName);

		RollingFileAppender<ILoggingEvent> a = (RollingFileAppender<ILoggingEvent>) ((AppenderAttachable<ILoggingEvent>) logbackLogger).getAppender(logName);
		if (a != null)
		{
			System.out.println("Log Appender already exist");
			return logbackLogger;
		}
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

		String fpn = "";
		if (pathname != null && pathname.trim().length() > 0)
			fpn = pathname + File.separator + logName + ".log";
		else
			fpn = "." + File.separator + logName + ".log";

		RollingFileAppender<ILoggingEvent> rfAppender = new RollingFileAppender<ILoggingEvent>();
		rfAppender.setContext(loggerContext);
		rfAppender.setFile(OptionHelper.substVars(fpn, loggerContext));
		rfAppender.setAppend(true);
		rfAppender.setPrudent(false);

		PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		encoder.setContext(loggerContext);
		encoder.setPattern(ptrn);
		
		TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();
		if (pathname != null && pathname.trim().length() > 0)
			fpn = pathname + File.separator + "archive" + File.separator + logName + ".%d{yyyy-MM-dd}.%i.log.zip";
		else
			fpn = "." + File.separator + "archive" + File.separator + logName + ".%d{yyyy-MM-dd}.%i.log.zip";
		rollingPolicy.setMaxHistory(3);
		rollingPolicy.setFileNamePattern(OptionHelper.substVars(fpn, loggerContext));
		rollingPolicy.setCleanHistoryOnStart(true);
		rollingPolicy.setContext(loggerContext);
		rollingPolicy.setParent(rfAppender);
		// Also impose a max size per file policy.
		SizeAndTimeBasedFNATP<ILoggingEvent> fnatp = new SizeAndTimeBasedFNATP<ILoggingEvent>();
		fnatp.setContext(loggerContext);
		fnatp.setTimeBasedRollingPolicy(rollingPolicy);
		fnatp.setMaxFileSize(FileSize.valueOf(String.format("%sMB", 30l)));
		
		rollingPolicy.setTimeBasedFileNamingAndTriggeringPolicy(fnatp);
		rfAppender.setRollingPolicy(rollingPolicy);
		rfAppender.setTriggeringPolicy(rollingPolicy);
		logbackLogger = loggerContext.getLogger(logName);
		logbackLogger.addAppender(rfAppender);
		if (level.equalsIgnoreCase("debug"))
		{
			logbackLogger.setLevel(Level.DEBUG);			
		}
		else if (level.equalsIgnoreCase("info"))
		{
			logbackLogger.setLevel(Level.INFO);						
		}
		else if (level.equalsIgnoreCase("error"))
		{
			logbackLogger.setLevel(Level.ERROR);						
		}
		else
		{
			logbackLogger.setLevel(Level.ALL);												
		}
		rfAppender.setEncoder(encoder);
		//rfAppender.setRollingPolicy(rollingPolicy);

		// attach the rolling file appender to the logger of your choice
		if (encoder.isStarted())
			encoder.stop();
		encoder.start();
		if (rollingPolicy.isStarted())
			rollingPolicy.stop();
		rollingPolicy.start();
		if (rfAppender.isStarted())
			rfAppender.stop();
		rfAppender.start();

		return logbackLogger;
	}

	public static void stopLog(Logger tarLog) {
		tarLog.detachAndStopAllAppenders();
	}
}
