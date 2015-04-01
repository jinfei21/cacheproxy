package com.ctriposs.cacheproxy.common;


public interface Strategy {
	Group getGroup(String key);
}
