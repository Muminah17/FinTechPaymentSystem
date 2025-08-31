package com.example.transfer_service;

import com.example.transfer_service.clients.LedgerClient;
import com.example.transfer_service.dto.TransferRequest;
import com.example.transfer_service.dto.TransferResponse;
import com.example.transfer_service.entity.Transfer;
import com.example.transfer_service.exception.NotFoundException;
import com.example.transfer_service.repository.TransferRepository;
import com.example.transfer_service.service.IdempotencyService;
import com.example.transfer_service.service.TransferService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransferServiceTest {

    private TransferRepository transferRepository;
    private IdempotencyService idempotencyService;
    private TransferService transferService;
    private LedgerClient ledgerClient;

    @BeforeEach
    void setUp() {
        transferRepository = mock(TransferRepository.class);
        idempotencyService = mock(IdempotencyService.class);
        ledgerClient = mock(LedgerClient.class);
        transferService = new TransferService(transferRepository, idempotencyService, ledgerClient);
    }

    @Test
    void applyTransfer_returnsIdempotent_whenFoundAndAlive() throws JsonProcessingException {
        String key = "key";
        TransferRequest req = new TransferRequest("id1", 1L, 2L, 10);
        TransferResponse expected = new TransferResponse("id1", "SUCCESS", "OK", 1L, 2L, 10);

        when(idempotencyService.findResponse(eq(key), eq(req)))
                .thenReturn(Optional.of(new ObjectMapper().writeValueAsString(expected)));
        when(idempotencyService.stillAlive(key)).thenReturn(true);

        TransferResponse result = transferService.applyTransfer(key, req);

        compareTransferResponses(expected,result);
        verify(transferRepository, never()).save(any());
    }

    @Test
    void applyTransfer_executesAndSaves_whenNoIdempotent() {
        String key = "key";
        TransferRequest req = new TransferRequest("id1", 1L, 2L, 10);
        TransferResponse expected = new TransferResponse("id1", "SUCCESS", "OK", 1L, 2L, 10);

        when(idempotencyService.findResponse(eq(key), eq(req))).thenReturn(Optional.empty());
        // Simulate successful save
        when(transferRepository.save(any(Transfer.class))).thenReturn(new Transfer());
        when(ledgerClient.applyTransfer(req)).thenReturn(expected);
        when(idempotencyService.stillAlive(key)).thenReturn(true);

        TransferService spyService = spy(transferService);
        TransferResponse result = spyService.applyTransfer(key, req);

        assertEquals(expected, result);
        verify(idempotencyService).saveResponse(key, req, expected);
    }

    @Test
    void get_returnsTransferResponse_whenFound() {
        String id = "id1";
        Transfer transfer = new Transfer(id, Transfer.Status.SUCCESS, "OK", 1L, 2L, 10);

        when(transferRepository.findByTransferId(id)).thenReturn(Optional.of(transfer));

        TransferResponse result = transferService.get(id);

        assertEquals(id, result.getTransferId());
        assertEquals("SUCCESS", result.getStatus());
        assertEquals("OK", result.getMessage());
    }

    @Test
    void get_throwsNotFound_whenMissing() {
        when(transferRepository.findByTransferId("id1")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> transferService.get("id1"));
    }

    private void compareTransferResponses(TransferResponse expected, TransferResponse actual) {
        assertEquals(expected.getTransferId(), actual.getTransferId());
        assertEquals(expected.getStatus(), actual.getStatus());
        assertEquals(expected.getMessage(), actual.getMessage());
        assertEquals(expected.getToAccountId(), actual.getToAccountId());
        assertEquals(expected.getFromAccountId(), actual.getFromAccountId());
        assertEquals(expected.getAmount(), actual.getAmount());
    }

    @Test
    void batchTransfer_returnsIdempotent_whenFound() throws JsonProcessingException {
        String key = "key";
        List<TransferRequest> reqs = List.of(
                new TransferRequest("id1", 1L, 2L, 10)
        );
        List<TransferResponse> expected = List.of(
                new TransferResponse("id1", "SUCCESS", "OK", 1L, 2L, 10)
        );
        ObjectMapper mapper = new ObjectMapper();
        when(idempotencyService.findResponse(eq(key), eq(reqs)))
                .thenReturn(Optional.of(mapper.writeValueAsString(expected)));

        List<TransferResponse> result = transferService.batchTransfer(key, reqs);

        compareTransferResponses(expected.get(0), result.get(0));
    }

    @Test
    void batchTransfer_executesAll_whenNoIdempotent() {
        String key = "key";
        List<TransferRequest> reqs = List.of(
                new TransferRequest("id1", 1L, 2L, 10),
                new TransferRequest("id2", 11L, 22L, 1)
        );
        List<TransferResponse> expected = List.of(
                new TransferResponse("id1", "SUCCESS", "OK", 1L, 2L, 10),
                new TransferResponse("id2", "SUCCESS", "OK", 11L, 22L, 1)
        );

        when(idempotencyService.findResponse(eq(key), eq(reqs))).thenReturn(Optional.empty());
        when(ledgerClient.applyTransfer(reqs.get(0))).thenReturn(expected.get(0));
        when(ledgerClient.applyTransfer(reqs.get(1))).thenReturn(expected.get(1));

        TransferService spyService = spy(transferService);
        List<TransferResponse> result = spyService.batchTransfer(key, reqs);

        assertEquals(expected, result);
    }
}
