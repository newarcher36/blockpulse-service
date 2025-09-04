package com.blockchain.blockpulseservice.controller;

import com.blockchain.blockpulseservice.model.event.AnalyzedTransactionEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class AnalysisStream {

    private final Sinks.Many<AnalyzedTransactionEvent> sink;

    public AnalysisStream(@Value("${app.analysis.tx.sliding-window-size:1000}") int replayLimit) {
        this.sink = Sinks.many().replay().limit(replayLimit);
    }

    public void publish(AnalyzedTransactionEvent dto) {
        sink.tryEmitNext(dto);
    }

    public Flux<AnalyzedTransactionEvent> flux() {
        return sink.asFlux();
    }
}