package com.blockchain.blockpulseservice.client.ws;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.URI;

@Slf4j
@Component
public class WebSocketMessageSender {
    public void send(WebSocketSession session, String message, URI serverUri, Runnable onSendFailure) {
        if (session == null || !session.isOpen()) {
            log.warn("Cannot send message - session not open for {}", serverUri);
            return;
        }

        try {
            session.sendMessage(new TextMessage(message));
            log.debug("Sent message to {}: {}", serverUri, message);
        } catch (IOException e) {
            log.error("Failed to send message to {}: {}", serverUri, message, e);
            onSendFailure.run();
        }
    }
}