package com.example.transfer_service.clients;

import com.example.transfer_service.dto.TransferRequest;
import com.example.transfer_service.dto.TransferResponse;
import com.example.transfer_service.exception.ApiError;
import com.example.transfer_service.exception.InsufficientFundsException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

@Component
public class LedgerClient {
    private final RestClient restClient;

    public LedgerClient(RestClient restClient) {

        this.restClient = RestClient.builder()
                .requestInterceptor((request, body, execution) -> {
                    String reqId = MDC.get("requestId");
                    if (reqId != null) {
                        request.getHeaders().add("X-Request-ID", reqId);
                    }
                    return execution.execute(request, body);
                })
                .build();;
    }

    public TransferResponse applyTransfer(TransferRequest req) {
        return restClient.post()
                .uri("http://localhost:8081/v1/ledger/transfer")
                .body(req)
                .retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals, (request, response) -> {
                    // Try to parse error body into ApiError (shared DTO)
                    ApiError apiError = null;
                    try {
                        apiError = (ApiError) convertInputStreamToObject(response.getBody());
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }

                    if (apiError != null && apiError.getCode() == ApiError.Code.INSUFFICIENT_FUNDS) {
                        throw new InsufficientFundsException("Insufficient funds");
                    }
                    throw new RuntimeException(apiError != null ? apiError.getMessage() : "Bad Request");
                })
                .body(TransferResponse.class);
    }

    public static Object convertInputStreamToObject(InputStream is) throws IOException, ClassNotFoundException {
        Object object = null;
        try (ObjectInputStream ois = new ObjectInputStream(is)) {
            object = ois.readObject();
        }
        return object;


    }
}
