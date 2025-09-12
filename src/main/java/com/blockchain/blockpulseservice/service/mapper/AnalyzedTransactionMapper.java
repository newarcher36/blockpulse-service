package com.blockchain.blockpulseservice.service.mapper;

import com.blockchain.blockpulseservice.model.event.AnalyzedTransactionEvent;
import com.blockchain.blockpulseservice.model.domain.AnalysisContext;
import com.blockchain.blockpulseservice.model.domain.FeeWindowStatsSummary;
import com.blockchain.blockpulseservice.model.dto.TransactionWindowSnapshotDTO;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
public class AnalyzedTransactionMapper {
    private final Clock clock;

    public AnalyzedTransactionMapper(Clock clock) {
        this.clock = clock;
    }

    public AnalyzedTransactionEvent map(AnalysisContext context) {
        return AnalyzedTransactionEvent.builder()
                .id(context.getNewTransaction().id())
                .producedAt(Instant.now(clock))
                .feePerVByte(context.getNewTransaction().feePerVSize())
                .totalFee(context.getNewTransaction().totalFee())
                .txSize(context.getNewTransaction().vSize())
                .timestamp(context.getNewTransaction().time())
                .patternTypes(context.getPatterns())
                .priceTier(context.getPriceTier())
                .isOutlier(context.isOutlier())
                .windowSnapshot(mapToTransactionWindowSnapshotDTO(context.getFeeWindowStatsSummary()))
                .build();
    }

    private TransactionWindowSnapshotDTO mapToTransactionWindowSnapshotDTO(FeeWindowStatsSummary windowSnapshot) {
        return new TransactionWindowSnapshotDTO(
                windowSnapshot.transactionCount(),
                windowSnapshot.outliersCount(),
                windowSnapshot.avgFeePerVByte(),
                windowSnapshot.median()
        );
    }
}