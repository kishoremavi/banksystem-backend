package com.banking.system.mapper;

import com.banking.system.entity.BankAccount;

import com.banking.system.dto.AccountResponse;

public class AccountMapper {

    public static AccountResponse toResponse(BankAccount bankAccount) {
        return new AccountResponse(bankAccount.getId(), bankAccount.getCustomerName(), bankAccount.getEmail(),
                bankAccount.getBalance());
    }

}
