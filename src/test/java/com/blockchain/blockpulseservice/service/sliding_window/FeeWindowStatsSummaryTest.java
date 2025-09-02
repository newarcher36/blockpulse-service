package com.blockchain.blockpulseservice.service.sliding_window;

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

class FeeWindowStatsSummaryTest {
    static List<BigDecimal> sortedFees;
    static BigDecimal sum;

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
}

