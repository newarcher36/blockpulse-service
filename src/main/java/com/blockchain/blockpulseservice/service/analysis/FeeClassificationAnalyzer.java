package com.blockchain.blockpulseservice.service.analysis;

import com.blockchain.blockpulseservice.model.domain.AnalysisContext;
import com.blockchain.blockpulseservice.model.domain.FeeClassification;
import com.google.common.collect.Range;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class FeeClassificationAnalyzer extends BaseTransactionAnalyzer {
    private final int mempoolSizeThreshold;

    public FeeClassificationAnalyzer(@Value("${app.analysis.tx.mempool-congestion-vbytes-threshold}")
                                     int mempoolSizeThreshold) {
        this.mempoolSizeThreshold = mempoolSizeThreshold;
    }

    @Override
    protected AnalysisContext doAnalyze(AnalysisContext context) {
        FeeClassification classification = classifyFee(context);
        return context.toBuilder()
                .feeClassification(classification)
                .build();
    }

    private FeeClassification classifyFee(AnalysisContext context) {
        var mempoolStats = context.getMempoolStats();
        var feePerVSize = context.getNewTransaction().feePerVSize();
        if (mempoolStats.mempoolSize() > mempoolSizeThreshold) {
            // Network congested → use mempool recommended fees
            if (feePerVSize.compareTo(BigDecimal.valueOf(mempoolStats.fastFeePerVByte())) > 0) {
                return FeeClassification.CHEAP;
            } else if (feePerVSize.compareTo(BigDecimal.valueOf(mempoolStats.mediumFeePerVByte())) <= 0) {
                return FeeClassification.NORMAL;
            } else {
                return FeeClassification.EXPENSIVE;
            }
        } else {
            // Normal network → use local percentiles
            var iqrRange = context.getTransactionWindowSnapshot().iqrRange();
            if (feePerVSize.compareTo(iqrRange.lowerEndpoint()) < 0) {
                return FeeClassification.CHEAP;
            } else if (iqrRange.contains(feePerVSize)) {
                return FeeClassification.NORMAL;
            } else {
                return FeeClassification.EXPENSIVE;
            }
        }
    }
}