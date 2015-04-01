package com.ctriposs.cacheproxy.common;

import io.netty.buffer.ByteBuf;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProxyMessage {
	private long time = System.currentTimeMillis();

	private Map<String, Object> attrsMap = new ConcurrentHashMap<String, Object>();

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public Object attr(String name) {
		return attrsMap.get(name);
	}

	public Object getAttr(String name, Object defaultValue) {
		Object o = attrsMap.get(name);
		return o == null ? defaultValue : o;
	}

	public void attr(String name, Object value) {
		attrsMap.put(name, value);
	}

	public Map<String, Object> attrs() {
		return attrsMap;
	}

	/** Below: byte info **/

	private ByteBuf head;
	private ByteBuf body;
	private String key;
	public ByteBuf getHead() {
		return head;
	}

	public void setHead(ByteBuf head) {
		this.head = head;
	}

	public ByteBuf getBody() {
		return body;
	}

	public void setBody(ByteBuf body) {
		this.body = body;
	}

	public void encodeAsByteBuf(ByteBuf byteBuf) {
		if(head != null){
			byteBuf.writeBytes(head);
		}
		if(body != null){
			byteBuf.writeBytes(body);
		}
		
	}
	
	public void setKey(String key){
		this.key = key;
	}
	
	public String getKey(){
		return this.key;
	}
	

	
	public int length(){
		int len = 0;
		if(head != null){
			len += head.writerIndex();
		}
		if(body != null){
			len += body.writerIndex();
		}
		return len;
	}
}
