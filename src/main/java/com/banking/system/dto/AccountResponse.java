package com.banking.system.dto;

import java.math.BigDecimal;

public class AccountResponse {

    private Long id;
    private String customerName;
    private String email;
    private BigDecimal balance;

    public AccountResponse(Long id, String customerName, String email, BigDecimal balance) {
        this.id = id;
        this.customerName = customerName;
        this.email = email;
        this.balance = balance;
    }

    public Long getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getEmail() {
        return email;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}
