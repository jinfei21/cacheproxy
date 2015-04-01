package com.ctriposs.cacheproxy.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ctrip.freeway.logging.ILog;
import com.ctrip.freeway.logging.LogManager;
import com.ctrip.freeway.metrics.IMetric;
import com.ctrip.freeway.metrics.MetricManager;
import com.ctriposs.cacheproxy.common.MemResponse;
import com.ctriposs.cacheproxy.common.Request;
import com.ctriposs.cacheproxy.common.Response;
import com.ctriposs.cacheproxy.filter.ProxyRunner;
import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;


@ChannelHandler.Sharable
public class ProxyHandler extends SimpleChannelInboundHandler<Request> {
    private static DynamicBooleanProperty CLOG_ENABLED = DynamicPropertyFactory.getInstance().getBooleanProperty("clog.enabled" + ProxyHandler.class.getSimpleName(), true);
    private static Logger logger = LoggerFactory.getLogger(ProxyHandler.class);
    
    private DynamicIntProperty poolCoreSize = DynamicPropertyFactory.getInstance().getIntProperty("server.thread-pool.core-size", 200);
    private DynamicIntProperty poolMaximumSize = DynamicPropertyFactory.getInstance().getIntProperty("server.thread-pool.maximum-size", 2000);
    private DynamicLongProperty poolAliveTime = DynamicPropertyFactory.getInstance().getLongProperty("server.thread-pool.alive-time", 1000 * 60 * 5);
    private DynamicLongProperty reportInterval = DynamicPropertyFactory.getInstance().getLongProperty("server.thread-pool.reporter.interval", 60000);
    private DynamicStringProperty appName = DynamicPropertyFactory.getInstance().getStringProperty("archaius.deployment.applicationId", "cacheproxy");
    
    private final ProxyRunner proxyRunner;
    private final ThreadPoolExecutor poolExecutor;
    
    private AtomicLong rejectedRequests = new AtomicLong(0);
    private AtomicLong beenConnected = new AtomicLong(0);
    private AtomicLong beenDisconnected = new AtomicLong(0);
    private String channelCreateTimeAttrName = "channelCreateTime";
    
    static ILog clogger = LogManager.getLogger(ProxyHandler.class);
    static Map<String,String> CONNECT_TAGS = new HashMap<>();
    static {
        CONNECT_TAGS.put("connectLog", "connectLog");
    }
    
    public ProxyHandler(){
        proxyRunner = new ProxyRunner();

        poolExecutor = new ThreadPoolExecutor(poolCoreSize.get(), poolMaximumSize.get(), poolAliveTime.get(), TimeUnit.MILLISECONDS, new SynchronousQueue<Runnable>());
        //Report PoolExecutor Stats.
        new PoolExecutorReporter().start();

        Runnable c2 = new Runnable() {
            @Override
            public void run() {
                poolExecutor.setCorePoolSize(poolCoreSize.get());
                poolExecutor.setMaximumPoolSize(poolMaximumSize.get());
                poolExecutor.setKeepAliveTime(poolAliveTime.get(),TimeUnit.MILLISECONDS);
            }
        };
        poolCoreSize.addCallback(c2);
        poolMaximumSize.addCallback(c2);
        poolMaximumSize.addCallback(c2);
    }
    
    
	@Override
	protected void messageReceived(ChannelHandlerContext ctx, Request request)
			throws Exception {
		Response response = null;
		switch(request.getRequestType()){
			case memcache:
				response = new MemResponse();
				
                response.setChannelHandlerContext(ctx);
                try {
                    poolExecutor.submit(new ProxyCallable(request, response, proxyRunner));
                } catch (Throwable t) {
                    rejectedRequests.incrementAndGet();
                    ctx.close();
                }

                break;
			case redis:
				System.out.println("none");
				
            case error:
                ctx.close();
                break;
		}
		
	}

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        beenConnected.incrementAndGet();

        Channel channel = ctx.channel();
        channel.attr(AttributeKey.valueOf(channelCreateTimeAttrName)).set(System.currentTimeMillis());

        if (CLOG_ENABLED.get()) {
        	clogger.info("ConnectInfo", channel.remoteAddress() + " connected", CONNECT_TAGS);
        }

        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        beenDisconnected.incrementAndGet();

