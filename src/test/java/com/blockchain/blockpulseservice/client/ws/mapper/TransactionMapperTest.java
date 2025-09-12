package com.blockchain.blockpulseservice.client.ws.mapper;

import com.blockchain.blockpulseservice.model.domain.Transaction;
import com.blockchain.blockpulseservice.model.dto.MempoolTransactionsDTOWrapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionMapperTest {

    private final TransactionMapper mapper = new TransactionMapper();

    @Test
    void mapsTransactionDtosToDomainTransactions_preservingOrder() {
        var t1 = new MempoolTransactionsDTOWrapper.MempoolTransactionsDTO.TransactionDTO(
                "tx-1",
                200,
                new BigDecimal("1500"),
                new BigDecimal("15.0"),
                Instant.parse("2024-09-01T00:00:00Z")
        );
        var t2 = new MempoolTransactionsDTOWrapper.MempoolTransactionsDTO.TransactionDTO(
                "tx-2",
                250,
                new BigDecimal("2250"),
                new BigDecimal("9.0"),
                Instant.parse("2024-09-01T00:00:10Z")
        );

        List<Transaction> result = mapper.mapToTransaction(List.of(t1, t2));

        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(new Transaction(
                "tx-1",
                new BigDecimal("15.0"),
                new BigDecimal("1500"),
                200,
                Instant.parse("2024-09-01T00:00:00Z")
        ));
        assertThat(result.get(1)).isEqualTo(new Transaction(
                "tx-2",
                new BigDecimal("9.0"),
                new BigDecimal("2250"),
                250,
                Instant.parse("2024-09-01T00:00:10Z")
        ));
    }

    @Test
    void mapsEmptyListToEmptyList() {
        assertThat(mapper.mapToTransaction(List.of())).isEmpty();
    }
}

