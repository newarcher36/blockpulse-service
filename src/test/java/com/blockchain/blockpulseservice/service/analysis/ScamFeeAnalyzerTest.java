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

import com.blockchain.blockpulseservice.model.domain.PatternMetric;
import static org.assertj.core.api.Assertions.assertThat;

class ScamFeeAnalyzerTest {
    private static final Range<BigDecimal> TUKEY_FENCES_LOW_5_HIGH_20 = Range.closed(new BigDecimal("5"), new BigDecimal("20"));
    private final ScamFeeAnalyzer analyzer = new ScamFeeAnalyzer();

    @Test
    void addsScamPatternWhenBelowLowerFence() {
        var statsSummary = summary(TUKEY_FENCES_LOW_5_HIGH_20);
        var analysisContext = analyzer.analyze(ctx(new BigDecimal("4.99"), statsSummary));
        assertThat(analysisContext.getPatternSignal()).isNotNull();
        assertThat(analysisContext.getPatternSignal().type()).isEqualTo(PatternType.SCAM);
        assertThat(analysisContext.getPatternSignal().metrics())
                .containsEntry(PatternMetric.LOWER_TUKEY_FENCE, 5.0);
    }

    @Test
    void doesNotAddScamWhenEqualsLowerFence() {
        var statsSummary = summary(TUKEY_FENCES_LOW_5_HIGH_20);
        var analysisContext = analyzer.analyze(ctx(new BigDecimal("5.00"), statsSummary));
        assertThat(analysisContext.getPatternSignal()).isNull();
    }

    @Test
    void doesNotAddScamWhenAboveLowerFence() {
        var statsSummary = summary(TUKEY_FENCES_LOW_5_HIGH_20);
        var analysisContext = analyzer.analyze(ctx(new BigDecimal("10.00"), statsSummary));
        assertThat(analysisContext.getPatternSignal()).isNull();
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
