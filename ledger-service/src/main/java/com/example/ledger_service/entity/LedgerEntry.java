package com.example.ledger_service.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "ledger_entries", indexes = {
        @Index(name = "idx_ledger_transfer", columnList = "transferId")
})
public class LedgerEntry {
    public enum Type { DEBIT, CREDIT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String transferId;

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = false, precision = 19, scale = 4)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Type type;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public LedgerEntry() {}

    public LedgerEntry(String transferId, Long accountId, Integer amount, Type type) {
        this.transferId = transferId;
        this.accountId = accountId;
        this.amount = amount;
        this.type = type;
    }

    public Long getId() { return id; }
    public String getTransferId() { return transferId; }
    public Long getAccountId() { return accountId; }
    public Integer getAmount() { return amount; }
    public Type getType() { return type; }
    public Instant getCreatedAt() { return createdAt; }
}
