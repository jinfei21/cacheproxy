package com.ctriposs.cacheproxy;




import org.apache.commons.configuration.AbstractConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ctriposs.cacheproxy.filter.FilterFileManager;
import com.ctriposs.cacheproxy.filter.FilterLoader;
import com.ctriposs.cacheproxy.groovy.GroovyCompiler;
import com.ctriposs.cacheproxy.groovy.GroovyFileFilter;
import com.ctriposs.cacheproxy.netty.NettyServer;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;

public class CacheProxyServer extends AbstractServer{
	
	private static Logger logger = LoggerFactory.getLogger(CacheProxyServer.class);
	
	private NettyServer nettyServer;
	
	public CacheProxyServer() throws Exception {}
		

	@Override
	protected void init() throws Exception {
		initProxy();
		this.nettyServer = new NettyServer();
	}

	@Override
	protected void doStart() throws Exception {
		
		
	}

	@Override
	protected void doClose() throws Exception {
		this.nettyServer.close();
	}
	
    private void initProxy() throws Exception {
    	logger.info("Starting Groovy Filter file manager");

        final DynamicIntProperty checkInterval = DynamicPropertyFactory.getInstance().getIntProperty("proxy.filter.check.interval.seconds", 5);

        AbstractConfiguration config = ConfigurationManager.getConfigInstance();
        String preFiltersPath = config.getString("proxy.filter.pre.path");
        String postFiltersPath = config.getString("proxy.filter.post.path");
        String routeFiltersPath = config.getString("proxy.filter.route.path");
        String customPath = config.getString("proxy.filter.customer.path");

        FilterLoader.getInstance().setCompiler(new GroovyCompiler());
        FilterFileManager.setFilenameFilter(new GroovyFileFilter());
        if (customPath == null) {
            FilterFileManager.init(checkInterval.get(), preFiltersPath, postFiltersPath, routeFiltersPath);
        } else {
            FilterFileManager.init(checkInterval.get(), preFiltersPath, postFiltersPath, routeFiltersPath, customPath);
        }

        checkInterval.addCallback(new Runnable() {
            @Override
            public void run() {
                FilterFileManager.setPollingInervalSeconds(checkInterval.get());
            }
        });

        logger.info("Groovy Filter file manager started");
    }
	
	
}
