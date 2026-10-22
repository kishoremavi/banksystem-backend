package com.banking.system.service;

import java.util.ArrayList;
import java.util.List;

import com.banking.system.entity.BankAccount;
import com.banking.system.repository.BankAccountRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {

    private final BankAccountRepository bankAccountRepository;
    private final BankAccountService bankAccountService;

    public TransferService(BankAccountRepository bankAccountRepository, BankAccountService bankAccountService) {
        this.bankAccountRepository = bankAccountRepository;
        this.bankAccountService = bankAccountService;
    }

    @Transactional
    private List<BankAccount> lockAccountsInOrder(Long firstAccountId, Long secondAccountId) {

        if (!bankAccountService.getBankAccountById(firstAccountId).isActive()
                || !bankAccountService.getBankAccountById(secondAccountId).isActive()) {
            throw new IllegalArgumentException("One of the accounts is not present");
        }

        List<BankAccount> accounts = new ArrayList<>();
        if (firstAccountId < secondAccountId) {
            accounts.add(bankAccountRepository.findByIdWithPessimisticWriteLock(firstAccountId));
            accounts.add(bankAccountRepository.findByIdWithPessimisticWriteLock(secondAccountId));
        } else {
            accounts.add(bankAccountRepository.findByIdWithPessimisticWriteLock(secondAccountId));
            accounts.add(bankAccountRepository.findByIdWithPessimisticWriteLock(firstAccountId));
        }
        return accounts;
    }

}
