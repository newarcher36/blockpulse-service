package com.blockchain.blockpulseservice.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MempoolInfoDTO(@JsonProperty("count") int memPoolSize) { }