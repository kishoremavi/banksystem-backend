package com.banking.system.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import com.banking.system.entity.IdempotencyRecord;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, Long> {
    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);

}
