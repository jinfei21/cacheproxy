package com.ctriposs.cacheproxy.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.List;

import com.ctriposs.cacheproxy.common.MemRequest;
import com.ctriposs.cacheproxy.common.QuitRequest;
import com.ctriposs.cacheproxy.common.Request;
import com.ctriposs.cacheproxy.netty.MemRequestDecoder.State;


public class MemRequestDecoder extends ReplayingDecoder<State> {

    

    private long time = -1L;
    private int bodyLen = 0;
    private ByteBuf head = null;
    private ByteBuf body = null;
    private String key = null;
    private volatile Request msg = null;
    private static Request QUIT = new QuitRequest();
    
    public MemRequestDecoder() {
        super(State.READ_HEAD);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

     switch(state()){
     	case READ_HEAD:{
     		 if(-1==time){
     			 time = System.currentTimeMillis();
     		 }
     	     final int eol = findEndOfLine(in);
     	     final int len = eol - in.readerIndex();
     	     
     	     if(eol > 0){
     	    	 final int delimLen = in.getByte(eol) == '\r'?2:1;
     	    	 head = in.readBytes(len+delimLen);
     	    	 String cmd = head.toString(Charset.defaultCharset());
     	    	 
     	    	 String[] tmp = cmd.split("\\s");
     	    	 if(tmp.length > 3){     	
     	    		bodyLen = Integer.parseInt(tmp[tmp.length - 1])+2;
     	    		key = tmp[1];
     	    		checkpoint(State.READ_BODY);
     	    	 }else{
     	    		 if("get".equalsIgnoreCase(tmp[0])){
     	    			 key = tmp[1];
     	    		 }else if("quit".equalsIgnoreCase(tmp[0])){
     	    			msg = QUIT;
     	    		 }
     	    	 }
     	     }else{
     	    	 if(eol == 0){
     	    		 in.readBytes(2);//skip \r\n
     	    	 }
     	    	 break;
     	     }
     		
     	}
     		
     	case READ_BODY:{
     		if(bodyLen > 0){
     			int available = in.readableBytes();
     			if(available >= bodyLen){
     				if(body == null){
     					body = in.readBytes(bodyLen);
     				}else{
     					body.writeBytes(in.readBytes(bodyLen));
     				}
     				bodyLen = 0;
     				Request req = new MemRequest();
                    InetSocketAddress local = (InetSocketAddress) ctx.channel().localAddress();
                    InetSocketAddress remote = (InetSocketAddress) ctx.channel().remoteAddress();
                    req.setLocalAddress(local.getAddress());
                    req.setLocalPort(local.getPort());
                    req.setRemoteAddress(remote.getAddress());
                    req.setRemotePort(remote.getPort());
                    req.setTime(time);
                    req.setCompletedReceiveTime(System.currentTimeMillis());
                    req.setBody(body);
                    req.setHead(head);
                    req.setKey(key);

                    msg = req;
     			}else{
     				if(body != null){
     					body.writeBytes(in.readBytes(available));
     				}else{
     					body = in.readBytes(available);
     				}
     				bodyLen -= available;
     			}
     		}
     		
     		if(bodyLen == 0){
     			if(msg == null){
     				msg = new MemRequest();
                    InetSocketAddress local = (InetSocketAddress) ctx.channel().localAddress();
                    InetSocketAddress remote = (InetSocketAddress) ctx.channel().remoteAddress();
                    msg.setLocalAddress(local.getAddress());
                    msg.setLocalPort(local.getPort());
                    msg.setRemoteAddress(remote.getAddress());
                    msg.setRemotePort(remote.getPort());
                    msg.setTime(time);
                    msg.setCompletedReceiveTime(System.currentTimeMillis());
                    msg.setBody(body);
                    msg.setHead(head);
                    msg.setKey(key);     
     			}
     			out.add(msg);
     			msg = null;
     			head = null;
     			body = null;
     			time = -1L;
     			key = null;
     			checkpoint(State.READ_HEAD);
     		}
     	}
     }
      
    }

    private static int findEndOfLine(final ByteBuf buffer) {
        final int n = buffer.writerIndex();
        for (int i = buffer.readerIndex(); i < n; i++) {
            final byte b = buffer.getByte(i);
            if (b == '\n') {
                return i;
            } else if (b == '\r' && i < n - 1 && buffer.getByte(i + 1) == '\n') {
                return i;  // \r\n
            }
        }
        return -1;  // Not found.
    }
    
    enum State{
        READ_HEAD,
        READ_BODY
    }
}
