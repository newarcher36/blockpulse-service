package com.blockchain.blockpulseservice.service.analysis;

import com.blockchain.blockpulseservice.model.domain.AnalysisContext;
import com.blockchain.blockpulseservice.model.domain.MempoolStats;
import com.blockchain.blockpulseservice.model.domain.PatternType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class SurgeFeeAnalyzer extends BaseFeeAnalyzer {
    private final double mempoolSizeFullThreshold;

    public SurgeFeeAnalyzer(@Value("${app.analysis.tx.mempool-congestion-vbytes-threshold}") double mempoolSizeFullThreshold) {
        this.mempoolSizeFullThreshold = mempoolSizeFullThreshold;
    }

    @Override
    protected AnalysisContext doAnalyze(AnalysisContext context) {
        var mempoolStats = context.getMempoolStats();
        var feePerVSize = context.getNewTransaction().feePerVSize();
        var upperEndpoint = context.getFeeWindowStatsSummary().tukeyFences().upperEndpoint();
        var isSurge = isBeyoundUpperFence(feePerVSize, upperEndpoint) &&
                isFarBeyondRecommendedFastFee(feePerVSize, mempoolStats.fastFeePerVByte()) &&
                mempoolIsFull(mempoolStats);
        if (isSurge) {
            log.debug("Surge detected for tx: {}", context.getNewTransaction().hash());
            return context.toBuilder()
                    .pattern(PatternType.SURGE)
                    .build();
        }
        return context;
    }

    private static boolean isBeyoundUpperFence(BigDecimal feePerVSize, BigDecimal upperEndpoint) {
        return feePerVSize.compareTo(upperEndpoint) > 0;
    }

    private boolean isFarBeyondRecommendedFastFee(BigDecimal feePerVSize, double fastFeePerVByte) {
        return isBeyoundUpperFence(feePerVSize, BigDecimal.valueOf(fastFeePerVByte));
    }

    private boolean mempoolIsFull(MempoolStats mempoolStats) {
        return mempoolStats.mempoolSize() >= mempoolSizeFullThreshold;
    }
}