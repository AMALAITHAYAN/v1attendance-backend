package com.example.attendance.controller;

import com.example.attendance.face.FaceService;
import com.example.attendance.model.StudentProfile;
import com.example.attendance.model.UserRole;
import com.example.attendance.repository.StudentProfileRepository;
import com.example.attendance.service.UserService;
import com.example.attendance.util.ByteArrayMultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/students")
public class StudentFaceAdminController {

    private final UserService users;
    private final StudentProfileRepository students;
    private final FaceService faceService;

    public StudentFaceAdminController(UserService users, StudentProfileRepository students, FaceService faceService) {
        this.users = users;
        this.students = students;
        this.faceService = faceService;
    }

    @PostMapping("/{id}/face/register")
    public ResponseEntity<?> registerFace(
            @RequestHeader("X-Auth-Username") String adminUser,
            @RequestHeader("X-Auth-Password") String adminPass,
            @PathVariable Long id
    ) throws Exception {
        if (!users.isRole(adminUser, adminPass, UserRole.SUPER_ADMIN))
            return ResponseEntity.status(403).body("Forbidden");

        StudentProfile sp = students.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        if (sp.getPhoto() == null || sp.getPhoto().length == 0)
            return ResponseEntity.badRequest().body("Student has no stored photo");

        // photo is Base64 in your JSON — decode to bytes
        byte[] bytes = Base64.getDecoder().decode(sp.getPhoto());
        ByteArrayMultipartFile mf = new ByteArrayMultipartFile(bytes, "photo", "photo.jpg", "image/jpeg");
        Map<String,Object> res = faceService.processFace(mf, "register");
        return ResponseEntity.ok(res);
    }
}
