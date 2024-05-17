package com.systex.sysgateii.comm.pool.fas;

import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * Created by MatsudairaSyume
 *  2020/01/15
 */

public class FASChannelPoolHandler implements ChannelPoolHandler {
	private static Logger log = LoggerFactory.getLogger(FASChannelPoolHandler.class);
	//20240516 MatsudairaSyuMe change to use private recBuf for every connect port private ByteBuf rcvBuf = null;
	private ConcurrentHashMap<Channel, File> seqf_map = null;
	//20240516 MatsudairaSyuMe mark producer private ServerProducer producer = null;
	private List<String> brnoList = null;
	private List<String> wsnoList = null;
	/*20240516 MatsudairaSyuMe change to use private recBuf for every connect port 
	public FASChannelPoolHandler(ByteBuf rcvBuf) {
		this.rcvBuf = rcvBuf;
	}
	*/
	public FASChannelPoolHandler() {
	}
	//20240516 MatsudairaSyuMe change to use private recBuf for every connect port 
	public FASChannelPoolHandler(ConcurrentHashMap<Channel, File> seqfmap, List<String> brnolist, List<String> wsnolist) {
		//20240516 MatsudairaSyuMe change to use private recBuf for every connect port this.rcvBuf = rcvBuf;
		this.seqf_map = seqfmap;
		this.brnoList = brnolist;
		this.wsnoList = wsnolist;
	}
	//20240516 MatsudairaSyuMe change to use private recBuf for every connect port 
	public FASChannelPoolHandler(ConcurrentHashMap<Channel, File> seqfmap) {
		//20240516 MatsudairaSyuMe change to use private recBuf for every connect port this.rcvBuf = rcvBuf;
		this.seqf_map = seqfmap;
	}
	/*20240516 MatsudairaSyuMe change to use private recBuf for every connect port
	public ByteBuf getRcvBuf() throws Exception {
		log.debug("getRcvBuf");
		return rcvBuf;
	}
	*/


	@Override
	public void channelAcquired(final Channel ch) throws Exception {
		// TODO Auto-generated method stub
		log.debug("channelAcquired");
	}

	@Override
	public void channelCreated(final Channel ch) throws Exception {
		// TODO Auto-generated method stub
		log.debug("channelCreated");
		//---- 20200422 test
		//20240516 MatsudairaSyuMe change to use private recBuf for every connect port 
		FASClientChannelHandler nf = new FASClientChannelHandler(seqf_map, brnoList, wsnoList);
		//20240516 MatsudairaSyuMe mark nf.addActorStatusListener(producer);
		//----
		SocketChannel channel = (SocketChannel) ch;
		channel.config().setKeepAlive(true);
		channel.config().setTcpNoDelay(true);
		channel.config().setReuseAddress(true);
//		channel.pipeline().addLast("log", new LoggingHandler(FASClientChannelHandler.class, LogLevel.INFO))
//				.addLast(new IdleStateHandler(4, 0, 0, TimeUnit.SECONDS)).addLast(new FASClientChannelHandler());
		//no heart beat check
		channel.pipeline().addLast("log", new LoggingHandler(FASClientChannelHandler.class, LogLevel.INFO))
//		.addLast(new FASClientChannelHandler(rcvBuf, seqf_map));
		//----  20200422 test
		.addLast(nf);
		//----
	}

	@Override
	public void channelReleased(final Channel ch) throws Exception {
		// TODO Auto-generated method stub
		log.debug("channelReleased");
	}
	/*20240516 mark producer
	public ServerProducer getProducer() {
		return producer;
	}
	public void setProducer(ServerProducer producer) {
		this.producer = producer;
	}
	*/

	public List<String> getBrnoList() {
		return this.brnoList;
	}

	public void setBrnoList(List<String> brnolist) {
		// TODO Auto-generated method stub
		this.brnoList = brnolist;
	}

	public List<String> getWsnoList() {
		return this.wsnoList;
	}

	public void setWsnoList(List<String> wsnolist) {
		// TODO Auto-generated method stub
		this.wsnoList = wsnolist;
	}

}
