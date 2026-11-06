package com.banking.system.specification;

import com.banking.system.entity.Transaction;
import com.banking.system.enums.TransactionType;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class TransactionSpecifications {

    public static Specification<Transaction> hasAccountId(Long accountId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
                root.get("bankAccount").get("id"),
                accountId);
    }

    public static Specification<Transaction> hasTransactionType(
            TransactionType transactionType) {

        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
                root.get("transactionType"),
                transactionType);
    }

    public static Specification<Transaction> createdBetween(
            LocalDateTime startDate,
            LocalDateTime endDate) {

        return (root, query, criteriaBuilder) -> criteriaBuilder.between(
                root.get("createdAt"),
                startDate,
                endDate);
    }

    public static Specification<Transaction> buildSpecification(
            Long accountId,
            TransactionType transactionType,
            LocalDateTime startDate,
            LocalDateTime endDate) {

        Specification<Transaction> specification = Specification.unrestricted();

        if (accountId != null) {
            specification = specification.and(hasAccountId(accountId));
        }

        if (transactionType != null) {
            specification = specification.and(
                    hasTransactionType(transactionType));
        }

        if (startDate != null && endDate != null) {
            specification = specification.and(
                    createdBetween(startDate, endDate));
        }

        return specification;
    }

}