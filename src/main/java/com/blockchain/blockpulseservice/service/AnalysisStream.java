package com.blockchain.blockpulseservice.service;

import com.blockchain.blockpulseservice.event.AnalyzedTransactionEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Service
public class AnalysisStream {

    // Keeps only the most recent item; new subscribers get it immediately.
    private final Sinks.Many<AnalyzedTransactionEvent> sink = Sinks.many().replay().latest();

    /** Push a new snapshot (non-blocking). */
    public void publish(AnalyzedTransactionEvent dto) {
        sink.tryEmitNext(dto); // ignore backpressure; latest wins
    }

    /** Public flux for the controller. */
    public Flux<AnalyzedTransactionEvent> flux() {
        return sink.asFlux();
    }
}