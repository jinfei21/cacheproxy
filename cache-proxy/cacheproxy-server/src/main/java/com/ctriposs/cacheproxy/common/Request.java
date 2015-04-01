package com.ctriposs.cacheproxy.common;

import java.net.InetAddress;

public abstract class Request extends ProxyMessage {

    private RequestType requestType;
   

    private InetAddress localAddress;
    private int localPort;

    private InetAddress remoteAddress;
    private int remotePort;

    private long completedReceiveTime;

    public void setRequestType(RequestType requestType){
    	this.requestType = requestType;
    }
    
    public RequestType getRequestType() {
        return requestType;
    }

    public InetAddress getLocalAddress() {
        return localAddress;
    }

    public void setLocalAddress(InetAddress localAddress) {
        this.localAddress = localAddress;
    }

    public int getLocalPort() {
        return localPort;
    }

    public void setLocalPort(int localPort) {
        this.localPort = localPort;
    }

    public InetAddress getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(InetAddress remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public void setRemotePort(int remotePort) {
        this.remotePort = remotePort;
    }

    public long getCompletedReceiveTime() {
        return completedReceiveTime;
    }

    public void setCompletedReceiveTime(long completedReceiveTime) {
        this.completedReceiveTime = completedReceiveTime;
    }
	
}
