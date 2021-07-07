package com.systex.sysgateii.comm.pool.test;

import java.io.Closeable;
import java.net.ConnectException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import com.systex.sysgateii.comm.pool.MultiNodeConnPoolImpl;
import com.systex.sysgateii.comm.pool.NonBlockingConnPool;
import com.systex.sysgateii.comm.pool.moke.DummyChannelPoolHandler;
import com.systex.sysgateii.comm.pool.moke.DummyClientChannelHandler;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.socket.SocketChannel;

public class EpollConnDropTest2 {
	private static final int CONCURRENCY = 2;
	private static final String[] NODES = new String[] { "127.0.0.1:15000" };
	private static final ChannelPoolHandler CPH = new DummyChannelPoolHandler();
	private static final long TEST_TIME_SECONDS = 100;
//	private static final int CONN_ATTEMPTS = 10_000;
	private static final int CONN_ATTEMPTS = 10;
	private static final int FAIL_EVERY_CONN_ATTEMPT = 10;
//	private static final ByteBuf PAYLOAD = Unpooled.directBuffer(0x1000).writeZero(0x1000);
	private static final ByteBuf PAYLOAD = Unpooled.directBuffer(0x1000);

	Closeable serverMock;
	NonBlockingConnPool connPool;
	EventLoopGroup group;
	Bootstrap bootstrap;
	public EpollConnDropTest2(String nodes[], int port) throws ConnectException, IllegalArgumentException, InterruptedException {
		group = new EpollEventLoopGroup();
		bootstrap = new Bootstrap().group(group).channel(EpollSocketChannel.class)
				.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected final void initChannel(final SocketChannel conn) throws Exception {
						conn.pipeline().addLast(new DummyClientChannelHandler());
					}
				}).option(ChannelOption.SO_KEEPALIVE, true).option(ChannelOption.SO_REUSEADDR, true)
				.option(ChannelOption.TCP_NODELAY, true);
		connPool = new MultiNodeConnPoolImpl(NODES, bootstrap, CPH, port, 0, 1, TimeUnit.SECONDS, 3000);
		connPool.preConnect(CONCURRENCY);
	}

	public static void main(String[] args) throws Exception {
		try {
			EpollConnDropTest2 ec2 = new EpollConnDropTest2 (NODES, 15000);
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
							ByteBuf req = Unpooled.wrappedBuffer("Hello".getBytes("UTF-8"));
//							conn.writeAndFlush(PAYLOAD.retain()).sync();
							conn.writeAndFlush(req);

							ec2.connPool.release(conn);
							connCounter.increment();
						} catch (final InterruptedException e) {
							break;
						} catch (final Throwable cause) {
							cause.printStackTrace(System.err);
						}
					}
				});
			}
			while (true);
//			executor.shutdown();
//			executor.awaitTermination(TEST_TIME_SECONDS, TimeUnit.SECONDS);
//			assertTrue(executor.isTerminated());
//			assertEquals(CONCURRENCY * CONN_ATTEMPTS, connCounter.sum(),
//					2 * CONCURRENCY * CONN_ATTEMPTS / FAIL_EVERY_CONN_ATTEMPT);
//			ec2.connPool.close();
//			ec2.group.shutdownGracefully();

		}catch (ConnectException e) {
			System.err.println(e);
		}catch (IllegalArgumentException e) {
			System.err.println(e);
		}catch (InterruptedException e) {
			System.err.println(e);
		}

	}
}
