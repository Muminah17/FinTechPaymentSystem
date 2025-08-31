package com.example.transfer_service.service;

import com.example.transfer_service.clients.LedgerClient;
import com.example.transfer_service.dto.TransferRequest;
import com.example.transfer_service.dto.TransferResponse;
import com.example.transfer_service.entity.Transfer;
import com.example.transfer_service.exception.ApiError;
import com.example.transfer_service.exception.InsufficientFundsException;
import com.example.transfer_service.exception.NotFoundException;
import com.example.transfer_service.repository.TransferRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);

    private final TransferRepository transferRepository;
    private final IdempotencyService idempotencyService;
    private final LedgerClient ledgerClient;
    private final ObjectMapper mapper;

    public TransferService(TransferRepository transferRepository, IdempotencyService idempotencyService, LedgerClient ledgerClient) {
        this.transferRepository = transferRepository;
        this.idempotencyService = idempotencyService;
        this.ledgerClient = ledgerClient;
        mapper = new ObjectMapper();

    }

    @Transactional
    public TransferResponse applyTransfer(String key,TransferRequest req) {
        // Check idempotency
        var transferResponseString = idempotencyService.findResponse(key, req);
        TransferResponse transferResponse;
        if (transferResponseString.isPresent() && idempotencyService.stillAlive(key)) {
            try {
                transferResponse = mapper.readValue(transferResponseString.get(), TransferResponse.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            log.info("Idempotent replay for transferId={} -> {}", transferResponse.getTransferId(), transferResponse.getStatus());
            return transferResponse;
        }

        final int maxRetries = 3;
        int attempt = 0;
        while (true) {
            try {
                attempt++;
                transferResponse = ledgerClient.applyTransfer(req);
                idempotencyService.saveResponse(key,req, transferResponse);
                transferRepository.save(new Transfer(req.getTransferId(),Transfer.Status.SUCCESS,
                        "OK", req.getToAccountId(), req.getFromAccountId(), req.getAmount()));
                return transferResponse;
            } catch (OptimisticLockException ole) {
                if (attempt >= maxRetries) throw ole;
                log.warn("Optimistic lock failed on attempt {} for transferId={}", attempt, key);
            } catch (InsufficientFundsException e) {
                transferRepository.save(new Transfer(req.getTransferId(),Transfer.Status.FAILED,
                        "Insufficient funds", req.getToAccountId(), req.getFromAccountId(), req.getAmount()));
                throw e;
            } catch (Exception e) {
                log.error("Transfer failed for transferId={}: {}", key, e.getMessage());
                transferRepository.save(new Transfer(req.getTransferId(),Transfer.Status.FAILED,
                        e.getMessage(), req.getToAccountId(), req.getFromAccountId(), req.getAmount()));
                throw e;
            }
        }
    }

    @Transactional(readOnly = true)
    public TransferResponse get(String id) {
        Transfer transfer = transferRepository.findByTransferId(id)
                .orElseThrow(() -> new NotFoundException("Account " + id + " not found"));
        return new TransferResponse(transfer.getTransferId(), transfer.getStatus().toString(), transfer.getMessage(), transfer.getToAccountId(), transfer.getFromAccountId(), transfer.getAmount());
    }

    @Transactional
    public List<TransferResponse> batchTransfer(String key, List<TransferRequest> requests) {
        // Check idempotency
        var responseOpt = idempotencyService.findResponse(key, requests);
        if (responseOpt.isPresent()) {
            try {
                List<TransferResponse> transferResponses = mapper.readValue(responseOpt.get(), new TypeReference<List<TransferResponse>>() {});
                log.info("Idempotent replay for key={} -> {}", key, transferResponses);
                return transferResponses;
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to deserialize idempotent response", e);
            }
        }

        ExecutorService executor = Executors.newFixedThreadPool(20);

        List<CompletableFuture<TransferResponse>> futures = requests.stream()
                .map(req -> CompletableFuture.supplyAsync(() -> ledgerClient.applyTransfer(req), executor))
                .toList();

        List<TransferResponse> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();

        executor.shutdown();

        return results;

    }

}