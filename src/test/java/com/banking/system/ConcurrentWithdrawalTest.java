package com.banking.system;

import com.banking.system.entity.BankAccount;
import com.banking.system.repository.BankAccountRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ConcurrentWithdrawalTest {

        private static final String BASE_URL = "http://localhost:8081/api/accounts/";

        @Autowired
        private BankAccountRepository bankAccountRepository;

        @Autowired
        private JdbcTemplate jdbcTemplate;

        private Long accountId;

        @BeforeEach
        void createTestAccount() {

                BankAccount account = new BankAccount(
                                "Concurrent Test Customer",
                                "concurrent.test." + System.nanoTime() + "@example.com",
                                new BigDecimal("1000.00"));

                account.setActive(true);

                BankAccount savedAccount = bankAccountRepository.saveAndFlush(account);

                accountId = savedAccount.getId();
        }

        @Test
        void concurrentWithdrawalsShouldMaintainDatabaseConsistency()
                        throws Exception {

                HttpClient client = HttpClient.newHttpClient();

                CountDownLatch ready = new CountDownLatch(2);
                CountDownLatch start = new CountDownLatch(1);

                ExecutorService executor = Executors.newFixedThreadPool(2);

                try {

                        Future<HttpResponse<String>> requestA = executor.submit(() -> sendWithdrawal(
                                        client,
                                        700,
                                        ready,
                                        start));

                        Future<HttpResponse<String>> requestB = executor.submit(() -> sendWithdrawal(
                                        client,
                                        500,
                                        ready,
                                        start));

                        // Wait until both requests are ready.
                        assertTrue(
                                        ready.await(5, TimeUnit.SECONDS),
                                        "Both requests should be ready");

                        // Release both requests at approximately the same time.
                        start.countDown();

                        HttpResponse<String> responseA = requestA.get(15, TimeUnit.SECONDS);

                        HttpResponse<String> responseB = requestB.get(15, TimeUnit.SECONDS);

                        List<Integer> statuses = List.of(
                                        responseA.statusCode(),
                                        responseB.statusCode());

                        System.out.println("Account ID: " + accountId);

                        System.out.println(
                                        "Request A status: " + responseA.statusCode());

                        System.out.println(
                                        "Request A response: " + responseA.body());

                        System.out.println(
                                        "Request B status: " + responseB.statusCode());

                        System.out.println(
                                        "Request B response: " + responseB.body());

                        // Exactly one withdrawal must succeed.
                        long successfulRequests = statuses.stream()
                                        .filter(status -> status >= 200 && status < 300)
                                        .count();

                        assertEquals(
                                        1,
                                        successfulRequests,
                                        "Exactly one withdrawal should succeed");

                        // The other withdrawal should fail due to insufficient funds.
                        assertTrue(
                                        statuses.stream().anyMatch(status -> status == 400),
                                        "The other withdrawal should return HTTP 400");

                        // Read the actual final balance from MySQL.
                        BigDecimal finalBalance = jdbcTemplate.queryForObject(
                                        """
                                                        SELECT balance
                                                        FROM bank_accounts
                                                        WHERE id = ?
                                                        """,
                                        BigDecimal.class,
                                        accountId);

                        assertNotNull(finalBalance);

                        // Either $700 or $500 can win the race.
                        assertTrue(
                                        finalBalance.compareTo(
                                                        new BigDecimal("300.00")) == 0
                                                        ||
                                                        finalBalance.compareTo(
                                                                        new BigDecimal("500.00")) == 0,
                                        "Final balance must be 300.00 or 500.00");

                        // Verify exactly one withdrawal transaction exists.
                        Integer withdrawalCount = jdbcTemplate.queryForObject(
                                        """
                                                        SELECT COUNT(*)
                                                        FROM transactions
                                                        WHERE account_id = ?
                                                          AND transaction_type = 'WITHDRAWAL'
                                                        """,
                                        Integer.class,
                                        accountId);

                        assertNotNull(withdrawalCount);

                        assertEquals(
                                        1,
                                        withdrawalCount,
                                        "Exactly one withdrawal transaction should be recorded");

                        // Retrieve the successful withdrawal record.
                        List<WithdrawalRecord> withdrawalRecords = jdbcTemplate.query(
                                        """
                                                        SELECT amount, balance_after
                                                        FROM transactions
                                                        WHERE account_id = ?
                                                          AND transaction_type = 'WITHDRAWAL'
                                                        """,
                                        (rs, rowNum) -> new WithdrawalRecord(
                                                        rs.getBigDecimal("amount"),
                                                        rs.getBigDecimal("balance_after")),
                                        accountId);

                        assertEquals(1, withdrawalRecords.size());

                        WithdrawalRecord record = withdrawalRecords.get(0);

                        // The transaction balance must match the account balance.
                        assertEquals(
                                        0,
                                        finalBalance.compareTo(record.balanceAfter()),
                                        "Transaction balance_after must match the final account balance");

                        // Verify the balance calculation.
                        BigDecimal expectedBalance = new BigDecimal("1000.00")
                                        .subtract(record.amount());

                        assertEquals(
                                        0,
                                        expectedBalance.compareTo(finalBalance),
                                        "Final balance must equal initial balance minus successful withdrawal");

                        System.out.println(
                                        "Final database balance: " + finalBalance);

                        System.out.println(
                                        "Successful withdrawal amount: " + record.amount());

                        System.out.println(
                                        "Recorded balance_after: " + record.balanceAfter());

                        System.out.println(
                                        "All concurrent withdrawal consistency checks passed.");

                } finally {

                        start.countDown();

                        executor.shutdownNow();

                        assertTrue(
                                        executor.awaitTermination(5, TimeUnit.SECONDS),
                                        "Executor should terminate");
                }
        }

        private HttpResponse<String> sendWithdrawal(
                        HttpClient client,
                        int amount,
                        CountDownLatch ready,
                        CountDownLatch start)
                        throws IOException, InterruptedException {

                ready.countDown();

                if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException(
                                        "Timed out waiting to start the request");
                }

                String json = """
                                {
                                    "amount": %d
                                }
                                """.formatted(amount);

                HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(
                                                BASE_URL + accountId + "/withdraw"))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString(json))
                                .build();

                return client.send(
                                request,
                                HttpResponse.BodyHandlers.ofString());
        }

        private record WithdrawalRecord(
                        BigDecimal amount,
                        BigDecimal balanceAfter) {
        }
}
