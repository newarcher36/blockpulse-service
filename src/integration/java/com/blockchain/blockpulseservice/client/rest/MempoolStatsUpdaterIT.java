package com.blockchain.blockpulseservice.client.rest;

import com.blockchain.blockpulseservice.BaseIT;
import com.blockchain.blockpulseservice.model.event.MempoolStatsUpdatedEvent;
import com.blockchain.blockpulseservice.service.TransactionAnalyzerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@TestPropertySource(properties = "spring.main.allow-bean-definition-overriding=true")
class MempoolStatsUpdaterIT extends BaseIT {
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private MempoolStatsUpdater updater;
    @MockitoSpyBean
    private TransactionAnalyzerService analyzerService;
    private MockRestServiceServer server;

    @BeforeEach
    void setup() {
        server = MockRestServiceServer.createServer(restTemplate);
    }

    @TestConfiguration
    static class OverrideExecConfig {
        @Bean(name = "mempoolRestExecutor")
        Executor mempoolRestExecutor() {
            return Runnable::run;
        }
    }

    @Test
    void publishesEventAndConsumedByTransactionAnalyzerService() {
        var feeJson = """
                {
                  "fastestFee": 50,
                  "halfHourFee": 25,
                  "hourFee": 10,
                  "economyFee": 5
                }
                """;
        var mempoolJson = """
                {
                  "count": 1234
                }
                """;

       server.expect(requestTo("https://mempool.space/api/v1/fees/recommended"))
                .andExpect(method(GET))
                .andRespond(withSuccess(feeJson, MediaType.APPLICATION_JSON));

        server.expect(requestTo("https://mempool.space/api/mempool"))
                .andExpect(method(GET))
                .andRespond(withSuccess(mempoolJson, MediaType.APPLICATION_JSON));

        updater.updateMempoolData();

        verify(analyzerService).onMempoolStatsUpdated(Mockito.argThat(ev -> {
            if (!(ev instanceof MempoolStatsUpdatedEvent e)) return false;
            return e.stats().fastFeePerVByte() == 50
                    && e.stats().mediumFeePerVByte() == 25
                    && e.stats().slowFeePerVByte() == 10
                    && e.stats().mempoolSize() == 1234;
        }));

        server.verify();
    }

    @Test
    void doesNotPublishEventWhenOnlyFeeApiFails() {
        server.expect(requestTo("https://mempool.space/api/v1/fees/recommended"))
                .andExpect(method(GET))
                .andRespond(withServerError());

        var mempoolJson = "{\n  \"count\": 42\n}";
        server.expect(requestTo("https://mempool.space/api/mempool"))
                .andExpect(method(GET))
                .andRespond(withSuccess(mempoolJson, MediaType.APPLICATION_JSON));

        updater.updateMempoolData();

        verify(analyzerService, never()).onMempoolStatsUpdated(any());
        server.verify();
    }

    @Test
    void doesNotPublishEventWhenOnlyMempoolApiFails() {
        var feeJson = """
                {
                  "fastestFee": 10,
                  "halfHourFee": 8,
                  "hourFee": 6,
                  "economyFee": 4
                }
                """;

        server.expect(requestTo("https://mempool.space/api/v1/fees/recommended"))
                .andExpect(method(GET))
                .andRespond(withSuccess(feeJson, MediaType.APPLICATION_JSON));

        server.expect(requestTo("https://mempool.space/api/mempool"))
                .andExpect(method(GET))
                .andRespond(withServerError());

        updater.updateMempoolData();

        verify(analyzerService, never()).onMempoolStatsUpdated(any());
        server.verify();
    }

    @Test
    void doesNotPublishEventWhenBothApisFail() {
        server.expect(requestTo("https://mempool.space/api/v1/fees/recommended"))
                .andExpect(method(GET))
                .andRespond(withServerError());

        server.expect(requestTo("https://mempool.space/api/mempool"))
                .andExpect(method(GET))
                .andRespond(withServerError());

        updater.updateMempoolData();

        verify(analyzerService, never()).onMempoolStatsUpdated(any());
        server.verify();
    }
}