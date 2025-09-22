package com.example.attendance.controller;

import com.example.attendance.dto.ManualMarkRequest;
import com.example.attendance.model.Attendance;
import com.example.attendance.service.AttendanceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teacher/attendance")
public class TeacherAttendanceController {

    private final AttendanceService attendanceService;

    public TeacherAttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/manual")
    public ResponseEntity<?> manualMark(
            @RequestHeader("X-Auth-Username") String username,
            @RequestHeader("X-Auth-Password") String password,
            @RequestBody @Valid ManualMarkRequest req,
            HttpServletRequest http
    ) {
        try {
            Attendance a = attendanceService.manualMark(username, password, req, http);
            return ResponseEntity.ok(a);
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).body(ex.getMessage());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
