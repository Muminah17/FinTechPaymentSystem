package com.example.ledger_service.controller;

import com.example.ledger_service.dto.TransferRequest;
import com.example.ledger_service.dto.TransferResponse;
import com.example.ledger_service.service.LedgerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/v1")
public class LedgerController {
    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @PostMapping("/ledger/transfer")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest req) {
        return ResponseEntity.ok(ledgerService.doApplyTransfer(req));
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok().body(java.util.Map.of("status", "UP"));
    }
}
