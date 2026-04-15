package com.docify.service;

import com.docify.model.User;
import com.docify.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService – registration and login logic.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    // ── register ──────────────────────────────────────────────────────

    @Test
    @DisplayName("register: should save a new user with encoded password")
    void register_newUser_success() {
        when(userRepository.findByEmail("alice@docify.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("$hashed$");

        User saved = new User();
        saved.setId(1L);
        saved.setName("Alice");
        saved.setEmail("alice@docify.com");
        saved.setPassword("$hashed$");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        User result = authService.register("Alice", "alice@docify.com", "secret123");

        assertThat(result.getEmail()).isEqualTo("alice@docify.com");
        assertThat(result.getPassword()).isEqualTo("$hashed$");
        verify(passwordEncoder).encode("secret123");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register: should throw if email already exists")
    void register_duplicateEmail_throws() {
        User existing = new User();
        existing.setEmail("alice@docify.com");
        when(userRepository.findByEmail("alice@docify.com")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> authService.register("Alice", "alice@docify.com", "pass"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("already");

        verify(userRepository, never()).save(any());
    }

    // ── login ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("login: valid credentials return user")
    void login_validCredentials_returnsUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("alice@docify.com");
        user.setPassword("$hashed$");

        when(userRepository.findByEmail("alice@docify.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "$hashed$")).thenReturn(true);

        User result = authService.login("alice@docify.com", "secret123");

        assertThat(result.getEmail()).isEqualTo("alice@docify.com");
    }

    @Test
    @DisplayName("login: wrong password throws exception")
    void login_wrongPassword_throws() {
        User user = new User();
        user.setEmail("alice@docify.com");
        user.setPassword("$hashed$");

        when(userRepository.findByEmail("alice@docify.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "$hashed$")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("alice@docify.com", "wrongpass"))
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("login: non-existent email throws exception")
    void login_unknownEmail_throws() {
        when(userRepository.findByEmail("ghost@docify.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("ghost@docify.com", "pass"))
            .isInstanceOf(RuntimeException.class);
    }
}
