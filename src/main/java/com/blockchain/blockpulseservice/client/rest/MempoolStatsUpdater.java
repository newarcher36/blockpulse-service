package com.blockchain.blockpulseservice.client.rest;

import com.blockchain.blockpulseservice.model.domain.MempoolStats;
import com.blockchain.blockpulseservice.model.dto.MempoolInfoDTO;
import com.blockchain.blockpulseservice.model.dto.RecommendedTransactionFeeDTO;
import com.blockchain.blockpulseservice.model.event.MempoolStatsUpdatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Service
public class MempoolStatsUpdater {
    private static final int REQUEST_TIMEOUT = 2;
    private final String feeApiUrl;
    private final String mempoolInfoUrl;
    private final RestTemplate restTemplate;
    private final Executor mempoolRestExecutor;
    private final ApplicationEventPublisher eventPublisher;

    public MempoolStatsUpdater(@Value("${app.mempool.space.rest.fee-api-url}") String feeApiUrl,
                               @Value("${app.mempool.space.rest.mempool-info-api-url}") String mempoolInfoUrl,
                               RestTemplate restTemplate,
                               Executor mempoolRestExecutor,
                               ApplicationEventPublisher eventPublisher) {
        this.feeApiUrl = feeApiUrl;
        this.mempoolInfoUrl = mempoolInfoUrl;
        this.restTemplate = restTemplate;
        this.mempoolRestExecutor = mempoolRestExecutor;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedRate = 10000) // Every 10 seconds
    public void updateMempoolData() {
        var feeFuture = initFuture(fetchFeeData(), "Failed to fetch fee data");
        var mempoolFuture = initFuture(fetchMempoolInfo(), "Failed to update mempool stats");

        feeFuture.thenCombine(mempoolFuture, (feeDto, mempoolInfoDTO) -> {
            if (feeDto != null && mempoolInfoDTO != null) {
                return mapToMempoolInfo(feeDto, mempoolInfoDTO);
            }
            return null;
        }).thenAccept(mempoolStats -> {
            if (mempoolStats != null) {
                log.debug("Updated mempool data: {}", mempoolStats);
                eventPublisher.publishEvent(new MempoolStatsUpdatedEvent(mempoolStats));
            } else {
                log.warn("Skipping update due to missing data.");
            }
        }).exceptionally(ex -> {
            log.error("Unexpected error while processing mempool data", ex);
            return null;
        });
    }

    private <T> CompletableFuture<T> initFuture(Supplier<T> supplier, String errorMessage) {
        return CompletableFuture
                .supplyAsync(supplier, mempoolRestExecutor)
                .orTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    log.error(errorMessage, ex);
                    return null;
                });
    }

    private Supplier<RecommendedTransactionFeeDTO> fetchFeeData() {
        return () -> restTemplate.getForObject(feeApiUrl, RecommendedTransactionFeeDTO.class);
    }

    private Supplier<MempoolInfoDTO> fetchMempoolInfo() {
        return () -> restTemplate.getForObject(mempoolInfoUrl, MempoolInfoDTO.class);
    }

    private MempoolStats mapToMempoolInfo(RecommendedTransactionFeeDTO feeDto, MempoolInfoDTO mempoolInfoDTO) {
        return MempoolStats.builder()
                .fastFeePerVByte(feeDto.fastestFee())
                .mediumFeePerVByte(feeDto.halfHourFee())
                .slowFeePerVByte(feeDto.hourFee())
                .mempoolSize(mempoolInfoDTO.memPoolSize())
                .build();
    }
}