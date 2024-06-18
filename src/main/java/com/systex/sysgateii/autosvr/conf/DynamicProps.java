package com.systex.sysgateii.autosvr.conf;

/*
 * DynamicProps
 * reading configuration files
 *    Auto detect configuration changed
 * MatsudairaSyuMe
 * Ver 1.0
 *  20190727 
 */

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration2.ConfigurationMap;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.ConfigurationBuilderEvent;
import org.apache.commons.configuration2.builder.ReloadingFileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.event.EventListener;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.reloading.PeriodicReloadingTrigger;
import org.apache.commons.configuration2.tree.DefaultExpressionEngine;
import org.apache.commons.configuration2.tree.DefaultExpressionEngineSymbols;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.systex.sysgateii.autosvr.Server;
import com.systex.sysgateii.autosvr.dao.GwDao;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import com.systex.sysgateii.autosvr.util.DecryptUtil;//20240508
public class DynamicProps {
	private static Logger log = LoggerFactory.getLogger(DynamicProps.class);
	private ReloadingFileBasedConfigurationBuilder<XMLConfiguration> builder = null;
	private final ConcurrentHashMap<String, String> conHashMap = new ConcurrentHashMap<String, String>();
	private final CopyOnWriteArrayList<String> prtbrws = new CopyOnWriteArrayList<String>();
	private final CopyOnWriteArrayList<String> prttype = new CopyOnWriteArrayList<String>();
	private final CopyOnWriteArrayList<String> prtcltip = new CopyOnWriteArrayList<String>();
	private final CopyOnWriteArrayList<String> prtcltautoturnpage = new CopyOnWriteArrayList<String>();
	private final CopyOnWriteArrayList<ConcurrentHashMap<String, Object>> cfgPrtMapList = new CopyOnWriteArrayList<ConcurrentHashMap<String, Object>>();
	//20200912
	private boolean readCfgFromCenter = false;
	GwDao jsel2ins = null;
	//----
	//20200926
	private String auid = "";
	private String svrip = "";
	//----
	static private String decryptedPd = ""; //20240508
	public DynamicProps(String string) {
		Parameters params = new Parameters();
		//20230328 MatsudairaSyuMe fix "Path Manipulation Vulnerability"
		string = "rateprtservice.xml";
		File file = new File(string);
       //20230328 MatsudairaSyuMe end
		builder = new ReloadingFileBasedConfigurationBuilder<XMLConfiguration>(XMLConfiguration.class)
				.configure(params.fileBased().setFile(file)); //20230328 change to use File(name)
		try {
			ChkCfg(builder.getConfiguration());
		} catch (ConfigurationException e) {
			// TODO Auto-generated catch block
			//20240503 MatsudairaSyuMe mark for System Information Leak e.printStackTrace();
			log.error("error : i/o exception");//20240503 change log message
		}
		PeriodicReloadingTrigger trigger = new PeriodicReloadingTrigger(builder.getReloadingController(), null, 5,
				TimeUnit.SECONDS);
		trigger.start();

		builder.addEventListener(ConfigurationBuilderEvent.ANY, new EventListener<ConfigurationBuilderEvent>() {
			public void onEvent(ConfigurationBuilderEvent event) {
				log.debug("rateprtservice.xml been modified Event: {}", event.getEventType().getName());
				if (event.getEventType() == ConfigurationBuilderEvent.RESET) {
					XMLConfiguration config;
					synchronized (this) {
						try {
							config = builder.getConfiguration();
							DefaultExpressionEngine engine = new DefaultExpressionEngine(
									DefaultExpressionEngineSymbols.DEFAULT_SYMBOLS);
							// 指定表達示引擎
							config.setExpressionEngine(engine);
							Map<Object, Object> cfg = new ConfigurationMap(config);
							//20201116
							//cfg.entrySet();
							//----
							for (@SuppressWarnings("rawtypes")
							Map.Entry entry : cfg.entrySet()) {
								log.info("modified Event ConfProc info! {}, {}", entry.getKey(), entry.getValue());
								if (entry.getKey().equals("system.ip") || entry.getKey().equals("system.port")
										//20200912 read data from center
										|| entry.getKey().equals("center")
										//20201115
										|| entry.getKey().equals("bkno")
										//----
										//20210629
										|| entry.getKey().equals("workno")
										//----
										//20220427
										|| entry.getKey().equals("reqtime")
										|| entry.getKey().equals("chgidletime")
										//----
										//----
										|| entry.getKey().equals("system.logpath")
										//20200815
										|| entry.getKey().equals("system.svrid")
										//----
										|| entry.getKey().equals("svrsubport.svrip")
										|| entry.getKey().equals("svrsubport.svrport")
										|| entry.getKey().equals("svrsubport.localip")
										|| entry.getKey().equals("svrsubport.localport")
										|| entry.getKey().equals("svrsubport.recvtimeout")
										//20201116 cancel verhbrno
										//|| entry.getKey().equals("svrsubport.verhbrno")
										//----
										|| entry.getKey().equals("svrsubport.verhwsno")
										|| entry.getKey().equals("boards.board.brno")
										//20200513 add for data base connect
										|| entry.getKey().equals("system.db[@url]")
										|| entry.getKey().equals("system.db[@user]")
										|| entry.getKey().equals("system.db[@pass]")
										|| entry.getKey().equals("system.statustb[@name]")
										|| entry.getKey().equals("system.statustb[@mkey]")
										|| entry.getKey().equals("system.statustb[@fields]")
										//20201106
										|| entry.getKey().equals("system.dmtb[@name]")
										|| entry.getKey().equals("system.dmtb[@mkey]")
										|| entry.getKey().equals("system.dmtb[@fields]")
										//20201026
										|| entry.getKey().equals("system.devcmdhistb[@name]")
										|| entry.getKey().equals("system.devcmdhistb[@mkey]")
										|| entry.getKey().equals("system.devcmdhistb[@fields]")
										|| entry.getKey().equals("system.devamtb[@name]")
										|| entry.getKey().equals("system.devamtb[@mkey]")
										|| entry.getKey().equals("system.devamtb[@fields]")
										//----
										//20201119
										|| entry.getKey().equals("system.svrcmdtb[@name]")
										|| entry.getKey().equals("system.svrcmdtb[@mkey]")
										|| entry.getKey().equals("system.svrcmdtb[@fields]")
										|| entry.getKey().equals("system.svrcmdhistb[@name]")
										|| entry.getKey().equals("system.svrcmdhistb[@mkey]")
										|| entry.getKey().equals("system.svrcmdhistb[@fields]")
										//----
										//20200926
										|| entry.getKey().equals("system.svrstatustb[@name]")
										|| entry.getKey().equals("system.svrstatustb[@mkey]")
										|| entry.getKey().equals("system.svrstatustb[@fields]")
										//----
										//20200815
										|| entry.getKey().equals("system.svrtbsdytb[@name]")
										|| entry.getKey().equals("system.svrtbsdytb[@mkey]")
										|| entry.getKey().equals("system.svrtbsdytb[@fields]")
										//----
										//20200901
										|| entry.getKey().equals("system.devcmdtb[@name]")
										|| entry.getKey().equals("system.devcmdtb[@mkey]")
										|| entry.getKey().equals("system.devcmdtb[@fields]")
										//----
										//20200912 param tab
										|| entry.getKey().equals("system.svrprmtb[@name]")
										|| entry.getKey().equals("system.svrprmtb[@mkey]")
										|| entry.getKey().equals("system.svrprmtb[@fields]")
										|| entry.getKey().equals("system.fasprmtb[@name]")
										|| entry.getKey().equals("system.fasprmtb[@mkey]")
										|| entry.getKey().equals("system.fasprmtb[@fields]")
										|| entry.getKey().equals("system.devprmtb[@name]")
										|| entry.getKey().equals("system.devprmtb[@mkey]")
										|| entry.getKey().equals("system.devprmtb[@fields]")
										//----
										|| entry.getKey().equals("boards.board.ip")) {
									//20200912
									if (entry.getKey().equals("center")) {
										if (entry.getValue().toString().trim().length() > 0)
											readCfgFromCenter = true;
										log.info("configure will read other data from center repository");
									}
									//----
									//20201116
									if (Server.getSvrId() > 0 && entry.getKey().equals("system.svrid")) {
										conHashMap.put(entry.getKey().toString(), Integer.toString(Server.getSvrId()));
										log.info("!!!! Change to use given svrid [{}]=[{}]", entry.getKey(), Integer.toString(Server.getSvrId()));
									} else
									//----
										conHashMap.put(entry.getKey().toString(), entry.getValue().toString());
									log.info("ConfProc put to config map info! {}, {}", entry.getKey(),
											entry.getValue());
								} else {
									String schk = entry.getKey().toString().trim();
									String sv = entry.getValue().toString().trim();
									if (sv.startsWith("["))
										sv = sv.substring(1);
									if (sv.endsWith("]"))
										sv = sv.substring(0, sv.length() - 1);
									String[] svary = sv.split(",");
									for (String value : svary) {
										switch (schk) {
										case "validDevice.dev[@brws]":
											prtbrws.add(value.trim());
											log.info("validDevice.dev[@brws] value ={}", value.trim());
											break;
										case "validDevice.dev[@type]":
											prttype.add(value.trim());
											log.info("validDevice.dev[@type] value ={}", value.trim());
											break;
										case "validDevice.dev[@ip]":
											prtcltip.add(value.trim());
											log.info("validDevice.dev[@ip] value ={}", value.trim());
											break;
										case "validDevice.dev[@autoturnpage]":
											prtcltautoturnpage.add(value.trim());
											log.info("validDevice.dev[@autoturnpage] value ={}", value.trim());
											break;
										default:
											log.error("unknow key={} value ={}", schk, value);
											break;
										}
									}
								}
							}
						} catch (ConfigurationException e) {
							// TODO Auto-generated catch block
							//20240503 MAtsudairaSyuMe mark for System Information Leak e.printStackTrace();
							log.error("error : i/o exception");//20240503 change log message
							log.info("[0000] : rateprtservice.xml is not well formed! PrnSrv");
						}
					}
					//20200912
					if (readCfgFromCenter) {
						String SvrId = conHashMap.get("system.svrid").trim();
						log.info("start to load from repository current SVRID=[{}]", SvrId);
						String fromurl = conHashMap.get("system.db[@url]").trim();
						String fromuser = conHashMap.get("system.db[@user]").trim();
						String frompass = conHashMap.get("system.db[@pass]").trim();
						String svrprmtb = conHashMap.get("system.svrprmtb[@name]").trim();
						String svrprmkey = conHashMap.get("system.svrprmtb[@mkey]").trim();
						String svrprmfields = conHashMap.get("system.svrprmtb[@fields]").trim();
						String fasprmtb = conHashMap.get("system.fasprmtb[@name]").trim();
						String fasprmkey = conHashMap.get("system.fasprmtb[@mkey]").trim();
						String fasprmfields = conHashMap.get("system.fasprmtb[@fields]").trim();
						String devprmtb = conHashMap.get("system.devprmtb[@name]").trim();
						String devprmkey = conHashMap.get("system.devprmtb[@mkey]").trim();
						String devprmfields = conHashMap.get("system.devprmtb[@fields]").trim();
						//20201116 change to given svrid
						prtbrws.clear();
						prttype.clear();
						prtcltip.clear();
						prtcltautoturnpage.clear();
						//----
						if (fromurl.length() <= 0 ||fromuser.length() <= 0 || frompass.length()<= 0
								|| svrprmtb.length() <= 0 || svrprmkey.length() <= 0 || svrprmfields.length() <= 0
								|| fasprmtb.length() <= 0 || fasprmkey.length() <= 0 || fasprmfields.length() <= 0
								|| devprmtb.length() <= 0 || devprmkey.length() <= 0 || devprmfields.length() <= 0 || SvrId.length() <= 0)
							log.info("connect parameters error {} {} {} SvrID=[{}]", fromurl, fromuser, frompass, SvrId);
						else {
							log.info("using {} {} {}", fromurl, fromuser, frompass);
							log.info("svrprmtb {} {} {}", svrprmtb, svrprmkey, svrprmfields);
							log.info("fasprmtb {} {} {}", fasprmtb, fasprmkey, fasprmfields);
							log.info("devprmtb {} {} {}", devprmtb, devprmkey, devprmfields);
							try {
								jsel2ins = new GwDao(fromurl, fromuser, frompass, false);
								//20240523 Poor Style: Value Never Read String origStr = "";
								log.debug("current svrprmtb=[{}] svrprmtbkey=[{}] svrprmtbsearkey=[{}]", svrprmtb, svrprmkey, SvrId);
								String[] svrflds = jsel2ins.SELMFLD(svrprmtb, svrprmfields, svrprmkey, SvrId, false);
								String[] svrfldsary = null; // store values of AUID,BRNO,IP,PORT,DEVTPE,RECVTIMEOUT,LOGPATH
								String fasfld = ""; // store values of CONNPRM
								String[] devfldsary = null; // store values of BRWS,IP,PORT,DEVTPE,AUTOTURNPAGE
								//20201116 change to use given svrid
								if(svrflds != null && svrflds.length > 0) {
									for (String s: svrflds) {
										s = s.trim();
										log.atDebug().setMessage("current svrfld [{}]").addArgument(s).log();//20240517 change for Log Forging(debug)
										if (s.length() > 0 && s.indexOf(',') > -1) {
											svrfldsary = s.split(",");
											for (int idx = 0; idx < svrfldsary.length; idx++) {
												switch (idx) {
													//20200925
													case 0:
														setAuid(svrfldsary[idx].trim());
														log.debug("SERVICE parameter [{}] set auid [{}]", idx, svrfldsary[idx]);
														break;
													//----
														//20201116 cancel verhbrno
														/*
													case 1:
														origStr = conHashMap.get("svrsubport.verhbrno").trim();
														conHashMap.put("svrsubport.verhbrno", svrfldsary[idx].trim());
														log.debug("SERVICE parameter [{}] set svrsubport.verhbrno [{}]", idx, svrfldsary[idx]);
														break;
													case 2:
														log.debug("SERVICE parameter [{}] prepare to set service ip for device [{}]", idx, svrfldsary[idx]);
														break;
													case 3:
														log.debug("SERVICE parameter [{}] prepare to set service port for device [{}]", idx, svrfldsary[idx]);
														break;
													case 4:
														log.debug("SERVICE parameter [{}] [{}]", idx, svrfldsary[idx]);
														break;
													case 5:
														origStr = conHashMap.get("svrsubport.recvtimeout").trim();
														conHashMap.put("svrsubport.recvtimeout", svrfldsary[idx].trim());
														log.debug("SERVICE parameter [{}] svrsubport.recvtimeout [{}]", idx, svrfldsary[idx]);
														break;
													case 6:
														origStr = conHashMap.get("system.logpath").trim();
														conHashMap.put("system.logpath", svrfldsary[idx].trim());
														log.debug("SERVICE parameter [{}] set system.logpath [{}]", idx, svrfldsary[idx]);
														break;
														*/
													case 1:
														log.debug("SERVICE parameter [{}] prepare to set service ip for device [{}]", idx, svrfldsary[idx]);
														break;
													case 2:
														log.debug("SERVICE parameter [{}] prepare to set service port for device [{}]", idx, svrfldsary[idx]);
														break;
													case 3:
														log.debug("SERVICE parameter [{}] [{}]", idx, svrfldsary[idx]);
														break;
													case 4:
														//20240510 Poor Style: Value Never Read origStr = conHashMap.get("svrsubport.recvtimeout").trim();
														conHashMap.put("svrsubport.recvtimeout", svrfldsary[idx].trim());
														log.debug("SERVICE parameter [{}] svrsubport.recvtimeout [{}]", idx, svrfldsary[idx]);
														break;
													case 5:
														//20240520 Poor Style: Value Never Read origStr = conHashMap.get("system.logpath").trim();
														conHashMap.put("system.logpath", svrfldsary[idx].trim());
														log.debug("SERVICE parameter [{}] set system.logpath [{}]", idx, svrfldsary[idx]);
														break;
													default:
														break;
												}
											}
										} else
											log.error("!!!!SERVICE parameters in service table [{}] error !!!", svrprmtb);
									}
								if (svrfldsary != null && svrfldsary.length > 5) { //20210413 MatsudairaSyuMe prevent Null Dereference
									log.debug("current fasprmtb=[{}] fasprmtbkey=[{}] fasprmtbsearkey=[{}]", fasprmtb,
											fasprmkey, svrfldsary[0]);
									String[] fasflds = jsel2ins.SELMFLD(fasprmtb, fasprmfields, fasprmkey,
											svrfldsary[0], false);
									if (fasflds != null && fasflds.length > 0)
										for (String s : fasflds) {
											s = s.trim();
											fasfld = s;
											//20240520 Poor Style: Value Never Read origStr = conHashMap.get("svrsubport.svrip").trim();
											conHashMap.put("svrsubport.svrip", fasfld);
											log.atDebug().setMessage("current fasfld set svrsubport.svrip [{}]").addArgument(fasfld).log();//20240517 change for Log Forging(debug)
										}
									log.debug("current devprmtb=[{}] devprmtbkey=[{}] devprmtbsearkey=[{}]", devprmtb,
											devprmkey, SvrId);
									String[] devflds = jsel2ins.SELMFLD(devprmtb, devprmfields, devprmkey, SvrId,
											false);
									String prtcltipStr = "";
									/*20201116 change to use given svrid
									prtbrws.clear();
									prttype.clear();
									prtcltip.clear();
									prtcltautoturnpage.clear();
									*/
									if (devflds != null && devflds.length > 0)
										for (String s : devflds) {
											s = s.trim();
											log.atDebug().setMessage("current devflds [{}]").addArgument(s).log();//20240517 change for Log Forging(debug)
											if (s.length() > 0 && s.indexOf(',') > -1) {
												devfldsary = s.split(",");
												for (int idx = 0; idx < devfldsary.length; idx++) {
													switch (idx) {
													case 0:
														prtbrws.add(devfldsary[idx].trim());
														log.debug("DEVICE parameter [{}] set prtbrws [{}]", idx,
																devfldsary[idx].trim());
														break;
													case 1:
														// 20200926
														setSvrip(svrfldsary[idx]);
														// ----
														log.debug("DEVICE parameter [{}] set ip for prtcltip [{}]", idx,
																devfldsary[idx].trim());
														break;
													case 2:
														// localhost:4002=localhost:3301
														// 20201116 cancel vhbrno
//														prtcltipStr =  devfldsary[1].trim() + ":"+ devfldsary[idx].trim() + "=" + svrfldsary[2].trim() + ":" + svrfldsary[3].trim();
														prtcltipStr = devfldsary[1].trim() + ":"
																+ devfldsary[idx].trim() + "=" + svrfldsary[1].trim()
																+ ":" + svrfldsary[2].trim();
														prtcltip.add(prtcltipStr);
														log.debug(
																"DEVICE parameter [{}] set port [{}] for prtcltip [{}]",
																idx, devfldsary[idx], prtcltipStr);
														break;
													case 3:
														if (devfldsary[idx].trim().equalsIgnoreCase("2")) {
															prttype.add("AUTO46");
															log.debug("DEVICE parameter [{}] set prttype [{}]", idx,
																	"AUTO46");
														} else if (devfldsary[idx].trim().equalsIgnoreCase("3")) {
															prttype.add("AUTO52"); // error set to "AUTO46
															log.debug("DEVICE parameter [{}] set prttype [{}]", idx,
																	"AUTO52");
														} else {
															log.debug("DEVICE parameter [{}] not autoprinter type",
																	idx);
															prttype.add("NOTAUTOPRT"); // //20201110 check for autoprt
																						// deivce
														}
														break;
													case 4:
														if (devfldsary[idx].trim().equalsIgnoreCase("N")) {
															prtcltautoturnpage.add("false");
															log.debug(
																	"DEVICE parameter [{}] set prtcltautoturnpage [{}]",
																	idx, "false");
														} else {
															prtcltautoturnpage.add("true");
															log.debug(
																	"DEVICE parameter [{}] set prtcltautoturnpage [{}]",
																	idx, "true");
														}
														break;
													default:
														break;
													}
												}

											} else
												log.error("!!!!DEVICE parameters in [{}] error !!!", devprmtb);
										}
								} else
									log.error("!!!!field parameters in fas parameter table [{}] error !!!", fasprmtb);
								} else { //20201116 add check given svrid
									log.error("!!!!svrid  not exist in table [{}] error !!!", SvrId);									
								}
								//----
								jsel2ins.CloseConnect();
								jsel2ins = null;
							} catch (Exception e) {
								//20240503 MatsudairaSyuMe mark for System Information Leak e.printStackTrace();
								log.info("monitorThread read database error exception");//20240503 change log message
							}

						}
					}
					//----
					if (prtbrws != null && prtbrws.size() > 0) {
						for (int i = 0; i < prtbrws.size(); i++) {
							if (!prttype.get(i).trim().equalsIgnoreCase("NOTAUTOPRT")) { //20201110 check for autoprt deivce
								ConcurrentHashMap<String, Object> cfgPrtMap = new ConcurrentHashMap<String, Object>();
								cfgPrtMap.put("brws", prtbrws.get(i));
								cfgPrtMap.put("type", prttype.get(i));
								cfgPrtMap.put("ip", prtcltip.get(i));
								cfgPrtMap.put("autoturnpage", prtcltautoturnpage.get(i));
								cfgPrtMapList.add(cfgPrtMap);
								log.info("RESET cfgPrtMapList add idx={}", i);
							}
						}
					}
				}
			}
		});
	}

