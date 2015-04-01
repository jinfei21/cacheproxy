package com.ctriposs.cacheproxy.common;

public class ErrorRequest  extends Request{

	public ErrorRequest(){
		setRequestType(RequestType.error);
	}
}
