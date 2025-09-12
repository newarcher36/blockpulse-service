package com.blockchain.blockpulseservice.service.analysis.price_tier;

import com.blockchain.blockpulseservice.model.domain.MempoolStats;
import com.blockchain.blockpulseservice.model.domain.PriceTier;
import com.google.common.collect.Range;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PriceTierClassifier {

    public PriceTier classifyUsingMempool(BigDecimal fee, MempoolStats stats) {
        var fast = BigDecimal.valueOf(stats.fastFeePerVByte());
        var medium = BigDecimal.valueOf(stats.mediumFeePerVByte());

        if (fee.compareTo(fast) > 0) return PriceTier.EXPENSIVE;
        if (fee.compareTo(medium) >= 0) return PriceTier.NORMAL;
        return PriceTier.CHEAP;
    }

    public PriceTier classifyUsingIqr(BigDecimal fee, Range<BigDecimal> iqr) {
        if (fee.compareTo(iqr.lowerEndpoint()) < 0) return PriceTier.CHEAP;
        if (iqr.contains(fee)) return PriceTier.NORMAL;
        return PriceTier.EXPENSIVE;
    }
}
