package com.ctriposs.cacheproxy.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Group {

	private static Random rand = new Random();
	private List<String> servers = new ArrayList<String>();
	
	private String groupID;	
	
	
	public Group(String groups){
		String[] group = groups.split("\\|");
		for(String server:group){
			if(!"".equals(server.trim())){
				servers.add(server.trim());
			}
		}
	}
	
	public Group(String gid,String groups){
		this(groups);
		this.groupID = gid; 
	}
	
	public String getGroupID(){
		return this.groupID;
	}
	
	public List<String> getServers(){
		return this.servers;
	}
	
	public List<String> getServers(boolean flag){
		if(flag){
			return this.servers;
		}else{
			List<String> list = new ArrayList<String>();
			list.add(getCurrentServer());
			return list;
		}
	}
	
	public byte[] getBytes(){
		return this.servers.get(0).getBytes();
	}
	
	public String getCurrentServer(){
		return this.servers.get(rand.nextInt(this.servers.size()));
	}
	
	public static void main(String[] args){

		Group g = new Group("1   |   2   |  3   ");
		for(int i=0;i<100;i++){
			System.out.println(g.getCurrentServer());
		}
	}

}
