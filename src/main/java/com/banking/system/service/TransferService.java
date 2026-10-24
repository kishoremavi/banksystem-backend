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
    public TransferResponse transferMoney(TransferRequest transferRequest, String idempotencyKey) {

        String requestHash = transferRequestHasher.hash(transferRequest);
        Optional<IdempotencyRecord> existingRecord = idempotencyRepository.findByIdempotencyKey(idempotencyKey);
        if (existingRecord.isPresent()) {
            IdempotencyRecord record = existingRecord.get();
            if (!record.getRequestHash().equals(requestHash)) {
                throw new IllegalArgumentException(
                        "Idempotency key was already used for a different transfer request");
            }
            Transfer existingTransfer = record.getTransfer();
            return new TransferResponse(
                    existingTransfer.getId(),
                    existingTransfer.getSourceAccount().getId(),
                    existingTransfer.getDestinationAccount().getId(),
                    existingTransfer.getAmount(),
                    existingTransfer.getStatus());
        }

        if (transferRequest.getSourceAccountId().equals(transferRequest.getDestinationAccountId())) {
            throw new IllegalArgumentException("Source and destination accounts cannot be the same");
        }

        List<BankAccount> accounts = lockAccountsInOrder(transferRequest.getSourceAccountId(),
                transferRequest.getDestinationAccountId());

        BankAccount sourceAccount;
        BankAccount destinationAccount;

        if (!accounts.get(0).getId().equals(transferRequest.getSourceAccountId())) {
            sourceAccount = accounts.get(1);
            destinationAccount = accounts.get(0);
        } else {
            sourceAccount = accounts.get(0);
            destinationAccount = accounts.get(1);
        }

        if (transferRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than 0");
        }
        if (sourceAccount.getBalance().compareTo(transferRequest.getAmount()) < 0) {
            throw new IllegalArgumentException("Source account has insufficient balance");
        }

        BigDecimal newSourceBalance = sourceAccount.getBalance().subtract(transferRequest.getAmount());
        BigDecimal newDestinationBalance = destinationAccount.getBalance().add(transferRequest.getAmount());
        sourceAccount.setBalance(newSourceBalance);
        destinationAccount.setBalance(newDestinationBalance);
        bankAccountRepository.save(sourceAccount);
        bankAccountRepository.save(destinationAccount);

        Transfer transfer = new Transfer(sourceAccount, destinationAccount, transferRequest.getAmount());
        transfer.setStatus("SUCCESS");
        Transfer savedTransfer = transferRepository.save(transfer);
        IdempotencyRecord idempotencyRecord = new IdempotencyRecord(idempotencyKey, requestHash, savedTransfer);
        idempotencyRepository.save(idempotencyRecord);
        return new TransferResponse(savedTransfer.getId(), savedTransfer.getSourceAccount().getId(),
                savedTransfer.getDestinationAccount().getId(),
                savedTransfer.getAmount(), savedTransfer.getStatus());
    }
}