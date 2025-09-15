package com.blockchain.blockpulseservice.service.mapper;

import com.blockchain.blockpulseservice.model.domain.AnalysisContext;
import com.blockchain.blockpulseservice.model.domain.FeeWindowStatsSummary;
import com.blockchain.blockpulseservice.model.domain.MempoolStats;
import com.blockchain.blockpulseservice.model.domain.PatternMetric;
import com.blockchain.blockpulseservice.model.domain.PatternSignal;
import com.blockchain.blockpulseservice.model.domain.PatternType;
import com.blockchain.blockpulseservice.model.domain.PriceTier;
import com.blockchain.blockpulseservice.model.domain.Transaction;
import com.google.common.collect.Range;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyzedTransactionMapperTest {

    @ParameterizedTest
    @MethodSource("patternSignalProvider")
    void mapsAnalysisContextToEvent(PatternSignal patternSignal) {
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
                .patternSignal(patternSignal)
                .isOutlier(true)
                .build();

        var event = new AnalyzedTransactionMapper(clock).map(context);

        assertEquals("tx-abc", event.id());
        assertEquals(fixedNow, event.producedAt());
        assertEquals(new BigDecimal("12.34"), event.feePerVByte());
        assertEquals(new BigDecimal("1000"), event.totalFee());
        assertEquals(225, event.txSize());
        assertEquals(txTime, event.timestamp());
        assertEquals(patternSignal, event.patternSignal());
        assertEquals(PriceTier.NORMAL, event.priceTier());
        assertTrue(event.isOutlier());

        var dto = event.windowSnapshot();
        assertEquals(10, dto.transactionsCount());
        assertEquals(2, dto.outliersCount());
        assertEquals(new BigDecimal("15.50"), dto.avgFeePerVByte());
        assertEquals(new BigDecimal("14.00"), dto.medianFeePerVByte());
    }

    private static Stream<Arguments> patternSignalProvider() {
        return Stream.of(Arguments.of(new PatternSignal(PatternType.SURGE, Map.of(PatternMetric.UPPER_TUKEY_FENCE, 25.0))),
                Arguments.of((PatternSignal) null)
        );
    }
}
