package com.systex.sysgateii.autosvr;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;

/**
 * 
 * Created by MatsudairaSyume 2019/11/5
 */


import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZThread;

import com.systex.sysgateii.autosvr.Monster.Conductor;
import com.systex.sysgateii.autosvr.autoPrtSvr.Server.FASSvr;
import com.systex.sysgateii.autosvr.autoPrtSvr.Server.PrnSvr;
import com.systex.sysgateii.autosvr.comm.Constants;
import com.systex.sysgateii.autosvr.conf.DynamicProps;
import com.systex.sysgateii.autosvr.dao.GwDao;
import com.systex.sysgateii.autosvr.util.StrUtil;
import com.systex.sysgateii.comm.mdp.mdbroker;
import com.systex.sysgateii.comm.mdp.mdworker2;
import com.systex.sysgateii.comm.mdp.spqueue;

import ch.qos.logback.classic.util.ContextInitializer;

public class Server {
	private static Logger log = null;
	private static AtomicBoolean isShouldShutDown = new AtomicBoolean(false);
	private static final long TEST_TIME_SECONDS = 3;
	//20200926
	private static GwDao jsel2ins = null;
	private static String svrstatustbname = "";
	private static String svrsstatustbmkey = "";
	private static String svrstatustbfields = "";
	//20201116 cancel verbrno
//	private static String updValueptrn = "'%s','%s','%s','%s','%s','SYSTEM','','%s'";
	//20201222 add 'SYSTEM' to TB_AUSVRSTS.MODIFIER
	private static String updValueptrn = "'%s','%s','%s','%s','SYSTEM','SYSTEM','%s'";
	//----
	private static String auid = "";
	private static String svrip = "";
	//----
	//20201116 using given svrid
	private static int svrId = 0;
	private static String svrIdStr = "";
	private static String dburl = "";
	private static String dbuser = "";
	private static String dbpass = "";

	//----
	//20201119 conduct mode
	private static AtomicBoolean isConductor = new AtomicBoolean(false);
	//----
	//202010909 conduct restore mode
	private static AtomicBoolean isConductorRestore = new AtomicBoolean(false);
	//----

