package com.blockchain.blockpulseservice.model.dto;

public record RecommendedTransactionFeeDTO(double fastestFee,
                                           double halfHourFee,
                                           double hourFee,
                                           double economyFee
) {}