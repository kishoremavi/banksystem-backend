package com.banking.system.controller;

import java.time.LocalDate;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.banking.system.dto.AccountResponse;
import com.banking.system.dto.AdminCreateCustomerRequest;
import com.banking.system.dto.RegisterRequest;
import com.banking.system.dto.TransactionRequest;
import com.banking.system.dto.TransactionResponse;
import com.banking.system.dto.UpdateAccountRequest;
import com.banking.system.entity.BankAccount;
import com.banking.system.entity.Transaction;
import com.banking.system.enums.TransactionType;
import com.banking.system.mapper.AccountMapper;
import com.banking.system.mapper.TransactionMapper;
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

    @GetMapping("/transactions")
    public ResponseEntity<Page<TransactionResponse>> getAllTransactionsWithPagination(
            @RequestParam(required = false) Long accountId,
            @RequestParam(required = false) TransactionType transactionType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("Invalid pagination parameters");
        }

        if ((startDate == null) != (endDate == null)) {
            throw new IllegalArgumentException(
                    "Both start date and end date must be provided");
        }

        if (startDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "Start date cannot be after end date");
        }

        Pageable pageable = PageRequest.of(page, size);

        Page<Transaction> transactions;

        if (accountId == null && transactionType == null && startDate == null) {

            transactions = bankAccountService
                    .getAllTransactionsWithPagination(pageable);

        } else if (accountId != null && transactionType == null && startDate == null) {

            transactions = bankAccountService
                    .getTransactionsByAccountIdWithPagination(accountId, pageable);

        } else if (accountId == null && transactionType != null && startDate == null) {

            transactions = bankAccountService
                    .getTransactionsByTypeWithPagination(transactionType, pageable);

        } else if (accountId == null && transactionType == null && startDate != null) {

            transactions = bankAccountService
                    .getTransactionsByDateRangeWithPagination(
                            startDate, endDate, pageable);

        } else if (accountId != null && transactionType != null && startDate == null) {

            transactions = bankAccountService
                    .getTransactionsByTypeAndAccountIdWithPagination(
                            transactionType, accountId, pageable);

        } else {
            throw new IllegalArgumentException(
                    "This combination of filters is not implemented yet");
        }

        Page<TransactionResponse> response = transactions
                .map(TransactionMapper::toTransactionResponse);

        return ResponseEntity.ok(response);
    }

}
