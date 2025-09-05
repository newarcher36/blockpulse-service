package com.blockchain.blockpulseservice.client.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

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
}