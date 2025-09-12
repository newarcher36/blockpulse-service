package com.blockchain.blockpulseservice.service.sliding_window;

import com.blockchain.blockpulseservice.model.domain.FeeWindowStatsSummary;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.common.collect.TreeMultiset;
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

    public FeeWindowStatsSummary calculateComprehensiveStats(TreeMultiset<BigDecimal> sortedFees) {
        if (sortedFees.isEmpty()) {
            return FeeWindowStatsSummary.empty();
        }
        var aggregate = aggregateFees(sortedFees);
        var feeList = aggregate.sortedFees();
        var sum = aggregate.sum();
        var fences = tukeyFences(feeList);
        return FeeWindowStatsSummary.builder()
                .transactionCount(feeList.size())
                .outliersCount(outlierCounter.countOutliers(sortedFees, fences))
                .avgFeePerVByte(average(sum, feeList.size()))
                .median(median(feeList))
                .iqrRange(iqrRange(feeList))
                .tukeyFences(fences)
                .build();
    }

    private FeesAggregate aggregateFees(TreeMultiset<BigDecimal> fees) {
        var listBuilder = ImmutableList.<BigDecimal>builderWithExpectedSize(fees.size());
        var sum = BigDecimal.ZERO;
        for (var f : fees) {
            listBuilder.add(f);
            sum = sum.add(f);
        }
        return new FeesAggregate(listBuilder.build(), sum);
    }

    private BigDecimal average(BigDecimal sum, int count) {
        return feeAverages.average(sum, count);
    }

    private BigDecimal median(List<BigDecimal> sortedFees) {
        return feeQuantiles.median(sortedFees);
    }

    private Range<BigDecimal> iqrRange(List<BigDecimal> sortedFees) {
        return tukey.iqrRange(sortedFees);
    }

    private Range<BigDecimal> tukeyFences(List<BigDecimal> fees) {
        return tukey.tukeyFences(fees);
    }

    private record FeesAggregate(ImmutableList<BigDecimal> sortedFees, BigDecimal sum) {}
}
