package com.blockchain.blockpulseservice.client.ws;

import com.blockchain.blockpulseservice.BaseIT;
import com.blockchain.blockpulseservice.model.event.NewTransactionEvent;
import com.blockchain.blockpulseservice.service.sliding_window.SlidingWindowManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@TestPropertySource(properties = {
        "app.websocket.message-size-limit=2048",
        "app.websocket.auto-start.enabled=false",
        "app.mempool.space.websocket.subscribe-message={ \"track-mempool\": true }",
        "spring.main.allow-bean-definition-overriding=true"
})
class MempoolSpaceWebSocketClientIT extends BaseIT {
    private static final String TXS = """
            {
              "mempool-transactions": {
                "added": [
                  {
                    "txid": "tx1",
                    "vsize": 100,
                    "fee": 1000,
                    "feePerVsize": 10,
                    "firstSeen": "1970-01-01T00:00:00Z"
                  },
                  {
                    "txid": "tx2",
                    "vsize": 200,
                    "fee": 2000,
                    "feePerVsize": 20,
                    "firstSeen": "1970-01-01T00:00:10Z"
                  }
                ]
              }
            }
            """;
    @Autowired
    private MempoolSpaceWebSocketClient client;
    @MockitoSpyBean
    private SlidingWindowManager slidingWindowManager;
    @Captor
    private ArgumentCaptor<NewTransactionEvent> eventCaptor;
    private static DisposableServer server;
    private static String wsUri;

    @BeforeAll
    static void startWsServer() {
        server = HttpServer.create()
                .port(0) // random port
                .route(routes -> routes.ws("/ws", (in, out) ->
                        out.sendString(Mono.just(TXS)).neverComplete())
                )
                .bindNow();

        wsUri = "ws://localhost:" + server.port() + "/ws";
    }

    @AfterAll
    static void stopWsServer() {
        if (server != null) server.disposeNow();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("app.mempool.space.websocket.track-mempool-api-url", () -> wsUri);
    }

    @TestConfiguration
    static class SyncAsyncConfig {
        @Bean(name = "taskExecutor")
        TaskExecutor taskExecutor() {
            return Runnable::run;
        }
    }

    @Test
    void connectAndPublishesNewTransactionEvents() {
        client.start();

        verify(slidingWindowManager, timeout(500).times(2)).onNewTransaction(eventCaptor.capture());
        var actualTxs = eventCaptor.getAllValues();
        assertThat(actualTxs)
                .first()
                .extracting(NewTransactionEvent::transaction)
                .hasFieldOrPropertyWithValue("id", "tx1")
                .hasFieldOrPropertyWithValue("vSize", 100)
                .hasFieldOrPropertyWithValue("totalFee", new BigDecimal(1000))
                .hasFieldOrPropertyWithValue("feePerVSize", new BigDecimal("10"))
                .hasFieldOrPropertyWithValue("time", Instant.parse("1970-01-01T00:00:00Z"));
        assertThat(actualTxs)
                .last()
                .extracting(NewTransactionEvent::transaction)
                .hasFieldOrPropertyWithValue("id", "tx2")
                .hasFieldOrPropertyWithValue("vSize", 200)
                .hasFieldOrPropertyWithValue("totalFee", new BigDecimal(2000))
                .hasFieldOrPropertyWithValue("feePerVSize", new BigDecimal("20"))
                .hasFieldOrPropertyWithValue("time", Instant.parse("1970-01-01T00:00:10Z"));
    }
}