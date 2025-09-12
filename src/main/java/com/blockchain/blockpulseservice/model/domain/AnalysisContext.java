package com.blockchain.blockpulseservice.model.domain;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.Set;

@Value
@Builder(toBuilder = true)
public class AnalysisContext {
    // input
    Transaction newTransaction;
    FeeWindowStatsSummary feeWindowStatsSummary;
    // output
    @Singular("pattern")
    Set<PatternType> patterns;
    PriceTier priceTier;
    @Builder.Default
    boolean isOutlier = false;
    MempoolStats mempoolStats;
}