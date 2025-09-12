package com.blockchain.blockpulseservice.config.ws;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.websocket.reconnect")
public record WebSocketReconnectionProperties(
        int maxAttempts,
        int initialDelaySeconds,
        int maxDelaySeconds
) {}