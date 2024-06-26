package com.systex.sysgateii.comm.pool;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.pool.ChannelPoolHandler;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
//20210217 MatsidairaSyume
import java.security.SecureRandom;
//----
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.systex.sysgateii.autosvr.util.ipAddrPars;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by andrey on 23.01.17. The simple multi-endpoint connection pool
 * which is throttled externally by providing the semaphore. The provided
 * semaphore limits the count of the simultaneously used connections. Based on
 * netty.
 * Modified by MatsudairaSyuMe on 2019/11/4
 * Modifyed by MatsudairaSynMe on 2021/1/12 change for peer-to-peer connection
 */
public class MultiNodeConnPoolImpl implements NonBlockingConnPool {
	private static Logger LOG = LoggerFactory.getLogger(MultiNodeConnPoolImpl.class);
	private final String nodes[];
	private final int n;
	private final int connAttemptsLimit;
	private final long connectTimeOut;
	private final TimeUnit connectTimeUnit;
	private final Map<String, Bootstrap> bootstraps;
	private final Map<String, Queue<Channel>> allConns;
	private final Map<String, Queue<Channel>> availableConns;
	private final Map<String, AtomicInteger> connCounts;
	private final Map<String, AtomicInteger> failedConnAttemptCounts;
	private final Lock closeLock = new ReentrantLock();
	private Timer timer_;
	private final long reconnectInterval;

	/**
	 * @param nodes             the array of the end point nodes, any element may
	 *                          contain the port (followed after ":") to override
	 *                          the defaultPort argument
	 * @param bootstrap         Netty's bootstrap instance
	 * @param connPoolHandler   channel pool handler instance being notified upon
	 *                          new connection is created
	 * @param defaultPort       default port used to connect (any node address from
	 *                          the nodes set may override this)
	 * @param connAttemptsLimit the max count of the subsequent connection failures
	 *                          to the node before the node will be excluded from
	 *                          the pool, 0 means no limit
	 * @param econnectInterval  the retry interval after connection drop failures,
	 *                          time unit milliseconds, <= 0 for not re-connect
	 */
	public MultiNodeConnPoolImpl(final String nodes[], final Bootstrap bootstrap,
			final ChannelPoolHandler connPoolHandler, final int defaultPort, final int connAttemptsLimit,
			final long connectTimeOut, final TimeUnit connectTimeUnit, final long reconnectInterval) {
		if (nodes.length == 0) {
			throw new IllegalArgumentException("Empty nodes array argument");
		}
		this.nodes = nodes;
		this.connAttemptsLimit = connAttemptsLimit;
		this.connectTimeOut = connectTimeOut;
		this.connectTimeUnit = connectTimeUnit;
		this.n = nodes.length;
		//20240529 Dead Code: Expression is Always true for "autosvr" if (reconnectInterval > 0) {
			this.timer_ = new Timer();
			this.reconnectInterval = reconnectInterval;
		/*} else {
			this.timer_ = null;
			this.reconnectInterval = -1;
		}*/
		bootstraps = new HashMap<>(n);
		allConns = new ConcurrentHashMap<>(n);
		availableConns = new ConcurrentHashMap<>(n);
		connCounts = new ConcurrentHashMap<>(n);
		failedConnAttemptCounts = new ConcurrentHashMap<>(n);
		ipAddrPars nodePars = new ipAddrPars();
		bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
		bootstrap.option(ChannelOption.SO_REUSEADDR, true);

		for (final String node : nodes) {
			InetSocketAddress nodeAddr = null;
			InetSocketAddress localnodeAddr = null;
			//20210204 MatsudairaSyuMe
			final String logStr = String.format("node-->[%s]", node);
			LOG.atDebug().setMessage(logStr).log();//20240517 change for Log Forging(debug)
			nodePars.init();
			try {
				nodePars.CheckAddrT(node, "=", false);
				nodePars.list();
				if (nodePars.getCurrentParseResult()) {
					nodeAddr = nodePars.getCurrentRemoteNodeAddress();
					if (nodePars.getCurrentNodeType()) {
						Iterator<InetSocketAddress> iterator = new ArrayList<InetSocketAddress>(
								nodePars.getCurrentLocalNodeAddressMap().values()).iterator();
						if (iterator.hasNext()) {
							localnodeAddr = iterator.next();
						}
					}
				}
			} catch (Exception e) {
				nodeAddr = new InetSocketAddress("127.0.0.1", defaultPort);
			}

			if (nodePars.getCurrentNodeType())
				bootstraps.put(node, bootstrap.clone().remoteAddress(nodeAddr).localAddress(localnodeAddr)
						.handler(new ChannelInitializer<Channel>() {
							@Override
							protected final void initChannel(final Channel conn) throws Exception {
								if (!conn.eventLoop().inEventLoop()) {
									throw new AssertionError();
								}
								connPoolHandler.channelCreated(conn);
							}
						}));
			else
				bootstraps.put(node,
						bootstrap.clone().remoteAddress(nodeAddr).handler(new ChannelInitializer<Channel>() {

							@Override
							protected final void initChannel(final Channel conn) throws Exception {
								LOG.debug("conn.eventLoop().inEventLoop={} connCounts={}",
										conn.eventLoop().inEventLoop(), connCounts);
								if (!conn.eventLoop().inEventLoop()) {
									throw new AssertionError();
								}
								connPoolHandler.channelCreated(conn);
							}
						}));
			availableConns.put(node, new ConcurrentLinkedQueue<>());
			connCounts.put(node, new AtomicInteger(1));
			failedConnAttemptCounts.put(node, new AtomicInteger(0));
		}
	}

