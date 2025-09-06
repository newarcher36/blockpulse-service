package com.blockchain.blockpulseservice;

import com.blockchain.blockpulseservice.client.ws.MempoolSpaceWebSocketClient;
import com.blockchain.blockpulseservice.controller.AnalysisStream;
import com.blockchain.blockpulseservice.model.domain.MempoolStats;
import com.blockchain.blockpulseservice.model.domain.PriceTier;
import com.blockchain.blockpulseservice.model.domain.Transaction;
import com.blockchain.blockpulseservice.model.event.AnalyzedTransactionEvent;
import com.blockchain.blockpulseservice.model.event.MempoolStatsUpdatedEvent;
import com.blockchain.blockpulseservice.model.event.NewTransactionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SlidingWindowAnalyzersIT {
    @Autowired
    private ApplicationEventPublisher publisher;
    @Autowired
    private AnalysisStream analysisStream;
    @MockitoBean
    private MempoolSpaceWebSocketClient wsClient;

    @BeforeEach
    void resetMempoolStats() {
        publisher.publishEvent(new MempoolStatsUpdatedEvent(MempoolStats.empty()));
    }

    @Test
    void transactionFeeClassificationUsingMempoolStats() {
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
                    publisher.publishEvent(new NewTransactionEvent(tx("tx-expensive", "51"))); // > fast → CHEAP
                    publisher.publishEvent(new NewTransactionEvent(tx("tx-medium", "25")));    // <= medium → NORMAL
                    publisher.publishEvent(new NewTransactionEvent(tx("tx-cheap", "24")));   // between → EXPENSIVE
                })
                .assertNext(e ->
                        assertThat(e)
                                .extracting(AnalyzedTransactionEvent::id, AnalyzedTransactionEvent::priceTier)
                                .containsExactly("tx-expensive", PriceTier.EXPENSIVE)
                )
                .assertNext(e ->
                        assertThat(e)
                                .extracting(AnalyzedTransactionEvent::id, AnalyzedTransactionEvent::priceTier)
                                .containsExactly("tx-medium", PriceTier.NORMAL))
                .assertNext(e ->
                        assertThat(e)
                                .extracting(AnalyzedTransactionEvent::id, AnalyzedTransactionEvent::priceTier)
                                .containsExactly("tx-cheap", PriceTier.CHEAP))
                .thenCancel()
                .verify(Duration.ofSeconds(3));
    }

    @Test
    void transactionFeeClassificationUsingIqr() {
        publisher.publishEvent(new MempoolStatsUpdatedEvent(MempoolStats.empty()));

        var flux = analysisStream.flux();

        StepVerifier.create(flux)
                .then(() -> {
                    publisher.publishEvent(new NewTransactionEvent(tx("s1", "10")));
                    publisher.publishEvent(new NewTransactionEvent(tx("s2", "12")));
                    publisher.publishEvent(new NewTransactionEvent(tx("s3", "15")));
                    publisher.publishEvent(new NewTransactionEvent(tx("s4", "18")));
                    publisher.publishEvent(new NewTransactionEvent(tx("s5", "20")));
                    publisher.publishEvent(new NewTransactionEvent(tx("s6", "25")));
                })
                .thenAwait(Duration.ofMillis(100))
                .expectNextCount(6)
                .then(() -> publisher.publishEvent(new NewTransactionEvent(tx("tx-cheap", "1"))))
                .assertNext(e ->
                        assertThat(e)
                                .extracting(AnalyzedTransactionEvent::id, AnalyzedTransactionEvent::priceTier)
                                .containsExactly("tx-cheap", PriceTier.CHEAP)
                )
                .then(() -> publisher.publishEvent(new NewTransactionEvent(tx("tx-normal", "18"))))
                .assertNext(e ->
                        assertThat(e)
                                .extracting(AnalyzedTransactionEvent::id, AnalyzedTransactionEvent::priceTier)
                                .containsExactly("tx-normal", PriceTier.NORMAL)
                )
                .then(() -> publisher.publishEvent(new NewTransactionEvent(tx("tx-expensive", "100"))))
                .assertNext(e ->
                        assertThat(e)
                                .extracting(AnalyzedTransactionEvent::id, AnalyzedTransactionEvent::priceTier)
                                .containsExactly("tx-expensive", PriceTier.EXPENSIVE)
                )
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
                    publisher.publishEvent(new NewTransactionEvent(tx("t1", "10")));
                    publisher.publishEvent(new NewTransactionEvent(tx("t2", "12")));
                    publisher.publishEvent(new NewTransactionEvent(tx("t3", "15")));
                    // upper fence is 73.375. Beyond that is considered a surge.
                    publisher.publishEvent(new NewTransactionEvent(tx("t-surge", "100")));
                })
                .thenAwait(Duration.ofMillis(100))
                .expectNextCount(3)
                .assertNext(e ->
                        assertThat(e)
                                .extracting(AnalyzedTransactionEvent::id, AnalyzedTransactionEvent::isOutlier)
                                .containsExactly("t-surge", true))
                .thenCancel()
                .verify(Duration.ofSeconds(3));
    }

    @Test
    void outliersFlagBasedOnTukeyFences() {
        publisher.publishEvent(new MempoolStatsUpdatedEvent(MempoolStats.empty()));

        var flux = analysisStream.flux();

        StepVerifier.create(flux)
                .then(() -> {
                    // Seed window to establish fences
                    publisher.publishEvent(new NewTransactionEvent(tx("tx-1", "20")));
                    publisher.publishEvent(new NewTransactionEvent(tx("tx-2", "22")));
                    publisher.publishEvent(new NewTransactionEvent(tx("tx-3", "25")));
                    publisher.publishEvent(new NewTransactionEvent(tx("tx-4", "28")));
                    publisher.publishEvent(new NewTransactionEvent(tx("tx-5", "20")));
                    publisher.publishEvent(new NewTransactionEvent(tx("tx-6", "25")));
                })
                .thenAwait(Duration.ofMillis(100))
                .expectNextCount(6)
                // Low outlier (below lower fence) -> isOutlier = true
                .then(() -> publisher.publishEvent(new NewTransactionEvent(tx("tx-low-outlier", "2"))))
                .assertNext(e ->
                        assertThat(e)
                                .extracting(AnalyzedTransactionEvent::id, AnalyzedTransactionEvent::isOutlier)
                                .containsExactly("tx-low-outlier", true))
                // Inside fences -> isOutlier = false
                .then(() -> publisher.publishEvent(new NewTransactionEvent(tx("tx-inside", "28"))))
                .assertNext(e ->
                        assertThat(e)
                                .extracting(AnalyzedTransactionEvent::id, AnalyzedTransactionEvent::isOutlier)
                                .containsExactly("tx-inside", false))
                // High outlier (above upper fence) -> isOutlier = true
                .then(() -> publisher.publishEvent(new NewTransactionEvent(tx("tx-high-outlier", "100"))))
                .assertNext(e ->
                        assertThat(e)
                                .extracting(AnalyzedTransactionEvent::id, AnalyzedTransactionEvent::isOutlier)
                                .containsExactly("tx-high-outlier", true))
                .thenCancel()
                .verify(Duration.ofSeconds(3));
    }

    private static Transaction tx(String id, String fee) {
        return new Transaction(id, new BigDecimal(fee), BigDecimal.ZERO, 100, Instant.EPOCH);
    }
}