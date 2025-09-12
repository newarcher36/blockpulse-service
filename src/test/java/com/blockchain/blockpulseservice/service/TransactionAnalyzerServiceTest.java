package com.blockchain.blockpulseservice.service;

import com.blockchain.blockpulseservice.model.domain.AnalysisContext;
import com.blockchain.blockpulseservice.model.domain.FeeWindowStatsSummary;
import com.blockchain.blockpulseservice.model.domain.MempoolStats;
import com.blockchain.blockpulseservice.model.domain.Transaction;
import com.blockchain.blockpulseservice.model.event.AnalyzedTransactionEvent;
import com.blockchain.blockpulseservice.model.event.MempoolStatsUpdatedEvent;
import com.blockchain.blockpulseservice.service.analysis.FeeAnalyzer;
import com.blockchain.blockpulseservice.service.mapper.AnalyzedTransactionMapper;
import com.blockchain.blockpulseservice.service.stream.AnalysisStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionAnalyzerServiceTest {
    private static final Transaction SAMPLE_TX = new Transaction("tx-1", new BigDecimal("12.3"), new BigDecimal("1234"), 200, Instant.EPOCH);
    @Mock
    private FeeAnalyzer analysisChain;
    @Mock
    private AnalysisStream analysisStream;
    @Mock
    private AnalyzedTransactionMapper analyzedTransactionMapper;
    @InjectMocks
    private TransactionAnalyzerService service;

    @Test
    void usesEmptyMempoolStatsBeforeAnyEvent() {
        var expectedContext = AnalysisContext.builder()
                .newTransaction(SAMPLE_TX)
                .feeWindowStatsSummary(FeeWindowStatsSummary.empty())
                .mempoolStats(MempoolStats.empty())
                .build();
        var analyzedTransactionEvent = mock(AnalyzedTransactionEvent.class);
        when(analysisChain.analyze(any())).thenReturn(expectedContext);
        when(analyzedTransactionMapper.map(any())).thenReturn(analyzedTransactionEvent);

        service.processTransaction(SAMPLE_TX, FeeWindowStatsSummary.empty());

        verify(analysisChain).analyze(expectedContext);
        verify(analyzedTransactionMapper).map(expectedContext);
        verify(analysisStream).publish(analyzedTransactionEvent);
    }

    @Test
    void updatesContextWithLatestMempoolStatsFromEvent() {
        var expectedStats = MempoolStats.builder()
                .fastFeePerVByte(10)
                .mediumFeePerVByte(5)
                .slowFeePerVByte(1)
                .mempoolSize(321)
                .build();
        var expectedContext = AnalysisContext.builder()
                .newTransaction(SAMPLE_TX)
                .feeWindowStatsSummary(FeeWindowStatsSummary.empty())
                .mempoolStats(expectedStats)
                .build();
        service.onMempoolStatsUpdated(new MempoolStatsUpdatedEvent(expectedStats));

        var analyzedTransactionEvent = mock(AnalyzedTransactionEvent.class);
        when(analysisChain.analyze(any())).thenReturn(expectedContext);
        when(analyzedTransactionMapper.map(expectedContext)).thenReturn(analyzedTransactionEvent);

        service.processTransaction(SAMPLE_TX, FeeWindowStatsSummary.empty());

        verify(analysisChain).analyze(expectedContext);
        verify(analyzedTransactionMapper).map(expectedContext);
        verify(analysisStream).publish(analyzedTransactionEvent);

    }
}