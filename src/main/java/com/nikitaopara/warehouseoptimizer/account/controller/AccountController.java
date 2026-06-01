package com.nikitaopara.warehouseoptimizer.account.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/accounts")
public class AccountController {


    @GetMapping
    public ResponseEntity<String> getUsers() {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok().build();
    }

    @PostMapping
    public ResponseEntity<String> createUser(@RequestBody String request) {
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}")
    public ResponseEntity<String> updateUser(
            @PathVariable Long id,
            @RequestBody String request
    ) {
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<String> activateUser(@PathVariable Long id) {
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<String> deactivateUser(@PathVariable Long id) {
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<String> resetPasswordForAdmin(
            @PathVariable Long id,
            @RequestBody String request
    ) {
        return ResponseEntity.ok().build();
    }
}