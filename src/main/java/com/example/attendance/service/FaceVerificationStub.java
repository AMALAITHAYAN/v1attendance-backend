package com.example.attendance.service;

import com.example.attendance.model.StudentProfile;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service

public class FaceVerificationStub implements FaceVerificationService {
    @Override
    public boolean verify(StudentProfile student, byte[] selfieBytes) {
        // TODO: replace with your real face verification (from your zip)
        // For now, accept if selfieBytes present or student has a photo.
        return selfieBytes != null && selfieBytes.length > 0 && student.getPhoto() != null;
    }
}
