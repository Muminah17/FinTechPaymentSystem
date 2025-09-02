package com.example.transfer_service.clients;

import com.example.transfer_service.dto.TransferRequest;
import com.example.transfer_service.dto.TransferResponse;
import com.example.transfer_service.exception.ApiError;
import com.example.transfer_service.exception.ConflictException;
import com.example.transfer_service.exception.InsufficientFundsException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class LedgerClient {
    private final RestClient restClient;
    private final ObjectMapper mapper;
    private static final Logger log = LoggerFactory.getLogger(LedgerClient.class);

    public LedgerClient(RestClient restClient) {

        this.restClient = RestClient.builder()
                .requestInterceptor((request, body, execution) -> {
                    String reqId = MDC.get("requestId");
                    if (reqId != null) {
                        request.getHeaders().add("X-Request-ID", reqId);
                    }
                    return execution.execute(request, body);
                })
                .build();
        this.mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build();

    }

    @CircuitBreaker(name = "LedgerClient", fallbackMethod = "fallbackForTransfer")
    public TransferResponse applyTransfer(TransferRequest req) {
        log.info("Calling Ledger service for transfer with Id -> {}", req.getTransferId());
        return
                restClient.post()
                        .uri("http://localhost:8081/v1/ledger/transfer")
                        .body(req)
                        .retrieve()
                        .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                            log.error("Ledger client received 4xx error: {}", response.getStatusCode());
                            try (InputStream is = response.getBody()) {

                                String raw = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                                ApiError apiError = mapper.readValue(raw, ApiError.class);
                                log.error("Ledger client received error response: {}", apiError);
                                switch (apiError.getCode()) {
                                    case NOT_FOUND -> {
                                        throw new com.example.transfer_service.exception.NotFoundException(apiError.getMessage());
                                    }
                                    case INSUFFICIENT_FUNDS -> {
                                        throw new InsufficientFundsException(apiError.getMessage());
                                    }
                                    case CONFLICT -> {
                                        throw new ConflictException(apiError.getMessage());
                                    }
                                    case VALIDATION -> {
                                        throw new IllegalArgumentException(apiError.getMessage());
                                    }
                                    default -> {
                                        throw new RuntimeException(apiError.getMessage());
                                    }
                                }

                            } catch (IOException e) {
                                throw new RuntimeException("Failed to parse ledger error response", e);
                            }
                        })

                        .body(TransferResponse.class);
    }

    private TransferResponse fallbackForTransfer(TransferRequest req, Throwable t) {
        log.error("Ledger service call failed for transfer with Id -> {} with message {}", req.getTransferId(), t.getMessage());

        if (t instanceof InsufficientFundsException e) {
            throw e;
        }
        if (t instanceof ConflictException e) {
            throw e;
        }
        if (t instanceof IllegalArgumentException e) {
            throw e;
        }
        if (t instanceof com.example.transfer_service.exception.NotFoundException e) {
            throw e;
        }
        throw new RuntimeException("Ledger service temporarily unavailable. Please retry later.", t);
    }

}
