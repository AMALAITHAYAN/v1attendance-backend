package com.example.attendance.controller;

import com.example.attendance.dto.TeacherCreateRequest;
import com.example.attendance.model.UserRole;
import com.example.attendance.service.TeacherService;
import com.example.attendance.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final UserService users;
    private final TeacherService teachers;

    public AdminController(UserService users, TeacherService teachers) {
        this.users = users;
        this.teachers = teachers;
    }

    // Only SUPER_ADMIN can call
    @PostMapping("/teachers")
    public ResponseEntity<?> createTeacher(
            @RequestHeader("X-Auth-Username") String adminUser,
            @RequestHeader("X-Auth-Password") String adminPass,
            @RequestBody @Valid TeacherCreateRequest req
    ) {
        if (!users.isRole(adminUser, adminPass, UserRole.SUPER_ADMIN)) {
            return ResponseEntity.status(403).body("Forbidden: SUPER_ADMIN required");
        }
        return ResponseEntity.ok(teachers.createTeacher(req));
    }
}
