package com.example.attendance.controller;

import com.example.attendance.dto.StudentCreateRequest;
import com.example.attendance.model.UserRole;
import com.example.attendance.service.StudentService;
import com.example.attendance.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher")
public class TeacherController {
    private final UserService users;
    private final StudentService students;

    public TeacherController(UserService users, StudentService students) {
        this.users = users;
        this.students = students;
    }

    // Only TEACHER can call
    @PostMapping(path = "/students", consumes = {"multipart/form-data"})
    public ResponseEntity<?> createStudent(
            @RequestHeader("X-Auth-Username") String teacherUser,
            @RequestHeader("X-Auth-Password") String teacherPass,
            @RequestPart("data") @Valid StudentCreateRequest req,
            @RequestPart(value = "photo", required = false) MultipartFile photo
    ) throws Exception {
        if (!users.isRole(teacherUser, teacherPass, UserRole.TEACHER)) {
            return ResponseEntity.status(403).body("Forbidden: TEACHER required");
        }
        byte[] photoBytes = (photo != null && !photo.isEmpty()) ? photo.getBytes() : null;
        return ResponseEntity.ok(students.createStudent(req, photoBytes));
    }

    // Bulk upload (.xlsx)
    @PostMapping(path = "/students/bulk-upload", consumes = {"multipart/form-data"})
    public ResponseEntity<?> bulkUpload(
            @RequestHeader("X-Auth-Username") String teacherUser,
            @RequestHeader("X-Auth-Password") String teacherPass,
            @RequestPart("file") MultipartFile file
    ) {
        if (!users.isRole(teacherUser, teacherPass, UserRole.TEACHER)) {
            return ResponseEntity.status(403).body("Forbidden: TEACHER required");
        }
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is required");
        }
        if (!StringUtils.endsWithIgnoreCase(file.getOriginalFilename(), ".xlsx")) {
            return ResponseEntity.badRequest().body("Only .xlsx files are supported");
        }
        Map<String,Object> result = students.bulkUpload(file);
        return ResponseEntity.ok(result);
    }

    // Optional: Download Excel template
    @GetMapping("/students/template")
    public ResponseEntity<byte[]> template() {
        String csv = "username,name,rollNo,className,year,department,password\n"
                + "john.doe@example.com,John Doe,21CS001,CS-A,2,CS,1234\n";
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename("students_template.csv").build());
        headers.add(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8");
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }
}
