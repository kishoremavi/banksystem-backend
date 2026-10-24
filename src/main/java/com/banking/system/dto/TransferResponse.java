package com.banking.system.dto;

import java.math.BigDecimal;

public class TransferResponse {

    private final Long transferId;
    private final Long sourceAccountId;
    private final Long destinationAccountId;
    private final BigDecimal amount;
    private final String status;

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