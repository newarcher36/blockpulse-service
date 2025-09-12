package com.blockchain.blockpulseservice.service.analysis;

import com.blockchain.blockpulseservice.model.domain.AnalysisContext;
import org.springframework.stereotype.Component;

@Component
public class OutlierFeeAnalyzer extends BaseFeeAnalyzer {
    @Override
    protected AnalysisContext doAnalyze(AnalysisContext context) {
        var feePerVSize = context.getNewTransaction().feePerVSize();
        boolean isOutOfRange = !context.getFeeWindowStatsSummary().tukeyFences().contains(feePerVSize);
        return context
                .toBuilder()
                .isOutlier(isOutOfRange)
                .build();
    }
}