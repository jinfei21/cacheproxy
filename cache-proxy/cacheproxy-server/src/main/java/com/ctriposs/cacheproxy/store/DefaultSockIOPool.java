package com.ctriposs.cacheproxy.store;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.ctriposs.cacheproxy.common.PoolStats;
import com.ctriposs.cacheproxy.common.SockIO;
import com.ctriposs.cacheproxy.common.SockIOFactory;
import com.ctriposs.cacheproxy.common.SockIOPool;
import com.ctriposs.cacheproxy.util.Args;



public class DefaultSockIOPool implements SockIOPool{
	
    private volatile int minSocks;
    private volatile int maxSocks;
    private volatile long sockTTL;

    private volatile long leaseTimeout = 1000;

    private AtomicInteger pending = new AtomicInteger(0);

    private Lock lock = new ReentrantLock();
    private Condition condition = lock.newCondition();

    private LinkedList<SockIO> free = new LinkedList<SockIO>();
    private Set<SockIO> leased = new HashSet<SockIO>();

    private SockIOFactory sockFactory;

    public DefaultSockIOPool(int minSocks, int maxSocks, long sockTTL, long leaseTimeout, SockIOFactory sockFactory) {
        this.minSocks = Args.positive(minSocks, "minSocks");
        this.maxSocks = Args.positive(maxSocks, "maxSocks");
        this.sockTTL = Args.positive(sockTTL, "sockTTL");
        this.leaseTimeout = Args.notNegative(leaseTimeout, "leaseTimeout");
        this.sockFactory = Args.notNull(sockFactory,"sockFactory");
    }

	@Override
	public SockIO lease() throws Exception {
        return lease(leaseTimeout);
	}

	@Override
	public SockIO lease(long timeout) throws Exception {
        pending.incrementAndGet();
        lock.lock();
        try {
        	SockIO sock = null;
            Date deadLine = new Date(System.currentTimeMillis() + timeout);

            while (true) {
                long now = System.currentTimeMillis();

                //If the count of existing sock is less than minSocks, create new one and return.
                if (_sockCount() < minSocks) {
                    return _createAndLease();
                }

                //Try to get a sock from the free list.
                while ((sock = free.pollFirst()) != null) {
                    if (!sock.isConnected()) {
                        continue;
                    } else if (sock.createTime() + sockTTL < now) {
                    	sock.close();
                    } else {
                        leased.add(sock);
                        return sock;
                    }
                }

                //Free list is empty, try to create new one if doesn't reach the upper limit maxSocks.
                if (_sockCount() < maxSocks) {
                    return _createAndLease();
                }

                condition.awaitUntil(deadLine);

                //Try to get again if doesn't reach the deadLine, or return by throwing a TimeoutException.
                if (deadLine.getTime() <= System.currentTimeMillis()) {
                    throw new TimeoutException("Timeout waiting for connection");
                }
            }
        } finally {
            lock.unlock();
            pending.decrementAndGet();
        }
	}

    @Override
    public PoolStats poolStats() {
        return new PoolStats(leased.size(), free.size(), pending.get(), minSocks, maxSocks);
    }

    private int _sockCount() {
        return free.size() + leased.size();
    }

    private SockIO _createAndLease() throws Exception {
    	SockIO sock;
    	sock = sockFactory.create();
        leased.add(sock);
        return sock;
    }
    
	@Override
	public void release(SockIO sock) {
		
        lock.lock();
        try {
            long now = System.currentTimeMillis();

            leased.remove(sock);

            if (!sock.isConnected()) {
                return;
            } else if (sock.createTime() + sockTTL < now) {
            	try {
					sock.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
            } else {
                free.add(sock);
            }
            condition.signal();
        } finally {
            lock.unlock();
        }
	}

	@Override
	public void closeAllSockIO() {
        lock.lock();
        try {
            for (SockIO sock : free) {
            	try {
					sock.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
            }
            free.clear();
            for (SockIO sock : leased) {
            	try {
					sock.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
            }
            leased.clear();
        } finally {
            lock.unlock();
        }
		
	}
	
    public int getMinSocks() {
        return minSocks;
    }

    public void setMinSocks(int minSocks) {
        this.minSocks = Args.positive(minSocks, "minSocks");
    }

    public int getMaxSocks() {
        return maxSocks;
    }

    public void setMaxSocks(int maxSocks) {
        this.maxSocks = Args.positive(maxSocks, "maxSocks");
    }

    public long getSockTTL() {
        return sockTTL;
    }

    public void setSockTTL(long sockTTL) {
        this.sockTTL = Args.positive(sockTTL, "sockTTL");
    }

    public long getLeaseTimeout() {
        return leaseTimeout;
    }

    public void setLeaseTimeout(long leaseTimeout) {
        this.leaseTimeout = Args.notNegative(leaseTimeout, "leaseTimeout");
    }

    public SockIOFactory getSockIOFactory() {
        return sockFactory;
    }

}
