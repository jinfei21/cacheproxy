package com.ctriposs.cacheproxy.netty;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ctrip.freeway.gen.v2.SpanType;
import com.ctrip.freeway.tracing.ISpan;
import com.ctrip.freeway.tracing.ITrace;
import com.ctrip.freeway.tracing.TraceManager;
import com.ctriposs.cacheproxy.common.MemOriginRequestWrapper;
import com.ctriposs.cacheproxy.common.MemOriginResponseWrapper;
import com.ctriposs.cacheproxy.common.OriginRequest;
import com.ctriposs.cacheproxy.common.OriginResponse;
import com.ctriposs.cacheproxy.common.Request;
import com.ctriposs.cacheproxy.common.Response;
import com.ctriposs.cacheproxy.filter.ProxyException;
import com.ctriposs.cacheproxy.filter.ProxyRunner;
import com.ctriposs.cacheproxy.filter.RequestContext;

public class ProxyCallable implements Callable {

    private static Logger logger = LoggerFactory.getLogger(ProxyCallable.class);

    private Request req;
    private Response res;
    private ProxyRunner proxyRunner;

    public ProxyCallable(Request req, Response res, ProxyRunner proxyRunner) {
        this.req = req;
        this.res = res;
        this.proxyRunner = proxyRunner;
    }

    @Override
    public Object call() throws Exception {
        ITrace tracer = TraceManager.getTracer(ProxyCallable.class);
        tracer.clear();
        ISpan root = tracer.startSpan("request", "CacheProxy", SpanType.URL);

        RequestContext.getCurrentContext().unset();
        RequestContext proxyContext = RequestContext.getCurrentContext();

        try {

            service(req, res);
        } catch (Throwable t) {
            logger.warn("ProxyCallable execute error.", t);
        } finally {
            //Release Resource
            try {
                proxyContext.unset();
            } finally {
                root.stop();
            }
        }
        return null;
    }

    private void service(Request req, Response res) {
        try {

            init(new MemOriginRequestWrapper(req), new MemOriginResponseWrapper(res));

            // marks this request as having passed through the "Gate engine", as opposed to servlets
            // explicitly bound in web.xml, for which requests will not have the same data attached
            RequestContext.getCurrentContext().setEngineRan();

            try {
                preRoute();
            } catch (ProxyException e) {
                error(e);
                postRoute();
                return;
            }
            try {
                route();
            } catch (ProxyException e) {
                error(e);
                postRoute();
                return;
            }
            try {
                postRoute();
            } catch (ProxyException e) {
                error(e);
                return;
            }

        } catch (Throwable e) {
            error(new ProxyException(e, 500, "UNHANDLED_EXCEPTION_" + e.getClass().getName()));
        }
    }

    /**
     * executes "post" GateFilters
     *
     * @throws GateException
     */
    void postRoute() throws ProxyException {
        proxyRunner.postRoute();
    }

    /**
     * executes "route" filters
     *
     * @throws GateException
     */
    void route() throws ProxyException {
        proxyRunner.route();
    }

    /**
     * executes "pre" filters
     *
     * @throws GateException
     */
    void preRoute() throws ProxyException {
        proxyRunner.preRoute();
    }

    /**
     * initializes request
     *
     * @param servletRequest
     * @param servletResponse
     */
    void init(OriginRequest servletRequest, OriginResponse servletResponse) {
        proxyRunner.init(servletRequest, servletResponse);
    }

    /**
     * sets error context info and executes "error" filters
     *
     * @param e
     */
    void error(ProxyException e) {
        RequestContext.getCurrentContext().setThrowable(e);
        proxyRunner.error();
        logger.warn(e.getMessage(), e);
    }
}
