package com.blockchain.blockpulseservice.service;

import com.blockchain.blockpulseservice.client.rest.MempoolStatsUpdater;
import com.blockchain.blockpulseservice.model.domain.AnalysisContext;
import com.blockchain.blockpulseservice.model.domain.Transaction;
import com.blockchain.blockpulseservice.model.domain.TransactionWindowSnapshot;
import com.blockchain.blockpulseservice.service.analysis.FeeAnalyzer;
import com.blockchain.blockpulseservice.service.mapper.AnalyzedTransactionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionAnalyzerService {
    private final FeeAnalyzer analysisChain;
    private final AnalysisStream analysisStream;
    private final MempoolStatsUpdater mempoolStatsUpdater;
    private final AnalyzedTransactionMapper analyzedTransactionMapper;

    public void processTransaction(Transaction transaction, TransactionWindowSnapshot transactionWindowSnapshot) {
        log.debug("Processing transaction: {}", transaction.hash());
        var context = AnalysisContext.builder()
                .newTransaction(transaction)
                .transactionWindowSnapshot(transactionWindowSnapshot)
                .mempoolStats(mempoolStatsUpdater.getMempoolStats())
                .build();

        var result = analysisChain.analyze(context);
        var analyzedTransaction = analyzedTransactionMapper.map(result);
        log.debug("Analyzed transaction: {}", analyzedTransaction);
        analysisStream.publish(analyzedTransaction);
    }
}
