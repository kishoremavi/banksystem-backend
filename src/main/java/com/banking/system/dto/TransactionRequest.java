package com.banking.system.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class TransactionRequest {
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.00", inclusive = false, message = "Amount must be greater than 0")
    private BigDecimal amount;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

}
