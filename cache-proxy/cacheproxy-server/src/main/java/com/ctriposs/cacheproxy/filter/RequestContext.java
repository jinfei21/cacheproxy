package com.ctriposs.cacheproxy.filter;

import io.netty.channel.ChannelFuture;

import java.io.NotSerializableException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ctriposs.cacheproxy.common.OriginRequest;
import com.ctriposs.cacheproxy.common.OriginResponse;
import com.ctriposs.cacheproxy.common.SockIO;
import com.ctriposs.cacheproxy.common.Strategy;

/**
 * The Request Context holds request, response,  state information and data for GateFilters to access and share.
 * The RequestContext lives for the duration of the request and is ThreadLocal.
 * extensions of RequestContext can be substituted by setting the contextClass.
 * Most methods here are convenience wrapper methods; the RequestContext is an extension of a ConcurrentHashMap
 *
 * @author:yjfei
 * @date: 2/27/2015.
 */
public class RequestContext extends ConcurrentHashMap<String, Object> {

    private static final Logger logger = LoggerFactory.getLogger(RequestContext.class);

    protected static Class<? extends RequestContext> contextClass = RequestContext.class;

    protected static final ThreadLocal<? extends RequestContext> threadLocal = new ThreadLocal<RequestContext>() {
        @Override
        protected RequestContext initialValue() {
            try {
                return contextClass.newInstance();
            } catch (Throwable e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    };


    public RequestContext() {
        super();

    }

    /**
     * Override the default RequestContext
     *
     * @param clazz
     */
    public static void setContextClass(Class<? extends RequestContext> clazz) {
        contextClass = clazz;
    }

    /**
     * Get the current RequestContext
     *
     * @return the current RequestContext
     */
    public static RequestContext getCurrentContext() {
        RequestContext context = threadLocal.get();
        return context;
    }

    /**
     * Convenience method to return a boolean value for a given key
     *
     * @param key
     * @return true or false depending what was set. default is false
     */
    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    /**
     * Convenience method to return a boolean value for a given key
     *
     * @param key
     * @param defaultResponse
     * @return true or false depending what was set. default defaultResponse
     */
    public boolean getBoolean(String key, boolean defaultResponse) {
        Boolean b = (Boolean) get(key);
        if (b != null) {
            return b.booleanValue();
        }
        return defaultResponse;
    }

    /**
     * sets a key value to Boolen.TRUE
     *
     * @param key
     */
    public void set(String key) {
        put(key, Boolean.TRUE);
    }

    /**
     * puts the key, value into the map. a null value will remove the key from the map
     *
     * @param key
     * @param value
     */
    public void set(String key, Object value) {
        if (value != null) put(key, value);
        else remove(key);
    }

    /**
     * true if  gateEngineRan
     *
     * @return
     */
    public boolean getEngineRan() {
        return getBoolean("proxyEngineRan");
    }

    /**
     * sets gateEngineRan to true
     */
    public void setEngineRan() {
        put("proxyEngineRan", true);
    }

    /**
     * @return the GateRequest from the "request" key
     */
    public OriginRequest getOriginRequest() {
        return (OriginRequest) get("originRequest");
    }

    /**
     * sets the GateRequest into the "request" key
     *
     * @param request
     */
    public void setOriginRequest(OriginRequest request) {
        put("originRequest", request);
    }
    
    

    /**
     * @return the GateResponse from the "response" key
     */
    public OriginResponse getOriginResponse() {
        return (OriginResponse) get("originResponse");
    }

    /**
     * sets the "response" key to the HttpServletResponse passed in
     *
     * @param response
     */
    public void setOriginResponse(OriginResponse response) {
        set("originResponse", response);
    }

    /**
     * returns a set throwable
     *
     * @return a set throwable
     */
    public Throwable getThrowable() {
        return (Throwable) get("throwable");

    }

    /**
     * sets a throwable
     *
     * @param th
     */
    public void setThrowable(Throwable th) {
        put("throwable", th);

    }
    
    public void setSendRequestStatus(Integer status){
    	put("sendRequestStatus", status);
    }
    
    public Integer getSendRequestStatus(){
    	return (Integer)get("sendRequestStatus");
    }
    
    
    public void setSendResponseStatus(Integer status){
    	put("sendResponseStatus", status);
    }
    
    public Integer getSendResponseStatus(){
    	return (Integer)get("sendResponseStatus");
    }
    
    public void setReadResponseStatus(Integer status){
    	put("readResponseStatus", status);
    }
    
    public Integer getReadResponseStatus(){
    	return (Integer)get("readResponseStatus");
    }
    
    public void setRouteStrategy(Strategy strategy){
    	 put("routeStrategy", strategy);
    }
    
    
    public Strategy getRouteStrategy(){
    	return (Strategy)get("routeStrategy");
    }
    
    public void setSockIOList(List<SockIO> sock){
    	 put("sockIO", sock);
    }
    
    public List<SockIO> getSockIOList(){
    	return (List<SockIO>)get("sockIO");
    }
    
    public void setSendRequestCost(Long cost){
    	 put("sendRequestCost", cost);
    }
    
    public Long getSendRequestCost(){
    	return (Long)get("sendRequestCost");
    }
    
    public void setSendResponseStart(Long start){
   	 	put("sendResponseStart", start);
    }
   
    public Long getSendResponseStart(){
   	 	return (Long)get("sendResponseStart");
    }
    
    public void setReadResponseCost(Long cost){
   	 	put("readResponseCost", cost);
    }
   
    public Long getReadResponseCost(){
   	 	return (Long)get("readResponseCost");
    }
   
    public void setWriteResponseFuture(ChannelFuture future){
    	put("writeResponseFuture", future);
    }
    
    public ChannelFuture getWriteResponseFuture(){
    	Object obj = get("writeResponseFuture");
    	if(obj==null){
    		return null;
    	}else{
    		return (ChannelFuture)obj;
    	}
    }
    
    public void setCloseResponseFuture(ChannelFuture future){
    	put("closeResponseFuture", future);
    }
    
    public ChannelFuture getCloseResponseFuture(){
    	Object obj = get("closeResponseFuture");
    	if(obj==null){
    		return null;
    	}else{
    		return (ChannelFuture)obj;
    	}
    }
    
    
    /**
     * sets  debugRouting
     *
     * @param bDebug
     */
    public void setDebugRouting(boolean bDebug) {
        set("debugRouting", bDebug);
    }

    /**
     * @return "debugRouting"
     */
    public boolean debugRouting() {
        return getBoolean("debugRouting");
    }
  
    
    /**
     * add a header to be sent to the origin
     *
     * @param name
     * @param value
     */
    public void addProxyRequestHeader(String name, String value) {
        getProxyRequestHeaders().put(name.toLowerCase(), value);
    }

    /**
     * return the list of requestHeaders to be sent to the origin
     *
     * @return the list of requestHeaders to be sent to the origin
     */
    public Map<String, String> getProxyRequestHeaders() {
        if (get("proxyRequestHeaders") == null) {
            HashMap<String, String> gateRequestHeaders = new HashMap<String, String>();
            putIfAbsent("proxyRequestHeaders", gateRequestHeaders);
        }
        return (Map<String, String>) get("proxyRequestHeaders");
    }
    
    /**
     * appends filter name and status to the filter execution history for the
     * current request
     *
     * @param name
     * @param status
     * @param time
     */
    public void addFilterExecutionSummary(String name, String status, long time) {
        StringBuilder sb = getFilterExecutionSummary();
        if (sb.length() > 0) sb.append(", ");
        sb.append(name).append('[').append(status).append(']').append('[').append(time).append("ms]");
    }

    /**
     * @return String that represents the filter execution history for the current request
     */
    public StringBuilder getFilterExecutionSummary() {
        if (get("executedFilters") == null) {
            putIfAbsent("executedFilters", new StringBuilder());
        }
        return (StringBuilder) get("executedFilters");
    }



    /**
     * unsets the threadLocal context. Done at the end of the request.
     */
    public void unset() {
        threadLocal.remove();
    }

    /**
     * Makes a copy of the RequestContext. This is used for debugging.
     *
     * @return
     */
    public RequestContext copy() {
        RequestContext copy = new RequestContext();
        Iterator<String> it = keySet().iterator();
        String key = it.next();
        while (key != null) {
            Object orig = get(key);
            try {
                Object copyValue = DeepCopy.copy(orig);
                if (copyValue != null) {
                    copy.set(key, copyValue);
                } else {
                    copy.set(key, orig);
                }
            } catch (NotSerializableException e) {
                copy.set(key, orig);
            }
            if (it.hasNext()) {
                key = it.next();
            } else {
                key = null;
            }
        }
        return copy;
    }


}
