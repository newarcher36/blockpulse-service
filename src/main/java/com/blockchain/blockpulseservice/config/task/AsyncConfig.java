package com.blockchain.blockpulseservice.config.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Bean(name = {"applicationTaskExecutor", "taskExecutor"})
    public TaskExecutor applicationTaskExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(1);
        exec.setMaxPoolSize(1);
        exec.setQueueCapacity(Integer.MAX_VALUE);
        exec.setThreadNamePrefix("events-");
        exec.initialize();
        return exec;
    }

    @Override
    public TaskExecutor getAsyncExecutor() {
        return applicationTaskExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new AsyncUncaughtExceptionHandler() {
            @Override
            public void handleUncaughtException(Throwable ex, Method method, Object... params) {
                log.error("Async error in {} with params {}: {}", method, params, ex.toString(), ex);
            }
        };
    }
}

