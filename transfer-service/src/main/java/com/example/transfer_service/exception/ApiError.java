package com.example.transfer_service.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public class ApiError {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    public enum Code {
        NOT_FOUND, VALIDATION, CONFLICT, INSUFFICIENT_FUNDS, SERVER_ERROR
    }


    private Code code;
    private String message;
    @JsonIgnore
    private final Instant timestamp = Instant.now();

    public ApiError() {}

    @JsonCreator
    public ApiError(
            @JsonProperty("code") Code code,
            @JsonProperty("message") String message) {
        this.code = code;
        this.message = message;
    }

    public Instant getTimestamp() { return timestamp; }
    public Code getCode() { return code; }
    public String getMessage() { return message; }
}
