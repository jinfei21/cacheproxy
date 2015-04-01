package scripts.route

import io.netty.util.CharsetUtil

import java.lang.ref.WeakReference

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.ctrip.freeway.logging.ILog
import com.ctrip.freeway.logging.LogManager
import com.ctrip.freeway.metrics.IMetric
import com.ctrip.freeway.metrics.MetricManager
import com.ctriposs.cacheproxy.common.Group
import com.ctriposs.cacheproxy.common.PoolStats
import com.ctriposs.cacheproxy.common.Request
import com.ctriposs.cacheproxy.common.SockClient
import com.ctriposs.cacheproxy.common.SockIO
import com.ctriposs.cacheproxy.common.SockIOPool
import com.ctriposs.cacheproxy.common.Strategy
import com.ctriposs.cacheproxy.filter.ProxyFilter
import com.ctriposs.cacheproxy.filter.RequestContext
import com.ctriposs.cacheproxy.hystrix.BaseSemaphoreIsolationCommand
import com.ctriposs.cacheproxy.hystrix.BaseThreadIsolationCommand
import com.ctriposs.cacheproxy.stat.Globals
import com.ctriposs.cacheproxy.stat.StatsKey
import com.ctriposs.cacheproxy.store.DefaultSockClient
import com.netflix.config.DynamicBooleanProperty
import com.netflix.config.DynamicIntProperty
import com.netflix.config.DynamicLongProperty
import com.netflix.config.DynamicPropertyFactory

class SendRequest extends ProxyFilter {

	ILog clog = LogManager.getLogger(SendRequest.class)
	Logger logger = LoggerFactory.getLogger(SendRequest.class)
	
	DynamicIntProperty minSocks = DynamicPropertyFactory.getInstance().getIntProperty("proxy.client.min.socks", 20);
	DynamicIntProperty maxSocks = DynamicPropertyFactory.getInstance().getIntProperty("proxy.client.max.socks", 2000);
	DynamicIntProperty sockTTL = DynamicPropertyFactory.getInstance().getIntProperty("proxy.client.socks.ttl", 1000*60*2);

	DynamicIntProperty releaseTimeout = DynamicPropertyFactory.getInstance().getIntProperty("proxy.client.release.timeout", 1000);
	DynamicIntProperty connectTimeout = DynamicPropertyFactory.getInstance().getIntProperty("proxy.client.connect.timeout", 1000);
	DynamicIntProperty readTimeout = DynamicPropertyFactory.getInstance().getIntProperty("proxy.client.read.timeout", 30000);

	public volatile long lastUsed = 0;

	final DefaultSockClient sockClient;
	final IMetric metric = MetricManager.getMetricer()
	
	SendRequest(){
		sockClient = new DefaultSockClient()
		configClient()
		new AWorker(this).start()
	}
	
	public void configClient() {
		logger.info("Start update config task.")

		sockClient.setMinSocks(minSocks.get())
		sockClient.setMaxSocks(maxSocks.get())
		sockClient.setSockTTL(sockTTL.get())
		sockClient.setLeaseTimeout(releaseTimeout.get())
		sockClient.setConnectTimeout(connectTimeout.get())
		sockClient.setReadTimeout(readTimeout.get())
		logger.info("End update config task.")
	}

	public void sendMetrics(){
		try {
			logger.info("Start metrics task.")
			Date date = new Date()
			for (Map.Entry<String, SockIOPool> entry : sockClient.getPools().entrySet()) {
				String k = entry.getKey()
				PoolStats s = entry.getValue().poolStats()
				Map<String,String> tags = new HashMap<>()
				tags.put("target", k)
				tags.put("type", "free")
				metric.log("cacheproxy.client.socks",Long.valueOf(s.getFree()),tags, date)
				tags = new HashMap<>()
				tags.put("target", k)
				tags.put("type", "leased")
				metric.log("cacheproxy.client.socks", Long.valueOf(s.getLeased()), tags, date)
				tags.put("type", "min")
				metric.log("cacheproxy.client.socks",Long.valueOf(s.getMin()),tags, date)
				tags.put("type", "max")
				metric.log("cacheproxy.client.socks",Long.valueOf(s.getMax()),tags, date)
			}
		} catch (Exception e) {
			logger.error("Report cacheproxy socks metrics error", e)
		}
		logger.info("End metrics task.")
	}

	@Override
	String filterType() {
		return "route"
	}

	@Override
	int filterOrder() {
		return 20
	}

	@Override
	boolean shouldFilter() {
		lastUsed = System.currentTimeMillis()
		return RequestContext.getCurrentContext().getRouteStrategy()!=null
	}

