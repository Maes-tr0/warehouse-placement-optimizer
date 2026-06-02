package com.nikitaopara.warehouseoptimizer.account.controller;

import com.nikitaopara.warehouseoptimizer.account.dto.CreateUserRequest;
import com.nikitaopara.warehouseoptimizer.account.dto.UpdateUserRequest;
import com.nikitaopara.warehouseoptimizer.account.dto.UserResponse;
import com.nikitaopara.warehouseoptimizer.account.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAccounts(Authentication authentication) {
        List<UserResponse> accounts = accountService.getAccounts(authentication);

        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getAccountById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        UserResponse response = accountService.getAccountById(id, authentication);

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<UserResponse> createAccount(
            @Valid @RequestBody CreateUserRequest request,
            Authentication authentication
    ) {
        UserResponse response = accountService.createAccount(request, authentication);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> updateAccount(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication
    ) {
        UserResponse response = accountService.updateAccount(id, request, authentication);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<UserResponse> activateAccount(
            @PathVariable Long id,
            Authentication authentication
    ) {
        UserResponse response = accountService.activateAccount(id, authentication);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<UserResponse> deactivateAccount(
            @PathVariable Long id,
            Authentication authentication
    ) {
        UserResponse response = accountService.deactivateAccount(id, authentication);

        return ResponseEntity.ok(response);
    }
}