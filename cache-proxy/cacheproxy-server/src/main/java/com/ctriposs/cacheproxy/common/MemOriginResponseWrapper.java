package com.ctriposs.cacheproxy.common;

import com.ctriposs.cacheproxy.util.Args;

public class MemOriginResponseWrapper implements OriginResponse<Response> {
    private final Response response;

    public MemOriginResponseWrapper(Response res) {
        this.response = Args.notNull(res, "Origin Response");
    }

    @Override
    public Response getOrigin() {
        return response;
    }

    @Override
    public void setStatus(int nStatusCode) {

    }
}
