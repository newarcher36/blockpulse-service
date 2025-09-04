package com.blockchain.blockpulseservice.model.event;

import com.blockchain.blockpulseservice.model.domain.MempoolStats;

public record MempoolStatsUpdatedEvent(MempoolStats stats) {}