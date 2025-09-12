package com.blockchain.blockpulseservice.config.ws;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.client.WebSocketClient;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class WebSocketClientConfig {
    
    @Bean
    public WebSocketClient webSocketClient() {
        return new StandardWebSocketClient();
    }
    
    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService webSocketScheduler() {
        int processors = Runtime.getRuntime().availableProcessors();
        return Executors.newScheduledThreadPool(processors);
    }

    @Bean
    public RetryTemplate retryTemplate(WebSocketReconnectionProperties properties) {
        RetryTemplate retryTemplate = new RetryTemplate();

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(properties.initialDelaySeconds() * 1000L);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(properties.maxDelaySeconds() * 1000L);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(properties.maxAttempts());
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }
}
