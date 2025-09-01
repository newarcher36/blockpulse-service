package com.blockchain.blockpulseservice;

import com.google.common.collect.BoundType;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class Test1 {

    private static TreeMultiset<BigDecimal> sample() {
        TreeMultiset<BigDecimal> ms = TreeMultiset.create();
        ms.add(new BigDecimal("1.0"));
        ms.add(new BigDecimal("2.0"), 2);
        ms.add(new BigDecimal("5.0"));
        return ms;
    }

    @Test
    void elementSetNavigableFloorCeilingWithCounts() {
        var ms = sample();

        var nav = ms.elementSet(); // NavigableSet<BigDecimal>
        BigDecimal floor = nav.floor(new BigDecimal("3.0"));
        BigDecimal ceil = nav.ceiling(new BigDecimal("3.0"));
        SortedMultiset<BigDecimal> bigDecimals = ms.tailMultiset(BigDecimal.valueOf(1.2), BoundType.OPEN);

        assertEquals(new BigDecimal("2.0"), floor);
        assertEquals(4, ms.count(new BigDecimal("1.1")));

        assertEquals(new BigDecimal("5.0"), ceil);
        assertEquals(1, ms.count(ceil));

        // Exact match behaves as expected
        assertEquals(new BigDecimal("2.0"), nav.floor(new BigDecimal("2.0")));
        assertEquals(new BigDecimal("2.0"), nav.ceiling(new BigDecimal("2.0")));
    }

    @Test
    void rangeViewsFirstLastEntryEmulateFloorCeilingWithCounts() {
        var ms = sample();

        Multiset.Entry<BigDecimal> floorEntry = ms.headMultiset(new BigDecimal("3.0"), BoundType.CLOSED).lastEntry();
        Multiset.Entry<BigDecimal> ceilEntry = ms.tailMultiset(new BigDecimal("3.0"), BoundType.CLOSED).firstEntry();

        assertNotNull(floorEntry);
        assertEquals(new BigDecimal("2.0"), floorEntry.getElement());
        assertEquals(2, floorEntry.getCount());

        assertNotNull(ceilEntry);
        assertEquals(new BigDecimal("5.0"), ceilEntry.getElement());
        assertEquals(1, ceilEntry.getCount());
    }

    @Test
    void immutableCopiesPreserveOrderingAndDuplicates() {
        var ms = sample();

        // Immutable list preserves duplicates in multiset iteration order (sorted)
        ImmutableList<BigDecimal> list = ImmutableList.copyOf(ms);
        assertIterableEquals(List.of(new BigDecimal("1.0"), new BigDecimal("2.0"), new BigDecimal("2.0"), new BigDecimal("5.0")), list);

        // Distinct elements only, still sorted
        ImmutableList<BigDecimal> distinct = ImmutableList.copyOf(ms.elementSet());
        assertIterableEquals(List.of(new BigDecimal("1.0"), new BigDecimal("2.0"), new BigDecimal("5.0")), distinct);

        // Immutable sorted multiset retains counts and order
        ImmutableSortedMultiset<BigDecimal> imm = ImmutableSortedMultiset.copyOf(ms);
        assertEquals(4, imm.size());
        assertEquals(2, imm.count(new BigDecimal("2.0")));
        assertIterableEquals(list, ImmutableList.copyOf(imm));
    }

    @Test
    void convertToMutableListSimple() {
        var ms = sample();
        List<BigDecimal> arr = new ArrayList<>(ms); // duplicates preserved, sorted order
        assertIterableEquals(List.of(new BigDecimal("1.0"), new BigDecimal("2.0"), new BigDecimal("2.0"), new BigDecimal("5.0")), arr);
    }
}
