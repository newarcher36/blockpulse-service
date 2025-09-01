package com.blockchain.blockpulseservice.service.sliding_window;

import com.blockchain.blockpulseservice.model.domain.TransactionWindowSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionWindowSnapshotService {
    private final FeeWindowStatsSummary feeWindowStatsSummary;
    private BigDecimal sum = BigDecimal.ZERO;

    public void addFee(BigDecimal feePerVSize) {
        this.sum = sum.add(feePerVSize);
    }

    public void subtractFee(BigDecimal feePerVSize) {
        this.sum = sum.subtract(feePerVSize);
    }

    public TransactionWindowSnapshot takeCurrentWindowSnapshot(List<BigDecimal> sortedFees) {
        log.debug("Taking current window snapshot...");
        if (sortedFees.isEmpty()) {
            log.debug("No transactions in window, returning empty snapshot");
            return TransactionWindowSnapshot.empty();
        }
        int txCount = sortedFees.size();
        return new TransactionWindowSnapshot(
                txCount,
                feeWindowStatsSummary.countOutliers(sortedFees),
                feeWindowStatsSummary.calculateAvg(sum, txCount),
                feeWindowStatsSummary.calculateMedian(sortedFees),
                feeWindowStatsSummary.calculateIQR(sortedFees),
                feeWindowStatsSummary.calculateTukeyFences(sortedFees)
        );
    }
}