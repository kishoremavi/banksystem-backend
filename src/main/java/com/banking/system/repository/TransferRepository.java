package com.banking.system.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.banking.system.entity.Transfer;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

}
