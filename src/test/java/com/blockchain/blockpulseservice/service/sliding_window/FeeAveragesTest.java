package com.blockchain.blockpulseservice.service.sliding_window;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FeeAveragesTest {
    private final FeeAverages averages = new FeeAverages();

    @Test
    void averageExactDivision() {
        var sum = new BigDecimal("10.00");
        var result = averages.average(sum, 4);
        assertEquals(new BigDecimal("2.50"), result);
    }

    @Test
    void averageRoundsHalfUpUpwards() {
        var sum = new BigDecimal("10.005"); // 10.005 / 3 = 3.335 -> 3.34 (HALF_UP)
        var result = averages.average(sum, 3);
        assertEquals(new BigDecimal("3.34"), result);
    }

    @Test
    void averageRoundsHalfUpDownwards() {
        var sum = new BigDecimal("10.004"); // 10.004 / 3 = 3.3346.. -> 3.33
        var result = averages.average(sum, 3);
        assertEquals(new BigDecimal("3.33"), result);
    }

    @Test
    void averageSmallValues() {
        var sum = new BigDecimal("1"); // 1 / 3 = 0.333.. -> 0.33
        var result = averages.average(sum, 3);
        assertEquals(new BigDecimal("0.33"), result);
    }
}