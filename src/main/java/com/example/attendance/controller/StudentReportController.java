package com.example.attendance.controller;

import com.example.attendance.dto.ReportDtos.*;
import com.example.attendance.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/student/reports")
public class StudentReportController {

    private final ReportService reports;
    public StudentReportController(ReportService reports) { this.reports = reports; }

    @GetMapping("/me/summary")
    public ResponseEntity<StudentSummary> mySummary(
            @RequestHeader("X-Auth-Username") String user,
            @RequestHeader("X-Auth-Password") String pass,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(reports.mySummary(user, pass, from, to));
    }

    @GetMapping("/me/logs")
    public ResponseEntity<List<AttendanceLog>> myLogs(
            @RequestHeader("X-Auth-Username") String user,
            @RequestHeader("X-Auth-Password") String pass,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(reports.myLogs(user, pass, from, to));
    }
}
