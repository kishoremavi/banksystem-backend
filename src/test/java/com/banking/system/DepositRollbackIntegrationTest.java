package com.banking.system;

import com.banking.system.entity.BankAccount;
import com.banking.system.entity.Transaction;
import com.banking.system.repository.BankAccountRepository;
import com.banking.system.repository.TransactionRepository;
import com.banking.system.service.BankAccountService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
class DepositRollbackIntegrationTest {

    @Autowired
    private BankAccountService bankAccountService;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoSpyBean
    private TransactionRepository transactionRepositorySpy;

    @Test
    void depositShouldRollbackWhenTransactionSaveFails() {

        // Arrange: create an account with an initial balance of $1,000
        BankAccount account = new BankAccount(
                "Rollback Test",
                "rollback-test@example.com",
                new BigDecimal("1000.00"));

        account = bankAccountRepository.saveAndFlush(account);

        Long accountId = account.getId();

        // Simulate a failure when saving the transaction record
        doThrow(new RuntimeException("Simulated transaction save failure"))
                .when(transactionRepositorySpy)
                .save(any(Transaction.class));

        // Act: attempt a deposit and verify that it fails
        assertThrows(
                RuntimeException.class,
                () -> bankAccountService.depositMoney(
                        accountId,
                        new BigDecimal("200.00")));

        // Assert: inspect the committed database state in a new transaction
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        transactionTemplate.executeWithoutResult(status -> {

            BankAccount savedAccount = bankAccountRepository.findById(accountId)
                    .orElseThrow();

            assertEquals(
                    0,
                    new BigDecimal("1000.00")
                            .compareTo(savedAccount.getBalance()),
                    "Account balance should remain $1,000 after rollback");

            assertEquals(
                    0,
                    transactionRepository
                            .findByBankAccountIdOrderByIdDesc(accountId)
                            .size(),
                    "No deposit transaction should have been committed");
        });
    }
}
