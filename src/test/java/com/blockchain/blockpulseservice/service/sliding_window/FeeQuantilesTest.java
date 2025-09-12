package com.blockchain.blockpulseservice.service.sliding_window;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FeeQuantilesTest {
    private final FeeQuantiles quantiles = new FeeQuantiles();

    @Test
    void quartilesOnOddSizedList() {
        List<BigDecimal> fees = List.of(
                new BigDecimal("1"),
                new BigDecimal("2"),
                new BigDecimal("3"),
                new BigDecimal("4"),
                new BigDecimal("5")
        );

        assertEquals(new BigDecimal("2.0"), quantiles.q1(fees));
        assertEquals(new BigDecimal("3.0"), quantiles.median(fees));
        assertEquals(new BigDecimal("4.0"), quantiles.q3(fees));
    }

    @Test
    void quartilesOnEvenSizedList() {
        List<BigDecimal> fees = List.of(
                new BigDecimal("1"),
                new BigDecimal("2"),
                new BigDecimal("3"),
                new BigDecimal("4"),
                new BigDecimal("5"),
                new BigDecimal("6"),
                new BigDecimal("7"),
                new BigDecimal("8")
        );

        assertEquals(new BigDecimal("2.75"), quantiles.q1(fees));
        assertEquals(new BigDecimal("4.5"), quantiles.median(fees));
        assertEquals(new BigDecimal("6.25"), quantiles.q3(fees));
    }
}

