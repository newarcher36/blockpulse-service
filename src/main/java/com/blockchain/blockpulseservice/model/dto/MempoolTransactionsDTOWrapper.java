package com.blockchain.blockpulseservice.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record MempoolTransactionsDTOWrapper(@JsonProperty("mempool-transactions") MempoolTransactionsDTO mempoolTransactions) {
    public record MempoolTransactionsDTO(List<TransactionDTO> added) {
        public record TransactionDTO(@JsonProperty("txid") String id,
                                         @JsonProperty("vsize") int vSize,
                                         @JsonProperty("fee") BigDecimal fee,
                                         @JsonProperty("feePerVsize") BigDecimal feePerVSize,
                                         @JsonProperty("firstSeen") Instant firstSeen
        ) {
        }
    }
}