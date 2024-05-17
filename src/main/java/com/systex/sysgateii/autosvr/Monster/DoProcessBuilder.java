package com.systex.sysgateii.autosvr.Monster;

/******************
 * MatsudairaSyume
 * 20201119
 * Running the program
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.systex.sysgateii.autosvr.util.StrUtil;

public class DoProcessBuilder {

	private static Logger log = LoggerFactory.getLogger(DoProcessBuilder.class);
	private String[] runArgs = null;
	//20210217 MatsudairaSyuMe
	private static final String TrustedCmd = "bin"+ java.io.File.separator + "autosvr";
	//----
	private static final String[] TrustedArg1 = {"start", "stop", "restart"};
	private static final String TrustedArg2 = "--svrid";
	private static final String[] TrustedPTRN = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};

	//20210202 MatsudairaSyuMe
/*
	DoProcessBuilder(String args[]) {
		runArgs = args;
	}
	*/
	//20210302 MatsudairaSyuMe
/*	DoProcessBuilder(String inArgs0, String inArgs1, String inArgs2, String inArgs3) {
		if (inArgs0 == null || inArgs0.trim().length() == 0 || inArgs1 == null || inArgs1.trim().length() == 0
				|| inArgs2 == null || inArgs2.trim().length() == 0 || inArgs3 == null || inArgs3.trim().length() == 0) {
			log.error("initial command error");
		} else {
			boolean chkOk = false;
			int arg1idx = -1;
			for (arg1idx = 0; arg1idx < TrustedArg1.length; arg1idx++) {
				log.debug("inArgs1 running is {} chkOk={} chkS={}", inArgs1, chkOk, TrustedArg1[arg1idx].trim());
				if (inArgs1.equals(TrustedArg1[arg1idx].trim())) {
					chkOk = true;
					break;
				}
			}
			if (chkOk == true) {
				chkOk = false;
				if (StrUtil.isNumeric(inArgs3.trim()))
					chkOk = true;
			}
			log.debug("inArgs0={} {} inArgs2={} {} chkOk={}", inArgs0, TrustedCmd, inArgs2, TrustedArg2, chkOk);
			if (chkOk && inArgs0.trim().equals(TrustedCmd) && inArgs2.trim().equals(TrustedArg2)) {
				runArgs = new String[4];
				runArgs[0] = TrustedCmd;
				runArgs[1] = TrustedArg1[arg1idx];
				runArgs[2] = TrustedArg2;
				runArgs[3] = inArgs3;
			} else {
				runArgs = null;
				log.error("initial command format error");
			}
		}
	}*/
	//----
	DoProcessBuilder() {
		runArgs = null;
	}
	//----
	public void Go(String runArgs0, String runArgs1, String runArgs2, String runArgs3) {
		if (runArgs0 == null || runArgs0.trim().length() == 0
				|| runArgs1 == null || runArgs1.trim().length() == 0
				|| runArgs2 == null || runArgs2.trim().length() == 0
				|| runArgs3 == null || runArgs3.trim().length() == 0
			) {
			log.info("initial command error");
			System.exit(-1);			
		}
		try {
			//20210226 for vulnerability scanning command injection defense
			boolean chkOk = false;
			int arg1idx = -1;
			for (arg1idx = 0; arg1idx <TrustedArg1.length; arg1idx++) {
				log.debug("runArgs1 running is {} chkOk={} chkS={}", runArgs1, chkOk, TrustedArg1[arg1idx].trim());
				if (runArgs1.equals(TrustedArg1[arg1idx].trim())) {
					chkOk = true;
					break;
				}
			}
			if (chkOk == true) {
				chkOk = false;
				if (StrUtil.isNumeric(runArgs3.trim()))
					chkOk = true;
			}
			log.debug("runArgs0={} {} runArgs2={} {} chkOk={}", runArgs0, TrustedCmd,
					runArgs2, TrustedArg2,chkOk);
			// 20210320 MatsudairaSyuMe for command injection
			runArgs3 = runArgs3.trim();
			Pattern FILTER_PATTERN = Pattern.compile("[0-9]+");
			if (!FILTER_PATTERN.matcher(runArgs3).matches()) {
				log.debug("input:{} not match", runArgs3);
				chkOk = false;
			} else
				log.debug("inpit:{} ok", runArgs3);
			String[] origsary = runArgs3.split("");
			String cnvs = "";
			for (String s : origsary) {
				for (int i = 0; i < TrustedPTRN.length; i++) {
					if (TrustedPTRN[i].equals(s)) {
						cnvs = cnvs + TrustedPTRN[i];
						break;
					}
				}
			}
			if (chkOk && cnvs.length() == 0)
				chkOk = false;
			if (chkOk && runArgs0.trim().equals(TrustedCmd)
					&& runArgs2.trim().equals(TrustedArg2)) {
				ProcessBuilder pb = new ProcessBuilder(TrustedCmd, TrustedArg1[arg1idx], TrustedArg2,  cnvs); //20240503 command injection
				//----TrustedArg1[arg1idx]
//				String currentDir = System.getProperty("user.dir");  20210426 MatsudairaSyuMe mark for path manipulation
				pb.directory(new File("." + File.separator)); //20210426 MatsudairaSyuMe use current directory
				Process process = pb.start();
				InputStream is = process.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line;

				log.debug("Output of running {} is:", Arrays.toString(runArgs));
				while ((line = br.readLine()) != null) {
					log.debug(line);
				}
			} else {
				//20210204 MatsudairaSyuMe
				final String logStr = String.format("!!!! Command %s ERROR !!!!", Arrays.toString(runArgs));
				log.error(logStr);
			}
		} catch (Exception e) {
			//20240503 MatsudairaSyuMe mark up for System Information Leak e.printStackTrace();
			log.error("fork process error"); //20240503 change log
		}
	}