	@Override
	public Object run() {
		RequestContext ctx = RequestContext.getCurrentContext();
		Request req = ctx.getOriginRequest().origin;
		String bucketid = String.valueOf(req.getLocalPort())
		Strategy strategy = ctx.getRouteStrategy()

		String isolationStrategy = DynamicPropertyFactory.instance.getStringProperty(bucketid + ".isolation.strategy", null).get();

		if (isolationStrategy == null) {
			isolationStrategy = DynamicPropertyFactory.instance.getStringProperty("proxy.isolation.strategy.global", "SEMAPHORE").get();
		}
		
		String key = req.getKey() != null ?req.getKey():req.getHead().toString(CharsetUtil.UTF_8);
		Group group = strategy.getGroup(key);
		List<SockIO> sockList = new ArrayList<String>();
		long sendStart = System.currentTimeMillis()
		if(group != null){
			SockIO sock = null;
			req.attr("groupid",group.getGroupID());
			for(String server:group.getServers()){
				long serverStart = System.currentTimeMillis();
				int status = 0
				try {
					if ("THREAD".equalsIgnoreCase(isolationStrategy)) {
						sock = new ThreadIsolationCommand(sockClient, server, req, bucketid, bucketid).execute();
					} else {
						sock = new SemaphoreIsolationCommand(sockClient, server, req, bucketid, bucketid).execute();
					}
					sockList.add(sock);
					ctx.setSendRequestStatus(0);
				} catch (Exception e) {
					status = 1;
					ctx.setSendRequestStatus(1);
					logger.error(bucketid + "send request error!", e);
					
				} finally {
					ctx.setSendRequestCost(System.currentTimeMillis() - serverStart);
					
					Globals.REQ_STATS_COLLECTOR.add(
						new StatsKey("cacheproxy.sendmem")
								.addTag("bucketid", bucketid)
								.addTag("memnode", server)
								.reportSize(false),
						System.currentTimeMillis() - serverStart,
						0,
						status);
				}				
		    }
		}
		ctx.setSockIOList(sockList)
		ctx.setSendRequestCost(System.currentTimeMillis() - sendStart)
		return null;
	}

    @Override
    protected void finalize() throws Throwable {
        logger.warn("Finalize before GC.")

        logger.warn("Release all socks while finalizing.")
        sockClient.releaseAllSocks()

        logger.warn("Has finalized before GC.")
    }


	/**
	 * For thread isolation
	 */
	class ThreadIsolationCommand extends BaseThreadIsolationCommand<SockIO> {
		static Logger logger = LoggerFactory.getLogger(ThreadIsolationCommand.class)

		private SockClient sockClient
		private Request request
		private String server

		public ThreadIsolationCommand(SockClient sockClient, String server, Request request, String commandGroup, String commandKey) {
			super(commandGroup, commandKey, generateThreadPoolKey(commandGroup, commandKey))

			this.sockClient = sockClient
			this.request = request
			this.server = server
		}

		String generateThreadPoolKey(String commandGroup, String commandKey) {
			String poolKey = DynamicPropertyFactory.getInstance().getStringProperty(commandKey + ".thread-pool.key", null).get();
			if (poolKey == null) {
				poolKey = DynamicPropertyFactory.getInstance().getStringProperty(commandGroup + ".thread-pool.key", commandGroup).get();
			}
			return poolKey;
		}

		@Override
		protected SockIO run() throws Exception {
			try {
				return sockClient.request(server, request)
			} catch (Exception e) {
				//Try again if connection reset or broken pipe
				String message = e.getMessage()
				if ("Connection reset".equals(message)
				|| "Connection is closed".equals(message)
				|| "Broken pipe".equals(message)) {
					logger.warn("There is a socket exception, would retry.", e)
					return sockClient.request(server, request)
				}
				throw e
			}
		}
	}

	/**
	 * For semaphore isolation
	 */
	class SemaphoreIsolationCommand extends BaseSemaphoreIsolationCommand<SockIO> {
		static Logger logger = LoggerFactory.getLogger(SemaphoreIsolationCommand.class)

		private SockClient sockClient
		private Request request
		private String server;

		public SemaphoreIsolationCommand(SockClient sockClient, String server, Request request, String commandGroup, String commandKey) {
			super(commandGroup, commandKey)

			this.sockClient = sockClient
			this.request = request
			this.server = server;
		}

		@Override
		protected SockIO run() throws Exception {
			try {
				return sockClient.request(server, request)
			} catch (Exception e) {
				//Try again if connection reset or broken pipe
				String message = e.getMessage()
				if ("Connection reset".equals(message)
				|| "Connection is closed".equals(message)
				|| "Broken pipe".equals(message)) {
					logger.warn("There is a socket exception, would retry.", e)
					return sockClient.request(server, request)
				}
				throw e
			}
		}
	}

	class AWorker extends Thread {
		private ILog logger = LogManager.getLogger(this.getClass())

		private DynamicBooleanProperty enabled = DynamicPropertyFactory.getInstance().getBooleanProperty("proxy.metrics.enabled", true);
		private DynamicLongProperty interval = DynamicPropertyFactory.getInstance().getLongProperty("proxy.metrics.enabled.interval", 60000);

		private WeakReference<SendRequest> masterReference

		AWorker(SendRequest master) {
			masterReference = new WeakReference<>(master)
			setName(AWorker.class.getSimpleName() + "-For-" + master.getClass().getSimpleName())
			setDaemon(true)
		}

		@Override
		void run() {
			while (masterReference.get() != null) {
				try {
					if (!enabled.get()) continue

						if (masterReference.get().lastUsed + interval.get() > System.currentTimeMillis()) {
							masterReference.get().configClient()
							masterReference.get().sendMetrics()
						}
				} catch (Exception e) {
					logger.error("Encounter an error as working.", e)
				} finally {
					try {
						sleep(interval.get())
					} catch (Exception e) {
						logger.warn("Encounter an error while working.", e)
					}
				}
			}
		}
	}
}
