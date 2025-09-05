package com.blockchain.blockpulseservice.client.ws.mapper;

import com.blockchain.blockpulseservice.model.domain.Transaction;
import com.blockchain.blockpulseservice.model.dto.MempoolTransactionsDTOWrapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TransactionMapper {
    public List<Transaction> mapToTransaction(List<MempoolTransactionsDTOWrapper.MempoolTransactionsDTO.TransactionDTO> transactionDTOS) {
        return transactionDTOS.stream()
                .map(t ->
                        new Transaction(t.id(), t.feePerVSize(), t.fee(), t.vSize(), t.firstSeen()))
                .toList();
    }
}