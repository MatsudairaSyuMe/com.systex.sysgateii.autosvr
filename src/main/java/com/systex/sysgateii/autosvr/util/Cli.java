package com.systex.sysgateii.autosvr.util;

import java.text.SimpleDateFormat;

import com.systex.sysgateii.autosvr.autoPrtSvr.Server.PrnSvr;
import com.systex.sysgateii.autosvr.comm.Constants;
import com.systex.sysgateii.autosvr.conf.DynamicProps;
import com.systex.sysgateii.autosvr.dao.GwDao;
import com.systex.sysgateii.autosvr.listener.EventType;

/*
 * Client command interface tools
 * MatsudairaSyuMe
 * 
 * 20201019
 * 
 */

public class Cli {
	private static GwDao jsel2ins = null;
	public static void main(String[] args) {
		DynamicProps dcf = null;
		try {
			if (args.length > 1) {
				//20201119 svrid will receive command
				String sidC = "";
				//----
				String sidS = "";
				String brwsS = "";
				String setcmdS = "";
				String url = "";
				String user = "";
				String usinsg = "";
				String updcmdPtrn = "%d, '%s','%s','%s','%s','%s'";
				String updsvrcmdPtrn = "'%s','%s','%s','%s','%s','%s'";

				boolean listcr = false;

				String userName = System.getProperty("user.name");
				if (userName == null || userName.length() == 0) userName = "SYSTEM";
				userName = userName.length() > 6 ? userName.substring(0, 6): userName;

				for (int i = 0; i < args.length; i++) {
					if (args[i].equalsIgnoreCase("-s") && ((i + 1) < args.length)) {
						System.out.println(String.format("i + 1=%s", args[i + 1]));
						if (args[i + 1].trim().indexOf(':') > -1) { //list status by service id
							String sary[] = args[i + 1].trim().split(":");
							switch (sary.length) {
							case 0: //list status of all services
								System.out.println("list status of all services");
								break;
							case 1: //list status of service sid
								System.out.println("list status of service sid");
								sidS = sary[0];
								break;
							case 2: //list status of device BRWS by service sid
								     //or list status of device BRWS
								if (sary[0].length() > 0)
									sidS = sary[0];
								if (sary[1].length() > 0)
									brwsS = sary[1];
								System.out.println("list status of device BRWS");
								break;
							default:
								System.err.println("command format error please check -help " + args[i]);
								System.exit(-1);
							}
						} else { //list status of device BRWS
							brwsS = args[i + 1].trim();
							System.out.println("list status of device BRWS");
						}
						i += 1;
					} else if (args[i].equalsIgnoreCase("-cr") && ((i + 1) < args.length)) {
						System.out.println(String.format("i + 1=%s", args[i + 1]));
						if (args[i + 1].trim().indexOf(':') > -1) { //list status by service id
							String sary[] = args[i + 1].trim().split(":");
							switch (sary.length) {
//							case 0: //list status of all devices
//								System.out.println("listt status of all devices");
//								break;
							case 1: //list status of service sid
								System.out.println("list command status of all device by service sid");
								sidS = sary[0];
								listcr = true;
								break;
							case 2: //list status of device BRWS by service sid
								     //or list status of device BRWS
								if (sary[0].length() > 0)
									sidS = sary[0];
								if (sary[1].length() > 0)
									brwsS = sary[1];
								System.out.println("list status of device BRWS");
								listcr = true;
								break;
							default:
								System.err.println("command format error please check -help " + args[i]);
								System.exit(-1);
							}
						} else { //show status of device BRWS
							brwsS = args[i + 1].trim();
							System.out.println("list status of device BRWS");
						}
						i += 1;
					} else if (args[i].equalsIgnoreCase("-c") && ((i + 2) < args.length)) {
						System.out.println(String.format("i + 1=%s i + 2=%s", args[i + 1], args[i + 2]));
						if (args[i + 1].trim().indexOf(':') > -1) { //show status by service id
							String sary[] = args[i + 1].trim().split(":");
							switch (sary.length) {
//							case 0: //show status of all services
//								System.out.println("show status of all services");
//								break;
							//20201127
							case 1: //set command to service sid
								sidC = sary[0];
								if (args[i + 2].trim().length() > 0)
									setcmdS = args[i + 2];
								System.out.println("set command to service sid");
								break;
							case 2: //set command to device BRWS by service sid
							        //or set command to of all devices on service sid 
								if (sary[0].length() > 0)
									sidS = sary[0];
								if (sary[1].length() > 0)
									brwsS = sary[1];
								if (args[i + 2].trim().length() > 0)
									setcmdS = args[i + 2];
								System.out.println("set command to device BRWS by service sid or set command to of all devices on service sid");
								break;
							default:
								System.err.println("command format error please check -help " + args[i]);
								System.exit(-1);
							}
						} else { //set command to device BRWS
							brwsS = args[i + 1].trim();
							if (args[i + 2].trim().length() > 0)
								setcmdS = args[i + 2];
							System.out.println("set command to device BRWS");
						}
						i += 2;
						if (setcmdS.length() <= 0) {
							System.err.println("command format error please check -help sid=" + sidS + " brws=" + brwsS + " cmd=" + setcmdS);
							System.exit(-1);							
						}
					} else {
						System.err.println("command " + args[i] + " format error please check -help");	
						System.exit(-1);
					}
				}
				//20240503 mark up System.out.println("listcr=" + listcr + " user name=" + userName);
				if (listcr) {
					dcf = new DynamicProps("rateprtservice.xml");
					url = dcf.getConHashMap().get("system.db[@url]");
					user = dcf.getConHashMap().get("system.db[@user]");
					usinsg = dcf.getConHashMap().get("system.db[@pass]");
					String cmdtbname = dcf.getConHashMap().get("system.devcmdtb[@name]");
					String cmdtbmkey = "SVRID";
					String cmdtbfields = "AUID,CMD,CMDCREATETIME,CMDRESULT,CMDRESULTTIME";
					String cmdtbfields2 = "SVRID,AUID,CMD,CMDCREATETIME,CMDRESULT,CMDRESULTTIME";
					if (jsel2ins == null)
						jsel2ins = new GwDao(url, user, usinsg, false);
					String[] cmd = null;

					if (brwsS.trim().length() <= 0)
						cmd = jsel2ins.SELMFLD(cmdtbname, "BRWS," + cmdtbfields, cmdtbmkey, sidS,
							false);
					else {
						if (sidS.trim().length() > 0)
							cmd = jsel2ins.SELMFLD(cmdtbname, cmdtbfields, cmdtbmkey + ",BRWS", sidS + "," + brwsS,
									false);
						else
							cmd = jsel2ins.SELMFLD(cmdtbname, cmdtbfields2, "BRWS", brwsS, false);
					}

					if (cmd != null && cmd.length > 0)
						for (String s : cmd) {
							s = s.trim();
							if (s.length() > 0 && s.indexOf(',') > -1) {
								String[] cmdary = s.split(",");
								if (cmdary.length > 1) {
									if (cmdary.length > 5)
										if (sidS.trim().length() > 0)
											System.out.println(
													String.format("SVRID=[%s] BRWS=[%s] AUID=[%s] CMD=[%s] CMDCREATETIME=[%s] CMDRESULT=[%s] CMDRESULTTIME=[%s]",
															sidS, cmdary[0], cmdary[1], cmdary[2], cmdary[3], cmdary[4], cmdary[5]));
										else
											System.out.println(
													String.format("SVRID=[%s] BRWS=[%s] AUID=[%s] CMD=[%s] CMDCREATETIME=[%s] CMDRESULT=[%s] CMDRESULTTIME=[%s]",
															cmdary[0], brwsS, cmdary[1], cmdary[2], cmdary[3], cmdary[4], cmdary[5]));
									else
										System.out.println(
											String.format("SVRID=[%s] BRWS=[%s] AUID=[%s] CMD=[%s] CMDCREATETIME=[%s] CMDRESULT=[%s] CMDRESULTTIME=[%s]",
													sidS, brwsS, cmdary[0], cmdary[1], cmdary[2], cmdary[3], cmdary[4]));
								} else
									System.out.println(
											"!!! data object node=[" + cmdary[0] + "] format error !!!");
							} else
								System.out.println("!!!current row data error [" + s + "]");
						}
					jsel2ins.CloseConnect();
					jsel2ins = null;
				} else {
					if (setcmdS.length() > 0) {//20201119 set command for brws
						dcf = new DynamicProps("rateprtservice.xml");
						url = dcf.getConHashMap().get("system.db[@url]");
						user = dcf.getConHashMap().get("system.db[@user]");
						usinsg = dcf.getConHashMap().get("system.db[@pass]");
						if (sidC.length() <= 0) {
							String cmdtbname = dcf.getConHashMap().get("system.devcmdtb[@name]");
							String cmdtbmkey = dcf.getConHashMap().get("system.devcmdtb[@mkey]");
							String cmdtbfields = "AUID,CMD,CMDCREATETIME,CMDRESULT,CMDRESULTTIME,EMPNO";
							GwDao jselonefield = null;
							String svrprmtbname = dcf.getConHashMap().get("system.svrprmtb[@name]");
							String svrprmtbmkey = dcf.getConHashMap().get("system.svrprmtb[@mkey]");
							String svrprmtbfields = "AUID";
							String auidS = "";

							if (jsel2ins == null) {
								jsel2ins = new GwDao(url, user, usinsg, false);
								jselonefield = new GwDao(url, user, usinsg, false);
								auidS = jselonefield.SELONEFLD(svrprmtbname, svrprmtbfields, svrprmtbmkey, sidS, true)
										.trim();
								System.out.println("current AUID=" + auidS);
							}
							SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
							String t = sdf.format(new java.util.Date());
							String updValue = String.format(updcmdPtrn, Integer.parseInt(auidS), setcmdS, t, "",
									sdf.format(0l), userName);
							//20240503 mark up updValue System.out.println("update " + updValue + " ");
							int row = jsel2ins.UPSERT(cmdtbname, cmdtbfields, updValue, cmdtbmkey, sidS + ",'" + brwsS + "'");
							System.out.println("total " + row + " records update");
							jsel2ins.CloseConnect();
							jsel2ins = null;
							if (jselonefield != null)
								jselonefield.CloseConnect(); //20210413 MatsudairaSyuMe prevent Null Dereference
							//20240510 Poor Style: Value Never Read jselonefield = null;
						} else {//20201119 set command for svrid
							String cmdtbname = dcf.getConHashMap().get("system.svrcmdtb[@name]");
							String cmdtbmkey = "SVRID";
							String cmdtbfields = "IP, CMD,CMDCREATETIME,CMDRESULT,CMDRESULTTIME,EMPNO";
							GwDao jselonefield = null;
							String svrprmtbname = dcf.getConHashMap().get("system.svrprmtb[@name]");
							String svrprmtbmkey = dcf.getConHashMap().get("system.svrprmtb[@mkey]");
							String svrprmtbfields = "IP";
							String ipS = "";

							if (jsel2ins == null) {
								jsel2ins = new GwDao(url, user, usinsg, false);
								jselonefield = new GwDao(url, user, usinsg, false);
								ipS = jselonefield.SELONEFLD(svrprmtbname, svrprmtbfields, svrprmtbmkey, sidC, true)
										.trim();
								System.out.println("current IP=" + ipS);
							}
							SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
							String t = sdf.format(new java.util.Date());
							String updValue = String.format(updsvrcmdPtrn, ipS, setcmdS, t, "",
									sdf.format(0l), userName);
							//20240503 mark up updValue System.out.println("update " + updValue + " ");
							int row = jsel2ins.UPSERT(cmdtbname, cmdtbfields, updValue, cmdtbmkey,
									sidC);
							System.out.println("total " + row + " records update");
							jsel2ins.CloseConnect();
							jsel2ins = null;
							if (jselonefield != null)
								jselonefield.CloseConnect();//20210413 MatsudairaSyuMe prevent Null Dereference
							//20240510 Poor Style: Value Never Read jselonefield = null;
						}
					} else if (brwsS.length() > -1 || sidS.length() > -1) {
						dcf = new DynamicProps("rateprtservice.xml");
						url = dcf.getConHashMap().get("system.db[@url]");
						user = dcf.getConHashMap().get("system.db[@user]");
						usinsg = dcf.getConHashMap().get("system.db[@pass]");
						String svrstatustbname = dcf.getConHashMap().get("system.svrstatustb[@name]");
						String svrsstatustbmkey = dcf.getConHashMap().get("system.svrstatustb[@mkey]");
						String svrstatustbfields = dcf.getConHashMap().get("system.svrstatustb[@fields]");
						String devstatustbname = dcf.getConHashMap().get("system.statustb[@name]");
						String devstatustbmkey = "BRWS";
						String devstatustbfields = "BRWS," + dcf.getConHashMap().get("system.statustb[@fields]");
						if (jsel2ins == null)
							jsel2ins = new GwDao(url, user, usinsg, false);
						if (brwsS.length() < 1) {
							String[] cmd = null;
							if (sidS.length() > 0)
								cmd = jsel2ins.SELMFLD(svrstatustbname, svrstatustbfields, svrsstatustbmkey, sidS,
										false);
							else
								cmd = jsel2ins.SELMFLDNOIDX(svrstatustbname, svrstatustbfields, false);
							if (cmd != null && cmd.length > 0)
								for (String s : cmd) {
									s = s.trim();
									System.out.println("current row data [" + s + "]");
									if (s.length() > 0 && s.indexOf(',') > -1) {
										String[] cmdary = s.split(",");
										if (cmdary.length > 1) {
											//20210726 change cmdary from 2,3,4,7 to 1,2,3,6
											System.out.println(
													String.format("SVRIP=[%s] CURSTS=[%s] PID=[%s] lastupdatetime=[%s]",
															cmdary[1], cmdary[2], cmdary[3], cmdary[6]));
											//20210726----
										} else
											System.out.println(
													"!!! data object node=[" + cmdary[0] + "] format error !!!");
									} else
										System.out.println("!!!current row data error [" + s + "]");
								}
						} else {
							String[] cmd = null;
							if (brwsS.trim().equals("*"))
								cmd = jsel2ins.SELMFLDNOIDX(devstatustbname, devstatustbfields, false);
							else
								cmd = jsel2ins.SELMFLD(devstatustbname, devstatustbfields, devstatustbmkey, brwsS,
										false);

							if (cmd != null && cmd.length > 0)
								for (String s : cmd) {
									s = s.trim();
									if (s.length() > 0 && s.indexOf(',') > -1) {
										String[] cmdary = s.split(",");
										if (cmdary.length > 1) {
											System.out.println(String.format(
													"BRWS=[%s] DEVIP=[%s] DEVPORT=[%s] SVRIP=[%s] SVRPORT=[%s] DEVTYPE=[%s] CURSTUS=[%s]",
													cmdary[0], cmdary[1], cmdary[2], cmdary[3], cmdary[4], cmdary[6],
													cmdary[7]));
										} else
											System.out.println(
													"!!! data object node=[" + cmdary[0] + "] format error !!!");
									} else
										System.out.println("!!!current row data error [" + s + "]");
								}
						}
						jsel2ins.CloseConnect();
						jsel2ins = null;
					} else
						System.err.println(String.format("error situation sidS=%s brwsS=%s setcmdS=%s", sidS, brwsS, setcmdS));
				}
			} else {
				System.out.println("Cli -h show help message");
				System.out.println("Cli -s {sid}:                                 list status of service sid");
				System.out.println("Cli -s :                                      list status of all services");
				System.out.println("Cli -s {sid}:*                                list status of all devices by service sid");
				System.out.println("Cli -s {sid}:{BRWS}                           list status of device BRWS by service sid");
				System.out.println("Cli -s :{BRWS} or {BRWS}                      list status of device BRWS");
				System.out.println("Cli -c {sid}: {START|STOP|RESTART}            set command to service sid");
				System.out.println("Cli -c {sid}:{BRWS} {START|STOP|RESTART}      set command to device BRWS by service sid");
//				System.out.println("Cli -c :{BRWS} or {BRWS} {START|STOP|RESTART} set command to device BRWS");
				System.out.println("Cli -cr {sid}:{BRWS}                          list command status of device BRWS by service sid");
				System.out.println("Cli -cr {sid}:                                list command status of all device by service sid");
				System.out.println("Cli -cr :{BRWS} or {BRWS}                     list command status of device BRWS");
			}
		} catch (Exception e) {
			//20240503 MatsudairaSyuMe mark for System Information Leak e.printStackTrace();
			System.err.println("error: exception"); //20240503 change log message
		}
	}
}
