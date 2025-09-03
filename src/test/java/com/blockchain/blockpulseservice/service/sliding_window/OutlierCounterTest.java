package com.blockchain.blockpulseservice.service.sliding_window;

import com.google.common.collect.Range;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OutlierCounterTest {
    private final OutlierCounter counter = new OutlierCounter();
    private static final Range<BigDecimal> TUKEY_FENCES_LOW_5_HIGH_20 = Range.closed(new BigDecimal("5"), new BigDecimal("20"));

    private static List<BigDecimal> fees() {
        return List.of(
                new BigDecimal("2"),
                new BigDecimal("2"),
                new BigDecimal("6"),
                new BigDecimal("10"),
                new BigDecimal("10"),
                new BigDecimal("15"),
                new BigDecimal("20"),
                new BigDecimal("25"),
                new BigDecimal("30"),
                new BigDecimal("35")
        );
    }

    @Test
    void countsOutliersOutsideClosedFences() {
        // below: 2,2 => 2; above: 25,30,35 => 3; total 5
        assertEquals(5, counter.countOutliers(fees(), TUKEY_FENCES_LOW_5_HIGH_20));
    }

    @Test
    void zeroOutliersWhenAllInside() {
        var list = List.of(new BigDecimal("5"), new BigDecimal("7"), new BigDecimal("10"), new BigDecimal("15"), new BigDecimal("20"));
        assertEquals(0, counter.countOutliers(list, TUKEY_FENCES_LOW_5_HIGH_20));
    }

    @Test
    void allExceptEqualsWhenNarrowFence() {
        var list = fees();
        var fences = Range.closed(new BigDecimal("10"), new BigDecimal("10"));
        // Only the two 10's are inside; others are outliers
        assertEquals(list.size() - 2, counter.countOutliers(list, fences));
    }

    @Test
    void emptyListReturnsZero() {
        assertEquals(0, counter.countOutliers(List.of(), TUKEY_FENCES_LOW_5_HIGH_20));
    }

    @Test
    void fencesOutsideRangeYieldZeroOutliers() {
        var fences = Range.closed(new BigDecimal("0"), new BigDecimal("100"));
        assertEquals(0, counter.countOutliers(fees(), fences));
    }

    @Test
    void duplicatesAtBoundsAreInside() {
        var fencesAtBounds = Range.closed(new BigDecimal("2"), new BigDecimal("20"));
        // below: 1 -> 1; above: 25,30 -> 2; total 3
        assertEquals(3, counter.countOutliers(fees(), fencesAtBounds));
    }
}
