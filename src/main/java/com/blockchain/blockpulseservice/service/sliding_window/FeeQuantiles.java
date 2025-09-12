package com.blockchain.blockpulseservice.service.sliding_window;

import org.apache.commons.statistics.descriptive.Quantile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class FeeQuantiles {
    private static final Quantile Q = Quantile.withDefaults().with(Quantile.EstimationMethod.HF7);

    public BigDecimal q1(List<BigDecimal> fees) {
        return percentile(0.25, fees);
    }

    public BigDecimal median(List<BigDecimal> fees) {
        return percentile(0.50, fees);
    }

    public BigDecimal q3(List<BigDecimal> fees) {
        return percentile(0.75, fees);
    }

    private BigDecimal percentile(double p, List<BigDecimal> fees) {
        double v = Q.evaluate(fees.size(), i -> fees.get(i).doubleValue(), p);
        return BigDecimal.valueOf(v);
    }
}

