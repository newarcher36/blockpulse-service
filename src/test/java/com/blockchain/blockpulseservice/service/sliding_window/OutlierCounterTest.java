package com.blockchain.blockpulseservice.service.sliding_window;

import com.google.common.collect.Range;
import com.google.common.collect.TreeMultiset;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OutlierCounterTest {
    private final OutlierCounter counter = new OutlierCounter();
    private static final Range<BigDecimal> TUKEY_FENCES_LOW_5_HIGH_20 = Range.closed(new BigDecimal("5"), new BigDecimal("20"));

    @Test
    void countsOutliersOutsideClosedFences() {
        // below: 2,2 => 2; above: 25,30,35 => 3; total 5
        assertEquals(5, counter.countOutliers(fees(), TUKEY_FENCES_LOW_5_HIGH_20));
    }

    @Test
    void countsOutliersWhenFencesNotInList() {
        var fencesNotInList = Range.closed(new BigDecimal("6"), new BigDecimal("19"));
        // below: 2,2,5 => 3; above: 20,25,30,35 => 4; total 7
        assertEquals(7, counter.countOutliers(fees(), fencesNotInList));
    }

    @Test
    void zeroOutliersWhenAllInside() {
        var fences = Range.closed(new BigDecimal("0"), new BigDecimal("35"));
        assertEquals(0, counter.countOutliers(fees(), fences));
    }

    @Test
    void allExceptEqualsWhenNarrowFence() {
        var list = fees();
        var fences = Range.closed(new BigDecimal("10"), new BigDecimal("10"));
        // Only the two 10's are inside; others are outliers
        assertEquals(8, counter.countOutliers(list, fences));
    }

    @Test
    void emptyListReturnsZero() {
        assertEquals(0, counter.countOutliers(TreeMultiset.create(), TUKEY_FENCES_LOW_5_HIGH_20));
    }

    @Test
    void fencesOutsideRangeYieldZeroOutliers() {
        var fences = Range.closed(new BigDecimal("0"), new BigDecimal("100"));
        assertEquals(0, counter.countOutliers(fees(), fences));
    }

    @Test
    void duplicatesAtBoundsAreInside() {
        var fencesAtBounds = Range.closed(new BigDecimal("2"), new BigDecimal("20"));
        // above: 25,30,35 -> total 3
        assertEquals(3, counter.countOutliers(fees(), fencesAtBounds));
    }

    private static TreeMultiset<BigDecimal> fees() {
        var list = List.of(
                new BigDecimal("2"),
                new BigDecimal("2"),
                new BigDecimal("5"),
                new BigDecimal("10"),
                new BigDecimal("10"),
                new BigDecimal("15"),
                new BigDecimal("20"),
                new BigDecimal("25"),
                new BigDecimal("30"),
                new BigDecimal("35")
        );
        return TreeMultiset.create(list);
    }
}