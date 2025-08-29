package com.blockchain.blockpulseservice.service.analysis;

import com.blockchain.blockpulseservice.model.domain.AnalysisContext;
import org.springframework.stereotype.Component;

@Component
public class OutlierAnalyzer extends BaseTransactionAnalyzer {
    @Override
    protected AnalysisContext doAnalyze(AnalysisContext context) {
        var percentile = context.getTransactionWindowSnapshot().outlierFeePerVBytePercentile();
        if (context.getNewTransaction().feePerVSize().compareTo(percentile) > 0) {
            return context
                    .toBuilder()
                    .isOutlier(true)
                    .build();
        }
        return context;
    }
}