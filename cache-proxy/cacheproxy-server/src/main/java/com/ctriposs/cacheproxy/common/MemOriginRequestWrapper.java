package com.ctriposs.cacheproxy.common;

import com.ctriposs.cacheproxy.util.Args;

public class MemOriginRequestWrapper implements OriginRequest<Request> {
    private final Request request;

    public MemOriginRequestWrapper(Request req) {
        this.request = Args.notNull(req, "Origin Request");
    }

    @Override
    public Request getOrigin() {
        return request;
    }
}
