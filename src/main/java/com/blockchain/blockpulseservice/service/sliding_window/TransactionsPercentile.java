package com.blockchain.blockpulseservice.service.sliding_window;

import com.blockchain.blockpulseservice.model.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class TransactionsPercentile {
    public BigDecimal getPercentileFeeRate(double percentile, List<Transaction> transactions) {
        int index = getPercentileIndex(percentile, transactions.size());
        return transactions.get(Math.max(0,index)).feePerVSize();
    }

    public int getNumOfOutliers(double outlierPercentileThreshold, int totalTransactions) {
        return getPercentileIndex(outlierPercentileThreshold, totalTransactions);
    }

    public BigDecimal getMedianFeeRate(List<Transaction> transactions) {
        int size = transactions.size();
        int mid = size / 2;

        if (size % 2 == 0) {
            BigDecimal lowerMid = transactions.get(mid - 1).feePerVSize();
            BigDecimal upperMid = transactions.get(mid).feePerVSize();
            return lowerMid.add(upperMid).divide(BigDecimal.valueOf(2), RoundingMode.HALF_UP);
        } else {
            return transactions.get(mid).feePerVSize();
        }
    }

    private int getPercentileIndex(double percentile, int totalTransactions) {
        return (int) Math.ceil(percentile * totalTransactions) - 1;
    }
}