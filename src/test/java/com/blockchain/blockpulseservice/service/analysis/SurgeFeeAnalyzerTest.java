package com.blockchain.blockpulseservice.service.analysis;

import com.blockchain.blockpulseservice.model.domain.AnalysisContext;
import com.blockchain.blockpulseservice.model.domain.FeeWindowStatsSummary;
import com.blockchain.blockpulseservice.model.domain.MempoolStats;
import com.blockchain.blockpulseservice.model.domain.PatternSignal;
import com.blockchain.blockpulseservice.model.domain.PatternType;
import com.blockchain.blockpulseservice.model.domain.Transaction;
import com.google.common.collect.Range;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import com.blockchain.blockpulseservice.model.domain.PatternMetric;

class SurgeFeeAnalyzerTest {
    private static final int MEMPOOL_SIZE_FULL_THRESHOLD = 1000;
    private static final Range<BigDecimal> TUKEY_FENCES_LOW_5_HIGH_20 = Range.closed(new BigDecimal("5"), new BigDecimal("20"));
    private final SurgeFeeAnalyzer analyzer = new SurgeFeeAnalyzer(MEMPOOL_SIZE_FULL_THRESHOLD);

    @Test
    void addsSurgeWhenAboveUpperFenceAndFastFeeAndMempoolFull() {
        var feeAboveUpperFence = new BigDecimal("30");
        var recommendedFastFee = 25d;
        var mempoolSizeCongested = MEMPOOL_SIZE_FULL_THRESHOLD + 1;
        var ctx = baseContext(feeAboveUpperFence, mempoolSizeCongested, recommendedFastFee, TUKEY_FENCES_LOW_5_HIGH_20);

        var actualAnalysisContext = analyzer.analyze(ctx);

        assertThat(actualAnalysisContext.getPatternSignal())
                .isNotNull()
                .extracting(PatternSignal::type)
                .isEqualTo(PatternType.SURGE);
        assertThat(actualAnalysisContext.getPatternSignal().metrics())
                .containsEntry(PatternMetric.UPPER_TUKEY_FENCE, 20.0)
                .containsEntry(PatternMetric.MEMPOOL_RECOMMENDED_FEE_PER_VBYTE, recommendedFastFee)
                .containsEntry(PatternMetric.MEMPOOL_SIZE, (double) mempoolSizeCongested);
        assertThat(actualAnalysisContext)
                .usingRecursiveComparison()
                .ignoringFields("patternSignal")
                .isEqualTo(ctx);
    }

    @Test
    void noSurgeWhenBelowUpperFence() {
        var feeBelowUpperFence = new BigDecimal("19.99");
        var recommendedFastFee = 15d;
        var mempoolSizeCongested = MEMPOOL_SIZE_FULL_THRESHOLD + 1;
        var ctx = baseContext(feeBelowUpperFence, mempoolSizeCongested, recommendedFastFee, TUKEY_FENCES_LOW_5_HIGH_20);

        var actualAnalysisContext = analyzer.analyze(ctx);

        assertThat(actualAnalysisContext.getPatternSignal()).isNull();
        assertThat(actualAnalysisContext).isEqualTo(ctx);
    }

    @Test
    void noSurgeWhenNotBeyondFastRecommended() {
        var feeNotBeyondFast = new BigDecimal("22");
        var recommendedFastFee = 25d;
        var mempoolSizeCongested = MEMPOOL_SIZE_FULL_THRESHOLD + 1;
        var ctx = baseContext(feeNotBeyondFast, mempoolSizeCongested, recommendedFastFee, TUKEY_FENCES_LOW_5_HIGH_20);

        var actualAnalysisContext = analyzer.analyze(ctx);

        assertThat(actualAnalysisContext.getPatternSignal()).isNull();
        assertThat(actualAnalysisContext).isEqualTo(ctx);
    }

    @Test
    void noSurgeWhenMempoolNotFull() {
        var feeAboveUpperFence = new BigDecimal("30");
        var mempoolSizeNotFull = MEMPOOL_SIZE_FULL_THRESHOLD - 1;
        var recommendedFastFee = 25d;
        var ctx = baseContext(feeAboveUpperFence, mempoolSizeNotFull, recommendedFastFee, TUKEY_FENCES_LOW_5_HIGH_20);

        var actualAnalysisContext = analyzer.analyze(ctx);

        assertThat(actualAnalysisContext.getPatternSignal()).isNull();
        assertThat(actualAnalysisContext).isEqualTo(ctx);
    }

    private static AnalysisContext baseContext(BigDecimal fee, int mempoolSize, double fastFee, Range<BigDecimal> fences) {
        var tx = new Transaction("tx", fee, BigDecimal.ZERO, 100, Instant.EPOCH);
        var summary = FeeWindowStatsSummary.builder()
                .transactionCount(1)
                .outliersCount(1)
                .avgFeePerVByte(Mockito.mock(BigDecimal.class))
                .median(Mockito.mock(BigDecimal.class))
                .iqrRange(Mockito.mock(Range.class))
                .tukeyFences(fences)
                .build();
        return AnalysisContext.builder()
                .newTransaction(tx)
                .feeWindowStatsSummary(summary)
                .mempoolStats(MempoolStats.builder()
                        .fastFeePerVByte(fastFee)
                        .mediumFeePerVByte(1)
                        .slowFeePerVByte(1)
                        .mempoolSize(mempoolSize)
                        .build())
                .build();
    }
}
