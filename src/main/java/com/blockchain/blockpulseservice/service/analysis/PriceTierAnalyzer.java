package com.blockchain.blockpulseservice.service.analysis;

import com.blockchain.blockpulseservice.model.MempoolStats;
import com.blockchain.blockpulseservice.model.domain.AnalysisContext;
import com.blockchain.blockpulseservice.model.domain.PriceTier;
import com.google.common.collect.Range;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PriceTierAnalyzer extends BaseFeeAnalyzer {
    private final int mempoolSizeThreshold;

    public PriceTierAnalyzer(@Value("${app.analysis.tx.mempool-congestion-vbytes-threshold}") int mempoolSizeThreshold) {
        this.mempoolSizeThreshold = mempoolSizeThreshold;
    }

    @Override
    protected AnalysisContext doAnalyze(AnalysisContext context) {
        var classification = classifyPriceTier(context);
        return context.toBuilder()
                .priceTier(classification)
                .build();
    }

    private PriceTier classifyPriceTier(AnalysisContext context) {
        if (context.isOutlier()) return PriceTier.OUTSIDE_MARKET;

        var fee = context.getNewTransaction().feePerVSize();
        var mempool = context.getMempoolStats();

        if (mempool.mempoolSize() > mempoolSizeThreshold) {
            return classifyUsingMempool(fee, mempool);
        }

        var iqr = context.getFeeWindowStatsSummary().iqrRange();
        return classifyUsingIqr(fee, iqr);
    }

    private PriceTier classifyUsingMempool(BigDecimal fee, MempoolStats stats) {
        var fast = BigDecimal.valueOf(stats.fastFeePerVByte());
        var medium = BigDecimal.valueOf(stats.mediumFeePerVByte());

        if (fee.compareTo(fast) > 0) return PriceTier.CHEAP;
        if (fee.compareTo(medium) <= 0) return PriceTier.NORMAL;
        return PriceTier.EXPENSIVE;
    }

    private PriceTier classifyUsingIqr(BigDecimal fee, Range<BigDecimal> iqr) {
        if (fee.compareTo(iqr.lowerEndpoint()) < 0) return PriceTier.CHEAP;
        if (iqr.contains(fee)) return PriceTier.NORMAL;
        return PriceTier.EXPENSIVE;
    }
}
