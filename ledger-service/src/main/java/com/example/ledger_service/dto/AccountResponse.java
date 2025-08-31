package com.example.ledger_service.dto;

import java.time.Instant;

public class AccountResponse {
    private Long id;
    private Integer balance;
    private Long version;
    private Instant createdAt;
    private String name;

    public AccountResponse(Long id, Integer balance, Long version, Instant createdAt, String name) {
        this.id = id;
        this.balance = balance;
        this.version = version;
        this.createdAt = createdAt;
        this.name = name;
    }

    public Long getId() { return id; }
    public Integer getBalance() { return balance; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public String getName() { return name; }
}