	public static void main(String[] args) {
		System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "." + File.separator + "logback.xml");
		//20210419 MatsudairaSyuMe mark
		//20210409 MatsudairaSyuMe
		//System.setProperty("log.name", ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
		//----
		//log = LoggerFactory.getLogger(Server.class);
		//20220802 MatsudairaSyuMe change to use spqueue
//		 mdbroker broker = null;
		 spqueue broker = null;
		try {
			//20201116 check if using given svrid
			if (args.length > 0) {
				for (int j = 0; j < args.length; j++) {
					if (args[j].equalsIgnoreCase("--svrid") && ((j + 1) < args.length)) {
						if (args[j + 1].trim().length() > 0) {
							String s = args[j + 1].trim();
							if (Integer.parseInt(s) > 0) {
								setSvrId(Integer.parseInt(s));
							}
						}
					} //20201119 conductor mode
					else if (args[j].equalsIgnoreCase("--conduct")) {
						setIsConductor(true);
						//20210419 MatsudairaSyuMe mark
//						log.info("sysgateii server conductor mode");
						//20210828 MatsudairaSyuMe start conductor only
//						svrip = "";
						//----
						//20210909 MatsudairaSyuMe conductor mode
						setIsConductorRestore(false);
						//---
						System.out.println("sysgateii server conductor mode");
					}
					//20210828,20210909 MatsudairaSyuMe start conductor only
					else if (args[j].equalsIgnoreCase("--restore")) {
						setIsConductor(true);
						setIsConductorRestore(true);
//						svrip = "r";
						System.out.println("sysgateii server conductor restore mode");
					}
					//----
				}
			}
			//20210419 MatsudairaSyuMe set get logger
			if (isConductor.get())
				System.setProperty("log.name", "conduct");
			else
				System.setProperty("log.name", Integer.toString(getSvrId()));
			//----
			log = LoggerFactory.getLogger(Server.class);

			if (getSvrId() > 0)
				log.info("sysgateii server given id [{}] start...", getSvrId());
			else
				log.info("sysgateii server start...");
			//----
			DynamicProps dcf = new DynamicProps("rateprtservice.xml");
			//20201116 change to use given svrid
			auid = dcf.getAuid();
//			//20210828 MatsudairaSyuMe start conductor only
//			if (!isConductor.get() || !svrip.equalsIgnoreCase("r"))
			svrip = dcf.getSvrip();
			//----
			dburl = dcf.getConHashMap().get("system.db[@url]");
			dbuser = dcf.getConHashMap().get("system.db[@user]");
			dbpass = dcf.getConHashMap().get("system.db[@pass]");

			if (auid.trim().length() == 0 || auid.trim().length() == 0) {
				auid = "0";
				//20201119
				if (!isConductor.get())
					 //if not Conduct mode and not set auid then stop process
					setIsShouldShutDown(true);
			}
			//----

			//20201116 add using given svrid
			svrIdStr = "";
			if (getSvrId() > 0)
				svrIdStr = Integer.toString(getSvrId());
			else
				svrIdStr = dcf.getConHashMap().get("system.svrid");
			if (!isShouldShutDown.get()) {
				addDaemonShutdownHook();
				//20201119 add conductor mode control
				if (!isConductor.get()) {
					//worker mode start the working module
//					FASSvr.createServer(dcf.getConHashMap());
//					FASSvr.startServer();
					//20210628 mark for using MDP
//					PrnSvr.createServer(dcf, FASSvr.getFASSvr());
					PrnSvr.createServer(dcf);
					PrnSvr.startServer();

				} else {
					//20210628 use MDB
					//conductor mode start the monitor module
					FASSvr.createServer(dcf.getConHashMap());
					FASSvr.startServer();
					//20210628 use MDP
//					broker = new mdbroker(true);
//					broker.bind("tcp://*:5555");
					//20220802 use spqueue
					broker = new spqueue(false); //20220803 turn off debug
					broker.bind("tcp://localhost:5555", "tcp://localhost:5556");
					Thread brokerThread = new Thread(broker);
					brokerThread.start();
					String tout = dcf.getConHashMap().get("svrsubport.recvtimeout");
					int setResponseTimeout = 300000;
					if (tout != null && tout.trim().length() > 0)
						setResponseTimeout = Integer.parseInt(tout);
					// config workno not set use default workno = 4
					int workno = 4;
					String workstr = "";
					try {
						log.info("workno=[{}]",dcf.getConHashMap().get("workno").trim());
						workstr = dcf.getConHashMap().get("workno").trim();
					} catch (Exception e) {
						log.error("error while get 'workno' from local config file");
					}
					if (workstr != null && workstr.length() > 0)
						workno = Integer.parseInt(workstr);
					for (int startwk = 0; startwk < workno; startwk++) //20220804 turn off debug
						ZThread.start(new mdworker2("mdworker" + startwk, "tcp://localhost:5556", "fas", setResponseTimeout, 500, FASSvr.getFASSvr(), false)); //20220505 change to localhost, 20220726 change debug verbose on
//						ZThread.start(new mdworker2("mdworker" + startwk, "tcp://localhost:5555", "fas", setResponseTimeout, 500, FASSvr.getFASSvr(), false)); //2022728 change to localhost, 20220726 change debug verbose off
					//----
					//ZThread.start(new mdworker2("tcp://*:5555", "fas", setResponseTimeout, 500, FASSvr.getFASSvr(), false));
					Conductor.createServer(dcf.getConHashMap(), svrip);
					Conductor.startServer();
					//20210204, 0210427 MatsudairaSyuMe Log Forging change remove final
					String logStr = String.format("sysgateii server after start conductor svrip=[%s]", svrip);
					//if (Constants.FilterNewlinePattern.matcher(logStr).find())
					logStr = "sysgateii server after start conductor";
					log.info(logStr);
				}
				//----
			}
			//----
			//----
			//20201119 add conductor mode function
			//20200926
			if (dcf.isReadCfgFromCenter() && !isConductor.get()) {
				svrstatustbname = dcf.getConHashMap().get("system.svrstatustb[@name]");
				svrsstatustbmkey = dcf.getConHashMap().get("system.svrstatustb[@mkey]");
				svrstatustbfields = dcf.getConHashMap().get("system.svrstatustb[@fields]");
				//20201116 change to use given svrid
//				log.info("sysgateii server start complete! auid=[{}]",dcf.getAuid());
				//----
				if (jsel2ins == null)
					//20201116
					//jsel2ins = new GwDao(PrnSvr.dburl, PrnSvr.dbuser, PrnSvr.dbpass, false);
					jsel2ins = new GwDao(dburl, dbuser, dbpass, false);
				//----
				//AUID,BRNO,IP,CURSTUS,PID,CREATOR,MODIFIER,LASTUPDATE
//				auid = dcf.getAuid();
//				svrip = dcf.getSvrip();
				// 20210702 MatsudairaSyuMe Log Forging
				// 20210714 MatsudairaSyuMe Log Forging
				//String logStr = String.format("sysgateii server start complete! auid=[%s] svrip=[%s]", auid, svrip);
				log.info("sysgateii server start complete!");//log.info(StrUtil.convertValidLog(logStr));
				if (auid.trim().length() == 0 || auid.trim().length() == 0) {
					auid = "0";
					setIsShouldShutDown(true);
				}
				//----
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
				String t = sdf.format(new java.util.Date());
				//20201116 cancel verbrno
//				String updValue = String.format(updValueptrn,auid,PrnSvr.verbrno, svrip,
//						Constants.STSUSEDACT, ManagementFactory.getRuntimeMXBean().getName().split("@")[0],t);
				//20201116 change to use given svrid
				String updValue = String.format(updValueptrn,auid, svrip,
						isShouldShutDown.get()?Constants.STSUSEDINACT:Constants.STSUSEDACT, ManagementFactory.getRuntimeMXBean().getName().split("@")[0],t);
				//----
				//20201116 use given svrid
				//int row = jsel2ins.UPSERT(svrstatustbname, svrstatustbfields, updValue, svrsstatustbmkey, PrnSvr.svrid);
				int row = jsel2ins.UPSERT(svrstatustbname, svrstatustbfields, updValue, svrsstatustbmkey, svrIdStr);
				//
				log.debug("total {} records update", row);
				jsel2ins.CloseConnect();
				jsel2ins = null;
				//----
			} else
				log.info("sysgateii server start complete!");
			//----
			while (!isShouldShutDown.get()) {
				TimeUnit.SECONDS.sleep(TEST_TIME_SECONDS);
			}
			log.info("AutoServer server stop!");
			//20201116
			System.exit(0);
			//----
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}

	}

	public static void addDaemonShutdownHook() {
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				log.debug("ShutdownHook run!!!");
				// 20201119 add conduction functoon
				setIsShouldShutDown(true);
				if (!isConductor.get()) {
//					PrnSvr.stopServer();
//            	WebServer.stopServer();
					// 20200926
					try {
						if (jsel2ins == null)
							// 20201116
							// jsel2ins = new GwDao(PrnSvr.dburl, PrnSvr.dbuser, PrnSvr.dbpass, false);
							jsel2ins = new GwDao(dburl, dbuser, dbpass, false);
						// ----
						// AUID,BRNO,IP,CURSTUS,PID,CREATOR,MODIFIER,LASTUPDATE
						SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
						String t = sdf.format(new java.util.Date());
						// 20201116 cancel verbrno
//					String updValue = String.format(updValueptrn, auid, PrnSvr.verbrno, svrip, Constants.STSUSEDINACT,
//							ManagementFactory.getRuntimeMXBean().getName().split("@")[0],t);
						String updValue = String.format(updValueptrn, auid, svrip, Constants.STSUSEDINACT,
								ManagementFactory.getRuntimeMXBean().getName().split("@")[0], t);
						// ----
						// 20201116 use given svrid
//					int row = jsel2ins.UPSERT(svrstatustbname, svrstatustbfields, updValue, svrsstatustbmkey,
//							PrnSvr.svrid);
						int row = jsel2ins.UPSERT(svrstatustbname, svrstatustbfields, updValue, svrsstatustbmkey,
								svrIdStr);
						// ----
						log.debug("total {} records update  status [{}]", row, Constants.STSUSEDINACT);
						jsel2ins.CloseConnect();
						jsel2ins = null;

					} catch (Exception e) {
						e.printStackTrace();
						log.error(e.getMessage());
					}
					// ----
				}
				//20201119
				else {
					Conductor.stopServer();
				}
				//----
			}
		});
	}

	public static void sleep(int t) {
		try {
			Thread.sleep(t * 3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static AtomicBoolean getIsShouldShutDown() {
		return isShouldShutDown;
	}

	public static void setIsShouldShutDown(boolean shutStatus) {
		Server.isShouldShutDown.set(shutStatus);
	}

	//20201116 used given svrid
	/**
	 * @return the svrId
	 */
	public static int getSvrId() {
		return svrId;
	}

	/**
	 * @param svrId the svrId to set
	 */
	public static void setSvrId(int svrId) {
		Server.svrId = svrId;
	}
	//----

	//20201119 Conductor mode function
	/**
	 * @return the isConductor
	 */
	public static AtomicBoolean getIsConductor() {
		return isConductor;
	}

	/**
	 * @param isConductor the isConductor to set
	 */
	public static void setIsConductor(boolean isConductor) {
		Server.isConductor.set(isConductor);
	}
	//20210909 MatsudairaSyuMe for ConductorResstorMode
	/**
	 * @return the isConductorRestore
	 */
	public static boolean getIsConductorRestore() {
		return isConductorRestore.get();
	}

	/**
	 * @param isConductorRestore the isConductorRestore to set
	 */
	public static void setIsConductorRestore(boolean isConductorRestore) {
		Server.isConductorRestore.set(isConductorRestore);;
	}
	//----
}
