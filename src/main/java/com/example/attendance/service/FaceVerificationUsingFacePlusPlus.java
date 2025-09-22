package com.example.attendance.service;

import com.example.attendance.face.FaceService;
import com.example.attendance.face.FaceServiceException;
import com.example.attendance.model.StudentProfile;
import com.example.attendance.util.ByteArrayMultipartFile;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Primary
public class FaceVerificationUsingFacePlusPlus implements FaceVerificationService {

    private final FaceService faceService;

    public FaceVerificationUsingFacePlusPlus(FaceService faceService) {
        this.faceService = faceService;
    }

    @Override
    public boolean verify(StudentProfile student, byte[] selfieBytes) {
        try {
            if (selfieBytes == null || selfieBytes.length == 0) return false;

            // Wrap the incoming selfie as MultipartFile and call your 'checkin'
            ByteArrayMultipartFile mf = new ByteArrayMultipartFile(
                    selfieBytes, "selfie", "selfie.jpg", "image/jpeg");

            Map<String,Object> res = faceService.processFace(mf, "checkin");
            Object ok = res.get("success");
            return ok instanceof Boolean && (Boolean) ok;

        } catch (FaceServiceException | java.io.IOException e) {
            return false;
        }
    }
}
