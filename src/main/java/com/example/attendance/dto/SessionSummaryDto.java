package com.example.attendance.dto;

public class SessionSummaryDto {
    public Long sessionId;
    public long present;
    public long total;
    public double ratio;

    public SessionSummaryDto(Long sessionId, long present, long total) {
        this.sessionId = sessionId;
        this.present = present;
        this.total = total;
        this.ratio = total > 0 ? (present * 1.0 / total) : 0.0;
    }
}
