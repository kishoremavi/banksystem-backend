package com.banking.system.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.banking.system.entity.BankAccount;
import com.banking.system.mapper.AccountMapper;
import com.banking.system.dto.AccountResponse;
import com.banking.system.dto.TransactionRequest;
import com.banking.system.dto.TransactionResponse;
import com.banking.system.dto.UpdateAccountRequest;
import com.banking.system.service.BankAccountService;

@RestController
@RequestMapping("/api/accounts")
public class BankAccountController {

    private final BankAccountService bankAccountService;

    public BankAccountController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getBankAccountById(@PathVariable Long id,
            @AuthenticationPrincipal String email) {
        bankAccountService.validateAccountOwnership(id, email);
        BankAccount bankAccount = bankAccountService.getBankAccountById(id);
        AccountResponse accountResponse = AccountMapper.toResponse(bankAccount);
        return ResponseEntity.ok(accountResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse> updateBankAccount(@PathVariable Long id,
            @Valid @RequestBody UpdateAccountRequest updateAccountRequest, @AuthenticationPrincipal String email) {
        bankAccountService.validateAccountOwnership(id, email);
        BankAccount updatedAccount = bankAccountService.updateBankAccount(id, updateAccountRequest);
        AccountResponse accountResponse = AccountMapper.toResponse(updatedAccount);
        return ResponseEntity.ok(accountResponse);
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<AccountResponse> depositMoney(@PathVariable Long id,
            @Valid @RequestBody TransactionRequest transactionRequest, @AuthenticationPrincipal String email) {
        bankAccountService.validateAccountOwnership(id, email);
        BankAccount depositedAccount = bankAccountService.depositMoney(id, transactionRequest.getAmount());
        AccountResponse accountResponse = AccountMapper.toResponse(depositedAccount);
        return ResponseEntity.ok(accountResponse);
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<AccountResponse> withdrawMoney(@PathVariable Long id,
            @Valid @RequestBody TransactionRequest transactionRequest, @AuthenticationPrincipal String email) {
        bankAccountService.validateAccountOwnership(id, email);
        BankAccount withdrawnAccount = bankAccountService.withdrawMoney(id, transactionRequest.getAmount());
        AccountResponse accountResponse = AccountMapper.toResponse(withdrawnAccount);
        return ResponseEntity.ok(accountResponse);
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactionsByAccountId(@PathVariable Long id,
            @AuthenticationPrincipal String email) {
        bankAccountService.validateAccountOwnership(id, email);
        List<TransactionResponse> transactions = bankAccountService.getTransactionsByAccountId(id);
        return ResponseEntity.ok().body(transactions);
    }
}
