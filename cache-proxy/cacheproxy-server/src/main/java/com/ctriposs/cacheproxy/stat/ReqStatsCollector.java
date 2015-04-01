package com.ctriposs.cacheproxy.stat;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ctrip.freeway.metrics.IMetric;
import com.ctrip.freeway.metrics.MetricManager;

public class ReqStatsCollector {
	private static Logger logger = LoggerFactory.getLogger(ReqStatsCollector.class);

    private AtomicReference<ConcurrentMap<StatsKey, ReqStats>> ref = new AtomicReference<ConcurrentMap<StatsKey, ReqStats>>(new ConcurrentHashMap<StatsKey, ReqStats>());

    private int reportInterval = 1000*60;

    public ReqStatsCollector() {
        start();
    }

    public void add(StatsKey key, long cost, long size, int status) {
        ReqStats stats = ref.get().get(key);

        if (stats == null) {
            stats = new ReqStats();
            ReqStats found = ref.get().putIfAbsent(key, stats);
            if (found != null) {
                stats = found;
            }
        }
        stats.addInfo(cost, size, status);
    }

    public ConcurrentMap<StatsKey, ReqStats> drainDry() {
        return ref.getAndSet(new ConcurrentHashMap<StatsKey, ReqStats>());
    }

    public int getReportInterval() {
        return reportInterval;
    }

    public void setReportInterval(int reportInterval) {
        this.reportInterval = reportInterval;
    }

    static AtomicInteger NO = new AtomicInteger(0);
    private void start() {
        Thread t = new Thread(){

            @Override
            public void run() {
                IMetric metricer = MetricManager.getMetricer();

                while (true) {
                    try {
                        Thread.sleep(reportInterval);
                        Date date = new Date();
                        writeRequestMetrics(metricer, drainDry(), date);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }finally {
                        logger.info("start to report req metrics.");
                    }
                }
            }
        };
        t.setName(this.getClass().getSimpleName() + "-" + NO.incrementAndGet());
        t.setDaemon(true);
        t.start();
    }

    private void writeRequestMetrics(IMetric metricer, ConcurrentMap<StatsKey, ReqStats> statsKeyStatsConcurrentMap, Date date) {
        for (Map.Entry<StatsKey, ReqStats> entry : statsKeyStatsConcurrentMap.entrySet()) {
            StatsKey k = entry.getKey();
            ReqStats s = entry.getValue();

            String name = k.getName();
            Map<String,String> tags = new HashMap<String, String>(k.getTags());

            if (k.reportCount()) {
                metricer.log(name + ".req.count", s.getCount(), tags, date);
            }

            if (k.reportCost()) {
                //Cost
                tags = new HashMap<String, String>(k.getTags());
                tags.put("type", "avg");
                metricer.log(name + ".req.cost", s.getAvgCost(), tags, date);

                tags = new HashMap<String, String>(k.getTags());
                tags.put("type", "min");
                metricer.log(name + ".req.cost", s.getMinCost(), tags, date);

                tags = new HashMap<String, String>(k.getTags());
                tags.put("type", "max");
                metricer.log(name + ".req.cost", s.getMaxCost(), tags, date);

                //Cost Distribution
                for (Map.Entry<String, AtomicInteger> ce : s.getTimeSlotCountMap().entrySet()) {
                    String range = ce.getKey();
                    int rangeCount = ce.getValue().get();

                    tags = new HashMap<String, String>(k.getTags());
                    tags.put("range", range);
                    metricer.log(name + ".req.cost.distribution.count", rangeCount, tags, date);

                    tags = new HashMap<String, String>(k.getTags());
                    tags.put("range", range);
                    metricer.log(name + ".req.cost.distribution.rate", (rangeCount*100)/s.getCount(), tags, date);
                }
            }

            if (k.reportSize()) {
                //Size
                tags = new HashMap<String, String>(k.getTags());
                tags.put("type", "avg");
                metricer.log(name + ".req.size", s.getAvgSize(), tags, date);

                tags = new HashMap<String, String>(k.getTags());
                tags.put("type", "min");
                metricer.log(name + ".req.size", s.getMinSize(), tags, date);

                tags = new HashMap<String, String>(k.getTags());
                tags.put("type", "max");
                metricer.log(name + ".req.size", s.getMaxSize(), tags, date);

                //Size Distribution
                for (Map.Entry<String, AtomicInteger> ce : s.getSizeSlotCountMap().entrySet()) {
                    String range = ce.getKey();
                    int rangeCount = ce.getValue().get();

                    tags = new HashMap<String, String>(k.getTags());
                    tags.put("range", range);
                    metricer.log(name + ".req.size.distribution.count", rangeCount, tags, date);

                    tags = new HashMap<String, String>(k.getTags());
                    tags.put("range", range);
                    metricer.log(name + ".req.size.distribution.rate", (rangeCount*100)/s.getCount(), tags, date);
                }

            }

            if (k.reportStatus()) {
                //Status Distribution
                for (Map.Entry<String, AtomicInteger> ce : s.getStatusSlotCountMap().entrySet()) {
                    String range = ce.getKey();
                    int rangeCount = ce.getValue().get();

                    tags = new HashMap<String, String>(k.getTags());
                    tags.put("range", range);
                    metricer.log(name + ".req.status.distribution.count", rangeCount, tags, date);

                    tags = new HashMap<String, String>(k.getTags());
                    tags.put("range", range);
                    metricer.log(name + ".req.status.distribution.rate", (rangeCount*100)/s.getCount(), tags, date);
                }

            }
        }
    }
}