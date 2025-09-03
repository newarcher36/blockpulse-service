package com.blockchain.blockpulseservice.service.analysis;

import com.blockchain.blockpulseservice.model.domain.MempoolStats;
import com.blockchain.blockpulseservice.model.domain.PriceTier;
import com.google.common.collect.Range;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PriceTierClassifierTest {
    private final PriceTierClassifier classifier = new PriceTierClassifier();

    @Test
    void classifyUsingMempool_cases() {
        var stats = new MempoolStats(50, 25, 10, 0);

        assertEquals(PriceTier.CHEAP, classifier.classifyUsingMempool(new BigDecimal("60"), stats));

        assertEquals(PriceTier.NORMAL, classifier.classifyUsingMempool(new BigDecimal("25"), stats));

        assertEquals(PriceTier.EXPENSIVE, classifier.classifyUsingMempool(new BigDecimal("40"), stats));
    }

    @Test
    void classifyUsingIqr_cases() {
        var iqr = Range.closed(new BigDecimal("10"), new BigDecimal("20"));

        assertEquals(PriceTier.CHEAP, classifier.classifyUsingIqr(new BigDecimal("9.99"), iqr));

        assertEquals(PriceTier.NORMAL, classifier.classifyUsingIqr(new BigDecimal("10.00"), iqr));
        assertEquals(PriceTier.NORMAL, classifier.classifyUsingIqr(new BigDecimal("15"), iqr));
        assertEquals(PriceTier.NORMAL, classifier.classifyUsingIqr(new BigDecimal("20.00"), iqr));

        assertEquals(PriceTier.EXPENSIVE, classifier.classifyUsingIqr(new BigDecimal("21"), iqr));
    }
}