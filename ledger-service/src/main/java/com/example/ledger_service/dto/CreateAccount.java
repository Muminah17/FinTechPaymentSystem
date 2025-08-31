package com.example.ledger_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateAccount {
    @NotNull @Min(0)
    private Integer initialBalance;

    @Size(max = 120)
    private String name;

    public Integer getInitialBalance() { return initialBalance; }
    public void setInitialBalance(Integer initialBalance) { this.initialBalance = initialBalance; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
