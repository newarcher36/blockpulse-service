package com.blockchain.blockpulseservice.service.sliding_window;

import com.blockchain.blockpulseservice.model.event.NewTransactionEvent;
import com.blockchain.blockpulseservice.model.domain.FeeWindowStatsSummary;
import com.blockchain.blockpulseservice.model.domain.Transaction;
import com.blockchain.blockpulseservice.service.TransactionAnalyzerService;
import com.google.common.collect.TreeMultiset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlidingWindowManagerTest {

    private static final int SLIDING_WINDOW_SIZE = 2;
    @Mock
    private TransactionAnalyzerService analyzerService;
    @Mock
    private FeeWindowStatsSummaryCalculator summaryCalculator;
    @Captor
    private ArgumentCaptor<TreeMultiset<BigDecimal>> multisetCaptor;
    @Captor
    private ArgumentCaptor<Transaction> txCaptor;
    @Captor
    private ArgumentCaptor<FeeWindowStatsSummary> snapshotCaptor;

    private SlidingWindowManager manager;

    private static NewTransactionEvent evt(Transaction tx) { return new NewTransactionEvent(tx); }

    @BeforeEach
    void setUp() {
        manager = new SlidingWindowManager(SLIDING_WINDOW_SIZE, analyzerService, summaryCalculator);
    }

    @Test
    void ignoresInvalidTransactions() {
        manager.onNewTransaction(evt(new Transaction("neg-fee", new BigDecimal("-1"), BigDecimal.ZERO, 100, Instant.EPOCH)));
        manager.onNewTransaction(evt(new Transaction("zero-vsize", new BigDecimal("5"), BigDecimal.ZERO, 0, Instant.EPOCH)));

        verifyNoInteractions(summaryCalculator, analyzerService);
    }

    @Test
    void addsFeesAndCallsAnalyzerWithinCapacity() {
        var t1 = tx("t1", "10");
        var t2 = tx("t2", "5");
        var snapshot1 = mock(FeeWindowStatsSummary.class);
        var snapshot2 = mock(FeeWindowStatsSummary.class);
        when(summaryCalculator.calculateComprehensiveStats(any(TreeMultiset.class))).thenReturn(snapshot1, snapshot2);

        manager.onNewTransaction(evt(t1));
        manager.onNewTransaction(evt(t2));

        verify(summaryCalculator, times(2)).calculateComprehensiveStats(multisetCaptor.capture());
        assertThat(multisetCaptor.getValue()).containsExactly(new BigDecimal("5"), new BigDecimal("10"));
        verify(analyzerService, times(2)).processTransaction(txCaptor.capture(), snapshotCaptor.capture());
        assertThat(txCaptor.getAllValues()).containsExactly(t1, t2);
        assertThat(snapshotCaptor.getAllValues()).containsExactly(snapshot1, snapshot2);
    }

    @Test
    void evictsOldestByInsertionOrderWhenFull() {
        var t1 = tx("t1", "10");
        var t2 = tx("t2", "20");
        var t3 = tx("t3", "30");
        when(summaryCalculator.calculateComprehensiveStats(any(TreeMultiset.class))).thenReturn(mock(FeeWindowStatsSummary.class));

        manager.onNewTransaction(evt(t1));
        manager.onNewTransaction(evt(t2));
        manager.onNewTransaction(evt(t3));

        verify(summaryCalculator, times(3)).calculateComprehensiveStats(multisetCaptor.capture());
        assertThat(multisetCaptor.getValue()).containsExactly(new BigDecimal("20"), new BigDecimal("30"));
    }

    private static Transaction tx(String id, String fee) {
        return new Transaction(id, new BigDecimal(fee), BigDecimal.ZERO, 100, Instant.EPOCH);
    }
}