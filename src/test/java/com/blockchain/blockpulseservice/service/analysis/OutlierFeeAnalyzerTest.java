package com.blockchain.blockpulseservice.service.analysis;

import com.blockchain.blockpulseservice.model.domain.AnalysisContext;
import com.blockchain.blockpulseservice.model.domain.FeeWindowStatsSummary;
import com.blockchain.blockpulseservice.model.domain.MempoolStats;
import com.blockchain.blockpulseservice.model.domain.Transaction;
import com.google.common.collect.Range;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutlierFeeAnalyzerTest {
    private static final Range<BigDecimal> TUKEY_FENCES_LOW_5_HIGH_20 = Range.closed(new BigDecimal("5"), new BigDecimal("20"));
    private final OutlierFeeAnalyzer analyzer = new OutlierFeeAnalyzer();

    @Test
    void flagsOutlierAboveUpperFence() {
        var summary = summary(TUKEY_FENCES_LOW_5_HIGH_20);
        var fee = new BigDecimal("25");
        var analysisContext = analyzer.analyze(ctx(fee, summary));
        assertTrue(analysisContext.isOutlier());
    }

    @Test
    void flagsOutlierBelowLowerFence() {
        var summary = summary(TUKEY_FENCES_LOW_5_HIGH_20);
        var fee = new BigDecimal("4.99");
        var analysisContext = analyzer.analyze(ctx(fee, summary));
        assertTrue(analysisContext.isOutlier());
    }

    @Test
    void insideFencesIsNotOutlier() {
        var summary = summary(TUKEY_FENCES_LOW_5_HIGH_20);
        var fee = new BigDecimal("10");
        var analysisContext = analyzer.analyze(ctx(fee, summary));
        assertFalse(analysisContext.isOutlier());
    }

    private static AnalysisContext ctx(BigDecimal fee, FeeWindowStatsSummary summary) {
        return AnalysisContext.builder()
                .newTransaction(new Transaction("tx", fee, BigDecimal.ZERO, 100, Instant.EPOCH))
                .feeWindowStatsSummary(summary)
                .mempoolStats(MempoolStats.empty())
                .build();
    }

    private static FeeWindowStatsSummary summary(Range<BigDecimal> tukeyFences) {
        return FeeWindowStatsSummary.builder()
                .transactionCount(10)
                .outliersCount(0)
                .avgFeePerVByte(new BigDecimal("0"))
                .median(new BigDecimal("0"))
                .iqrRange(Range.closed(BigDecimal.ONE, BigDecimal.TEN))
                .tukeyFences(tukeyFences)
                .build();
    }
}
