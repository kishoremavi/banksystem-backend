package com.banking.system.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.banking.system.entity.BankAccount;
import com.banking.system.entity.Transaction;
import com.banking.system.mapper.AccountMapper;
import com.banking.system.mapper.TransactionMapper;
import com.banking.system.dto.AccountResponse;
import com.banking.system.dto.CreateAccountRequest;
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

    @PostMapping
    public ResponseEntity<AccountResponse> createBankAccount(
            @Valid @RequestBody CreateAccountRequest createAccountRequest) {
        BankAccount createdAccount = bankAccountService.createBankAccount(createAccountRequest.getCustomerName(),
                createAccountRequest.getEmail());
        AccountResponse accountResponse = AccountMapper.toResponse(createdAccount);
        return ResponseEntity.status(HttpStatus.CREATED).body(accountResponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getBankAccountById(@PathVariable Long id) {
        BankAccount bankAccount = bankAccountService.getBankAccountById(id);
        AccountResponse accountResponse = AccountMapper.toResponse(bankAccount);
        return ResponseEntity.ok(accountResponse);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse> updateBankAccount(@PathVariable Long id,
            @Valid @RequestBody UpdateAccountRequest updateAccountRequest) {
        BankAccount updatedAccount = bankAccountService.updateBankAccount(id, updateAccountRequest);
        AccountResponse accountResponse = AccountMapper.toResponse(updatedAccount);
        return ResponseEntity.ok(accountResponse);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBankAccount(@PathVariable Long id) {
        bankAccountService.deleteBankAccount(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<AccountResponse> depositMoney(@PathVariable Long id,
            @Valid @RequestBody TransactionRequest transactionRequest) {
        BankAccount depositedAccount = bankAccountService.depositMoney(id, transactionRequest.getAmount());
        AccountResponse accountResponse = AccountMapper.toResponse(depositedAccount);
        return ResponseEntity.ok(accountResponse);
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<AccountResponse> withdrawMoney(@PathVariable Long id,
            @Valid @RequestBody TransactionRequest transactionRequest) {
        BankAccount withdrawnAccount = bankAccountService.withdrawMoney(id, transactionRequest.getAmount());
        AccountResponse accountResponse = AccountMapper.toResponse(withdrawnAccount);
        return ResponseEntity.ok(accountResponse);
    }

    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactionsByAccountId(@PathVariable Long id) {
        List<TransactionResponse> transactions = bankAccountService.getTransactionsByAccountId(id);
        return ResponseEntity.ok().body(transactions);
    }
}
