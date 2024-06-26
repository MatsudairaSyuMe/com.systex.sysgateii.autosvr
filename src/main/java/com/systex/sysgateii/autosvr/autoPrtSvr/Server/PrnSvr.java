package com.systex.sysgateii.autosvr.autoPrtSvr.Server;

/**
 * 
 * Created by MatsudairaSyume 2019/11/5
 */

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
//20201006
import java.util.Collections;
//----
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;

/*
 * PrnSvr
 * Print Server controller
 *    
 * MatsudairaSyuMe
 * Ver 1.0
 *  20191126 
 */

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.systex.sysgateii.autosvr.autoPrtSvr.Client.PrtCli;
import com.systex.sysgateii.autosvr.comm.Constants;
import com.systex.sysgateii.autosvr.conf.DynamicProps;
import com.systex.sysgateii.autosvr.dao.GwDao;
import com.systex.sysgateii.autosvr.listener.EventType;
import com.systex.sysgateii.autosvr.listener.MessageListener;
import com.systex.sysgateii.autosvr.util.Big5FontImg;
import com.systex.sysgateii.autosvr.util.DateTimeUtil;
import com.systex.sysgateii.autosvr.util.StrUtil;
import com.systex.sysgateii.comm.sdk.RouteConnection;

public class PrnSvr implements MessageListener<byte[]> {
	private static Logger log = LoggerFactory.getLogger(PrnSvr.class);
	//20201115
//	public static Logger amlog = null;
//	public static Logger atlog = null;
	//----
	public static Big5FontImg big5funt = null;
	public static AtomicBoolean p_fun_flag = new AtomicBoolean(false);
	public static String dburl = "";
	public static String dbuser = "";
	public static String dbpass = "";
	public static String statustbname = "";
	public static String statustbmkey = "";
	public static String statustbfields = "";
	//20200815
	public static String svrid = "";
	//20201115
	public static String bkno = "";
	//----
	public static String svrtbsdytbname = "";
	public static String svrtbsdytbmkey = "";
	public static String svrtbsdytbfields = "";
	//----
	//20201026 cmdhis and am error log
	public static String devcmdhistbname = "";
	public static String devcmdhistbsearkey = "";
	public static String devcmdhistbfields = "";
	//20201119 add EMPNO
	private String hisfldvalssptrn = "%s,%s,'%s','%s','%s','%s','%s'";
	private String hisfldvalssptrn2 = "'%s','%s','%s','%s','%s'";
	private String hisfldvalssptrn3 = "'%s','%s','%s','%s','%s','%s','%s','%s','%s'";
	//----
	//20201028
	//20201119 add EMPNO
	private String hisfldvalssptrn4 = "%s,%s,'%s','%s','%s','%s','%s','%s'";
	//----
	public static String devamtbname = "";
	public static String devamtbsearkey = "";
	public static String devamtbfields = "";

	//----
	static PrnSvr server;
	public static String logPath = "";
	static FASSvr fasDespacther;
	static ConcurrentHashMap<String, Object> cfgMap = null;
	static List<ConcurrentHashMap<String, Object>> list = null;
	//20200901
	private static PrnSvr me;
	//20201006
	Map<String, Thread> threadMap = Collections.synchronizedMap(new LinkedHashMap<String, Thread>());
	List<ConcurrentHashMap<String, Object>> lastcfglist = null;
	//----
	static List<Thread> threadList = Collections.synchronizedList(new ArrayList<Thread>());
	Map<String, PrtCli> nodeList = Collections.synchronizedMap(new LinkedHashMap<String, PrtCli>());
	Thread monitorThread;
	//20220607 MatsudairaSyuMe jdawcon, cmdhiscon set to local parameter
	private GwDao jdawcon = null;
	//20201226
	private GwDao cmdhiscon = null;
	//----
	public static String cmdtbname = "";
	public static String cmdtbsearkey = "";
	public static String cmdtbfields = "";
	//20201106
	public static String dmtbname = "";
	public static String dmtbsearkey = "";
	public static String dmtbfields = "";
	//----
	//20201116 cancel verbno
	//public static String verbrno = "";
	//----
	public static int setResponseTimeout = 60 * 1000;// 毫秒
	//20201006
	static DynamicProps dcf = null;
	//----
	//20220429  MatsudairaSyuMe
	private static int reqTime = 250; //miniseconds
	private static int chgidleTime = 60; //seconds
	//----
	//20220905 MatsudairaSyuMe use RouteConnection as dispatcher
	private static RouteConnection dispatcher = null;
	//--

	public PrnSvr() {
		log.info("[0000]:=============[Start]=============");
	}

	@Override
	public void messageReceived(String serverId, byte[] msg) {
		// TODO Auto-generated method stub
		log.debug("msg received");
	}
	public void stop()
	{
		log.debug("Enter stop");
	}

