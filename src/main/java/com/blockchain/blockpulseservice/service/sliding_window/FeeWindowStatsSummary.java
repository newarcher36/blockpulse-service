package com.blockchain.blockpulseservice.service.sliding_window;

import com.google.common.collect.Range;
import org.apache.commons.statistics.descriptive.Quantile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

@Component
public class FeeWindowStatsSummary {
    private static final double TUKEY_FENCE_K_CONSTANT = 1.5;
    private static final double FIRST_QUARTILE_THRESHOLD = 0.25;
    private static final double THIRD_QUARTILE_THRESHOLD = 0.75;

    public int countOutliers(List<BigDecimal> sortedFees) {
        var tukeyFences = calculateTukeyFences(sortedFees);
        var lowerEndpoint = tukeyFences.lowerEndpoint();
        var upperEndpoint = tukeyFences.upperEndpoint();
        int lowerIndex = Collections.binarySearch(sortedFees, lowerEndpoint);
        int upperIndex = Collections.binarySearch(sortedFees, upperEndpoint);
        lowerIndex = lowerIndex < 0 ? Math.abs(lowerIndex) - 1 : lowerIndex;
        upperIndex = upperIndex < 0 ? Math.abs(upperIndex) - 1 : upperIndex;
        return upperIndex - lowerIndex + 1;
    }

    public BigDecimal calculateAvg(BigDecimal sum, int feesCount) {
        return sum.divide(BigDecimal.valueOf(feesCount), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateMedian(List<BigDecimal> sortedFees) {
        return sortedFees.get(sortedFees.size() / 2);
    }

    public Range<BigDecimal> calculateTukeyFences(List<BigDecimal> fees) {
        return calculateTukeyFences(fees, TUKEY_FENCE_K_CONSTANT);
    }

    public BigDecimal calculatePercentile(double percentileThreshold, List<BigDecimal> fees) {
        double percentileValue = Quantile
                .withDefaults()
                .with(Quantile.EstimationMethod.HF7)
                .evaluate(fees.size(), i -> fees.get(i).doubleValue(), percentileThreshold);
        return BigDecimal.valueOf(percentileValue);
    }

    public Range<BigDecimal> calculateIQR(List<BigDecimal> sortedFees) {
        return Range.closed(
                calculatePercentile(FIRST_QUARTILE_THRESHOLD, sortedFees),
                calculatePercentile(THIRD_QUARTILE_THRESHOLD, sortedFees)
        );
    }

    private Range<BigDecimal> calculateTukeyFences(List<BigDecimal> fees, double k) {
        var q1 = calculatePercentile(FIRST_QUARTILE_THRESHOLD, fees);
        var q3 = calculatePercentile(THIRD_QUARTILE_THRESHOLD, fees);
        var iqr = q3.subtract(q1);
        var range = iqr.multiply(BigDecimal.valueOf(k));
        var lower = q1.subtract(range);
        var upper = q3.add(range);
        return Range.closed(lower, upper);
    }
}