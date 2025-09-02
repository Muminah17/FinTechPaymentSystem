package com.example.transfer_service.service;

import com.example.transfer_service.clients.LedgerClient;
import com.example.transfer_service.dto.TransferRequest;
import com.example.transfer_service.dto.TransferResponse;
import com.example.transfer_service.entity.Transfer;
import com.example.transfer_service.exception.ConflictException;
import com.example.transfer_service.exception.InsufficientFundsException;
import com.example.transfer_service.exception.NotFoundException;
import com.example.transfer_service.repository.TransferRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                log.info("Storing idempotent response for key={} -> {}", key, transferResponse);
                idempotencyService.saveResponse(key,req, transferResponse);
                transferRepository.save(new Transfer(req.getTransferId(),Transfer.Status.SUCCESS,
                        "OK", req.getToAccountId(), req.getFromAccountId(), req.getAmount()));
                log.info("Transfer completed for transferId={} -> {}", transferResponse.getTransferId(), transferResponse.getStatus());
                return transferResponse;
            } catch (OptimisticLockException ole) {
                if (attempt >= maxRetries) throw ole;
                log.warn("Optimistic lock failed on attempt {} for transferId={}", attempt, key);
            } catch (InsufficientFundsException e) {
                log.warn("Insufficient funds for transferId={}: {}", key, e.getMessage());
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
                .orElseThrow(() -> new NotFoundException("Transfer with " + id + " not found"));
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

        ExecutorService executor = Executors.newFixedThreadPool(requests.size());

        List<CompletableFuture<TransferResponse>> futures = requests.stream()
                .map(req -> CompletableFuture.supplyAsync(() -> ledgerClient.applyTransfer(req), executor)
                        .exceptionally(ex -> new TransferResponse(
                                req.getTransferId(),
                                "FAILED",
                                getErrorMessage((Exception) ex.getCause()),
                                req.getToAccountId(),
                                req.getFromAccountId(),
                                req.getAmount()
                        )))
                .toList();

        List<TransferResponse> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();
        results.forEach(res -> transferRepository.save(new Transfer(res.getTransferId(), Transfer.Status.valueOf(res.getStatus()),
                res.getMessage(), res.getToAccountId(), res.getFromAccountId(), res.getAmount())));
        log.info("Batch transfer completed for key={}", key);
        executor.shutdown();

        return results;

    }

    private String getErrorMessage(Exception e) {
        if (e.getClass().equals(NotFoundException.class)) {
            return "Account not found";
        } else if (e.getClass().equals(InsufficientFundsException.class)) {
            return "Insufficient funds";
        } else if (e.getClass().equals(ConflictException.class)) {
            return "Conflict error";
        } else if (e.getClass().equals(IllegalArgumentException.class)) {
            return "Validation error";
        }
        return "Unknown error";

    }

}