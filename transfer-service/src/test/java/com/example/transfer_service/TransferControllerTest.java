package com.example.transfer_service;

import com.example.transfer_service.controller.TransferController;
import com.example.transfer_service.dto.TransferRequest;
import com.example.transfer_service.dto.TransferResponse;
import com.example.transfer_service.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransferControllerTest {

    private TransferService transferService;
    private TransferController controller;

    @BeforeEach
    void setUp() {
        transferService = mock(TransferService.class);
        controller = new TransferController(transferService);
    }

    @Test
    void transfer_happyPath() {
        TransferRequest req = new TransferRequest("id1", 1L, 2L, 10);
        TransferResponse res = new TransferResponse("id1", "SUCCESS", "OK", 1L, 2L, 10);

        when(transferService.applyTransfer(anyString(), any())).thenReturn(res);

        ResponseEntity<TransferResponse> response = controller.transfer("key", req);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(res, response.getBody());
    }

    @Test
    void get_happyPath() {
        TransferResponse res = new TransferResponse("id1", "SUCCESS", "OK", 1L, 2L, 10);

        when(transferService.get("id1")).thenReturn(res);

        ResponseEntity<TransferResponse> response = controller.get("id1");

        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(res, response.getBody());
    }

    @Test
    void batchTransfer_happyPath() {
        TransferRequest req1 = new TransferRequest("id1", 1L, 2L, 10);
        TransferRequest req2 = new TransferRequest("id2", 1L, 2L, 1);
        List<TransferRequest> reqs = List.of(req1, req2);

        TransferResponse res1 = new TransferResponse("id1", "SUCCESS", "OK", 1L, 2L, 10);
        TransferResponse res2 = new TransferResponse("id2", "SUCCESS", "OK", 1L, 2L, 1);
        List<TransferResponse> responses = List.of(res1, res2);

        when(transferService.batchTransfer(anyString(), ArgumentMatchers.anyList())).thenReturn(responses);

        ResponseEntity<List<TransferResponse>> response = controller.batchTransfer("key", reqs);

        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals(responses, response.getBody());
    }

//    @Test
//    void batchTransfer_emptyList_edgeCase() {
//        List<TransferRequest> reqs = Collections.emptyList();
//
//        // No need to mock service, should fail validation before service is called
//        assertThrows(Exception.class, () -> controller.batchTransfer("key", reqs));
//    }
//
//    @Test
//    void transfer_nullRequest_edgeCase() {
//        assertThrows(Exception.class, () -> controller.transfer("key", null));
//    }
}