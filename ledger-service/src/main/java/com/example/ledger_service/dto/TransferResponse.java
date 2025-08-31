package com.example.ledger_service.dto;

public class TransferResponse {
    private String transferId;
    private String status;
    private String message;
    private Long fromAccountId;
    private Long toAccountId;
    private Integer amount;

    public TransferResponse(String transferId, String status, String message,
                            Long fromAccountId, Long toAccountId, Integer amount) {
        this.transferId = transferId;
        this.status = status;
        this.message = message;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
    }

    public String getTransferId() { return transferId; }
    public String getStatus() { return status; }
    public String getMessage() { return message; }
    public Long getFromAccountId() { return fromAccountId; }
    public Long getToAccountId() { return toAccountId; }
    public Integer getAmount() { return amount; }
}
