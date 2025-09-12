package com.blockchain.blockpulseservice.client.ws;

import com.blockchain.blockpulseservice.client.ws.manager.ConnectionStateManager;
import com.blockchain.blockpulseservice.client.ws.manager.ReconnectionManager;
import com.blockchain.blockpulseservice.client.ws.mapper.TransactionMapper;
import com.blockchain.blockpulseservice.model.domain.Transaction;
import com.blockchain.blockpulseservice.model.dto.MempoolTransactionsDTOWrapper;
import com.blockchain.blockpulseservice.model.event.NewTransactionEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
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
    private WebSocketMessageSender messageSender;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private WebSocketSessionHolder sessionHolder;
    @Mock
    private WebSocketSession session;
    @Captor
    private ArgumentCaptor<NewTransactionEvent> applicationEventCaptor;

    private MempoolSpaceWebSocketClient mempoolSpaceWebSocketClient;
    private static final String SERVER_URI = "wss://example.test/ws";
    private static final int MSG_LIMIT = 1024;
    private static final String SUBSCRIBE_MSG = "{\"track-mempool\": true}";


    @BeforeEach
    void setUp() {
        mempoolSpaceWebSocketClient = new MempoolSpaceWebSocketClient(
                transactionMapper,
                objectMapper,
                webSocketClient,
                connectionState,
                reconnectionManager,
                messageSender,
                sessionHolder,
                SERVER_URI,
                MSG_LIMIT,
                true,
                0,
                eventPublisher,
                SUBSCRIBE_MSG
        );
    }

    @Test
    void doesNothingWhenAlreadyConnected() {
        when(connectionState.isConnected()).thenReturn(true);
        when(sessionHolder.isOpen()).thenReturn(true);

        mempoolSpaceWebSocketClient.start();

        verify(connectionState).isConnected();
        verify(sessionHolder).isOpen();
        verifyNoInteractions(webSocketClient);
        verifyNoInteractions(reconnectionManager);
    }

    @Test
    void connectsWhenConnectionStateIsNotConnected() {
        when(connectionState.isConnected()).thenReturn(false);
        when(webSocketClient.execute(any(), isNull(), any(URI.class))).thenReturn(mock(CompletableFuture.class));

        mempoolSpaceWebSocketClient.start();

        verify(connectionState).isConnected();
        verify(webSocketClient).execute(any(WebSocketHandler.class), isNull(), eq(URI.create(SERVER_URI)));
        verifyNoInteractions(reconnectionManager);
    }

    @Test
    void connectsWhenSessionOpenAndNotConnected() {
        when(connectionState.isConnected()).thenReturn(true);
        when(sessionHolder.isOpen()).thenReturn(false);
        when(webSocketClient.execute(any(), isNull(), any(URI.class))).thenReturn(mock(CompletableFuture.class));

        mempoolSpaceWebSocketClient.start();

        verify(connectionState).isConnected();
        verify(sessionHolder).isOpen();
        verify(webSocketClient).execute(any(WebSocketHandler.class), isNull(), eq(URI.create(SERVER_URI)));
        verifyNoInteractions(reconnectionManager);
    }


    @Test
    void afterConnectionEstablishedSetsLimitMarksConnectedAndSendsSubscribe() {
        when(sessionHolder.getSessionIfOpen()).thenReturn(session);

        mempoolSpaceWebSocketClient.afterConnectionEstablished(session);

        verify(session).setTextMessageSizeLimit(MSG_LIMIT);
        verify(connectionState).setConnected(true);
        verify(messageSender).send(eq(session), eq(SUBSCRIBE_MSG), eq(URI.create(SERVER_URI)), any(Runnable.class));
        verify(sessionHolder).set(session);
    }

    @Test
    void handleAndProcessMessage() throws JsonProcessingException {
        var dto1 = new MempoolTransactionsDTOWrapper.MempoolTransactionsDTO.TransactionDTO("tx1", 100, new BigDecimal("1000"), new BigDecimal("10"), Instant.parse("1970-01-01T00:00:00Z"));
        var dto2 = new MempoolTransactionsDTOWrapper.MempoolTransactionsDTO.TransactionDTO("tx2", 200, new BigDecimal("2000"), new BigDecimal("20"), Instant.parse("1970-01-01T00:00:10Z"));
        when(objectMapper.readValue(anyString(), eq(MempoolTransactionsDTOWrapper.class))).thenReturn(new MempoolTransactionsDTOWrapper(new MempoolTransactionsDTOWrapper.MempoolTransactionsDTO(List.of(dto1, dto2))));
        var tx1 = new Transaction("tx1", new BigDecimal("1000"), BigDecimal.ZERO, 100, Instant.EPOCH);
        var tx2 = new Transaction("tx2", new BigDecimal("2000"), BigDecimal.ZERO, 200, Instant.EPOCH);
        when(transactionMapper.mapToTransaction(anyList())).thenReturn(List.of(tx1, tx2));
        doNothing().when(eventPublisher).publishEvent(any(NewTransactionEvent.class));

        mempoolSpaceWebSocketClient.handleMessage(null, new TextMessage("anyMessage"));

        verify(objectMapper).readValue("anyMessage", MempoolTransactionsDTOWrapper.class);
        verify(transactionMapper).mapToTransaction(List.of(dto1, dto2));
        verify(eventPublisher, times(2)).publishEvent(applicationEventCaptor.capture());
        assertThat(applicationEventCaptor.getAllValues())
                .hasSize(2)
                .extracting(NewTransactionEvent::transaction)
                .containsExactlyInAnyOrder(tx1, tx2);
    }

    @Test
    void handleTransportErrorClosesSession() {
        mempoolSpaceWebSocketClient.handleTransportError(null, new RuntimeException("boom"));

        verify(sessionHolder).closeIfOpen(eq(CloseStatus.SERVER_ERROR), eq(URI.create(SERVER_URI)));
    }

    @Test
    void afterConnectionClosed_marksDisconnected_andSchedulesReconnect() {
        mempoolSpaceWebSocketClient.afterConnectionClosed(null, CloseStatus.NORMAL);

        verify(connectionState).setConnected(false);
        verify(reconnectionManager).scheduleReconnect(any(Runnable.class), eq(URI.create(SERVER_URI)));
    }

    @Test
    void supportsPartialMessages_returnsFalse() {
        assertThat(mempoolSpaceWebSocketClient.supportsPartialMessages()).isFalse();
    }

    @Test
    void stopInvokesDisconnectAndPreventsReconnectOnSubsequentClose() {
        mempoolSpaceWebSocketClient.stop();

        verify(reconnectionManager).cancelReconnect();
        verify(sessionHolder).closeIfOpen(eq(CloseStatus.NORMAL), eq(URI.create(SERVER_URI)));
        verify(connectionState).setConnected(false);

        // Simulate late close after stop; should not schedule reconnect
        mempoolSpaceWebSocketClient.afterConnectionClosed(null, CloseStatus.GOING_AWAY);
        verifyNoMoreInteractions(reconnectionManager);
    }

    @Test
    void stopWithCallbackRunsCallback() {
        var called = new AtomicBoolean(false);
        mempoolSpaceWebSocketClient.stop(() -> called.set(true));
        assertThat(called.get()).isTrue();
    }

    @Test
    void isRunningReflectsConnectionAndSessionState() {
        when(connectionState.isConnected()).thenReturn(true);
        when(sessionHolder.isOpen()).thenReturn(true);
        assertThat(mempoolSpaceWebSocketClient.isRunning()).isTrue();

        when(sessionHolder.isOpen()).thenReturn(false);
        assertThat(mempoolSpaceWebSocketClient.isRunning()).isFalse();
    }

    @Test
    void lifecyclePropsAutoStartupAndPhase() {
        assertThat(mempoolSpaceWebSocketClient.isAutoStartup()).isTrue();
        assertThat(mempoolSpaceWebSocketClient.getPhase()).isZero();
    }
}
