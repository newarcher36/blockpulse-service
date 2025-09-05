package com.blockchain.blockpulseservice;

import com.blockchain.blockpulseservice.controller.AnalysisStream;
import com.blockchain.blockpulseservice.model.domain.PatternType;
import com.blockchain.blockpulseservice.model.domain.PriceTier;
import com.blockchain.blockpulseservice.model.domain.Transaction;
import com.blockchain.blockpulseservice.model.event.AnalyzedTransactionEvent;
import com.blockchain.blockpulseservice.model.event.MempoolStatsUpdatedEvent;
import com.blockchain.blockpulseservice.model.domain.MempoolStats;
import com.blockchain.blockpulseservice.model.event.NewTransactionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SlidingWindowAnalyzersIT {

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    private AnalysisStream analysisStream;

    @BeforeEach
    void resetMempoolStats() {
        publisher.publishEvent(new MempoolStatsUpdatedEvent(MempoolStats.empty()));
    }

    @Test
    void transactionFeeClassificationUsingMempoolThresholds() {
        var congestedMempool = MempoolStats.builder()
                .fastFeePerVByte(50)
                .mediumFeePerVByte(25)
                .slowFeePerVByte(10)
                .mempoolSize(10_000)
                .build();
        publisher.publishEvent(new MempoolStatsUpdatedEvent(congestedMempool));

        var flux = analysisStream.flux();

        StepVerifier.create(flux)
                .then(() -> {
                    publisher.publishEvent(new NewTransactionEvent(tx("t-fast-plus", "60"))); // > fast → CHEAP
                    publisher.publishEvent(new NewTransactionEvent(tx("t-medium", "25")));    // <= medium → NORMAL
                    publisher.publishEvent(new NewTransactionEvent(tx("t-between", "40")));   // between → EXPENSIVE
                })
                .assertNext(e -> assertThat(e.priceTier()).isEqualTo(PriceTier.CHEAP))
                .assertNext(e -> assertThat(e.priceTier()).isEqualTo(PriceTier.NORMAL))
                .assertNext(e -> assertThat(e.priceTier()).isEqualTo(PriceTier.EXPENSIVE))
                .thenCancel()
                .verify(Duration.ofSeconds(3));
    }

    @Test
    void surgePatternEmittedWhenAboveUpperFenceAndFastFeeInCongestedMempool() {
        var congestedMempool = MempoolStats.builder()
                .fastFeePerVByte(25)
                .mediumFeePerVByte(10)
                .slowFeePerVByte(5)
                .mempoolSize(10_000)
                .build();
        publisher.publishEvent(new MempoolStatsUpdatedEvent(congestedMempool));

        var flux = analysisStream.flux();

        StepVerifier.create(flux)
                .then(() -> {
                    // Seed window with modest fees to get low fences. upper endpoint 73.375
                    publisher.publishEvent(new NewTransactionEvent(tx("t1", "10")));
                    publisher.publishEvent(new NewTransactionEvent(tx("t2", "12")));
                    publisher.publishEvent(new NewTransactionEvent(tx("t3", "15")));
                    publisher.publishEvent(new NewTransactionEvent(tx("t-surge", "100")));
                })
                .thenAwait(Duration.ofMillis(100))
                .expectNextCount(3)
                .assertNext(e -> assertThat(e.patternTypes()).contains(PatternType.SURGE))
                .thenCancel()
                .verify(Duration.ofSeconds(3));
    }

    private static Transaction tx(String id, String fee) {
        return new Transaction(id, new BigDecimal(fee), BigDecimal.ZERO, 100, Instant.EPOCH);
    }
}