package com.banking.system.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.banking.system.enums.TransactionType;

public class TransactionResponse {

    private Long id;
    private BigDecimal amount;
    private TransactionType transactionType;
    private BigDecimal balanceAfter;
    private LocalDateTime createdAt;

    public TransactionResponse(Long id, BigDecimal amount, TransactionType transactionType, BigDecimal balanceAfter,
            LocalDateTime createdAt) {
        this.id = id;
        this.amount = amount;
        this.transactionType = transactionType;
        this.balanceAfter = balanceAfter;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

}
