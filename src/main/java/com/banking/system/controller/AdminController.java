package com.banking.system.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.banking.system.dto.AccountResponse;
import com.banking.system.dto.AdminCreateCustomerRequest;
import com.banking.system.dto.RegisterRequest;
import com.banking.system.dto.TransactionRequest;
import com.banking.system.dto.TransactionResponse;
import com.banking.system.dto.UpdateAccountRequest;
import com.banking.system.entity.BankAccount;
import com.banking.system.mapper.AccountMapper;
import com.banking.system.service.BankAccountService;
import com.banking.system.service.RegistrationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/accounts")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final BankAccountService bankAccountService;
    private final RegistrationService registrationService;

    public AdminController(BankAccountService bankAccountService, RegistrationService registrationService) {
        this.bankAccountService = bankAccountService;
        this.registrationService = registrationService;
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

    @PostMapping("/createaccount")
    public ResponseEntity<Long> createCustomer(
            @Valid @RequestBody AdminCreateCustomerRequest request) {

        RegisterRequest registerRequest = new RegisterRequest(
                request.customerName(),
                request.email(),
                request.password());

        Long userId = registrationService.register(registerRequest);

        return ResponseEntity.status(HttpStatus.CREATED).body(userId);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AccountResponse> updateBankAccount(@PathVariable Long id,
            @Valid @RequestBody UpdateAccountRequest updateAccountRequest) {
        BankAccount updatedAccount = bankAccountService.updateBankAccount(id, updateAccountRequest);
        AccountResponse accountResponse = AccountMapper.toResponse(updatedAccount);
        return ResponseEntity.ok(accountResponse);
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
        return ResponseEntity.ok(transactions);
    }

    @DeleteMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateAccount(@PathVariable Long id) {

        bankAccountService.deactivateBankAccount(id);

        return ResponseEntity.noContent().build();
    }

}
