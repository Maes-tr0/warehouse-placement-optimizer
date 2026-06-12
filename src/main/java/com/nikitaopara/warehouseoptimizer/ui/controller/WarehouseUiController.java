package com.nikitaopara.warehouseoptimizer.ui.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WarehouseUiController {

    @GetMapping("/login")
    public String login(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/";
        }
        return "login";
    }

    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        boolean administrator = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ROOT_ADMIN")
                        || authority.getAuthority().equals("ROLE_ADMIN"));

        return administrator
                ? "redirect:/app/admin/dashboard"
                : "redirect:/app/operator/dashboard";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }

    @GetMapping("/app/admin/dashboard")
    public String adminDashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/app/admin/warehouse")
    public String adminWarehouse() {
        return "admin/warehouse";
    }

    @GetMapping("/app/admin/inventory")
    public String adminInventory() {
        return "admin/inventory";
    }

    @GetMapping("/app/admin/demand")
    public String adminDemand() {
        return "admin/demand";
    }

    @GetMapping("/app/admin/optimization")
    public String adminOptimization() {
        return "admin/optimization";
    }

    @GetMapping("/app/admin/accounts")
    public String adminAccounts() {
        return "admin/accounts";
    }

    @GetMapping("/app/admin/movements")
    public String adminMovements() {
        return "admin/movements";
    }

    @GetMapping("/app/admin/audit")
    public String adminAudit() {
        return "admin/audit";
    }

    @GetMapping("/app/operator/dashboard")
    public String operatorDashboard() {
        return "operator/dashboard";
    }

    @GetMapping("/app/operator/receiving")
    public String operatorReceiving() {
        return "operator/receiving";
    }

    @GetMapping("/app/operator/placement")
    public String operatorPlacement() {
        return "operator/placement";
    }

    @GetMapping("/app/operator/relocation")
    public String operatorRelocation() {
        return "operator/relocation";
    }

    @GetMapping("/app/operator/inventory")
    public String operatorInventory() {
        return "operator/inventory";
    }
}
