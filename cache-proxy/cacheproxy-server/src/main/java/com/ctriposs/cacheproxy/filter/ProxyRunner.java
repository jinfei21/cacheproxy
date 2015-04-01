package com.ctriposs.cacheproxy.filter;

import com.ctriposs.cacheproxy.common.OriginRequest;
import com.ctriposs.cacheproxy.common.OriginResponse;

/**
 * This class initializes servlet requests and responses into the RequestContext and wraps the FilterProcessor calls
 * to preRoute(), route(),  postRoute(), and error() methods
 *
 * @author:yjfei
 * @date: 2/26/2015.
 */
public class ProxyRunner {
    /**
     * Creates a new <code>GateRunner</code> instance.
     */
    public ProxyRunner() {
    }

    /**
     * sets GateRequest request and GateResponse
     *
     * @param request
     * @param response
     */
    public void init(OriginRequest request, OriginResponse response) {
        RequestContext.getCurrentContext().setOriginRequest(request);
        RequestContext.getCurrentContext().setOriginResponse(response);
    }

    /**
     * executes "post" filterType  GateFilters
     *
     * @throws com.ctriposs.gatekeeper.tcp.gate.context.GateException
     */
    public void postRoute() throws ProxyException {
        FilterProcessor.getInstance().postRoute();
    }

    /**
     * executes "route" filterType  GateFilters
     *
     * @throws GateException
     */
    public void route() throws ProxyException {
        FilterProcessor.getInstance().route();
    }

    /**
     * executes "pre" filterType  GateFilters
     *
     * @throws GateException
     */
    public void preRoute() throws ProxyException {
        FilterProcessor.getInstance().preRoute();
    }

    /**
     * executes "error" filterType  GateFilters
     */
    public void error() {
        FilterProcessor.getInstance().error();
    }
}

