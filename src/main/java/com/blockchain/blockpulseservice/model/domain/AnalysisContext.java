package com.blockchain.blockpulseservice.model.domain;

import lombok.Builder;
import lombok.Value;

 

@Value
@Builder(toBuilder = true)
public class AnalysisContext {
    // input
    Transaction newTransaction;
    FeeWindowStatsSummary feeWindowStatsSummary;
    // output
    PatternSignal patternSignal;
    PriceTier priceTier;
    @Builder.Default
    boolean isOutlier = false;
    MempoolStats mempoolStats;
}
