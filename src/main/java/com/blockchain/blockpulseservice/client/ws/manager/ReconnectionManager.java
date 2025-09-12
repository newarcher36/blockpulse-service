package com.blockchain.blockpulseservice.client.ws.manager;


import com.blockchain.blockpulseservice.config.ws.WebSocketReconnectionProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class ReconnectionManager {

    private final ScheduledExecutorService scheduler;
    private final RetryTemplate retryTemplate;
    private final WebSocketReconnectionProperties properties;
    private ScheduledFuture<?> reconnectTask;

    public ReconnectionManager(ScheduledExecutorService scheduler,
                               RetryTemplate retryTemplate,
                               WebSocketReconnectionProperties properties) {
        this.scheduler = scheduler;
        this.retryTemplate = retryTemplate;
        this.properties = properties;
    }

    public void scheduleReconnect(Runnable reconnectCallback, URI serverUri) {
        int initialDelay = Math.max(0, properties.initialDelaySeconds());
        log.info("Scheduling reconnection in {}s...", initialDelay);
        reconnectTask = scheduler.schedule(() -> {
            retryTemplate.execute(context -> {
                log.info("Reconnect attempt {} for {}", context.getRetryCount() + 1, serverUri);
                reconnectCallback.run();
                return null;
            });
        }, initialDelay, TimeUnit.SECONDS);
    }

    public void cancelReconnect() {
        if (reconnectTask != null && !reconnectTask.isDone()) {
            reconnectTask.cancel(false);
        }
    }
}
