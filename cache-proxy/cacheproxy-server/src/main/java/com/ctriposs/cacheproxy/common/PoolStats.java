package com.ctriposs.cacheproxy.common;
public class PoolStats {
    private int leased;
    private int free;
    private int pending;
    private int min;
    private int max;

    public PoolStats(int leased, int free, int pending, int min, int max) {
        this.leased = leased;
        this.free = free;
        this.pending = pending;
        this.min = min;
        this.max = max;
    }

    public int getLeased() {
        return leased;
    }

    public int getFree() {
        return free;
    }

    public int getPending() {
        return pending;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    @Override
    public String toString() {
        return "PoolStats{" +
                "leased=" + leased +
                ", free=" + free +
                ", pending=" + pending +
                ", min=" + min +
                ", max=" + max +
                '}';
    }
}