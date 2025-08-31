package com.example.transfer_service.controller;


import com.example.transfer_service.dto.TransferRequest;
import com.example.transfer_service.dto.TransferResponse;
import com.example.transfer_service.service.TransferService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1")
public class TransferController {
    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping("/transfers")
    public ResponseEntity<TransferResponse> transfer(@RequestHeader("Idempotency-Key") String key,
                                                     @Valid @RequestBody TransferRequest req) {
        return ResponseEntity.ok(transferService.applyTransfer(key, req));
    }

    @GetMapping("transfers/{id}")
    public ResponseEntity<TransferResponse> get(@PathVariable String id) {
        return ResponseEntity.ok(transferService.get(id));
    }


    @PostMapping("/transfers/batch")
    public ResponseEntity<List<TransferResponse>> batchTransfer(@RequestHeader("Idempotency-Key") String key,
                                                                @RequestBody @Valid @Size(min = 1, max = 20) List<TransferRequest> reqs) {
        return ResponseEntity.ok(transferService.batchTransfer(key, reqs));
    }
}
