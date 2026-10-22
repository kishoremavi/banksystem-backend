package com.banking.system;

import com.banking.system.controller.BankAccountController;
import com.banking.system.exception.GlobalExceptionHandler;
import com.banking.system.service.BankAccountService;
import com.banking.system.dto.UpdateAccountRequest;

import org.junit.jupiter.api.Test;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OptimisticLockingHttpTest {

    @Test
    void optimisticLockingConflictShouldReturnHttp409() throws Exception {

        BankAccountService bankAccountService = mock(BankAccountService.class);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new BankAccountController(bankAccountService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        when(bankAccountService.updateBankAccount(
                eq(123L),
                any(UpdateAccountRequest.class))).thenThrow(
                        new ObjectOptimisticLockingFailureException(
                                "BankAccount",
                                123L));

        mockMvc.perform(put("/api/accounts/123")
                .contentType("application/json")
                .content("""
                        {
                          "customerName": "Updated Customer",
                          "email": "updated@example.com"
                        }
                        """))
                .andExpect(status().isConflict())
                .andExpect(content().string(
                        "Account was modified by another request. Please retry."));

        verify(bankAccountService).updateBankAccount(
                eq(123L),
                any(UpdateAccountRequest.class));
    }
}