package com.ctriposs.cacheproxy.common;




public interface SockIOPool {

	SockIO lease() throws Exception;

	SockIO lease(long timeout) throws Exception;

    void release(SockIO sock);

    void closeAllSockIO();

    PoolStats poolStats();
	
}
