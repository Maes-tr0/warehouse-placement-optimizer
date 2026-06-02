package com.nikitaopara.warehouseoptimizer.bootstrap;

import com.nikitaopara.warehouseoptimizer.account.model.Role;
import com.nikitaopara.warehouseoptimizer.account.model.Status;
import com.nikitaopara.warehouseoptimizer.account.model.User;
import com.nikitaopara.warehouseoptimizer.account.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RootAdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.full-name}")
    private String adminFullName;

    @Override
    public void run(String... args) {
        if (userRepository.existsByRole(Role.ROOT_ADMIN)) {
            return;
        }

        userRepository.findUserByEmail(adminEmail).ifPresentOrElse(existingUser -> {
            existingUser.setRole(Role.ROOT_ADMIN);
            existingUser.setStatus(Status.ACTIVE);
            existingUser.setPasswordHash(passwordEncoder.encode(adminPassword));
            existingUser.setFullName(adminFullName);

            userRepository.save(existingUser);
        }, () -> {
            User rootAdmin = User.builder()
                    .email(adminEmail)
                    .passwordHash(passwordEncoder.encode(adminPassword))
                    .fullName(adminFullName)
                    .role(Role.ROOT_ADMIN)
                    .status(Status.ACTIVE)
                    .build();

            userRepository.save(rootAdmin);
        });
    }
}