package com.banking.system.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.banking.system.dto.TransferRequest;
import com.banking.system.dto.TransferResponse;
import com.banking.system.entity.BankAccount;
import com.banking.system.entity.IdempotencyRecord;
import com.banking.system.entity.Transfer;
import com.banking.system.exception.ResourceNotFoundException;
import com.banking.system.repository.BankAccountRepository;
import com.banking.system.repository.IdempotencyRepository;
import com.banking.system.repository.TransferRepository;

import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {

    private final BankAccountRepository bankAccountRepository;
    private final BankAccountService bankAccountService;
    private final TransferRepository transferRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final TransferRequestHasher transferRequestHasher;

    public TransferService(BankAccountRepository bankAccountRepository, BankAccountService bankAccountService,
            TransferRepository transferRepository, IdempotencyRepository idempotencyRepository,
            TransferRequestHasher transferRequestHasher) {
        this.bankAccountRepository = bankAccountRepository;
        this.bankAccountService = bankAccountService;
        this.transferRepository = transferRepository;
        this.idempotencyRepository = idempotencyRepository;
        this.transferRequestHasher = transferRequestHasher;
    }

    private TransferResponse buildTransferResponse(Transfer transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getSourceAccount().getId(),
                transfer.getDestinationAccount().getId(),
                transfer.getAmount(),
                transfer.getStatus());
    }

    private TransferResponse getExistingTransferResponse(
            String idempotencyKey,
            String requestHash) {

        Optional<IdempotencyRecord> existingRecord = idempotencyRepository.findByIdempotencyKey(idempotencyKey);

        if (existingRecord.isEmpty()) {
            return null;
        }

        IdempotencyRecord record = existingRecord.get();

        if (!record.getRequestHash().equals(requestHash)) {
            throw new IllegalArgumentException(
                    "Idempotency key was already used for a different transfer request");
        }

        return buildTransferResponse(record.getTransfer());
    }

    private List<BankAccount> lockAccountsInOrder(Long firstAccountId, Long secondAccountId) {

        List<BankAccount> accounts = new ArrayList<>();
        if (firstAccountId < secondAccountId) {
            accounts.add(bankAccountRepository.findByIdWithPessimisticWriteLock(firstAccountId));
            accounts.add(bankAccountRepository.findByIdWithPessimisticWriteLock(secondAccountId));
        } else {
            accounts.add(bankAccountRepository.findByIdWithPessimisticWriteLock(secondAccountId));
            accounts.add(bankAccountRepository.findByIdWithPessimisticWriteLock(firstAccountId));
        }
        if (accounts.get(0) == null || accounts.get(1) == null) {
            throw new ResourceNotFoundException("One of the accounts is not found");
        }

        if (!accounts.get(0).isActive() || !accounts.get(1).isActive()) {
            throw new IllegalArgumentException("One of the accounts was not Active");
        }
        return accounts;
    }

    @Transactional
    public TransferResponse transferMoney(
            TransferRequest transferRequest,
            String idempotencyKey) {

        // 1. Validate the request and idempotency key.
        if (transferRequest == null) {
            throw new IllegalArgumentException(
                    "Transfer request cannot be null");
        }

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Idempotency key is required");
        }

        if (transferRequest.getSourceAccountId() == null
                || transferRequest.getDestinationAccountId() == null) {
            throw new IllegalArgumentException(
                    "Source and destination account IDs are required");
        }

        if (transferRequest.getSourceAccountId()
                .equals(transferRequest.getDestinationAccountId())) {
            throw new IllegalArgumentException(
                    "Source and destination accounts cannot be the same");
        }

        if (transferRequest.getAmount() == null
                || transferRequest.getAmount()
                        .compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be greater than zero");
        }

        // 2. Calculate a hash representing this request.
        String requestHash = transferRequestHasher.hash(transferRequest);

        // 3. Check whether this idempotency key was already processed.
        TransferResponse existingResponse = getExistingTransferResponse(idempotencyKey, requestHash);

        if (existingResponse != null) {
            return existingResponse;
        }

        // 4. Lock both accounts in a consistent order.
        List<BankAccount> accounts = lockAccountsInOrder(
                transferRequest.getSourceAccountId(),
                transferRequest.getDestinationAccountId());

        // 5. Check again after acquiring the locks.
        // Another request may have completed the same transfer while
        // this request was waiting for the account locks.
        existingResponse = getExistingTransferResponse(idempotencyKey, requestHash);

        if (existingResponse != null) {
            return existingResponse;
        }

        // 6. Identify the source and destination accounts.
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

        // 7. Check that the source account has sufficient funds.
        if (sourceAccount.getBalance()
                .compareTo(transferRequest.getAmount()) < 0) {
            throw new IllegalArgumentException(
                    "Source account has insufficient balance");
        }

        // 8. Calculate the new balances.
        BigDecimal newSourceBalance = sourceAccount.getBalance()
                .subtract(transferRequest.getAmount());

        BigDecimal newDestinationBalance = destinationAccount.getBalance()
                .add(transferRequest.getAmount());

        // 9. Update both account balances.
        sourceAccount.setBalance(newSourceBalance);
        destinationAccount.setBalance(newDestinationBalance);

        bankAccountRepository.save(sourceAccount);
        bankAccountRepository.save(destinationAccount);

        // 10. Create and save the transfer record.
        Transfer transfer = new Transfer(
                sourceAccount,
                destinationAccount,
                transferRequest.getAmount());

        transfer.setStatus("SUCCESS");

        Transfer savedTransfer = transferRepository.save(transfer);

        // 11. Save the idempotency record in the same transaction.
        IdempotencyRecord idempotencyRecord = new IdempotencyRecord(
                idempotencyKey,
                requestHash,
                savedTransfer);

        idempotencyRepository.save(idempotencyRecord);

        // 12. Return the successful transfer response.
        return buildTransferResponse(savedTransfer);
    }

}