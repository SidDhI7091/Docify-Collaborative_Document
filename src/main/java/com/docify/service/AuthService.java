// AuthService.java
package com.docify.service;

import com.docify.model.User;
import com.docify.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service @RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User register(String name, String email, String password) {
        if (userRepository.existsByEmail(email))
            throw new RuntimeException("Email already registered");
        User user = User.builder()
            .name(name).email(email)
            .password(passwordEncoder.encode(password))
            .build();
        return userRepository.save(user);
    }

    public Optional<User> login(String email, String password) {
        return userRepository.findByEmail(email)
            .filter(u -> passwordEncoder.matches(password, u.getPassword()));
    }
}