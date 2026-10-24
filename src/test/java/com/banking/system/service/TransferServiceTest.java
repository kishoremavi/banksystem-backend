package com.banking.system.service;

import com.banking.system.dto.TransferRequest;
import com.banking.system.dto.TransferResponse;
import com.banking.system.entity.BankAccount;
import com.banking.system.entity.IdempotencyRecord;
import com.banking.system.entity.Transfer;
import com.banking.system.repository.BankAccountRepository;
import com.banking.system.repository.IdempotencyRepository;
import com.banking.system.repository.TransferRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private IdempotencyRepository idempotencyRepository;

    @Mock
    private TransferRequestHasher transferRequestHasher;

    @Mock
    private BankAccountService bankAccountService;

    @InjectMocks
    private TransferService transferService;

    private TransferRequest transferRequest;
    private BankAccount sourceAccount;
    private BankAccount destinationAccount;
    private Transfer savedTransfer;

    private String idempotencyKey;
    private String requestHash;

    @BeforeEach
    void setUp() {
        idempotencyKey = "transfer-123";
        requestHash = "original-request-hash";

        transferRequest = new TransferRequest();
        transferRequest.setSourceAccountId(1L);
        transferRequest.setDestinationAccountId(2L);
        transferRequest.setAmount(new BigDecimal("100.00"));

        sourceAccount = new BankAccount(
                "Alice",
                "alice@example.com",
                new BigDecimal("1000.00"));
        sourceAccount.setId(1L);

        destinationAccount = new BankAccount(
                "Bob",
                "bob@example.com",
                new BigDecimal("500.00"));
        destinationAccount.setId(2L);

        savedTransfer = new Transfer(
                sourceAccount,
                destinationAccount,
                new BigDecimal("100.00"));
        savedTransfer.setId(101L);
        savedTransfer.setStatus("SUCCESS");
    }

    @Test
    void repeatedRequestWithSameKeyReturnsOriginalTransfer() {

        when(transferRequestHasher.hash(transferRequest))
                .thenReturn(requestHash);

        IdempotencyRecord existingRecord = new IdempotencyRecord(
                idempotencyKey,
                requestHash,
                savedTransfer);

        when(idempotencyRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(existingRecord));

        TransferResponse response = transferService.transferMoney(
                transferRequest,
                idempotencyKey);

        assertEquals(101L, response.getTransferId());
        assertEquals(1L, response.getSourceAccountId());
        assertEquals(2L, response.getDestinationAccountId());
        assertEquals(new BigDecimal("100.00"), response.getAmount());
        assertEquals("SUCCESS", response.getStatus());

        verify(bankAccountRepository, never())
                .findByIdWithPessimisticWriteLock(anyLong());

        verify(bankAccountRepository, never())
                .save(any(BankAccount.class));

        verify(transferRepository, never())
                .save(any(Transfer.class));

        verify(idempotencyRepository, never())
                .save(any(IdempotencyRecord.class));
    }

    @Test
    void reusedIdempotencyKeyWithDifferentRequestShouldBeRejected() {

        TransferRequest differentRequest = new TransferRequest();
        differentRequest.setSourceAccountId(1L);
        differentRequest.setDestinationAccountId(2L);
        differentRequest.setAmount(new BigDecimal("500.00"));

        when(transferRequestHasher.hash(differentRequest))
                .thenReturn("different-request-hash");

        IdempotencyRecord existingRecord = new IdempotencyRecord(
                idempotencyKey,
                requestHash,
                savedTransfer);

        when(idempotencyRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.of(existingRecord));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> transferService.transferMoney(
                        differentRequest,
                        idempotencyKey));

        assertEquals(
                "Idempotency key was already used for a different transfer request",
                exception.getMessage());

        verify(bankAccountRepository, never())
                .findByIdWithPessimisticWriteLock(anyLong());

        verify(bankAccountRepository, never())
                .save(any(BankAccount.class));

        verify(transferRepository, never())
                .save(any(Transfer.class));

        verify(idempotencyRepository, never())
                .save(any(IdempotencyRecord.class));
    }

    @Test
    void newIdempotencyKeyShouldProcessAndSaveTransfer() {

        // Arrange
        when(transferRequestHasher.hash(transferRequest))
                .thenReturn(requestHash);

        when(idempotencyRepository.findByIdempotencyKey(idempotencyKey))
                .thenReturn(Optional.empty());

        when(bankAccountRepository.findByIdWithPessimisticWriteLock(1L))
                .thenReturn(sourceAccount);

        when(bankAccountRepository.findByIdWithPessimisticWriteLock(2L))
                .thenReturn(destinationAccount);

        when(transferRepository.save(any(Transfer.class)))
                .thenAnswer(invocation -> {
                    Transfer transfer = invocation.getArgument(0);
                    transfer.setId(101L);
                    return transfer;
                });

        when(idempotencyRepository.save(any(IdempotencyRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        TransferResponse response = transferService.transferMoney(
                transferRequest,
                idempotencyKey);

        // Assert response
        assertEquals(101L, response.getTransferId());
        assertEquals(1L, response.getSourceAccountId());
        assertEquals(2L, response.getDestinationAccountId());
        assertEquals(new BigDecimal("100.00"), response.getAmount());
        assertEquals("SUCCESS", response.getStatus());

        // Assert account balances
        assertEquals(
                new BigDecimal("900.00"),
                sourceAccount.getBalance());

        assertEquals(
                new BigDecimal("600.00"),
                destinationAccount.getBalance());

        // Verify the transfer was saved
        ArgumentCaptor<Transfer> transferCaptor = ArgumentCaptor.forClass(Transfer.class);

        verify(transferRepository, times(1))
                .save(transferCaptor.capture());

        Transfer actualTransfer = transferCaptor.getValue();

        assertEquals(sourceAccount, actualTransfer.getSourceAccount());
        assertEquals(destinationAccount, actualTransfer.getDestinationAccount());
        assertEquals(new BigDecimal("100.00"), actualTransfer.getAmount());
        assertEquals("SUCCESS", actualTransfer.getStatus());

        // Verify the idempotency record was saved
        ArgumentCaptor<IdempotencyRecord> recordCaptor = ArgumentCaptor.forClass(IdempotencyRecord.class);

        verify(idempotencyRepository, times(1))
                .save(recordCaptor.capture());

        IdempotencyRecord actualRecord = recordCaptor.getValue();

        assertEquals(idempotencyKey, actualRecord.getIdempotencyKey());
        assertEquals(requestHash, actualRecord.getRequestHash());
        assertEquals(101L, actualRecord.getTransfer().getId());

        // Both account updates should be requested.
        verify(bankAccountRepository, times(1)).save(sourceAccount);
        verify(bankAccountRepository, times(1)).save(destinationAccount);
    }
}
