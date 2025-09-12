package com.blockchain.blockpulseservice.service.sliding_window;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class FeeAverages {
    public BigDecimal average(BigDecimal sum, int count) {
        return sum.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }
}

