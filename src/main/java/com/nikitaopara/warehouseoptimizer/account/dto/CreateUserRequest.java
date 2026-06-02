package com.nikitaopara.warehouseoptimizer.account.dto;

import com.nikitaopara.warehouseoptimizer.account.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @Email
        @NotBlank
        String email,

        @NotBlank
        @Size(min = 6, max = 100)
        String password,

        @NotBlank
        @Size(min = 1, max = 255)
        String fullName,

        @NotNull
        Role role
) {
}