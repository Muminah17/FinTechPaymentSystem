package com.example.ledger_service.exception;

public class ConflictException extends RuntimeException {
    public ConflictException(String msg) { super(msg); }
}
