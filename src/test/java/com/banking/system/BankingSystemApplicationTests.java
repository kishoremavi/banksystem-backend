package com.banking.system;

import com.banking.system.entity.BankAccount;
import com.banking.system.repository.BankAccountRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

@SpringBootTest
class BankingSystemApplicationTests {

	@Autowired
	private BankAccountRepository bankAccountRepository;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@Test
	void optimisticLockingShouldRejectStaleUpdate() {

		TransactionTemplate transaction = new TransactionTemplate(transactionManager);

		// Step 1: Create a test account in its own transaction
		Long accountId = transaction.execute(status -> {

			BankAccount account = new BankAccount(
					"Optimistic Test",
					"optimistic.test@example.com",
					new BigDecimal("1000.00"));

			BankAccount savedAccount = bankAccountRepository.saveAndFlush(account);

			return savedAccount.getId();
		});

		// Step 2: Load the account and record its initial version
		BankAccount firstRead = transaction.execute(status -> {
			BankAccount account = bankAccountRepository.findById(accountId).orElseThrow();

			entityManager.detach(account);

			return account;
		});

		BankAccount staleRead = transaction.execute(status -> {
			BankAccount account = bankAccountRepository.findById(accountId).orElseThrow();

			entityManager.detach(account);

			return account;
		});

		// Step 3: Update using the first copy
		transaction.executeWithoutResult(status -> {
			BankAccount account = bankAccountRepository.findById(accountId).orElseThrow();

			account.setCustomerName("Updated By Request A");

			bankAccountRepository.saveAndFlush(account);
		});

		// Step 4: Try to update using the stale copy
		staleRead.setCustomerName("Updated By Request B");

		ObjectOptimisticLockingFailureException exception = assertThrows(
				ObjectOptimisticLockingFailureException.class,
				() -> transaction.executeWithoutResult(status -> {
					bankAccountRepository.saveAndFlush(staleRead);
				}));

		// Step 5: Verify the first update remains in the database
		BankAccount finalAccount = bankAccountRepository.findById(accountId).orElseThrow();

		assertEquals(
				"Updated By Request A",
				finalAccount.getCustomerName());

		assertNotNull(firstRead.getVersion());

		System.out.println(
				"Optimistic locking test passed. Final version: "
						+ finalAccount.getVersion());
	}
}