        if (CLOG_ENABLED.get()) {
            Channel channel = ctx.channel();
            clogger.info("ConnectInfo", channel.remoteAddress() + " disconnected" + ", having lived:" + (System.currentTimeMillis() - (Long) channel.attr(AttributeKey.valueOf(channelCreateTimeAttrName)).get()) + "ms", CONNECT_TAGS);
        }

        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (CLOG_ENABLED.get()) {
            Channel channel = ctx.channel();
            clogger.warn("ConnectInfo", channel.remoteAddress() + " exception" + ", having lived:" + (System.currentTimeMillis() - (Long) channel.attr(AttributeKey.valueOf(channelCreateTimeAttrName)).get()) + "ms. exception:" +  cause.getClass().getName() + "::::" + cause.getMessage(), CONNECT_TAGS);
        }
    }
    
    //Below is poolExecutorReporter
    private static AtomicInteger NO = new AtomicInteger(0);
    private class PoolExecutorReporter extends Thread{
        private PoolExecutorReporter() {
            setName(getClass().getSimpleName() + "-" + NO.getAndIncrement());
            setDaemon(true);
        }

        long preCompletedTasks = 0;
        long preTotalTasks = 0;
        long preRejectTasks = 0;
        long preBeenConnectedConnections = 0;
        long preBeenDisonnectedConnections = 0;

        @Override
        public void run() {
            try {
                IMetric metric = MetricManager.getMetricer();
                Date date;

                while (true) {
                    try {
                    	logger.info("start to report thread pool metrics.");
                        date = new Date();
                        reportCountStats(metric, date);
                    } catch (Throwable e) {
                    	logger.error("Encounter an error while reporting.", e);
                    } finally {
                        sleep(reportInterval.get());
                    }
                }
            } catch (InterruptedException e) {
            	logger.error("Async Servlet Reporter stopped because some error.", e);
            }
        }

        private void reportCountStats(IMetric metric, Date date) {
            ThreadPoolExecutor p = poolExecutor;

            int activeTasks = p.getActiveCount();
            long completedTasks = p.getCompletedTaskCount();
            long totalTasks = p.getTaskCount();
            int waitingTasks = p.getQueue().size();
            int threads = p.getPoolSize();
            long rejectTasks = rejectedRequests.get();
            long beenConnectedConnections = beenConnected.get();
            long beenDisconnectedConnections = beenDisconnected.get();

            long completedTasksThisRound = completedTasks - preCompletedTasks;
            long totalTasksThisRound = totalTasks - preTotalTasks;
            long rejectTasksThisRound = rejectTasks - preRejectTasks;
            long beenConnectedThisRound = beenConnectedConnections - preBeenConnectedConnections;
            long beenDisconnectedThisRound = beenDisconnectedConnections - preBeenDisonnectedConnections;

            preCompletedTasks = completedTasks;
            preTotalTasks = totalTasks;
            preRejectTasks = rejectTasks;
            preBeenConnectedConnections = beenConnectedConnections;
            preBeenDisonnectedConnections = beenDisconnectedConnections;

            String prefix = appName.get();
            metric.log(prefix + ".request.processing", activeTasks, date);
            metric.log(prefix + ".request.waiting", waitingTasks, date);
            metric.log(prefix + ".request.completed", completedTasksThisRound, date);
            metric.log(prefix + ".request.rejected", rejectTasksThisRound, date);
            metric.log(prefix + ".request.request", totalTasksThisRound, date);
            metric.log(prefix + ".thread-pool.size", threads, date);

            metric.log(prefix + ".connections.new", beenConnectedThisRound, date);
            metric.log(prefix + ".connections.close", beenDisconnectedThisRound, date);
            metric.log(prefix + ".connections.live", beenConnectedConnections - beenDisconnectedConnections, date);

            logger.info("\n{} stats:\n" +
                            "\trequests processing:\t{}\n" +
                            "\trequests waiting:\t{}\n" +
                            "\n" +
                            "\trequests complected in a round:\t{}\n" +
                            "\trequests rejected in a round:\t{}\n" +
                            "\trequests request in a round:\t{}\n" +
                            "\n" +
                            "\trequests complected total:\t{}\n" +
                            "\trequests rejected total:\t{}\n" +
                            "\trequests total:\t{}\n" +
                            "" +
                            "\tthread pool size:\t{}\n" +
                            "\n" +
                            "\tconnections connected in a round:\t{}\n" +
                            "\tconnections disconnected in a round:\t{}\n" +
                            "\tconnections live now:\t{}\n",
                    prefix, activeTasks, waitingTasks, completedTasksThisRound, rejectTasksThisRound, totalTasksThisRound, completedTasks, rejectTasks, totalTasks, threads, beenConnectedThisRound, beenDisconnectedThisRound, beenConnectedConnections - beenDisconnectedConnections);
        }
    }
}
