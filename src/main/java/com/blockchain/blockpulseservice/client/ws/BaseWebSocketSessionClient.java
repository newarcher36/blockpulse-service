package com.blockchain.blockpulseservice.client.ws;

import com.blockchain.blockpulseservice.client.ws.manager.ConnectionStateManager;
import com.blockchain.blockpulseservice.client.ws.manager.ReconnectionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;

import java.net.URI;

@Slf4j
public abstract class BaseWebSocketSessionClient implements WebSocketHandler, SmartLifecycle {
    private final WebSocketSessionHolder sessionHolder;
    protected final URI serverUri;
    private final WebSocketClient webSocketClient;
    private final ConnectionStateManager connectionState;
    private final ReconnectionManager reconnectionManager;
    private final WebSocketMessageSender messageSender;
    private final int messageSizeLimit;
    private final boolean autoStartup;
    private final int lifecyclePhase;
    private volatile boolean shuttingDown = false;

    protected BaseWebSocketSessionClient(URI serverUri,
                                         int messageSizeLimit,
                                         WebSocketClient webSocketClient,
                                         ConnectionStateManager connectionState,
                                         ReconnectionManager reconnectionManager,
                                         WebSocketMessageSender messageSender,
                                         WebSocketSessionHolder sessionHolder,

                                         boolean autoStartup,
                                         int lifecyclePhase) {
        this.serverUri = serverUri;
        this.messageSizeLimit = messageSizeLimit;
        this.webSocketClient = webSocketClient;
        this.connectionState = connectionState;
        this.reconnectionManager = reconnectionManager;
        this.messageSender = messageSender;
        this.sessionHolder = sessionHolder;
        this.autoStartup = autoStartup;
        this.lifecyclePhase = lifecyclePhase;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        this.sessionHolder.set(session);
        session.setTextMessageSizeLimit(messageSizeLimit);
        connectionState.setConnected(true);
        log.info("WebSocket connected to: {}", serverUri);
        onConnectionEstablished(session);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        if (message instanceof TextMessage textMessage) {
            var payload = textMessage.getPayload();
            log.debug("Received message from {}: {}", serverUri, payload.substring(0, Math.min(200, payload.length())));
            processMessage(payload);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket transport error for {}", serverUri, exception);
        sessionHolder.closeIfOpen(CloseStatus.SERVER_ERROR, serverUri);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        log.error("WebSocket connection closed for {}: {} - {}", serverUri, closeStatus.getCode(), closeStatus.getReason());
        handleConnectionLoss();
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    @Override
    public void start() {
        shuttingDown = false;
        connect();
    }

    @Override
    public void stop() {
        shuttingDown = true;
        reconnectionManager.cancelReconnect();
        sessionHolder.closeIfOpen(CloseStatus.NORMAL, serverUri);
        connectionState.setConnected(false);
        log.info("Disconnected from {}", serverUri);
    }

    @Override
    public void stop(Runnable callback) {
        try {
            stop();
        } finally {
            callback.run();
        }
    }
    @Override
    public boolean isRunning() {
        return connectionState.isConnected() && sessionHolder.isOpen();
    }

    @Override
    public boolean isAutoStartup() {
        return autoStartup;
    }

    @Override
    public int getPhase() {
        return lifecyclePhase;
    }

    protected void sendMessage(String message) {
        messageSender.send(sessionHolder.getSessionIfOpen(), message, serverUri, this::connect);
    }

    protected abstract void onConnectionEstablished(WebSocketSession session);
    protected abstract void processMessage(String message);

    private void connect() {
        if (connectionState.isConnected() && sessionHolder.isOpen()) {
            log.debug("Already connected to {}", serverUri);
            return;
        }
        try {
            log.info("Connecting to WebSocket: {}", serverUri);
            webSocketClient.execute(this, null, serverUri).get();
        } catch (Exception e) {
            log.error("Failed to connect to {}", serverUri, e);
            reconnectionManager.scheduleReconnect(this::connect, serverUri);
        }
    }

    private void handleConnectionLoss() {
        connectionState.setConnected(false);
        if (!shuttingDown) {
            reconnectionManager.scheduleReconnect(this::connect, serverUri);
        } else {
            log.debug("Skipping reconnect for {} due to shutdown in progress", serverUri);
        }
    }
}