	public void Chat() throws InterruptedException, ConfigurationException {
		checkresult(this.builder);
	}

	private static void checkresult(ReloadingFileBasedConfigurationBuilder<XMLConfiguration> builder)
			throws InterruptedException, ConfigurationException {
		while (true) {
			Thread.sleep(1000);
			getcfg(builder.getConfiguration());
		}
	}

	public static void getcfg(XMLConfiguration config) {

		DefaultExpressionEngine engine = new DefaultExpressionEngine(DefaultExpressionEngineSymbols.DEFAULT_SYMBOLS);
		// 指定表達示引擎
		config.setExpressionEngine(engine);

		System.out.println(config.getInt("boards.board.brno"));
		System.out.println(config.getInt("boards.board.id"));
		System.out.println(config.getString("boards.board.ip"));
		System.out.println(config.getString("boards.board.start[@description]"));
	}

	@SuppressWarnings("rawtypes")
	private final void ChkCfg(XMLConfiguration config) throws ConfigurationException {  //20240503 change to private final void for Code Correctness: Constructor Invokes Overridable Function

		DefaultExpressionEngine engine = new DefaultExpressionEngine(DefaultExpressionEngineSymbols.DEFAULT_SYMBOLS);
		// 指定表達示引擎
		config.setExpressionEngine(engine);
		synchronized (this) {
			Map<Object, Object> cfg = new ConfigurationMap(config);
//			cfg.entrySet();
			//20201116
			//20240508
			if (config.containsKey("system.db[@pass]")) {
				String encryptedPd = config.getString("system.db[@pass]");
				try {
					this.decryptedPd = DecryptUtil.decryptPd(encryptedPd);
				} catch (Exception e) {
					log.error("decryptPd fail");
				}
			}
			//----
			for (@SuppressWarnings("rawtypes")
			Map.Entry entry : cfg.entrySet()) {
				if (!"system.db[@pass]".equals(entry.getKey().toString())) { // 20240604 log.info excluded pass
				//20200420 add logpath
				log.info("ChkCfg ConfProc info! {}, {} len={}", entry.getKey(), entry.getValue().toString(),
						entry.getValue().toString().length());
	            } //--
				if (entry.getKey().equals("system.ip") || entry.getKey().equals("system.port")
						//20200912 read data from center
						|| entry.getKey().equals("center")
						//----
						//20201115
						|| entry.getKey().equals("bkno")
						//----
						//20210629
						|| entry.getKey().equals("workno")
						//----
						//20220427
                        || entry.getKey().equals("reqtime")
                        || entry.getKey().equals("chgidletime")
						//----
						|| entry.getKey().equals("system.logpath")
						//20200815
						|| entry.getKey().equals("system.svrid")
						//----
						|| entry.getKey().equals("svrsubport.svrip") || entry.getKey().equals("svrsubport.svrport")
						|| entry.getKey().equals("svrsubport.localip") || entry.getKey().equals("svrsubport.localport")
						|| entry.getKey().equals("svrsubport.recvtimeout")
						//20201116 canceel verhbrno
//						|| entry.getKey().equals("svrsubport.verhbrno") || entry.getKey().equals("svrsubport.verhwsno")
						|| entry.getKey().equals("svrsubport.verhwsno")
						//----
						//20200513 add for data base connect
						|| entry.getKey().equals("system.db[@url]")
						|| entry.getKey().equals("system.db[@user]")
						|| entry.getKey().equals("system.db[@pass]")
						|| entry.getKey().equals("system.statustb[@name]")
						|| entry.getKey().equals("system.statustb[@mkey]")
						|| entry.getKey().equals("system.statustb[@fields]")
						//20201106
						|| entry.getKey().equals("system.dmtb[@name]")
						|| entry.getKey().equals("system.dmtb[@mkey]")
						|| entry.getKey().equals("system.dmtb[@fields]")
						//20201026
						|| entry.getKey().equals("system.devcmdhistb[@name]")
						|| entry.getKey().equals("system.devcmdhistb[@mkey]")
						|| entry.getKey().equals("system.devcmdhistb[@fields]")
						|| entry.getKey().equals("system.devamtb[@name]")
						|| entry.getKey().equals("system.devamtb[@mkey]")
						|| entry.getKey().equals("system.devamtb[@fields]")
						//----
						//20201119
						|| entry.getKey().equals("system.svrcmdtb[@name]")
						|| entry.getKey().equals("system.svrcmdtb[@mkey]")
						|| entry.getKey().equals("system.svrcmdtb[@fields]")
						|| entry.getKey().equals("system.svrcmdhistb[@name]")
						|| entry.getKey().equals("system.svrcmdhistb[@mkey]")
						|| entry.getKey().equals("system.svrcmdhistb[@fields]")
						//----
						//20200926
						|| entry.getKey().equals("system.svrstatustb[@name]")
						|| entry.getKey().equals("system.svrstatustb[@mkey]")
						|| entry.getKey().equals("system.svrstatustb[@fields]")
						//----
						//20200815
						|| entry.getKey().equals("system.svrtbsdytb[@name]")
						|| entry.getKey().equals("system.svrtbsdytb[@mkey]")
						|| entry.getKey().equals("system.svrtbsdytb[@fields]")
						//----
						//20200901
						|| entry.getKey().equals("system.devcmdtb[@name]")
						|| entry.getKey().equals("system.devcmdtb[@mkey]")
						|| entry.getKey().equals("system.devcmdtb[@fields]")
						//----
						//20200912 param tab
						|| entry.getKey().equals("system.svrprmtb[@name]")
						|| entry.getKey().equals("system.svrprmtb[@mkey]")
						|| entry.getKey().equals("system.svrprmtb[@fields]")
						|| entry.getKey().equals("system.fasprmtb[@name]")
						|| entry.getKey().equals("system.fasprmtb[@mkey]")
						|| entry.getKey().equals("system.fasprmtb[@fields]")
						|| entry.getKey().equals("system.devprmtb[@name]")
						|| entry.getKey().equals("system.devprmtb[@mkey]")
						|| entry.getKey().equals("system.devprmtb[@fields]")
						//----
						|| entry.getKey().equals("boards.board.brno") || entry.getKey().equals("boards.board.ip")) {
					//20200912
					if (entry.getKey().equals("center")) {
						if (entry.getValue().toString().trim().length() > 0)
							readCfgFromCenter = true;
						log.info("configure will read other data from center repository");
					}
					//----
					//20201116
					if (Server.getSvrId() > 0 && entry.getKey().equals("system.svrid")) {
						conHashMap.put(entry.getKey().toString(), Integer.toString(Server.getSvrId()));
						log.info("!!!! Change to use given svrid [{}]=[{}]", entry.getKey(), Integer.toString(Server.getSvrId()));
					} else
					//----
						conHashMap.put(entry.getKey().toString(), entry.getValue().toString());
						if ("system.db[@pass]".equals(entry.getKey().toString())) {	 // 20240604 log.info excluded pass
							continue;
						} // --
					log.info("ConfProc put to config map info! {}, {}", entry.getKey(), entry.getValue());
				} else {
					String schk = entry.getKey().toString().trim();
					String sv = entry.getValue().toString().trim();
					if (sv.startsWith("["))
						sv = sv.substring(1);
					if (sv.endsWith("]"))
						sv = sv.substring(0, sv.length() - 1);
					String[] svary = sv.split(",");
					for (String value : svary) {
						switch (schk) {
						case "validDevice.dev[@brws]":
							prtbrws.add(value.trim());
							log.info("validDevice.dev[@brws] value ={}", value.trim());
							break;
						case "validDevice.dev[@type]":
							prttype.add(value.trim());
							log.info("validDevice.dev[@type] value ={}", value.trim());
							break;
						case "validDevice.dev[@ip]":
							prtcltip.add(value.trim());
							log.info("validDevice.dev[@ip] value ={}", value.trim());
							break;
						case "validDevice.dev[@autoturnpage]":
							prtcltautoturnpage.add(value.trim());
							log.info("validDevice.dev[@autoturnpage] value ={}", value.trim());
							break;
						default:
							log.error("unknow key={} value ={}", schk, value);
							break;
						}
					}
				}
			}
			//20200912
			if (readCfgFromCenter) {
				String SvrId = conHashMap.get("system.svrid").trim();
				log.info("start to load from repository current SVRID=[{}]", SvrId);
				String fromurl = conHashMap.get("system.db[@url]").trim();
				String fromuser = conHashMap.get("system.db[@user]").trim();
				String frompass = conHashMap.get("system.db[@pass]").trim();
				String svrprmtb = conHashMap.get("system.svrprmtb[@name]").trim();
				String svrprmkey = conHashMap.get("system.svrprmtb[@mkey]").trim();
				String svrprmfields = conHashMap.get("system.svrprmtb[@fields]").trim();
				String fasprmtb = conHashMap.get("system.fasprmtb[@name]").trim();
				String fasprmkey = conHashMap.get("system.fasprmtb[@mkey]").trim();
				String fasprmfields = conHashMap.get("system.fasprmtb[@fields]").trim();
				String devprmtb = conHashMap.get("system.devprmtb[@name]").trim();
				String devprmkey = conHashMap.get("system.devprmtb[@mkey]").trim();
				String devprmfields = conHashMap.get("system.devprmtb[@fields]").trim();
				//20201116 change to given svrid
				prtbrws.clear();
				prttype.clear();
				prtcltip.clear();
				prtcltautoturnpage.clear();
				//----
				if (fromurl.length() <= 0 ||fromuser.length() <= 0 || frompass.length()<= 0
						|| svrprmtb.length() <= 0 || svrprmkey.length() <= 0 || svrprmfields.length() <= 0
						|| fasprmtb.length() <= 0 || fasprmkey.length() <= 0 || fasprmfields.length() <= 0
						|| devprmtb.length() <= 0 || devprmkey.length() <= 0 || devprmfields.length() <= 0 || SvrId.length() <= 0)
					log.info("connect parameters error {} {} {} SvrID=[{}]", fromurl, fromuser, frompass, SvrId);
				else {
					log.info("using {} {} {}", fromurl, fromuser, frompass);
					log.info("svrprmtb {} {} {}", svrprmtb, svrprmkey, svrprmfields);
					log.info("fasprmtb {} {} {}", fasprmtb, fasprmkey, fasprmfields);
					log.info("devprmtb {} {} {}", devprmtb, devprmkey, devprmfields);
					try {
						jsel2ins = new GwDao(fromurl, fromuser, frompass, false);
						//20240523 Poor Style: Value Never Read String origStr = "";
						log.debug("current svrprmtb=[{}] svrprmtbkey=[{}] svrprmtbsearkey=[{}]", svrprmtb, svrprmkey, SvrId);
						String[] svrflds = jsel2ins.SELMFLD(svrprmtb, svrprmfields, svrprmkey, SvrId, false);
						String[] svrfldsary = null; // store values of AUID,BRNO,IP,PORT,DEVTPE,RECVTIMEOUT,LOGPATH
						String fasfld = ""; // store values of CONNPRM
						String[] devfldsary = null; // store values of BRWS,IP,PORT,DEVTPE,AUTOTURNPAGE
						if(svrflds != null && svrflds.length > 0) {
							for (String s: svrflds) {
								s = s.trim();
								log.atDebug().setMessage("current svrfld [{}]").addArgument(s).log();//20240517 change for Log Forging(debug)
								if (s.length() > 0 && s.indexOf(',') > -1) {
									svrfldsary = s.split(",");
									for (int idx = 0; idx < svrfldsary.length; idx++) {
										switch (idx) {
												//20200926
											case 0:
												setAuid(svrfldsary[idx].trim());
												log.debug("SERVICE parameter [{}] set auid [{}]", idx, svrfldsary[idx]);
												break;
												//----
												//20201116 cancel verhbrno
												/*
											case 1:
												origStr = conHashMap.get("svrsubport.verhbrno").trim();
												conHashMap.put("svrsubport.verhbrno", svrfldsary[idx].trim());
												log.debug("SERVICE parameter [{}] set svrsubport.verhbrno [{}]", idx, svrfldsary[idx]);
												break;
											case 2:
												//20200926
												setSvrip(svrfldsary[idx]);
												//----
												log.debug("SERVICE parameter [{}] prepare to set service ip for device [{}]", idx, svrfldsary[idx]);
												break;
											case 3:
												log.debug("SERVICE parameter [{}] prepare to set service port for device [{}]", idx, svrfldsary[idx]);
												break;
											case 4:
												log.debug("SERVICE parameter [{}] [{}]", idx, svrfldsary[idx]);
												break;
											case 5:
												origStr = conHashMap.get("svrsubport.recvtimeout").trim();
												conHashMap.put("svrsubport.recvtimeout", svrfldsary[idx].trim());
												log.debug("SERVICE parameter [{}] svrsubport.recvtimeout [{}]", idx, svrfldsary[idx]);
												break;
											case 6:
												origStr = conHashMap.get("system.logpath").trim();
												conHashMap.put("system.logpath", svrfldsary[idx].trim());
												log.debug("SERVICE parameter [{}] set system.logpath [{}]", idx, svrfldsary[idx]);
												break;
												*/
											case 1:
												//20200926
												setSvrip(svrfldsary[idx]);
												//----
												log.debug("SERVICE parameter [{}] prepare to set service ip for device [{}]", idx, svrfldsary[idx]);
												break;
											case 2:
												log.debug("SERVICE parameter [{}] prepare to set service port for device [{}]", idx, svrfldsary[idx]);
												break;
											case 3:
												log.debug("SERVICE parameter [{}] [{}]", idx, svrfldsary[idx]);
												break;
											case 4:
												//20240520 Poor Style: Value Never Read origStr = conHashMap.get("svrsubport.recvtimeout").trim();
												conHashMap.put("svrsubport.recvtimeout", svrfldsary[idx].trim());
												log.debug("SERVICE parameter [{}] svrsubport.recvtimeout [{}]", idx, svrfldsary[idx]);
												break;
											case 5:
												//20240520 Poor Style: Value Never Read origStr = conHashMap.get("system.logpath").trim();
												conHashMap.put("system.logpath", svrfldsary[idx].trim());
												log.debug("SERVICE parameter [{}] set system.logpath [{}]", idx, svrfldsary[idx]);
												break;
											default:
												break;
										}
									}
								} else
									log.error("!!!!SERVICE parameters in service table [{}] error !!!", svrprmtb);
							}
							if (svrfldsary != null && svrfldsary.length > 5) { //20210413 MatsudairaSyuMe prevent Null Dereference
								log.debug("current fasprmtb=[{}] fasprmtbkey=[{}] fasprmtbsearkey=[{}]", fasprmtb,
										fasprmkey, svrfldsary[0]);
								String[] fasflds = jsel2ins.SELMFLD(fasprmtb, fasprmfields, fasprmkey, svrfldsary[0],
										false);
								if (fasflds != null && fasflds.length > 0)
									for (String s : fasflds) {
										s = s.trim();
										fasfld = s;
										//20240520 Poor Style: Value Never Read origStr = conHashMap.get("svrsubport.svrip").trim();
										conHashMap.put("svrsubport.svrip", fasfld);
										log.atDebug().setMessage("current fasfld set svrsubport.svrip [{}]").addArgument(fasfld).log();//20240517 change for Log Forging(debug)
									}
								log.debug("current devprmtb=[{}] devprmtbkey=[{}] devprmtbsearkey=[{}]", devprmtb,
										devprmkey, SvrId);
								String[] devflds = jsel2ins.SELMFLD(devprmtb, devprmfields, devprmkey, SvrId, false);
								String prtcltipStr = "";
								/* 20201116 change to use given svrid
								prtbrws.clear();
								prttype.clear();
								prtcltip.clear();
								prtcltautoturnpage.clear();
								*/
								if (devflds != null && devflds.length > 0)
									for (String s : devflds) {
										s = s.trim();
										log.atDebug().setMessage("current devflds [{}]").addArgument(s).log();//20240517 change for Log Forging(debug)
										if (s.length() > 0 && s.indexOf(',') > -1) {
											devfldsary = s.split(",");
											for (int idx = 0; idx < devfldsary.length; idx++) {
												switch (idx) {
												case 0:
													prtbrws.add(devfldsary[idx].trim());
													log.debug("DEVICE parameter [{}] set prtbrws [{}]", idx,
															devfldsary[idx].trim());
													break;
												case 1:
													log.debug("DEVICE parameter [{}] set ip for prtcltip [{}]", idx,
															devfldsary[idx].trim());
													break;
												case 2:
													// localhost:4002=localhost:3301
													// 20201116 cancel vhbrno
//												prtcltipStr =  devfldsary[1].trim() + ":"+ devfldsary[idx].trim() + "=" + svrfldsary[2].trim() + ":" + svrfldsary[3].trim();
													prtcltipStr = devfldsary[1].trim() + ":" + devfldsary[idx].trim()
															+ "=" + svrfldsary[1].trim() + ":" + svrfldsary[2].trim();
													// ----
													prtcltip.add(prtcltipStr);
													log.debug("DEVICE parameter [{}] set port [{}] for prtcltip [{}]",
															idx, devfldsary[idx], prtcltipStr);
													break;
												case 3:
													if (devfldsary[idx].trim().equalsIgnoreCase("2")) {
														prttype.add("AUTO46");
														log.debug("DEVICE parameter [{}] set prttype [{}]", idx,
																"AUTO46");
													} else if (devfldsary[idx].trim().equalsIgnoreCase("3")) {
														prttype.add("AUTO52"); // error set to AUTO46
														log.debug("DEVICE parameter [{}] set prttype [{}]", idx,
																"AUTO52");
													} else {
														log.debug("DEVICE parameter [{}] not autoprinter type", idx);
														prttype.add("NOTAUTOPRT"); // //20201110 check for autoprt
																					// deivce
													}
													break;
												case 4:
													if (devfldsary[idx].trim().equalsIgnoreCase("N")) {
														prtcltautoturnpage.add("false");
														log.debug("DEVICE parameter [{}] set prtcltautoturnpage [{}]",
																idx, "false");
													} else {
														prtcltautoturnpage.add("true");
														log.debug("DEVICE parameter [{}] set prtcltautoturnpage [{}]",
																idx, "true");
													}
													break;
												default:
													break;
												}
											}

										} else
											log.error("!!!!DEVICE parameters in [{}] error !!!", devprmtb);
									}
							} else
								log.error("!!!!field parameters in fas parameter table [{}] error !!!", fasprmtb);
						} else { //20201116 add check given svrid
							log.error("!!!!svrid  not exist in table [{}] error !!!", SvrId);							
						}
						//----
						jsel2ins.CloseConnect();
						jsel2ins = null;
					} catch (Exception e) {
						//20240503 MatsudairaSyuMe mark for System Information Leak e.printStackTrace();
						log.info("monitorThread read database error");//20240503 change log message
					}

				}
			}
			//----
			if (prtbrws != null && prtbrws.size() > 0) {
				for (int i = 0; i < prtbrws.size(); i++) {
					ConcurrentHashMap<String, Object> cfgPrtMap = new ConcurrentHashMap<String, Object>();
					if (!prttype.get(i).trim().equalsIgnoreCase("NOTAUTOPRT")) { //20201110 check for autoprt deivce
						cfgPrtMap.put("brws", prtbrws.get(i));
						cfgPrtMap.put("type", prttype.get(i));
						cfgPrtMap.put("ip", prtcltip.get(i));
						cfgPrtMap.put("autoturnpage", prtcltautoturnpage.get(i));
						cfgPrtMapList.add(cfgPrtMap);
						log.info("cfgPrtMapList add idx={}", i);
					}
				}
			}
		}
		/**/
	}

