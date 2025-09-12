package com.blockchain.blockpulseservice.client.rest;

import com.blockchain.blockpulseservice.model.domain.MempoolStats;
import com.blockchain.blockpulseservice.model.dto.MempoolInfoDTO;
import com.blockchain.blockpulseservice.model.dto.RecommendedTransactionFeeDTO;
import com.blockchain.blockpulseservice.model.event.MempoolStatsUpdatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.client.RestTemplate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class MempoolStatsUpdaterTest {
    private RestTemplate restTemplate;
    private MempoolStatsUpdater updater;
    private ApplicationEventPublisher publisher;
    private static final String FEE_URL = "http://fee";
    private static final String MEMPOOL_URL = "http://mempool";

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        publisher = mock(ApplicationEventPublisher.class);
        updater = new MempoolStatsUpdater(FEE_URL, MEMPOOL_URL, restTemplate, Runnable::run, publisher);
    }

    @Test
    void updatesStatsWhenBothCallsSucceed() {
        when(restTemplate.getForObject(eq(FEE_URL), eq(RecommendedTransactionFeeDTO.class)))
                .thenReturn(new RecommendedTransactionFeeDTO(50, 25, 10, 5));
        when(restTemplate.getForObject(eq(MEMPOOL_URL), eq(MempoolInfoDTO.class)))
                .thenReturn(new MempoolInfoDTO(1234));

        updater.updateMempoolData();

        var expectedMempoolStats = new MempoolStatsUpdatedEvent(MempoolStats.builder()
                .fastFeePerVByte(50)
                .mediumFeePerVByte(25)
                .slowFeePerVByte(10)
                .mempoolSize(1234)
                .build());
        verify(publisher).publishEvent(expectedMempoolStats);
    }

    @Test
    void doesNotUpdateWhenOneCallFails() {
        when(restTemplate.getForObject(eq(FEE_URL), eq(RecommendedTransactionFeeDTO.class)))
                .thenThrow(new RuntimeException("boom"));
        when(restTemplate.getForObject(eq(MEMPOOL_URL), eq(MempoolInfoDTO.class)))
                .thenReturn(new MempoolInfoDTO(1234));

        updater.updateMempoolData();
        verifyNoInteractions(publisher);
    }
}