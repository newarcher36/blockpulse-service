package com.blockchain.blockpulseservice.model.domain;

import com.google.common.collect.Range;

import java.math.BigDecimal;

import static java.math.BigDecimal.ZERO;

public record TransactionWindowSnapshot(int transactionCount,
                                        int outliersCount,
                                        BigDecimal avgFeePerVByte,
                                        BigDecimal median,
                                        Range<BigDecimal> iqrRange,
                                        Range<BigDecimal> tukeyFences) {
    public static TransactionWindowSnapshot empty() {
        return new TransactionWindowSnapshot(0, 0, ZERO, ZERO, Range.open(ZERO, ZERO), Range.open(ZERO, ZERO));
    }
}