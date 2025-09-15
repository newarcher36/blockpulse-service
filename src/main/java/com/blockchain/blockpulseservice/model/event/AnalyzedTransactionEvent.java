package com.blockchain.blockpulseservice.model.event;

import com.blockchain.blockpulseservice.model.domain.PriceTier;
import com.blockchain.blockpulseservice.model.domain.PatternSignal;
import com.blockchain.blockpulseservice.model.dto.TransactionWindowSnapshotDTO;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record AnalyzedTransactionEvent(String id,
                                       Instant producedAt,
                                       BigDecimal feePerVByte,
                                       BigDecimal totalFee,
                                       int txSize,
                                       Instant timestamp,
                                       PatternSignal patternSignal,
                                       PriceTier priceTier,
                                       boolean isOutlier,
                                       TransactionWindowSnapshotDTO windowSnapshot) {}
