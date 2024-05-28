package com.systex.sysgateii.autosvr.Monster;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
/******************
 * MatsudairaSyume
 * 20201119
 * Conductor initial service program
 */
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//20210909 MatsudairaSyuMe conductor only
import com.systex.sysgateii.autosvr.Server;
//---
import com.systex.sysgateii.autosvr.comm.Constants;
import com.systex.sysgateii.autosvr.dao.GwDao;
import com.systex.sysgateii.autosvr.util.DateTimeUtil;
//import com.systex.sysgateii.autosvr.util.StrUtil; 20210909 markup

public class Conductor implements Runnable {
	private static Logger log = LoggerFactory.getLogger(Conductor.class);
	private static String svrip = "";
	private static String dburl = "";
	private static String dbuser = "";
	private static String dbpass = "";
	private static String svrprmtb = "";
	//storing all configuration parameters
	static ConcurrentHashMap<String, String> map;
	//storing all svrid for this Conductor
	static Map<String, String> svridnodeMap = Collections.synchronizedMap(new LinkedHashMap<String, String>());
	static GwDao jsel2ins = null;
	static Conductor server;
	//20220607 MatsudairaSyuMe jdawcon, cmdhiscon set to local parameter
	private GwDao jdawcon = null;
	private GwDao cmdhiscon = null;
	private String hisfldvalssptrn = "%s,'%s','%s','%s', '%s'";
	//update svrcmdhis fail
	private String hisfldvalssptrn4 = "%s,'%s','%s','%s','%s','%s', '%s'";

	public static void sleep(int t) {
		try {
			Thread.sleep(t * 1000);
		} catch (InterruptedException e) {
			//20250503 MatsidairaSyuMe mark for System Information Leak e.printStackTrace();
		}
	}
	public static void createServer(ConcurrentHashMap<String, String> _map, String _svrip) {
		setSvrip(_svrip);
		log.debug("Enter createServer Conductor ip=[{}]", getSvrip());
		map = _map;
		dburl = map.get("system.db[@url]");
		dbuser = map.get("system.db[@user]");
		dbpass = map.get("system.db[@pass]");
		svrprmtb = map.get("system.svrprmtb[@name]").trim();
		Conductor.svridnodeMap.clear();
	}

	public static void startServer() {
		log.debug("Enter startServer Conductor check table[{}] svrnodelist size=[{}]", svrprmtb,
				Conductor.svridnodeMap.size());
		// 20210828 MatsudairaSyuMe start conductor only
//		if (!getSvrip().equalsIgnoreCase("r")) {  20210909 mark up
			// ----
		try {
			jsel2ins = new GwDao(dburl, dbuser, dbpass, false);
			String[] svrflds = jsel2ins.SELMFLD(svrprmtb, "SVRID", "IP", "'" + getSvrip() + "'", false);
			if (svrflds != null && svrflds.length > 0) {
				for (String s : svrflds) {
					s = s.trim();
					log.atDebug().setMessage("current svrfld [{}]").addArgument(s).log();//20240517 change for Log Forging(debug)
					if (s.length() > 0 && s.indexOf(',') > -1) {
						String[] svrfldsary = s.split(",");
						for (int idx = 0; idx < svrfldsary.length; idx++) {
							log.debug("idx:[{}]=[{}]", idx, svrfldsary[idx].trim());
						}
					} else if (s.length() > 0) {
						log.atDebug().setMessage("get SERVICE [{}] in service table [{}]").addArgument(s).addArgument(svrprmtb).log();//20240517 change for Log Forging(debug)
						// 20210302 MatsudairsSyuMe
//						String[] setArg = {"bin/autosvr", "start", "--svrid", s};
//						DoProcessBuilder dp = new DoProcessBuilder(setArg);
//						DoProcessBuilder dp = new DoProcessBuilder("bin/autosvr", "start", "--svrid", s);
//						dp.Go();
						//2021090 9MatsudairsSyuMe check if start conductor only
						// 20210202, MatsudairsSyuMe
						if (!Server.getIsConductorRestore()) {
							DoProcessBuilder dp = new DoProcessBuilder();
							dp.Go("bin/autosvr", "start", "--svrid", s);
						}// store new service
						//20210911 MatsudairaSyuMe init set svridnode while conductor and restore mode
						Conductor.svridnodeMap.put(s, getSvrip());
						//----
						// ----
					} else
						log.error("ERROR!!! SERVICE parameters error in service table [{}] !!!", svrprmtb);
				}
			} else {
				log.error("ERROR!!! no svrid exist in table while IP=[{}] !!!", getSvrip());
			}
		} catch (Exception e) {
			//20240503 MatsudairaSyuMe mark up for System Information Leak e.printStackTrace();
			log.info("read database service data error");//20240503 change log message
		} finally {
			try {
				jsel2ins.CloseConnect();
			} catch (Exception e) {
				//20240503 MatsudairaSyuMe mark up for System Information Leak e.printStackTrace();
				log.error("close connect from database error");//20240503 change log message
			}
			jsel2ins = null;
		}
		// 20210828 MatsudairaSyuMe start conductor only
//		} 20210909 mark up
		// ----
		server = getMe();
		if (dburl != null && dburl.trim().length() > 0)
			server.run();
		else
			log.error("ERROR!!! url not set conductor moniter can't be initiated !!!!");
	}
	public static void stopServer() {
		if (server != null) {
			server.stop(0);
		}
	}

