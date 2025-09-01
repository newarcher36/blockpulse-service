package com.blockchain.blockpulseservice.model.dto;

import java.math.BigDecimal;

public record TransactionWindowSnapshotDTO(
        int transactionsCount,
        int outliersCount,
        BigDecimal avgFeePerVByte,
        BigDecimal medianFeePerVByte) {}