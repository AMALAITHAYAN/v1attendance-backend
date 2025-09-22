package com.example.attendance.dto.check;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class QrCheckRequest {
    @NotNull
    private Long sessionId;

    @NotBlank
    private String qrToken;

    // getters/setters
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    public String getQrToken() { return qrToken; }
    public void setQrToken(String qrToken) { this.qrToken = qrToken; }
}
