package com.blockchain.blockpulseservice.e2e;

import com.blockchain.blockpulseservice.BaseIT;
import com.blockchain.blockpulseservice.model.domain.PriceTier;
import com.blockchain.blockpulseservice.model.event.AnalyzedTransactionEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@TestPropertySource(
        properties = {
                "app.stream.sse.sample-ms=100",
                "app.websocket.auto-start.enabled=true",
                "app.analysis.tx.sliding-window-size=2","server.shutdown=immediate"
        })
@AutoConfigureWebTestClient
class GetAnalyzedTransactionEventsIT extends BaseIT {
    private static final ParameterizedTypeReference<ServerSentEvent<AnalyzedTransactionEvent>> PARAMETERIZED_TYPE_REFERENCE = new ParameterizedTypeReference<>() {
    };
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
    private static final String MEMPOOL_JSON = """
            {
              "count": 1234
            }
            """;
    private static final String FEE_JSON = """
            {
              "fastestFee": 50,
              "halfHourFee": 25,
              "hourFee": 10,
              "economyFee": 5
            }
            """;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private WebTestClient webTestClient;
    private static DisposableServer disposableServer;
    private static String wsUri;

    @BeforeEach
    void startWsServer() {
        var mockRestServiceServer = MockRestServiceServer.createServer(restTemplate);
        mockRestServiceServer.expect(requestTo("https://mempool.space/api/v1/fees/recommended"))
                .andExpect(method(GET))
                .andRespond(withSuccess(FEE_JSON, APPLICATION_JSON));

        mockRestServiceServer.expect(requestTo("https://mempool.space/api/mempool"))
                .andExpect(method(GET))
                .andRespond(withSuccess(MEMPOOL_JSON, APPLICATION_JSON));
    }

    @AfterAll
    static void stopWsServer() {
        if (disposableServer != null) disposableServer.disposeNow();
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        disposableServer = HttpServer.create()
                .port(0) // random port
                .route(routes -> routes.ws("/ws", (in, out) ->
                        out.sendString(Mono.just(TXS)).neverComplete())
                )
                .bindNow();
        wsUri = "ws://localhost:" + disposableServer.port() + "/ws";
        r.add("app.mempool.space.websocket.track-mempool-api-url", () -> wsUri);
    }

    @Test
    void consumesTwoSseEvents() {
        var body = webTestClient.get()
                .uri("/api/v1/transactions/stream")
                .accept(TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(TEXT_EVENT_STREAM)
                .returnResult(PARAMETERIZED_TYPE_REFERENCE)
                .getResponseBody();

        StepVerifier.create(body
                        .mapNotNull(ServerSentEvent::data)
                        .take(1)
                )
                .assertNext(tx -> {
                    assertThat(tx).isNotNull();
                    assertThat(tx.id()).isEqualTo("tx2");
                    assertThat(tx.feePerVByte()).isEqualByComparingTo("20");
                    assertThat(tx.totalFee()).isEqualByComparingTo("2000");
                    assertThat(tx.txSize()).isEqualTo(200);
                    assertThat(tx.timestamp()).isEqualTo(Instant.parse("1970-01-01T00:00:10Z"));
                    assertThat(tx.patternSignal()).isNull();
                    assertThat(tx.priceTier()).isEqualTo(PriceTier.EXPENSIVE);
                    assertThat(tx.isOutlier()).isFalse();
                })
                .verifyComplete();
    }
}
