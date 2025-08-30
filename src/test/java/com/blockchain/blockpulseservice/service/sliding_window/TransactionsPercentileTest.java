package com.blockchain.blockpulseservice.service.sliding_window;

import com.blockchain.blockpulseservice.model.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransactionsPercentileTest {

    private final TransactionsPercentile percentile = new TransactionsPercentile();

    private static Transaction tx(String id, String fee) {
        return new Transaction(id, new BigDecimal(fee), BigDecimal.ZERO, 100, Instant.EPOCH);
    }

    private static List<Transaction> sortedTxs(String... fees) {
        List<Transaction> txs = new ArrayList<>();
        for (int i = 0; i < fees.length; i++) {
            txs.add(tx("tx-" + i, fees[i]));
        }
        return txs;
    }

    @Test
    void getPercentileFeeRateBasicCases() {
        // Sorted ascending by feePerVSize
        List<Transaction> txs = sortedTxs("1", "2", "3", "4", "5");

        // 0th percentile -> index ceil(0*5)-1 => -1 => max(0, -1) => 0 => first
        assertEquals(new BigDecimal("1"), percentile.getPercentileFeeRate(0.0, txs));

        // 25th percentile -> ceil(0.25*5)-1 => ceil(1.25)-1 => 1
        assertEquals(new BigDecimal("2"), percentile.getPercentileFeeRate(0.25, txs));

        // 75th percentile -> ceil(0.75*5)-1 => ceil(3.75)-1 => 3
        assertEquals(new BigDecimal("4"), percentile.getPercentileFeeRate(0.75, txs));

        // 99th percentile -> ceil(0.99*5)-1 => ceil(4.95)-1 => 4 => last
        assertEquals(new BigDecimal("5"), percentile.getPercentileFeeRate(0.99, txs));

        // 100th percentile -> ceil(1.0*5)-1 => 5-1 => 4
        assertEquals(new BigDecimal("5"), percentile.getPercentileFeeRate(1.0, txs));
    }

    @Test
    void getPercentileFeeRateSingletonList() {
        List<Transaction> txs = sortedTxs("42");
        // Any percentile should yield the only element
        assertEquals(new BigDecimal("42"), percentile.getPercentileFeeRate(0.0, txs));
        assertEquals(new BigDecimal("42"), percentile.getPercentileFeeRate(0.5, txs));
        assertEquals(new BigDecimal("42"), percentile.getPercentileFeeRate(1.0, txs));
    }

    @Test
    void getNumOfOutliersComputesIndex() {
        // For size N, returns ceil(p*N)-1
        assertEquals(0, percentile.getNumOfOutliers(0.99, 1));
        assertEquals(1, percentile.getNumOfOutliers(0.99, 2));
        assertEquals(4, percentile.getNumOfOutliers(0.99, 5));
        assertEquals(98, percentile.getNumOfOutliers(0.99, 100));
        assertEquals(24, percentile.getNumOfOutliers(0.25, 100));
        assertEquals(74, percentile.getNumOfOutliers(0.75, 100));
    }

    @Test
    void getMedianFeeRateOddAndEven() {
        var odd = sortedTxs("1", "2", "3", "4", "5");
        assertEquals(new BigDecimal("3"), percentile.getMedianFeeRate(odd));

        var even = sortedTxs("1.0", "2.0", "3.0", "4.0");
        assertEquals(new BigDecimal("2.5"), percentile.getMedianFeeRate(even));

        var evenWithDecimals = sortedTxs("1.1", "2.2", "3.3", "4.4");
        assertEquals(new BigDecimal("2.75"), percentile.getMedianFeeRate(evenWithDecimals));
    }
}