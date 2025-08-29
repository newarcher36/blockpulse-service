package com.blockchain.blockpulseservice.model.dto;

import com.blockchain.blockpulseservice.model.domain.FeeClassification;
import com.blockchain.blockpulseservice.model.domain.PatternType;
import com.blockchain.blockpulseservice.model.TransactionWindowSnapshotDTO;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

@Builder
public record AnalyzedTransactionDTO(String id,
                                     int seq,
                                     Instant producedAt,
                                     BigDecimal feePerVByte,
                                     BigDecimal totalFee,
                                     int size,
                                     Instant timestamp,
                                     Set<PatternType> patternTypes,
                                     FeeClassification feeClassification,
                                     boolean isOutlier,
                                     TransactionWindowSnapshotDTO windowSnapshot) {}