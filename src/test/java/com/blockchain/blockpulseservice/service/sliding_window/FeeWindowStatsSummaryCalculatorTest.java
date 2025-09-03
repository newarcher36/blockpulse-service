package com.blockchain.blockpulseservice.service.sliding_window;

import com.blockchain.blockpulseservice.model.domain.FeeWindowStatsSummary;
import com.google.common.collect.Range;
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
        var fees = List.of(new BigDecimal("1"), new BigDecimal("2"), new BigDecimal("3"));
        var sum = new BigDecimal("6.00");
        var expectedIqr = Range.closed(new BigDecimal("1.5"), new BigDecimal("2.5"));
        var expectedFences = Range.closed(new BigDecimal("0.0"), new BigDecimal("5.0"));
        when(tukey.tukeyFences(fees)).thenReturn(expectedFences);
        when(outlierCounter.countOutliers(fees, expectedFences)).thenReturn(2);
        when(feeAverages.average(sum, fees.size())).thenReturn(new BigDecimal("2.00"));
        when(feeQuantiles.median(fees)).thenReturn(new BigDecimal("2"));
        when(tukey.iqrRange(fees)).thenReturn(expectedIqr);

        var snapshot = calculator.calculateComprehensiveStats(fees, sum);

        assertThat(snapshot.transactionCount()).isEqualTo(3);
        assertThat(snapshot.outliersCount()).isEqualTo(2);
        assertThat(snapshot.avgFeePerVByte()).isEqualTo(new BigDecimal("2.00"));
        assertThat(snapshot.median()).isEqualTo(new BigDecimal("2"));
        assertThat(snapshot.iqrRange()).isEqualTo(expectedIqr);
        assertThat(snapshot.tukeyFences()).isEqualTo(expectedFences);
        verify(tukey).tukeyFences(fees);
        verify(outlierCounter).countOutliers(fees, expectedFences);
        verify(feeAverages).average(sum, fees.size());
        verify(feeQuantiles).median(fees);
        verify(tukey).iqrRange(fees);
        verifyNoMoreInteractions(feeQuantiles, feeAverages, tukey, outlierCounter);
    }

    @Test
    void returnsEmptySnapshotWhenNoFees() {
        var snapshot = calculator.calculateComprehensiveStats(List.of(), BigDecimal.ZERO);
        assertThat(snapshot).usingRecursiveComparison().isEqualTo(FeeWindowStatsSummary.empty());
        verifyNoInteractions(feeQuantiles, feeAverages, tukey, outlierCounter);
    }
}

