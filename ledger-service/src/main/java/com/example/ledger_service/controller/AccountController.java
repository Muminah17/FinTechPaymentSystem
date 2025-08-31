package com.example.ledger_service.controller;


import com.example.ledger_service.dto.AccountResponse;
import com.example.ledger_service.dto.CreateAccount;
import com.example.ledger_service.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/accounts")
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccount req) {
        return ResponseEntity.ok(accountService.create(req));
    }

    @GetMapping("accounts/{id}")
    public ResponseEntity<AccountResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(accountService.get(id));
    }
}