	@Override
	public void preConnect(final int count) throws ConnectException, IllegalArgumentException, InterruptedException {
		if (count > 0) {
			final CountDownLatch latch = new CountDownLatch(count);
			for (int i = 0; i < count; i++) {
				String node = nodes[i % nodes.length];

				bootstraps.get(node).connect().addListener((ChannelFutureListener) future -> {
					try {
						Channel conn = future.channel();
						conn.closeFuture().addListener(new CloseChannelListener(node, conn));
						conn.attr(ATTR_KEY_NODE).set(node);
						allConns.computeIfAbsent(node, na -> new ConcurrentLinkedQueue<>()).add(conn);
						synchronized (connCounts) {
							connCounts.get(node).incrementAndGet();
						}
						if (connAttemptsLimit > 0) {
							// reset the connection failures counter if connected successfully
							failedConnAttemptCounts.get(node).set(0);
						}
						LOG.info("lambda New connection to " + node + " created {}", connAttemptsLimit);
						if (conn.isActive()) {
							final Queue<Channel> connQueue = availableConns.get(node);
							if (connQueue != null) {
								connQueue.add(conn);
								LOG.info("add connQueue");
							}
						} else {
							conn.close();
						}
					} finally {
						latch.countDown();
					}
				});
			}

			if (latch.await(connectTimeOut, connectTimeUnit)) {
				LOG.info("Pre-created " + count + " connections");
			} else {
				LOG.warn("Pre-created " + (count - latch.getCount()) + " connections");
			}
		} else {
			throw new IllegalArgumentException("Connection count should be > 0, but got " + count);
		}
		//LOG.info("allConns.size=[{}] [{}]", allConns.size(), allConns);
		//LOG.info("availableConns.size=[{}] [{}]", availableConns.size(), availableConns);
	}

	private final class CloseChannelListener implements ChannelFutureListener {

		private final String nodeAddr;
		private final Channel conn;

		private CloseChannelListener(final String nodeAddr, final Channel conn) {
			this.nodeAddr = nodeAddr;
			this.conn = conn;
		}

		@Override
		public final void operationComplete(final ChannelFuture future) throws Exception {
			LOG.info("Connection to " + nodeAddr + " closed");
			closeLock.lock();
			try {
				synchronized (connCounts) {
					if (connCounts.containsKey(nodeAddr)) {
						connCounts.get(nodeAddr).decrementAndGet();
					}
				}
				synchronized (allConns) {
					final Queue<Channel> nodeConns = allConns.get(nodeAddr);
					if (nodeConns != null) {
						nodeConns.remove(conn);
					}
				}
				//20220315 MatsudairasyuMe also remove availeConns Map
				synchronized (availableConns) {
					final Queue<Channel> nodeConns = availableConns.get(nodeAddr);
					if (nodeConns != null) {
						nodeConns.remove(conn);
					}
				}
				//----
			} finally {
				closeLock.unlock();
				LOG.debug("===>disconnect addr={}", nodeAddr);
				scheduleConnect(nodeAddr);
			}
		}
	}

