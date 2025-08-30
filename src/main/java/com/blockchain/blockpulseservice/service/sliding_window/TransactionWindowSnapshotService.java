package com.blockchain.blockpulseservice.service.sliding_window;

import com.blockchain.blockpulseservice.model.domain.Transaction;
import com.blockchain.blockpulseservice.model.domain.TransactionWindowSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
public class TransactionWindowSnapshotService {
    private final TransactionsPercentile percentile;
    private static final double FIRST_QUARTILE_THRESHOLD = 0.25;
    private static final double THIRD_QUARTILE_THRESHOLD = 0.75;
    private final double outliersPercentileThreshold;
    private BigDecimal sum = BigDecimal.ZERO;

    public TransactionWindowSnapshotService(TransactionsPercentile percentile,
                                            @Value("${app.analysis.tx.outliers-percentile-threshold:0.99}")
                                            double outliersPercentileThreshold) {
        this.percentile = percentile;
        this.outliersPercentileThreshold = outliersPercentileThreshold;
    }

    public void addFee(BigDecimal feePerVSize) {
        this.sum = sum.add(feePerVSize);
    }

    public void subtractFee(BigDecimal feePerVSize) {
        this.sum = sum.subtract(feePerVSize);
    }

    public TransactionWindowSnapshot takeCurrentWindowSnapshot(List<Transaction> sortedTransactions) {
        log.debug("Taking current window snapshot...");
        if (sortedTransactions.isEmpty()) {
            log.debug("No transactions in window, returning empty snapshot");
            return TransactionWindowSnapshot.empty();
        }

        int totalTransactions = sortedTransactions.size();
        var avgFeePerVByte = sum.divide(BigDecimal.valueOf(totalTransactions), 2, RoundingMode.HALF_UP);
        return new TransactionWindowSnapshot(
                totalTransactions,
                avgFeePerVByte,
                percentile.getMedianFeeRate(sortedTransactions),
                percentile.getNumOfOutliers(outliersPercentileThreshold),
                percentile.getPercentileValue(outliersPercentileThreshold, sortedTransactions),
                percentile.getPercentileValue(FIRST_QUARTILE_THRESHOLD, sortedTransactions),
                percentile.getPercentileValue(THIRD_QUARTILE_THRESHOLD, sortedTransactions)
        );
    }
}