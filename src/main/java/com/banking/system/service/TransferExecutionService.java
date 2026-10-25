package com.banking.system.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.banking.system.dto.TransferRequest;
import com.banking.system.dto.TransferResponse;
import com.banking.system.entity.BankAccount;
import com.banking.system.entity.IdempotencyRecord;
import com.banking.system.entity.Transfer;
import com.banking.system.exception.ResourceNotFoundException;
import com.banking.system.repository.BankAccountRepository;
import com.banking.system.repository.IdempotencyRepository;
import com.banking.system.repository.TransferRepository;

@Service
public class TransferExecutionService {

    private final BankAccountRepository bankAccountRepository;
    private final TransferRepository transferRepository;
    private final IdempotencyRepository idempotencyRepository;

    public TransferExecutionService(
            BankAccountRepository bankAccountRepository,
            TransferRepository transferRepository,
            IdempotencyRepository idempotencyRepository) {

        this.bankAccountRepository = bankAccountRepository;
        this.transferRepository = transferRepository;
        this.idempotencyRepository = idempotencyRepository;
    }

    @Transactional
    public TransferResponse executeTransfer(
            TransferRequest transferRequest,
            String idempotencyKey,
            String requestHash) {

        // 1. Lock both accounts in a consistent order.
        List<BankAccount> accounts = lockAccountsInOrder(
                transferRequest.getSourceAccountId(),
                transferRequest.getDestinationAccountId());

        // 2. Identify the source and destination accounts.
        BankAccount sourceAccount;
        BankAccount destinationAccount;

        if (accounts.get(0).getId()
                .equals(transferRequest.getSourceAccountId())) {

            sourceAccount = accounts.get(0);
            destinationAccount = accounts.get(1);

        } else {
            sourceAccount = accounts.get(1);
            destinationAccount = accounts.get(0);
        }

        // 3. Check that the source account has sufficient funds.
        if (sourceAccount.getBalance()
                .compareTo(transferRequest.getAmount()) < 0) {
            throw new IllegalArgumentException(
                    "Source account has insufficient balance");
        }

        // 4. Calculate new balances.
        BigDecimal newSourceBalance = sourceAccount.getBalance()
                .subtract(transferRequest.getAmount());

        BigDecimal newDestinationBalance = destinationAccount.getBalance()
                .add(transferRequest.getAmount());

        // 5. Update both account balances.
        sourceAccount.setBalance(newSourceBalance);
        destinationAccount.setBalance(newDestinationBalance);

        bankAccountRepository.save(sourceAccount);
        bankAccountRepository.save(destinationAccount);

        // 6. Save the transfer record.
        Transfer transfer = new Transfer(
                sourceAccount,
                destinationAccount,
                transferRequest.getAmount());

        transfer.setStatus("SUCCESS");

        Transfer savedTransfer = transferRepository.save(transfer);

        // 7. Save the idempotency record in the same transaction.
        IdempotencyRecord idempotencyRecord = new IdempotencyRecord(
                idempotencyKey,
                requestHash,
                savedTransfer);

        idempotencyRepository.save(idempotencyRecord);

        // 8. Return the response.
        return buildTransferResponse(savedTransfer);
    }

    private List<BankAccount> lockAccountsInOrder(
            Long firstAccountId,
            Long secondAccountId) {

        List<BankAccount> accounts = new ArrayList<>();

        if (firstAccountId < secondAccountId) {
            accounts.add(
                    bankAccountRepository
                            .findByIdWithPessimisticWriteLock(firstAccountId));

            accounts.add(
                    bankAccountRepository
                            .findByIdWithPessimisticWriteLock(secondAccountId));

        } else {
            accounts.add(
                    bankAccountRepository
                            .findByIdWithPessimisticWriteLock(secondAccountId));

            accounts.add(
                    bankAccountRepository
                            .findByIdWithPessimisticWriteLock(firstAccountId));
        }

        if (accounts.get(0) == null || accounts.get(1) == null) {
            throw new ResourceNotFoundException(
                    "One of the accounts was not found");
        }

        if (!accounts.get(0).isActive()
                || !accounts.get(1).isActive()) {
            throw new IllegalArgumentException(
                    "Both accounts must be active");
        }

        return accounts;
    }

    private TransferResponse buildTransferResponse(Transfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getSourceAccount().getId(),
                transfer.getDestinationAccount().getId(),
                transfer.getAmount(),
                transfer.getStatus());
    }
}
