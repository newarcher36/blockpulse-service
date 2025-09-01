package com.blockchain.blockpulseservice.service.analysis;

import com.blockchain.blockpulseservice.model.domain.AnalysisContext;
import org.springframework.stereotype.Component;

@Component
public class OutlierAnalyzer extends BaseTransactionAnalyzer {
    @Override
    protected AnalysisContext doAnalyze(AnalysisContext context) {
        var feePerVSize = context.getNewTransaction().feePerVSize();
        boolean isOutOfRange = !context.getTransactionWindowSnapshot().tukeyFences().contains(feePerVSize);
        return context
                .toBuilder()
                .isOutlier(isOutOfRange)
                .build();
    }
}