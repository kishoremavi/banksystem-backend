package com.banking.system.service;

import com.banking.system.entity.BankAccount;
import com.banking.system.entity.Transaction;
import com.banking.system.enums.TransactionType;
import com.banking.system.exception.ResourceNotFoundException;
import com.banking.system.repository.BankAccountRepository;
import com.banking.system.repository.TransactionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BankAccountServiceTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private BankAccountService bankAccountService;

    private BankAccount bankAccount;

    @BeforeEach
    void setUp() {
        bankAccount = new BankAccount(
                "Test Customer",
                "test@example.com",
                new BigDecimal("1000.00"));

        bankAccount.setId(10L);
        bankAccount.setActive(true);
    }

    // =====================================================
    // 1. SUCCESSFUL DEPOSIT
    // =====================================================

    @Test
    void depositMoneyShouldSucceed() {

        when(bankAccountRepository.findById(10L))
                .thenReturn(Optional.of(bankAccount));

        when(bankAccountRepository.save(any(BankAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BankAccount result = bankAccountService.depositMoney(
                10L,
                new BigDecimal("200.00"));

        assertEquals(
                new BigDecimal("1200.00"),
                result.getBalance());

        verify(bankAccountRepository).save(bankAccount);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        verify(transactionRepository).save(captor.capture());

        Transaction savedTransaction = captor.getValue();

        assertEquals(
                TransactionType.DEPOSIT,
                savedTransaction.getTransactionType());

        assertEquals(
                new BigDecimal("200.00"),
                savedTransaction.getAmount());

        assertEquals(
                new BigDecimal("1200.00"),
                savedTransaction.getBalanceAfter());

        assertEquals(
                bankAccount,
                savedTransaction.getBankAccount());
    }

    // =====================================================
    // 2. REJECT ZERO DEPOSIT
    // =====================================================

    @Test
    void depositMoneyShouldRejectZeroAmount() {

        when(bankAccountRepository.findById(10L))
                .thenReturn(Optional.of(bankAccount));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bankAccountService.depositMoney(
                        10L,
                        BigDecimal.ZERO));

        assertEquals(
                "Amount must be greater than 0",
                exception.getMessage());

        verify(bankAccountRepository, never())
                .save(any(BankAccount.class));

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }

    // =====================================================
    // 3. REJECT NEGATIVE DEPOSIT
    // =====================================================

    @Test
    void depositMoneyShouldRejectNegativeAmount() {

        when(bankAccountRepository.findById(10L))
                .thenReturn(Optional.of(bankAccount));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bankAccountService.depositMoney(
                        10L,
                        new BigDecimal("-100.00")));

        assertEquals(
                "Amount must be greater than 0",
                exception.getMessage());

        verify(bankAccountRepository, never())
                .save(any(BankAccount.class));

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }

    // =====================================================
    // 4. REJECT DEPOSIT INTO INACTIVE ACCOUNT
    // =====================================================

    @Test
    void depositMoneyShouldRejectInactiveAccount() {

        bankAccount.setActive(false);

        when(bankAccountRepository.findById(10L))
                .thenReturn(Optional.of(bankAccount));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bankAccountService.depositMoney(
                        10L,
                        new BigDecimal("200.00")));

        assertEquals(
                "Bank account is not active",
                exception.getMessage());

        assertEquals(
                new BigDecimal("1000.00"),
                bankAccount.getBalance());

        verify(bankAccountRepository, never())
                .save(any(BankAccount.class));

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }

    // =====================================================
    // 5. REJECT DEPOSIT INTO NONEXISTENT ACCOUNT
    // =====================================================

    @Test
    void depositMoneyShouldRejectNonexistentAccount() {

        when(bankAccountRepository.findById(9999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> bankAccountService.depositMoney(
                        9999L,
                        new BigDecimal("200.00")));

        verify(bankAccountRepository, never())
                .save(any(BankAccount.class));

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }

    // =====================================================
    // 6. SUCCESSFUL WITHDRAWAL
    // =====================================================

    @Test
    void withdrawMoneyShouldSucceed() {

        when(bankAccountRepository
                .findByIdWithPessimisticWriteLock(10L))
                .thenReturn(bankAccount);

        when(bankAccountRepository.save(any(BankAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BankAccount result = bankAccountService.withdrawMoney(
                10L,
                new BigDecimal("300.00"));

        assertEquals(
                new BigDecimal("700.00"),
                result.getBalance());

        verify(bankAccountRepository).save(bankAccount);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        verify(transactionRepository).save(captor.capture());

        Transaction savedTransaction = captor.getValue();

        assertEquals(
                TransactionType.WITHDRAWAL,
                savedTransaction.getTransactionType());

        assertEquals(
                new BigDecimal("300.00"),
                savedTransaction.getAmount());

        assertEquals(
                new BigDecimal("700.00"),
                savedTransaction.getBalanceAfter());

        assertEquals(
                bankAccount,
                savedTransaction.getBankAccount());
    }

    // =====================================================
    // 7. REJECT WITHDRAWAL WITH INSUFFICIENT FUNDS
    // =====================================================

    @Test
    void withdrawMoneyShouldRejectInsufficientFunds() {

        when(bankAccountRepository
                .findByIdWithPessimisticWriteLock(10L))
                .thenReturn(bankAccount);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bankAccountService.withdrawMoney(
                        10L,
                        new BigDecimal("1500.00")));

        assertEquals(
                "Insufficient balance",
                exception.getMessage());

        assertEquals(
                new BigDecimal("1000.00"),
                bankAccount.getBalance());

        verify(bankAccountRepository, never())
                .save(any(BankAccount.class));

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }

    // =====================================================
    // 8. REJECT ZERO WITHDRAWAL
    // =====================================================

    @Test
    void withdrawMoneyShouldRejectZeroAmount() {

        when(bankAccountRepository
                .findByIdWithPessimisticWriteLock(10L))
                .thenReturn(bankAccount);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bankAccountService.withdrawMoney(
                        10L,
                        BigDecimal.ZERO));

        assertEquals(
                "Amount must be greater than 0",
                exception.getMessage());

        verify(bankAccountRepository, never())
                .save(any(BankAccount.class));

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }

    // =====================================================
    // 9. REJECT NEGATIVE WITHDRAWAL
    // =====================================================

    @Test
    void withdrawMoneyShouldRejectNegativeAmount() {

        when(bankAccountRepository
                .findByIdWithPessimisticWriteLock(10L))
                .thenReturn(bankAccount);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bankAccountService.withdrawMoney(
                        10L,
                        new BigDecimal("-100.00")));

        assertEquals(
                "Amount must be greater than 0",
                exception.getMessage());

        verify(bankAccountRepository, never())
                .save(any(BankAccount.class));

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }

    // =====================================================
    // 10. REJECT WITHDRAWAL FROM INACTIVE ACCOUNT
    // =====================================================

    @Test
    void withdrawMoneyShouldRejectInactiveAccount() {

        bankAccount.setActive(false);

        when(bankAccountRepository
                .findByIdWithPessimisticWriteLock(10L))
                .thenReturn(bankAccount);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bankAccountService.withdrawMoney(
                        10L,
                        new BigDecimal("200.00")));

        assertEquals(
                "Bank account is not active",
                exception.getMessage());

        assertEquals(
                new BigDecimal("1000.00"),
                bankAccount.getBalance());

        verify(bankAccountRepository, never())
                .save(any(BankAccount.class));

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }

    // =====================================================
    // 11. REJECT WITHDRAWAL FROM NONEXISTENT ACCOUNT
    // =====================================================

    @Test
    void withdrawMoneyShouldRejectNonexistentAccount() {

        when(bankAccountRepository
                .findByIdWithPessimisticWriteLock(9999L))
                .thenReturn(null);

        assertThrows(
                ResourceNotFoundException.class,
                () -> bankAccountService.withdrawMoney(
                        9999L,
                        new BigDecimal("200.00")));

        verify(bankAccountRepository, never())
                .save(any(BankAccount.class));

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }
}
