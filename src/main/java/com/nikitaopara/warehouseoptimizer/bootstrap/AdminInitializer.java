package com.nikitaopara.warehouseoptimizer.bootstrap;

import com.nikitaopara.warehouseoptimizer.user.Role;
import com.nikitaopara.warehouseoptimizer.user.Status;
import com.nikitaopara.warehouseoptimizer.user.User;
import com.nikitaopara.warehouseoptimizer.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.full-name}")
    private String adminFullName;

    @Override
    @NullMarked
    public void run(String... args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }

        User admin = User.builder()
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .fullName(adminFullName)
                .role(Role.ADMIN)
                .status(Status.ACTIVE)
                .build();

        userRepository.save(admin);
    }
}