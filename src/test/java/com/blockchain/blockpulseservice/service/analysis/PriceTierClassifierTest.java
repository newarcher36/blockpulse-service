package com.blockchain.blockpulseservice.service.analysis;

import com.blockchain.blockpulseservice.model.domain.MempoolStats;
import com.blockchain.blockpulseservice.model.domain.PriceTier;
import com.blockchain.blockpulseservice.service.analysis.price_tier.PriceTierClassifier;
import com.google.common.collect.Range;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PriceTierClassifierTest {
    private final PriceTierClassifier classifier = new PriceTierClassifier();

    @Test
    void classifyUsingMempoolStats() {
        var stats = MempoolStats.builder()
                .fastFeePerVByte(50)
                .mediumFeePerVByte(25)
                .slowFeePerVByte(10)
                .mempoolSize(0)
                .build();

        assertEquals(PriceTier.EXPENSIVE, classifier.classifyUsingMempool(new BigDecimal("51"), stats));

        assertEquals(PriceTier.NORMAL, classifier.classifyUsingMempool(new BigDecimal("26"), stats));
        assertEquals(PriceTier.NORMAL, classifier.classifyUsingMempool(new BigDecimal("25"), stats));

        assertEquals(PriceTier.CHEAP, classifier.classifyUsingMempool(new BigDecimal("24"), stats));
    }

    @Test
    void classifyUsingIqrFromFeesWindow() {
        var iqr = Range.closed(new BigDecimal("10"), new BigDecimal("20"));

        assertEquals(PriceTier.CHEAP, classifier.classifyUsingIqr(new BigDecimal("9.99"), iqr));

        assertEquals(PriceTier.NORMAL, classifier.classifyUsingIqr(new BigDecimal("10.00"), iqr));
        assertEquals(PriceTier.NORMAL, classifier.classifyUsingIqr(new BigDecimal("15"), iqr));
        assertEquals(PriceTier.NORMAL, classifier.classifyUsingIqr(new BigDecimal("20.00"), iqr));

        assertEquals(PriceTier.EXPENSIVE, classifier.classifyUsingIqr(new BigDecimal("21"), iqr));
    }
}
