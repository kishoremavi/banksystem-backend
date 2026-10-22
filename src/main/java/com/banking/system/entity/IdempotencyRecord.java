package com.banking.system.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "idempotency_records")
@Data
@NoArgsConstructor
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;
    @Column(name = "request_hash", nullable = false)
    private String requestHash;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transfer_id", nullable = false)
    private Transfer transfer;
    private LocalDateTime createdAt = LocalDateTime.now();

    public IdempotencyRecord(String idempotencyKey, String requestHash, Transfer transfer) {
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.transfer = transfer;
    }

}
