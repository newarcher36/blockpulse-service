package com.blockchain.blockpulseservice.event;

import com.blockchain.blockpulseservice.model.domain.PriceTier;
import com.blockchain.blockpulseservice.model.domain.PatternType;
import com.blockchain.blockpulseservice.model.dto.TransactionWindowSnapshotDTO;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

@Builder
public record AnalyzedTransactionEvent(String id,
                                       int seq,
                                       Instant producedAt,
                                       BigDecimal feePerVByte,
                                       BigDecimal totalFee,
                                       int size,
                                       Instant timestamp,
                                       Set<PatternType> patternTypes,
                                       PriceTier priceTier,
                                       boolean isOutlier,
                                       TransactionWindowSnapshotDTO windowSnapshot) {}