package com.banking.system.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.banking.system.entity.Transaction;
import com.banking.system.enums.TransactionType;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

        List<Transaction> findByBankAccountIdOrderByIdDesc(Long bankAccountId);

        Page<Transaction> findByBankAccountIdOrderByIdDesc(
                        Long bankAccountId, Pageable pageable);

        Page<Transaction> findAllByOrderByIdDesc(Pageable pageable);

        Page<Transaction> findByTransactionTypeOrderByIdDesc(
                        TransactionType transactionType,
                        Pageable pageable);

        Page<Transaction> findByBankAccountIdAndTransactionTypeOrderByIdDesc(
                        Long bankAccountId,
                        TransactionType transactionType,
                        Pageable pageable);

        Page<Transaction> findAllByCreatedAtBetweenOrderByIdDesc(
                        LocalDateTime startDate,
                        LocalDateTime endDate,
                        Pageable pageable);
}
