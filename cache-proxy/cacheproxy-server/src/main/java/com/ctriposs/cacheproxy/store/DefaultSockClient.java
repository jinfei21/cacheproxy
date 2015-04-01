package com.ctriposs.cacheproxy.store;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ctriposs.cacheproxy.common.Request;
import com.ctriposs.cacheproxy.common.SockClient;
import com.ctriposs.cacheproxy.common.SockIO;
import com.ctriposs.cacheproxy.common.SockIOFactory;
import com.ctriposs.cacheproxy.common.SockIOPool;
import com.ctriposs.cacheproxy.util.Args;

public class DefaultSockClient implements SockClient {
	private static Logger logger = LoggerFactory
			.getLogger(DefaultSockClient.class);

	private volatile int minSocks = 10;
	private volatile int maxSocks = 1000;
	private volatile long sockTTL = 1000 * 60 * 3;
	private volatile long leaseTimeout = 10000;

	private volatile int connectTimeout = 5000;
	private volatile int readTimeout = 10000;

	private static ConcurrentHashMap<String, SockIOPool> pools = new ConcurrentHashMap<String, SockIOPool>();

	@Override
	public SockIO request(String server, Request request) throws Exception {

		SockIO currentSock = null;
		SockIOPool pool = null;
		try {

			if (server != null) {
				pool = _getSockIOPool(server);
				currentSock = pool.lease();
				if (currentSock != null) {
					sendRequest(request, currentSock);
				}
			}

		} catch (Throwable t) {
			logger.error("send request error!", t);
			if (currentSock != null) {
				pool.release(currentSock);
			}
			throw new Exception(t);
		}
		return currentSock;
	}

	private void sendRequest(Request request, SockIO sock) throws IOException {
		ByteBuf head = request.getHead();
		if (head != null) {
			sock.write(head.array());
		}

		ByteBuf body = request.getBody();

		if (body != null) {
			sock.write(body.array());
		}
		sock.flush();
	}

	protected SockIOPool _getSockIOPool(String host) {

		SockIOPool pool = pools.get(host);
		if (pool == null) {
			SockIOFactory sockFactory = new DefaultSockIOFactory(host,
					connectTimeout, readTimeout);
			pool = new DefaultSockIOPool(minSocks, maxSocks, sockTTL,
					leaseTimeout, sockFactory);
			SockIOPool found = pools.putIfAbsent(host, pool);
			if (found != null) {
				pool = found;
			}
		}

		return pool;
	}

	protected void _updatePoolConfig() {
		DefaultSockIOPool p;
		for (SockIOPool pool : pools.values()) {
			p = (DefaultSockIOPool) pool;
			p.setSockTTL(sockTTL);
			p.setLeaseTimeout(leaseTimeout);
			p.setMinSocks(minSocks);
			p.setMaxSocks(maxSocks);
		}

	}

	protected void _updateSocketFactoryConfig() {
		DefaultSockIOPool p;
		DefaultSockIOFactory f;
		SockIO s;
		for (SockIOPool pool : pools.values()) {
			p = (DefaultSockIOPool) pool;
			f = (DefaultSockIOFactory) p.getSockIOFactory();
			f.setConnectTimeout(connectTimeout);
			f.setReadTimeout(readTimeout);
		}
	}

	public void setMaxSocks(int maxSocks) {
		this.maxSocks = Args.positive(maxSocks, "maxSocks");
		_updatePoolConfig();
	}

	public void setMinSocks(int minSocks) {
		this.minSocks = Args.positive(minSocks, "minSocks");
		_updatePoolConfig();
	}

	public long getSockTTL() {
		return sockTTL;
	}

	public void setSockTTL(long sockTTL) {
		this.sockTTL = Args.positive(sockTTL, "sockTTL");
		_updatePoolConfig();
	}

	public long getLeaseTimeout() {
		return leaseTimeout;
	}

	public void setLeaseTimeout(long leaseTimeout) {
		this.leaseTimeout = Args.notNegative(leaseTimeout, "leaseTimeout");
		_updatePoolConfig();
	}

	public int getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = Args.positive(connectTimeout, "connectTimeout");
		_updateSocketFactoryConfig();
	}

	public int getReadTimeout() {
		return readTimeout;
	}

	public void setReadTimeout(int readTimeout) {
		this.readTimeout = Args.positive(readTimeout, "readTimeout");
		_updateSocketFactoryConfig();
	}

	public static ConcurrentHashMap<String, SockIOPool> getPools() {
		return pools;
	}

	public void releaseAllSocks() {
		for (SockIOPool pool : pools.values()) {
			pool.closeAllSockIO();
		}
	}
}
