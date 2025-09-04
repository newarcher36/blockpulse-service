package com.blockchain.blockpulseservice.service.sliding_window;

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import com.google.common.collect.TreeMultiset;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class OutlierCounter {
    public int countOutliers(TreeMultiset<BigDecimal> sortedFees, Range<BigDecimal> fences) {
        int outliersBelowFence = sortedFees.headMultiset(fences.lowerEndpoint(), BoundType.OPEN).size();
        int outliersAboveFence = sortedFees.tailMultiset(fences.upperEndpoint(), BoundType.OPEN).size();
        return outliersBelowFence + outliersAboveFence;
    }
}