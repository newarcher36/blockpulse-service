package com.blockchain.blockpulseservice.service.analysis;

import com.blockchain.blockpulseservice.model.domain.AnalysisContext;
import com.blockchain.blockpulseservice.model.domain.FeeWindowStatsSummary;
import com.blockchain.blockpulseservice.model.domain.MempoolStats;
import com.blockchain.blockpulseservice.model.domain.PriceTier;
import com.blockchain.blockpulseservice.model.domain.Transaction;
import com.blockchain.blockpulseservice.service.analysis.price_tier.PriceTierAnalyzer;
import com.blockchain.blockpulseservice.service.analysis.price_tier.PriceTierClassifier;
import com.google.common.collect.Range;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceTierAnalyzerTest {
    private static final int MEMPOOL_SIZE_THRESHOLD = 1000;
    private static final Range<BigDecimal> DEFAULT_IQR_RANGE = Range.closed(new BigDecimal("10"), new BigDecimal("20"));
    @Mock
    private PriceTierClassifier priceTierClassifier;
    private PriceTierAnalyzer priceTierAnalyzer;

    @BeforeEach
    void setUp() {
        priceTierAnalyzer = new PriceTierAnalyzer(MEMPOOL_SIZE_THRESHOLD, priceTierClassifier);
    }

    @Test
    void returnsAbnormalPriceWhenOutlier() {
        var fee = new BigDecimal("15");
        var baseCtx = baseContext(fee, MEMPOOL_SIZE_THRESHOLD)
                .toBuilder()
                .isOutlier(true)
                .build();

        var actualCtx = priceTierAnalyzer.analyze(baseCtx);

        assertEquals(PriceTier.ABNORMAL_PRICE, actualCtx.getPriceTier());
        assertThat(actualCtx)
                .usingRecursiveComparison()
                .ignoringFields("priceTier")
                .isEqualTo(baseCtx);
        verifyNoInteractions(priceTierClassifier);
    }

    @Test
    void analyzePriceWithMempoolStatsWhenCongested() {
        var fee = new BigDecimal("0");
        var mempoolSizeCongested = MEMPOOL_SIZE_THRESHOLD + 1;
        var baseCtx = baseContext(fee, mempoolSizeCongested);
        when(priceTierClassifier.classifyUsingMempool(any(), any())).thenReturn(PriceTier.CHEAP);

        var actualCtx = priceTierAnalyzer.analyze(baseCtx);

        assertThat(actualCtx.getPriceTier()).isEqualTo(PriceTier.CHEAP);
        assertThat(actualCtx)
                .usingRecursiveComparison()
                .ignoringFields("priceTier")
                .isEqualTo(baseCtx);
        verify(priceTierClassifier).classifyUsingMempool(actualCtx.getNewTransaction().feePerVSize(), actualCtx.getMempoolStats());
        verify(priceTierClassifier, never()).classifyUsingIqr(any(), any());
    }

    @Test
    void analyzePriceUsingIqrTukeyWhenMempoolIsNotCongested() {
        var fee = new BigDecimal("15");
        var mempoolSizeNotCongested = MEMPOOL_SIZE_THRESHOLD - 1;
        var baseCtx = baseContext(fee, mempoolSizeNotCongested);
        when(priceTierClassifier.classifyUsingIqr(any(BigDecimal.class), any(Range.class))).thenReturn(PriceTier.CHEAP);

        var actualCtx = priceTierAnalyzer.analyze(baseCtx);

        assertThat(actualCtx.getPriceTier()).isEqualTo(PriceTier.CHEAP);
        assertThat(actualCtx)
                .usingRecursiveComparison()
                .ignoringFields("priceTier")
                .isEqualTo(baseCtx);
        verify(priceTierClassifier).classifyUsingIqr(actualCtx.getNewTransaction().feePerVSize(), baseCtx.getFeeWindowStatsSummary().iqrRange());
        verify(priceTierClassifier, never()).classifyUsingMempool(any(), any());
    }

    private static AnalysisContext baseContext(BigDecimal fee, int mempoolSize) {
        var tx = new Transaction("tx", fee, BigDecimal.ZERO, 100, Instant.EPOCH);
        var summary = FeeWindowStatsSummary.builder()
                .transactionCount(10)
                .outliersCount(0)
                .avgFeePerVByte(BigDecimal.ZERO)
                .median(BigDecimal.ZERO)
                .iqrRange(DEFAULT_IQR_RANGE)
                .tukeyFences(Mockito.mock(Range.class))
                .build();
        return AnalysisContext.builder()
                .newTransaction(tx)
                .feeWindowStatsSummary(summary)
                .mempoolStats(MempoolStats.builder()
                        .fastFeePerVByte(50)
                        .mediumFeePerVByte(25)
                        .slowFeePerVByte(10)
                        .mempoolSize(mempoolSize)
                        .build())
                .build();
    }
}