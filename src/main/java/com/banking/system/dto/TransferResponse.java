package com.banking.system.dto;

import java.math.BigDecimal;

public class TransferResponse {

    private Long transferId;
    private Long sourceAccountId;
    private Long destinationAccountId;
    private BigDecimal amount;
    private String status;

    public TransferResponse() {
    }

    public TransferResponse(
            Long transferId,
            Long sourceAccountId,
            Long destinationAccountId,
            BigDecimal amount,
            String status) {
        this.transferId = transferId;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.status = status;
    }

    public Long getTransferId() {
        return transferId;
    }

    public Long getSourceAccountId() {
        return sourceAccountId;
    }

    public Long getDestinationAccountId() {
        return destinationAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

}