package com.example.ledger_service;

import com.example.ledger_service.controller.LedgerController;
import com.example.ledger_service.dto.TransferRequest;
import com.example.ledger_service.dto.TransferResponse;
import com.example.ledger_service.service.LedgerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.OK;

class LedgerControllerTest {

    private LedgerService ledgerService;
    private LedgerController ledgerController;

    @BeforeEach
    void setUp() {
        ledgerService = mock(LedgerService.class);
        ledgerController = new LedgerController(ledgerService);
    }

    @Test
    void testCreateLedger_Success() {
        TransferRequest req = new TransferRequest();
        TransferResponse mockResponse = new TransferResponse("abc123", "SUCCESS", "Transfer successful", 1L, 2L, 100);
        when(ledgerService.doApplyTransfer(any(TransferRequest.class))).thenReturn(mockResponse);

        ResponseEntity<TransferResponse> response = ledgerController.transfer(req);

        assertEquals(OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(ledgerService, times(1)).doApplyTransfer(req);
    }

}
