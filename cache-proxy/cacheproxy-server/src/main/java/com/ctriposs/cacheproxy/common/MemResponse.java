package com.ctriposs.cacheproxy.common;

public class MemResponse extends Response{

	public MemResponse(){
		setResponseType(ResponseType.memcache);
	}
}
