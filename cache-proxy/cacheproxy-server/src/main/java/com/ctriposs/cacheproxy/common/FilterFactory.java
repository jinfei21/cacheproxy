package com.ctriposs.cacheproxy.common;

import com.ctriposs.cacheproxy.filter.ProxyFilter;



public interface FilterFactory {
    public ProxyFilter newInstance(Class clazz) throws Exception;
}
