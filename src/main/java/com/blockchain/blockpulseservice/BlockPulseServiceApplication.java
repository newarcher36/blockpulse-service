package com.blockchain.blockpulseservice;

import com.blockchain.blockpulseservice.config.ws.WebSocketReconnectionProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(WebSocketReconnectionProperties.class)
public class BlockPulseServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BlockPulseServiceApplication.class, args);
    }
}