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
    private final double mempoolSizeCongestionThreshold;

    public SurgeFeeAnalyzer(@Value("${app.analysis.tx.mempool-congestion-vbytes-threshold}") double mempoolSizeCongestionThreshold) {
        this.mempoolSizeCongestionThreshold = mempoolSizeCongestionThreshold;
    }

    @Override
    protected AnalysisContext doAnalyze(AnalysisContext context) {
        var mempoolStats = context.getMempoolStats();
        var feePerVSize = context.getNewTransaction().feePerVSize();
        var upperFence = context.getFeeWindowStatsSummary().tukeyFences().upperEndpoint();
        var isSurge = isBeyondUpperFence(feePerVSize, upperFence) &&
                isFarBeyondRecommendedFastFee(feePerVSize, mempoolStats.fastFeePerVByte()) &&
                isMempoolCongested(mempoolStats);
        if (isSurge) {
            log.debug("Surge detected for tx: {}", context.getNewTransaction().id());
            return context.toBuilder()
                    .pattern(PatternType.SURGE)
                    .build();
        }
        return context;
    }

    private static boolean isBeyondUpperFence(BigDecimal feePerVSize, BigDecimal upperEndpoint) {
        return feePerVSize.compareTo(upperEndpoint) > 0;
    }

    private boolean isFarBeyondRecommendedFastFee(BigDecimal feePerVSize, double fastFeePerVByte) {
        return feePerVSize.compareTo(BigDecimal.valueOf(fastFeePerVByte)) > 0;
    }

    private boolean isMempoolCongested(MempoolStats mempoolStats) {
        return mempoolStats.mempoolSize() >= mempoolSizeCongestionThreshold;
    }
}