package com.banking.system.service;

import com.banking.system.dto.TransferRequest;
import com.banking.system.dto.TransferResponse;
import com.banking.system.entity.IdempotencyRecord;
import com.banking.system.entity.Transfer;
import com.banking.system.repository.IdempotencyRepository;
import com.banking.system.service.TransferRequestHasher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class TransferService {

    private final TransferExecutionService transferExecutionService;
    private final IdempotencyRepository idempotencyRepository;
    private final TransferRequestHasher transferRequestHasher;
    private final TransactionTemplate transactionTemplate;

    public TransferService(
            TransferExecutionService transferExecutionService,
            IdempotencyRepository idempotencyRepository,
            TransferRequestHasher transferRequestHasher,
            TransactionTemplate transactionTemplate) {

        this.transferExecutionService = transferExecutionService;
        this.idempotencyRepository = idempotencyRepository;
        this.transferRequestHasher = transferRequestHasher;
        this.transactionTemplate = transactionTemplate;
    }

    public TransferResponse transferMoney(
            TransferRequest request,
            String idempotencyKey) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Idempotency key is required");
        }

        if (request == null) {
            throw new IllegalArgumentException(
                    "Transfer request is required");
        }

        if (request.getSourceAccountId() == null
                || request.getDestinationAccountId() == null
                || request.getAmount() == null) {
            throw new IllegalArgumentException(
                    "Source account, destination account, and amount are required");
        }

        if (request.getSourceAccountId()
                .equals(request.getDestinationAccountId())) {
            throw new IllegalArgumentException(
                    "Source and destination accounts must be different");
        }

        if (request.getAmount().signum() <= 0) {
            throw new IllegalArgumentException(
                    "Transfer amount must be greater than zero");
        }

        String requestHash = transferRequestHasher.hash(request);

        // Fast path: return the result of an earlier successful request.
        TransferResponse existingResponse = findExistingTransfer(idempotencyKey, requestHash);

        if (existingResponse != null) {
            return existingResponse;
        }

        try {
            return transferExecutionService.executeTransfer(
                    request,
                    idempotencyKey,
                    requestHash);

        } catch (DataIntegrityViolationException ex) {

            /*
             * A concurrent request may have inserted the same
             * idempotency key. The execution transaction has already
             * failed and rolled back before control reaches this catch.
             */
            TransferResponse existingAfterConflict = findExistingTransfer(idempotencyKey, requestHash);

            if (existingAfterConflict != null) {
                return existingAfterConflict;
            }

            throw ex;
        }
    }

    private TransferResponse findExistingTransfer(
            String idempotencyKey,
            String requestHash) {

        return transactionTemplate.execute(status -> {

            IdempotencyRecord record = idempotencyRepository
                    .findByIdempotencyKey(idempotencyKey)
                    .orElse(null);

            if (record == null) {
                return null;
            }

            if (!record.getRequestHash().equals(requestHash)) {
                throw new IllegalArgumentException(
                        "Idempotency key has already been used "
                                + "with a different request");
            }

            Transfer transfer = record.getTransfer();

            /*
             * Construct the response while the transaction is open.
             * This allows Hibernate to initialize any required
             * relationships before the session closes.
             */
            return new TransferResponse(
                    transfer.getId(),
                    transfer.getSourceAccount().getId(),
                    transfer.getDestinationAccount().getId(),
                    transfer.getAmount(),
                    transfer.getStatus());
        });
    }
}
