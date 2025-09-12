package com.blockchain.blockpulseservice.service;

import com.blockchain.blockpulseservice.service.stream.AnalysisStream;
import com.blockchain.blockpulseservice.model.domain.AnalysisContext;
import com.blockchain.blockpulseservice.model.domain.FeeWindowStatsSummary;
import com.blockchain.blockpulseservice.model.domain.MempoolStats;
import com.blockchain.blockpulseservice.model.domain.Transaction;
import com.blockchain.blockpulseservice.model.event.MempoolStatsUpdatedEvent;
import com.blockchain.blockpulseservice.service.analysis.FeeAnalyzer;
import com.blockchain.blockpulseservice.service.mapper.AnalyzedTransactionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionAnalyzerService {
    private final FeeAnalyzer analysisChain;
    private final AnalysisStream analysisStream;
    private final AnalyzedTransactionMapper analyzedTransactionMapper;
    private final AtomicReference<MempoolStats> mempoolStats = new AtomicReference<>(MempoolStats.empty());

    public void processTransaction(Transaction transaction, FeeWindowStatsSummary feeWindowStatsSummary) {
        log.debug("Processing transaction: {}", transaction.id());
        var context = AnalysisContext.builder()
                .newTransaction(transaction)
                .feeWindowStatsSummary(feeWindowStatsSummary)
                .mempoolStats(mempoolStats.get())
                .build();

        var result = analysisChain.analyze(context);
        var analyzedTransaction = analyzedTransactionMapper.map(result);
        log.debug("Analyzed transaction: {}", analyzedTransaction);
        analysisStream.publish(analyzedTransaction);
    }

    @EventListener
    public void onMempoolStatsUpdated(MempoolStatsUpdatedEvent event) {
        this.mempoolStats.set(event.stats());
        log.debug("Received MempoolStats update: {}", event.stats());
    }
}
