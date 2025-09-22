package com.example.attendance.dto.check;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class WifiCheckRequest {
    @NotNull
    private Long sessionId;

    @NotBlank
    private String studentPublicIp;

    // getters/setters
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public String getStudentPublicIp() { return studentPublicIp; }
    public void setStudentPublicIp(String studentPublicIp) { this.studentPublicIp = studentPublicIp; }
}
