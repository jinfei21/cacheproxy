package com.ctriposs.cacheproxy.common;

import java.util.ArrayList;
import java.util.List;

public class Cluster {

	private List<Group> groups = new ArrayList<Group>();
	
	public Cluster(String cluster){
		String[] groups = cluster.split(",");
		
		for(String group:groups){
			if(!"".equals(group)){
				this.groups.add(new Group(group.trim()));
			}
		}
	}
	
	public Cluster(){
		
	}
	
	public void addGroup(Group group){
		this.groups.add(group);
	}
	
	public List<Group> getGroups(){
		return this.groups;
	}
	
	public static void main(String[] args){
		Cluster c = new Cluster("127.0.0.1:dfsf|fdsa:12,fsdfs:re|fsafsa:ll");
		
		List<Group> list = c.getGroups();
		
		for(Group g:list){
			List<String> l = g.getServers();
			for(String s:l){
				System.out.print(s+"   ");
			}
			System.out.println();
		}
		
		
	}
	
}
