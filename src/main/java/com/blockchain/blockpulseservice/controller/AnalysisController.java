package com.blockchain.blockpulseservice.controller;

import com.blockchain.blockpulseservice.model.event.AnalyzedTransactionEvent;
import com.blockchain.blockpulseservice.service.stream.AnalysisStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import static org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE;

@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class AnalysisController {
    private final AnalysisStream stream;

    @GetMapping(value = "/stream", produces = TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<AnalyzedTransactionEvent>> stream() {
        return stream.flux()
                .doOnSubscribe(s -> log.info("Stream subscribed."))
                .doOnCancel(() -> log.info("Stream cancelled."))
                .map(dto -> ServerSentEvent.builder(dto).build());
    }
}
