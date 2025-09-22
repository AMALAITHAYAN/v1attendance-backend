package com.example.attendance.controller;

import com.example.attendance.dto.AttendanceMarkRequest;
import com.example.attendance.dto.StudentSessionMeta;
import com.example.attendance.dto.check.*;
import com.example.attendance.live.LiveSessionService; // NEW
import com.example.attendance.model.Attendance;
import com.example.attendance.service.AttendanceService;
import com.example.attendance.util.ClientIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
public class StudentAttendanceController {

    private final AttendanceService attendance;
    private final LiveSessionService live; // NEW

    public StudentAttendanceController(AttendanceService attendance, LiveSessionService live) { // NEW
        this.attendance = attendance;
        this.live = live; // NEW
    }

    /* ----- Flow for a session ----- */
    @GetMapping("/sessions/{id}/flow")
    public ResponseEntity<List<String>> flow(@PathVariable("id") Long id) {
        return ResponseEntity.ok(attendance.getFlowForSession(id));
    }

    /* ----- Step checks (return 200 OK or 409 with details) ----- */
    @PostMapping("/attendance/check/wifi")
    public ResponseEntity<StepResult> checkWifi(@RequestBody @Valid WifiCheckRequest req) {
        StepResult r = attendance.checkWifi(req);
        return r.isOk() ? ResponseEntity.ok(r) : ResponseEntity.status(409).body(r);
    }

    @PostMapping("/attendance/check/geo")
    public ResponseEntity<StepResult> checkGeo(@RequestBody @Valid GeoCheckRequest req) {
        StepResult r = attendance.checkGeo(req);
        return r.isOk() ? ResponseEntity.ok(r) : ResponseEntity.status(409).body(r);
    }

    @PostMapping(path = "/attendance/check/face", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StepResult> checkFace(
            @RequestHeader("X-Auth-Username") String username,
            @RequestHeader("X-Auth-Password") String password,
            @RequestParam("sessionId") Long sessionId,
            @RequestPart(value = "selfie", required = false) MultipartFile selfie) throws Exception {

        byte[] selfieBytes = (selfie != null && !selfie.isEmpty()) ? selfie.getBytes() : null;
        StepResult r = attendance.checkFace(username, password, sessionId, selfieBytes);
        return r.isOk() ? ResponseEntity.ok(r) : ResponseEntity.status(409).body(r);
    }

    @PostMapping("/attendance/check/qr")
    public ResponseEntity<StepResult> checkQr(
            @RequestHeader("X-Auth-Username") String username,
            @RequestHeader("X-Auth-Password") String password,
            @RequestBody @Valid QrCheckRequest req) {

        StepResult r = attendance.checkQr(username, password, req);
        return r.isOk() ? ResponseEntity.ok(r) : ResponseEntity.status(409).body(r);
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<?> getSessionMeta(@PathVariable("id") Long id) {
        try {
            StudentSessionMeta meta = attendance.getStudentSessionMeta(id);
            if (!meta.active) {
                // inactive/expired session
                return ResponseEntity.status(410).body(
                        Map.of("message", "Session overed")
                );
            }
            return ResponseEntity.ok(meta);
        } catch (IllegalArgumentException ex) {
            // thrown with message "NOT_FOUND" in service
            return ResponseEntity.status(404).body(
                    Map.of("message", "Session not found")
            );
        }
    }

    /* ----- Final commit ----- */
    @PostMapping(
            path = "/attendance/mark",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> mark(
            @RequestHeader("X-Auth-Username") String username,
            @RequestHeader("X-Auth-Password") String password,
            @RequestPart("data") @Valid AttendanceMarkRequest req,
            @RequestPart(value = "selfie", required = false) MultipartFile selfie,
            HttpServletRequest http
    ) throws Exception {

        final byte[] selfieBytes = (selfie != null && !selfie.isEmpty()) ? selfie.getBytes() : null;
        final String clientIp = ClientIpUtil.getClientIp(http); // audit only

        Attendance result = attendance.mark(username, password, req, clientIp, selfieBytes);

        // NEW: broadcast live update to the teacher’s SSE stream for this session
        live.broadcast(result.getSession().getId(), "attendance-marked", new LivePayload(result));

        return ResponseEntity.ok(result);
    }

    // NEW: lightweight payload to avoid JPA graph recursion in SSE
    static class LivePayload {
        public final Long sessionId;
        public final Long attendanceId;
        public final boolean success;
        public final boolean manual;
        public final String markedBy;
        public final String studentName;
        public final String rollNo;
        public final String at;

        LivePayload(Attendance a) {
            this.sessionId = a.getSession().getId();
            this.attendanceId = a.getId();
            this.success = a.isSuccess();
            this.manual = a.isManual();
            this.markedBy = a.getMarkedBy();
            this.studentName = a.getStudent().getName();
            this.rollNo = a.getStudent().getRollNo();
            this.at = a.getMarkedAt().toString();
        }
    }
}
