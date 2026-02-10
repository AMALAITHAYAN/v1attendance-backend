package com.example.attendance.service;

import com.example.attendance.dto.TeacherCreateRequest;
import com.example.attendance.model.TeacherProfile;
import com.example.attendance.model.User;
import com.example.attendance.model.UserRole;
import com.example.attendance.repository.TeacherProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections; // ✅ ADD THIS

@Service
public class TeacherService {

    private final UserService userService;
    private final TeacherProfileRepository teacherRepo;

    public TeacherService(UserService userService, TeacherProfileRepository teacherRepo) {
        this.userService = userService;
        this.teacherRepo = teacherRepo;
    }

    @Transactional
    public TeacherProfile createTeacher(TeacherCreateRequest req) {

        if (userService.exists(req.getGmailId())) {
            throw new IllegalArgumentException(
                "Username already exists: " + req.getGmailId()
            );
        }

        // Create login user
        User user = userService.createUser(
            req.getGmailId(),
            req.getPassword(),
            UserRole.TEACHER
        );

        // Create teacher profile
        TeacherProfile t = new TeacherProfile();

        t.setUser(user);
        t.setName(req.getName());
        t.setGender(req.getGender());

        // 🚨 TEMP FIX: Disable subjects saving (avoids teacher_subjects table)
        t.setSubjects(Collections.emptyList());

        t.setPhoneNumber(req.getPhoneNumber());
        t.setIdNumber(req.getIdNumber());

        return teacherRepo.save(t);
    }
}
