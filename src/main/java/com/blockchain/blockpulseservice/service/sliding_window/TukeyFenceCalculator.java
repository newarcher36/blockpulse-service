package com.blockchain.blockpulseservice.service.sliding_window;

import com.google.common.collect.Range;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class TukeyFenceCalculator {
    private final FeeQuantiles feeQuantiles;
    private final double k;

    public TukeyFenceCalculator(FeeQuantiles feeQuantiles,
                                @Value("${app.analysis.tx.tukey-k:1.5}")
                                double k) {
        this.feeQuantiles = feeQuantiles;
        this.k = k;
    }

    public Range<BigDecimal> iqrRange(List<BigDecimal> fees) {
        var q1 = feeQuantiles.q1(fees);
        var q3 = feeQuantiles.q3(fees);
        return Range.closed(q1, q3);
    }

    public Range<BigDecimal> tukeyFences(List<BigDecimal> fees) {
        var q1 = feeQuantiles.q1(fees);
        var q3 = feeQuantiles.q3(fees);
        var iqr = q3.subtract(q1);
        var range = iqr.multiply(BigDecimal.valueOf(k));
        var lower = q1.subtract(range);
        if (lower.signum() < 0) {
            lower = BigDecimal.ZERO;
        }
        var upper = q3.add(range);
        return Range.closed(lower, upper);
    }
}
