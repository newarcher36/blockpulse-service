package com.blockchain.blockpulseservice.service.analysis;

import com.blockchain.blockpulseservice.model.domain.AnalysisContext;
import com.blockchain.blockpulseservice.model.domain.PatternType;
import com.blockchain.blockpulseservice.model.domain.PatternSignal;
import com.blockchain.blockpulseservice.model.domain.PatternMetric;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class ScamFeeAnalyzer extends BaseFeeAnalyzer {
    @Override
    protected AnalysisContext doAnalyze(AnalysisContext context) {
        var fee = context.getNewTransaction().feePerVSize();
        var lowerFence = context.getFeeWindowStatsSummary().tukeyFences().lowerEndpoint();

        if (fee.compareTo(lowerFence) < 0) {
            return context.toBuilder()
                    .patternSignal(new PatternSignal(
                            PatternType.SCAM,
                            Map.of(PatternMetric.LOWER_TUKEY_FENCE, lowerFence.doubleValue())
                    ))
                    .build();
        }

        return context;
    }
}
