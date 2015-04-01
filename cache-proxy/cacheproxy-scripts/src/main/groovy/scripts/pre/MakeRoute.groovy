package scripts.pre

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.ctriposs.cacheproxy.common.Cluster
import com.ctriposs.cacheproxy.common.Group
import com.ctriposs.cacheproxy.common.Request
import com.ctriposs.cacheproxy.common.Strategy
import com.ctriposs.cacheproxy.filter.ProxyFilter
import com.ctriposs.cacheproxy.filter.RequestContext
import com.ctriposs.cacheproxy.store.KetamaHashStrategy
import com.netflix.config.DynamicPropertyFactory
import com.netflix.config.DynamicStringProperty


class MakeRoute extends ProxyFilter {
	private Logger logger = LoggerFactory.getLogger(MakeRoute.class);
	private DynamicStringProperty routeChange = DynamicPropertyFactory.getInstance().getStringProperty("proxy.route.update", "none");
	
	private DynamicStringProperty bucketSet = DynamicPropertyFactory.getInstance().getStringProperty("proxy.route.bucketids", "none");
	
	private static ConcurrentMap<String, Strategy> strategyCache = new ConcurrentHashMap<String, Strategy>();
	
	MakeRoute(){
		
		
		Runnable c2 = new Runnable() {
			
			@Override
			public void run() {
				String[] bids = bucketSet.get().split(",");
				
				for(String bid:bids){
					if(!"".equals(bid.trim())){
						strategyCache.put(bid, MakeRoute.getRemoteStrategy(bid.trim()));
					}	
				}
				
			}
		};
	
		routeChange.addCallback(c2);
		init();
	}
	
	private void init(){
		Cluster cluster = new Cluster();
		cluster.addGroup(new Group("5b85cadd-1ca5-45be-989c-06682675d7ab","10.2.6.94:11211"));
		cluster.addGroup(new Group("54522cf4-c037-479b-a4cf-09a61938e536","10.2.6.95:11211"));
		cluster.addGroup(new Group("cc2f324a-76aa-4388-bfb4-20e15fb9d35f","10.2.6.237:11211"));
		cluster.addGroup(new Group("71c1f9a2-b6c8-40a6-8b9f-34f7d6dd58a1","10.2.6.239:11211"));
		Strategy strategy = new KetamaHashStrategy(cluster);
		strategyCache.put("11211", strategy);
	}
	
	
	public static Strategy getStrategy(String bid){
		Strategy strategy = strategyCache.get(bid);
		
		if(strategy == null){	
			strategy = getRemoteStrategy(bid);
		}
		
		return strategy;
	} 
	
	public static Strategy getRemoteStrategy(String bid){
		Strategy strategy = null;
		Cluster cluster = new Cluster();
		String clusterStr = DynamicPropertyFactory.getInstance().getStringProperty("proxy."+bid+".cluster", "test").get();
		
		if(clusterStr != null){
			String[] groups = clusterStr.split(",")
			for(String gid:groups){
				if(!"".equals(gid)){
					String group = DynamicPropertyFactory.getInstance().getStringProperty("proxy."+gid+".group", "10.2.6.94:11211").get();
					if(group != null){
						cluster.addGroup(new Group(gid,group))
					}
				}
			}
			strategy = new KetamaHashStrategy(cluster);
		}
		
		return strategy;
	}
	
	@Override
	String filterType() {
		return "pre";
	}

	@Override
	int filterOrder() {
		return 20;
	}

	@Override
	boolean shouldFilter() {

		return true;
	}

	@Override
	public Object run() {

		RequestContext ctx = RequestContext.getCurrentContext();
		Request req = ctx.getOriginRequest().origin;
		String bucketid = String.valueOf(req.getLocalPort());
		Strategy strategy = getStrategy(bucketid)
		if(strategy != null){
			ctx.setRouteStrategy(strategy);
		}else{
			logger.error(bucketid + " routetable is null!");
		}
		return null;
	}

	
}