	//20200901
	//20210627
//	public static void createServer(DynamicProps cfg, FASSvr setfassvr)
	public static void createServer(DynamicProps cfg) {
		//20210627 change orignail createServer to loadConfig
//		createServer(cfg);
		loadConfig(cfg);
		//20210627 mark to use MDP
//		fasDespacther = setfassvr;
		log.info("[0000]:------Call MaintainLog OK------");
		//20201115 mark atlog
		//20240523 Poor Style: Value Never Read RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
		//20240510 Poor Style: Value Never Read String jvmName = bean.getName();
		//20240503 mark for no use String pid = jvmName.split("@")[0];
		MDC.put("WSNO", "0000");
		log.info("[0000]:------MainThreadId------");//20240503 change log message
		//20201115mark atlog
		try {
			Thread thread;
			PrtCli conn;
			threadList.clear();
			getMe().nodeList.clear();
			if (list != null && list.size() > 0) {
				//20220905 MatsudairaSyuMe use RouteConnection as dispatcher
				dispatcher = new RouteConnection("127.0.0.1", 5555, new Timer());
				for (int i = 0; i < list.size(); i++) {
					synchronized (getMe()) {
						//20201006
						cfgMap = list.get(i);
						//20210628 use MDP
//						conn = new PrtCli(cfgMap, fasDespacther, new Timer());
						//conn = new PrtCli(cfgMap, new Timer());
						//20220905 MatsudairaSyuMe using RouteConnection as dispatcher
						//conn = new PrtCli(cfgMap, new Timer());
						conn = new PrtCli(cfgMap, dispatcher, new Timer());
						//---- 20220905 using RouteConnection as dispatcher
						thread = new Thread(conn);
						getMe().threadMap.put(conn.getId(), thread);
						//----
						getMe().nodeList.put(conn.getId(), conn);
					}
				}
				//20201006
				//for Java8 approach
				threadList = getMe().threadMap.values().stream().collect(Collectors.toList());
				//----
			}
		} catch (Exception e) {
			//20240503 MatsudairaSyuMe mark for System Information Leak e.printStackTrace();
			log.error("create server error");  //20240503 change log message
		}
	}

