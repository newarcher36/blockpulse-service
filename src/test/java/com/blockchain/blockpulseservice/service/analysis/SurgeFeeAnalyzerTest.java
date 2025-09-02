package com.blockchain.blockpulseservice.service.analysis;

import com.blockchain.blockpulseservice.model.domain.AnalysisContext;
import com.blockchain.blockpulseservice.model.domain.FeeWindowStatsSummary;
import com.blockchain.blockpulseservice.model.domain.MempoolStats;
import com.blockchain.blockpulseservice.model.domain.PatternType;
import com.google.common.collect.Range;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class SurgeFeeAnalyzerTest {
    @Test
    void addsSurgeWhenAboveUpperFenceAndFastAndMempoolFull() {
        var analyzer = new SurgeFeeAnalyzer(1000);
        var ctx = baseContext(new BigDecimal("30"), 1000, 25, Range.closed(new BigDecimal("10"), new BigDecimal("20")), Range.closed(new BigDecimal("5"), new BigDecimal("20")));
        var result = analyzer.analyze(ctx);
        assertTrue(result.getPatterns().contains(PatternType.SURGE));
    }

    @Test
    void noSurgeWhenBelowUpperFence() {
        var analyzer = new SurgeFeeAnalyzer(1000);
        var ctx = baseContext(new BigDecimal("19.99"), 2000, 15, Range.closed(new BigDecimal("10"), new BigDecimal("20")), Range.closed(new BigDecimal("5"), new BigDecimal("20")));
        var result = analyzer.analyze(ctx);
        assertFalse(result.getPatterns().contains(PatternType.SURGE));
    }

    @Test
    void noSurgeWhenNotBeyondFastRecommended() {
        var analyzer = new SurgeFeeAnalyzer(1000);
        var ctx = baseContext(new BigDecimal("22"), 2000, 25, Range.closed(new BigDecimal("10"), new BigDecimal("20")), Range.closed(new BigDecimal("5"), new BigDecimal("20")));
        var result = analyzer.analyze(ctx);
        assertFalse(result.getPatterns().contains(PatternType.SURGE));
    }

    @Test
    void noSurgeWhenMempoolNotFull() {
        var analyzer = new SurgeFeeAnalyzer(1000);
        var ctx = baseContext(new BigDecimal("30"), 999, 25, Range.closed(new BigDecimal("10"), new BigDecimal("20")), Range.closed(new BigDecimal("5"), new BigDecimal("20")));
        var result = analyzer.analyze(ctx);
        assertFalse(result.getPatterns().contains(PatternType.SURGE));
    }

    private static AnalysisContext baseContext(BigDecimal fee, int mempoolSize, double fastFee,
                                               Range<BigDecimal> iqr, Range<BigDecimal> fences) {
        var tx = new com.blockchain.blockpulseservice.model.domain.Transaction("tx", fee, BigDecimal.ZERO, 100, Instant.EPOCH);
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
                .mempoolStats(new MempoolStats(fastFee, 0, 0, mempoolSize))
                .build();
    }
}

