package com.blockchain.blockpulseservice.event;

import com.blockchain.blockpulseservice.model.domain.Transaction;

public record NewTransactionEvent(Transaction transaction) {}

