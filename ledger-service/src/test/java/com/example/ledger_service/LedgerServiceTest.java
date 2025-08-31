package com.example.ledger_service;

import com.example.ledger_service.dto.CreateAccount;
import com.example.ledger_service.dto.TransferRequest;
import com.example.ledger_service.dto.TransferResponse;
import com.example.ledger_service.exception.ConflictException;
import com.example.ledger_service.exception.InsufficientFundsException;
import com.example.ledger_service.repository.LedgerEntryRepository;
import com.example.ledger_service.service.AccountService;
import com.example.ledger_service.service.LedgerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class LedgerServiceTest {


    @Autowired
    AccountService accountService;
    @Autowired
    LedgerService ledgerService;
    @Autowired
    LedgerEntryRepository ledgerRepo;

    @Test
    void happyPathTransfer() {
        var a1 = accountService.create(req(100));
        var a2 = accountService.create(req(10));

        var t = transfer(a1.getId(), a2.getId(), 25);
        TransferResponse res = ledgerService.doApplyTransfer(t);

        assertEquals("SUCCESS", res.getStatus());
        assertEquals(75, accountService.get(a1.getId()).getBalance());
        assertEquals(35, accountService.get(a2.getId()).getBalance());
        assertEquals(2, ledgerRepo.findByTransferId(t.getTransferId()).size());
    }

    @Test
    void insufficientFundsFails() {
        var a1 = accountService.create(req(10));
        var a2 = accountService.create(req(5));

        var t = transfer(a1.getId(), a2.getId(), 50);

        assertThrows(InsufficientFundsException.class,
                () -> ledgerService.doApplyTransfer(t));
        // Ensure no ledger entries written
        assertTrue(ledgerRepo.findByTransferId(t.getTransferId()).isEmpty());
    }

    @Test
    void sameAccountTransferFails() {
        var a1 = accountService.create(req(100));

        var t = transfer(a1.getId(), a1.getId(), 10);

        assertThrows(ConflictException.class, () -> ledgerService.doApplyTransfer(t));
    }

    private CreateAccount req(int balance) {
        var r = new CreateAccount();
        r.setInitialBalance(balance);
        return r;
    }

    private TransferRequest transfer(Long from, Long to, int amount) {
        TransferRequest t = new TransferRequest();
        t.setTransferId(UUID.randomUUID().toString());
        t.setFromAccountId(from);
        t.setToAccountId(to);
        t.setAmount(amount);
        return t;
    }
}
