package com.blockchain.blockpulseservice.model;

public record MempoolStats(double fastFeePerVByte,
                           double mediumFeePerVByte,
                           double slowFeePerVByte,
                           int mempoolSize) {}