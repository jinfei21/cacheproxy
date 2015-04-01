package com.ctriposs.cacheproxy.common;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;

public class Response extends ProxyMessage {

    private ResponseType responseType;
    private ChannelHandlerContext channelHandlerContext;

    public ResponseType getResponseType() {
        return responseType;
    }

    public void setResponseType(ResponseType responseType) {
        this.responseType = responseType;
    }

    public ChannelHandlerContext getChannelHandlerContext() {
        return channelHandlerContext;
    }

    public void setChannelHandlerContext(ChannelHandlerContext channelHandlerContext) {
        this.channelHandlerContext = channelHandlerContext;
    }

    public ChannelFuture close() {
        return channelHandlerContext.close();
    }
}

