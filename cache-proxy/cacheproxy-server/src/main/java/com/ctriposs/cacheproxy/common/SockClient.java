package com.ctriposs.cacheproxy.common;




public interface SockClient {

    SockIO request(String server, Request request) throws Exception;
   
}
