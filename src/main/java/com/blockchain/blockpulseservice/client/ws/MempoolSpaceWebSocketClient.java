package com.blockchain.blockpulseservice.client.ws;

import com.blockchain.blockpulseservice.client.ws.manager.ConnectionStateManager;
import com.blockchain.blockpulseservice.client.ws.manager.ReconnectionManager;
import com.blockchain.blockpulseservice.mapper.TransactionMapper;
import com.blockchain.blockpulseservice.model.dto.MempoolTransactionsDTOWrapper;
import com.blockchain.blockpulseservice.service.sliding_window.SlidingWindowManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;

import java.net.URI;


@Slf4j
@Component
public class MempoolSpaceWebSocketClient extends BaseWebSocketSessionClient {
    private final TransactionMapper transactionMapper;
    private final ObjectMapper objectMapper;
    private final SlidingWindowManager slidingWindowManager;

    public MempoolSpaceWebSocketClient(TransactionMapper transactionMapper,
                                       ObjectMapper objectMapper,
                                       WebSocketClient webSocketClient,
                                       ConnectionStateManager connectionState,
                                       ReconnectionManager reconnectionManager,
                                       WebSocketMessageHandler messageHandler,
                                       WebSocketMessageSender messageSender,
                                       @Value("${app.mempool.space.websocket.track-mempool-api-url}") String serverUri,
                                       @Value("${app.websocket.message-size-limit}") int messageSizeLimit,
                                       SlidingWindowManager slidingWindowManager) {
        super(URI.create(serverUri),
                messageSizeLimit,
                webSocketClient,
                connectionState,
                reconnectionManager,
                messageHandler,
                messageSender);
        this.transactionMapper = transactionMapper;
        this.objectMapper = objectMapper;
        this.slidingWindowManager = slidingWindowManager;
    }

    @Override
    protected void onConnectionEstablished(WebSocketSession session) {
        log.info("Connected to {}", serverUri);
        subscribeToTrackMempoolTransactions();
    }

    @Override
    protected void processMessage(String message) {
        log.debug("Processing message: {}", message.substring(0, Math.min(200, message.length())));

        try {
            var txWrapper = objectMapper.readValue(message, MempoolTransactionsDTOWrapper.class);
            var mempoolTransactions = txWrapper.mempoolTransactions();
            if (mempoolTransactions != null) {
                var txs = transactionMapper.mapToTransaction(mempoolTransactions.added());
                slidingWindowManager.addTransaction(txs);
            }
        } catch (Exception e) {
            log.error("Error processing blockchain.info message: {}", message, e);
        }
    }

    private void subscribeToTrackMempoolTransactions() {
        // TODO: to properties.
        var subscribeMessage = "{ \"track-mempool\": true }";
        sendMessage(subscribeMessage);
        log.info("Subscribed to track mempool transactions");
    }
}