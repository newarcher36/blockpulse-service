package com.blockchain.blockpulseservice.model.event;

import com.blockchain.blockpulseservice.model.domain.Transaction;

public record NewTransactionEvent(Transaction transaction) {}