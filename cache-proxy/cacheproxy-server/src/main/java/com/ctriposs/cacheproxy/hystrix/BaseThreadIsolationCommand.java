package com.ctriposs.cacheproxy.hystrix;

import com.netflix.hystrix.*;

/**
 * @author:yjfei
 * @date: 2/26/2015.
 */
public abstract class BaseThreadIsolationCommand<R> extends HystrixCommand<R> {
    protected BaseThreadIsolationCommand(String commandGroup, String commandKey,String threadPoolKey) {
        super(Setter
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey(commandGroup))
                .andCommandKey(HystrixCommandKey.Factory.asKey(commandKey))
                .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(threadPoolKey))
                .andCommandPropertiesDefaults(
                        HystrixCommandProperties.Setter()
                                .withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.THREAD)
                ));
    }
}
