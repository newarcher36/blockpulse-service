package com.blockchain.blockpulseservice.service.analysis;

import com.blockchain.blockpulseservice.model.MempoolStats;
import com.blockchain.blockpulseservice.model.domain.AnalysisContext;
import com.blockchain.blockpulseservice.model.domain.PatternType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class SurgeFeeAnalyzer extends BaseFeeAnalyzer {
    private final double mempoolThreshold;

    public SurgeFeeAnalyzer(@Value("${app.analysis.tx.mempool-congestion-vbytes-threshold}") double mempoolThreshold) {
        this.mempoolThreshold = mempoolThreshold;
    }

    @Override
    protected AnalysisContext doAnalyze(AnalysisContext context) {
        var mempoolStats = context.getMempoolStats();
        var feePerVSize = context.getNewTransaction().feePerVSize();
        var upperEndpoint = context.getTransactionWindowSnapshot().tukeyFences().upperEndpoint();
        boolean isSurge = feePerVSize.compareTo(upperEndpoint) > 0  &&
                isFarBeyondRecommendedFastFee(feePerVSize, mempoolStats.fastFeePerVByte()) &&
                mempoolIsFull(mempoolStats);
        if (isSurge) {
            log.debug("Surge detected for tx: {}", context.getNewTransaction().hash());
            return context
                    .addInsight(PatternType.SURGE)
                    .build();
        }
        return context;
    }

    private boolean isFarBeyondRecommendedFastFee(BigDecimal feePerVSize, double fastFeePerVByte) {
        return feePerVSize.compareTo(BigDecimal.valueOf(fastFeePerVByte)) > 0;
    }

    private boolean mempoolIsFull(MempoolStats mempoolStats) {
        return mempoolStats.mempoolSize() >= mempoolThreshold;
    }
}