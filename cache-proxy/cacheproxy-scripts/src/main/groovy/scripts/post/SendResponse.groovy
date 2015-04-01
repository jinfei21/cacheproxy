package scripts.post

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

import java.nio.charset.Charset

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.ctriposs.cacheproxy.common.Request
import com.ctriposs.cacheproxy.common.Response
import com.ctriposs.cacheproxy.common.SockIO
import com.ctriposs.cacheproxy.common.SockIOPool
import com.ctriposs.cacheproxy.filter.ProxyFilter
import com.ctriposs.cacheproxy.filter.RequestContext
import com.ctriposs.cacheproxy.stat.Globals
import com.ctriposs.cacheproxy.stat.StatsKey
import com.ctriposs.cacheproxy.store.DefaultSockClient

class SendResponse extends ProxyFilter {
	Logger logger = LoggerFactory.getLogger(SendResponse.class)
	private static final String VALUE 		 = "VALUE";         // start of value line from server
	private static final String STATS        = "STAT";			// start of stats line from server
	private static final String END          = "END";			// end of data from server

	@Override
	String filterType() {
		return "post"
	}

	@Override
	int filterOrder() {
		return 20
	}

	@Override
	boolean shouldFilter() {
		return true
	}

	@Override
	public Object run() {

		RequestContext ctx = RequestContext.getCurrentContext();
		Request req = ctx.getOriginRequest().origin
		Response res = ctx.getOriginResponse().origin
		List<SockIO> sockList = ctx.getSockIOList();
		String bucketid = String.valueOf(req.getLocalPort())
		long start = System.currentTimeMillis();
		int status = 1;
		for(SockIO sock:sockList){
			long serverStart = System.currentTimeMillis();
			try{				
				ByteBuf body = getResponsePackage(sock);
				res.setBody(body);
				status = 0;
			}catch(Throwable t){		
				status = 1;
				logger.error(String.valueOf(req.getLocalPort()) + " read response error!", t);
				break;
			}finally{
				Globals.REQ_STATS_COLLECTOR.add(
					new StatsKey("cacheproxy.readmem")
							.addTag("bucketid", bucketid)
							.addTag("memnode", sock.server())
							.reportSize(false),
					System.currentTimeMillis() - serverStart,
					0,
					status);
			}
		}
		if(status == 1){
			res.setBody(Unpooled.copiedBuffer("SERVER_ERROR\r\n", Charset.defaultCharset()));
		}
		ctx.setReadResponseStatus(status)
		ctx.setReadResponseCost(System.currentTimeMillis() - start);
		ctx.setSendResponseStart(System.currentTimeMillis())
		try{
			ctx.setWriteResponseFuture(res.channelHandlerContext.writeAndFlush(res))
		}catch(Throwable t){
			logger.error(String.valueOf(req.getLocalPort()) + "send response error!", t)
		}
		for(SockIO sock:sockList){
			if(sock!= null){
				String server = sock.server()
				SockIOPool pool = DefaultSockClient.getPools().get(server);
				if(pool != null){
					pool.release(sock)
				}
			}
		}
		return null;
	}
	

	private ByteBuf getResponsePackage(SockIO sock) throws IOException {

		String line = sock.readLine();
		ByteBuf body = Unpooled.copiedBuffer(line + "\r\n", Charset.defaultCharset());

		if(line.startsWith(VALUE)) {
			boolean isEnd =false;
			while (!isEnd) {

				String[] info = line.split(" ");
				
				int length = Integer.parseInt(info[3])+2;
				int endP =0;
				while (endP<length) {
					int readCount = 1024;
					if(length-endP<readCount){
						readCount = length-endP;
					}

					endP+=readCount;

					byte[] buffer= new byte[readCount];
					sock.read(buffer);

					body.writeBytes(buffer);
				}

				line = sock.readLine();
				if(line.startsWith(END)){
					isEnd=true;
				}
				body.writeBytes(Unpooled.copiedBuffer(line + "\r\n", Charset.defaultCharset()));
			}
		}
		else if(line.startsWith(STATS)){
			boolean isEnd =false;
			while (!isEnd) {

				line = sock.readLine();
				if(line.startsWith(END)){
					isEnd=true;
				}
				body.writeBytes(Unpooled.copiedBuffer(line + "\r\n", Charset.defaultCharset()));
			}

		}
		//println body.toString(Charset.defaultCharset());
		return body;
	}
}
