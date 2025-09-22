package com.example.attendance.controller;

import com.example.attendance.dto.ReportDtos.*;
import com.example.attendance.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/reports")
public class AdminReportController {

    private final ReportService reports;

    public AdminReportController(ReportService reports) { this.reports = reports; }

    @GetMapping("/active-sessions")
    public ResponseEntity<List<SessionBrief>> activeSessions(
            @RequestHeader("X-Auth-Username") String user,
            @RequestHeader("X-Auth-Password") String pass) {
        return ResponseEntity.ok(reports.adminActiveSessions(user, pass));
    }

    @GetMapping("/students/{studentId}/summary")
    public ResponseEntity<StudentSummary> studentSummary(
            @RequestHeader("X-Auth-Username") String user,
            @RequestHeader("X-Auth-Password") String pass,
            @PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(reports.adminStudentSummary(studentId, from, to, user, pass));
    }
}
