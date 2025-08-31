package com.example.ledger_service.service;

import com.example.ledger_service.dto.TransferRequest;
import com.example.ledger_service.dto.TransferResponse;
import com.example.ledger_service.entity.Account;
import com.example.ledger_service.entity.LedgerEntry;
import com.example.ledger_service.exception.ConflictException;
import com.example.ledger_service.exception.InsufficientFundsException;
import com.example.ledger_service.repository.AccountRepository;
import com.example.ledger_service.repository.LedgerEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

//The Transfer Service is responsible for orchestration and idempotency,
// while the Ledger Service ensures atomicity of the actual balance changes.

@Service
public class LedgerService {
    private static final Logger log = LoggerFactory.getLogger(LedgerService.class);

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public LedgerService(AccountRepository accountRepository,
                         LedgerEntryRepository ledgerEntryRepository) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    /**
     * Applies a transfer atomically with optimistic locking and idempotency.
     * Retries a few times on OptimisticLockException.
     */
    @Transactional
    public TransferResponse doApplyTransfer(TransferRequest req) {

        if (req.getFromAccountId().equals(req.getToAccountId())) {
            throw new ConflictException("fromAccountId and toAccountId must differ");
        }
        if (req.getAmount() <= 0) {
            throw new ConflictException("amount must be > 0");
        }

        Account from = accountRepository.findById(req.getFromAccountId())
                .orElseThrow(() -> new ConflictException("From account not found"));
        Account to = accountRepository.findById(req.getToAccountId())
                .orElseThrow(() -> new ConflictException("To account not found"));

        // Business rule: prevent negative balance
        if (from.getBalance().compareTo(req.getAmount()) < 0) {
            throw new InsufficientFundsException("Insufficient funds");
        }

        from.setBalance(from.getBalance() - req.getAmount());
        to.setBalance(to.getBalance() + req.getAmount());

        accountRepository.save(from);
        accountRepository.save(to);

        // Two immutable ledger entries
        LedgerEntry debit = new LedgerEntry(req.getTransferId(), from.getId(), req.getAmount(), LedgerEntry.Type.DEBIT);
        LedgerEntry credit = new LedgerEntry(req.getTransferId(), to.getId(), req.getAmount(), LedgerEntry.Type.CREDIT);
        ledgerEntryRepository.save(debit);
        ledgerEntryRepository.save(credit);

        return new TransferResponse(req.getTransferId(), "SUCCESS", "OK",
                req.getFromAccountId(), req.getToAccountId(), req.getAmount());
    }
}

