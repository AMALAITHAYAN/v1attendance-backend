package com.example.attendance.dto;

import jakarta.validation.constraints.NotNull;

public class ManualMarkRequest {
    @NotNull private Long sessionId;
    @NotNull private Long studentId;
    private String reason; // optional

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
