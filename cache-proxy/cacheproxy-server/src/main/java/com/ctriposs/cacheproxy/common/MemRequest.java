package com.ctriposs.cacheproxy.common;

public class MemRequest extends Request{

	public MemRequest(){
		setRequestType(RequestType.memcache);
	}
}
