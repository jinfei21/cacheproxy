package com.ctriposs.cacheproxy.filter;

/**
 * All handled exceptions in GateKeeper are GateExceptions
 *
 * @author:xingchaowang
 * @date: 8/15/2014.
 */
public class ProxyException extends Exception {
    public int nStatusCode;
    public String errorCause;

    /**
     * Source Throwable, message, status code and info about the cause
     * @param throwable
     * @param sMessage
     * @param nStatusCode
     * @param errorCause
     */
    public ProxyException(Throwable throwable, String sMessage, int nStatusCode, String errorCause) {
        super(sMessage, throwable);
        this.nStatusCode = nStatusCode;
        this.errorCause = errorCause;
        incrementCounter("GATE::EXCEPTION:" + errorCause + ":" + nStatusCode);
    }

    /**
     * error message, status code and info about the cause
     * @param sMessage
     * @param nStatusCode
     * @param errorCause
     */
    public ProxyException(String sMessage, int nStatusCode, String errorCause) {
        super(sMessage);
        this.nStatusCode = nStatusCode;
        this.errorCause = errorCause;
        incrementCounter("GATE::EXCEPTION:" + errorCause + ":" + nStatusCode);

    }

    /**
     * Source Throwable,  status code and info about the cause
     * @param throwable
     * @param nStatusCode
     * @param errorCause
     */
    public ProxyException(Throwable throwable, int nStatusCode, String errorCause) {
        super(throwable.getMessage(), throwable);
        this.nStatusCode = nStatusCode;
        this.errorCause = errorCause;
        incrementCounter("GATE::EXCEPTION:" + errorCause + ":" + nStatusCode);

    }

    private static final void incrementCounter(String name) {
        //ToDo:
    }
}