	public void start() {
		try {
			//20210914 MatsudairaSyuMe change for threads of printer and monitor separately running
			if (!threadList.isEmpty()) {
				for (Thread t : threadList) {
					t.start();
					log.info("thread [{}] start", t.getName());
				}
			}
			//20240205 MatsudairaSyuMe add closeEachNodesDaemonShutdownHook
			closeEachNodesDaemonShutdownHook();
			//--
			//----
			monitorThread = new Thread(new Runnable() {
				@Override
				public void run() {
					// 20220607 MatsudairaSyuMe
					try {
						if (jdawcon == null) {
							jdawcon = new GwDao(dburl, dbuser, dbpass, false);
							//20220613 MatsudairaSyuMe
							String selfld = "";
							String selkey = "";
							//20240510 Poor Style: Value Never Read String[] sno = null;
							if (PrnSvr.cmdtbfields.indexOf(',') > -1) {
								selfld = PrnSvr.cmdtbfields.substring(PrnSvr.cmdtbfields.indexOf(',') + 1);
								selkey = PrnSvr.cmdtbfields.substring(0, PrnSvr.cmdtbfields.indexOf(','));
							} else {
								selfld = PrnSvr.cmdtbfields;
								selkey = PrnSvr.cmdtbsearkey;
							}
//							log.info("initial select devcmdtbl [{}]", jdawcon.SELMFLD_R(PrnSvr.cmdtbname, selfld, selkey, "?", true));
							jdawcon.SELMFLD_R(PrnSvr.cmdtbname, selfld, selkey, "?", true);
							log.info("initial delete SVRID devcmdtbl [{}]", jdawcon.DELETETB_R(PrnSvr.cmdtbname, "SVRID,BRWS", "?,?", true));
							//----
						}
						/* 20230517 MatsudairaSyuMe mark up for closing connection after access db 
						if (cmdhiscon == null)
							cmdhiscon = new GwDao(dburl, dbuser, dbpass, false);
						*/
						// 20230517 MatsudairaSyuMe add for closing connection after access db
						if (jdawcon != null) {
							try {
								jdawcon.CloseConnect();
							} catch (Exception any) {
								//20240503 MatsudairaSyuMe mark for System Information Leak any.printStackTrace();
								log.error("jdawcon close error ignore");
							}
							jdawcon = null;
						}
						// 20230517 ----
					// ----
					while (true) {
						log.info("monitorThread");
						if (PrnSvr.dburl != null && PrnSvr.dburl.trim().length() > 0) {
							String selfld = "";
							String selkey = "";
							String[] sno = null;
							if (PrnSvr.cmdtbfields.indexOf(',') > -1) {
								selfld = PrnSvr.cmdtbfields.substring(PrnSvr.cmdtbfields.indexOf(',') + 1);
								selkey = PrnSvr.cmdtbfields.substring(0, PrnSvr.cmdtbfields.indexOf(','));
							} else {
								selfld = PrnSvr.cmdtbfields;
								selkey = PrnSvr.cmdtbsearkey;
							}
							try {
								// 20220607 MatsydairaSyuMe, 20230517 take out mark for closing connection after access db 
								jdawcon = new GwDao(PrnSvr.dburl, PrnSvr.dbuser, PrnSvr.dbpass, false);
								// 20230517 take out mark
								log.debug("current selfld=[{}] selkey=[{}] cmdtbsearkey=[{}]", selfld, selkey, PrnSvr.svrid);
								//20220613 MatsudairasyuMe Change to use reused prepared statement, 20230517 take out mark for closing connection after access db 
								String[] cmd = jdawcon.SELMFLD(PrnSvr.cmdtbname, selfld, selkey, PrnSvr.svrid, false);
								//String[] cmd = jdawcon.SELMFLD_R(PrnSvr.cmdtbname, selfld, selkey, PrnSvr.svrid, false);20230517 mark up for closing connection after access db 
								if(cmd != null && cmd.length > 0)
									for (String s: cmd) {
										s = s.trim();
										log.atDebug().setMessage("current row cmd [{}]").addArgument(s).log();//20240517 change for Log Forging(debug)
										if (s.length() > 0 && s.indexOf(',') > -1) {
											String[] cmdary = s.split(",");
											//20201006
//											log.debug("cmd object node=[{}] curState=[{}] cmd getCurMode=[{}]", getMe().nodeList.get(cmdary[0]).getId(), getMe().nodeList.get(cmdary[0]).getCurState(), getMe().nodeList.get(cmdary[0]).getCurMode());
											//----
											if (cmdary.length > 1) {
												//20201026
												int idx = 0;
												String sts = "0";
												//20201028
												sno = null;
												boolean createNode = false;
												boolean restartAlreadyStop = false;
												if (DateTimeUtil.MinDurationToCurrentTime(3,cmdary[3])) {
													log.atDebug().setMessage("brws=[{}] keep in cmd table longer then 3 minutes will be cleared").addArgument(cmdary[0]).log();//20240517 change for Log Forging(debug)
													if (cmdary[1].trim().length() > 0) {
														log.atDebug().setMessage("brws=[{}] cmd[{}] not execute will be marked fail in cmdhis").addArgument(cmdary[0]).addArgument( cmdary[1]).log();//20240517 change for Log Forging(debug)
														/*20220607 MatsudairaSyuMe, 20230517 take out mark for closing connection after access db */
														if (cmdhiscon == null)
															cmdhiscon = new GwDao(PrnSvr.dburl, PrnSvr.dbuser, PrnSvr.dbpass, false);
														/* 20230517 take out mark*/
														//20201119
														String[] chksno = cmdhiscon.SELMFLD(PrnSvr.devcmdhistbname, "SNO", "BRWS,CMD,CMDCREATETIME", "'" + cmdary[0] + "','"+ cmdary[1] + "','"+ cmdary[3]+ "'", false);
														SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
														String t = sdf.format(new java.util.Date());
														//20201119
														String failfldvals = String.format(hisfldvalssptrn4, PrnSvr.svrid, cmdary[2], cmdary[0],cmdary[1],cmdary[3],"FAIL",t,cmdary[4]);
														if (chksno == null || chksno.length == 0) {
															chksno = new String[1];
															chksno[0] = "-1";
														}
														cmdhiscon.INSSELChoiceKey(PrnSvr.devcmdhistbname, "SVRID,AUID,BRWS,CMD,CMDCREATETIME,CMDRESULT,CMDRESULTTIME,EMPNO", failfldvals, PrnSvr.devcmdhistbsearkey, chksno[0], false, false);//20240510 Poor Style: Value Never Read for sno
														/*20220607 MatsudairaSyuMe, 20230517 take out mark for closing connection after access db */
														cmdhiscon.CloseConnect();
														cmdhiscon = null;
														/* 2023016 take out mark */
														//20240510 Poor Style: Value Never Read sno = null;
													}
													/*20230517 mark up for closing connection after access db 
													jdawcon.DELETETB_R(PrnSvr.cmdtbname, "SVRID,BRWS",PrnSvr.svrid+",'" + cmdary[0] + "'", false);  //20220613 change to use reused statement
													*/
													jdawcon.DELETETB(PrnSvr.cmdtbname, "SVRID,BRWS",PrnSvr.svrid+",'" + cmdary[0] + "'");
													// 20230517 MatsudairaSyuMe add for closing connection after access db
													if (jdawcon != null) {
														try {
															jdawcon.CloseConnect();
														} catch (Exception any) {
															//20240503 MatsudairaSyuMe mark for System Information Leak any.printStackTrace();
															log.error("jdawcon close error ignore");
														}
														jdawcon = null;
													}
													// 20230517 ----
													continue;
												}
												//----
												for (String ss: cmdary)
													log.debug("cmd[{}]=[{}]",idx++, ss);
												String curcmd = ""; curcmd = cmdary[1].trim().toUpperCase();//20240523 prevent Redundant Null Check

												//20201026 for cmdhis
												if (curcmd != null && curcmd.length() > 0) {
													/*20220607 MatsudairaSyuMe, 20230517 take out mark for closing connection after access db */
													cmdhiscon = new GwDao(PrnSvr.dburl, PrnSvr.dbuser, PrnSvr.dbpass, false);
													/* 20240516 take out mark */
													if (getMe().nodeList != null && getMe().nodeList.size() > 0) {
														if (getMe().nodeList.containsKey(cmdary[0])) {
															//20210204, 20210714 MatsudairaSyuMe Log Forging
															//final String logStr = String.format("!!! cmd object node=[%s] already in nodeList please STOP this node before START !!!", cmdary[0]);
															//if (Constants.FilterNewlinePattern.matcher(logStr).find())
															log.error("!!! cmd object node already in nodeList please STOP this node before START !!! check dashboard");
															//else
															//	log.error(logStr);
															if (getMe().nodeList.get(cmdary[0]).getCurState() >= 0)
																sts = "2";
															createNode = false;
														} else {
															log.atDebug().setMessage("!!! cmd object node=[{}] not in nodeList will be created").addArgument(cmdary[0]).log();//20240517 change for Log Forging(debug)
															createNode = true;
														}
													}
													String fldvals = String.format(hisfldvalssptrn, PrnSvr.svrid, cmdary[2], cmdary[0],cmdary[1],cmdary[3],sts,cmdary[4]);
													//20201028 check sno if command already insert to cmdhis
													String[] chksno = cmdhiscon.SELMFLD(PrnSvr.devcmdhistbname, "SNO", "BRWS,CMD,CMDCREATETIME", "'" + cmdary[0] + "','"+ cmdary[1] + "','"+ cmdary[3]+ "'", false);
//													log.debug("chksno=[{}]",chksno);
//													if (chksno != null && chksno.length > 0 && Integer.parseInt(chksno[0].trim()) > -1) {//20240527 prevent Redundant Null Check
													if (chksno.length > 0 && Integer.parseInt(chksno[0].trim()) > -1) {//20240527 prevent Redundant Null Check
														for (String sss: chksno)
															log.atDebug().setMessage("sno[{}] already exist").addArgument(sss).log();//20240517 change for Log Forging(debug)
														//20210413 MatsudairaSyuMe prevent Portability Flaw: Locale Dependent Comparison change equals to 
														if (curcmd.equalsIgnoreCase("RESTART")) { // current command is RESTART check cmdhis if already done STOP
															for (int i = 0; i < chksno.length; i++) {
																String chkcmdresult = cmdhiscon.SELONEFLD(PrnSvr.devcmdhistbname, "CMDRESULT", "SNO", chksno[0], false);
																log.atDebug().setMessage("table sno=[{}] cmdhis cmd is RESTART and cmdresult=[{}]").addArgument(chksno[i]).addArgument(chkcmdresult).log();//20240517 change for Log Forging(debug)
																if (chkcmdresult != null && chkcmdresult.equals("STOP")) {//20240527 Redundant Null Check
																	if (!restartAlreadyStop) {
																		sno = null; // prepared to start new node
																		restartAlreadyStop = true;
																		sleep(4);  //20230517 MatsudairasyuMe PrtCli add 3 second delay wait time
																	} else {
																		sno = new String[1];
																		sno[0] = chksno[i];																																				
																	}
																	log.atDebug().setMessage("table son=[{}] chksno=[{}] cmdhis cmd is RESTART and cmdresult=[{}] restartAlreadyStop=[{}]").addArgument(sno).addArgument(chksno[i]).addArgument(chkcmdresult).addArgument(restartAlreadyStop).log();//20240517 change for Log Forging(debug)
																} else {
																// current command is RESTART and waiting to STOP or already set ACTIVE waiting to finish
																	sno = new String[1];
																	sno[0] = chksno[i];																	
																}
															}
														} else {
															// current command is not RESTART and waiting to finish
															sno = new String[1];
															sno[0] = chksno[0];
														}
													}
													if (sno == null) {// first time receive command insert new record to cmdhis
														//20201119 add EMPNO
														sno = cmdhiscon.INSSELChoiceKey(PrnSvr.devcmdhistbname, "SVRID,AUID,BRWS,CMD,CMDCREATETIME,CURSTUS,EMPNO", fldvals, PrnSvr.devcmdhistbsearkey, "-1", false, false);
														if (sno != null) {
															for (int i = 0; i < sno.length; i++)
																log.debug("sno[{}]=[{}]",i,sno[i]);
															} else
																log.error("sno null");
													}
													//----
												}
												//----
												log.atDebug().setMessage("table sno=[{}] createNode=[{}] restartAlreadyStop=[{}]").addArgument((sno == null ? 0: sno[0])).addArgument(createNode).addArgument(restartAlreadyStop).log();//20240517 change for Log Forging(debug)
												//20210413 MatsudairaSyuMe prevent Null Dereference
												if (sno == null) {
													sno = new String[1];
													sno[0] = "";
												}
												//----
												//20210426 MatsudairaSyuMe prevent Portability Flaw: Locale Dependent Comparison
												int selCmd = Constants.UNKNOWN;
												if (curcmd != null) {
												if (curcmd.equalsIgnoreCase("START"))//20240528 Redundant Null Check
													selCmd = Constants.START;
												else if(curcmd.equalsIgnoreCase("STOP"))
													selCmd = Constants.STOP;
												else if(curcmd.equalsIgnoreCase("RESTART"))
													selCmd = Constants.RESTART;
												}
												switch (selCmd) {//20210426 MatsudairaSyuMe prevent Portability Flaw: Locale Dependent Comparison
												case Constants.START://20210426 MatsudairaSyuMe prevent Portability Flaw: Locale Dependent Comparison
													//20201006, 20201026 cmdhis, 20220905 change function name to create Nodefun
													sleep(4);  //20230517 MatsudairasyuMe PrtCli add 3 second delay wait time
													createNodefun(cmdary[0]);
													//----
													if (getMe().nodeList.get(cmdary[0]).getCurState() == -1) {
														//20201026 for cmdhis
														getMe().nodeList.get(cmdary[0]).onEvent(getMe().nodeList.get(cmdary[0]).getId(), EventType.ACTIVE, sno[0]);
														log.debug("cmd object node=[{}] enable session getCurMode=[{}]", getMe().nodeList.get(cmdary[0]).getId(), getMe().nodeList.get(cmdary[0]).getCurMode());
													} else {
														SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
														String t = sdf.format(new java.util.Date());
														int row = jdawcon.UPDT(PrnSvr.cmdtbname, "CMD, CMDRESULT,CMDRESULTTIME", "'','START','" + t + "'",
																"SVRID,BRWS", PrnSvr.svrid + "," + cmdary[0]);
														//----
														log.debug("total {} records update", row);
														log.debug("cmd object node=[{}] already active!!!! getCurMode=[{}]", getMe().nodeList.get(cmdary[0]).getId(), getMe().nodeList.get(cmdary[0]).getCurMode());
														//20201026
//														String fldvals2 = String.format(hisfldvalssptrn2, "", cmdary[1], t, sts);
														PrtCli conn = getMe().nodeList.get(cmdary[0]);
														//20201119 add EMPNO
														//20201218 add original cmd to devcmdhis
														String fldvals3 = String.format(hisfldvalssptrn3, cmdary[1], cmdary[1], t, conn.getRemoteHostAddr(),//20210427 MatsudairaSyuMe Often Misused: Authentication
																conn.getRmtaddr().getPort(),conn.getLocalHostAddr(), conn.getLocaladdr().getPort(),sts,cmdary[4]);
														//----
														//---
//														sno = cmdhiscon.INSSELChoiceKey(PrnSvr.devcmdhistbname, "SVRID,AUID,BRWS,CMD,CMDCREATETIME,CMDRESULT,CMDRESULTTIME,CURSTUS", "1,1,'9838901','','2020-10-21 09:46:38.368000','START','2020-10-21 09:46:38.368000','0','2'", "SNO", "31", false, true);
														sno = cmdhiscon.INSSELChoiceKey(PrnSvr.devcmdhistbname, "CMD,CMDRESULT,CMDRESULTTIME,DEVIP,DEVPORT,SVRIP,SVRPORT,RESULTSTUS,EMPNO", fldvals3, PrnSvr.devcmdhistbsearkey, sno[0], false, true);
														//----
														if (sno != null) {
															for (int i = 0; i < sno.length; i++)
																log.debug("sno[{}]=[{}]",i,sno[i]);
														} else
															log.error("sno null");
														//----
													}
													break;
												case Constants.STOP://20210426 MatsudairaSyuMe prevent Portability Flaw: Locale Dependent Comparison
													if (getMe().nodeList.get(cmdary[0]).getCurState() != -1) {
														//20201026 for cmdhis
														getMe().nodeList.get(cmdary[0]).onEvent(getMe().nodeList.get(cmdary[0]).getId(), EventType.SHUTDOWN, sno[0]);
														log.debug("cmd object node=[{}] stop session getCurMode=[{}]", getMe().nodeList.get(cmdary[0]).getId(), getMe().nodeList.get(cmdary[0]).getCurMode());
													} else {
														SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
														String t = sdf.format(new java.util.Date());
														int row = jdawcon.UPDT(PrnSvr.cmdtbname, "CMD, CMDRESULT,CMDRESULTTIME", "'','STOP','" + t + "'",
																"SVRID,BRWS", PrnSvr.svrid + "," + cmdary[0]);
														log.debug("total {} records update", row);
														log.debug("cmd object node=[{}] already shutdown!!!! getCurMode=[{}]", getMe().nodeList.get(cmdary[0]).getId(), getMe().nodeList.get(cmdary[0]).getCurMode());
														//20201026
														//20201119 add EMPNO
														//20201218 add original cmd to devcmdhis
														String fldvals2 = String.format(hisfldvalssptrn2, cmdary[1], cmdary[1], t, sts,cmdary[4]);
														//----
														sno = cmdhiscon.INSSELChoiceKey(PrnSvr.devcmdhistbname, "CMD,CMDRESULT,CMDRESULTTIME,RESULTSTUS,EMPNO", fldvals2, PrnSvr.devcmdhistbsearkey, sno[0], false, true);
														//----
														if (sno != null) {
															for (int i = 0; i < sno.length; i++)
																log.debug("sno[{}]=[{}]",i,sno[i]);
														} else
															log.error("sno null");
														//----
														}
													break;
												case Constants.RESTART://20210426 MatsudairaSyuMe prevent Portability Flaw: Locale Dependent Comparison
													//20201221 1st time RESTART mode node already STOP
													if (!restartAlreadyStop && createNode)
														restartAlreadyStop = true;
													//----
												//20201028 add cmdhis
													if (!restartAlreadyStop && !createNode && getMe().nodeList.get(cmdary[0]).getCurState() != -1) {
														getMe().nodeList.get(cmdary[0]).onEvent(getMe().nodeList.get(cmdary[0]).getId(), EventType.RESTART, sno[0]);
														log.debug("cmd object node=[{}] stop session getCurMode=[{}]", getMe().nodeList.get(cmdary[0]).getId(), getMe().nodeList.get(cmdary[0]).getCurMode());
													} else {
														createNodefun(cmdary[0]);  //20220905 change function name to createNodefun
														//----
														if (getMe().nodeList.get(cmdary[0]).getCurState() == -1) {
															getMe().nodeList.get(cmdary[0]).onEvent(getMe().nodeList.get(cmdary[0]).getId(), EventType.ACTIVE, sno[0]);
															log.debug("cmd object node=[{}] enable session getCurMode=[{}]", getMe().nodeList.get(cmdary[0]).getId(), getMe().nodeList.get(cmdary[0]).getCurMode());
														} else {
															SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
															String t = sdf.format(new java.util.Date());
															int row = jdawcon.UPDT(PrnSvr.cmdtbname, "CMD, CMDRESULT,CMDRESULTTIME", "'','RESTART','" + t + "'",
																	"SVRID,BRWS", PrnSvr.svrid + "," + cmdary[0]);
															log.debug("total {} records update", row);
															log.debug("cmd object node=[{}] already active!!!! getCurMode=[{}]", getMe().nodeList.get(cmdary[0]).getId(), getMe().nodeList.get(cmdary[0]).getCurMode());
															PrtCli conn = getMe().nodeList.get(cmdary[0]);
															//20201119 add EMPNO
															//20201218 add original cmd to devcmdhis
															String fldvals3 = String.format(hisfldvalssptrn3, cmdary[1], cmdary[1], t, conn.getRemoteHostAddr(),//20210427 MatsudairaSyuMe Often Misused: Authentication
																	conn.getRmtaddr().getPort(),conn.getLocalHostAddr(), conn.getLocaladdr().getPort(),sts,cmdary[4]);
															//----
															sno = cmdhiscon.INSSELChoiceKey(PrnSvr.devcmdhistbname, "CMD,CMDRESULT,CMDRESULTTIME,DEVIP,DEVPORT,SVRIP,SVRPORT,RESULTSTUS,EMPNO", fldvals3, PrnSvr.devcmdhistbsearkey, sno[0], false, true);
															//----
															if (sno != null) {
																for (int i = 0; i < sno.length; i++)
																	log.debug("sno[{}]=[{}]",i,sno[i]);
															} else
																log.error("sno null");
														}
													}
													break;
												default:
													log.atDebug().setMessage("!!! cmd object node=[{}] cmd [{}] ignore").addArgument(cmdary[0]).addArgument(cmdary[1]).log();//20240517 change for Log Forging(debug)
													break;
												}
											} else
												log.atDebug().setMessage("!!! cmd object node=[{}] format error !!!").addArgument(cmdary[0]).log();//20240517 change for Log Forging(debug)													
										} else {
											// 20210714 MatsudairaSyuMe Log Forging
											//final String chks = StrUtil.convertValidLog(s);
											log.error("!!!current row cmd error"); //chks
										}
									}
								/*20220607 MatsudairaSyuMe, 20230517 MatsudairaSyuMe take out mark for closing connection after access db */
								jdawcon.CloseConnect();
								jdawcon = null;
								//20201026
								if (cmdhiscon != null)
									cmdhiscon.CloseConnect();
								cmdhiscon = null;
								//----
								 /*20230517 take out mark*/ 
							} catch (Exception e) {
								//20240503 MatsudairaSyuMe mark for System Information Leak e.printStackTrace();
								log.info("monitorThread read database error");//20240503 change log message
							}
						}
						sleep(3);
					} // while
					// 20220607 MatsudairaSyuMe
					} catch (Exception e) {
						//20240503 MatsudairaSyuMe mark for System Information Leak e.printStackTrace();
						log.error("jdawcon error exception ");//20240503 change log message
					} finally {
						if (jdawcon != null) {
							try {
								jdawcon.CloseConnect();
							} catch (Exception any) {
								//20240503 MatsudairaSyuMe mark for System Information Leak any.printStackTrace();
								log.error("jdawcon close error ignore");
							}
							jdawcon = null;
						}
						if (cmdhiscon != null)
							try {
								cmdhiscon.CloseConnect();
							} catch (Exception any) {
								//20240503 MatsudairaSyuMe mark for System Information Leak any.printStackTrace();
								log.error("cmdhiscon close error ignore");
							}
						cmdhiscon = null;
					}
				}
			});// monitorThread
			monitorThread.start();
			//20210914 MatsudairaSyuMe change for threads of printer and monitor separately running
		} catch (Exception e) {
			//20240503 MatsudairaSyuMe mark for System Information Leak e.printStackTrace();
			log.error("db exception");//20240503 change log message
		}
	}
	//20201006
	public static int closeNode(String nid, boolean isRemove) {
		int rtn = 0;
		log.debug("start current threadMap size=[{}]", getMe().threadMap.size());
		log.debug("start current nodeList size=[{}]", getMe().nodeList.size());
		if (!getMe().nodeList.containsKey(nid))
			log.atDebug().setMessage("!!! cmd object node=[{}] not found in nodeList !!!").addArgument(nid).log();//20240517 change for Log Forging(debug)
		else {
			synchronized(getMe())
			{
				log.atDebug().setMessage("!!! start to remove node=[{}] !!!").addArgument(nid).log();//20240517 change for Log Forging(debug)
				Thread t = getMe().threadMap.get(nid);
				try {
					t.interrupt();
					t.join(1 * 1000);
				} catch (InterruptedException e) {
					//20240503 MatsudairaSyuMe mark for System Information Leak e.printStackTrace();
					log.error("!!! error for stop thread for prtcli node !!!");//20240515 change log message
				}
				getMe().threadMap.remove(nid);
				getMe().nodeList.remove(nid);
				rtn += 1;
			}
		}
		log.debug("stop current threadMap size=[{}]", getMe().threadMap.size());
		log.debug("stop current nodeList size=[{}]", getMe().nodeList.size());
		return rtn;
	};
	//20220905 MAtsudairaSyyuMe change function name from createNode to createNodefun
	public int createNodefun(String nid) {
		int ret = 0;
		//20220905 MatsudairaSyuMe use RouteConnection as dispatcher
		if (dispatcher == null) {
			dispatcher = new RouteConnection("127.0.0.1", 5555, new Timer());
		}
		//--
		if (getMe().nodeList != null && getMe().nodeList.size() > 0) {
			if (getMe().nodeList.containsKey(nid)) {
				// 20210714 MatsudairaSyuMe Log Forging
				//final String chknid = StrUtil.convertValidLog(nid);
				log.error("!!! cmd object node already in nodeList please STOP this node before START !!!"); //chknid
				return ret;
			} else
				log.atDebug().setMessage("!!! cmd object node=[{}] not in nodeList will be created").addArgument(nid).log();//20240517 change for Log Forging(debug)
		}
		//20201021 mark
		log.debug("start current threadMap size=[{}] nodeList size=[{}]", getMe().threadMap.size(), getMe().nodeList.size());
		//----

		if (dcf != null)
			lastcfglist = dcf.getLastcfgPrtMapList();
		else
			log.info("dcf == null !!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		if (lastcfglist != null && lastcfglist.size() > 0) {
			log.info("lastcfglist.size() == [{}]", lastcfglist.size());
			for(int i=0; i < lastcfglist.size(); i++) {
				ConcurrentHashMap<String, Object> newcfgMap = lastcfglist.get(i);
				log.atDebug().setMessage("check brws cmd [{}] lastcfglist [{}]").addArgument(nid).addArgument(newcfgMap.get("brws")).log();//20240517 change for Log Forging(debug)
				if (nid.trim().equals(newcfgMap.get("brws"))) {
					log.atDebug().setMessage("prepare to create node brws [{}]").addArgument(newcfgMap.get("brws")).log();//20240517 change for Log Forging(debug)
					//20210628 use MDP
//					PrtCli conn = new PrtCli(newcfgMap, fasDespacther, new Timer());
					//20220905 MatsudairaSyuMe using RouteConnection as dispatcher
					//PrtCli conn = new PrtCli(newcfgMap, new Timer());
					PrtCli conn = new PrtCli(newcfgMap, dispatcher, new Timer());//20221204 cfgMap change to newcfgMap
					//---- 20220905 using RouteConnection as dispatcher
					Thread thread = new Thread(conn);
					getMe().threadMap.put(conn.getId(), thread);
					getMe().nodeList.put(conn.getId(), conn);
					ret += 1;
					thread.start();
					//20240510 Poor Style: Value Never Read i = lastcfglist.size();
					break;
				}
			}
		} else
			log.debug("create node return 0");
		//20201021 mark
		log.debug("stop current threadMap size=[{}] size=[{}]", getMe().threadMap.size(), getMe().nodeList.size());
		//----

		return ret;
	}
	//----
	public static void startServer() {
		log.debug("Enter startServer");
		getMe().start();
		//----
	}

