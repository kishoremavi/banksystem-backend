package com.banking.system.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.banking.system.dto.TransferRequest;

class TransferRequestHasherTest {

    private final TransferRequestHasher hasher = new TransferRequestHasher();

    @Test
    void identicalRequestsProduceIdenticalHashes() {
        TransferRequest first = request(1L, 2L, "25.00");
        TransferRequest second = request(1L, 2L, "25.00");

        assertEquals(hasher.hash(first), hasher.hash(second));
    }

    @Test
    void differentSourceAccountsProduceDifferentHashes() {
        TransferRequest first = request(1L, 2L, "25.00");
        TransferRequest second = request(9L, 2L, "25.00");

        assertNotEquals(hasher.hash(first), hasher.hash(second));
    }

    @Test
    void differentDestinationAccountsProduceDifferentHashes() {
        TransferRequest first = request(1L, 2L, "25.00");
        TransferRequest second = request(1L, 9L, "25.00");

        assertNotEquals(hasher.hash(first), hasher.hash(second));
    }

    @Test
    void differentAmountsProduceDifferentHashes() {
        TransferRequest first = request(1L, 2L, "25.00");
        TransferRequest second = request(1L, 2L, "40.00");

        assertNotEquals(hasher.hash(first), hasher.hash(second));
    }

    private TransferRequest request(Long sourceAccountId, Long destinationAccountId, String amount) {
        TransferRequest request = new TransferRequest();
        request.setSourceAccountId(sourceAccountId);
        request.setDestinationAccountId(destinationAccountId);
        request.setAmount(new BigDecimal(amount));
        return request;
    }
}
