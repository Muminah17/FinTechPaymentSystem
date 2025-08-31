package com.example.ledger_service.service;

import com.example.ledger_service.dto.AccountResponse;
import com.example.ledger_service.dto.CreateAccount;
import com.example.ledger_service.entity.Account;
import com.example.ledger_service.exception.NotFoundException;
import com.example.ledger_service.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional
    public AccountResponse create(CreateAccount req) {
        int initial = req.getInitialBalance() == null ? 0 : req.getInitialBalance();
        if (initial < 0) {
            throw new IllegalArgumentException("Initial balance must be >= 0");
        }
        Account acc = new Account(initial, req.getName());
        acc = accountRepository.save(acc);
        return new AccountResponse(acc.getId(), acc.getBalance(), acc.getVersion(), acc.getCreatedAt(), acc.getName());
    }

    @Transactional(readOnly = true)
    public AccountResponse get(Long id) {
        Account acc = accountRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Account " + id + " not found"));
        return new AccountResponse(acc.getId(), acc.getBalance(), acc.getVersion(), acc.getCreatedAt(), acc.getName());
    }
}
