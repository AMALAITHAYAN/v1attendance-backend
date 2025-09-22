package com.example.attendance.dto;

import jakarta.validation.constraints.NotNull;

public class AttendanceMarkRequest {
    @NotNull
    private Long sessionId;

    // optional depending on flow
    private Double studentLat;
    private Double studentLng;
    private String qrToken;

    // NEW: student's public IP (from frontend)
    private String publicIp;

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public Double getStudentLat() { return studentLat; }
    public void setStudentLat(Double studentLat) { this.studentLat = studentLat; }

    public Double getStudentLng() { return studentLng; }
    public void setStudentLng(Double studentLng) { this.studentLng = studentLng; }

    public String getQrToken() { return qrToken; }
    public void setQrToken(String qrToken) { this.qrToken = qrToken; }

    public String getPublicIp() { return publicIp; }
    public void setPublicIp(String publicIp) { this.publicIp = publicIp; }
}
