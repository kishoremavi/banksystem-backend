package com.banking.system.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

import com.banking.system.dto.TransferRequest;

@Component
public class TransferRequestHasher {

    public String hash(TransferRequest request) {
        String canonical = request.getSourceAccountId()
                + "|"
                + request.getDestinationAccountId()
                + "|"
                + request.getAmount().toPlainString();

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(canonical.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}