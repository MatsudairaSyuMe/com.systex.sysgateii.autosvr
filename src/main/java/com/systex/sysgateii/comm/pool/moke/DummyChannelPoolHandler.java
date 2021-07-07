package com.systex.sysgateii.comm.pool.moke;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * Created by andrey on 13.05.17.
 *  modyfied by MatsudairaSyume 2019/11/4
 */

public final class DummyChannelPoolHandler implements ChannelPoolHandler {
	private static Logger log = LoggerFactory.getLogger(DummyChannelPoolHandler.class);

	@Override
	public final void channelReleased(final Channel ch) throws Exception {
		log.debug("channelReleased");
	}

	@Override
	public final void channelAcquired(final Channel ch) throws Exception {
		log.debug("channelAcquired");
	}

	@Override
	public final void channelCreated(final Channel ch) throws Exception {
		log.debug("channelCreated");
		SocketChannel channel = (SocketChannel) ch;
		channel.config().setKeepAlive(true);
		channel.config().setTcpNoDelay(true);
		channel.config().setReuseAddress(true);
		channel.pipeline()
		       .addLast("log", new LoggingHandler(DummyClientChannelHandler.class, LogLevel.INFO)) // 測試用
		       .addLast(new IdleStateHandler(4, 0, 0, TimeUnit.SECONDS))
				 .addLast(new DummyClientChannelHandler());
	}
}
