package com.blockchain.blockpulseservice.model.domain;

import com.google.common.collect.Range;
import lombok.Builder;

import java.math.BigDecimal;

import static java.math.BigDecimal.ZERO;

@Builder
public record FeeWindowStatsSummary(int transactionCount,
                                    int outliersCount,
                                    BigDecimal avgFeePerVByte,
                                    BigDecimal median,
                                    Range<BigDecimal> iqrRange,
                                    Range<BigDecimal> tukeyFences) {
    public static FeeWindowStatsSummary empty() {
        return new FeeWindowStatsSummary(0, 0, ZERO, ZERO, Range.singleton(ZERO), Range.singleton(ZERO));
    }
}