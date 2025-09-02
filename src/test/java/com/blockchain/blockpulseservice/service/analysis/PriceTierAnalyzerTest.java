package com.blockchain.blockpulseservice.service.analysis;

import com.blockchain.blockpulseservice.model.domain.AnalysisContext;
import com.blockchain.blockpulseservice.model.domain.FeeWindowStatsSummary;
import com.blockchain.blockpulseservice.model.domain.MempoolStats;
import com.blockchain.blockpulseservice.model.domain.PriceTier;
import com.blockchain.blockpulseservice.model.domain.Transaction;
import com.google.common.collect.Range;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class PriceTierAnalyzerTest {
    private final PriceTierClassifier priceTierClassifier = new PriceTierClassifier();
    private final PriceTierAnalyzer priceTierAnalyzer = new PriceTierAnalyzer(1000, priceTierClassifier);

    @Test
    void returnsAbnormalWhenOutlier() {
        var ctx = baseContext(new BigDecimal("15"), 100, Range.closed(new BigDecimal("10"), new BigDecimal("20")), Range.closed(new BigDecimal("5"), new BigDecimal("30")))
                .toBuilder()
                .isOutlier(true)
                .build();

        var result = new PriceTierAnalyzer(1000, priceTierClassifier).analyze(ctx);

        assertEquals(PriceTier.ABNORMAL_PRICE, result.getPriceTier());
    }

    @Test
    void congestedUsesMempoolThresholds() {
        var base = baseContext(new BigDecimal("0"), 2000, defaultIqr(), defaultFences());

        // fee > fast -> CHEAP
        var res1 = new PriceTierAnalyzer(1000, priceTierClassifier).analyze(
                base.toBuilder()
                        .newTransaction(new Transaction("t1", new BigDecimal("60"), BigDecimal.ZERO, 100, Instant.EPOCH))
                        .mempoolStats(new MempoolStats(50, 25, 10, 2000))
                        .build());
        assertEquals(PriceTier.CHEAP, res1.getPriceTier());

        // fee <= medium -> NORMAL
        var res2 = new PriceTierAnalyzer(1000, priceTierClassifier).analyze(base.toBuilder()
                .newTransaction(new Transaction("t2", new BigDecimal("25"), BigDecimal.ZERO, 100, Instant.EPOCH))
                .mempoolStats(new MempoolStats(50, 25, 10, 2000))
                .build());
        assertEquals(PriceTier.NORMAL, res2.getPriceTier());

        // else -> EXPENSIVE (fee between medium and fast)
        var res3 = new PriceTierAnalyzer(1000, priceTierClassifier).analyze(base.toBuilder()
                .newTransaction(new Transaction("t3", new BigDecimal("40"), BigDecimal.ZERO, 100, Instant.EPOCH))
                .mempoolStats(new MempoolStats(50, 25, 10, 2000))
                .build());
        assertEquals(PriceTier.EXPENSIVE, res3.getPriceTier());
    }

    @Test
    void notCongestedUsesIqr() {
        var iqr = Range.closed(new BigDecimal("10"), new BigDecimal("20"));
        var base = baseContext(new BigDecimal("0"), 500, iqr, defaultFences());

        // fee < lower -> CHEAP
        var r1 = priceTierAnalyzer.analyze(base.toBuilder()
                .newTransaction(new Transaction("t1", new BigDecimal("9.99"), BigDecimal.ZERO, 100, Instant.EPOCH))
                .build());
        assertEquals(PriceTier.CHEAP, r1.getPriceTier());

        // inside -> NORMAL
        var r2 = new PriceTierAnalyzer(1000, priceTierClassifier).analyze(base.toBuilder()
                .newTransaction(new Transaction("t2", new BigDecimal("15"), BigDecimal.ZERO, 100, Instant.EPOCH))
                .build());
        assertEquals(PriceTier.NORMAL, r2.getPriceTier());

        // above -> EXPENSIVE
        var r3 = new PriceTierAnalyzer(1000, priceTierClassifier).analyze(base.toBuilder()
                .newTransaction(new Transaction("t3", new BigDecimal("25"), BigDecimal.ZERO, 100, Instant.EPOCH))
                .build());
        assertEquals(PriceTier.EXPENSIVE, r3.getPriceTier());
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
