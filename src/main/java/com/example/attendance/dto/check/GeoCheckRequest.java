package com.example.attendance.dto.check;

import jakarta.validation.constraints.NotNull;

public class GeoCheckRequest {
    @NotNull
    private Long sessionId;

    @NotNull
    private Double studentLat;

    @NotNull
    private Double studentLng;

    // getters/setters
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public Double getStudentLat() { return studentLat; }
    public void setStudentLat(Double studentLat) { this.studentLat = studentLat; }
    public Double getStudentLng() { return studentLng; }
    public void setStudentLng(Double studentLng) { this.studentLng = studentLng; }
}
