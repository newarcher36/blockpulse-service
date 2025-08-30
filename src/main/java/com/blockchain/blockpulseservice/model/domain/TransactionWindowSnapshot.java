package com.blockchain.blockpulseservice.model.domain;

import java.math.BigDecimal;

import static java.math.BigDecimal.ZERO;

public record TransactionWindowSnapshot(int transactionsCount,
                                        BigDecimal avgFeePerVByte,
                                        BigDecimal medianFeePerVByte,
                                        int outliersCount,
                                        BigDecimal outlierFeePerVBytePercentile,
                                        BigDecimal firstQuartile,
                                        BigDecimal thirdQuartile) {
    public static TransactionWindowSnapshot empty() {
        return new TransactionWindowSnapshot(0, ZERO, ZERO, 0, ZERO,  ZERO,ZERO);
    }
}