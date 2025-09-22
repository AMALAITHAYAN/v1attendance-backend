package com.example.attendance.controller;

import com.example.attendance.dto.SessionStartRequest;
import com.example.attendance.model.Session;
import com.example.attendance.model.TeacherProfile;
import com.example.attendance.model.User;
import com.example.attendance.model.UserRole;
import com.example.attendance.model.ValidationStep;
import com.example.attendance.model.WifiPolicy;
import com.example.attendance.repository.AttendanceRepository;
import com.example.attendance.repository.SessionRepository;
import com.example.attendance.repository.StudentProfileRepository;
import com.example.attendance.repository.TeacherProfileRepository;
import com.example.attendance.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/sessions")
public class TeacherSessionController {

    private final UserService users;
    private final SessionRepository sessions;
    private final AttendanceRepository attendance;
    private final TeacherProfileRepository teachers;
    private final StudentProfileRepository students; // ✅ used to compute class size

    public TeacherSessionController(
            UserService users,
            SessionRepository sessions,
            AttendanceRepository attendance,
            TeacherProfileRepository teachers,
            StudentProfileRepository students
    ) {
        this.users = users;
        this.sessions = sessions;
        this.attendance = attendance;
        this.teachers = teachers;
        this.students = students;
    }

    /* ------------------- helpers ------------------- */

    /** Auth as TEACHER and load a session that belongs to them. */
    private Session loadOwnedSession(String username, String password, Long id) {
        User t = users.login(username, password)
                .filter(u -> u.getRole() == UserRole.TEACHER)
                .orElseThrow(() -> new IllegalArgumentException("Invalid teacher credentials"));

        Session s = sessions.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        if (!s.getTeacher().getUser().getId().equals(t.getId())) {
            throw new IllegalStateException("This session does not belong to the teacher");
        }
        return s;
    }

    /* ------------------- START ------------------- */

    /**
     * POST /api/teacher/sessions/start
     * Body: SessionStartRequest (matches your React form)
     */
    @PostMapping("/start")
    public ResponseEntity<?> start(
            @RequestHeader("X-Auth-Username") String username,
            @RequestHeader("X-Auth-Password") String password,
            @RequestBody @Valid SessionStartRequest req
    ) {
        // 1) auth as TEACHER
        User teacherUser = users.login(username, password)
                .filter(u -> u.getRole() == UserRole.TEACHER)
                .orElseThrow(() -> new IllegalArgumentException("Invalid teacher credentials"));

        // resolve TeacherProfile (User doesn't have getTeacher())
        TeacherProfile teacher = teachers.findByUser(teacherUser)
                .orElseThrow(() -> new IllegalStateException("Teacher profile not found"));

        // 2) validations (return 409 -> avoids 500s)
        if (!req.getEndTime().isAfter(req.getStartTime())) {
            return ResponseEntity.status(409).body("End time must be after start time");
        }

        boolean wantsWifi = req.getFlow() != null && req.getFlow().contains(ValidationStep.WIFI);
        boolean wantsGeo  = req.getFlow() != null && req.getFlow().contains(ValidationStep.GEO);

        if (wantsWifi) {
            WifiPolicy p = req.getWifiPolicy();
            if (p == WifiPolicy.PUBLIC_IP || p == WifiPolicy.BOTH) {
                if (req.getPublicIp() == null || req.getPublicIp().isBlank()) {
                    return ResponseEntity.status(409).body("PUBLIC_IP/BOTH selected but publicIp is missing");
                }
            }
        }

        if (wantsGeo) {
            if (req.getLatitude() == null || req.getLongitude() == null || req.getRadiusMeters() == null) {
                return ResponseEntity.status(409).body("GEO selected but latitude/longitude/radiusMeters are missing");
            }
        }

        // 3) persist session
        Session s = new Session();
        s.setTeacher(teacher);

        s.setYear(req.getYear().trim());
        s.setDepartment(req.getDepartment().trim());
        s.setClassName(req.getClassName().trim());
        s.setSubject(req.getSubject().trim());

        s.setStartTime(req.getStartTime());
        s.setEndTime(req.getEndTime());
        s.setActive(true);

        s.setWifiPolicy(req.getWifiPolicy());
        s.setNetworkSignature(req.getNetworkSignature());
        s.setTeacherPublicIp(req.getPublicIp());

        s.setLatitude(req.getLatitude());
        s.setLongitude(req.getLongitude());
        // entity expects Double, DTO has Integer -> convert
        s.setRadiusMeters(req.getRadiusMeters() == null ? null : Double.valueOf(req.getRadiusMeters()));

        s.setFlow(req.getFlow());
        s.setQrIntervalSeconds(req.getQrIntervalSeconds() == null ? 5 : req.getQrIntervalSeconds());

        s = sessions.save(s);

        return ResponseEntity.ok(
                Map.of(
                        "id", s.getId(),
                        "startTime", s.getStartTime().toString(),
                        "endTime", s.getEndTime().toString()
                )
        );
    }

    /* ------------------- SUMMARY ------------------- */

    /** GET /api/teacher/sessions/{id}/summary */
    @GetMapping("/{id}/summary")
    public Map<String, Object> summary(
            @RequestHeader("X-Auth-Username") String username,
            @RequestHeader("X-Auth-Password") String password,
            @PathVariable Long id
    ) {
        Session s = loadOwnedSession(username, password, id);

        // ✅ present = successful marks in this session
        long present = attendance.countBySessionAndSuccessIsTrue(s);

        // ✅ total = size of the class roster (Year + Department + ClassName)
        long total = students.countByYearAndDepartmentAndClassName(
                s.getYear(), s.getDepartment(), s.getClassName()
        );

        double pct = total == 0 ? 0.0 : (present * 100.0) / total;

        return Map.of(
                "sessionId", s.getId(),
                "present", present,
                "total", total,
                "percentage", Math.round(pct * 100.0) / 100.0
        );
    }

    /* ------------------- STOP ------------------- */

    /** POST /api/teacher/sessions/{id}/stop */
    @PostMapping("/{id}/stop")
    public ResponseEntity<?> stop(
            @RequestHeader("X-Auth-Username") String username,
            @RequestHeader("X-Auth-Password") String password,
            @PathVariable Long id
    ) {
        Session s = loadOwnedSession(username, password, id);

        if (s.isActive()) {
            s.setActive(false);
            s.setEndTime(LocalDateTime.now());
            sessions.save(s);
        }
        return ResponseEntity.ok(Map.of("message", "Session stopped"));
    }
}
