package com.banking.system.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.banking.system.dto.TransactionResponse;
import com.banking.system.dto.UpdateAccountRequest;
import com.banking.system.entity.BankAccount;
import com.banking.system.entity.Transaction;
import com.banking.system.enums.TransactionType;
import com.banking.system.exception.ResourceNotFoundException;
import com.banking.system.mapper.TransactionMapper;
import com.banking.system.repository.BankAccountRepository;
import com.banking.system.repository.TransactionRepository;
import com.banking.system.repository.UserRepository;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.security.access.AccessDeniedException;

@Service
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public BankAccountService(BankAccountRepository bankAccountRepository,
            TransactionRepository transactionRepository, UserRepository userRepository) {
        this.bankAccountRepository = bankAccountRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    public void validateAccountOwnership(Long accountId, String email) {
        // Check whether the user owns this bank account.
        boolean isAccountOwner = userRepository.existsByEmailAndBankAccount_Id(email, accountId);
        // If not, throw an AccessDeniedException.
        if (!isAccountOwner) {
            throw new AccessDeniedException("You are not authorized customer for this account");
        }
    }

    public BankAccount createBankAccount(String customerName, String email) {
        BankAccount bankAccount = new BankAccount();
        bankAccount.setCustomerName(customerName);
        bankAccount.setEmail(email);
        bankAccount.setBalance(BigDecimal.ZERO);
        BankAccount createdAccount = bankAccountRepository.save(bankAccount);
        return createdAccount;
    }

    public BankAccount getBankAccountById(Long id) {
        return bankAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bank account not found : " + id));
    }

    @Transactional
    public BankAccount updateBankAccount(
            Long id,
            UpdateAccountRequest updateAccountRequest) {

        BankAccount bankAccount = getBankAccountById(id);
        if (!bankAccount.isActive()) {
            throw new IllegalArgumentException("Bank account is not active");
        }

        bankAccount.setCustomerName(updateAccountRequest.getCustomerName());
        bankAccount.setEmail(updateAccountRequest.getEmail());

        BankAccount updatedAccount = bankAccountRepository.save(bankAccount);
        return updatedAccount;
    }

    public void deleteBankAccount(Long id) {
        BankAccount bankAccount = getBankAccountById(id);
        if (!bankAccount.isActive()) {
            throw new IllegalArgumentException("Bank account is not active");
        }
        bankAccount.setActive(false);
        bankAccountRepository.save(bankAccount);
    }

    @Transactional
    public BankAccount depositMoney(Long id, BigDecimal amount) {
        BankAccount bankAccount = getBankAccountById(id);
        if (!bankAccount.isActive()) {
            throw new IllegalArgumentException("Bank account is not active");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
        BigDecimal newBalance = bankAccount.getBalance().add(amount);
        bankAccount.setBalance(newBalance);
        BankAccount updatedAccount = bankAccountRepository.save(bankAccount);
        Transaction transaction = new Transaction(amount, TransactionType.DEPOSIT, newBalance, bankAccount);
        transactionRepository.save(transaction);
        return updatedAccount;
    }

    @Transactional
    public BankAccount withdrawMoney(Long id, BigDecimal amount) {

        BankAccount bankAccount = bankAccountRepository.findByIdWithPessimisticWriteLock(id);
        if (bankAccount == null) {
            throw new ResourceNotFoundException("Bank account not found : " + id);
        }
        if (!bankAccount.isActive()) {
            throw new IllegalArgumentException("Bank account is not active");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }

        if (bankAccount.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        BigDecimal newBalance = bankAccount.getBalance().subtract(amount);
        bankAccount.setBalance(newBalance);
        BankAccount updatedAccount = bankAccountRepository.save(bankAccount);
        Transaction transaction = new Transaction(amount, TransactionType.WITHDRAWAL, newBalance,
                bankAccount);
        transactionRepository.save(transaction);
        return updatedAccount;
    }

    public List<TransactionResponse> getTransactionsByAccountId(Long bankAccountId) {
        BankAccount bankAccount = getBankAccountById(bankAccountId);

        List<Transaction> transactions = transactionRepository.findByBankAccountIdOrderByIdDesc(bankAccount.getId());
        return transactions.stream()
                .map(TransactionMapper::toTransactionResponse)
                .collect(Collectors.toList());
    }

    public List<BankAccount> getAllAccounts() {
        return bankAccountRepository.findAll();
    }

}
