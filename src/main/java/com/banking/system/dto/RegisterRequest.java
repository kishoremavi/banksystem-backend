package com.banking.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

                @NotBlank(message = "Customer name is required") @Size(max = 100, message = "Customer name cannot exceed 100 characters") String customerName,

                @NotBlank(message = "Email is required") @Email(message = "Please provide a valid email address") @Size(max = 255, message = "Email cannot exceed 255 characters") String email,

                @NotBlank(message = "Password is required") @Size(min = 5, max = 72, message = "Password must be between 12 and 72 characters") String password

) {
}
