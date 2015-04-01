package com.ctriposs.cacheproxy.common;

public class QuitRequest extends Request{

	public QuitRequest(){
		setRequestType(RequestType.error);
	}
}
