package com.blockchain.blockpulseservice.client.ws;

import com.blockchain.blockpulseservice.BaseIT;
import com.blockchain.blockpulseservice.model.domain.Transaction;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@TestPropertySource(properties = {
        "app.websocket.auto-start.enabled=false",
        "app.websocket.message-size-limit=2048",
        "app.mempool.space.websocket.subscribe-message={ \"track-mempool\": true }",
        // Speed up reconnection policy
        "app.websocket.reconnect.max-attempts=5",
        "app.websocket.reconnect.initial-delay-seconds=0",
        "app.websocket.reconnect.max-delay-seconds=1",
        // Shorten graceful shutdown in ITs (optional safeguard)
        "spring.lifecycle.timeout-per-shutdown-phase=5s",
        "spring.main.allow-bean-definition-overriding=true"
})
class WebSocketReconnectIT extends BaseIT {

    private static final String TXS1 = """
            {
              "mempool-transactions": {
                "added": [
                  {
                    "txid": "tx-first",
                    "vsize": 100,
                    "fee": 1000,
                    "feePerVsize": 10,
                    "firstSeen": "1970-01-01T00:00:00Z"
                  }
                ]
              }
            }
            """;

    private static final String TXS2 = """
            {
              "mempool-transactions": {
                "added": [
                  {
                    "txid": "tx-second",
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
    private static final AtomicInteger connections = new AtomicInteger();

    @BeforeAll
    static void startWsServer() {
        server = HttpServer.create()
                .port(0)
                .route(routes -> routes.ws("/ws", (in, out) -> {
                    int attempt = connections.incrementAndGet();
                    if (attempt == 1) {
                        // First connection: send one message and then complete (close) to trigger reconnect
                        return out.sendString(Mono.just(TXS1)).then();
                    } else {
                        // Subsequent connection(s): send and keep open
                        return out.sendString(Mono.just(TXS2)).neverComplete();
                    }
                }))
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
    void reconnectsAfterServerCloseAndProcessesEventsAgain() {
        client.start();

        // After server closed first connection, client should reconnect and receive the second event
        verify(slidingWindowManager, timeout(5000).times(2)).onNewTransaction(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues())
                .hasSize(2)
                .containsExactly(
                        new NewTransactionEvent(tx("tx-first", "10", "1000", 100, "1970-01-01T00:00:00Z")),
                        new NewTransactionEvent(tx("tx-second", "20","2000", 200, "1970-01-01T00:00:10Z")));
    }

    private static Transaction tx(String id, String feePerVSize, String totalFee, int vSize, String time) {
        return new Transaction(id, new BigDecimal(feePerVSize), new BigDecimal(totalFee), vSize, Instant.parse(time));
    }
}

