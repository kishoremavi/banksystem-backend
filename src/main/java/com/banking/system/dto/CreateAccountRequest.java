package com.banking.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateAccountRequest {

    @NotBlank
    @Size(max = 100)
    private String customerName;

    @NotBlank
    @Email(message = "Invalid email address")
    @Size(max = 100)
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
