package com.blockchain.blockpulseservice.service;

import com.blockchain.blockpulseservice.client.rest.MempoolStatsUpdater;
import com.blockchain.blockpulseservice.model.domain.Transaction;
import com.blockchain.blockpulseservice.model.domain.TransactionWindowSnapshot;
import com.blockchain.blockpulseservice.model.dto.AnalyzedTransactionDTO;
import com.blockchain.blockpulseservice.model.domain.AnalysisContext;
import com.blockchain.blockpulseservice.service.analysis.TransactionAnalyzer;
import com.blockchain.blockpulseservice.model.TransactionWindowSnapshotDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionAnalyzerService {
    private final AtomicInteger txSequence = new AtomicInteger(0);
    private final TransactionAnalyzer analysisChain;
    private final AnalysisStream analysisStream;
    private final MempoolStatsUpdater mempoolStatsUpdater;

    public void processTransaction(Transaction transaction, TransactionWindowSnapshot transactionWindowSnapshot) {
        log.debug("Processing transaction: {}", transaction.hash());
        try {
            var context = AnalysisContext.builder()
                    .newTransaction(transaction)
                    .transactionWindowSnapshot(transactionWindowSnapshot)
                    .mempoolStats(mempoolStatsUpdater.getMempoolStats())
                    .build();

            var result = analysisChain.analyze(context);
            var analyzedTransaction = mapToAnalyzedTransaction(result);
            log.debug("Analyzed transaction: {}", analyzedTransaction);
            analysisStream.publish(analyzedTransaction);
        } catch (Exception e) {
            log.error("Failed to process transaction {}: {}", transaction.hash(), e.getMessage(), e);
        }
    }

    private AnalyzedTransactionDTO mapToAnalyzedTransaction(AnalysisContext context) {
        return AnalyzedTransactionDTO.builder()
                .id(context.getNewTransaction().hash())
                .seq(txSequence.incrementAndGet())
                .producedAt(Instant.now())
                .feePerVByte(context.getNewTransaction().feePerVSize())
                .totalFee(context.getNewTransaction().totalFee())
                .size(context.getNewTransaction().vSize())
                .timestamp(context.getNewTransaction().time())
                .patternTypes(context.getPatterns())
                .feeClassification(context.getFeeClassification())
                .isOutlier(context.isOutlier())
                .windowSnapshot(mapToTransactionWindowSnapshotDTO(context.getTransactionWindowSnapshot()))
                .build();
    }

    private TransactionWindowSnapshotDTO mapToTransactionWindowSnapshotDTO(TransactionWindowSnapshot windowSnapshot) {
        return new TransactionWindowSnapshotDTO(
                windowSnapshot.transactionsCount(),
                windowSnapshot.avgFeePerVByte(),
                windowSnapshot.medianFeePerVByte(),
                windowSnapshot.outliersCount());
    }
}