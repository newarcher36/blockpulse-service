package com.blockchain.blockpulseservice.client.ws.manager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class ConnectionStateManager {
    private final AtomicBoolean connected = new AtomicBoolean(false);

    public boolean isConnected() {
        return connected.get();
    }
    
    public void setConnected(boolean connected) {
        this.connected.set(connected);
        log.debug("Connection state changed to: {}", connected);
    }
}