package com.ctriposs.cacheproxy.store;

import java.util.ArrayList;

import com.ctriposs.cacheproxy.common.Group;
import com.ctriposs.cacheproxy.common.Strategy;
import com.ctriposs.cacheproxy.util.RendezvousHash;
import com.google.common.hash.Funnel;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.hash.PrimitiveSink;

public class ConsistentHashStrategy implements Strategy{
	
    private volatile RendezvousHash<String,Group> hashTable;
    
	private static final HashFunction hfunc = Hashing.murmur3_128();
	private static final Funnel<Group> nFunnel = new Funnel<Group>(){ 
		public void funnel(Group from, PrimitiveSink into) {
			into.putBytes(from.getBytes());
		}};
		
	private static final Funnel<String> kFunnel = new Funnel<String>(){ 
		public void funnel(String from, PrimitiveSink into) {
			into.putBytes(from.getBytes());
		}};
	
	public ConsistentHashStrategy(String cluster){
		this.hashTable = new RendezvousHash<String,Group>(hfunc, kFunnel, nFunnel, new ArrayList<Group>());

	}

	@Override
	public Group getGroup(String key) {
		
		return this.hashTable.get(key);
	}


}
