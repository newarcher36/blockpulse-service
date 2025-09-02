package com.blockchain.blockpulseservice.service.analysis;

import com.blockchain.blockpulseservice.model.domain.AnalysisContext;
import com.blockchain.blockpulseservice.model.domain.PatternType;
import org.springframework.stereotype.Component;

@Component
public class SpamFeeAnalyzer extends BaseFeeAnalyzer {
    @Override
    protected AnalysisContext doAnalyze(AnalysisContext context) {
        var fee = context.getNewTransaction().feePerVSize();
        var lowerFence = context.getFeeWindowStatsSummary().tukeyFences().lowerEndpoint();

        if (fee.compareTo(lowerFence) < 0) {
            return context.toBuilder()
                    .pattern(PatternType.SCAM)
                    .build();
        }

        return context;
    }
}