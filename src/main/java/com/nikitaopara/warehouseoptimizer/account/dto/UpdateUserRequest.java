package com.nikitaopara.warehouseoptimizer.account.dto;

import com.nikitaopara.warehouseoptimizer.account.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Email
        String email,

        @Size(min = 6, max = 100)
        String password,

        @Size(min = 1, max = 255)
        String fullName,

        Role role
) {
}