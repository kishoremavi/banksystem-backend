package com.banking.system.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.banking.system.dto.AccountResponse;
import com.banking.system.entity.BankAccount;
import com.banking.system.mapper.AccountMapper;
import com.banking.system.service.BankAccountService;

@RestController
@RequestMapping("/api/admin/accounts")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final BankAccountService bankAccountService;

    public AdminController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAllAccounts() {

        List<AccountResponse> accounts = bankAccountService.getAllAccounts()
                .stream()
                .map(AccountMapper::toResponse)
                .toList();

        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getAccountById(@PathVariable Long id) {
        BankAccount bankAccount = bankAccountService.getBankAccountById(id);
        AccountResponse accountResponse = AccountMapper.toResponse(bankAccount);
        return ResponseEntity.ok(accountResponse);
    }

}
