package com.blockchain.blockpulseservice.service.analysis;

import com.blockchain.blockpulseservice.model.domain.AnalysisContext;
import com.blockchain.blockpulseservice.model.domain.MempoolStats;
import com.blockchain.blockpulseservice.model.domain.PriceTier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PriceTierAnalyzer extends BaseFeeAnalyzer {
    private final int mempoolSizeThreshold;
    private final PriceTierClassifier classifier;

    public PriceTierAnalyzer(@Value("${app.analysis.tx.mempool-congestion-vbytes-threshold}") int mempoolSizeThreshold,
                             PriceTierClassifier classifier) {
        this.mempoolSizeThreshold = mempoolSizeThreshold;
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
        return mempool.mempoolSize() > mempoolSizeThreshold;
    }

    // helpers retained only for readability of the mempool check
    }
