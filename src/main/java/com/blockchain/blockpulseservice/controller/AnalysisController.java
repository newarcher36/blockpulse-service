package com.blockchain.blockpulseservice.controller;

import com.blockchain.blockpulseservice.event.AnalyzedTransactionEvent;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/transactions")
public class AnalysisController {
    private final AnalysisStream stream;

    public AnalysisController(AnalysisStream stream) {
        this.stream = stream;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AnalyzedTransactionEvent>> stream() {
        return stream.flux()
                .map(dto -> ServerSentEvent.builder(dto).build())
                .sample(Duration.ofSeconds(2));
    }
}