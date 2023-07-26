package com.ziptooss.platform.zip.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author canxiusi.yan
 * @description ThreadPoolConfig 微服务线程池
 * @date 2022/2/01 11:34
 */
@Configuration
public class ThreadPoolConfig {

    /**
     * 自定义线程池
     *
     * @return
     */
    @Bean("thread-pool")
    public static ThreadPoolExecutor threadPoolExecutor() {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(200, 400, 1000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(1000),
                new NamedThreadFactory("oss-service"), new ThreadPoolExecutor.CallerRunsPolicy());
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        return threadPoolExecutor;
    }

}
