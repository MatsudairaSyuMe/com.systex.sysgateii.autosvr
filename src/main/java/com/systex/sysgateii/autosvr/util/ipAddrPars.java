package com.systex.sysgateii.autosvr.util;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.systex.sysgateii.autosvr.comm.Constants;

public class ipAddrPars {
	private String remoteHostAddr = "";
	private String localHostAddr = "*";
	private boolean checkAddrPeer = false;
	private int remotePort = 0;
	private List<Integer> localPortList = new ArrayList<Integer>();
	private boolean formatError = false;
	private InetSocketAddress remotenodeAddr = null;
	private Map<Integer, InetSocketAddress> localnodeAddrMap = new HashMap<>();
	private static Logger log = LoggerFactory.getLogger(ipAddrPars.class);

	public ipAddrPars() {

	}

	public void init() {
		this.remoteHostAddr = "";
		this.localHostAddr = "*";
		this.checkAddrPeer = false;
		this.remotePort = 0;
		this.localPortList.clear();
		this.formatError = false;
		this.remotenodeAddr = null;
		this.localnodeAddrMap.clear();
	}

	public void list() {
		Iterator<Integer> iterator = null;
		if (!this.formatError) {
			if (this.checkAddrPeer) {
				if ((this.remoteHostAddr.length() != 0 && this.remotePort != 0)
						|| (this.localHostAddr.length() != 0 && this.localPortList.size() != 0)
						|| ((this.remoteHostAddr.length() == 0 || this.remotePort == 0)
								|| (this.localHostAddr.length() == 0 || this.localPortList.size() == 0))) {
					if (this.localPortList.size() <= 1) {
						if (this.remoteHostAddr.length() > 0 && this.localHostAddr.length() == 0)
							this.localHostAddr = this.remoteHostAddr;
						if (this.localHostAddr.length() > 0 && this.remoteHostAddr.length() == 0)
							this.remoteHostAddr = this.localHostAddr;
						if (this.remotePort <= 0) {
							iterator = this.localPortList.iterator();
							if (iterator.hasNext()) {
								int p = (int) iterator.next();
								if (p > 0)
									this.remotePort = p;
								else
									this.formatError = false;
							}
						}
						if (this.localPortList.size() == 0)
							localPortList.add(this.remotePort);
					} else
						this.formatError = false;
				} else
					this.formatError = false;
			} else {
				if (this.remoteHostAddr.length() == 0 || this.remotePort == 0)
					this.formatError = true;
			}
		}
		log.atDebug().setMessage("result ====> remoteAddr: {}  remotePort:{}  localAddr:{}  localPort:").addArgument(this.remoteHostAddr).addArgument(
				this.remotePort).addArgument(this.localHostAddr).log();//20240517 change for Log Forging(debug)
		if (!this.formatError) {
			this.remotenodeAddr = new InetSocketAddress(this.remoteHostAddr, this.remotePort);
			iterator = this.localPortList.iterator();
			while (iterator.hasNext()) {
				int p = (int) iterator.next();
				this.localnodeAddrMap.put(p, new InetSocketAddress(this.localHostAddr, p));
				log.debug(" {}", p);
			}
		}
		log.debug(" checkAddrPeer:{} format:{}", this.checkAddrPeer, (this.formatError ? "ERROR" : "CORRECT"));
	}

	private void calcExpressionReange(final String localPortExpression) {

		String[] portArr = localPortExpression.split(",");
		for (final String port : portArr) {
			String tempPort = port.trim();
			int idx = tempPort.indexOf("-");
			try {
				if (idx == -1) {
					// System.out.println("1===>add localPort:" + tempPort);
					localPortList.add(Integer.parseInt(tempPort));
					continue;
				}
				String[] portReange = tempPort.split("-", 2);
				int startPort = Integer.parseInt(portReange[0].trim());
				int endPort = Integer.parseInt(portReange[1].trim());
				for (int j = startPort; j <= endPort; j++) {
					// System.out.println("2===>add localPort:" + j);
					localPortList.add(j);
				}
			} catch (NumberFormatException e) {
				log.debug("NumberFormatException format error");
			}
		}
	}

