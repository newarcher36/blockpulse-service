package com.blockchain.blockpulseservice.model;

import java.math.BigDecimal;

public record TransactionWindowSnapshotDTO(
        int transactionsCount,
        BigDecimal avgFeePerVByte,
        BigDecimal medianFeePerVByte,
        int outliersCount) {
}