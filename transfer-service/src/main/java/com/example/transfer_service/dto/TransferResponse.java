package com.example.transfer_service.dto;

public class TransferResponse {
    private String transferId;
    private String status;
    private String message;
    private Long fromAccountId;
    private Long toAccountId;
    private Integer amount;

    public  TransferResponse(){}
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

    public void setTransferId(String transferId) {
        this.transferId = transferId;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setFromAccountId(Long fromAccountId) {
        this.fromAccountId = fromAccountId;
    }

    public void setToAccountId(Long toAccountId) {
        this.toAccountId = toAccountId;
    }

    public void setAmount(Integer amount) {
        this.amount = amount;
    }
}
