package com.blockchain.blockpulseservice.service.analysis.price_tier;

import com.blockchain.blockpulseservice.model.domain.AnalysisContext;
import com.blockchain.blockpulseservice.model.domain.MempoolStats;
import com.blockchain.blockpulseservice.model.domain.PriceTier;
import com.blockchain.blockpulseservice.service.analysis.BaseFeeAnalyzer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PriceTierAnalyzer extends BaseFeeAnalyzer {
    private final int mempoolSizeCongestionThreshold;
    private final PriceTierClassifier classifier;

    public PriceTierAnalyzer(@Value("${app.analysis.tx.mempool-congestion-vbytes-threshold}")
                             int mempoolSizeCongestionThreshold,
                             PriceTierClassifier classifier) {
        this.mempoolSizeCongestionThreshold = mempoolSizeCongestionThreshold;
        this.classifier = classifier;
    }

    @Override
    protected AnalysisContext doAnalyze(AnalysisContext context) {
        var classification = classifyPriceTier(context);
        return context.toBuilder()
                .priceTier(classification)
                .build();
    }

    private PriceTier classifyPriceTier(AnalysisContext context) {
        if (context.isOutlier()) return PriceTier.ABNORMAL_PRICE;

        var fee = context.getNewTransaction().feePerVSize();
        var mempool = context.getMempoolStats();

        return isMempoolCongested(mempool) ?
                classifier.classifyUsingMempool(fee, mempool) :
                classifier.classifyUsingIqr(fee, context.getFeeWindowStatsSummary().iqrRange());
    }

    private boolean isMempoolCongested(MempoolStats mempool) {
        return mempool.mempoolSize() > mempoolSizeCongestionThreshold;
    }
}