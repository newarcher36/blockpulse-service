package com.blockchain.blockpulseservice.service.sliding_window;

import com.blockchain.blockpulseservice.model.event.NewTransactionEvent;
import com.blockchain.blockpulseservice.model.domain.Transaction;
import com.blockchain.blockpulseservice.service.TransactionAnalyzerService;
import com.google.common.collect.TreeMultiset;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.Deque;

import static java.math.BigDecimal.ZERO;

@Slf4j
@Component
public class SlidingWindowManager {
    private final TreeMultiset<BigDecimal> sortedFees = TreeMultiset.create();
    private final Deque<BigDecimal> feeInsertionOrder = new ArrayDeque<>();
    private final int slidingWindowSize;
    private final TransactionAnalyzerService analyzerService;
    private final FeeWindowStatsSummaryCalculator feeWindowStatsSummaryCalculator;

    public SlidingWindowManager(@Value("${app.analysis.tx.sliding-window-size:1000}") int slidingWindowSize,
                                TransactionAnalyzerService analyzerService,
                                FeeWindowStatsSummaryCalculator feeWindowStatsSummaryCalculator) {
        this.slidingWindowSize = slidingWindowSize;
        this.analyzerService = analyzerService;
        this.feeWindowStatsSummaryCalculator = feeWindowStatsSummaryCalculator;
    }

    @Async
    @EventListener
    public void onNewTransaction(NewTransactionEvent event) {
        var tx = event.transaction();
        if (!isValidTransaction(tx)) {
            log.warn("Invalid transaction: {}", tx);
            return;
        }

        resizeSlidingWindowIfFull();

        sortedFees.add(tx.feePerVSize());
        feeInsertionOrder.addLast(tx.feePerVSize());

        var feeWindowStatsSummary = feeWindowStatsSummaryCalculator.calculateComprehensiveStats(sortedFees);
        analyzerService.processTransaction(tx, feeWindowStatsSummary);
    }

    private void resizeSlidingWindowIfFull() {
        if (sortedFees.size() >= slidingWindowSize) {
            var oldestFee = feeInsertionOrder.pollFirst();
            log.debug("Sliding window is full, removing oldest tx feePerVSize: {}", oldestFee);
            sortedFees.remove(oldestFee);
        }
    }

    private boolean isValidTransaction(Transaction tx) {
        if (tx.feePerVSize().compareTo(ZERO) < 0) {
            log.warn("Invalid fee rate: {}", tx.feePerVSize());
            return false;
        }
        if (tx.vSize() <= 0) {
            log.warn("Invalid transaction vSize: {}", tx.vSize());
            return false;
        }
        return true;
    }
}