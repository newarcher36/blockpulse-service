package com.blockchain.blockpulseservice.service.sliding_window;

import com.blockchain.blockpulseservice.model.domain.FeeWindowStatsSummary;
import com.google.common.collect.Range;
import com.google.common.collect.TreeMultiset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeeWindowStatsSummaryCalculatorTest {
    @Mock
    private FeeQuantiles feeQuantiles;
    @Mock
    private FeeAverages feeAverages;
    @Mock
    private TukeyFenceCalculator tukey;
    @Mock
    private OutlierCounter outlierCounter;

    @InjectMocks
    private FeeWindowStatsSummaryCalculator calculator;

    @Test
    void calculateComprehensiveStatsDelegatesAndComposesSnapshot() {
        var feeList = List.of(new BigDecimal("1"), new BigDecimal("2"), new BigDecimal("3"));
        var multiset = TreeMultiset.create(feeList);
        var sum = new BigDecimal("6");
        var expectedIqr = Range.closed(new BigDecimal("1.5"), new BigDecimal("2.5"));
        var expectedFences = Range.closed(new BigDecimal("0.0"), new BigDecimal("5.0"));
        when(tukey.tukeyFences(anyList())).thenReturn(expectedFences);
        when(outlierCounter.countOutliers(any(TreeMultiset.class), eq(expectedFences))).thenReturn(2);
        when(feeAverages.average(eq(sum), eq(feeList.size()))).thenReturn(new BigDecimal("2.00"));
        when(feeQuantiles.median(anyList())).thenReturn(new BigDecimal("2"));
        when(tukey.iqrRange(anyList())).thenReturn(expectedIqr);

        var snapshot = calculator.calculateComprehensiveStats(multiset);

        assertThat(snapshot.transactionCount()).isEqualTo(3);
        assertThat(snapshot.outliersCount()).isEqualTo(2);
        assertThat(snapshot.avgFeePerVByte()).isEqualTo(new BigDecimal("2.00"));
        assertThat(snapshot.median()).isEqualTo(new BigDecimal("2"));
        assertThat(snapshot.iqrRange()).isEqualTo(expectedIqr);
        assertThat(snapshot.tukeyFences()).isEqualTo(expectedFences);
        verify(tukey).tukeyFences(feeList);
        verify(outlierCounter).countOutliers(eq(multiset), eq(expectedFences));
        verify(feeAverages).average(eq(sum), eq(feeList.size()));
        verify(feeQuantiles).median(feeList);
        verify(tukey).iqrRange(feeList);
        verifyNoMoreInteractions(feeQuantiles, feeAverages, tukey, outlierCounter);
    }

    @Test
    void returnsEmptySnapshotWhenNoFees() {
        var snapshot = calculator.calculateComprehensiveStats(TreeMultiset.create());
        assertThat(snapshot).usingRecursiveComparison().isEqualTo(FeeWindowStatsSummary.empty());
        verifyNoInteractions(feeQuantiles, feeAverages, tukey, outlierCounter);
    }
}