	private Channel connectToAnyNode() throws ConnectException {

		Channel conn = null;

		// select the endpoint node having the minimum count of established connections
		String nodeAddr = null;
		String nextNodeAddr;
		int minConnsCount = Integer.MAX_VALUE;
		int nextConnsCount = 0;
		//20210406 MatsudairaSyuMe change for Insecure Randomness
		//final int i = ThreadLocalRandom.current().nextInt(n);
		SecureRandom secureRandomGenerator;
		int i = n - 1;
		try {
			secureRandomGenerator = SecureRandom.getInstance("SHA1PRNG");
			i = secureRandomGenerator.nextInt(n);
		} catch (NoSuchAlgorithmException e1) {
			// TODO Auto-generated catch block
			//20240503 MatsudairaSyuMe mark for System Information Leak e1.printStackTrace();
			LOG.error("get random number error use default [{}]", i);
		}	
		//----
		for (int j = i; j < n; j++) {
			nextNodeAddr = nodes[j % n];
			nextConnsCount = connCounts.get(nextNodeAddr).intValue();
			if (nextConnsCount == 0) {
				nodeAddr = nextNodeAddr;
				break;
			} else if (nextConnsCount < minConnsCount) {
				minConnsCount = nextConnsCount;
				nodeAddr = nextNodeAddr;
			}
		}

		if (nodeAddr != null) {
			// connect to the selected endpoint node
			LOG.info("New connection to selected end point node \"{}\"", nodeAddr);
			try {
				conn = connect(nodeAddr);
			} catch (final Exception e) {
				LOG.warn("Failed to create a new connection to {}: {}", nodeAddr, e.toString());
				if (connAttemptsLimit > 0) {
					int connAttempts = failedConnAttemptCounts.get(nodeAddr).incrementAndGet();
					if (connAttempts > connAttemptsLimit) {
						LOG.warn(
								"Failed to connect to the node \"{}\" {} times successively, excluding the node from the connection pool forever",
								nodeAddr, connAttempts);
						// the node having virtually Integer.MAX_VALUE established connections
						// will never be selected by the algorithm
						connCounts.get(nodeAddr).set(Integer.MAX_VALUE);
						boolean allNodesExcluded = true;
						for (final String node : nodes) {
							if (connCounts.get(node).get() < Integer.MAX_VALUE) {
								allNodesExcluded = false;
								break;
							}
						}
						if (allNodesExcluded) {
							LOG.info("No endpoint nodes left in the connection pool!");
						}
					}
				}

				if (e instanceof ConnectException) {
					throw (ConnectException) e;
				} else {
					throw new ConnectException(e.getMessage());
				}
			}
		}

		if (conn != null) {
			conn.closeFuture().addListener(new CloseChannelListener(nodeAddr, conn));
			conn.attr(ATTR_KEY_NODE).set(nodeAddr);
			allConns.computeIfAbsent(nodeAddr, na -> new ConcurrentLinkedQueue<>()).add(conn);
			synchronized (connCounts) {
				connCounts.get(nodeAddr).incrementAndGet();
			}
			if (connAttemptsLimit > 0) {
				// reset the connection failures counter if connected successfully
				failedConnAttemptCounts.get(nodeAddr).set(0);
			}
			LOG.info("New connection to {} successfully been created", nodeAddr);
		}

		return conn;
	}

	protected Channel connect(final String addr) throws Exception {

		Channel conn = null;
		final Bootstrap bootstrap = bootstraps.get(addr);
		if (bootstrap != null) {
			final ChannelFuture connFuture = bootstrap.connect();
			if (connectTimeOut > 0) {
				if (connFuture.await(connectTimeOut, connectTimeUnit)) {
					conn = connFuture.channel();
				}
			} else {
				conn = connFuture.sync().channel();
			}
		}
		LOG.info("connection to {} created", addr);
		return conn;
	}

