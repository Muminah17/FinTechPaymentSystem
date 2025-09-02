package com.example.transfer_service;

import com.example.transfer_service.clients.LedgerClient;
import com.example.transfer_service.dto.TransferRequest;
import com.example.transfer_service.dto.TransferResponse;
import com.example.transfer_service.entity.Transfer;
import com.example.transfer_service.repository.TransferRepository;
import com.example.transfer_service.service.TransferService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.UniqueConstraint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class TransferServiceIntegrationTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private TransferRepository transferRepository;

    @MockitoBean
    private LedgerClient ledgerClient;


    @Test
    void applyTransfer_savesTransferAndReturnsResponse() {
        TransferRequest request = new TransferRequest("id1", 1L, 2L, 10);
        TransferResponse expectedResponse = new TransferResponse("id1", "SUCCESS", "OK", 1L, 2L, 10);

        when(ledgerClient.applyTransfer(request)).thenReturn(expectedResponse);

        TransferResponse response = transferService.applyTransfer("key", request);

        assertEquals(expectedResponse, response);

        Optional<Transfer> savedTransfer = transferRepository.findByTransferId("id1");
        assertTrue(savedTransfer.isPresent());
        assertEquals("SUCCESS", savedTransfer.get().getStatus().toString());
    }

    //TODO
    @Test
    void get_returnsTransferResponse_whenFound() {
        Transfer transfer = new Transfer("id2", Transfer.Status.SUCCESS, "OK", 1L, 2L, 10);
        transferRepository.save(transfer);

        TransferResponse response = transferService.get("id1");

        assertEquals("id1", response.getTransferId());
        assertEquals("SUCCESS", response.getStatus());
    }

    @Test
    void get_throwsUniqueConstraint_whenFound() {
        Transfer transfer = new Transfer("id3", Transfer.Status.SUCCESS, "OK", 1L, 2L, 10);
        Transfer transfer2 = new Transfer("id3", Transfer.Status.SUCCESS, "OK", 1L, 2L, 10);
        transferRepository.save(transfer);

        assertThrows(DataIntegrityViolationException.class, () -> transferRepository.save(transfer2));

    }

    @Test
    void get_throwsNotFoundException_whenMissing() {
        assertThrows(Exception.class, () -> transferService.get("nonexistent-id"));
    }

    @Test
    void batchedTransfers_areProcessedCorrectly() {
        int batchSize = 5;
        TransferRequest[] requests = new TransferRequest[batchSize];
        TransferResponse[] expectedResponses = new TransferResponse[batchSize];

        for (int i = 0; i < batchSize; i++) {
            requests[i] = new TransferRequest("batch-" + i, 100L, 200L, 10 + i);
            expectedResponses[i] = new TransferResponse("batch-" + i, "SUCCESS", "OK", 100L, 200L, 10 + i);
            when(ledgerClient.applyTransfer(requests[i])).thenReturn(expectedResponses[i]);
        }

        List<TransferResponse> responses = transferService.batchTransfer("key-batch-", java.util.Arrays.asList(requests));

        for (int i = 0; i < batchSize; i++) {
            Optional<Transfer> savedTransfer = transferRepository.findByTransferId("batch-" + i);
            assertTrue(savedTransfer.isPresent());
            assertEquals("SUCCESS", savedTransfer.get().getStatus().toString());
        }
    }

    @Test
    void batchTransfer_handlesPartialFailures() {
        int batchSize = 5;
        TransferRequest[] requests = new TransferRequest[batchSize];

        for (int i = 0; i < batchSize; i++) {
            requests[i] = new TransferRequest("partial-" + i, 100L, 200L, 10 + i);
            if (i == 2) {
                when(ledgerClient.applyTransfer(requests[i])).thenThrow(new RuntimeException("Simulated failure"));
            } else {
                when(ledgerClient.applyTransfer(requests[i]))
                        .thenReturn(new TransferResponse("partial-" + i, "SUCCESS", "OK", 100L, 200L, 10 + i));
            }
        }

        transferService.batchTransfer("key-batch", java.util.Arrays.asList(requests));

        for (int i = 0; i < batchSize; i++) {
            Optional<Transfer> savedTransfer = transferRepository.findByTransferId("partial-" + i);
            assertTrue(savedTransfer.isPresent());
            if (i == 2) {
                assertEquals("FAILED", savedTransfer.get().getStatus().toString());
            } else {
                assertEquals("SUCCESS", savedTransfer.get().getStatus().toString());
            }
        }
    }

    @Test
    void batchedTransfers_withDuplicateIds_areHandledIdempotently() {
        int batchSize = 5;
        TransferRequest[] requests = new TransferRequest[batchSize];
        String duplicateId = "dup-2";

        for (int i = 0; i < batchSize; i++) {
            String transferId = (i == 2) ? duplicateId : "dup-" + i;
            requests[i] = new TransferRequest(transferId, 100L, 200L, 10 + i);
            when(ledgerClient.applyTransfer(requests[i]))
                    .thenReturn(new TransferResponse(transferId, "SUCCESS", "OK", 100L, 200L, 10 + i));
        }

        transferService.batchTransfer("key-batch", java.util.Arrays.asList(requests));
//        for (int i = 0; i < batchSize; i++) {
//            TransferResponse response = transferService.applyTransfer("key-dup-" + i, requests[i]);
//            assertEquals((i == 2) ? duplicateId : "dup-" + i, response.getTransferId());
//            assertEquals("SUCCESS", response.getStatus());
//        }

        long successCount = transferRepository.findAll().stream()
                .filter(t -> t.getTransferId().equals(duplicateId) && t.getStatus() == Transfer.Status.SUCCESS)
                .count();
        assertEquals(1, successCount, "Duplicate transfer ID should be processed only once");
    }

    @Test
    void concurrentTransfers_areProcessedCorrectly() throws Exception {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        String baseTransferId = "concurrent-";
        long fromAccount = 100L;
        long toAccount = 200L;

        // Mock ledgerClient to always succeed
        for (int i = 0; i < threadCount; i++) {
            String transferId = baseTransferId + i;
            TransferRequest request = new TransferRequest(transferId, fromAccount, toAccount, 10);
            TransferResponse response = new TransferResponse(transferId, "SUCCESS", "OK", fromAccount, toAccount, 10);
            when(ledgerClient.applyTransfer(request)).thenReturn(response);
        }

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    String transferId = baseTransferId + idx;
                    TransferRequest request = new TransferRequest(transferId, fromAccount, toAccount, 10);
                    TransferResponse response = transferService.applyTransfer("key-" + transferId, request);
                    assertEquals(transferId, response.getTransferId());
                    assertEquals("SUCCESS", response.getStatus());
                } finally {
                    latch.countDown();
                    executor.shutdown();
                }
            });
        }
    }
}
