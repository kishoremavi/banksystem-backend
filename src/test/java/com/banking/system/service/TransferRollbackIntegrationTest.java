package com.banking.system.service;

import com.banking.system.dto.TransferRequest;
import com.banking.system.entity.BankAccount;
import com.banking.system.repository.BankAccountRepository;
import com.banking.system.repository.IdempotencyRepository;
import com.banking.system.repository.TransferRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
class TransferRollbackIntegrationTest {

        @Autowired
        private TransferService transferService;

        @Autowired
        private BankAccountRepository bankAccountRepository;

        @Autowired
        private TransferRepository transferRepository;

        @Autowired
        private IdempotencyRepository idempotencyRepository;

        @Autowired
        private PlatformTransactionManager transactionManager;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        @MockitoSpyBean
        private IdempotencyRepository idempotencyRepositorySpy;

        private Long sourceAccountId;
        private Long destinationAccountId;

        @BeforeEach
        void setUp() {
                BankAccount sourceAccount = new BankAccount();
                sourceAccount.setCustomerName("Rollback Source");
                sourceAccount.setEmail("rollback-source@example.com");
                sourceAccount.setBalance(new BigDecimal("1000.00"));
                sourceAccount.setActive(true);

                BankAccount destinationAccount = new BankAccount();
                destinationAccount.setCustomerName("Rollback Destination");
                destinationAccount.setEmail("rollback-destination@example.com");
                destinationAccount.setBalance(new BigDecimal("500.00"));
                destinationAccount.setActive(true);

                sourceAccount = bankAccountRepository.saveAndFlush(sourceAccount);
                destinationAccount = bankAccountRepository.saveAndFlush(destinationAccount);

                sourceAccountId = sourceAccount.getId();
                destinationAccountId = destinationAccount.getId();
        }

        @Test
        void failedIdempotencySaveShouldRollbackEntireTransfer() {

                doThrow(new RuntimeException("Simulated idempotency save failure"))
                                .when(idempotencyRepositorySpy)
                                .save(any());

                TransferRequest request = new TransferRequest();
                request.setSourceAccountId(sourceAccountId);
                request.setDestinationAccountId(destinationAccountId);
                request.setAmount(new BigDecimal("100.00"));

                TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

                assertThrows(RuntimeException.class,
                                () -> transactionTemplate.execute(status -> transferService.transferMoney(
                                                request,
                                                "rollback-test-key")));

                TransactionTemplate readTransaction = new TransactionTemplate(transactionManager);

                readTransaction.executeWithoutResult(status -> {

                        BankAccount sourceAfter = bankAccountRepository.findById(sourceAccountId).orElseThrow();

                        BankAccount destinationAfter = bankAccountRepository.findById(destinationAccountId)
                                        .orElseThrow();

                        assertEquals(
                                        0,
                                        new BigDecimal("1000.00")
                                                        .compareTo(sourceAfter.getBalance()),
                                        "Source balance should be restored");

                        assertEquals(
                                        0,
                                        new BigDecimal("500.00")
                                                        .compareTo(destinationAfter.getBalance()),
                                        "Destination balance should be restored");

                        assertEquals(
                                        0,
                                        transferRepository.count(),
                                        "No transfer should be committed after failure");

                        assertEquals(
                                        0,
                                        idempotencyRepository.count(),
                                        "No idempotency record should be committed after failure");
                });
        }
}