	protected Channel poll() {
		//20210127 MatsidairaSyume
//		final int i = ThreadLocalRandom.current().nextInt(n);
		SecureRandom secureRandomGenerator = null;
		int i = 0;
		try {
			secureRandomGenerator = SecureRandom.getInstance("SHA1PRNG");
			i = secureRandomGenerator.nextInt(n);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			//20240503 MatsudairaSyuMe mark for System Information Leak e.printStackTrace();
			LOG.error("SecureRandom error:NoSuchAlgorithmException");
		}
		//----
		//LOG.info("TEST 1 availableConns[{}]", availableConns);
		Queue<Channel> connQueue;
		Channel conn = null;
		for (int j = i; j < i + n; j++) {
			//LOG.info("TEST 2 j = [{}] n = [{}] nodes[j % n] = [{}]", j, n, nodes[j % n]);
			connQueue = availableConns.get(nodes[j % n]);
			if (connQueue != null) {
				conn = connQueue.poll();
				if (conn != null && conn.isOpen()) {
					break;
				}
			}
		}
		return conn;
	}

/*	private void doConnect(final String nodeAddr, final long _wait) {
		try {
			Channel conn = null;
			final Bootstrap bootstrap = bootstraps.get(nodeAddr);
			if (bootstrap != null) {
				final ChannelFuture connFuture = bootstrap.connect();
				if (connectTimeOut > 0) {
					if (connFuture.await(connectTimeOut, connectTimeUnit)) {
						conn = connFuture.channel();
					}
				} else {
					conn = connFuture.sync().channel();
				}
			}
			//20210407 MatsudairaSyuMe check Null Dereference
			if (conn != null) {
				LOG.info("doConnect connection to {} created", nodeAddr);
				conn.closeFuture().addListener(new CloseChannelListener(nodeAddr, conn));
				conn.attr(ATTR_KEY_NODE).set(nodeAddr);
				allConns.computeIfAbsent(nodeAddr, na -> new ConcurrentLinkedQueue<>()).add(conn);
				synchronized (connCounts) {
					connCounts.get(nodeAddr).incrementAndGet();
				}
				if (connAttemptsLimit > 0) {
					// reset the connection failures counter if connected successfully
					failedConnAttemptCounts.get(nodeAddr).set(0);
				}
				// 20200621
				LOG.info("doConnect New connection to {} successfully been created connAttemptsLimit={}", nodeAddr,
						connAttemptsLimit);
				if (conn.isActive()) {
					final Queue<Channel> connQueue = availableConns.get(nodeAddr);
					if (connQueue != null) {
						connQueue.add(conn);
						LOG.info("add connQueue");
					}
				} else {
					conn.close();
				}
				// 20200621
			// 20210407 MatsudairaSyuMe check Null Dereference
			} else {
				LOG.error("!!! doConnect fail !!!!");
				scheduleConnect(nodeAddr);  //20210722 schedultConnect
			}
			//----
		} catch (Exception ex) {
			scheduleConnect(nodeAddr);
		}
	}*/
	//20220315 MatsudairaSyuMe
	private void reConnect(final String node, final long _wait) {
		bootstraps.get(node).connect().addListener((ChannelFutureListener) future -> {
		    Channel conn = future.channel();
		    conn.closeFuture().addListener(new CloseChannelListener(node, conn));
		    conn.attr(ATTR_KEY_NODE).set(node);
//		    LOG.info("reConnect 1 availableConns.size=[{}] [{}]",availableConns.size(), availableConns);
//		    LOG.info("reConnect 1 allConns.size=[{}] [{}]",allConns.size(), allConns);
		    //2020325 add
		    if (!allConns.containsKey(node))
		    //----
		    	allConns.computeIfAbsent(node, na -> new ConcurrentLinkedQueue<>()).add(conn);
		    //20220325 add
		    else {
		    	final Queue<Channel> connQueue = allConns.get(node);
			    if (connQueue != null) {
				    connQueue.add(conn);
				    LOG.info("reConnect add connQueue to allConns");
			    }		    	
		    }
		    //----
//		    availableConns.computeIfAbsent(node, na -> new ConcurrentLinkedQueue<>()).add(conn);
		    synchronized (connCounts) {
			    connCounts.get(node).incrementAndGet();
		    }
		    if (connAttemptsLimit > 0) {
			    // reset the connection failures counter if connected successfully
			    failedConnAttemptCounts.get(node).set(0);
		    }
		    LOG.info("reConnect lambda New connection to " + node + " created {}", connAttemptsLimit);
		    if (conn.isActive()) {
				// 2020325 add
				if (!availableConns.containsKey(node)) {
					// ----
					availableConns.computeIfAbsent(node, na2 -> new ConcurrentLinkedQueue<>()).add(conn);
					LOG.info("reConnect add connQueue to availableConns");
				} else {
					final Queue<Channel> connQueue = availableConns.get(node);
					if (connQueue != null) {
						connQueue.add(conn);
						LOG.info("reConnect add connQueue");
					}
				}
//			    LOG.info("reConnect 2 allConns.size=[{}] [{}]", allConns.size(), allConns);
//			    LOG.info("reConnect 2 availableConns.size=[{}] [{}]", availableConns.size(), availableConns);
		    } else {
			    conn.close();
		    }
	    });
	}

