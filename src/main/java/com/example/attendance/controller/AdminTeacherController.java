// src/main/java/com/example/attendance/controller/AdminTeacherController.java
package com.example.attendance.controller;

import com.example.attendance.dto.AdminDtos.TeacherRow;
import com.example.attendance.model.TeacherProfile;
import com.example.attendance.model.User;
import com.example.attendance.model.UserRole;
import com.example.attendance.repository.TeacherProfileRepository;
import com.example.attendance.service.UserService;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminTeacherController {

    private final UserService users;
    private final TeacherProfileRepository teachers;

    public AdminTeacherController(UserService users, TeacherProfileRepository teachers) {
        this.users = users;
        this.teachers = teachers;
    }

    @GetMapping("/teachers")
    public ResponseEntity<Page<TeacherRow>> listTeachers(
            @RequestHeader("X-Auth-Username") String user,
            @RequestHeader("X-Auth-Password") String pass,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String q
    ) {
        User admin = users.login(user, pass)
                .orElseThrow(() -> new IllegalArgumentException("Invalid admin credentials"));
        if (admin.getRole() != UserRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("Forbidden: requires SUPER_ADMIN");
        }

        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                Math.min(100, Math.max(1, size)),
                Sort.by(Sort.Direction.DESC, "id")
        );

        Page<TeacherProfile> pg = (q == null || q.isBlank())
                ? teachers.findAll(pageable)
                : teachers.search(q.trim().toLowerCase(), pageable);

        Page<TeacherRow> mapped = pg.map(t -> {
            String email = (t.getUser() != null && t.getUser().getUsername() != null)
                    ? t.getUser().getUsername()
                    : "";
            String subjects = (t.getSubjects() == null) ? "" : String.join(", ", t.getSubjects());
            return new TeacherRow(t.getId(), t.getName(), email, subjects);
        });
        return ResponseEntity.ok(mapped);
    }
}
