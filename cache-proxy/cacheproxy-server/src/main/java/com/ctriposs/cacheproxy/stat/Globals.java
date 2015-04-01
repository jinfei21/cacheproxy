package com.ctriposs.cacheproxy.stat;

import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;

public class Globals {
    public static ReqStatsCollector REQ_STATS_COLLECTOR = new ReqStatsCollector();
    static {
        final DynamicIntProperty reportInterval = DynamicPropertyFactory.getInstance().getIntProperty("req.metrics.interval", 1000*60);
        REQ_STATS_COLLECTOR.setReportInterval(reportInterval.get());
        reportInterval.addCallback(new Runnable() {
            @Override
            public void run() {
                REQ_STATS_COLLECTOR.setReportInterval(reportInterval.get());
            }
        });

    }
}