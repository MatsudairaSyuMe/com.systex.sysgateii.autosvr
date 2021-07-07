package com.systex.sysgateii.comm.pool.moke;

import java.util.concurrent.TimeUnit;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

public class DummyClientReconnectionTask implements Runnable, ChannelFutureListener {

    Channel previous;

    public DummyClientReconnectionTask(Channel c) {
        this.previous = c;
    }

    @Override
    public void run() {
         Bootstrap b = new Bootstrap().group(new NioEventLoopGroup()).channel(NioSocketChannel.class);
         b.option(ChannelOption.SO_KEEPALIVE, true);
         b.option(ChannelOption.SO_REUSEADDR, true);
         b.remoteAddress(previous.remoteAddress()).localAddress(previous.localAddress())
          .connect()
          .addListener(this);
    }

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
        if (!future.isSuccess()) {
            // Will try to connect again in 100 ms.
            // Here you should probably use exponential backoff or some sort of randomization to define the retry period.
            previous.eventLoop()
                    .schedule(this, 100, TimeUnit.MILLISECONDS); 
            return;
        }
    }
}
