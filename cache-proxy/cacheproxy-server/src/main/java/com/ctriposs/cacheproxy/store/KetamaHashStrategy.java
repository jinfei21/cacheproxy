package com.ctriposs.cacheproxy.store;

import com.ctriposs.cacheproxy.common.Cluster;
import com.ctriposs.cacheproxy.common.Group;
import com.ctriposs.cacheproxy.common.Strategy;

public class KetamaHashStrategy implements Strategy{
	
	private KetamaHash hash;
	
	public KetamaHashStrategy(Cluster cluster) throws Exception{
		this.hash = new KetamaHash(cluster);
	}


	@Override
	public Group getGroup(String key) {
		
		return this.hash.getGroup(key);
	}
	
}