	public boolean CheckAddrT(final String s, final String split, final boolean chklocalip) throws Exception {
		boolean formatCorrect = false;
		String ptn = "addrParts";
		if (s == null || s.length() == 0 || split == null || split.length() == 0) {
			log.error("format error");
			this.formatError = true;
			return formatCorrect;
		}
		if (s.contains(split)) {
			if (split.contains("=") && !this.checkAddrPeer && this.localHostAddr.contains("*")) { // 1st time check full
																									// text
				ptn = "addrPeerParts"; // peer to peer address text pattern
				this.localHostAddr = "";
				this.checkAddrPeer = true;
			}
			final String sParts[] = s.split(split);
			if (sParts.length > 1) {
				// log.debug("{} 0, 1 {} {} -->{}", ptn, sParts[0], sParts[1], chklocalip);
				if (this.checkAddrPeer && split.contains("=")) {
					// 1st time check 1st part pattern
					if (CheckAddrT(sParts[0], ":", false))
						CheckAddrT(sParts[1], ":", true);
				} else {
					if (chklocalip) {
						if (sParts[0].trim().length() > 0)
							this.localHostAddr = sParts[0];
						if (sParts[1].trim().length() > 0)
							calcExpressionReange(sParts[1].trim());
					}
				}
//          log.debug("1ready {}=========>{}",split, chklocalip);
				if (split.contains(":") && !chklocalip) {
					remoteHostAddr = sParts[0];
					try {
						remotePort = Integer.parseInt(sParts[1]);
					} catch (NumberFormatException e) {
						// 20210714 MatsudairaSyuMe Log Forging
						//final String chksParts = StrUtil.convertValidLog(sParts[1]);
						log.error("port format error!!");//, chksParts
						this.formatError = true;
						return formatCorrect;
					}
//          log.debug("1===>add remoteHostAddr: {} remotePort:{}", remoteHostAddr, sParts[1]);
				}
			} else if (sParts.length == 0) {
				log.error("{} format error !!! {}", ptn, s);
				this.formatError = true;
			} else {
//          log.debug("{} 0 {} -->{}", ptn, sParts[0], chklocalip);
				if (this.checkAddrPeer) {
					CheckAddrT(sParts[0], ":", false);
				}
//          log.debug("2ready {} =========>{}+++{}",split, chklocalip, sParts.length);
				if (!chklocalip) {
					if (split.contains(":")) {
						remoteHostAddr = sParts[0];
//                log.debug("2===>add remoteHostAddr: {}", remoteHostAddr);
					} else {
						// 20210714 MatsudairaSyuMe Log Forging
						//final String chksParts = StrUtil.convertValidLog(sParts[1]);
						log.error("port format error!! ");//, chksParts
						this.formatError = true;
						return formatCorrect;
					}
				}
			}
		} else {
			if (split.contains("=") && !this.checkAddrPeer && this.localHostAddr.contains("*")) { // 1st time check full
																									// text
				// non peer to peer address text pattern
//          log.debug("{} stand alone check !0 {} -->{}", ptn, s, chklocalip);
				return CheckAddrT(s, ":", false);
			}
//       log.debug("{} split<>{} !0 {}-->{} localHostAddr={}", ptn, split, s, chklocalip, this.localHostAddr);
			if (s.trim().length() > 0) {
				if (chklocalip)
					calcExpressionReange(s.trim());
				else {
					if (this.remoteHostAddr.length() > 0) {
						try {
							remotePort = Integer.parseInt(s);
//                   log.debug("remotePort ={}",s);
						} catch (NumberFormatException e) {
							// 20210714 MatsudairaSyuMe Log Forging
							//final String chks = StrUtil.convertValidLog(s);
							log.error("remotePort format error!!");//, chks
							this.formatError = true;
							return formatCorrect;
						}
					} else {
//                   log.debug("3ready {}  =========>{}", split, chklocalip);
						try {
							remotePort = Integer.parseInt(s);
//                   log.debug("3===>remotePort ={}", s);
						} catch (NumberFormatException e) {
							remoteHostAddr = s;
//                   log.debug("3===>add remoteHostAddr: {}",remoteHostAddr);
						}
					}
				}
			}
		}

		formatCorrect = true;
		return formatCorrect;
	}

	public boolean getCurrentParseResult() {
		// formatError: true address format error
		// :false address format OK
		return !this.formatError;
	}

	public boolean getCurrentNodeType() {
		// checkAddrPeer true: Peer to Peer connect type
		// false: useful connect type
		return this.checkAddrPeer;
	}

	public InetSocketAddress getCurrentRemoteNodeAddress() {
		return this.remotenodeAddr;
	}

	public String getCurrentLocalHostAddress() {
		return this.localHostAddr;
	}

	//20210427 MatsudairaSyuMe
	public String getCurrentRemoteHostAddress() {
		return this.remoteHostAddr;
	}
	//----

	public Map<Integer, InetSocketAddress> getCurrentLocalNodeAddressMap() {
		if (!this.checkAddrPeer)
			this.localnodeAddrMap.clear();
		return this.localnodeAddrMap;
	}
	public List<Integer> getCurrentLocalPortList() {
		if (!this.checkAddrPeer)
			this.localnodeAddrMap.clear();
		return this.localPortList;
	}

}
