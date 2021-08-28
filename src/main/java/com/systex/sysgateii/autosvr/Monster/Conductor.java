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

import com.systex.sysgateii.autosvr.comm.Constants;
import com.systex.sysgateii.autosvr.dao.GwDao;
import com.systex.sysgateii.autosvr.util.DateTimeUtil;
import com.systex.sysgateii.autosvr.util.StrUtil;

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
	GwDao jdawcon = null;
	GwDao cmdhiscon = null;
	private String hisfldvalssptrn = "%s,'%s','%s','%s', '%s'";
	//update svrcmdhis fail
	private String hisfldvalssptrn4 = "%s,'%s','%s','%s','%s','%s', '%s'";

	public static void sleep(int t) {
		try {
			Thread.sleep(t * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
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
		if (!getSvrip().equalsIgnoreCase("r")) {
			// ----
			try {
				jsel2ins = new GwDao(dburl, dbuser, dbpass, false);
				String[] svrflds = jsel2ins.SELMFLD(svrprmtb, "SVRID", "IP", "'" + getSvrip() + "'", false);
				if (svrflds != null && svrflds.length > 0) {
					for (String s : svrflds) {
						s = s.trim();
						log.debug("current svrfld [{}]", s);
						if (s.length() > 0 && s.indexOf(',') > -1) {
							String[] svrfldsary = s.split(",");
							for (int idx = 0; idx < svrfldsary.length; idx++) {
								log.debug("idx:[{}]=[{}]", idx, svrfldsary[idx].trim());
							}
						} else if (s.length() > 0) {
							log.debug("get SERVICE [{}] in service table [{}]", s, svrprmtb);
							// 20210302 MatsudairsSyuMe
//						String[] setArg = {"bin/autosvr", "start", "--svrid", s};
//						DoProcessBuilder dp = new DoProcessBuilder(setArg);
//						DoProcessBuilder dp = new DoProcessBuilder("bin/autosvr", "start", "--svrid", s);
//						dp.Go();
							// 20210202 MatsudairsSyuMe
							DoProcessBuilder dp = new DoProcessBuilder();
							dp.Go("bin/autosvr", "start", "--svrid", s);
							// ----
							// store new service
							Conductor.svridnodeMap.put(s, getSvrip());
						} else
							log.error("ERROR!!! SERVICE parameters error in service table [{}] !!!", svrprmtb);
					}
				} else {
					log.error("ERROR!!! no svrid exist in table while IP=[{}] !!!", getSvrip());
				}
			} catch (Exception e) {
				e.printStackTrace();
				log.info("read database error [{}]", e.toString());
			} finally {
				try {
					jsel2ins.CloseConnect();
				} catch (Exception e) {
					e.printStackTrace();
					log.error("close connect from database error [{}]", e.toString());
				}
				jsel2ins = null;
			}
			// 20210828 MatsudairaSyuMe start conductor only
		}
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

		while (true) {
			log.info("monitorThread");
			try {
				jdawcon = new GwDao(dburl, dbuser, dbpass, false);
				log.debug("current selfld=[{}] selkey=[{}] cmdtbsearkey=[{}]", selfld, selkey, cmdtbsearkey);
				String[] cmd = jdawcon.SELMFLD(cmdtbname, selfld, selkey, "'" + getSvrip() + "'", false);
				if(cmd != null && cmd.length > 0)
					for (String s: cmd) {
						//20210302 MatsudairaSyuMe check row command not null
						if (s != null && s.trim().length() > 0) {
						s = s.trim();
						log.debug("current row cmd [{}]", s);
						if (s.length() > 0 && s.indexOf(',') > -1) {
							String[] cmdary = s.split(",");
							if (cmdary.length > 1) {
								int idx = 0;
								sno = null;
								boolean createNode = false;
								boolean restartAlreadyStop = false;
								if (DateTimeUtil.MinDurationToCurrentTime(3,cmdary[2])) {
									log.debug("brws=[{}] keep in cmd table longer then 3 minutes will be cleared",cmdary[0]);
									if (cmdary[1].trim().length() > 0) {
										//20210204 MatsudairaSyuMe
										final String logStr = String.format("brws=[%s] cmd[%s] not execute will be marked fail in cmdhis",((cmdary == null) || (cmdary[0] == null)) ? "" : cmdary[0], ((cmdary == null) || (cmdary.length < 2) || (cmdary[1] == null)) ? "" : cmdary[1]);
										log.debug(logStr);
										if (cmdhiscon == null)
											cmdhiscon = new GwDao(dburl, dbuser, dbpass, false);
										String[] chksno = cmdhiscon.SELMFLD(svrcmdhistbname, "SNO", "SVRID,CMD,CMDCREATETIME", "'" + cmdary[0] + "','"+ cmdary[1] + "','"+ cmdary[2]+ "'", false);

										SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
										String t = sdf.format(new java.util.Date());
										String failfldvals = String.format(hisfldvalssptrn4, cmdary[0], getSvrip(), cmdary[1], cmdary[2],"FAIL",t,cmdary[3]);
										if (chksno == null || chksno.length == 0) {
											chksno = new String[1];
											chksno[0] = "-1";
										}
										sno = cmdhiscon.INSSELChoiceKey(svrcmdhistbname, svrcmdhistbfields, failfldvals, svrcmdhistbsearkey, chksno[0], false, false);
										cmdhiscon.CloseConnect();
										cmdhiscon = null;
										sno = null;
									}
									jdawcon.DELETETB(cmdtbname, "SVRID",cmdary[0]);
									continue;
								}
								//----
								for (String ss: cmdary)
									log.debug("cmd[{}]=[{}]",idx++, ss);
								String curcmd = cmdary[1].trim().toUpperCase();
								//svrcmdhis
								if (curcmd != null && curcmd.length() > 0) {
									cmdhiscon = new GwDao(dburl, dbuser, dbpass, false);
									if (Conductor.svridnodeMap != null && Conductor.svridnodeMap.size() > 0) {
										if (Conductor.svridnodeMap.containsKey(cmdary[0])) {
											//20210204,20210427 MatsudairaSyuMe Log Forging remove final
											// 20210714 MatsudairaSyuMe Log Forging
											//String chkcmd = StrUtil.convertValidLog(cmdary[0]);
											log.error("!!! cmd object node already in nodeMap please STOP this node before START !!!");//chkcmd
											createNode = false;
										} else {
											//20210204 MatsudairaSyuMe
											final String logStr = String.format("!!! cmd object node=[%s] not in nodeList will be created", cmdary[0]);
//											log.debug("!!! cmd object node=[{}] not in nodeList will be created", cmdary[0]);
											log.debug(logStr);
											createNode = true;
										}
									}
									String fldvals = String.format(hisfldvalssptrn, cmdary[0], getSvrip(), cmdary[1], cmdary[2], cmdary[3]);
									String[] chksno = cmdhiscon.SELMFLD(svrcmdhistbname, "SNO", "SVRID,CMD,CMDCREATETIME", "'" + cmdary[0] + "','"+ cmdary[1] + "','"+ cmdary[2]+ "'", false);
//									log.debug("chksno=[{}]",chksno);
									if (chksno != null && chksno.length > 0 && Integer.parseInt(chksno[0].trim()) > -1) {
										for (String sss: chksno)
											log.debug("sno[{}] already exist",sss);
										//20210413 MatsudairaSyuMe prevent Portability Flaw: Locale Dependent Comparison change equals to 
										if (curcmd.equalsIgnoreCase("RESTART")) { // current command is RESTART check cmdsvrhis if already done STOP
											for (int i = 0; i < chksno.length; i++) {
												String chkcmdresult = cmdhiscon.SELONEFLD(svrcmdhistbname, "CMDRESULT", "SNO", chksno[0], false);
												log.debug("table sno=[{}] svrcmdhis cmd is RESTART and cmdresult=[{}]", chksno[i], chkcmdresult);
												if (chkcmdresult != null && chkcmdresult.trim().length() > 0 && chkcmdresult.equals("STOP")) {
													if (!restartAlreadyStop) {
														sno = null; // prepared to start new node
														restartAlreadyStop = true;
													} else {
														sno = new String[1];
														sno[0] = chksno[i];																																				
													}
													log.debug("table son=[{}] chksno=[{}] svrcmdhis cmd is RESTART and cmdresult=[{}] restartAlreadyStop=[{}]", sno, chksno[i], chkcmdresult, restartAlreadyStop);
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
									if (sno == null) {// first time receive command insert new record to svrcmdhis
										sno = cmdhiscon.INSSELChoiceKey(svrcmdhistbname, "SVRID,IP,CMD,CMDCREATETIME,EMPNO", fldvals, svrcmdhistbsearkey, "-1", false, false);
										if (sno != null) {
											for (int i = 0; i < sno.length; i++)
												log.debug("sno[{}]=[{}]",i,sno[i]);
											} else
												log.error("sno null");
									}
									//----
									//20210302 MatsudairaSyuMe
									cmdhiscon.CloseConnect();
									cmdhiscon = null;
									//----
								}
								//----
								//log.debug("table sno=[{}] createNode=[{}] restartAlreadyStop=[{}]", (sno == null ? 0: sno[0]), createNode, restartAlreadyStop);
								//20210204 MatsudairaSyume
								final String logStr = String.format("table sno=[%s] createNode=[%s] restartAlreadyStop=[%s]", (sno == null ? 0: sno[0]), createNode, restartAlreadyStop);
								log.debug(logStr);
								//20210302 MatsudairaSyuMe
								if (cmdhiscon == null)
									cmdhiscon = new GwDao(dburl, dbuser, dbpass, false);
								//----
								//20210413 MatsudairaSyuMe prevent Null Dereference
								if (sno == null) {
									sno = new String[1];
									sno[0] = "";
								}
								//----
								//20210426 MatsudairaSyuMe prevent Portability Flaw: Locale Dependent Comparison
								int selCmd = Constants.UNKNOWN;
								if (curcmd.toUpperCase(Locale.ENGLISH).equals("START"))
									selCmd = Constants.START;
								else if(curcmd.toUpperCase(Locale.ENGLISH).equals("STOP"))
									selCmd = Constants.STOP;
								else if(curcmd.toUpperCase(Locale.ENGLISH).equals("RESTART"))
									selCmd = Constants.RESTART;
								switch (selCmd) { //20210426 MatsudairaSyuMe prevent Portability Flaw: Locale Dependent Comparison
								case Constants.START://20210426 MatsudairaSyuMe prevent Portability Flaw: Locale Dependent Comparison
									if (Conductor.svridnodeMap.containsKey(cmdary[0])) {
										log.info("cmd object node=[{}] process already been initiated please STOP or Shutdown before START");
									} else {
										Conductor.svridnodeMap.put(cmdary[0], getSvrip());
										//20210302 MatsudairaSyuMe
//										String[] monSetArg = {"bin/autosvr", "start", "--svrid", cmdary[0]};
										//20210202 MatsudairaSyuMe
//										DoProcessBuilder monDp = new DoProcessBuilder(monSetArg);
//										DoProcessBuilder monDp = new DoProcessBuilder("bin/autosvr", "start", "--svrid", cmdary[0]);
//										monDp.Go();
										DoProcessBuilder monDp = new DoProcessBuilder();
										monDp.Go("bin/autosvr", "start", "--svrid", cmdary[0]);
										//----
										SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
										String t = sdf.format(new java.util.Date());
										int row = jdawcon.UPDT(cmdtbname, "CMD,CMDRESULT,CMDRESULTTIME", "'','START','" + t + "'",
												"SVRID, IP", cmdary[0] + ",'" + getSvrip() + "'");
										log.debug("total {} records update", row);
										log.debug("cmd object node=[{}] already active!!!!", cmdary[0]);
										//20201218 keep original cmd to svrcmdhis
										String fldvals3 = String.format(hisfldvalssptrn4, cmdary[0], getSvrip(), cmdary[1], cmdary[2], cmdary[1], t, cmdary[3]);
										//----
										sno = cmdhiscon.INSSELChoiceKey(svrcmdhistbname, svrcmdhistbfields, fldvals3, svrcmdhistbsearkey, sno[0], false, true);
										if (sno != null) {
											for (int i = 0; i < sno.length; i++)
												log.debug("sno[{}]=[{}]",i,sno[i]);
										} else
											log.error("sno null");
										//----
									}
									break;
								case Constants.STOP://20210426 MatsudairaSyuMe prevent Portability Flaw: Locale Dependent Comparison
									if (!Conductor.svridnodeMap.containsKey(cmdary[0])) {
										//20210204,20210427 MatsudairaSyuMe Log Forging remove final
										// 20210714 MatsudairaSyuMe Log Forging
										//String logStr2 = StrUtil.convertValidLog(cmdary[0]);
										log.info("current cmd object node is not running in this server no need to STOP!!"); //logStr2
										//---
									} else {
										//20210302 MatsudairaSyuMe
										//20210202 MatsudairSyuMe
//										String[] monSetArg = {"bin/autosvr", "stop", "--svrid", cmdary[0]};
//										DoProcessBuilder monDp = new DoProcessBuilder(monSetArg);
//										DoProcessBuilder monDp = new DoProcessBuilder("bin/autosvr", "stop", "--svrid", cmdary[0]);
//										monDp.Go();
										DoProcessBuilder monDp = new DoProcessBuilder();
										monDp.Go("bin/autosvr", "stop", "--svrid", cmdary[0]);
										//----
										SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
										String t = sdf.format(new java.util.Date());
//										int row = jdawcon.UPDT(cmdtbname, "CMD,CMDCREATETIME,CMDRESULT,CMDRESULTTIME", "'','','STOP','" + t + "'",
//												"SVRID, IP", cmdary[0] + ",'" + getSvrip() + "'");
										int row = jdawcon.UPDT(cmdtbname, "CMD,CMDRESULT,CMDRESULTTIME", "'','STOP','" + t + "'",
												"SVRID", cmdary[0]);
										log.debug("total {} records update", row);
										Conductor.svridnodeMap.remove(cmdary[0]);
										log.debug("cmd object node=[{}] already shutdown!!!!", cmdary[0]);
										//20201218 keep original cmd to svrcmdhis
										String fldvals3 = String.format(hisfldvalssptrn4, cmdary[0], getSvrip(), cmdary[1], cmdary[2], cmdary[1], t, cmdary[3]);
										//----
										sno = cmdhiscon.INSSELChoiceKey(svrcmdhistbname, svrcmdhistbfields, fldvals3, svrcmdhistbsearkey, sno[0], false, true);
										if (sno != null) {
											for (int i = 0; i < sno.length; i++)
												log.debug("sno[{}]=[{}]",i,sno[i]);
										} else
											log.error("sno null");
										//----

									}
									break;
								case Constants.RESTART://20210426 MatsudairaSyuMe prevent Portability Flaw: Locale Dependent Comparison
									//20210302 MatsudairaSyuMe
									String monSetArg[] = null;
									//----
									if (Conductor.svridnodeMap.containsKey(cmdary[0])) {
										String[] tmpsetArg = {"bin/autosvr", "restart", "--svrid", cmdary[0]};
										monSetArg = tmpsetArg;
										log.debug("cmd object node=[{}] try to restart process", cmdary[0]);
									} else {
										//start to create new node and start
										Conductor.svridnodeMap.put(cmdary[0], getSvrip());
										String[] tmpsetArg = {"bin/autosvr", "start", "--svrid", cmdary[0]};
										monSetArg = tmpsetArg;
										log.debug("start to create new node=[{}]", cmdary[0]);
									}
									//20210302 MAtsuDairaSyuMe
									//20210202 MatsuDairaSyume
//									DoProcessBuilder monDp = null;
//									if (Conductor.svridnodeMap.containsKey(cmdary[0]))
//										monDp = new DoProcessBuilder("bin/autosvr", "restart", "--svrid", cmdary[0]);
//									else
//										monDp = new DoProcessBuilder("bin/autosvr", "start", "--svrid", cmdary[0]);
//									monDp.Go();
									DoProcessBuilder monDp = new DoProcessBuilder();
									monDp.Go(monSetArg[0], monSetArg[1], monSetArg[2], monSetArg[3]);
									//----
									SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
									String t = sdf.format(new java.util.Date());
									int row = jdawcon.UPDT(cmdtbname, "CMD,CMDRESULT,CMDRESULTTIME", "'','RESTART','" + t + "'",
											"SVRID, IP", cmdary[0] + ",'" + getSvrip() + "'");
									log.debug("total {} records update", row);
									log.debug("cmd object node=[{}] already restart!!!!", cmdary[0]);
									//20201218 keep original cmd to svrcmdhis
									String fldvals3 = String.format(hisfldvalssptrn4, cmdary[0], getSvrip(), cmdary[1], cmdary[2], cmdary[1], t, cmdary[3]);
									//----
									sno = cmdhiscon.INSSELChoiceKey(svrcmdhistbname, svrcmdhistbfields, fldvals3, svrcmdhistbsearkey, sno[0], false, true);
									if (sno != null) {
										for (int i = 0; i < sno.length; i++)
											log.debug("sno[{}]=[{}]",i,sno[i]);
									} else
										log.error("sno null");
									//----
									break;
								default:
									log.debug("!!! cmd object node=[{}] cmd [{}] ignore", cmdary[0], cmdary[1]);
									break;
								}
								//20210302 MatsudairaSyuMe
								if (cmdhiscon != null)
									cmdhiscon.CloseConnect();
								cmdhiscon = null;
								//----
							} else {
								//20210204 MatsidairaSyuMe
								final String logStr = String.format("!!! cmd object node=[%s] format error !!!", ((cmdary == null) || (cmdary[0] == null)) ? "" : cmdary[0]);
								log.debug(logStr);	
							}
						} else {
							//20210204 ,20210427 MatsudairaSyuMe Log Forging
							// 20210713 MatsudairaSyuMe Log Forging
							//String chks = StrUtil.convertValidLog(s);
							log.error("!!!current row cmd error"); // chks
							//---
						}
					} else {
						// 20210713 MatsudairaSyuMe Log Forging
						log.warn("select raw command data error drop it");//, StrUtil.convertValidLog(s)
					}
				}
				//20210302----
				jdawcon.CloseConnect();
				jdawcon = null;
				if (cmdhiscon != null)
					cmdhiscon.CloseConnect();
				cmdhiscon = null;
			} catch (Exception e) {
				e.printStackTrace();
				log.error("parse command error:{}",e.getMessage());
			}
			sleep(3);
		}

	}
	
	public void stop(int waitTime) {
		log.debug("Enter Conductor stop");
		try {
//			thread.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
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
