package com.blockchain.blockpulseservice.service.mapper;

import com.blockchain.blockpulseservice.model.domain.AnalysisContext;
import com.blockchain.blockpulseservice.model.domain.FeeWindowStatsSummary;
import com.blockchain.blockpulseservice.model.domain.MempoolStats;
import com.blockchain.blockpulseservice.model.domain.PatternType;
import com.blockchain.blockpulseservice.model.domain.PriceTier;
import com.blockchain.blockpulseservice.model.domain.Transaction;
import com.google.common.collect.Range;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyzedTransactionMapperTest {
    @Test
    void mapsAnalysisContextToEvent_usingInjectedClock() {
        var fixedNow = Instant.parse("2024-01-02T03:04:05Z");
        var clock = Clock.fixed(fixedNow, ZoneOffset.UTC);
        var txTime = Instant.parse("2024-01-01T00:00:00Z");
        var tx = new Transaction(
                "tx-abc",
                new BigDecimal("12.34"),
                new BigDecimal("1000"),
                225,
                txTime
        );
        var summary = FeeWindowStatsSummary.builder()
                .transactionCount(10)
                .outliersCount(2)
                .avgFeePerVByte(new BigDecimal("15.50"))
                .median(new BigDecimal("14.00"))
                .iqrRange(Range.closed(new BigDecimal("10"), new BigDecimal("20")))
                .tukeyFences(Range.closed(new BigDecimal("5"), new BigDecimal("30")))
                .build();
        var context = AnalysisContext.builder()
                .newTransaction(tx)
                .feeWindowStatsSummary(summary)
                .mempoolStats(MempoolStats.builder()
                        .fastFeePerVByte(25)
                        .mediumFeePerVByte(15)
                        .slowFeePerVByte(5)
                        .mempoolSize(1000)
                        .build())
                .priceTier(PriceTier.NORMAL)
                .isOutlier(true)
                .patterns(Set.of(PatternType.SURGE, PatternType.SCAM))
                .build();

        var event = new AnalyzedTransactionMapper(clock).map(context);

        assertEquals("tx-abc", event.id());
        assertEquals(fixedNow, event.producedAt());
        assertEquals(new BigDecimal("12.34"), event.feePerVByte());
        assertEquals(new BigDecimal("1000"), event.totalFee());
        assertEquals(225, event.txSize());
        assertEquals(txTime, event.timestamp());
        assertEquals(Set.of(PatternType.SURGE, PatternType.SCAM), event.patternTypes());
        assertEquals(PriceTier.NORMAL, event.priceTier());
        assertTrue(event.isOutlier());

        var dto = event.windowSnapshot();
        assertEquals(10, dto.transactionsCount());
        assertEquals(2, dto.outliersCount());
        assertEquals(new BigDecimal("15.50"), dto.avgFeePerVByte());
        assertEquals(new BigDecimal("14.00"), dto.medianFeePerVByte());
    }
}
