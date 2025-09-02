package com.example.transfer_service;

import com.example.transfer_service.clients.LedgerClient;
import com.example.transfer_service.dto.TransferRequest;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.lang.Thread.sleep;
import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@EnableWireMock({@ConfigureWireMock(port = 8081)})
@Import(LedgerClientTest.TestConfig.class)
class LedgerClientTest {

    @Autowired
    private CircuitBreakerRegistry registry;

    @Autowired
    private LedgerClient ledgerClient;

    private CircuitBreaker cb;
    private static final Logger log = LoggerFactory.getLogger(LedgerClientTest.class);

    @BeforeEach
    void setup() {
        cb = registry.circuitBreaker("LedgerClient");
        cb.reset(); // Start fresh
        WireMock.reset();
    }

    @Test
    void testCircuitBreakerOpensAfterFailures() throws InterruptedException {
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        // Simulate Ledger failing with 500
        stubFor(post(urlEqualTo("/v1/ledger/transfer"))
                .willReturn(serverError()));

        // Make 5 calls to trigger breaker
        for (int i = 0; i < 5; i++) {
            try {
                ledgerClient.applyTransfer(new TransferRequest("123" + i,1L, 2L, 100));
            } catch (Exception ignored) {
                log.info("Call failed as expected: " + i);
            }
        }
        // Small delay to ensure state transition is visible
        sleep(150);

        assertThat(cb.getMetrics().getFailureRate()).isGreaterThan((float) 50);
        // Breaker should now be OPEN
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void testCircuitBreakerRecovers() throws InterruptedException {
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        // Simulate Ledger failing with 500
        stubFor(post(urlEqualTo("/v1/ledger/transfer"))
                .willReturn(serverError()));

        // Make 5 calls to trigger breaker
        for (int i = 0; i < 5; i++) {
            try {
                ledgerClient.applyTransfer(new TransferRequest("123" + i,1L, 2L, 100));
            } catch (Exception ignored) {
                log.info("Call failed as expected: " + i);
            }
        }

        // Small delay to ensure state transition is visible
        sleep(150);

        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Ledger is healthy now
        stubFor(post(urlEqualTo("/v1/ledger/transfer"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\n" +
                                "    \"transferId\": \"1111\",\n" +
                                "    \"status\": \"SUCCESS\",\n" +
                                "    \"message\": \"OK\",\n" +
                                "    \"fromAccountId\": 1,\n" +
                                "    \"toAccountId\": 2,\n" +
                                "    \"amount\": 100\n" +
                                "}")));

        ledgerClient.applyTransfer(new TransferRequest("123" ,1L, 2L, 100));
        // Wait long enough for open → half-open transition
        sleep(300);
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

        for (int i = 0; i < 3; i++) {
            ledgerClient.applyTransfer(new TransferRequest("123" ,1L, 2L, 100));
        }

        // Small delay to ensure state transition is visible
        sleep(150);

        assertThat(cb.getMetrics().getFailureRate()).isLessThan((float) 50);
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public CircuitBreakerRegistry circuitBreakerRegistry() {
            CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                    .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                    .failureRateThreshold(50)
                    .slidingWindowSize(5)
                    .waitDurationInOpenState(Duration.ofMillis(100))
                    .permittedNumberOfCallsInHalfOpenState(3)
                    .minimumNumberOfCalls(5)
                    .build();

            return CircuitBreakerRegistry.of(config);
        }
    }


}

