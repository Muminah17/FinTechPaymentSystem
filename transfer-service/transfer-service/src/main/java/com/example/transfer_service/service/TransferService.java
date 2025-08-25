package com.example.transfer_service.service;

import com.example.transfer_service.dto.TransferRequest;
import com.example.transfer_service.dto.TransferResponse;
import com.example.transfer_service.entity.Transfer;
import com.example.transfer_service.exception.ApiError;
import com.example.transfer_service.exception.NotFoundException;
import com.example.transfer_service.repository.TransferRepository;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final TransferRepository transferRepository;
    private final IdempotencyService idempotencyService;
    private final RestClient restClient;

    public TransferService(TransferRepository transferRepository, IdempotencyService idempotencyService) {
        this.transferRepository = transferRepository;
        this.idempotencyService = idempotencyService;
        restClient = RestClient.builder().build();

    }

    private final String BASE_URL = "http://localhost:8081/v1";

    @Transactional
    public TransferResponse applyTransfer(String key,TransferRequest req) {
        // Check idempotency
        var response = idempotencyService.findResponse(key, req);
        if (response != null) {
            log.info("Idempotent replay for transferId={} -> {}", response.getTransferId(), response.getStatus());
            return response;
        }

        final int maxRetries = 3;
        int attempt = 0;
        while (true) {
            try {
                attempt++;
                response = doApplyTransfer(req);
                idempotencyService.saveResponse(key,req, response);
                return response;
            } catch (OptimisticLockException ole) {
                if (attempt >= maxRetries) throw ole;
                log.warn("Optimistic lock failed on attempt {} for transferId={}", attempt, key);
            }
        }
    }

    private TransferResponse doApplyTransfer(TransferRequest req) {

        TransferResponse res = restClient.post()
                .uri(BASE_URL + "/ledger/transfer")
                .contentType(MediaType.APPLICATION_JSON)
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

                    transferRepository.save(new Transfer(Transfer.Status.FAILED,
                                "Insufficient funds", req.getToAccountId(), req.getFromAccountId(), req.getAmount()));
                    }
                    throw new RuntimeException(apiError != null ? apiError.getMessage() : "Bad Request");
                })
                .body(TransferResponse.class);

        transferRepository.save(new Transfer(Transfer.Status.SUCCESS,
                "OK", req.getToAccountId(), req.getFromAccountId(), req.getAmount()));
        return res;

    }


    @Transactional(readOnly = true)
    public TransferResponse get(Long id) {
        Transfer transfer = transferRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Account " + id + " not found"));
        return new TransferResponse(transfer.getTransferId(), transfer.getStatus().toString(), transfer.getMessage(), transfer.getToAccountId(), transfer.getFromAccountId(), transfer.getAmount());
    }

    @Transactional
    public List<TransferResponse> batchTransfer(List<TransferRequest> requests) {

        ExecutorService executor = Executors.newFixedThreadPool(20);

        List<CompletableFuture<TransferResponse>> futures = requests.stream()
                .map(req -> CompletableFuture.supplyAsync(() -> doApplyTransfer(req), executor))
                .toList();

        List<TransferResponse> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        executor.shutdown();

        return results;

    }

    public static Object convertInputStreamToObject(InputStream is) throws IOException, ClassNotFoundException {
        Object object = null;
        try (ObjectInputStream ois = new ObjectInputStream(is)) {
            object = ois.readObject();
        }
        return object;


    }
}