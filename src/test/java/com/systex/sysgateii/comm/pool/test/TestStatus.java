package com.systex.sysgateii.comm.pool.test;

import com.systex.sysgateii.comm.pool.test.util.PortTools;

public class TestStatus {
	public static void main(String[] args) throws Exception {
		final int actualConnCount = PortTools.getConnectionCount("0.0.0.0:" + 8080);
		System.out.println("actualConnCount=" + actualConnCount);
	}
}
