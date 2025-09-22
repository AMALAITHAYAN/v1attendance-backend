package com.example.attendance.controller;

import com.example.attendance.dto.ReportDtos.*;
import com.example.attendance.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/teacher/reports")
public class TeacherReportController {

    private final ReportService reports;
    public TeacherReportController(ReportService reports) { this.reports = reports; }

    @GetMapping("/sessions")
    public ResponseEntity<List<SessionBrief>> mySessions(
            @RequestHeader("X-Auth-Username") String user,
            @RequestHeader("X-Auth-Password") String pass,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(reports.teacherSessions(user, pass, from, to));
    }

    @GetMapping("/sessions/{sessionId}/summary")
    public ResponseEntity<SessionSummary> sessionSummary(
            @RequestHeader("X-Auth-Username") String user,
            @RequestHeader("X-Auth-Password") String pass,
            @PathVariable Long sessionId) {
        return ResponseEntity.ok(reports.teacherSessionSummary(user, pass, sessionId));
    }
}
