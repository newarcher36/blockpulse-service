package com.blockchain.blockpulseservice.service.analysis;

import com.blockchain.blockpulseservice.model.domain.AnalysisContext;
import com.blockchain.blockpulseservice.model.domain.FeeWindowStatsSummary;
import com.blockchain.blockpulseservice.model.domain.MempoolStats;
import com.blockchain.blockpulseservice.model.domain.PriceTier;
import com.blockchain.blockpulseservice.model.domain.Transaction;
import com.google.common.collect.Range;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceTierAnalyzerTest {
    @Mock
    private PriceTierClassifier priceTierClassifier;

    private PriceTierAnalyzer priceTierAnalyzer;

    @BeforeEach
    void setUp() {
        priceTierAnalyzer = new PriceTierAnalyzer(1000, priceTierClassifier);
    }

    @Test
    void returnsAbnormalWhenOutlier() {
        var ctx = baseContext(new BigDecimal("15"), 100,
                Range.closed(new BigDecimal("10"), new BigDecimal("20")),
                Range.closed(new BigDecimal("5"), new BigDecimal("30")))
                .toBuilder()
                .isOutlier(true)
                .build();

        var result = priceTierAnalyzer.analyze(ctx);

        assertEquals(PriceTier.ABNORMAL_PRICE, result.getPriceTier());
        verifyNoInteractions(priceTierClassifier);
    }

    @Test
    void congestedUsesMempoolThresholds() {
        var base = baseContext(new BigDecimal("0"), 2000, defaultIqr(), defaultFences());

        // Stub sequential calls when mempool is congested
        when(priceTierClassifier.classifyUsingMempool(any(), any()))
                .thenReturn(PriceTier.CHEAP, PriceTier.NORMAL, PriceTier.EXPENSIVE);

        var res1 = priceTierAnalyzer.analyze(
                base.toBuilder()
                        .newTransaction(new Transaction("t1", new BigDecimal("60"), BigDecimal.ZERO, 100, Instant.EPOCH))
                        .mempoolStats(new MempoolStats(50, 25, 10, 2000))
                        .build());
        assertEquals(PriceTier.CHEAP, res1.getPriceTier());

        var res2 = priceTierAnalyzer.analyze(base.toBuilder()
                .newTransaction(new Transaction("t2", new BigDecimal("25"), BigDecimal.ZERO, 100, Instant.EPOCH))
                .mempoolStats(new MempoolStats(50, 25, 10, 2000))
                .build());
        assertEquals(PriceTier.NORMAL, res2.getPriceTier());

        var res3 = priceTierAnalyzer.analyze(base.toBuilder()
                .newTransaction(new Transaction("t3", new BigDecimal("40"), BigDecimal.ZERO, 100, Instant.EPOCH))
                .mempoolStats(new MempoolStats(50, 25, 10, 2000))
                .build());
        assertEquals(PriceTier.EXPENSIVE, res3.getPriceTier());

        verify(priceTierClassifier, times(3)).classifyUsingMempool(any(), any());
        verify(priceTierClassifier, never()).classifyUsingIqr(any(), any());
    }

    @Test
    void notCongestedUsesIqr() {
        var iqr = Range.closed(new BigDecimal("10"), new BigDecimal("20"));
        var base = baseContext(new BigDecimal("0"), 500, iqr, defaultFences());

        // Stub sequential calls when mempool is not congested
        when(priceTierClassifier.classifyUsingIqr(any(), any()))
                .thenReturn(PriceTier.CHEAP, PriceTier.NORMAL, PriceTier.EXPENSIVE);

        var r1 = priceTierAnalyzer.analyze(base.toBuilder()
                .newTransaction(new Transaction("t1", new BigDecimal("9.99"), BigDecimal.ZERO, 100, Instant.EPOCH))
                .build());
        assertEquals(PriceTier.CHEAP, r1.getPriceTier());

        var r2 = priceTierAnalyzer.analyze(base.toBuilder()
                .newTransaction(new Transaction("t2", new BigDecimal("15"), BigDecimal.ZERO, 100, Instant.EPOCH))
                .build());
        assertEquals(PriceTier.NORMAL, r2.getPriceTier());

        var r3 = priceTierAnalyzer.analyze(base.toBuilder()
                .newTransaction(new Transaction("t3", new BigDecimal("25"), BigDecimal.ZERO, 100, Instant.EPOCH))
                .build());
        assertEquals(PriceTier.EXPENSIVE, r3.getPriceTier());

        verify(priceTierClassifier, times(3)).classifyUsingIqr(any(), any());
        verify(priceTierClassifier, never()).classifyUsingMempool(any(), any());
    }

    private static AnalysisContext baseContext(BigDecimal fee, int mempoolSize, Range<BigDecimal> iqr, Range<BigDecimal> fences) {
        var tx = new Transaction("tx", fee, BigDecimal.ZERO, 100, Instant.EPOCH);
        var summary = FeeWindowStatsSummary.builder()
                .transactionCount(10)
                .outliersCount(0)
                .avgFeePerVByte(BigDecimal.ZERO)
                .median(BigDecimal.ZERO)
                .iqrRange(iqr)
                .tukeyFences(fences)
                .build();
        return AnalysisContext.builder()
                .newTransaction(tx)
                .feeWindowStatsSummary(summary)
                .mempoolStats(new MempoolStats(50, 25, 10, mempoolSize))
                .build();
    }

    private static Range<BigDecimal> defaultIqr() {
        return Range.closed(new BigDecimal("10"), new BigDecimal("20"));
    }

    private static Range<BigDecimal> defaultFences() {
        return Range.closed(new BigDecimal("5"), new BigDecimal("30"));
    }
}
