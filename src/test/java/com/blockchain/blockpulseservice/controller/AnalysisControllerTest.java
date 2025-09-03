package com.blockchain.blockpulseservice.controller;

import com.blockchain.blockpulseservice.event.AnalyzedTransactionEvent;
import com.blockchain.blockpulseservice.model.domain.PatternType;
import com.blockchain.blockpulseservice.model.domain.PriceTier;
import com.blockchain.blockpulseservice.model.dto.TransactionWindowSnapshotDTO;
import com.blockchain.blockpulseservice.service.AnalysisStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = AnalysisController.class)
class AnalysisControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private AnalysisStream analysisStream;

    @Test
    void stream_emitsSseEventFromAnalysisStream() {
        var event = AnalyzedTransactionEvent.builder()
                .id("tx-1")
                .producedAt(Instant.now())
                .feePerVByte(new BigDecimal("15.5"))
                .totalFee(new BigDecimal("1550"))
                .txSize(100)
                .timestamp(Instant.now())
                .patternTypes(Set.of(PatternType.SURGE))
                .priceTier(PriceTier.NORMAL)
                .isOutlier(false)
                .windowSnapshot(new TransactionWindowSnapshotDTO(10, 0, BigDecimal.ZERO, BigDecimal.ZERO))
                .build();

        // Emit frequently so controller's sample(2s) picks up a latest value at the first tick
        Flux<AnalyzedTransactionEvent> source = Flux.interval(Duration.ofMillis(100))
                .map(i -> event)
                .take(50);

        when(analysisStream.flux()).thenReturn(source);

        var result = webTestClient.get()
                .uri("/api/v1/transactions/stream")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .returnResult(new ParameterizedTypeReference<ServerSentEvent<AnalyzedTransactionEvent>>() {
                });

        StepVerifier.withVirtualTime(() -> result.getResponseBody()
                        .mapNotNull(ServerSentEvent::data)
                        .filter(Objects::nonNull))
                .thenAwait(Duration.ofSeconds(2))
                .assertNext(actualEvent -> assertThat(actualEvent).isEqualTo(event))
                .thenCancel()
                .verify();
    }
}
