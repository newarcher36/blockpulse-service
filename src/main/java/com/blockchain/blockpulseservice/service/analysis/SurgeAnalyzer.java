package com.blockchain.blockpulseservice.service.analysis;

import com.blockchain.blockpulseservice.model.domain.AnalysisContext;
import com.blockchain.blockpulseservice.model.domain.PatternType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class SurgeAnalyzer extends BaseTransactionAnalyzer {
    private final double mempoolThreshold;

    public SurgeAnalyzer(@Value("${app.analysis.tx.mempool-congestion-vbytes-threshold}") double mempoolThreshold) {
        this.mempoolThreshold = mempoolThreshold;
    }

    @Override
    protected AnalysisContext doAnalyze(AnalysisContext context) {
        var mempoolStats = context.getMempoolStats();
        var transaction = context.getNewTransaction();
        boolean isSurge = context.isOutlier() &&
                isFarBeyondRecommendedFastFee(transaction.feePerVSize(), mempoolStats.fastFeePerVByte()) &&
                mempoolStats.mempoolSize() >= mempoolThreshold;
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
}