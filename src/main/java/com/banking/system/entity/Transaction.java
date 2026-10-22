package com.banking.system.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import com.banking.system.enums.TransactionType;

@Entity
@Table(name = "transactions")
@Data
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;

    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal balanceAfter;

    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private BankAccount bankAccount;

    public Transaction() {

    }

    public Transaction(BigDecimal amount, TransactionType transactionType, BigDecimal balanceAfter,
            BankAccount bankAccount) {
        this.amount = amount;
        this.transactionType = transactionType;
        this.balanceAfter = balanceAfter;
        this.createdAt = LocalDateTime.now();
        this.bankAccount = bankAccount;
    }

}
