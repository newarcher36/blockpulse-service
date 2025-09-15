package com.blockchain.blockpulseservice.model.domain;

import java.util.Map;

public record PatternSignal(PatternType type, Map<PatternMetric, Double> metrics) {}
