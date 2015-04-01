package com.ctriposs.cacheproxy.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;

public class NettyServer {

	private static Logger logger = LoggerFactory.getLogger(NettyServer.class);

	private final NioEventLoopGroup bossGroup;
	private final NioEventLoopGroup workerGroup;

	private List<Channel> channels = new CopyOnWriteArrayList<Channel>();
	private ServerInitializer serverInitializer;
	private DynamicStringProperty port = DynamicPropertyFactory.getInstance().getStringProperty("server.port", "8080");

	public NettyServer() throws InterruptedException {

		this.bossGroup = new NioEventLoopGroup(1);
		this.workerGroup = new NioEventLoopGroup();
		this.serverInitializer = new ServerInitializer();

		String[] ports = port.get().split(",");
		for (String s : ports) {

			int port = 0;
			try {
				port = Integer.parseInt(s.trim());
			} catch (Exception ignore) {
			}
			if (port <= 0 || port > 65535) {
				logger.error("The server port [{}] is illegal.", s);
				continue;
			}
			ServerBootstrap b = new ServerBootstrap();
			b.childOption(ChannelOption.SO_KEEPALIVE, true);
			b.option(ChannelOption.SO_KEEPALIVE, true);
			b.group(bossGroup, workerGroup)
					.channel(NioServerSocketChannel.class)
					.handler(new LoggingHandler(LogLevel.INFO))
					.childHandler(serverInitializer);
			channels.add(b.bind(port).sync().channel());
		}
	}

	public void close() throws InterruptedException {
		try {

			for (Channel channel : channels) {
				channel.close().sync();
			}
		} finally {
			this.bossGroup.shutdownGracefully();
			this.workerGroup.shutdownGracefully();
		}
	}

}