/*	public void Go() {
		if (runArgs == null || runArgs.length < 4) {
			log.error("Need command to run");
		} else {
			try {
				ProcessBuilder pb = new ProcessBuilder(runArgs);
				String currentDir = System.getProperty("user.dir");
				pb.directory(new File(currentDir));
				Process process = pb.start();
				InputStream is = process.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line;

				log.debug("Output of running {} is:", Arrays.toString(runArgs));
				while ((line = br.readLine()) != null) {
					log.debug(line);
				}
			} catch (Exception e) {
				e.printStackTrace();
				log.error("fork process error [{}]", e.toString());
			}
		}
	}*/
	/*
	public void Go() {
		if (runArgs.length <= 0) {
			log.info("Need command to run");
			System.exit(-1);
		}
		try {
			//20210111 for vulnerability scanning command injection defense
			boolean chkOk = false;
			if (runArgs.length >= 4) {
				if (runArgs[1] != null && runArgs[1].trim().length() != 0) {
					for (String chkS : TrustedArg1) {
						log.debug("Output of runArgs[1]running is :{} chkOk={} chkS={}", runArgs[1], chkOk, chkS);
						if (chkS.equals(runArgs[1].trim())) {
							chkOk = true;
							break;
						}
					}
				}
				if (chkOk == true) {
					chkOk = false;
					if (runArgs[3] != null && runArgs[3].trim().length() != 0 && StrUtil.isNumeric(runArgs[3].trim()))
						chkOk = true;
				}
			}
			log.debug("Output of runArgs[0]={} {} runArgs[2]={} {} chkOk={}", runArgs[0], TrustedCmd,
					runArgs[2], TrustedArg2,chkOk);
			if (runArgs.length >= 4 && chkOk && runArgs[0].trim().equals(TrustedCmd)
					&& runArgs[2].trim().equals(TrustedArg2)) {
//				Process process = new ProcessBuilder(runArgs).start();
				ProcessBuilder pb = new ProcessBuilder(runArgs);
				//20210202 MatsudairaSyuMe
//				Map<String, String> env = pb.environment();
				String currentDir = System.getProperty("user.dir");
				pb.directory(new File(currentDir));
				Process process = pb.start();
				//Process process = new ProcessBuilder(runArgs).start();
				InputStream is = process.getInputStream();
				InputStreamReader isr = new InputStreamReader(is);
				BufferedReader br = new BufferedReader(isr);
				String line;

				log.debug("Output of running {} is:", Arrays.toString(runArgs));
				while ((line = br.readLine()) != null) {
					log.debug(line);
				}
			} else {
				log.error("!!!! Command {} ERROR !!!!", Arrays.toString(runArgs));
			}
		} catch (Exception e) {
			e.printStackTrace();
			log.error("fork process error [{}]", e.toString());
		}
	}*/

/*	public static void main(String args[]) throws IOException {
		// 20210202 MatsuDairasyume
		DoProcessBuilder dp = null;
		boolean chkOk = false;
		if (args.length >= 4) {
			if (args[1] != null && args[1].trim().length() != 0) {
				for (String chkS : TrustedArg1) {
					log.debug("runArgs[1] is :{} chkOk={} chkS={}", args[1], chkOk, chkS);
					if (chkS.equals(args[1].trim())) {
						chkOk = true;
						break;
					}
				}
			}
			if (chkOk == true) {
				chkOk = false;
				if (args[3] != null && args[3].trim().length() != 0 && StrUtil.isNumeric(args[3].trim()))
					chkOk = true;
			}
			log.debug("runArgs[0]={} {} runArgs[2]={} {} chkOk={}", args[0], TrustedCmd, args[2], TrustedArg2,
					chkOk);
			if (chkOk && args[0].trim().equals(TrustedCmd) && args[2].trim().equals(TrustedArg2)) {
				dp = new DoProcessBuilder();
				dp.Go(args[0], args[1], args[2], args[3]);
			} else
				log.debug("!!!ERROR chkOk=[{}] runArgs[0]=[{}] runArgs[2]=[{}]", chkOk, args[0], args[2]);
		} else {
			log.error("parameter error:");
			for (int i = 0; i < args.length; i++)
				log.error("i=arg[{}]", i, args[i]);
		}
		// ----
	}*/
}
