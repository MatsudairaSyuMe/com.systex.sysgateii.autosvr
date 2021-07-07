package com.systex.sysgateii.comm.pool.test;

import com.systex.sysgateii.comm.pool.MultiNodeConnPoolImpl;
import com.systex.sysgateii.comm.pool.NonBlockingConnPool;
import com.systex.sysgateii.comm.pool.moke.DummyChannelPoolHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.socket.nio.NioSocketChannel;

import static org.junit.Assert.assertEquals;
import java.io.Closeable;
import java.net.ConnectException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public class NioConnDropTest2 {
	private static final int CONCURRENCY = 2;
//	private static final int CONCURRENCY = 1;
	private static final String[] NODES = new String[] { "127.0.0.1:15000=127.0.0.1:15101",
			"127.0.0.1:15000=127.0.0.1:15102", "127.0.0.1:15000=127.0.0.1:15103" };
//	private static final String[] NODES = new String[] { "127.0.0.1:15000=127.0.0.1:15101"};
//	private static final String[] NODES = new String[] { "127.0.0.1:15000"};
	private static final ChannelPoolHandler CPH = new DummyChannelPoolHandler();
//	private static final int DEFAULT_PORT = 12_345;
	private static final int DEFAULT_PORT = 15_000;
	private static final long TEST_TIME_SECONDS = 20;
	private static final int CONN_ATTEMPTS = 4;
	private static final int FAIL_EVERY_CONN_ATTEMPT = 10;

	Closeable serverMock;
	NonBlockingConnPool connPool;
	EventLoopGroup group;

	public NioConnDropTest2(String nodes[])
			throws ConnectException, IllegalArgumentException, InterruptedException {
		group = new NioEventLoopGroup();
		final Bootstrap bootstrap = new Bootstrap().group(group).channel(NioSocketChannel.class);
		connPool = new MultiNodeConnPoolImpl(NODES, bootstrap, CPH, DEFAULT_PORT, 0, 1, TimeUnit.SECONDS, 3000);
		connPool.preConnect(NODES.length);
	}

	public static void main(String[] args) throws Exception {
		try {
			NioConnDropTest2 ec2 = new NioConnDropTest2(NODES);
			final LongAdder connCounter = new LongAdder();

			final ExecutorService executor = Executors.newFixedThreadPool(CONCURRENCY);
			for (int i = 0; i < CONCURRENCY; i++) {
				executor.submit(() -> {
					Channel conn;
					for (int j = 0; j < CONN_ATTEMPTS; j++) {
						try {
							while (null == (conn = ec2.connPool.lease())) {
								Thread.sleep(1);
							}
							System.out.println("get a channel and send messsage");
							ByteBuf req = Unpooled.wrappedBuffer("Hello".getBytes("UTF-8"));
							conn.writeAndFlush(req.retain()).sync();

							ec2.connPool.release(conn);
							System.out.println("return a channel");
							connCounter.increment();
						} catch (final InterruptedException e) {
							break;
						} catch (final Throwable cause) {
							cause.printStackTrace(System.err);
						}
					}
/*					try {
						TimeUnit.SECONDS.sleep(TEST_TIME_SECONDS);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

					for (int j = 0; j < CONN_ATTEMPTS; j++) {
						try {
							while (null == (conn = ec2.connPool.lease())) {
								Thread.sleep(1);
							}
							System.out.println("get a channel and send messsage");
							ByteBuf req = Unpooled.wrappedBuffer("Hello".getBytes("UTF-8"));
							conn.writeAndFlush(req.retain()).sync();

							ec2.connPool.release(conn);
							System.out.println("return a channel");
							connCounter.increment();
						} catch (final InterruptedException e) {
							break;
						} catch (final Throwable cause) {
							cause.printStackTrace(System.err);
						}
					}
*/
				});
			}
			while (true);
//			TimeUnit.SECONDS.sleep(TEST_TIME_SECONDS);
//			executor.shutdownNow();
//			ec2.connPool.close();
//			assertEquals(CONCURRENCY * CONN_ATTEMPTS, connCounter.sum(),
//					2 * CONCURRENCY * CONN_ATTEMPTS / FAIL_EVERY_CONN_ATTEMPT);
		} catch (ConnectException e) {
			System.err.println(e);
		} catch (IllegalArgumentException e) {
			System.err.println(e);
		} catch (InterruptedException e) {
			System.err.println(e);
		}
		System.gc();
		System.exit(0);
	}
}
