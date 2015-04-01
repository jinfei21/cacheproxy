package com.ctriposs.cacheproxy.filter;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ctriposs.cacheproxy.common.ExecutionStatus;

/**
 * This the the core class to execute filters.
 *
 * @author:yjfei
 * @date: 2/27/2015.
 */
public class FilterProcessor {
    static FilterProcessor INSTANCE = new FilterProcessor();
    protected static final Logger LOG = LoggerFactory.getLogger(FilterProcessor.class);

    /**
     * @return the singleton FilterProcessor
     */
    public static FilterProcessor getInstance() {
        return INSTANCE;
    }

    /**
     * sets a singleton processor in case of a need to override default behavior
     *
     * @param processor
     */
    public static void setProcessor(FilterProcessor processor) {
        INSTANCE = processor;
    }

    private FilterProcessor() {
    }

    /**
     * runs "post" filters which are called after "route" filters. GateExceptions from GateFilters are thrown.
     * Any other Throwables are caught and a GateException is thrown out with a 500 status code
     *
     * @throws com.ctriposs.ProxyException.tcp.gate.context.GateException
     */
    public void postRoute() throws ProxyException {
        try {
            runFilters("post");
        } catch (Throwable e) {
            if (e instanceof ProxyException) {
                throw (ProxyException) e;
            }
            throw new ProxyException(e, 500, "UNCAUGHT_EXCEPTION_IN_POST_FILTER_" + e.getClass().getName());
        }

    }

    /**
     * runs all "error" filters. These are called only if an exception occurs. Exceptions from this are swallowed and logged so as not to bubble up.
     */
    public void error() {
        try {
            runFilters("error");
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * Runs all "route" filters. These filters route calls to an origin.
     *
     * @throws ProxyException if an exception occurs.
     */
    public void route() throws ProxyException {
        try {
            runFilters("route");
        } catch (Throwable e) {
            if (e instanceof ProxyException) {
                throw (ProxyException) e;
            }
            throw new ProxyException(e, 500, "UNCAUGHT_EXCEPTION_IN_ROUTE_FILTER_" + e.getClass().getName());
        }
    }

    /**
     * runs all "pre" filters. These filters are run before routing to the orgin.
     *
     * @throws ProxyException
     */
    public void preRoute() throws ProxyException {
        try {
            runFilters("pre");
        } catch (Throwable e) {
            if (e instanceof ProxyException) {
                throw (ProxyException) e;
            }
            throw new ProxyException(e, 500, "UNCAUGHT_EXCEPTION_IN_PRE_FILTER_" + e.getClass().getName());
        }
    }

    /**
     * runs all filters of the filterType sType/ Use this method within filters to run custom filters by type
     *
     * @param sType the filterType.
     * @return
     * @throws Throwable throws up an arbitrary exception
     */
    public Object runFilters(String sType) throws Throwable {
        if (RequestContext.getCurrentContext().debugRouting()) {
            Debug.addRoutingDebug("Invoking {" + sType + "} type filters");
        }
        boolean bResult = false;
        List<ProxyFilter> list = FilterLoader.getInstance().getFiltersByType(sType);
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
            	ProxyFilter gateFilter = list.get(i);
                Object result = processFilter(gateFilter);
                if (result != null && result instanceof Boolean) {
                    bResult |= ((Boolean) result);
                }
            }
        }
        return bResult;
    }

    /**
     * Processes an individual GateFilter. This method adds Debug information. Any uncaught Thowables are caught by this method and converted to a GateException with a 500 status code.
     *
     * @param filter
     * @return the return value for that filter
     * @throws ProxyException
     */
    public Object processFilter(ProxyFilter filter) throws ProxyException {

        RequestContext ctx = RequestContext.getCurrentContext();
        boolean bDebug = ctx.debugRouting();
        long execTime = 0;
        String filterName = "";
        try {
            long ltime = System.currentTimeMillis();
            filterName = filter.getClass().getSimpleName();

            RequestContext copy = null;
            Object o = null;
            Throwable t = null;

            if (bDebug) {
                Debug.addRoutingDebug("Filter " + filter.filterType() + " " + filter.filterOrder() + " " + filterName);
                copy = ctx.copy();
            }

            FilterResult result = filter.runFilter();
            ExecutionStatus s = result.getStatus();
            execTime = System.currentTimeMillis() - ltime;

            switch (s) {
                case FAILED:
                    t = result.getException();
                    ctx.addFilterExecutionSummary(filterName, ExecutionStatus.FAILED.name(), execTime);
                    break;
                case SUCCESS:
                    o = result.getResult();
                    ctx.addFilterExecutionSummary(filterName, ExecutionStatus.SUCCESS.name(), execTime);
                    if (bDebug) {
                        Debug.addRoutingDebug("Filter {" + filterName + " TYPE:" + filter.filterType() + " ORDER:" + filter.filterOrder() + "} Execution time = " + execTime + "ms");
                        Debug.compareContextState(filterName, copy);
                    }
                    break;
                default:
                    break;
            }

            if (t != null) throw t;
            return o;
        } catch (Throwable e) {
            if (bDebug) {
                Debug.addRoutingDebug("Running Filter failed " + filterName + " type:" + filter.filterType() + " order:" + filter.filterOrder() + " " + e.getMessage());
            }

            if (e instanceof ProxyException) {
                throw (ProxyException) e;
            } else {
                ProxyException ex = new ProxyException(e, "Filter threw Exception", 500, filter.filterType() + ":" + filterName);
                ctx.addFilterExecutionSummary(filterName, ExecutionStatus.FAILED.name(), execTime);
                throw ex;
            }
        }
    }
}
