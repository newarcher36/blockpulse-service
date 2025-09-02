package com.blockchain.blockpulseservice.service.sliding_window;

import com.blockchain.blockpulseservice.model.domain.FeeWindowStatsSummary;
import com.google.common.collect.Range;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FeeWindowStatsSummaryCalculator {
    private final FeeQuantiles feeQuantiles;
    private final FeeAverages feeAverages;
    private final TukeyFenceCalculator tukey;
    private final OutlierCounter outlierCounter;

    public FeeWindowStatsSummary calculateComprehensiveStats(List<BigDecimal> sortedFees, BigDecimal sum) {
        if (sortedFees.isEmpty()) {
            return FeeWindowStatsSummary.empty();
        }
        var fences = tukeyFences(sortedFees);
        return FeeWindowStatsSummary.builder()
                .transactionCount(sortedFees.size())
                .outliersCount(outlierCounter.countOutliers(sortedFees, fences))
                .avgFeePerVByte(average(sum, sortedFees.size()))
                .median(median(sortedFees))
                .iqrRange(iqrRange(sortedFees))
                .tukeyFences(fences)
                .build();
    }

    private BigDecimal average(BigDecimal sum, int count) {
        return feeAverages.average(sum, count);
    }

    private BigDecimal median(List<BigDecimal> sortedFees) {
        return feeQuantiles.median(sortedFees);
    }

    public Range<BigDecimal> iqrRange(List<BigDecimal> sortedFees) {
        return tukey.iqrRange(sortedFees);
    }

    private Range<BigDecimal> tukeyFences(List<BigDecimal> fees) {
        return tukey.tukeyFences(fees);
    }
}