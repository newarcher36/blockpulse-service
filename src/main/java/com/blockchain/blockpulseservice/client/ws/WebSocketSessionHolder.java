package com.blockchain.blockpulseservice.client.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.URI;

@Slf4j
@Component
public class WebSocketSessionHolder {
    private volatile WebSocketSession session;

    public WebSocketSession get() {
        return session;
    }

    public void set(WebSocketSession session) {
        this.session = session;
    }

    public boolean isOpen() {
        return this.session != null && this.session.isOpen();
    }

    public WebSocketSession getSessionIfOpen() {
        return isOpen() ? session : null;
    }

    public void closeIfOpen(CloseStatus status, URI serverUri) {
        if (!isOpen()) return;
        try {
            session.close(status);
        } catch (IOException e) {
            log.error("Error closing WebSocket session for {}", serverUri, e);
        }
    }
}