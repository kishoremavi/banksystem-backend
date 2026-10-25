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
import java.util.UUID;

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

        private String idempotencyKey;

        @BeforeEach
        void setUp() {

                BankAccount sourceAccount = new BankAccount();
                sourceAccount.setCustomerName("Rollback Source");
                sourceAccount.setEmail(
                                "rollback-source-" + UUID.randomUUID() + "@example.com");
                sourceAccount.setBalance(new BigDecimal("1000.00"));
                sourceAccount.setActive(true);

                BankAccount destinationAccount = new BankAccount();
                destinationAccount.setCustomerName("Rollback Destination");
                destinationAccount.setEmail(
                                "rollback-destination-" + UUID.randomUUID() + "@example.com");
                destinationAccount.setBalance(new BigDecimal("500.00"));
                destinationAccount.setActive(true);

                sourceAccount = bankAccountRepository.saveAndFlush(sourceAccount);
                destinationAccount = bankAccountRepository.saveAndFlush(destinationAccount);

                sourceAccountId = sourceAccount.getId();
                destinationAccountId = destinationAccount.getId();

                idempotencyKey = "rollback-test-" + UUID.randomUUID();
        }

        @Test
        void failedIdempotencySaveShouldRollbackEntireTransfer() {

                long transfersBefore = transferRepository.count();
                long idempotencyRecordsBefore = idempotencyRepository.count();

                doThrow(new RuntimeException(
                                "Simulated idempotency save failure"))
                                .when(idempotencyRepositorySpy)
                                .save(any());

                TransferRequest request = new TransferRequest();
                request.setSourceAccountId(sourceAccountId);
                request.setDestinationAccountId(destinationAccountId);
                request.setAmount(new BigDecimal("100.00"));

                assertThrows(
                                RuntimeException.class,
                                () -> transferService.transferMoney(
                                                request,
                                                idempotencyKey));

                TransactionTemplate readTransaction = new TransactionTemplate(transactionManager);

                readTransaction.executeWithoutResult(status -> {

                        BankAccount sourceAfter = bankAccountRepository.findById(sourceAccountId)
                                        .orElseThrow();

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

                        long transfersAfter = transferRepository.count();
                        long idempotencyRecordsAfter = idempotencyRepository.count();

                        assertEquals(
                                        transfersBefore,
                                        transfersAfter,
                                        "Failed transfer should not create a transfer record");

                        assertEquals(
                                        idempotencyRecordsBefore,
                                        idempotencyRecordsAfter,
                                        "Failed transfer should not create an idempotency record");
                });
        }
}
