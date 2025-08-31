package com.example.ledger_service;

import com.example.ledger_service.controller.AccountController;
import com.example.ledger_service.dto.AccountResponse;
import com.example.ledger_service.dto.CreateAccount;
import com.example.ledger_service.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AccountControllerTest {

    private AccountService accountService;
    private AccountController accountController;

    @BeforeEach
    void setUp() {
        accountService = mock(AccountService.class);
        accountController = new AccountController(accountService);
    }

    @Test
    void testCreate() {
        CreateAccount req = new CreateAccount();
        AccountResponse mockResponse = new AccountResponse(1L,100, 2L, Instant.now(), "User1");
        when(accountService.create(ArgumentMatchers.any(CreateAccount.class))).thenReturn(mockResponse);

        ResponseEntity<AccountResponse> response = accountController.create(req);

        assertEquals(mockResponse, response.getBody());
        assertEquals(200, response.getStatusCodeValue());
        verify(accountService, times(1)).create(req);
    }

    @Test
    void testGet() {
        Long id = 1L;
        AccountResponse mockResponse = new AccountResponse(1L,100, 2L, Instant.now(), "User1");
        when(accountService.get(id)).thenReturn(mockResponse);

        ResponseEntity<AccountResponse> response = accountController.get(id);

        assertEquals(mockResponse, response.getBody());
        assertEquals(200, response.getStatusCodeValue());
        verify(accountService, times(1)).get(id);
    }
}
