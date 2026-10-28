package com.banking.system.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.banking.system.dto.RegisterRequest;
import com.banking.system.entity.BankAccount;
import com.banking.system.entity.User;
import com.banking.system.enums.UserRole;
import com.banking.system.repository.BankAccountRepository;
import com.banking.system.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class RegistrationService {

    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final BankAccountService bankAccountService;

    public RegistrationService(
            UserRepository userRepository,
            BankAccountRepository bankAccountRepository,
            PasswordEncoder passwordEncoder, BankAccountService bankAccountService) {

        this.userRepository = userRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.bankAccountService = bankAccountService;
    }

    @Transactional
    public Long register(RegisterRequest request) {

        // Normalize the email
        String email = request.email().trim().toLowerCase();

        // Validate duplicate online resgistration
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("An online user with this email already exists");
        }

        if (bankAccountRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("A bank account already exists for this email. "
                    + "Account ownership must be verified before online registration.");
        }

        BankAccount bankAccount = bankAccountService.createBankAccount(request.customerName().trim(), email);

        // Hash the password
        String passwordHash = passwordEncoder.encode(request.password());

        User user = new User(email, passwordHash, UserRole.CUSTOMER, bankAccount);

        // Save the online user
        User savedUser = userRepository.save(user);

        return savedUser.getId();

    }
}