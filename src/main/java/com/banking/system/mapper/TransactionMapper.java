package com.banking.system.mapper;

import com.banking.system.dto.TransactionResponse;
import com.banking.system.entity.Transaction;

public class TransactionMapper {

    public static TransactionResponse toTransactionResponse(Transaction transaction) {
        return new TransactionResponse(transaction.getId(), transaction.getAmount(), transaction.getTransactionType(),
                transaction.getBalanceAfter(), transaction.getCreatedAt());
    }

}
