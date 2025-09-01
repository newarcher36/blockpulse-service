package com.blockchain.blockpulseservice.service.sliding_window;

import com.google.common.collect.Range;
import org.apache.commons.statistics.descriptive.Mean;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class FeeWindowStatsSummaryTest {
    static List<BigDecimal> sortedFees;
    static BigDecimal sum;
    final FeeWindowStatsSummary summary = new FeeWindowStatsSummary();

    @BeforeAll
    static void loadCsv() throws Exception {
        try (var in = FeeWindowStatsSummaryTest.class.getResourceAsStream("/mempool_txs.csv");
             var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            var lines = reader.lines().skip(1); // skip header
            var fees = new ArrayList<BigDecimal>();
            lines.forEach(line -> {
                var parts = line.split(",");
                if (parts.length >= 2) {
                    fees.add(new BigDecimal(parts[1].trim()));
                }
            });
            sortedFees = fees.stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList());
            sum = sortedFees.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        }
    }

    @Test
    void calculateIqr_andTukeyFences() {
        BigDecimal iqr = summary.calculateIQRTest(sortedFees);
        System.out.println("IQR = " + iqr);
    }

    @Test
    void calculateMedian_fromSortedList() {
        BigDecimal median = summary.calculateMedian(sortedFees);
        System.out.println("MEDIAN = " + median);
    }

    @Test
    void calculateAvg_fromSumAndCount() {
        var avg = summary.calculateAvg(sum, sortedFees.size());
        System.out.println("AVG = " + avg);
    }

    @Test
    void countOutliers_belowAndAbove() {
        int actual = summary.countOutliers(sortedFees);
        System.out.println("OUTLIERS = " + actual);
    }
}