	/*20240503 MatsudairaSyuuMe mark up for Code Correctness: Constructor Invokes Overridable Function public void ChkCfg() throws ConfigurationException {
	}  */

	public ConcurrentHashMap<String, String> getConHashMap() {
		return conHashMap;
	}

	public CopyOnWriteArrayList<ConcurrentHashMap<String, Object>> getCfgPrtMapList() {
		return cfgPrtMapList;
	}

	/**
	 * 2020091s
	 * @return the readCfgFromCenter
	 */
	public boolean isReadCfgFromCenter() {
		return readCfgFromCenter;
	}
	
	public static void main(String[] args) throws Exception {
		DynamicProps dcf = new DynamicProps("rateprtservice.xml");
		dcf.Chat();
	}

	/**
	 * 20200926
	 * @return the auid
	 */
	public String getAuid() {
		return auid;
	}

	/**
	 * 20200926
	 * @param auid the auid to set
	 */
	public void setAuid(String auid) {
		this.auid = auid;
	}

	/**
	 * 20200926
	 * @return the svrip
	 */
	public String getSvrip() {
		return svrip;
	}

	/**
	 * 20200926
	 * @param svrip the svrip to set
	 */
	public void setSvrip(String svrip) {
		this.svrip = svrip;
	}

	/**
	 * 20201006
	 * 
	 */
	public CopyOnWriteArrayList<ConcurrentHashMap<String, Object>> getLastcfgPrtMapList() {
		CopyOnWriteArrayList<ConcurrentHashMap<String, Object>> lastcfgPrtMapList = new CopyOnWriteArrayList<ConcurrentHashMap<String, Object>>();
		if (readCfgFromCenter) {
			String SvrId = conHashMap.get("system.svrid").trim();
			log.info("start to load new data from repository current SVRID=[{}]", SvrId);
			String fromurl = conHashMap.get("system.db[@url]").trim();
			String fromuser = conHashMap.get("system.db[@user]").trim();
			String frompass = conHashMap.get("system.db[@pass]").trim();
			String svrprmtb = conHashMap.get("system.svrprmtb[@name]").trim();
			String svrprmkey = conHashMap.get("system.svrprmtb[@mkey]").trim();
			String svrprmfields = conHashMap.get("system.svrprmtb[@fields]").trim();
			String fasprmtb = conHashMap.get("system.fasprmtb[@name]").trim();
			String fasprmkey = conHashMap.get("system.fasprmtb[@mkey]").trim();
			String fasprmfields = conHashMap.get("system.fasprmtb[@fields]").trim();
			String devprmtb = conHashMap.get("system.devprmtb[@name]").trim();
			String devprmkey = conHashMap.get("system.devprmtb[@mkey]").trim();
			String devprmfields = conHashMap.get("system.devprmtb[@fields]").trim();
			if (fromurl.length() <= 0 || fromuser.length() <= 0 || frompass.length() <= 0 || svrprmtb.length() <= 0
					|| svrprmkey.length() <= 0 || svrprmfields.length() <= 0 || fasprmtb.length() <= 0
					|| fasprmkey.length() <= 0 || fasprmfields.length() <= 0 || devprmtb.length() <= 0
					|| devprmkey.length() <= 0 || devprmfields.length() <= 0 || SvrId.length() <= 0)
				log.info("connect parameters error {} {} {} SvrID=[{}]", fromurl, fromuser, frompass, SvrId);
			else {
				log.info("using {} {} {}", fromurl, fromuser, frompass);
				log.info("svrprmtb {} {} {}", svrprmtb, svrprmkey, svrprmfields);
				log.info("fasprmtb {} {} {}", fasprmtb, fasprmkey, fasprmfields);
				log.info("devprmtb {} {} {}", devprmtb, devprmkey, devprmfields);
				try {
					jsel2ins = new GwDao(fromurl, fromuser, frompass, false);
					log.debug("current svrprmtb=[{}] svrprmtbkey=[{}] svrprmtbsearkey=[{}]", svrprmtb, svrprmkey,
							SvrId);
					String[] svrflds = jsel2ins.SELMFLD(svrprmtb, svrprmfields, svrprmkey, SvrId, false);
					String[] svrfldsary = null; // store values of AUID,BRNO,IP,PORT,DEVTPE,RECVTIMEOUT,LOGPATH
					String fasfld = ""; // store values of CONNPRM
					String[] devfldsary = null; // store values of BRWS,IP,PORT,DEVTPE,AUTOTURNPAGE
					if (svrflds != null && svrflds.length > 0)
						for (String s : svrflds) {
							s = s.trim();
							log.atDebug().setMessage("current svrfld [{}]").addArgument(s).log();//20240517 change for Log Forging(debug)
							if (s.length() > 0 && s.indexOf(',') > -1) {
								svrfldsary = s.split(",");
								for (int idx = 0; idx < svrfldsary.length; idx++) {
									switch (idx) {
									// 20200926
									case 0:
										setAuid(svrfldsary[idx].trim());
										log.debug("SERVICE parameter [{}] set auid [{}]", idx, svrfldsary[idx]);
										break;
									// ----
										//20201116 canceel verhbrno
										/*
									case 1:
										conHashMap.put("svrsubport.verhbrno", svrfldsary[idx].trim());
										log.debug("SERVICE parameter [{}] set svrsubport.verhbrno [{}]", idx,
												svrfldsary[idx]);
										break;
									case 2:
										// 20200926
										setSvrip(svrfldsary[idx]);
										// ----
										log.debug("SERVICE parameter [{}] prepare to set service ip for device [{}]",
												idx, svrfldsary[idx]);
										break;
									case 3:
										log.debug("SERVICE parameter [{}] prepare to set service port for device [{}]",
												idx, svrfldsary[idx]);
										break;
									case 4:
										log.debug("SERVICE parameter [{}] [{}]", idx, svrfldsary[idx]);
										break;
									case 5:
										conHashMap.put("svrsubport.recvtimeout", svrfldsary[idx].trim());
										log.debug("SERVICE parameter [{}] svrsubport.recvtimeout [{}]", idx,
												svrfldsary[idx]);
										break;
									case 6:
										conHashMap.put("system.logpath", svrfldsary[idx].trim());
										log.debug("SERVICE parameter [{}] set system.logpath [{}]", idx,
												svrfldsary[idx]);
										break;
										*/
									case 1:
										// 20200926
										setSvrip(svrfldsary[idx]);
										// ----
										log.debug("SERVICE parameter [{}] prepare to set service ip for device [{}]",
												idx, svrfldsary[idx]);
										break;
									case 2:
										log.debug("SERVICE parameter [{}] prepare to set service port for device [{}]",
												idx, svrfldsary[idx]);
										break;
									case 3:
										log.debug("SERVICE parameter [{}] [{}]", idx, svrfldsary[idx]);
										break;
									case 4:
										conHashMap.put("svrsubport.recvtimeout", svrfldsary[idx].trim());
										log.debug("SERVICE parameter [{}] svrsubport.recvtimeout [{}]", idx,
												svrfldsary[idx]);
										break;
									case 5:
										conHashMap.put("system.logpath", svrfldsary[idx].trim());
										log.debug("SERVICE parameter [{}] set system.logpath [{}]", idx,
												svrfldsary[idx]);
										break;
									default:
										break;
									}
								}
							} else
								log.error("!!!!SERVICE parameters in service table [{}] error !!!", svrprmtb);
						}
					if (svrfldsary != null && svrfldsary.length > 5) { //20210413 MatsudairaSyuMe prevent Null Dereference
						log.debug("current fasprmtb=[{}] fasprmtbkey=[{}] fasprmtbsearkey=[{}]", fasprmtb, fasprmkey,
								svrfldsary[0]);
						String[] fasflds = jsel2ins.SELMFLD(fasprmtb, fasprmfields, fasprmkey, svrfldsary[0], false);
						if (fasflds != null && fasflds.length > 0)
							for (String s : fasflds) {
								s = s.trim();
								fasfld = s;
								conHashMap.put("svrsubport.svrip", fasfld);
								log.atDebug().setMessage("current fasfld set svrsubport.svrip [{}]").addArgument(fasfld).log();//20240517 change for Log Forging(debug)
							}
						log.debug("current devprmtb=[{}] devprmtbkey=[{}] devprmtbsearkey=[{}]", devprmtb, devprmkey,
								SvrId);
						String[] devflds = jsel2ins.SELMFLD(devprmtb, devprmfields, devprmkey, SvrId, false);
						String prtcltipStr = "";
						prtbrws.clear();
						prttype.clear();
						prtcltip.clear();
						prtcltautoturnpage.clear();
						if (devflds != null && devflds.length > 0)
							for (String s : devflds) {
								s = s.trim();
								log.atDebug().setMessage("current devflds [{}]").addArgument(s).log();//20240517 change for Log Forging(debug)
								if (s.length() > 0 && s.indexOf(',') > -1) {
									devfldsary = s.split(",");
									for (int idx = 0; idx < devfldsary.length; idx++) {
										switch (idx) {
										case 0:
											prtbrws.add(devfldsary[idx].trim());
											log.debug("DEVICE parameter [{}] set prtbrws [{}]", idx,
													devfldsary[idx].trim());
											break;
										case 1:
											log.debug("DEVICE parameter [{}] set ip for prtcltip [{}]", idx,
													devfldsary[idx].trim());
											break;
										case 2:
											// localhost:4002=localhost:3301
											//20201116
//											prtcltipStr = devfldsary[1].trim() + ":" + devfldsary[idx].trim() + "="
//													+ svrfldsary[2].trim() + ":" + svrfldsary[3].trim();
											prtcltipStr = devfldsary[1].trim() + ":" + devfldsary[idx].trim() + "="
													+ svrfldsary[1].trim() + ":" + svrfldsary[2].trim();
											//----
											prtcltip.add(prtcltipStr);
											log.debug("DEVICE parameter [{}] set port [{}] for prtcltip [{}]", idx,
													devfldsary[idx], prtcltipStr);
											break;
										case 3:
											if (devfldsary[idx].trim().equalsIgnoreCase("2")) {
												prttype.add("AUTO46");
												log.debug("DEVICE parameter [{}] set prttype [{}]", idx, "AUTO46");
											} else if (devfldsary[idx].trim().equalsIgnoreCase("3")) {
												prttype.add("AUTO52"); // error set to AUTO46
												log.debug("DEVICE parameter [{}] set prttype [{}]", idx, "AUTO52");
											} else {													
												log.debug("DEVICE parameter [{}] not autoprinter type", idx);
												prttype.add("NOTAUTOPRT"); // //20201110 check for autoprt deivce
											}
											break;
										case 4:
											if (devfldsary[idx].trim().equalsIgnoreCase("N")) {
												prtcltautoturnpage.add("false");
												log.debug("DEVICE parameter [{}] set prtcltautoturnpage [{}]", idx,
														"false");
											} else {
												prtcltautoturnpage.add("true");
												log.debug("DEVICE parameter [{}] set prtcltautoturnpage [{}]", idx,
														"true");
											}
											break;
										default:
											break;
										}
									}

								} else
									log.error("!!!!DEVICE parameters in [{}] error !!!", devprmtb);
							}
					} else
						log.error("!!!!field parameters in fas parameter table [{}] error !!!", fasprmtb);
					jsel2ins.CloseConnect();
					jsel2ins = null;
				} catch (Exception e) {
					//20240503 MatsudairaSyuMe mark for System Information Leak e.printStackTrace();
					log.info("read database error");//20240503 change log message
				}
			}
		}
		// ----
		if (prtbrws != null && prtbrws.size() > 0) {
			for (int i = 0; i < prtbrws.size(); i++) {
				if (!prttype.get(i).trim().equalsIgnoreCase("NOTAUTOPRT")) { //20201110 check for autoprt deivce
					ConcurrentHashMap<String, Object> cfgPrtMap = new ConcurrentHashMap<String, Object>();
					cfgPrtMap.put("brws", prtbrws.get(i));
					cfgPrtMap.put("type", prttype.get(i));
					cfgPrtMap.put("ip", prtcltip.get(i));
					cfgPrtMap.put("autoturnpage", prtcltautoturnpage.get(i));
					lastcfgPrtMapList.add(cfgPrtMap);
					log.info("lastcfgPrtMapList add idx={}", i);
				}
			}
		}
		return lastcfgPrtMapList;
	}

	/**20240508
	 * @return the encryptedPd
	 */
	public static String getDecryptedPd() {
		return decryptedPd;
	}
}
