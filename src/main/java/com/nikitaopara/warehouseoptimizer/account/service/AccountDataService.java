package com.nikitaopara.warehouseoptimizer.account.service;

import com.nikitaopara.warehouseoptimizer.account.model.User;
import com.nikitaopara.warehouseoptimizer.account.repository.UserRepository;
import com.nikitaopara.warehouseoptimizer.common.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccountDataService {

    private final UserRepository userRepository;

    public List<User> getAll() {
        return userRepository.findAll();
    }

    public User getUserByIdOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public User getUserByEmailOrThrow(String email) {
        return userRepository.findUserByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findUserByEmail(email);
    }
}
