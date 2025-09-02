package com.blockchain.blockpulseservice.client.rest;

import com.blockchain.blockpulseservice.model.domain.MempoolStats;
import com.blockchain.blockpulseservice.model.dto.RecommendedTransactionFeeDTO;
import com.blockchain.blockpulseservice.model.dto.MempoolInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Service
public class MempoolStatsUpdater {
    private static final ExecutorService ioExecutor = Executors.newFixedThreadPool(2);
    private static final int REQUEST_TIMEOUT = 2;
    private final String feeApiUrl;
    private final String mempoolInfoUrl;
    private final RestTemplate restTemplate;
    private volatile MempoolStats mempoolStats;

    public MempoolStatsUpdater(@Value("${app.mempool.space.rest.fee-api-url}") String feeApiUrl,
                               @Value("${app.mempool.space.rest.mempool-info-api-url}") String mempoolInfoUrl,
                               RestTemplate restTemplate) {
        this.feeApiUrl = feeApiUrl;
        this.mempoolInfoUrl = mempoolInfoUrl;
        this.restTemplate = restTemplate;
    }

    @Scheduled(fixedRate = 10000) // Every 10 seconds
    public void updateMempoolData() {
        var feeFuture = CompletableFuture
                .supplyAsync(fetchFeeData(), ioExecutor)
                .orTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    log.error("Failed to fetch fee data", ex);
                    return null;
                });
        var mempoolFuture = CompletableFuture
                .supplyAsync(fetchMempoolInfo(), ioExecutor)
                .orTimeout(REQUEST_TIMEOUT, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    log.error("Failed to update mempool stats", ex);
                    return null;
                });

        feeFuture.thenCombine(mempoolFuture, (feeDto, mempoolInfoDTO) -> {
            if (feeDto != null && mempoolInfoDTO != null) {
                return mapToMempoolInfo(feeDto, mempoolInfoDTO);
            }
            return null;
        }).thenAccept(mempoolStats -> {
            if (mempoolStats != null) {
                this.mempoolStats = mempoolStats;
                log.debug("Updated mempool data: {}", mempoolStats);
            } else {
                log.warn("Skipping update due to missing data.");
            }
        }).exceptionally(ex -> {
            log.error("Unexpected error while processing mempool data", ex);
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
        return new MempoolStats(feeDto.fastestFee(), feeDto.halfHourFee(), feeDto.hourFee(), mempoolInfoDTO.memPoolSize());
    }

    public MempoolStats getMempoolStats() {
        return mempoolStats == null ? MempoolStats.empty() : mempoolStats;
    }
}
