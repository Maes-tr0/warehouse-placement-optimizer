package com.nikitaopara.warehouseoptimizer.account.service;

import com.nikitaopara.warehouseoptimizer.account.dto.CreateUserRequest;
import com.nikitaopara.warehouseoptimizer.account.dto.UpdateUserRequest;
import com.nikitaopara.warehouseoptimizer.account.dto.UserResponse;
import com.nikitaopara.warehouseoptimizer.account.model.Status;
import com.nikitaopara.warehouseoptimizer.account.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountDataService accountDataService;
    private final AccountValidationService accountValidationService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponse createAccount(CreateUserRequest request, Authentication authentication) {
        User actor = getAuthenticatedUser(authentication);

        accountValidationService.validateCreateRequest(actor, request);

        User userToSave = User.builder()
                .email(request.email())
                .fullName(request.fullName())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .status(Status.ACTIVE)
                .build();

        User savedUser = accountDataService.save(userToSave);

        return UserResponse.from(savedUser);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAccounts(Authentication authentication) {
        User actor = getAuthenticatedUser(authentication);

        accountValidationService.validateGetAccountsRequest(actor);

        return accountDataService.getAll()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getAccountById(Long userId, Authentication authentication) {
        User actor = getAuthenticatedUser(authentication);
        User targetUser = accountDataService.getUserByIdOrThrow(userId);

        accountValidationService.validateGetAccountRequest(actor, targetUser);

        return UserResponse.from(targetUser);
    }

    @Transactional
    public UserResponse updateAccount(Long userId, UpdateUserRequest request, Authentication authentication) {
        User actor = getAuthenticatedUser(authentication);
        User targetUser = accountDataService.getUserByIdOrThrow(userId);

        accountValidationService.validateUpdateAccountRequest(actor, targetUser, request);

        applyUpdateRequest(targetUser, request);

        User updatedUser = accountDataService.save(targetUser);

        return UserResponse.from(updatedUser);
    }

    @Transactional
    public UserResponse activateAccount(Long userId, Authentication authentication) {
        User actor = getAuthenticatedUser(authentication);
        User targetUser = accountDataService.getUserByIdOrThrow(userId);

        accountValidationService.validateActivateRequest(actor, targetUser);

        targetUser.setStatus(Status.ACTIVE);

        User updatedUser = accountDataService.save(targetUser);

        return UserResponse.from(updatedUser);
    }

    @Transactional
    public UserResponse deactivateAccount(Long userId, Authentication authentication) {
        User actor = getAuthenticatedUser(authentication);
        User targetUser = accountDataService.getUserByIdOrThrow(userId);

        accountValidationService.validateDeactivateRequest(actor, targetUser);

        targetUser.setStatus(Status.INACTIVE);

        User updatedUser = accountDataService.save(targetUser);

        return UserResponse.from(updatedUser);
    }

    private User getAuthenticatedUser(Authentication authentication) {
        return accountDataService.getUserByEmailOrThrow(authentication.getName());
    }

    private void applyUpdateRequest(User targetUser, UpdateUserRequest request) {
        if (request.email() != null) {
            targetUser.setEmail(request.email());
        }

        if (request.fullName() != null) {
            targetUser.setFullName(request.fullName());
        }

        if (request.password() != null) {
            targetUser.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        if (request.role() != null) {
            targetUser.setRole(request.role());
        }
    }
}