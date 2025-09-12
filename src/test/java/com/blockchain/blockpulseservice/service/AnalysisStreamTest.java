package com.blockchain.blockpulseservice.service;

import com.blockchain.blockpulseservice.service.stream.AnalysisStream;
import com.blockchain.blockpulseservice.model.domain.PatternType;
import com.blockchain.blockpulseservice.model.domain.PriceTier;
import com.blockchain.blockpulseservice.model.dto.TransactionWindowSnapshotDTO;
import com.blockchain.blockpulseservice.model.event.AnalyzedTransactionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

class AnalysisStreamTest {
    private static final int REPLAY_LIMIT = 2;
    private static final int SAMPLE_MS = 100;
    private static final Duration VERIFY_TIMEOUT = Duration.ofSeconds(1);
    private AnalysisStream stream;

    @BeforeEach
    void setUp() {
        stream = new AnalysisStream(REPLAY_LIMIT, SAMPLE_MS);
    }

    @Test
    void publishesToActiveSubscribers() {
        var e = sampleEvent("e1");

        StepVerifier.create(stream.flux())
                .then(() -> stream.publish(e))
                .expectNext(e)
                .thenCancel()
                .verify(VERIFY_TIMEOUT);
    }

    @Test
    void replaysLatestToNewSubscribersWithSingleEmission() {
        var e1 = sampleEvent("e1");
        stream.publish(e1);

        StepVerifier.create(stream.flux())
                .expectNext(e1)
                .thenCancel()
                .verify(VERIFY_TIMEOUT);
    }

    @Test
    void replaysLatestEventsBelowReplayLimitAlsoToLateSubscribers() {
        var e1 = sampleEvent("e1");
        var e2 = sampleEvent("e2");

        // Early subscriber sees both events in order
        StepVerifier.create(stream.flux())
                .then(() -> stream.publish(e1))
                .thenAwait(Duration.ofMillis(SAMPLE_MS * 2))
                .then(() -> stream.publish(e2))
                .thenAwait(Duration.ofMillis(SAMPLE_MS * 2))
                .expectNext(e1, e2)
                .thenCancel()
                .verify(VERIFY_TIMEOUT);

        // Late subscriber immediately receives the last two events (replay limit = 2)
        StepVerifier.create(stream.flux())
                .expectNext(e1, e2)
                .thenCancel()
                .verify(VERIFY_TIMEOUT);
    }

    @Test
    void replaysLatestEventsOverReplayLimitAlsoToLateSubscribers() {
        var e1 = sampleEvent("e1");
        var e2 = sampleEvent("e2");
        var e3 = sampleEvent("e3");

        // Early subscriber sees both events in order
        StepVerifier.create(stream.flux())
                .then(() -> stream.publish(e1))
                .thenAwait(Duration.ofMillis(SAMPLE_MS * 2))
                .then(() -> stream.publish(e2))
                .thenAwait(Duration.ofMillis(SAMPLE_MS * 2))
                .then(() -> stream.publish(e3))
                .thenAwait(Duration.ofMillis(SAMPLE_MS * 2))
                .expectNext(e1, e2, e3)
                .thenCancel()
                .verify(VERIFY_TIMEOUT);

        // Late subscriber immediately receives the last two events (replay limit = 2)
        StepVerifier.create(stream.flux())
                .expectNext(e2, e3)
                .thenCancel()
                .verify(VERIFY_TIMEOUT);
    }

    private static AnalyzedTransactionEvent sampleEvent(String id) {
        return AnalyzedTransactionEvent.builder()
                .id(id)
                .producedAt(Instant.now())
                .feePerVByte(new BigDecimal("12.3"))
                .totalFee(new BigDecimal("1234"))
                .txSize(200)
                .timestamp(Instant.now())
                .patternTypes(Set.of(PatternType.SURGE))
                .priceTier(PriceTier.NORMAL)
                .isOutlier(false)
                .windowSnapshot(new TransactionWindowSnapshotDTO(10, 0, BigDecimal.ZERO, BigDecimal.ZERO))
                .build();
    }
}
