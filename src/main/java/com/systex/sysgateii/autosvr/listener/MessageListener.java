package com.systex.sysgateii.autosvr.listener;

public interface MessageListener<T> {
	public void messageReceived(String serverId, T msg);
}
