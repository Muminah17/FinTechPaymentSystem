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
import java.util.Optional;

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
}
