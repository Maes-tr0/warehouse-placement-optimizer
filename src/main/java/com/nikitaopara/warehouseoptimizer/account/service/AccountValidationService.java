package com.nikitaopara.warehouseoptimizer.account.service;

import com.nikitaopara.warehouseoptimizer.account.dto.CreateUserRequest;
import com.nikitaopara.warehouseoptimizer.account.dto.UpdateUserRequest;
import com.nikitaopara.warehouseoptimizer.account.model.Role;
import com.nikitaopara.warehouseoptimizer.account.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AccountValidationService {

    private final AccountDataService accountDataService;

    public void validateCreateRequest(User actor, CreateUserRequest request) {
        validateActor(actor);
        validateCreateRequestData(request);
        validateEmailIsFree(request.email());
        validateCanCreateUser(actor, request.role());
    }

    public void validateActivateRequest(User actor, User targetUser) {
        validateActor(actor);
        validateTargetUser(targetUser);
        validateTargetIsNotRootAdmin(targetUser);
        validateCanManageStatus(actor, targetUser);
    }

    public void validateDeactivateRequest(User actor, User targetUser) {
        validateActor(actor);
        validateTargetUser(targetUser);
        validateTargetIsNotRootAdmin(targetUser);
        validateCanManageStatus(actor, targetUser);
    }

    public void validateGetAccountsRequest(User actor) {
        validateActor(actor);
        validateActorIsAdminOrRootAdmin(actor);
    }

    public void validateGetAccountRequest(User actor, User targetUser) {
        validateActor(actor);
        validateTargetUser(targetUser);

        if (actor.getRole() == Role.ROOT_ADMIN) {
            return;
        }

        if (actor.getRole() == Role.ADMIN && targetUser.getRole() != Role.ROOT_ADMIN) {
            return;
        }

        throw new AccessDeniedException("You are not allowed to view this account");
    }

    public void validateUpdateAccountRequest(User actor, User targetUser, UpdateUserRequest request) {
        validateActor(actor);
        validateTargetUser(targetUser);
        validateUpdateRequestData(request);
        validateTargetIsNotRootAdmin(targetUser);
        validateCanUpdateAccount(actor, targetUser);

        if (request.email() != null) {
            validateEmailIsFreeForUpdate(request.email(), targetUser.getId());
        }

        if (request.role() != null) {
            validateCanChangeRole(actor, targetUser, request.role());
        }

        if (request.password() != null) {
            validateCanChangePassword(actor, targetUser);
        }
    }



    private void validateActor(User actor) {
        if (actor == null) {
            throw new AccessDeniedException("Authenticated user is required");
        }

        if (actor.getRole() == null) {
            throw new AccessDeniedException("Authenticated user has no role");
        }
    }

    private void validateTargetUser(User targetUser) {
        if (targetUser == null) {
            throw new IllegalArgumentException("Target user is required");
        }

        if (targetUser.getRole() == null) {
            throw new IllegalArgumentException("Target user has no role");
        }
    }

    private void validateActorIsAdminOrRootAdmin(User actor) {
        if (actor.getRole() == Role.ROOT_ADMIN || actor.getRole() == Role.ADMIN) {
            return;
        }

        throw new AccessDeniedException("Only administrators can perform this action");
    }

    private void validateCreateRequestData(CreateUserRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Create user request cannot be null");
        }

        if (!StringUtils.hasText(request.email())) {
            throw new IllegalArgumentException("Email is required");
        }

        if (!StringUtils.hasText(request.password())) {
            throw new IllegalArgumentException("Password is required");
        }

        if (!StringUtils.hasText(request.fullName())) {
            throw new IllegalArgumentException("Full name is required");
        }

        if (request.role() == null) {
            throw new IllegalArgumentException("Role is required");
        }

        if (request.role() == Role.ROOT_ADMIN) {
            throw new IllegalArgumentException("ROOT_ADMIN cannot be created manually");
        }
    }

    private void validateUpdateRequestData(UpdateUserRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Update user request cannot be null");
        }

        boolean noFieldsProvided =
                request.email() == null &&
                        request.password() == null &&
                        request.fullName() == null &&
                        request.role() == null;

        if (noFieldsProvided) {
            throw new IllegalArgumentException("At least one field must be provided for update");
        }

        if (request.email() != null && !StringUtils.hasText(request.email())) {
            throw new IllegalArgumentException("Email cannot be blank");
        }

        if (request.password() != null && !StringUtils.hasText(request.password())) {
            throw new IllegalArgumentException("Password cannot be blank");
        }

        if (request.fullName() != null && !StringUtils.hasText(request.fullName())) {
            throw new IllegalArgumentException("Full name cannot be blank");
        }

        if (request.role() == Role.ROOT_ADMIN) {
            throw new IllegalArgumentException("Role cannot be changed to ROOT_ADMIN");
        }
    }

    private void validateEmailIsFree(String email) {
        if (accountDataService.existsByEmail(email)) {
            throw new IllegalArgumentException("User with this email already exists");
        }
    }

    private void validateEmailIsFreeForUpdate(String email, Long targetUserId) {
        accountDataService.getUserByEmail(email)
                .filter(existingUser -> !existingUser.getId().equals(targetUserId))
                .ifPresent(existingUser -> {
                    throw new IllegalArgumentException("User with this email already exists");
                });
    }

    private void validateCanCreateUser(User actor, Role targetRole) {
        if (actor.getRole() == Role.ROOT_ADMIN) {
            return;
        }

        if (actor.getRole() == Role.ADMIN && targetRole == Role.OPERATOR) {
            return;
        }

        throw new AccessDeniedException("You are not allowed to create user with role: " + targetRole);
    }

    private void validateCanUpdateAccount(User actor, User targetUser) {
        if (actor.getRole() == Role.ROOT_ADMIN) {
            return;
        }

        if (actor.getRole() == Role.ADMIN && targetUser.getRole() == Role.OPERATOR) {
            return;
        }

        throw new AccessDeniedException("You are not allowed to update this account");
    }

    private void validateCanChangePassword(User actor, User targetUser) {
        if (actor.getRole() == Role.ROOT_ADMIN) {
            return;
        }

        if (actor.getRole() == Role.ADMIN && targetUser.getRole() == Role.OPERATOR) {
            return;
        }

        throw new AccessDeniedException("You are not allowed to change password for this account");
    }

    private void validateCanChangeRole(User actor, User targetUser, Role newRole) {
        if (newRole == Role.ROOT_ADMIN) {
            throw new AccessDeniedException("Cannot assign ROOT_ADMIN role");
        }

        if (actor.getRole() == Role.ROOT_ADMIN) {
            return;
        }

        throw new AccessDeniedException("Only ROOT_ADMIN can change user roles");
    }

    private void validateCanManageStatus(User actor, User targetUser) {
        if (actor.getRole() == Role.ROOT_ADMIN) {
            return;
        }

        if (actor.getRole() == Role.ADMIN && targetUser.getRole() == Role.OPERATOR) {
            return;
        }

        throw new AccessDeniedException("You are not allowed to change status for this account");
    }

    private void validateTargetIsNotRootAdmin(User targetUser) {
        if (targetUser.getRole() == Role.ROOT_ADMIN) {
            throw new AccessDeniedException("ROOT_ADMIN account cannot be modified");
        }
    }
}