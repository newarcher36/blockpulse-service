package com.blockchain.blockpulseservice.service.stream;

import com.blockchain.blockpulseservice.model.event.AnalyzedTransactionEvent;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;

@Slf4j
@Component
public class AnalysisStream {
    private final Sinks.Many<AnalyzedTransactionEvent> sink;
    private final Flux<AnalyzedTransactionEvent> shared;

    public AnalysisStream(@Value("${app.analysis.tx.sliding-window-size:1000}") int replayLimit,
                          @Value("${app.stream.sse.sample-ms}") long sampleMs) {
        this.sink = Sinks.many().multicast().onBackpressureBuffer();
        this.shared = sink.asFlux()
                .sample(Duration.ofMillis(sampleMs))
                .replay(replayLimit)
                .autoConnect();
        log.info("Analysis stream initialized. Sampling every {} ms, replaying up to {} events.", sampleMs, replayLimit);
    }

    public void publish(AnalyzedTransactionEvent dto) {
        sink.tryEmitNext(dto);
    }

    public Flux<AnalyzedTransactionEvent> flux() {
        return shared;
    }

    @PreDestroy
    public void complete() {
        sink.tryEmitComplete();
    }
}

