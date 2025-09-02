package com.example.ledger_service;

import com.example.ledger_service.dto.AccountResponse;
import com.example.ledger_service.dto.CreateAccount;
import com.example.ledger_service.entity.Account;
import com.example.ledger_service.exception.NotFoundException;
import com.example.ledger_service.repository.AccountRepository;
import com.example.ledger_service.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountServiceTest {

    private AccountRepository accountRepository;
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        accountService = new AccountService(accountRepository);
    }

    @Test
    void createAccount_Success() {
        CreateAccount req = new CreateAccount();
        req.setInitialBalance(100);
        req.setName("Test Account");
        Account savedAccount = new Account(100, "Test Account");

        savedAccount.setVersion(0L);
        when(accountRepository.save(ArgumentMatchers.any(Account.class))).thenReturn(savedAccount);
        AccountResponse response = accountService.create(req);
        assertNotNull(response);

        assertEquals(100, response.getBalance());
        assertEquals("Test Account", response.getName());
        verify(accountRepository, times(1)).save(ArgumentMatchers.any(Account.class));
    }

    @Test
    void createAccount_NegativeInitialBalance_ThrowsException() {
        CreateAccount req = new CreateAccount();
        req.setInitialBalance(-50);
        req.setName("Invalid Account");
        assertThrows(IllegalArgumentException.class, () -> accountService.create(req));
        verify(accountRepository, never()).save(ArgumentMatchers.any(Account.class));
    }

    @Test
    void getAccount_Success() {
        Account account = new Account(200, "Existing Account");
        account.setVersion(1L);
        when(accountRepository.findById(2L)).thenReturn(Optional.of(account));
        AccountResponse response = accountService.get(2L);
        assertNotNull(response);
        assertEquals(200, response.getBalance());
        assertEquals("Existing Account", response.getName());
        verify(accountRepository, times(1)).findById(2L);
    }

    @Test
    void getAccount_NotFound_ThrowsException() {
        when(accountRepository.findById(3L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> accountService.get(3L));
        verify(accountRepository, times(1)).findById(3L);
    }

    @Test
    void createAccount_ZeroInitialBalance_Success() {
        CreateAccount req = new CreateAccount();
        req.setInitialBalance(0);
        req.setName("Zero Balance Account");
        Account savedAccount = new Account(0, "Zero Balance Account");
        savedAccount.setVersion(0L);
        when(accountRepository.save(ArgumentMatchers.any(Account.class))).thenReturn(savedAccount);
        AccountResponse response = accountService.create(req);
        assertNotNull(response);

        assertEquals(0, response.getBalance());
        assertEquals("Zero Balance Account", response.getName());
        verify(accountRepository, times(1)).save(ArgumentMatchers.any(Account.class));
    }

    @Test
    void createAccount_NullInitialBalance_Success() {
        CreateAccount req = new CreateAccount();
        req.setInitialBalance(null);
        req.setName("Null Balance Account");
        Account savedAccount = new Account(0, "Null Balance Account");
        savedAccount.setVersion(0L);
        when(accountRepository.save(ArgumentMatchers.any(Account.class))).thenReturn(savedAccount);
        AccountResponse response = accountService.create(req);
        assertNotNull(response);
        assertEquals(0, response.getBalance());
        assertEquals("Null Balance Account", response.getName());
        verify(accountRepository, times(1)).save(ArgumentMatchers.any(Account.class));
    }
}