	private void scheduleConnect(final String nodeAddr) {
		if (this.reconnectInterval > 0) {
			timer_.schedule(new TimerTask() {
				@Override
				public void run() {
					reConnect(nodeAddr, reconnectInterval); //20220315 change doConnect to reConnect
				}
			}, reconnectInterval);
		}
	}

	@Override
	public final Channel lease() throws ConnectException {
		Channel conn;
		if (null == (conn = poll())) {
			//20210112 MatsudairaSyume
			LOG.warn("WORNING!!! no connection");
//			conn = connectToAnyNode();
		}
		if (conn == null) {
			throw new ConnectException();
		}
		return conn;
	}

	@Override
	public final int lease(final List<Channel> conns, final int count) throws ConnectException {
		Channel conn;
		for (int i = 0; i < count; i++) {
			if (null == (conn = poll())) {
				conn = connectToAnyNode();
			}
			if (conn == null) {
				throw new ConnectException();
			} else {
				conns.add(conn);
			}
		}
		return count;
	}
	//20210112 MatsudairaSyume
	@Override
	public final Channel lease(int fail_every_conn_attempt, long test_time_seconds) throws ConnectException {
		Channel conn;
		synchronized (allConns) {
			int attempt = 0;
			while (null == (conn = poll())) {
				if (++attempt < fail_every_conn_attempt) {
					// 20210112 MatsudairaSyume
					LOG.warn("WORNING!!! poll busy re-try after {} second(s)", test_time_seconds);
					try {
						Thread.sleep(test_time_seconds * 1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						;
					}
				} else
					break;
			}
			if (conn == null) {
				throw new ConnectException();
			}
		}
		return conn;
	}

	@Override
	public final void release(final Channel conn) {
		final String nodeAddr = conn.attr(ATTR_KEY_NODE).get();
		if (conn.isActive()) {
			final Queue<Channel> connQueue = availableConns.get(nodeAddr);
			if (connQueue != null) {
				connQueue.add(conn);
			}
		} else {
			conn.close();
		}
	}

	@Override
	public final void release(final List<Channel> conns) {
		String nodeAddr;
		Queue<Channel> connQueue;
		for (final Channel conn : conns) {
			nodeAddr = conn.attr(ATTR_KEY_NODE).get();
			if (conn.isActive()) {
				connQueue = availableConns.get(nodeAddr);
				connQueue.add(conn);
			} else {
				conn.close();
			}
		}
	}

	@Override
	public void close() throws IOException {
		closeLock.lock();
		int closedConnCount = 0;
		try {
			for (final String nodeAddr : availableConns.keySet()) {
				for (final Channel conn : availableConns.get(nodeAddr)) {
					if (conn.isOpen()) {
						conn.close();
						closedConnCount++;
					}
				}
			}
			availableConns.clear();

			try {
				TimeUnit.MILLISECONDS.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				//20240503 MatsudairaSyuMe mark for System Information Leak e.printStackTrace();
			}

			for (final String nodeAddr : allConns.keySet()) {
				for (final Channel conn : allConns.get(nodeAddr)) {
					if (conn.isOpen()) {
						conn.close();
						closedConnCount++;
					}
				}
			}
			allConns.clear();
			bootstraps.clear();
			connCounts.clear();
		} finally {
			closeLock.unlock();
		}
		LOG.info("Closed " + closedConnCount + " connections");
	}

}
