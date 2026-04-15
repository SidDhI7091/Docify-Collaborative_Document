// AuthController.java
package com.docify.controller;

import com.docify.model.User;
import com.docify.service.AuthService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        try {
            User user = authService.register(
                body.get("name"), body.get("email"), body.get("password"));
            return ResponseEntity.ok(Map.of(
                "id", user.getId(), "name", user.getName(), "email", user.getEmail()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // @PostMapping("/login")
    // public ResponseEntity<?> login(@RequestBody Map<String, String> body,
    //                                HttpSession session) {
    //     Optional<User> user = authService.login(body.get("email"), body.get("password"));
    //     if (user.isPresent()) {
    //         session.setAttribute("userId", user.get().getId());
    //         session.setAttribute("userName", user.get().getName());
    //         return ResponseEntity.ok(Map.of(
    //             "id", user.get().getId(),
    //             "name", user.get().getName(),
    //             "email", user.get().getEmail()));
    //     }
    //     return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
    // }
       @PostMapping("/login")
public ResponseEntity<?> login(@RequestBody Map<String, String> body,
                               HttpSession session) {
    try {
        // Call service
        User user = authService.login(body.get("email"), body.get("password"));

        // Store session attributes
        session.setAttribute("userId", user.getId());
        session.setAttribute("userName", user.getName());

        // Return response
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "name", user.getName(),
                "email", user.getEmail()
        ));

    } catch (Exception e) {
        return ResponseEntity.status(401).body(Map.of(
                "error", "Invalid credentials"
        ));
    }
}

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null)
            return ResponseEntity.status(401).body(Map.of("error", "Not logged in"));
        return ResponseEntity.ok(Map.of(
            "id", userId, "name", session.getAttribute("userName")));
    }
}