package com.ctriposs.cacheproxy.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.AbstractMatcherFilter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * @author:yjfei
 * @date: 2/26/2015.
 */
public class LogLevelFilter  extends AbstractMatcherFilter<ILoggingEvent> {
    private String level;

    private Level lowLevel;

    @Override
    public void start() {
        lowLevel = Level.toLevel(level, Level.WARN);
        super.start();
    }

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (!isStarted()) {
            return FilterReply.NEUTRAL;
        }

        if (event.getLevel().levelInt >= lowLevel.levelInt) {
            return FilterReply.ACCEPT;
        } else {
            return FilterReply.DENY;
        }
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }
}
