package com.example.transfer_service.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

import static org.springframework.data.jpa.domain.AbstractPersistable_.id;

@Entity
@Table(name = "transfer")
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String transferId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status;

    @Column(length = 255)
    private String message;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false, updatable = false)
    private Long fromAccountId;
    @Column(nullable = false, updatable = false)
    private Long toAccountId;
    @Column(nullable = false, updatable = false)
    private Integer amount;

    public enum Status { SUCCESS, FAILED }

    public Transfer() {}

    public Transfer(String transferId, Status status, String message, Long toAccountId, Long fromAccountId, Integer amount) {
        this.transferId = transferId;
        this.status = status;
        this.message = message;
        this.toAccountId = toAccountId;
        this.fromAccountId = fromAccountId;
        this.amount = amount;
    }

    public String getTransferId() { return transferId; }
    public Status getStatus() { return status; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }

    public void setStatus(Status status) { this.status = status; }
    public void setMessage(String message) { this.message = message; }

    public Long getFromAccountId() { return fromAccountId; }
    public void setFromAccountId(Long fromAccountId) { this.fromAccountId = fromAccountId; }
    public Long getToAccountId() { return toAccountId; }
    public void setToAccountId(Long toAccountId) { this.toAccountId = toAccountId; }
    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }
}

