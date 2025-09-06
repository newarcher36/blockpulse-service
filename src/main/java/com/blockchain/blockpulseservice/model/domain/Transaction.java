package com.blockchain.blockpulseservice.model.domain;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record Transaction(String id,
                          BigDecimal feePerVSize,
                          BigDecimal totalFee,
                          int vSize,
                          Instant time) {
}