	public static PrnSvr getMe() {
		if (me == null) {
			me = new PrnSvr();
		}
		return me;
	}
	//----
	//20210627 change orignail createServer to loadConfig
	public static void loadConfig(DynamicProps cfg) {
		//20210627 change orignail createServer to loadConfig
		log.debug("Enter loadConfig");
		//20201006
		dcf = cfg;
		//----
		cfgMap = null;
		list = cfg.getCfgPrtMapList();
		logPath = cfg.getConHashMap().get("system.logpath");
		String tout = cfg.getConHashMap().get("svrsubport.recvtimeout");
		if (tout != null && tout.trim().length() > 0) {
			setResponseTimeout = Integer.parseInt(tout);
		}
		log.debug("Enter createServer size={}", list.size());
		log.debug("receive timeout is ={} mili-seconds", setResponseTimeout);
		dburl = cfg.getConHashMap().get("system.db[@url]");
		dbuser = cfg.getConHashMap().get("system.db[@user]");
		dbpass = cfg.getConHashMap().get("system.db[@pass]");
		statustbname = cfg.getConHashMap().get("system.statustb[@name]");
		statustbmkey = cfg.getConHashMap().get("system.statustb[@mkey]");
		statustbfields = cfg.getConHashMap().get("system.statustb[@fields]");
		//20200815
		svrid = cfg.getConHashMap().get("system.svrid");
		bkno = cfg.getConHashMap().get("bkno").trim();
		//20201115
		//----
		svrtbsdytbname = cfg.getConHashMap().get("system.svrtbsdytb[@name]");
		svrtbsdytbmkey = cfg.getConHashMap().get("system.svrtbsdytb[@mkey]");
		svrtbsdytbfields = cfg.getConHashMap().get("system.svrtbsdytb[@fields]");
		//----
		//20200901
		cmdtbname = cfg.getConHashMap().get("system.devcmdtb[@name]");
		cmdtbsearkey = cfg.getConHashMap().get("system.devcmdtb[@mkey]");
		cmdtbfields = cfg.getConHashMap().get("system.devcmdtb[@fields]");
		//----
		//20201026 cmdhis and am error log
		devcmdhistbname = cfg.getConHashMap().get("system.devcmdhistb[@name]");
		devcmdhistbsearkey = cfg.getConHashMap().get("system.devcmdhistb[@mkey]");
		devcmdhistbfields = cfg.getConHashMap().get("system.devcmdhistb[@fields]");
		devamtbname = cfg.getConHashMap().get("system.devamtb[@name]");
		devamtbsearkey = cfg.getConHashMap().get("system.devamtb[@mkey]");
		devamtbfields = cfg.getConHashMap().get("system.devamtb[@fields]");
		//----
		
		//20220429 MatsudairaSyuMe
		String teststr = cfg.getConHashMap().get("reqtime").trim();
		if(StrUtil.isEmpty(teststr))
			setReqTime(Integer.parseInt("50"));
		else
			setReqTime(Integer.parseInt(teststr)); //202204 30 change from (Integer.parseInt(teststr) / 2) to Integer.parseInt(teststr)
		//20240510 Poor Style: Value Never Read teststr = "";
		teststr = cfg.getConHashMap().get("chgidletime").trim();
		if(StrUtil.isEmpty(teststr))
			setChgidleTime(Integer.parseInt("60"));
		else
			setChgidleTime(Integer.parseInt(teststr));
		//----

		//20201106
		dmtbname = cfg.getConHashMap().get("system.dmtb[@name]");
		dmtbsearkey = cfg.getConHashMap().get("system.dmtb[@mkey]");
		dmtbfields = cfg.getConHashMap().get("system.dmtb[@fields]");
		//----

		if (dburl != null && dburl.trim().length() > 0) {
			log.debug("will use db url:[{}] user name:[{}] update status table [{}] main key [{}] fields [{}]", dburl, dbuser, statustbname, statustbmkey, statustbfields);
			//20201115
			log.debug("check tbsdy from table [{}] main key [{}]=[{}] fields [{}]", svrtbsdytbname, svrtbsdytbmkey, bkno, svrtbsdytbfields);
		}
		log.debug("receive timeout is ={} mili-seconds", setResponseTimeout);
		MDC.put("WSNO", "0000");
		MDC.put("PID", ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
		//20240523 Poor Style: Value Never Read SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		//202450510 Poor Style: Value Never Read String byDate = sdf.format(new Date());
		//20201115
		//----
		try {
			p_fun_flag.set(false);
			//20211004 MatsudairaSyuMe change the path to "/biscon/tns"
//			big5funt = new Big5FontImg("FontTable_low.bin", "FontData_All.bin");
//20230522 MatsudairaSyuMe change the tns path from /biscon/tns to /tns
			big5funt = new Big5FontImg(File.separator + "tns" + File.separator + "FontTable_low.bin", File.separator + "tns" + File.separator +  "FontData_All.bin");
			//----
			p_fun_flag.set(true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			//20240503 MatsudairaSyuMe mark for System Information Leak e.printStackTrace();
			log.error("Font file name error");//20230522 MatsudairaSyuMe change the tns path from /biscon/tns to /tns
		}
		log.debug("p_fun_flag={}", p_fun_flag);
//20200901
//		server = new PrnSvr();
		server = getMe();
		//----
	}

	public static void stopServer() {
		log.debug("Enter stopServer");
		if (server != null) {
			server.stop();
		}
	}

	public static void sleep(int t) {
		try {
			TimeUnit.SECONDS.sleep(t);
		} catch (InterruptedException e) {
			//20240503 MatsudairaSyuMe mark for System Information Leak e.printStackTrace();
		}
	}

	//20220429 MatsudaioraSyuMe
	public static int getReqTime() {
		return PrnSvr.reqTime;
	}

	public static void setReqTime(int reqTime) {
		PrnSvr.reqTime = reqTime;
	}

	public static int getChgidleTime() {
		return chgidleTime;
	}

	public static void setChgidleTime(int chgidleTime) {
		PrnSvr.chgidleTime = chgidleTime;
	}
	//-----

	//20240205 stop each node
	private void closeEachNodesDaemonShutdownHook() {
		log.debug("add closeEachNodesDaemonShutdownHook");
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				log.debug("closeEachNodesDaemonShutdownHook run!!!");
				for (Map.Entry<String, PrtCli> entry : getMe().nodeList.entrySet()) {
					if (getMe().nodeList.get( entry.getKey()).getCurState() != -1)
						getMe().nodeList.get( entry.getKey()).saveSeqFile(getMe().nodeList.get( entry.getKey()).getId());
				}
			}
		});
	}

	//----
}
