package com.ctriposs.cacheproxy.store;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ctriposs.cacheproxy.common.SockIO;

public class NodeSock implements SockIO{

	private static Logger logger = LoggerFactory.getLogger(NodeSock.class);
	
	private String host;
	private Socket socket;
	
	private DataInputStream input;
	private BufferedOutputStream output;
	
	private long createTime = System.currentTimeMillis();
	
	public NodeSock(String host,int readTimeout,int connectTimeout,boolean noDelay) throws NumberFormatException, IOException{
		
		this.host = host;
		String[] ips = host.split(":");
		this.socket = creatSocket(ips[0],Integer.parseInt(ips[1]),connectTimeout);
		if(readTimeout >= 0){
			this.socket.setSoTimeout(readTimeout);
		}
		
		this.socket.setTcpNoDelay(noDelay);
		
		this.input = new DataInputStream(new BufferedInputStream(this.socket.getInputStream()));
		this.output = new BufferedOutputStream(this.socket.getOutputStream());
		
	}
	
	public NodeSock(String ip,int port,int readTimeout,int connectTimeout,boolean noDelay) throws NumberFormatException, IOException{
		
		this.host = ip+":"+port;
		
		this.socket = creatSocket(ip,port,connectTimeout);
		if(readTimeout >= 0){
			this.socket.setSoTimeout(readTimeout);
		}
		
		this.socket.setTcpNoDelay(noDelay);
		
		this.input = new DataInputStream(new BufferedInputStream(this.socket.getInputStream()));
		this.output = new BufferedOutputStream(this.socket.getOutputStream());
		
	}
	
	private Socket creatSocket(String host,int port,int timeout) throws IOException{
		SocketChannel sock = SocketChannel.open();
		sock.socket().connect(new InetSocketAddress(host, port),timeout);
		return sock.socket();
	}
	
	@Override
	public boolean isConnected(){
		return (this.socket != null && this.socket.isConnected()); 
	}
	
	@Override
	public void close() throws IOException{
		if(this.socket != null){
			this.socket.close();
		}
	}
	
	@Override
	public String readLine() throws IOException {
		
		if(this.socket == null || !this.socket.isConnected()){
			logger.error(this.host +" socket closed!");
			throw new IOException(" attemptint to read from closed socket!");
		}
		
		byte[] buf = new byte[1];
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		boolean eol = false;
		
		while(input.read(buf, 0, 1) != -1){
			if(buf[0] == 13){
				eol = true;
			}else{
				if(eol){
					if(buf[0] == 10){
						break;
					}
					eol = false;
				}
			}
			bos.write(buf, 0, 1);
		}
		
		if(bos == null || bos.size() <= 0){
			throw new IOException("stream is dead!");
		}
		return bos.toString().trim();
	}

	@Override
	public void clearEOL() throws IOException {
		if(socket == null || !socket.isConnected()){
			logger.error(this.host+" socket close!");
			throw new IOException(" attempting to read from closed socket!");
		}
		byte[] buf = new byte[1];
		boolean eol = false;
		//end by \r(13) followed by \n(10)
		while(input.read(buf,0,1) != -1){
			if(buf[0] == 13){
				eol = true;
				continue;
			}
			
			if(eol){
				if(buf[0] == 10){
					break;
				}
				eol = false;
			}
		}
		
	}

	@Override
	public int read(byte[] buf) throws IOException {
		if(socket == null || !socket.isConnected()){
			logger.error(this.host+" socket close!");
			throw new IOException(" attempting to read from closed socket!");
		}
		int count = 0;
		while(count < buf.length){
			int cnt = input.read(buf, count, (buf.length - count));
			count += cnt;
		}
		return count;
	}

	@Override
	public void flush() throws IOException {
		if(socket == null || !socket.isConnected()){
			logger.error(this.host+" socket close!");
			throw new IOException(" attempting to write to closed socket!");
		}
		output.flush();
	}

	@Override
	public void write(byte[] buf) throws IOException {
		if(socket == null || !socket.isConnected()){
			logger.error(this.host+" socket close!");
			throw new IOException(" attempting to write to closed socket!");
		}
		output.write(buf);
	}

	@Override
	public void write(byte b) throws IOException {
		if(socket == null || !socket.isConnected()){
			logger.error(this.host+" socket close!");
			throw new IOException(" attempting to write to closed socket!");
		}
		output.write(b);
	}

	@Override
	public long createTime() {
		return this.createTime;
	}

	@Override
	public String server() {
		return this.host;
	}
	
}
