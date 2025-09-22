package com.example.attendance.controller;

import com.example.attendance.dto.ClassCreateRequest;
import com.example.attendance.dto.StudentCreateRequest;
import com.example.attendance.model.ClassRoom;
import com.example.attendance.repository.ClassRoomRepository;
import com.example.attendance.repository.StudentProfileRepository;
import com.example.attendance.service.StudentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.attendance.dto.StudentCreateInClassRequest; // NEW

import java.util.List;

@RestController
@RequestMapping("/api/teacher/classes")
public class TeacherClassController {

    private final ClassRoomRepository classes;
    private final StudentProfileRepository students;
    private final StudentService studentService;

    public TeacherClassController(ClassRoomRepository classes,
                                  StudentProfileRepository students,
                                  StudentService studentService) {
        this.classes = classes;
        this.students = students;
        this.studentService = studentService;
    }

    /** Create (or fetch existing) class using YEAR/DEPARTMENT/SECTION/BLOCK/NAME */
    @PostMapping
    public ResponseEntity<ClassRoom> createClass(@RequestBody @Valid ClassCreateRequest req) {
        ClassRoom cr = classes.findByYearAndDepartmentAndSectionAndBlockAndName(
                req.getYear(), req.getDepartment(), req.getSection(), req.getBlock(), req.getName()
        ).orElseGet(() -> {
            ClassRoom c = new ClassRoom();
            c.setYear(req.getYear());
            c.setDepartment(req.getDepartment());
            c.setSection(req.getSection());
            c.setBlock(req.getBlock());
            c.setName(req.getName());
            return classes.save(c);
        });
        return ResponseEntity.ok(cr);
    }

    /** List all classes (scope to teacher later if you add auth) */
    @GetMapping
    public ResponseEntity<List<ClassRoom>> listClasses() {
        return ResponseEntity.ok(classes.findAll());
    }

    /** Add a single student to a class (photo optional). */
    @PostMapping(path = "/{classId}/students", consumes = {"multipart/form-data"})
    public ResponseEntity<?> addStudentToClass(@PathVariable Long classId,
                                               @RequestPart("data") @Valid StudentCreateInClassRequest data, // CHANGED
                                               @RequestPart(value = "photo", required = false) MultipartFile photo) throws Exception {
        ClassRoom cr = classes.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Class not found: " + classId));

        // Build the legacy DTO that the service already expects
        StudentCreateRequest full = new StudentCreateRequest();
        full.setUsername(data.getUsername());
        full.setName(data.getName());
        full.setRollNo(data.getRollNo());
        full.setPassword((data.getPassword() == null || data.getPassword().isBlank()) ? "1234" : data.getPassword());
        // inject class info from the ClassRoom
        full.setYear(cr.getYear());
        full.setDepartment(cr.getDepartment());
        full.setClassName(cr.getName());

        byte[] photoBytes = (photo != null && !photo.isEmpty()) ? photo.getBytes() : null;
        return ResponseEntity.ok(studentService.createStudent(full, photoBytes));
    }

    /** Roster for a class */
    @GetMapping("/{classId}/students")
    public ResponseEntity<?> listStudents(@PathVariable Long classId) {
        ClassRoom cr = classes.findById(classId)
                .orElseThrow(() -> new IllegalArgumentException("Class not found: " + classId));
        return ResponseEntity.ok(
                students.findByYearAndDepartmentAndClassName(cr.getYear(), cr.getDepartment(), cr.getName())
        );
    }
}
