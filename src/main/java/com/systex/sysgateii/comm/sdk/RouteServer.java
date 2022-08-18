package com.systex.sysgateii.comm.sdk;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.systex.sysgateii.autosvr.autoPrtSvr.Server.FASSvr;
import com.systex.sysgateii.autosvr.comm.Constants;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;

public class RouteServer implements Runnable {
	private static Logger log = LoggerFactory.getLogger(RouteServer.class);
	private String IP = "localhost";
	private int port = 5555;
	private static int timeout = 30000;
	//static RouteServer brokerserver;
	private int bufferSize = Integer.parseInt(System.getProperty("bufferSize", Constants.DEF_CHANNEL_BUFFER_SIZE + ""));
	public RouteServer (String _IP, int _port, int _tout) {
		this.IP = _IP;
		this.port = _port;
		//brokerserver = new RouteServer();
		this.setTimeout(_tout);
	}

	/*
	public static void startServer() {
		log.debug("Enter startServer");
		Thread monitorThread = new Thread(new Runnable() {
//		if (brokerserver != null) {
//			try {
//				brokerserver.run();
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//				log.error("start RouteServer Exception");
//			}
//		}

		@Override
		public void run() {
				log.error("RouteServer run starting");
				EventLoopGroup bossGroup = new NioEventLoopGroup();
				EventLoopGroup workerGroup = new NioEventLoopGroup();
				try {
					ServerBootstrap b = new ServerBootstrap();
					b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
							.childHandler(new ChannelInitializer<SocketChannel>() {
								@Override
								protected void initChannel(SocketChannel ch) throws Exception {
									// ByteBuf delimiter = Unpooled.copiedBuffer("$_".getBytes());
									// ch.pipeline().addLast(new DelimiterBasedFrameDecoder(1024, delimiter))
									// .addLast(new StringDecoder()).addLast(new StringEncoder())
									// ch.pipeline().addLast(new StringDecoder()).addLast(new StringEncoder())
									// .addLast(new DefaultEventExecutorGroup(8),
									ch.pipeline().addLast(new DefaultEventExecutorGroup(100), new RouteServerHandler());
								}
							}).option(ChannelOption.SO_BACKLOG, 128).childOption(ChannelOption.SO_KEEPALIVE, true);
					// ChannelFuture future = b.bind(port).sync();
					ChannelFuture future = b.bind(IP, port).sync();
					future.channel().closeFuture().sync();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					log.error("RouteServer run error");
				} finally {
					bossGroup.shutdownGracefully();
					workerGroup.shutdownGracefully();
				}
			}
		});// monitorThread
		monitorThread.start();

	}*/
	public void run() {
		log.error("RouteServer run starting");
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
					.childHandler(new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(SocketChannel ch) throws Exception {
							// ByteBuf delimiter = Unpooled.copiedBuffer("$_".getBytes());
							// ch.pipeline().addLast(new DelimiterBasedFrameDecoder(1024, delimiter))
							// .addLast(new StringDecoder()).addLast(new StringEncoder())
							// ch.pipeline().addLast(new StringDecoder()).addLast(new StringEncoder())
							// .addLast(new DefaultEventExecutorGroup(8),
							ch.pipeline().addLast("log", new LoggingHandler(RouteServer.class, LogLevel.INFO)).
							addLast(new DefaultEventExecutorGroup(100), new RouteServerHandler());
						}
					}).option(ChannelOption.SO_BACKLOG, 128)
					.childOption(ChannelOption.SO_RCVBUF, bufferSize)
					.childOption(ChannelOption.SO_SNDBUF, bufferSize)
					.childOption(ChannelOption.SO_KEEPALIVE, true);
			// ChannelFuture future = b.bind(port).sync();
			ChannelFuture future = b.bind(IP, port).sync();
			future.channel().closeFuture().sync();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			log.error("RouteServer run error");
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

	/**
	 * @return the timeout
	 */
	public static int getTimeout() {
		return timeout;
	}

	/**
	 * @param timeout the timeout to set
	 */
	public void setTimeout(int timeout) {
		RouteServer.timeout = timeout;
	}
}
