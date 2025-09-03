package com.blockchain.blockpulseservice.service.analysis;

import com.blockchain.blockpulseservice.model.domain.AnalysisContext;
import com.blockchain.blockpulseservice.model.domain.FeeWindowStatsSummary;
import com.blockchain.blockpulseservice.model.domain.MempoolStats;
import com.blockchain.blockpulseservice.model.domain.PatternType;
import com.blockchain.blockpulseservice.model.domain.Transaction;
import com.google.common.collect.Range;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpamFeeAnalyzerTest {
    private static final Range<BigDecimal> TUKEY_FENCES_LOW_5_HIGH_20 = Range.closed(new BigDecimal("5"), new BigDecimal("20"));
    private final SpamFeeAnalyzer analyzer = new SpamFeeAnalyzer();

    @Test
    void addsScamPatternWhenBelowLowerFence() {
        var statsSummary = summary(TUKEY_FENCES_LOW_5_HIGH_20);
        var analysisContext = analyzer.analyze(ctx(new BigDecimal("4.99"), statsSummary));
        assertTrue(analysisContext.getPatterns().contains(PatternType.SCAM));
    }

    @Test
    void doesNotAddScamWhenEqualsLowerFence() {
        var statsSummary = summary(TUKEY_FENCES_LOW_5_HIGH_20);
        var analysisContext = analyzer.analyze(ctx(new BigDecimal("5.00"), statsSummary));
        assertFalse(analysisContext.getPatterns().contains(PatternType.SCAM));
    }

    @Test
    void doesNotAddScamWhenAboveLowerFence() {
        var statsSummary = summary(TUKEY_FENCES_LOW_5_HIGH_20);
        var analysisContext = analyzer.analyze(ctx(new BigDecimal("10.00"), statsSummary));
        assertFalse(analysisContext.getPatterns().contains(PatternType.SCAM));
    }

    private static FeeWindowStatsSummary summary(Range<BigDecimal> tukeyFences) {
        return FeeWindowStatsSummary.builder()
                .transactionCount(10)
                .outliersCount(0)
                .avgFeePerVByte(BigDecimal.ZERO)
                .median(BigDecimal.ZERO)
                .iqrRange(Range.closed(BigDecimal.ONE, BigDecimal.TEN))
                .tukeyFences(tukeyFences)
                .build();
    }

    private static AnalysisContext ctx(BigDecimal fee, FeeWindowStatsSummary statsSummary) {
        return AnalysisContext.builder()
                .newTransaction(new Transaction("tx", fee, BigDecimal.ZERO, 100, Instant.EPOCH))
                .feeWindowStatsSummary(statsSummary)
                .mempoolStats(MempoolStats.empty())
                .build();
    }
}
