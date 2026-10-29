package com.banking.system.service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.banking.system.dto.LoginRequest;
import com.banking.system.dto.LoginResponse;
import com.banking.system.entity.User;
import com.banking.system.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {

        // 1. Normalize the email
        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        // 2. Find the user
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid email or password"));

        // 3. Verify the submitted password
        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                user.getPasswordHash());

        if (!passwordMatches) {
            throw new IllegalArgumentException(
                    "Invalid email or password");
        }

        // 4. Generate the JWT
        String jwt = jwtService.generateToken(user);

        // 5. Return the login response
        return new LoginResponse(jwt, "Bearer");
    }
}
