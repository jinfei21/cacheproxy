package com.ctriposs.cacheproxy.common;



public interface OriginResponse<T> {
    T getOrigin();
    void setStatus(int nStatusCode);
}
