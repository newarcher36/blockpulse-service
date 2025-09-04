package com.blockchain.blockpulseservice.config.rest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class RestClientConfig {
    @Bean
    public RestTemplate restClient() {
        return new RestTemplate();
    }

    @Bean(name = "mempoolRestExecutor")
    public Executor mempoolRestExecutor() {
        return Executors.newFixedThreadPool(2);
    }
}
