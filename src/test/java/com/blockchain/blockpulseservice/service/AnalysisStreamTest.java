package com.blockchain.blockpulseservice.service;

import com.blockchain.blockpulseservice.controller.AnalysisStream;
import com.blockchain.blockpulseservice.event.AnalyzedTransactionEvent;
import com.blockchain.blockpulseservice.model.domain.PatternType;
import com.blockchain.blockpulseservice.model.domain.PriceTier;
import com.blockchain.blockpulseservice.model.dto.TransactionWindowSnapshotDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

class AnalysisStreamTest {

    private AnalysisStream stream;

    @BeforeEach
    void setUp() {
        stream = new AnalysisStream();
    }

    @Test
    void publishesToActiveSubscribers() {
        var e = sampleEvent("a1");

        StepVerifier.create(stream.flux())
                .then(() -> stream.publish(e))
                .expectNext(e)
                .thenCancel()
                .verify();
    }

    @Test
    void replaysLatestToNewSubscribers() {
        var e1 = sampleEvent("a1");
        stream.publish(e1);

        StepVerifier.create(stream.flux())
                .expectNext(e1)
                .thenCancel()
                .verify();
    }

    @Test
    void latestWinsForLateSubscribers() {
        var e1 = sampleEvent("a1");
        var e2 = sampleEvent("a2");

        // Early subscriber sees both events in order
        StepVerifier.create(stream.flux())
                .then(() -> {
                    stream.publish(e1);
                    stream.publish(e2);
                })
                .expectNext(e1, e2)
                .thenCancel()
                .verify();

        // Late subscriber immediately receives only the latest (replay latest)
        StepVerifier.create(stream.flux())
                .expectNext(e2)
                .thenCancel()
                .verify();
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

