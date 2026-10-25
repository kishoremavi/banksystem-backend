package com.banking.system.service;

import com.banking.system.dto.TransferRequest;
import com.banking.system.dto.TransferResponse;
import com.banking.system.entity.BankAccount;
import com.banking.system.repository.BankAccountRepository;
import com.banking.system.repository.IdempotencyRepository;
import com.banking.system.repository.TransferRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ConcurrentTransferIdempotencyTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private IdempotencyRepository idempotencyRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Long sourceAccountId;
    private Long destinationAccountId;
    private String idempotencyKey;

    @BeforeEach
    void setUp() {
        // Create dedicated accounts for this test.
        // We do not delete existing accounts or transactions.

        BankAccount sourceAccount = new BankAccount();
        sourceAccount.setCustomerName("Concurrent Source");
        sourceAccount.setEmail(
                "concurrent-source-" + UUID.randomUUID() + "@example.com");
        sourceAccount.setBalance(new BigDecimal("1000.00"));
        sourceAccount.setActive(true);

        BankAccount destinationAccount = new BankAccount();
        destinationAccount.setCustomerName("Concurrent Destination");
        destinationAccount.setEmail(
                "concurrent-destination-" + UUID.randomUUID() + "@example.com");
        destinationAccount.setBalance(new BigDecimal("500.00"));
        destinationAccount.setActive(true);

        sourceAccount = bankAccountRepository.saveAndFlush(sourceAccount);
        destinationAccount = bankAccountRepository.saveAndFlush(destinationAccount);

        sourceAccountId = sourceAccount.getId();
        destinationAccountId = destinationAccount.getId();

        // Each test run gets a unique key.
        idempotencyKey = "concurrent-transfer-" + UUID.randomUUID();
    }

    @Test
    void concurrentRequestsWithSameKeyShouldTransferMoneyOnlyOnce()
            throws Exception {

        TransferRequest request = new TransferRequest();
        request.setSourceAccountId(sourceAccountId);
        request.setDestinationAccountId(destinationAccountId);
        request.setAmount(new BigDecimal("100.00"));

        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            // Request A
            Future<TransferResponse> firstRequest = executor.submit(() -> {
                readyLatch.countDown();

                if (!startLatch.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                            "Timed out waiting to start request A");
                }

                return transferService.transferMoney(
                        request,
                        idempotencyKey);
            });

            // Request B: same payload and same idempotency key
            Future<TransferResponse> secondRequest = executor.submit(() -> {
                readyLatch.countDown();

                if (!startLatch.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                            "Timed out waiting to start request B");
                }

                return transferService.transferMoney(
                        request,
                        idempotencyKey);
            });

            // Wait until both threads are ready.
            assertTrue(
                    readyLatch.await(10, TimeUnit.SECONDS),
                    "Both requests should be ready");

            // Release both requests at approximately the same time.
            startLatch.countDown();

            // If either request throws an exception, Future.get() exposes it.
            TransferResponse responseA = firstRequest.get(30, TimeUnit.SECONDS);

            TransferResponse responseB = secondRequest.get(30, TimeUnit.SECONDS);

            // Both requests should return a result.
            assertNotNull(responseA);
            assertNotNull(responseB);

            // Both responses must refer to the same transfer.
            assertEquals(
                    responseA.getTransferId(),
                    responseB.getTransferId(),
                    "Both requests should return the same transfer ID");

            assertEquals(
                    sourceAccountId,
                    responseA.getSourceAccountId());

            assertEquals(
                    destinationAccountId,
                    responseA.getDestinationAccountId());

            assertEquals(
                    0,
                    new BigDecimal("100.00")
                            .compareTo(responseA.getAmount()));

            assertEquals("SUCCESS", responseA.getStatus());
            assertEquals("SUCCESS", responseB.getStatus());

        } finally {
            startLatch.countDown();
            executor.shutdownNow();
            executor.awaitTermination(10, TimeUnit.SECONDS);
        }

        // Verify the committed database state in a separate transaction.
        TransactionTemplate readTransaction = new TransactionTemplate(transactionManager);

        readTransaction.executeWithoutResult(status -> {

            BankAccount sourceAfter = bankAccountRepository.findById(sourceAccountId)
                    .orElseThrow();

            BankAccount destinationAfter = bankAccountRepository.findById(destinationAccountId)
                    .orElseThrow();

            // The transfer must happen exactly once.
            assertEquals(
                    0,
                    new BigDecimal("900.00")
                            .compareTo(sourceAfter.getBalance()),
                    "Source account should be debited only once");

            assertEquals(
                    0,
                    new BigDecimal("600.00")
                            .compareTo(destinationAfter.getBalance()),
                    "Destination account should be credited only once");

            // Count transfers involving this dedicated source account.
            Integer transferCount = jdbcTemplate.queryForObject(
                    """
                            SELECT COUNT(*)
                            FROM transfers
                            WHERE source_account_id = ?
                              AND destination_account_id = ?
                            """,
                    Integer.class,
                    sourceAccountId,
                    destinationAccountId);

            assertEquals(
                    1,
                    transferCount,
                    "Exactly one transfer should be committed");

            // The idempotency key must be stored exactly once.
            Integer idempotencyCount = jdbcTemplate.queryForObject(
                    """
                            SELECT COUNT(*)
                            FROM idempotency_records
                            WHERE idempotency_key = ?
                            """,
                    Integer.class,
                    idempotencyKey);

            assertEquals(
                    1,
                    idempotencyCount,
                    "Exactly one idempotency record should exist");
        });
    }
}
