package com.blockchain.blockpulseservice.model.domain;

import lombok.Builder;

@Builder
public record MempoolStats(double fastFeePerVByte,
                           double mediumFeePerVByte,
                           double slowFeePerVByte,
                           int mempoolSize) {
    public static MempoolStats empty() {
        return new MempoolStats(0, 0, 0, 0);
    }
}
