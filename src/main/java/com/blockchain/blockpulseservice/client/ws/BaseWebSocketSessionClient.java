package com.blockchain.blockpulseservice.client.ws;

import com.blockchain.blockpulseservice.client.ws.manager.ConnectionStateManager;
import com.blockchain.blockpulseservice.client.ws.manager.ReconnectionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;

import java.io.IOException;
import java.net.URI;

@Slf4j
public abstract class BaseWebSocketSessionClient implements WebSocketHandler {
    private final WebSocketSessionHolder sessionHolder;
    protected final URI serverUri;
    private final WebSocketClient webSocketClient;
    private final ConnectionStateManager connectionState;
    private final ReconnectionManager reconnectionManager;
    private final WebSocketMessageSender messageSender;
    private final int messageSizeLimit;

    protected BaseWebSocketSessionClient(URI serverUri,
                                         int messageSizeLimit,
                                         WebSocketClient webSocketClient,
                                         ConnectionStateManager connectionState,
                                         ReconnectionManager reconnectionManager,
                                         WebSocketMessageSender messageSender,
                                         WebSocketSessionHolder sessionHolder) {
        this.serverUri = serverUri;
        this.messageSizeLimit = messageSizeLimit;
        this.webSocketClient = webSocketClient;
        this.connectionState = connectionState;
        this.reconnectionManager = reconnectionManager;
        this.messageSender = messageSender;
        this.sessionHolder = sessionHolder;
    }


    public void connect() {
        if (isConnected()) {
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

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        this.sessionHolder.set(session);
        session.setTextMessageSizeLimit(messageSizeLimit);
        connectionState.setConnected(true);

        log.info("WebSocket connected to: {}", serverUri);

        try {
            onConnectionEstablished(session);
        } catch (Exception e) {
            log.error("Error in connection established callback", e);
        }
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
        handleConnectionLoss();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        log.warn("WebSocket connection closed for {}: {} - {}", serverUri, closeStatus.getCode(), closeStatus.getReason());
        handleConnectionLoss();
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    protected void sendMessage(String message) {
        messageSender.send(sessionHolder.getSessionIfOpen(), message, serverUri, this::connect);
    }

    protected abstract void onConnectionEstablished(WebSocketSession session);

    protected abstract void processMessage(String message);

    public void disconnect() {
        reconnectionManager.cancelReconnect();
        closeSession();
        connectionState.setConnected(false);
        log.info("Disconnected from {}", serverUri);
    }

    private void handleConnectionLoss() {
        connectionState.setConnected(false);
        reconnectionManager.scheduleReconnect(this::connect, serverUri);
    }

    private void closeSession() {
        sessionHolder.closeIfOpen(CloseStatus.NORMAL, serverUri);
    }
    private boolean isConnected() {
        return connectionState.isConnected() && sessionHolder.isOpen();
    }
}