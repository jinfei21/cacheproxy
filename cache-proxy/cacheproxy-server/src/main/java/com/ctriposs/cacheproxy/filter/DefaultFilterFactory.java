package com.ctriposs.cacheproxy.filter;

import com.ctriposs.cacheproxy.common.FilterFactory;



public class DefaultFilterFactory implements FilterFactory {
    @Override
    public ProxyFilter newInstance(Class clazz) throws Exception {
        return (ProxyFilter) clazz.newInstance();
    }
}
