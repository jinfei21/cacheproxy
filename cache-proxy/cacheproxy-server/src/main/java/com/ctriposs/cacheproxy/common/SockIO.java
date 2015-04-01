package com.ctriposs.cacheproxy.common;

import java.io.IOException;

public interface SockIO {

	public String readLine() throws IOException;
	
	public void clearEOL() throws IOException;
	
	public int read(byte[] buf) throws IOException;
	
	public void flush() throws IOException;
	
	public void write( byte[] buf) throws IOException;
	
	public void write(byte b) throws IOException;
	
	public void close() throws IOException;
	
	public boolean isConnected();
	
	public long createTime();
	
	public String server();
}
