package com.blockchain.blockpulseservice.service.sliding_window;

import com.google.common.collect.Range;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Component
public class OutlierCounter {
    public int countOutliers(List<BigDecimal> sortedFees, Range<BigDecimal> fences) {
        int lowerIndex = findPosition(sortedFees, fences.lowerEndpoint());
        int upperIndex = findPosition(sortedFees, fences.upperEndpoint());
        return sortedFees.size() - upperIndex + lowerIndex;
    }

    private int findPosition(List<BigDecimal> sortedFees, BigDecimal endpoint) {
        int position = Collections.binarySearch(sortedFees, endpoint);
        return position < 0 ? Math.abs(position) - 1 : position;
    }
}

