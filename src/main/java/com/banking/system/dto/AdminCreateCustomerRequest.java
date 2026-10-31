package com.banking.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminCreateCustomerRequest(

        @NotBlank(message = "Customer name is required") @Size(max = 100) String customerName,

        @NotBlank(message = "Email is required") @Email(message = "Invalid email") @Size(max = 255) String email,

        @NotBlank(message = "Password is required") @Size(min = 5, max = 72) String password

) {
}
