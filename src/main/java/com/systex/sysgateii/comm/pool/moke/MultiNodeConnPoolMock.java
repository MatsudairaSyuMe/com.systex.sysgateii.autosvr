package com.systex.sysgateii.comm.pool.moke;

import com.systex.sysgateii.comm.pool.MultiNodeConnPoolImpl;
import com.systex.sysgateii.comm.pool.NonBlockingConnPool;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.pool.ChannelPoolHandler;

import java.util.concurrent.TimeUnit;

/**
 * Created by andrey on 12.05.17.
 *  Modyfied by MatsudairaSyume 2019/11/4
 */

public class MultiNodeConnPoolMock extends MultiNodeConnPoolImpl implements NonBlockingConnPool {
	public MultiNodeConnPoolMock(final String[] nodes, final Bootstrap bootstrap,
			final ChannelPoolHandler connPoolHandler, final int defaultPort, final int connFailSeqLenLimit, final long reconnectInterval) {
		super(nodes, bootstrap, connPoolHandler, defaultPort, connFailSeqLenLimit, 0, TimeUnit.SECONDS, reconnectInterval);
	}

	protected final Channel connect(final String addr) {
		final Channel c = new EmbeddedChannel();
		c.attr(ATTR_KEY_NODE).set(addr);
		return c;
	}
}
