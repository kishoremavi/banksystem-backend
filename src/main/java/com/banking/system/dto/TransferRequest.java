package com.banking.system.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class TransferRequest {

    @NotNull(message = "Source account ID is required")
    @Positive(message = "Source account ID must be greater than zero")
    private Long sourceAccountId;

    @NotNull(message = "Destination account ID is required")
    @Positive(message = "Destination account ID must be greater than zero")
    private Long destinationAccountId;

    @NotNull(message = "Transfer amount is required")
    @DecimalMin(value = "0.00", inclusive = false, message = "Transfer amount must be greater than zero")
    private BigDecimal amount;

    public TransferRequest() {
    }

    public Long getSourceAccountId() {
        return sourceAccountId;
    }

    public void setSourceAccountId(Long sourceAccountId) {
        this.sourceAccountId = sourceAccountId;
    }

    public Long getDestinationAccountId() {
        return destinationAccountId;
    }

    public void setDestinationAccountId(Long destinationAccountId) {
        this.destinationAccountId = destinationAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
