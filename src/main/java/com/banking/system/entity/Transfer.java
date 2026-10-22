package com.banking.system.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transfers")
@Data
@NoArgsConstructor
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_account_id", nullable = false)
    private BankAccount sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_account_id", nullable = false)
    private BankAccount destinationAccount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Transfer(BankAccount sourceAccount, BankAccount destinationAccount, BigDecimal amount) {
        this.sourceAccount = sourceAccount;
        this.destinationAccount = destinationAccount;
        this.amount = amount;
    }

}