
package com.ctriposs.cacheproxy.store;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.ctriposs.cacheproxy.common.Cluster;
import com.ctriposs.cacheproxy.common.Group;

public class KetamaHash {

	private Cluster cluster;
	private Long[] values;
	private Group[] groups;
	
	private static MessageDigest MD5_DIGEST = null;
	
	static{
        try {
            MD5_DIGEST = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not supported", e);
        }
	}
	
	
	public KetamaHash(Cluster cluster) throws Exception{
		this.cluster = cluster;
		int copyCount = 10000;
		
		Map<Long,Group> list = new TreeMap<Long,Group>();
		
		if((cluster.getGroups().size()*copyCount) > 320*1000){
			throw new Exception("There is too many copyCount or real nodes! nodes.Count multiply copyNodes must be not greater than 320*1000 ");
		}
		
		for(Group group : cluster.getGroups()){
			for(int i=0;i<copyCount;i++){
				long m = hash(group.getGroupID()+"_"+i);
				list.put(m, group);
			}
		}
		
		this.values = new Long[list.size()];
		this.groups = new Group[list.size()];
		
		int index = 0;
		
		for(Entry<Long,Group> entry:list.entrySet()){
			values[index] = entry.getKey();
			groups[index] = entry.getValue();
			index++;
		}
	}
	
    protected static byte[] computeMd5(String k) {
        MessageDigest md5=MD5_DIGEST;
        
        md5.reset();
        return md5.digest(getKeyBytes(k));
    }

    protected long hash(final String k) {
        final byte[] bKey = computeMd5(k);
        
        final long rv = ((long) (bKey[3] & 0xFF& 0xFF) << 24)
                | ((long) (bKey[2] & 0xFF& 0xFF) << 16)
                | ((long) (bKey[1] & 0xFF& 0xFF) << 8)
                | (bKey[0] & 0xFF& 0xFF);
        return rv & 0xffffffffL; /* Truncate to 32-bits */
    }

    protected static byte[] getKeyBytes(String k) {
        try {
            return k.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
    
    public Group getGroup(String key){
    	if(key == null || key.trim().length() == 0){
    		throw new IllegalArgumentException("key is null!");
    	}
    	
    	long value = hash(key);
    	int result = Arrays.binarySearch(values, value);
    	
    	if(result < 0){
    		result = ~result;
    	}
    	
    	if(result > groups.length){
    		return groups[groups.length -1];
    	}else{
    		return groups[result];
    	}
   
    }
    
}
