package com.systex.sysgateii.comm.pool.moke;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

public class DummyClientChannelHandler extends ChannelInboundHandlerAdapter {

	private static Logger log = LoggerFactory.getLogger(DummyClientChannelHandler.class);
	private static final ByteBuf HEARTBEAT_SEQUENCE = Unpooled
			.unreleasableBuffer(Unpooled.copiedBuffer("hb_request", CharsetUtil.UTF_8));

	static AtomicInteger count = new AtomicInteger(1);

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		log.debug("channelActive==========");
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		log.debug("channelInactive==========");
//		Channel channel = ctx.channel();
		/* If shutdown is on going, ignore */
//		if (channel.eventLoop().isShuttingDown())
//			return;

//		DummyClientReconnectionTask reconnect = new DummyClientReconnectionTask(channel);
//		reconnect.run();

	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		log.debug("ChannelRead==========");
		try {
			if (msg instanceof ByteBuf) {
				ByteBuf buf = (ByteBuf) msg;
				log.debug("capacity=" + buf.capacity() + " readableBytes=" + buf.readableBytes() + " barray="
						+ buf.hasArray() + " nio=  " + buf.nioBufferCount());
				byte[] req = new byte[buf.readableBytes()];
				buf.readBytes(req);
				String body = new String(req, CharsetUtil.UTF_8);
				log.debug(count.getAndIncrement() + ":" + ctx.channel().id() + "<-" + body);
			} else
				log.error("not ByteBuf message");
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object obj) throws Exception {

		if (obj instanceof IdleStateEvent) {
			IdleStateEvent event = (IdleStateEvent) obj;
//			if (IdleState.WRITER_IDLE.equals(event.state())) { // 如果寫通道處於空閒狀態就發送心跳命令
			if (IdleState.READER_IDLE.equals(event.state())) { // 如果讀通道處於空閒狀態就發送心跳命令
				ctx.channel().writeAndFlush(HEARTBEAT_SEQUENCE.duplicate());
			}
		}
	}

	private String date() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return sdf.format(new Date());
	}

}
