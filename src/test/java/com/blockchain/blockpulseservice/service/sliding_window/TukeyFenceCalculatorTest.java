package com.blockchain.blockpulseservice.service.sliding_window;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TukeyFenceCalculatorTest {
    private static final double TUKEY_K = 1.5;
    @Mock
    private FeeQuantiles feeQuantiles;
    private TukeyFenceCalculator tukeyFenceCalculator;

    @BeforeEach
    void setUp() {
        tukeyFenceCalculator = new TukeyFenceCalculator(feeQuantiles, TUKEY_K);
    }

    @Test
    void clampsLowerFenceToZeroWhenNegative() {
        when(feeQuantiles.q1(anyList())).thenReturn(BigDecimal.valueOf(5));
        when(feeQuantiles.q3(anyList())).thenReturn(BigDecimal.valueOf(30));

        var fees = List.of(BigDecimal.valueOf(1), BigDecimal.valueOf(2));
        var fences = tukeyFenceCalculator.tukeyFences(fees);

        assertThat(fences.lowerEndpoint()).isEqualTo(BigDecimal.ZERO);
        assertThat(fences.upperEndpoint()).isEqualTo(BigDecimal.valueOf(67.5));
        verify(feeQuantiles).q1(fees);
        verify(feeQuantiles).q3(fees);
    }

    @Test
    void doesNotClampWhenLowerIsNonNegative() {
        when(feeQuantiles.q1(anyList())).thenReturn(BigDecimal.valueOf(20));
        when(feeQuantiles.q3(anyList())).thenReturn(BigDecimal.valueOf(30));

        var fees = List.of(BigDecimal.valueOf(1), BigDecimal.valueOf(2));
        var fences = tukeyFenceCalculator.tukeyFences(fees);

        assertThat(fences.lowerEndpoint()).isEqualTo(BigDecimal.valueOf(5.0)); // 10 - (0.5*10)
        assertThat(fences.upperEndpoint()).isEqualTo(BigDecimal.valueOf(45.0)); // 20 + 5
        verify(feeQuantiles).q1(fees);
        verify(feeQuantiles).q3(fees);
    }

    @Test
    void iqrRangeReturnsClosedRangeBetweenQ1AndQ3() {
        when(feeQuantiles.q1(anyList())).thenReturn(BigDecimal.valueOf(10));
        when(feeQuantiles.q3(anyList())).thenReturn(BigDecimal.valueOf(20));

        var fees = List.of(BigDecimal.valueOf(3), BigDecimal.valueOf(7));
        var iqr = tukeyFenceCalculator.iqrRange(fees);

        assertThat(iqr.lowerEndpoint()).isEqualTo(BigDecimal.valueOf(10));
        assertThat(iqr.upperEndpoint()).isEqualTo(BigDecimal.valueOf(20));
        verify(feeQuantiles).q1(fees);
        verify(feeQuantiles).q3(fees);
    }
}
