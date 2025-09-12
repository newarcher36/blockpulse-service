package com.blockchain.blockpulseservice.client.ws;

import com.blockchain.blockpulseservice.client.ws.manager.ConnectionStateManager;
import com.blockchain.blockpulseservice.client.ws.manager.ReconnectionManager;
import com.blockchain.blockpulseservice.client.ws.mapper.TransactionMapper;
import com.blockchain.blockpulseservice.model.dto.MempoolTransactionsDTOWrapper;
import com.blockchain.blockpulseservice.model.event.NewTransactionEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;

import java.net.URI;


@Slf4j
@Component
public class MempoolSpaceWebSocketClient extends BaseWebSocketSessionClient {
    private final TransactionMapper transactionMapper;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final String subscribeMessage;

    public MempoolSpaceWebSocketClient(TransactionMapper transactionMapper,
                                       ObjectMapper objectMapper,
                                       WebSocketClient webSocketClient,
                                       ConnectionStateManager connectionState,
                                       ReconnectionManager reconnectionManager,
                                       WebSocketMessageSender messageSender,
                                       WebSocketSessionHolder sessionHolder,
                                       @Value("${app.mempool.space.websocket.track-mempool-api-url}") String serverUri,
                                       @Value("${app.websocket.message-size-limit}") int messageSizeLimit,
                                       @Value("${app.websocket.auto-start.enabled:true}") boolean autoStartup,
                                       @Value("${app.websocket.lifecycle.phase:0}") int lifecyclePhase,
                                       ApplicationEventPublisher eventPublisher,
                                       @Value("${app.mempool.space.websocket.subscribe-message:{ \"track-mempool\": true }}") String subscribeMessage) {
        super(URI.create(serverUri),
                messageSizeLimit,
                webSocketClient,
                connectionState,
                reconnectionManager,
                messageSender,
                sessionHolder,
                autoStartup,
                lifecyclePhase);
        this.transactionMapper = transactionMapper;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.subscribeMessage = subscribeMessage;
    }

    @Override
    protected void onConnectionEstablished(WebSocketSession session) {
        log.info("Connected to {}", serverUri);
        sendMessage(this.subscribeMessage);
        log.info("Subscribed to track mempool transactions");
    }

    @Override
    protected void processMessage(String message) {
        log.debug("Processing message: {}", message.substring(0, Math.min(200, message.length())));

        try {
            var txWrapper = objectMapper.readValue(message, MempoolTransactionsDTOWrapper.class);
            var mempoolTransactions = txWrapper.mempoolTransactions();
            transactionMapper
                    .mapToTransaction(mempoolTransactions.added())
                    .forEach(tx -> eventPublisher.publishEvent(new NewTransactionEvent(tx)));
        } catch (Exception e) {
            log.error("Error processing blockchain.info message: {}", message, e);
        }
    }
}
