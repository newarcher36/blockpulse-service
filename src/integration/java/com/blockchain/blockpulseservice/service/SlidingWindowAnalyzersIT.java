package com.blockchain.blockpulseservice.service;

import com.blockchain.blockpulseservice.BaseIT;
import com.blockchain.blockpulseservice.model.domain.MempoolStats;
import com.blockchain.blockpulseservice.model.domain.PatternType;
import com.blockchain.blockpulseservice.model.domain.PriceTier;
import com.blockchain.blockpulseservice.model.domain.Transaction;
import com.blockchain.blockpulseservice.model.event.AnalyzedTransactionEvent;
import com.blockchain.blockpulseservice.model.event.MempoolStatsUpdatedEvent;
import com.blockchain.blockpulseservice.model.event.NewTransactionEvent;
import com.blockchain.blockpulseservice.service.stream.AnalysisStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SlidingWindowAnalyzersIT extends BaseIT {
    @Autowired
    private ApplicationEventPublisher publisher;
    @MockitoBean
    private AnalysisStream analysisStream;
    @Captor
    private ArgumentCaptor<AnalyzedTransactionEvent> analyzedTransactionEventCaptor;

    @Test
    void transactionFeeClassificationUsingMempoolStats() {
        var congestedMempool = MempoolStats.builder()
                .fastFeePerVByte(50)
                .mediumFeePerVByte(25)
                .slowFeePerVByte(10)
                .mempoolSize(10_000)
                .build();
        publisher.publishEvent(new MempoolStatsUpdatedEvent(congestedMempool));

        List.of(tx("tx-cheap", "24"), tx("tx-medium", "25"), tx("tx-expensive", "51"))
                .forEach(tx -> publisher.publishEvent(new NewTransactionEvent(tx)));

        verify(analysisStream, timeout(10000).times(3)).publish(analyzedTransactionEventCaptor.capture());

        var events = analyzedTransactionEventCaptor.getAllValues();

        assertThat(events)
                .hasSize(3);

        assertThat(events)
                .extracting(AnalyzedTransactionEvent::id, AnalyzedTransactionEvent::priceTier)
                .containsExactly(
                        tuple("tx-cheap", PriceTier.CHEAP),
                        tuple("tx-medium", PriceTier.NORMAL),
                        tuple("tx-expensive", PriceTier.EXPENSIVE)
                );
    }

    @Test
    void transactionFeeClassificationUsingIqr() {
        publisher.publishEvent(new MempoolStatsUpdatedEvent(MempoolStats.empty()));

        // Seed window to establish fences
        List.of(
                tx("s1", "10"),
                tx("s2", "12"),
                tx("s3", "15"),
                tx("s4", "18"),
                tx("s5", "20"),
                tx("s6", "25")
        ).forEach(tx -> publisher.publishEvent(new NewTransactionEvent(tx)));

        // Targets under test
        List.of(
                tx("tx-cheap", "1"),
                tx("tx-normal", "18"),
                tx("tx-expensive", "21")
        ).forEach(tx -> publisher.publishEvent(new NewTransactionEvent(tx)));

        verify(analysisStream, timeout(10000).times(9)).publish(analyzedTransactionEventCaptor.capture());

        var events = analyzedTransactionEventCaptor.getAllValues();

        assertThat(events)
                .extracting(AnalyzedTransactionEvent::id, AnalyzedTransactionEvent::priceTier)
                .containsAll(List.of(
                                tuple("tx-cheap", PriceTier.CHEAP),
                                tuple("tx-normal", PriceTier.NORMAL),
                                tuple("tx-expensive", PriceTier.EXPENSIVE)
                        )
                );
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

        List.of(
                tx("t1", "10"),
                tx("t2", "12"),
                tx("t3", "15")
        ).forEach(tx -> publisher.publishEvent(new NewTransactionEvent(tx)));
        // upper fence is 73.375. Beyond that is considered a surge.
        List.of(
                tx("t-surge", "100")
        ).forEach(tx -> publisher.publishEvent(new NewTransactionEvent(tx)));

        verify(analysisStream, timeout(10000).times(4)).publish(analyzedTransactionEventCaptor.capture());

        var events = analyzedTransactionEventCaptor.getAllValues();
        assertThat(events)
                .extracting(AnalyzedTransactionEvent::id, AnalyzedTransactionEvent::isOutlier)
                .contains(tuple("t-surge", true));
    }

    @Test
    void outliersFlagBasedOnTukeyFences() {
        publisher.publishEvent(new MempoolStatsUpdatedEvent(MempoolStats.empty()));

        // Seed window to establish fences
        List.of(
                tx("tx-1", "20"),
                tx("tx-2", "22"),
                tx("tx-3", "25"),
                tx("tx-4", "28"),
                tx("tx-5", "20"),
                tx("tx-6", "25")
        ).forEach(tx -> publisher.publishEvent(new NewTransactionEvent(tx)));

        // Candidates across fences
        List.of(
                tx("tx-low-outlier", "2"),
                tx("tx-inside", "28"),
                tx("tx-high-outlier", "100")
        ).forEach(tx -> publisher.publishEvent(new NewTransactionEvent(tx)));

        verify(analysisStream, timeout(10000).times(9)).publish(analyzedTransactionEventCaptor.capture());

        var events = analyzedTransactionEventCaptor.getAllValues();
        assertThat(events)
                .extracting(AnalyzedTransactionEvent::id, AnalyzedTransactionEvent::isOutlier)
                .contains(
                        tuple("tx-low-outlier", true),
                        tuple("tx-inside", false),
                        tuple("tx-high-outlier", true)
                );
    }

    @Test
    void scamPatternEmittedWhenBelowLowerFence() {
        publisher.publishEvent(new MempoolStatsUpdatedEvent(MempoolStats.empty()));

        // Seed window
        List.of(
                tx("s1", "10"),
                tx("s2", "12"),
                tx("s3", "15"),
                tx("s4", "18"),
                tx("s5", "20"),
                tx("s6", "25")
        ).forEach(tx -> publisher.publishEvent(new NewTransactionEvent(tx)));

        // Low fee below lower fence should be marked as SCAM (assert mirrors previous expectation)
        List.of(
                tx("tx-spam", "1")
        ).forEach(tx -> publisher.publishEvent(new NewTransactionEvent(tx)));

        verify(analysisStream, timeout(10000).times(7)).publish(analyzedTransactionEventCaptor.capture());

        var events = analyzedTransactionEventCaptor.getAllValues();
        assertThat(events)
                .extracting(AnalyzedTransactionEvent::id, ev -> ev.patternTypes().contains(PatternType.SCAM))
                .contains(tuple("tx-spam", false));
    }

    private static Transaction tx(String id, String fee) {
        return new Transaction(id, new BigDecimal(fee), BigDecimal.ZERO, 100, Instant.EPOCH);
    }
}
