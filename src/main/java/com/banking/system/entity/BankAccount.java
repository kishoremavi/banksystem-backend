package com.banking.system.entity;

import java.math.BigDecimal;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "bank_accounts")
@Getter
@Setter
public class BankAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String customerName;
    @Column(unique = true, nullable = false)
    private String email;
    @Column(precision = 19, scale = 2, nullable = false)
    private BigDecimal balance;
    private boolean active = true;
    @Version
    private Long version;

    public BankAccount() {
    }

    public BankAccount(String customerName, String email, BigDecimal balance) {
        this.customerName = customerName;
        this.email = email;
        this.balance = balance;
    }

}
