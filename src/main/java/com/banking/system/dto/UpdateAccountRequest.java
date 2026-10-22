package com.banking.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UpdateAccountRequest {

    @NotBlank(message = "Customer name is required")
    @Size(min = 3, max = 100, message = "Customer name must be between 3 and 100 characters")
    private String customerName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    @Size(min = 3, max = 100, message = "Email must be between 3 and 100 characters")
    private String email;

    public String getCustomerName() {
        return customerName;
    }

    public String getEmail() {
        return email;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

}
