package com.ctriposs.cacheproxy.filter;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author:yjfei
 * @date: 2/27/2015.
 */
public class FilterRegistry {

    private static final FilterRegistry INSTANCE = new FilterRegistry();

    public static final FilterRegistry instance() {
        return INSTANCE;
    }

    private final ConcurrentHashMap<String, ProxyFilter> filters = new ConcurrentHashMap<String, ProxyFilter>();

    private FilterRegistry() {
    }

    public ProxyFilter remove(String key) {
        return this.filters.remove(key);
    }

    public ProxyFilter get(String key) {
        return this.filters.get(key);
    }

    public void put(String key, ProxyFilter filter) {
        this.filters.putIfAbsent(key, filter);
    }

    public int size() {
        return this.filters.size();
    }

    public Collection<ProxyFilter> getAllFilters() {
        return this.filters.values();
    }
}
