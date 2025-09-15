package com.blockchain.blockpulseservice.controller;

import com.blockchain.blockpulseservice.model.domain.PriceTier;
import com.blockchain.blockpulseservice.model.domain.PatternSignal;
import com.blockchain.blockpulseservice.model.domain.PatternMetric;
import com.blockchain.blockpulseservice.model.domain.PatternType;
import com.blockchain.blockpulseservice.model.dto.TransactionWindowSnapshotDTO;
import com.blockchain.blockpulseservice.model.event.AnalyzedTransactionEvent;
import com.blockchain.blockpulseservice.service.stream.AnalysisStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.Map;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM;

@WebFluxTest(
        controllers = AnalysisController.class,
        properties = "app.stream.sse.sample-ms=50"
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AnalysisControllerIT {
    @Autowired
    private WebTestClient webTestClient;
    @MockitoBean
    private AnalysisStream analysisStream;

    @Test
    void streamAndConsumeSseEvent() {
        var expectedEvent = AnalyzedTransactionEvent.builder()
                .id("tx-1")
                .producedAt(Instant.now())
                .feePerVByte(new BigDecimal("15.5"))
                .totalFee(new BigDecimal("1550"))
                .txSize(100)
                .timestamp(Instant.now())
                .patternSignal(new PatternSignal(PatternType.SURGE, Map.of(PatternMetric.UPPER_TUKEY_FENCE, 30.0)))
                .priceTier(PriceTier.NORMAL)
                .isOutlier(false)
                .windowSnapshot(new TransactionWindowSnapshotDTO(10, 0, BigDecimal.ZERO, BigDecimal.ZERO))
                .build();
        when(analysisStream.flux()).thenReturn(Flux.just(expectedEvent));

        var result = webTestClient.get()
                .uri("/api/v1/transactions/stream")
                .accept(TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(TEXT_EVENT_STREAM)
                .returnResult(new ParameterizedTypeReference<ServerSentEvent<AnalyzedTransactionEvent>>() {
                });

        StepVerifier.create(
                        result.getResponseBody()
                                .mapNotNull(ServerSentEvent::data)
                                .take(1)
                )
                .assertNext(actual ->
                        assertThat(actual)
                                .usingRecursiveComparison()
                                .isEqualTo(expectedEvent))
                .verifyComplete();

    }
}
