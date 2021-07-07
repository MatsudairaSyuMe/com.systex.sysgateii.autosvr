package com.systex.sysgateii.autosvr.util;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

public class LogTest {
    private static final int TEST_FILE_NUM = 5;
    
    private static final File TEST_DIR = new File(System.getProperty("java.io.tmpdir"), "logbak-test-dir");
 
    @Before
    public void setUp() throws IOException {
 //       System.setProperty("appli.config.path", ClassLoader.getSystemClassLoader().getResource("logback.properties").getPath());
        TEST_DIR.mkdirs();
        FileUtils.cleanDirectory(TEST_DIR);
    }
    
    @Test
    public void testLogbackFileConfig() throws InterruptedException {
 
        Logger logger = (Logger) LoggerFactory.getLogger(LogTest.class);
 
        // assume SLF4J is bound to logback in the current environment
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        JoranConfigurator configurator = new JoranConfigurator();
        configurator.setContext(context);
        context.reset();
 
        try {
            configurator.doConfigure(ClassLoader.getSystemClassLoader().getResource("aslogback.xml").getFile());
        } catch (JoranException je) {
            StatusPrinter.print(context);
            Assert.fail("Erreur de chargement des paramètre logback");
        }
 
        for (int i = 1; i <= TEST_FILE_NUM; i++) {
            MDC.put("loggerFileName", "file" + i);
            logger.info("log in File{}", i);
            logger.debug("log in File{}", i);
            logger.error("log in File" + i, new Exception("My test exception", new Exception("my cause of exception")));
        }
 
        Assert.assertEquals(TEST_FILE_NUM * 2, TEST_DIR.listFiles().length);
    }
}
