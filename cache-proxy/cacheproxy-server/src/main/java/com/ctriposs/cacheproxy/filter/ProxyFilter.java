package com.ctriposs.cacheproxy.filter;


import com.ctriposs.cacheproxy.common.ExecutionStatus;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicPropertyFactory;

/**
 * @author:yjfei
 * @date: 2/27/2015.
 */
public abstract class ProxyFilter implements Comparable<ProxyFilter>{

    private final DynamicBooleanProperty filterDisabled =
            DynamicPropertyFactory.getInstance().getBooleanProperty(disablePropertyName(), false);

    /**
     * to classify a filter by type. Standard types in GateKeeper are "pre" for pre-routing filtering,
     * "route" for routing to an origin, "post" for post-routing filters, "error" for error handling.
     * We also support a "static" type for static responses see  StaticResponseFilter.
     * Any filterType made be created or added and run by calling FilterProcessor.runFilters(type)
     *
     * @return A String representing that type
     */
    abstract public String filterType();

    /**
     * filterOrder() must also be defined for a filter. Filters may have the same  filterOrder if precedence is not
     * important for a filter. filterOrders do not need to be sequential.
     *
     * @return the int order of a filter
     */
    abstract public int filterOrder();

    /**
     * a "true" return from this method means that the run() method should be invoked
     *
     * @return true if the run() method should be invoked. false will not invoke the run() method
     */
    abstract public boolean shouldFilter();

    /**
     * if shouldFilter() is true, this method will be invoked. this method is the core method of a GateFilter
     *
     * @return Some arbitrary artifact may be returned. Current implementation ignores it.
     */
    abstract public Object run();

    /**
     * By default GateFilters are static; they don't carry state. This may be overridden by overriding the isStaticFilter() property to false
     *
     * @return true by default
     */
    public boolean isStaticFilter() {
        return true;
    }

    /**
     * The name of the Archaius property to disable this filter. by default it is gate.[classname].[filtertype].disable
     *
     * @return
     */
    public String disablePropertyName() {
        return "proxy." + this.getClass().getSimpleName() + "." + filterType() + ".disable";
    }

    /**
     * If true, the filter has been disabled by archaius and will not be run
     *
     * @return
     */
    public boolean isFilterDisabled() {
        return filterDisabled.get();
    }

    /**
     * runFilter checks !isFilterDisabled() and shouldFilter(). The run() method is invoked if both are true.
     *
     * @return the return from GateFilterResult
     */
    public FilterResult runFilter() {
        FilterResult tr = new FilterResult();
        if (!filterDisabled.get()) {
            if (shouldFilter()) {
                try {
                    Object res = run();
                    tr = new FilterResult(res, ExecutionStatus.SUCCESS);
                } catch (Throwable e) {
                    tr = new FilterResult(ExecutionStatus.FAILED);
                    tr.setException(e);
                } finally {
                }
            } else {
                tr = new FilterResult(ExecutionStatus.SKIPPED);
            }
        }
        return tr;
    }


    public int compareTo(ProxyFilter filter) {
        return this.filterOrder() - filter.filterOrder();
    }

}
