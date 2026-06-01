package com.nikitaopara.warehouseoptimizer.account.repository;

import com.nikitaopara.warehouseoptimizer.account.model.Role;
import com.nikitaopara.warehouseoptimizer.account.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findUserByEmail(String email);

    boolean existsByRole(Role role);
}
