package com.blockchain.blockpulseservice.service.sliding_window;

import com.blockchain.blockpulseservice.model.domain.Transaction;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Slf4j
@Component
public class TransactionCsvWriter {
    private final Path csvPath = Path.of("transactions.csv");

    @PostConstruct
    void ensureCsvHeader() {
        try {
            if (Files.notExists(csvPath) || Files.size(csvPath) == 0) {
                var header = "txid,feePerVsize" + System.lineSeparator();
                Files.writeString(csvPath, header, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            }
        } catch (IOException e) {
            log.warn("Failed to write CSV header to {}: {}", csvPath, e.getMessage());
        }
    }

    public void append(Transaction tx) {
        append(tx.hash(), tx.feePerVSize());
    }

    public void append(String txid, BigDecimal feePerVSize) {
        var line = txid + "," + feePerVSize + System.lineSeparator();
        try {
            Files.writeString(csvPath, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.warn("Failed to append tx to CSV {}: {}", csvPath, e.getMessage());
        }
    }
}

