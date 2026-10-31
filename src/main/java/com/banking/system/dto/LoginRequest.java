package com.banking.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
                @NotBlank(message = "Email is required") @Email(message = "Please provide a valid email address") @Size(max = 255, message = "Email cannot exceed 255 characters") String email,

                @NotBlank(message = "Password is required") @Size(min = 5, max = 72, message = "Password must be between 12 and 72 characters") @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^a-zA-Z0-9\\s]).*$", message = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character") String password

) {
}
