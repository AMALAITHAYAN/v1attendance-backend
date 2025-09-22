package com.example.attendance.service;

import com.example.attendance.model.StudentProfile;

public interface FaceVerificationService {
    boolean verify(StudentProfile student, byte[] selfieBytes);
}
