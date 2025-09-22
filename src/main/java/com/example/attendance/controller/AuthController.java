package com.example.attendance.controller;

import com.example.attendance.dto.AuthDtos;
import com.example.attendance.model.User;
import com.example.attendance.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService users;

    public AuthController(UserService users) {
        this.users = users;
    }

    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> login(@RequestBody @Valid AuthDtos.LoginRequest req) {
        Optional<User> u = users.login(req.getUsername(), req.getPassword());

        if (!u.isPresent()) { // Java 8 friendly (instead of Optional.isEmpty)
            Map<String, String> body = new HashMap<>();
            body.put("message", "Invalid credentials");
            return ResponseEntity.status(401).body(body);
        }

        Map<String, String> body = new HashMap<>();
        body.put("message", "Login success");
        body.put("role", u.get().getRole().name()); // "SUPER_ADMIN" | "TEACHER" | "STUDENT"
        return ResponseEntity.ok(body);
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody @Valid AuthDtos.ChangePasswordRequest req) {
        users.changePassword(req.getUsername(), req.getOldPassword(), req.getNewPassword());
        return ResponseEntity.ok("Password changed");
    }
}
