package com.nikitaopara.warehouseoptimizer.account.dto;

import com.nikitaopara.warehouseoptimizer.account.model.Role;
import com.nikitaopara.warehouseoptimizer.account.model.Status;
import com.nikitaopara.warehouseoptimizer.account.model.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long id
        , String email
        , String fullName
        , Role role
        , Status status
        , LocalDateTime createdAt
        , LocalDateTime updatedAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
