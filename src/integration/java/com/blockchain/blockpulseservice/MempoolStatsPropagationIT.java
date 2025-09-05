package com.blockchain.blockpulseservice;

import com.blockchain.blockpulseservice.client.rest.MempoolStatsUpdater;
import com.blockchain.blockpulseservice.model.event.MempoolStatsUpdatedEvent;
import com.blockchain.blockpulseservice.service.TransactionAnalyzerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true"
})
class MempoolStatsPropagationIT {
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

    @Test
    void publishesEventAndConsumedByTransactionAnalyzerService() {
        // Given fee and mempool responses
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

        verify(analyzerService, timeout(500)).onMempoolStatsUpdated(Mockito.argThat(ev -> {
            if (!(ev instanceof MempoolStatsUpdatedEvent e)) return false;
            return e.stats().fastFeePerVByte() == 50
                    && e.stats().mediumFeePerVByte() == 25
                    && e.stats().slowFeePerVByte() == 10
                    && e.stats().mempoolSize() == 1234;
        }));

        server.verify();
    }

    @TestConfiguration
    static class OverrideExecConfig {
        @Bean(name = "mempoolRestExecutor")
        Executor mempoolRestExecutor() {
            return Runnable::run; // make updater run inline in tests
        }
    }
}
