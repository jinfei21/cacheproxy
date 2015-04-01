package scripts.post

import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelFutureListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.ctriposs.cacheproxy.common.Request
import com.ctriposs.cacheproxy.common.Response
import com.ctriposs.cacheproxy.common.SockIO
import com.ctriposs.cacheproxy.filter.ProxyFilter
import com.ctriposs.cacheproxy.filter.RequestContext
import com.ctriposs.cacheproxy.stat.Globals
import com.ctriposs.cacheproxy.stat.StatsKey

class ReportMetrics extends ProxyFilter {
	private Logger logger = LoggerFactory.getLogger(ProxyFilter.class)
	
	@Override
	public String filterType() {
		return "post"
	}

	@Override
	public int filterOrder() {
		return 100
	}

	@Override
	public boolean shouldFilter() {
		return true;
	}

	@Override
	public Object run() {
		RequestContext ctx = RequestContext.getCurrentContext();
		Request req = ctx.getOriginRequest().origin
		Response res = ctx.getOriginResponse().origin

		ChannelFuture writeResponseFuture = null
		ChannelFuture closeResponseFuture = null
		try {
			writeResponseFuture = ctx.getWriteResponseFuture()
			closeResponseFuture = ctx.getCloseResponseFuture()
		} catch (Throwable ignore) {
			logger.error("get future error", ignore)
		}
		req.attr("sendRequestCost", ctx.getSendRequestCost())
		req.attr("readResponseCost",ctx.getReadResponseCost())
		req.attr("sendResponseStart",ctx.getSendResponseStart())
		req.attr("sendRequestStatus",ctx.getSendRequestStatus())
		req.attr("readResponseStatus",ctx.getReadResponseStatus())
		
		//For normal write response.
		if (writeResponseFuture != null) {
			writeResponseFuture.addListener(new ChannelFutureListener() {
				@Override
				void operationComplete(ChannelFuture future) throws Exception {
					try {
						
						req.attr("responseType", "response")
						ReportMetrics.logAndMetrics(req, res, future)
					} catch (Exception e) {
						logger.error("log req metrics error", e)
					}
				}
			});
		} else if (closeResponseFuture != null) {
			//For close connection because exception.
			logger.warn("write future is null")
			closeResponseFuture.addListener(new ChannelFutureListener() {
				@Override
				void operationComplete(ChannelFuture future) throws Exception {
					try {
						req.attr("responseType", "close")
						ReportMetrics.logAndMetrics(req, res, future)
					} catch (Exception e) {
						logger.error("log req metrics error", e)
					}
				}
			})
		} else {
			logger.error("write future and close future all is null")
		}
		return null;
	}

	public static void logAndMetrics(final Request req, final Response res, final ChannelFuture future){
		String bucketid = String.valueOf(req.getLocalPort());
		String responseType = req.getAttr("responseType", "null");
		String groupid = req.getAttr("groupid", "null");
		long readRequestCost = req.completedReceiveTime - req.time;
		long sendRequestCost = req.attr("sendRequestCost");
		long readResponseCost = req.attr("readResponseCost");
		long sendResponseStart = req.attr("sendResponseStart");
		int sendRequestStatus = req.attr("sendRequestStatus");
		int readResponseStatus = req.attr("readResponseStatus");
		int requestSize = req.length();
		int reponseSize = res.length();
		Globals.REQ_STATS_COLLECTOR.add(
			new StatsKey("cacheproxy.readrequest")
					.addTag("bucketid", bucketid)
					.addTag("groupid", groupid)
					.addTag("responseType", responseType)			
					.reportStatus(false),
			readRequestCost,
			requestSize,
			0);
		
		Globals.REQ_STATS_COLLECTOR.add(
			new StatsKey("cacheproxy.sendrequest")
					.addTag("bucketid", bucketid)
					.addTag("groupid", groupid)
					.addTag("responseType", responseType),

			sendRequestCost,
			requestSize,
			sendRequestStatus);
		
		Globals.REQ_STATS_COLLECTOR.add(
			new StatsKey("cacheproxy.readresponse")
					.addTag("bucketid", bucketid)
					.addTag("groupid", groupid)
					.addTag("responseType", responseType),
			readResponseCost,
			reponseSize,
			readResponseStatus);
		
		boolean responseIsSuccess = future.isSuccess();
		Globals.REQ_STATS_COLLECTOR.add(
			new StatsKey("cacheproxy.sendresponse")
					.addTag("bucketid", bucketid)
					.addTag("groupid", groupid)
					.addTag("responseType", responseType),

			System.currentTimeMillis() - sendResponseStart,
			reponseSize,
			responseIsSuccess ? 0 : 1);
		
		Globals.REQ_STATS_COLLECTOR.add(
			new StatsKey("cacheproxy.fullcost")
					.addTag("bucketid", bucketid)
					.addTag("groupid", groupid)
					.addTag("responseType", responseType)
					.reportStatus(false),

			System.currentTimeMillis() - sendResponseStart+readResponseCost+sendRequestCost,
			reponseSize+requestSize,
			0);
		
	}
}
