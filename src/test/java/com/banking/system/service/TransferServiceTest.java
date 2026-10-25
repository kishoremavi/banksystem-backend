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
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
        private TransferExecutionService transferExecutionService;

        @Mock
        private TransactionTemplate transactionTemplate;

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

                // Execute TransactionTemplate callbacks inside unit tests.
                lenient().when(transactionTemplate.execute(
                                any(TransactionCallback.class)))
                                .thenAnswer(invocation -> {
                                        TransactionCallback<?> callback = invocation.getArgument(0);

                                        return callback.doInTransaction(
                                                        mock(TransactionStatus.class));
                                });
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

                verify(transferExecutionService, never())
                                .executeTransfer(any(), any(), any());

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
                                "Idempotency key has already been used with a different request",
                                exception.getMessage());

                verify(transferExecutionService, never())
                                .executeTransfer(any(), any(), any());

                verify(transferRepository, never())
                                .save(any(Transfer.class));

                verify(idempotencyRepository, never())
                                .save(any(IdempotencyRecord.class));
        }

        @Test
        void newIdempotencyKeyShouldProcessAndSaveTransfer() {

                when(transferRequestHasher.hash(transferRequest))
                                .thenReturn(requestHash);

                when(idempotencyRepository.findByIdempotencyKey(idempotencyKey))
                                .thenReturn(Optional.empty());

                TransferResponse expectedResponse = new TransferResponse(
                                101L,
                                1L,
                                2L,
                                new BigDecimal("100.00"),
                                "SUCCESS");

                when(transferExecutionService.executeTransfer(
                                transferRequest,
                                idempotencyKey,
                                requestHash))
                                .thenAnswer(invocation -> {

                                        sourceAccount.setBalance(
                                                        new BigDecimal("900.00"));

                                        destinationAccount.setBalance(
                                                        new BigDecimal("600.00"));

                                        return expectedResponse;
                                });

                TransferResponse response = transferService.transferMoney(
                                transferRequest,
                                idempotencyKey);

                assertEquals(101L, response.getTransferId());
                assertEquals(1L, response.getSourceAccountId());
                assertEquals(2L, response.getDestinationAccountId());
                assertEquals(new BigDecimal("100.00"), response.getAmount());
                assertEquals("SUCCESS", response.getStatus());

                assertEquals(
                                new BigDecimal("900.00"),
                                sourceAccount.getBalance());

                assertEquals(
                                new BigDecimal("600.00"),
                                destinationAccount.getBalance());

                verify(transferExecutionService, times(1))
                                .executeTransfer(
                                                transferRequest,
                                                idempotencyKey,
                                                requestHash);
        }

        @Test
        void sameKeyAndSameRequestShouldNotExecuteTransferTwice() {

                when(transferRequestHasher.hash(transferRequest))
                                .thenReturn(requestHash);

                IdempotencyRecord existingRecord = new IdempotencyRecord(
                                idempotencyKey,
                                requestHash,
                                savedTransfer);

                when(idempotencyRepository.findByIdempotencyKey(idempotencyKey))
                                .thenReturn(Optional.of(existingRecord));

                TransferResponse firstResponse = transferService.transferMoney(
                                transferRequest,
                                idempotencyKey);

                TransferResponse secondResponse = transferService.transferMoney(
                                transferRequest,
                                idempotencyKey);

                assertEquals(
                                firstResponse.getTransferId(),
                                secondResponse.getTransferId());

                verify(transferExecutionService, never())
                                .executeTransfer(any(), any(), any());
        }

        @Test
        void duplicateKeyConflictShouldReturnExistingTransfer() {

                when(transferRequestHasher.hash(transferRequest))
                                .thenReturn(requestHash);

                when(idempotencyRepository.findByIdempotencyKey(idempotencyKey))
                                .thenReturn(
                                                Optional.empty(),
                                                Optional.of(new IdempotencyRecord(
                                                                idempotencyKey,
                                                                requestHash,
                                                                savedTransfer)));

                when(transferExecutionService.executeTransfer(
                                transferRequest,
                                idempotencyKey,
                                requestHash))
                                .thenThrow(new org.springframework.dao.DataIntegrityViolationException(
                                                "Duplicate idempotency key"));

                TransferResponse response = transferService.transferMoney(
                                transferRequest,
                                idempotencyKey);

                assertEquals(101L, response.getTransferId());
                assertEquals("SUCCESS", response.getStatus());

                verify(transferExecutionService, times(1))
                                .executeTransfer(
                                                transferRequest,
                                                idempotencyKey,
                                                requestHash);
        }
}
