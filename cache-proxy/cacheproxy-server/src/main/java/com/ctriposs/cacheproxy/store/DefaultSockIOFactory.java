package com.ctriposs.cacheproxy.store;

import com.ctriposs.cacheproxy.common.SockIO;
import com.ctriposs.cacheproxy.common.SockIOFactory;
import com.ctriposs.cacheproxy.util.Args;

public class DefaultSockIOFactory implements SockIOFactory{


    private String host;
    private int port;
    private volatile int connectTimeout;
    private volatile int readTimeout;

    public DefaultSockIOFactory(String addr, int connectTimeout, int readTimeout) {   	
        Args.notEmpty(addr, "host:port");
        String[] ip = addr.split(":");
        this.host = Args.notEmpty(ip[0],"host");
        this.port = Args.positive(Integer.parseInt(ip[1]), "port");
        this.connectTimeout = Args.positive(connectTimeout, "connectTimeout");
        this.readTimeout = Args.positive(readTimeout, "readTimeout");
    }
    
    public DefaultSockIOFactory(String host, int port, int connectTimeout, int readTimeout) {
        this.host = Args.notEmpty(host, "host");
        this.port = Args.positive(port, "port");
        this.connectTimeout = Args.positive(connectTimeout, "connectTimeout");
        this.readTimeout = Args.positive(readTimeout, "readTimeout");
    }

	@Override
	public SockIO create() throws Exception {
		return new NodeSock(host, port, readTimeout, connectTimeout,false);
	}

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = Args.positive(connectTimeout, "connectTimeout");
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = Args.positive(readTimeout, "readTimeout");
    }
}
