package com.systex.sysgateii.comm.pool.fas;

import com.systex.sysgateii.comm.pool.MultiNodeConnPoolImpl;
import com.systex.sysgateii.comm.pool.NonBlockingConnPool;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.Closeable;
import java.io.File;
import java.net.ConnectException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/*
 * FASSocketChannel
 * SocketChannel pool handler for FAS
 *  MatsudairaSyuMe
 *  Ver 1.0
 *  20200115
 */
public class FASSocketChannel {
	private static Logger log = LoggerFactory.getLogger(FASSocketChannel.class);
	private static final int CONCURRENCY = 2;
//	private static final int CONCURRENCY = 1;
	private static final String[] NODES = new String[] { "127.0.0.1:15000=127.0.0.1:15101",
			"127.0.0.1:15000=127.0.0.1:15102", "127.0.0.1:15000=127.0.0.1:15103" };
//	private static final String[] NODES = new String[] { "127.0.0.1:15000=127.0.0.1:15101"};
//	private static final String[] NODES = new String[] { "127.0.0.1:15000"};
	//20240516 MatsudairaSyuMe change to use private recBuf for every connect port private static ByteBuf rcvBuf = Unpooled.buffer(16384);
	private static ConcurrentHashMap<Channel, File> seqf_map = new ConcurrentHashMap<Channel, File>();
	/*20240516 MatsudairaSyuMe change to use private recBuf for every connect port
	private static final ChannelPoolHandler CPH = new FASChannelPoolHandler(rcvBuf, seqf_map);
	*/
	private static final ChannelPoolHandler CPH = new FASChannelPoolHandler(seqf_map);
	private static final int DEFAULT_PORT = 15_000;
	private static final long TEST_TIME_SECONDS = 20;
	private static final int CONN_ATTEMPTS = 4;
	private static final int FAIL_EVERY_CONN_ATTEMPT = 10;

	Closeable serverMock;
	private NonBlockingConnPool connPool;
	EventLoopGroup group;

//	public FASSocketChannel(String nodes[], ServerProducer producer, List<String> brnolist, List<String> wsnolist) throws ConnectException, IllegalArgumentException, InterruptedException {
	public FASSocketChannel(String nodes[], List<String> brnolist, List<String> wsnolist) throws ConnectException, IllegalArgumentException, InterruptedException {
		group = new NioEventLoopGroup();
		final Bootstrap bootstrap = new Bootstrap().group(group).channel(NioSocketChannel.class);
		//---- 20200422 test
		//20240516 mark producer ((FASChannelPoolHandler)CPH).setProducer(producer);
		((FASChannelPoolHandler)CPH).setBrnoList(brnolist);
		((FASChannelPoolHandler)CPH).setWsnoList(wsnolist);
		//----
		connPool = new MultiNodeConnPoolImpl(nodes, bootstrap, CPH, DEFAULT_PORT, 0, 1, TimeUnit.SECONDS, 3000);
		connPool.preConnect(nodes.length);
	}

	public FASSocketChannel(String nodes[]) throws ConnectException, IllegalArgumentException, InterruptedException {
		group = new NioEventLoopGroup();
		final Bootstrap bootstrap = new Bootstrap().group(group).channel(NioSocketChannel.class);
		connPool = new MultiNodeConnPoolImpl(nodes, bootstrap, CPH, DEFAULT_PORT, 0, 1, TimeUnit.SECONDS, 3000);
		connPool.preConnect(nodes.length);
	}

	public NonBlockingConnPool getConnPool() {
		return connPool;
	}

	public void setConnPool(NonBlockingConnPool connPool) {
		this.connPool = connPool;
	}
	/*20240516 MatsudairaSyuMe change to use private recBuf for every connect port
	public ByteBuf getrcvBuf() {
		return this.rcvBuf;
	}
	*/
	public ConcurrentHashMap<Channel, File> getseqfMap() {
		return this.seqf_map;
	}

}
