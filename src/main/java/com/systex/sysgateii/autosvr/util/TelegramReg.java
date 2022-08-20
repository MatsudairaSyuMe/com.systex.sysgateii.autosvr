package com.systex.sysgateii.autosvr.util;

import io.netty.channel.ChannelHandlerContext;

public class TelegramReg {
	private long outTime = 0l;
	private ChannelHandlerContext sourceHandlerCtx;
	public TelegramReg(long ot, ChannelHandlerContext ctx) {
		this.outTime = ot;
		this.sourceHandlerCtx = ctx;
	}
	/**
	 * @return the outTime
	 */
	public long getOutTime() {
		return outTime;
	}
	/**
	 * @param outTime the outTime to set
	 */
	public void setOutTime(long outTime) {
		this.outTime = outTime;
	}
	/**
	 * @return the sourceHandlerCtx
	 */
	public ChannelHandlerContext getSourceHandlerCtx() {
		return sourceHandlerCtx;
	}
	/**
	 * @param sourceHandlerCtx the sourceHandlerCtx to set
	 */
	public void setSourceHandlerCtx(ChannelHandlerContext sourceHandlerCtx) {
		this.sourceHandlerCtx = sourceHandlerCtx;
	}
}