	public static Conductor getMe() {
		if (server == null) {
			server = new Conductor();
		}
		return server;
	}

	public void run() {
		log.debug("Enter Conductor moniter thread start");
		String selfld = "";
		String selkey = "";
		String[] sno = null;
		String cmdtbname = map.get("system.svrcmdtb[@name]");
		String cmdtbsearkey = map.get("system.svrcmdtb[@mkey]");
		String cmdtbfields = map.get("system.svrcmdtb[@fields]");
		String svrcmdhistbname = map.get("system.svrcmdhistb[@name]");
		String svrcmdhistbsearkey = map.get("system.svrcmdhistb[@mkey]");
		String svrcmdhistbfields = map.get("system.svrcmdhistb[@fields]");
		if (cmdtbfields.indexOf(',') > -1) {
			selfld = cmdtbfields.substring(cmdtbfields.indexOf(',') + 1);
			selkey = cmdtbfields.substring(0, cmdtbfields.indexOf(','));
		} else {
			selfld = cmdtbfields;
			selkey = cmdtbsearkey;
		}
		// 20220607 MatsudairaSyuMe
		try {
			if (jdawcon == null) {
				jdawcon = new GwDao(dburl, dbuser, dbpass, false);
				//20220613 MatsudairaSyuMe
				//log.info("initial select svrcmdtbl [{}]", jdawcon.SELMFLD_R(cmdtbname, selfld, selkey, "?", true));
				jdawcon.SELMFLD_R(cmdtbname, selfld, selkey, "?", true);
				log.info("initial delete SVRID svrcmdtbl [{}]", jdawcon.DELETETB_R(cmdtbname, "SVRID", "?", true));
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
				try {
					// 20220607 MatsydairaSyuMe, 20230517 take out mark for closing connection after access db 
					jdawcon = new GwDao(dburl, dbuser, dbpass, false);
					log.debug("current selfld=[{}] selkey=[{}] cmdtbsearkey=[{}]", selfld, selkey, cmdtbsearkey);
					//20220613 MatsudairasyuMe Change to use reused prepared statement, 20230517 take out mark for closing connection after access db 
					String[] cmd = jdawcon.SELMFLD(cmdtbname, selfld, selkey, "'" + getSvrip() + "'", false);
//					log.debug("current connect [{}]",jdawcon.getConn().isValid(1));
					//String[] cmd = jdawcon.SELMFLD_R(cmdtbname, selfld, selkey, "'" + getSvrip() + "'", false);20230517 mark up for closing connection after access db
					//----
					if (cmd != null && cmd.length > 0)
						for (String s : cmd) {
							// 20210302 MatsudairaSyuMe check row command not null
							if (s != null && s.trim().length() > 0) {
								s = s.trim();
								log.atDebug().setMessage("current row cmd [{}]").addArgument(s).log();//20240517 change for Log Forging(debug)
								if (s.length() > 0 && s.indexOf(',') > -1) {
									String[] cmdary = s.split(",");
									if (cmdary != null && cmdary.length > 1) {//20240523 prevent Redundant Null Check
										int idx = 0;
										sno = null;
										boolean createNode = false;
										boolean restartAlreadyStop = false;
										if (DateTimeUtil.MinDurationToCurrentTime(3, cmdary[2])) {
											log.atDebug().setMessage(
													"brws=[{}] keep in cmd table longer then 3 minutes will be cleared"
													).addArgument(cmdary[0]).log();//20240517 change for Log Forging(debug)
											if (cmdary[1].trim().length() > 0) {
												// 20210204 MatsudairaSyuMe
												final String logStr = String.format(
														"brws=[%s] cmd[%s] not execute will be marked fail in cmdhis",
														((cmdary == null) || (cmdary[0] == null)) ? "" : cmdary[0],
														((cmdary == null) || (cmdary.length < 2) || (cmdary[1] == null))
																? ""
																: cmdary[1]);
												log.atDebug().setMessage(logStr).log();//20240517 change for Log Forging(debug)
												/*
												 * 20220607 MatsudairaSyuMe/*20220607 MatsudairaSyuMe, 20230517 take out mark for closing connection after access db */
												if (cmdhiscon == null)
													cmdhiscon = new GwDao(dburl, dbuser, dbpass, false);
												 /* 20230517 take out mark for closing connection after access db */
												String[] chksno = cmdhiscon.SELMFLD(svrcmdhistbname, "SNO",
														"SVRID,CMD,CMDCREATETIME",
														"'" + cmdary[0] + "','" + cmdary[1] + "','" + cmdary[2] + "'",
														false);

												SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
												String t = sdf.format(new java.util.Date());
												String failfldvals = String.format(hisfldvalssptrn4, cmdary[0],
														getSvrip(), cmdary[1], cmdary[2], "FAIL", t, cmdary[3]);
												if (chksno == null || chksno.length == 0) {
													chksno = new String[1];
													chksno[0] = "-1";
												}
												cmdhiscon.INSSELChoiceKey(svrcmdhistbname, svrcmdhistbfields,
														failfldvals, svrcmdhistbsearkey, chksno[0], false, false);//20240510 Poor Style: Value Never Read for sno
												/*
												 * 20220607 MatsudairaSyuMe, 20230517 take out mark for closing connection after access db */
												 cmdhiscon.CloseConnect();
												 cmdhiscon = null;
												/*20230517 take out mark for closing connection after access db */
												//20240510 Poor Style: Value Never Read sno = null;
											}
											/*20230517 mark up for closing connection after access db 
											jdawcon.DELETETB_R(cmdtbname, "SVRID", cmdary[0], false);  //20220613 change to use reused statement
											*/
											jdawcon.DELETETB(cmdtbname, "SVRID", cmdary[0]);
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
										// ----
										for (String ss : cmdary)
											log.debug("cmd[{}]=[{}]", idx++, ss);
										String curcmd = "";curcmd = cmdary[1].trim().toUpperCase();//20240523 prevent Redundant Null Check
										// svrcmdhis
										if (curcmd != null && curcmd.length() > 0) {
											/*
											 * 20220607 MatsudairaSyuMe, 20230517 take out mark for closing connection after access db */
											cmdhiscon = new GwDao(dburl, dbuser, dbpass, false);
											/*20230517 take out mark for closing connection after access db */
											if (Conductor.svridnodeMap != null && Conductor.svridnodeMap.size() > 0) {
												if (Conductor.svridnodeMap.containsKey(cmdary[0])) {
													// 20210204,20210427 MatsudairaSyuMe Log Forging remove final
													// 20210714 MatsudairaSyuMe Log Forging
													// String chkcmd = StrUtil.convertValidLog(cmdary[0]);
													log.error(
															"!!! cmd object node already in nodeMap please STOP this node before START !!!");// chkcmd
													createNode = false;
												} else {
													// 20210204 MatsudairaSyuMe
													final String logStr = String.format(
															"!!! cmd object node=[%s] not in nodeList will be created",
															cmdary[0]);
//											log.debug("!!! cmd object node=[{}] not in nodeList will be created", cmdary[0]);
													log.atDebug().setMessage(logStr).log();//20240517 change for Log Forging(debug)
													createNode = true;
												}
											}
											String fldvals = String.format(hisfldvalssptrn, cmdary[0], getSvrip(),
													cmdary[1], cmdary[2], cmdary[3]);
											String[] chksno = cmdhiscon.SELMFLD(svrcmdhistbname, "SNO",
													"SVRID,CMD,CMDCREATETIME",
													"'" + cmdary[0] + "','" + cmdary[1] + "','" + cmdary[2] + "'",
													false);
//									log.debug("chksno=[{}]",chksno);
//											if (chksno != null && chksno.length > 0
//													&& Integer.parseInt(chksno[0].trim()) > -1) {//20240523 check null first for preventing Redundant Null Check
											if (chksno.length > 0 && Integer.parseInt(chksno[0].trim()) > -1) {//20240523 check null first for preventing Redundant Null Check
												for (String sss : chksno)
													log.atDebug().setMessage("sno[{}] already exist").addArgument(sss).log();//20240517 change for Log Forging(debug)
												// 20210413 MatsudairaSyuMe prevent Portability Flaw: Locale Dependent
												// Comparison change equals to
												if (curcmd.equalsIgnoreCase("RESTART")) { // current command is RESTART
																							// check cmdsvrhis if
																							// already done STOP
													for (int i = 0; i < chksno.length; i++) {
														String chkcmdresult = cmdhiscon.SELONEFLD(svrcmdhistbname,
																"CMDRESULT", "SNO", chksno[0], false);
														log.atDebug().setMessage(
																"table sno=[{}] svrcmdhis cmd is RESTART and cmdresult=[{}]"
																).addArgument(chksno[i]).addArgument(chkcmdresult).log();
														if (chkcmdresult != null//20240517 change for Log Forging(debug)
																&& chkcmdresult.equals("STOP")) {
															if (!restartAlreadyStop) {
																sno = null; // prepared to start new node
																restartAlreadyStop = true;
															} else {
																sno = new String[1];
																sno[0] = chksno[i];
															}
															log.atDebug().setMessage(
																	"table son=[{}] chksno=[{}] svrcmdhis cmd is RESTART and cmdresult=[{}] restartAlreadyStop=[{}]"
																	).addArgument(sno).addArgument(chksno[i]).addArgument(chkcmdresult).addArgument(restartAlreadyStop).log();//20240517 change for Log Forging(debug)
														} else {
															// current command is RESTART and waiting to STOP or already
															// set ACTIVE waiting to finish
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
											if (sno == null) {// first time receive command insert new record to
																// svrcmdhis
												sno = cmdhiscon.INSSELChoiceKey(svrcmdhistbname,
														"SVRID,IP,CMD,CMDCREATETIME,EMPNO", fldvals, svrcmdhistbsearkey,
														"-1", false, false);
												if (sno != null) {
													for (int i = 0; i < sno.length; i++)
														log.debug("sno[{}]=[{}]", i, sno[i]);
												} else
													log.error("sno null");
											}
											/*
											 * 20220607 MatsudairaSyuMe //---- //20210302 MatsudairaSyuMe, 20230517 take out mark for closing connection after access db */
											cmdhiscon.CloseConnect();
											cmdhiscon = null; //----
											/* 20230517 take out mark for closing connection after access d*/
										}
										// ----
										// log.debug("table sno=[{}] createNode=[{}] restartAlreadyStop=[{}]", (sno ==
										// null ? 0: sno[0]), createNode, restartAlreadyStop);
										// 20210204 MatsudairaSyume
										final String logStr = String.format(
												"table sno=[%s] createNode=[%s] restartAlreadyStop=[%s]",
												(sno == null ? 0 : sno[0]), createNode, restartAlreadyStop);
										log.atDebug().setMessage(logStr).log();//20240517 change for Log Forging(debug)
										/*
										 * 20220607 MatsudairaSyuMe //20210302 MatsudairaSyuMe, 20230517 take out mark for closing connection after access db */
										 if (cmdhiscon == null)
											 cmdhiscon = new GwDao(dburl, dbuser, dbpass, false); //----
										 /* , 20230517 take out mark for closing connection after access db */
										// 20210413 MatsudairaSyuMe prevent Null Dereference
										if (sno == null) {
											sno = new String[1];
											sno[0] = "";
										}
										// ----
										// 20210426 MatsudairaSyuMe prevent Portability Flaw: Locale Dependent
										// Comparison
										int selCmd = Constants.UNKNOWN;if (curcmd != null) {
										if (curcmd.toUpperCase(Locale.ENGLISH).equals("START"))
											selCmd = Constants.START;
										else if (curcmd.toUpperCase(Locale.ENGLISH).equals("STOP"))
											selCmd = Constants.STOP;
										else if (curcmd.toUpperCase(Locale.ENGLISH).equals("RESTART"))
											selCmd = Constants.RESTART;
										switch (selCmd) { // 20210426 MatsudairaSyuMe prevent Portability Flaw: Locale
															// Dependent Comparison
										case Constants.START:// 20210426 MatsudairaSyuMe prevent Portability Flaw:
																// Locale Dependent Comparison
											if (Conductor.svridnodeMap.containsKey(cmdary[0])) {
												log.info(
														"cmd object node=[{}] process already been initiated please STOP or Shutdown before START");
											} else {
												Conductor.svridnodeMap.put(cmdary[0], getSvrip());
												// 20210302 MatsudairaSyuMe
//										String[] monSetArg = {"bin/autosvr", "start", "--svrid", cmdary[0]};
												// 20210202 MatsudairaSyuMe
//										DoProcessBuilder monDp = new DoProcessBuilder(monSetArg);
//										DoProcessBuilder monDp = new DoProcessBuilder("bin/autosvr", "start", "--svrid", cmdary[0]);
//										monDp.Go();
												DoProcessBuilder monDp = new DoProcessBuilder();
												monDp.Go("bin/autosvr", "start", "--svrid", cmdary[0]);
												// ----
												SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
												String t = sdf.format(new java.util.Date());
												int row = jdawcon.UPDT(cmdtbname, "CMD,CMDRESULT,CMDRESULTTIME",
														"'','START','" + t + "'", "SVRID, IP",
														cmdary[0] + ",'" + getSvrip() + "'");
												log.atDebug().setMessage("total {} records update").addArgument(row).log();//20240517 change for Log Forging(debug)
												log.atDebug().setMessage("cmd object node=[{}] already active!!!!").addArgument(cmdary[0]).log();//20240517 change for Log Forging(debug)
												// 20201218 keep original cmd to svrcmdhis
												String fldvals3 = String.format(hisfldvalssptrn4, cmdary[0], getSvrip(),
														cmdary[1], cmdary[2], cmdary[1], t, cmdary[3]);
												// ----
												sno = cmdhiscon.INSSELChoiceKey(svrcmdhistbname, svrcmdhistbfields,
														fldvals3, svrcmdhistbsearkey, sno[0], false, true);
												if (sno != null) {
													for (int i = 0; i < sno.length; i++)
														log.debug("sno[{}]=[{}]", i, sno[i]);
												} else
													log.error("sno null");
												// ----
											}
											break;
										case Constants.STOP:// 20210426 MatsudairaSyuMe prevent Portability Flaw: Locale
															// Dependent Comparison
											if (!Conductor.svridnodeMap.containsKey(cmdary[0])) {
												// 20210204,20210427 MatsudairaSyuMe Log Forging remove final
												// 20210714 MatsudairaSyuMe Log Forging
												// String logStr2 = StrUtil.convertValidLog(cmdary[0]);
												log.info(
														"current cmd object node is not running in this server no need to STOP!!"); // logStr2
												// ---
											} else {
												// 20210302 MatsudairaSyuMe
												// 20210202 MatsudairSyuMe
//										String[] monSetArg = {"bin/autosvr", "stop", "--svrid", cmdary[0]};
//										DoProcessBuilder monDp = new DoProcessBuilder(monSetArg);
//										DoProcessBuilder monDp = new DoProcessBuilder("bin/autosvr", "stop", "--svrid", cmdary[0]);
//										monDp.Go();
												DoProcessBuilder monDp = new DoProcessBuilder();
												monDp.Go("bin/autosvr", "stop", "--svrid", cmdary[0]);
												// ----
												SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
												String t = sdf.format(new java.util.Date());
//										int row = jdawcon.UPDT(cmdtbname, "CMD,CMDCREATETIME,CMDRESULT,CMDRESULTTIME", "'','','STOP','" + t + "'",
//												"SVRID, IP", cmdary[0] + ",'" + getSvrip() + "'");
												int row = jdawcon.UPDT(cmdtbname, "CMD,CMDRESULT,CMDRESULTTIME",
														"'','STOP','" + t + "'", "SVRID", cmdary[0]);
												log.debug("total {} records update", row);
												Conductor.svridnodeMap.remove(cmdary[0]);
												log.atDebug().setMessage("cmd object node=[{}] already shutdown!!!!").addArgument(cmdary[0]).log();//20240517 change for Log Forging(debug)
												// 20201218 keep original cmd to svrcmdhis
												String fldvals3 = String.format(hisfldvalssptrn4, cmdary[0], getSvrip(),
														cmdary[1], cmdary[2], cmdary[1], t, cmdary[3]);
												// ----
												sno = cmdhiscon.INSSELChoiceKey(svrcmdhistbname, svrcmdhistbfields,
														fldvals3, svrcmdhistbsearkey, sno[0], false, true);
												if (sno != null) {
													for (int i = 0; i < sno.length; i++)
														log.debug("sno[{}]=[{}]", i, sno[i]);
												} else
													log.error("sno null");
												// ----

											}
											break;
										case Constants.RESTART:// 20210426 MatsudairaSyuMe prevent Portability Flaw:
																// Locale Dependent Comparison
											// 20210302 MatsudairaSyuMe
											String monSetArg[] = null;
											// ----
											if (Conductor.svridnodeMap.containsKey(cmdary[0])) {
												String[] tmpsetArg = { "bin/autosvr", "restart", "--svrid", cmdary[0] };
												monSetArg = tmpsetArg;
												log.atDebug().setMessage("cmd object node=[{}] try to restart process").addArgument(cmdary[0]).log();//20240517 change for Log Forging(debug)
											} else {
												// start to create new node and start
												Conductor.svridnodeMap.put(cmdary[0], getSvrip());
												String[] tmpsetArg = { "bin/autosvr", "start", "--svrid", cmdary[0] };
												monSetArg = tmpsetArg;
												log.atDebug().setMessage("start to create new node=[{}]").addArgument(cmdary[0]).log();//20240517 change for Log Forging(debug)
											}
											// 20210302 MAtsuDairaSyuMe
											// 20210202 MatsuDairaSyume
//									DoProcessBuilder monDp = null;
//									if (Conductor.svridnodeMap.containsKey(cmdary[0]))
//										monDp = new DoProcessBuilder("bin/autosvr", "restart", "--svrid", cmdary[0]);
//									else
//										monDp = new DoProcessBuilder("bin/autosvr", "start", "--svrid", cmdary[0]);
//									monDp.Go();
											DoProcessBuilder monDp = new DoProcessBuilder();
											monDp.Go(monSetArg[0], monSetArg[1], monSetArg[2], monSetArg[3]);
											// ----
											SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
											String t = sdf.format(new java.util.Date());
											int row = jdawcon.UPDT(cmdtbname, "CMD,CMDRESULT,CMDRESULTTIME",
													"'','RESTART','" + t + "'", "SVRID, IP",
													cmdary[0] + ",'" + getSvrip() + "'");
											log.atDebug().setMessage("total {} records update").addArgument(row).log();//20240517 change for Log Forging(debug)
											log.atDebug().setMessage("cmd object node=[{}] already restart!!!!").addArgument(cmdary[0]).log();//20240517 change for Log Forging(debug)
											// 20201218 keep original cmd to svrcmdhis
											String fldvals3 = String.format(hisfldvalssptrn4, cmdary[0], getSvrip(),
													cmdary[1], cmdary[2], cmdary[1], t, cmdary[3]);
											// ----
											sno = cmdhiscon.INSSELChoiceKey(svrcmdhistbname, svrcmdhistbfields,
													fldvals3, svrcmdhistbsearkey, sno[0], false, true);
											if (sno != null) {
												for (int i = 0; i < sno.length; i++)
													log.debug("sno[{}]=[{}]", i, sno[i]);
											} else
												log.error("sno null");
											// ----
											break;
										default:
											log.atDebug().setMessage("!!! cmd object node=[{}] cmd [{}] ignore").addArgument(cmdary[0]).addArgument(cmdary[1]).log();//20240517 change for Log Forging(debug)
											break;
										} } else { log.error("!!! cmd error"); }
										/*
										 * 20220607 MatsudairaSyuMe //20210302 MatsudairaSyuMe, 20230517 take out mark for closing connection after access db */
										if (cmdhiscon != null)
											cmdhiscon.CloseConnect();
										cmdhiscon = null;
										 /* 20230517 take out mark for closing connection after access db */
										// ----
									} else {
										// 20210204 MatsidairaSyuMe
										log.atDebug().setMessage("!!! cmd object node format error !!!").log();//20240517 change for Log Forging(debug)
									}
								} else {
									// 20210204 ,20210427 MatsudairaSyuMe Log Forging
									// 20210713 MatsudairaSyuMe Log Forging
									// String chks = StrUtil.convertValidLog(s);
									log.error("!!!current row cmd error"); // chks
									// ---
								}
							} else {
								// 20210713 MatsudairaSyuMe Log Forging
								log.warn("select raw command data error drop it");// , StrUtil.convertValidLog(s)
							}
						}
					/*
					 * 20220607 MatsudairaSyuMe, 20230517 MatsudairaSyuMe take out mark for closing connection after access db */
					   //20210302---- 
					   jdawcon.CloseConnect();
					   jdawcon = null;
					   if (cmdhiscon != null)
						   cmdhiscon.CloseConnect();
					   cmdhiscon = null;
					 /*20230517 MatsudairaSyuMe take out mark for closing connection after access db */
				} catch (Exception e) {
					//20240503 MatsudairaSyuMe mark up for System Information Leak e.printStackTrace();
					log.error("parse command error"); //20240503 change log message
				}
				sleep(3);
			}
		// 20220607 MatsudairaSyuMe
		} catch (Exception e) {
			//20240503 MatsudairaSyuMe mark up for System Information Leak e.printStackTrace();
			log.error("jdawcon read or update error"); //20240503 change log message
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
		// ----
	}
	
	public void stop(int waitTime) {
		log.debug("Enter Conductor stop");
//		try {20240508
//			thread.start();
//		} catch (Exception e) {
//			//20240503 MatsudairaSyuMe mark for System Information Leak e.printStackTrace();
//		}
	}// stop


	/**
	 * @return the svrip
	 */
	public static String getSvrip() {
		return svrip;
	}
	/**
	 * @param svrip the svrip to set
	 */
	public static void setSvrip(String svrip) {
		Conductor.svrip = svrip;
	}
}
