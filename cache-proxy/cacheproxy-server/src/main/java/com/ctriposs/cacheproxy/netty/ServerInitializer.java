package com.ctriposs.cacheproxy.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;

import java.util.concurrent.TimeUnit;

import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;

public class ServerInitializer  extends ChannelInitializer<SocketChannel> {
	
	
	private DynamicIntProperty channelReadTimeout = DynamicPropertyFactory.getInstance().getIntProperty("channel.read.timeout", 1000 * 60);
	private DynamicIntProperty channelWriteTimeout = DynamicPropertyFactory.getInstance().getIntProperty("channel.write.timeout", 1000 * 60);

	private ProxyHandler handler;
	
	public ServerInitializer(){
	
		this.handler = new ProxyHandler();
	}
	
	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline p = ch.pipeline();
        //p.addLast(new ReadTimeoutHandler(channelReadTimeout.get(), TimeUnit.MILLISECONDS));
       // p.addLast(new WriteTimeoutHandler(channelWriteTimeout.get(),TimeUnit.MILLISECONDS));
        p.addLast(new MemRequestDecoder());
		p.addLast(new ProxyMessageEncoder());
		p.addLast(handler);
	}
	

}
