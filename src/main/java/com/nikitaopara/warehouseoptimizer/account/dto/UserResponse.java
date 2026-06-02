package com.nikitaopara.warehouseoptimizer.account.dto;

import com.nikitaopara.warehouseoptimizer.account.model.Role;
import com.nikitaopara.warehouseoptimizer.account.model.Status;

import java.time.LocalDateTime;

public record UserResponse(
        Long id
        ,String email
        ,String fullName
        ,Role role
        ,Status status
        ,LocalDateTime createdAt
        ,LocalDateTime updatedAt
) {
}
