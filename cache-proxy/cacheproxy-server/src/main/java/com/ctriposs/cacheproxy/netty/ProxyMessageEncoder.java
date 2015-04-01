package com.ctriposs.cacheproxy.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import com.ctriposs.cacheproxy.common.ProxyMessage;

@ChannelHandler.Sharable
public class ProxyMessageEncoder extends MessageToByteEncoder<ProxyMessage>{
    @Override
    protected void encode(ChannelHandlerContext ctx, ProxyMessage msg, ByteBuf out) throws Exception {
        msg.encodeAsByteBuf(out);
    }
}
