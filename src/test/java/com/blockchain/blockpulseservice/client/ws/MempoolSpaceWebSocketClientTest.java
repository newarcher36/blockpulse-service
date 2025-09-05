package com.blockchain.blockpulseservice.client.ws;

import com.blockchain.blockpulseservice.client.ws.manager.ConnectionStateManager;
import com.blockchain.blockpulseservice.client.ws.manager.ReconnectionManager;
import com.blockchain.blockpulseservice.client.ws.mapper.TransactionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MempoolSpaceWebSocketClientTest {
    @Mock
    private TransactionMapper transactionMapper;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private WebSocketClient webSocketClient;
    @Mock
    private ConnectionStateManager connectionState;
    @Mock
    private ReconnectionManager reconnectionManager;
    @Mock
    private WebSocketMessageHandler messageHandler;
    @Mock
    private WebSocketMessageSender messageSender;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private WebSocketSessionHolder sessionHolder;
    @Mock
    private WebSocketSession session;

    private MempoolSpaceWebSocketClient client;
    private static final String SERVER_URI = "wss://example.test/ws";
    private static final int MSG_LIMIT = 1024;
    private static final String SUBSCRIBE_MSG = "{\"track-mempool\": true}";


    @BeforeEach
    void setUp() {
        client = new MempoolSpaceWebSocketClient(
                transactionMapper,
                objectMapper,
                webSocketClient,
                connectionState,
                reconnectionManager,
                messageHandler,
                messageSender,
                sessionHolder,
                SERVER_URI,
                MSG_LIMIT,
                eventPublisher,
                SUBSCRIBE_MSG
        );
    }

    @Test
    void doesNothingWhenAlreadyConnected() {
        when(connectionState.isConnected()).thenReturn(true);
        when(sessionHolder.isOpen()).thenReturn(true);

        client.connect();

        verify(connectionState).isConnected();
        verify(sessionHolder).isOpen();
        verifyNoInteractions(webSocketClient);
        verifyNoInteractions(reconnectionManager);
    }

    @Test
    void connectsWhenConnectionStateIsNotConnected() {
        when(connectionState.isConnected()).thenReturn(false);
        when(webSocketClient.execute(any(), isNull(), any(URI.class))).thenReturn(Mockito.mock(CompletableFuture.class));

        client.connect();

        verify(connectionState).isConnected();
        verify(webSocketClient).execute(any(WebSocketHandler.class), isNull(), eq(URI.create(SERVER_URI)));
        verifyNoInteractions(reconnectionManager);
    }

    @Test
    void connectsWhenSessionOpenAndNotConnected() {
        when(connectionState.isConnected()).thenReturn(true);
        when(sessionHolder.isOpen()).thenReturn(false);
        when(webSocketClient.execute(any(), isNull(), any(URI.class))).thenReturn(Mockito.mock(CompletableFuture.class));

        client.connect();

        verify(connectionState).isConnected();
        verify(sessionHolder).isOpen();
        verify(webSocketClient).execute(any(WebSocketHandler.class), isNull(), eq(URI.create(SERVER_URI)));
        verifyNoInteractions(reconnectionManager);
    }


    @Test
    void afterConnectionEstablished_setsLimit_marksConnected_andSendsSubscribe() {
        when(sessionHolder.getSessionIfOpen()).thenReturn(session);

        client.afterConnectionEstablished(session);

        verify(session).setTextMessageSizeLimit(MSG_LIMIT);
        verify(connectionState).setConnected(true);
        verify(messageSender).sendMessage(eq(session), eq(SUBSCRIBE_MSG), eq(URI.create(SERVER_URI)), any(Runnable.class));
        verify(sessionHolder).set(session);
    }
}