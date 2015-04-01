package com.ctriposs.cacheproxy.stat;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author:yjfei
 * @date: 2/26/2015.
 */
public class ReqStats {
    private int count;

    private long totalCost;
    private long minCost = 0;
    private long maxCost = 0;

    private long totalSize;
    private long minSize = 0;
    private long maxSize = 0;

    private Map<String, AtomicInteger> timeSlotCountMap = new HashMap<String, AtomicInteger>();
    private Map<String, AtomicInteger> sizeSlotCountMap = new HashMap<String, AtomicInteger>();
    private Map<String, AtomicInteger> statusSlotCountMap = new HashMap<String, AtomicInteger>();

    public void addInfo(long cost, long size, int statusCode){
        count += 1;
        totalCost += cost;
        totalSize += size;

        if (minCost > cost || minCost==0) {
            minCost = cost;
        }

        if (maxCost < cost) {
            maxCost = cost;
        }

        if (minSize > size || minSize==0) {
            minSize = size;
        }

        if (maxSize < size) {
            maxSize = size;
        }

        addCountMap(TimeSlot.fromCost(cost).getName(), timeSlotCountMap);
        addCountMap(SizeSlot.fromSize(size).getName(), sizeSlotCountMap);
        addCountMap(StatusSlot.fromStatus(statusCode).getName(), statusSlotCountMap);
    }

    public int getCount() {
        return count;
    }

    public long getAvgCost(){
        return totalCost/count;
    }

    public long getMinCost(){
        return minCost;
    }

    public long getMaxCost(){
        return maxCost;
    }

    public long getAvgSize(){
        return totalSize/count;
    }

    public long getMinSize(){
        return minSize;
    }

    public long getMaxSize(){
        return maxSize;
    }

    public Map<String, AtomicInteger> getTimeSlotCountMap() {
        return timeSlotCountMap;
    }

    public Map<String, AtomicInteger> getSizeSlotCountMap() {
        return sizeSlotCountMap;
    }

    public Map<String, AtomicInteger> getStatusSlotCountMap() {
        return statusSlotCountMap;
    }

    private void addCountMap(String key, Map<String,AtomicInteger> map) {
        AtomicInteger count = map.get(key);
        if (count == null) {
            count = new AtomicInteger(0);
            map.put(key,count);
        }
        count.getAndIncrement();
    }

    enum StatusSlot{
        SUCCESS("Success"),
        FAILED("Failed"),
        NOROUTE("NoRoute"),
        UNKNOWN("Unknown");

        private String name;

        StatusSlot(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        static StatusSlot fromStatus(int ordinal) {
            try {
                return StatusSlot.values()[ordinal];
            } catch (Exception e) {
                return UNKNOWN;
            }
        }
    }
    enum TimeSlot{
        MS_10("0~10ms"),
        MS_50("10~50ms"),
        MS_200("50~200ms"),
        MS_500("200~500ms"),
        MS_1000("500~1000ms"),
        MS_5000("1~5s"),
        MS_10000("5~10s"),
        MS_20000("10~20s"),
        MS_30000("20~30s"),
        MS_50000("30~50s"),
        MS_100000("50~100s"),
        MS_100001(">100s");

        private String name;
        TimeSlot(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        static TimeSlot fromCost(long cost) {
            if (cost < 10l) {
                return MS_10;
            }else if (cost < 50l) {
                return MS_50;
            }else if (cost < 200l) {
                return MS_200;
            }else if (cost < 500l) {
                return MS_500;
            }else if (cost < 1000l) {
                return MS_1000;
            }else if (cost < 5000l) {
                return MS_5000;
            }else if (cost < 10000l) {
                return MS_10000;
            }else if (cost < 20000l) {
                return MS_20000;
            }else if (cost < 30000l) {
                return MS_30000;
            }else if (cost < 50000l) {
                return MS_50000;
            }else if (cost < 100000l) {
                return MS_100000;
            }else{
                return MS_100001;
            }
        }
    }

    enum SizeSlot{
        K_10("0~10K"),
        K_50("10~50K"),
        K_200("50~200K"),
        K_500("200~500K"),
        K_1000("500K~1M"),
        K_5000("1~5M"),
        K_10000("5~10M"),
        K_10001(">10M");

        private String name;
        SizeSlot(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        static SizeSlot fromSize(long size){
            if(size < 1024*10){
                return K_10;
            }else if(size < 1024*50){
                return K_50;
            }else if(size < 1024*200){
                return K_200;
            }else if(size < 1024*500){
                return K_500;
            }else if(size < 1024*1024){
                return K_1000;
            }else if(size < 1024*1024*5){
                return K_5000;
            }else if(size < 1024*1024*10){
                return K_10000;
            }else{
                return K_10001;
            }
        }
    }
}
