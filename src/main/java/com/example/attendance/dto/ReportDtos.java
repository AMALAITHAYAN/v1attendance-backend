package com.example.attendance.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ReportDtos {

    public record SubjectPercentage(String subject, long present, long total, double percentage) {}

    public record StudentSummary(long present, long total, double percentage,
                                 List<SubjectPercentage> bySubject) {}

    public record SessionBrief(Long id, String subject, String className,
                               LocalDateTime startTime, LocalDateTime endTime,
                               String teacherName) {}

    public record SessionSummary(Long sessionId, long present, long total) {}

    public record AttendanceLog(Long attendanceId, Long sessionId, String subject,
                                LocalDateTime markedAt, boolean success,
                                boolean geoOk, boolean wifiOk, boolean faceOk, boolean qrOk) {}
}
