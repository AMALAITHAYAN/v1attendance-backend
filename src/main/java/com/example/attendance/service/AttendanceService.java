package com.example.attendance.service;

import com.example.attendance.dto.AttendanceMarkRequest;
import com.example.attendance.dto.ManualMarkRequest;
import com.example.attendance.dto.StudentSessionMeta;
import com.example.attendance.dto.check.GeoCheckRequest;
import com.example.attendance.dto.check.QrCheckRequest;
import com.example.attendance.dto.check.StepResult;
import com.example.attendance.dto.check.WifiCheckRequest;
import com.example.attendance.model.*;
import com.example.attendance.repository.AttendanceRepository;
import com.example.attendance.repository.SessionRepository;
import com.example.attendance.repository.StudentProfileRepository;
import com.example.attendance.util.ClientIpUtil;
import com.example.attendance.util.GeoUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AttendanceService {

    private final UserService userService;
    private final SessionRepository sessions;
    private final StudentProfileRepository students;
    private final AttendanceRepository attendance;
    private final QrTokenService qr;
    private final FaceVerificationService faceVerify;
    private final LiveStreamService liveStream; // ✅ broadcast live events

    public AttendanceService(UserService userService,
                             SessionRepository sessions,
                             StudentProfileRepository students,
                             AttendanceRepository attendance,
                             QrTokenService qr,
                             FaceVerificationService faceVerify,
                             LiveStreamService liveStream) {
        this.userService = userService;
        this.sessions = sessions;
        this.students = students;
        this.attendance = attendance;
        this.qr = qr;
        this.faceVerify = faceVerify;
        this.liveStream = liveStream;
    }

    /* ---------- small helper: read teacher public IP from Session regardless of field name ---------- */
    private String sessionPublicIp(Session s) {
        if (s == null) return null;
        String[] getters = { "getPublicIp", "getTeacherPublicIp", "getDetectedPublicIp", "getWifiPublicIp" };
        for (String g : getters) {
            try {
                Method m = s.getClass().getMethod(g);
                Object v = m.invoke(s);
                if (v instanceof String str && !str.isBlank()) return str;
            } catch (ReflectiveOperationException ignored) { }
        }
        return null;
    }

    /* ------------ Helpers ------------ */
    private Session requireSession(Long id) {
        return sessions.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
    }

    private StudentProfile requireStudent(String username, String password) {
        User u = userService.login(username, password)
                .filter(x -> x.getRole() == UserRole.STUDENT)
                .orElseThrow(() -> new IllegalArgumentException("Invalid student credentials"));
        return students.findByUser(u)
                .orElseThrow(() -> new IllegalStateException("Student profile not found"));
    }

    private long classSize(Session s) {
        return students.countByYearAndDepartmentAndClassName(
                s.getYear(), s.getDepartment(), s.getClassName()
        );
    }

    /* ------------ Flow listing ------------ */
    public List<String> getFlowForSession(Long sessionId) {
        Session s = requireSession(sessionId);
        List<String> out = new ArrayList<>();
        if (s.getFlow() != null) {
            for (ValidationStep step : s.getFlow()) out.add(step.name());
        }
        return out;
    }

    /* ------------ student session meta ------------ */
    public StudentSessionMeta getStudentSessionMeta(Long sessionId) {
        Session s = requireSession(sessionId);
        return StudentSessionMeta.from(s);
    }

    /* ------------ Step checks ------------ */

    public StepResult checkWifi(WifiCheckRequest req) {
        Session s = requireSession(req.getSessionId());
        String teacherIp = sessionPublicIp(s);

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("policy", s.getWifiPolicy() == null ? "NONE" : s.getWifiPolicy().name());
        details.put("teacherPublicIp", teacherIp);
        details.put("studentPublicIp", req.getStudentPublicIp());

        boolean ok = true;
        if (s.getWifiPolicy() == WifiPolicy.PUBLIC_IP || s.getWifiPolicy() == WifiPolicy.BOTH) {
            if (teacherIp == null || teacherIp.isBlank()) {
                return StepResult.fail("Session missing teacher public IP", Map.of("reason", "sessionPublicIpMissing"));
            }
            ok = teacherIp.trim().equals(req.getStudentPublicIp().trim());
        }

        details.put("ok", ok);
        return ok ? StepResult.ok("Wi-Fi OK", details)
                : StepResult.fail("Wi-Fi mismatch", details);
    }

    public StepResult checkGeo(GeoCheckRequest req) {
        Session s = requireSession(req.getSessionId());

        if (s.getLatitude() == null || s.getLongitude() == null || s.getRadiusMeters() == null) {
            return StepResult.fail("Session geofence not configured", Map.of("reason", "sessionGeofenceMissing"));
        }

        double dist = GeoUtil.distanceMeters(
                s.getLatitude(), s.getLongitude(),
                req.getStudentLat(), req.getStudentLng()
        );

        boolean ok = dist <= s.getRadiusMeters();
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("distanceMeters", Math.round(dist));
        details.put("radiusMeters", s.getRadiusMeters());
        details.put("sessionLat", s.getLatitude());
        details.put("sessionLng", s.getLongitude());
        details.put("studentLat", req.getStudentLat());
        details.put("studentLng", req.getStudentLng());
        details.put("ok", ok);

        return ok ? StepResult.ok("Geo OK", details)
                : StepResult.fail("Outside geofence", details);
    }

    public StepResult checkFace(String username, String password, Long sessionId, byte[] selfieBytes) {
        if (selfieBytes == null || selfieBytes.length == 0) {
            return StepResult.fail("Selfie required for face verification", Map.of("reason", "missingSelfie"));
        }
        Session s = requireSession(sessionId);
        if (!s.isActive()) return StepResult.fail("Session not active", Map.of("reason", "inactive"));

        StudentProfile sp = requireStudent(username, password);
        boolean ok = faceVerify.verify(sp, selfieBytes);

        return ok ? StepResult.ok("Face match", Map.of("ok", true))
                : StepResult.fail("Face not match", Map.of("ok", false));
    }

    public StepResult checkQr(String username, String password, QrCheckRequest req) {
        Session s = requireSession(req.getSessionId());
        if (!s.isActive()) return StepResult.fail("Session not active", Map.of("reason", "inactive"));

        StudentProfile sp = requireStudent(username, password);

        long slot;
        try {
            slot = qr.validateToken(s, req.getQrToken(), System.currentTimeMillis());
        } catch (Exception ex) {
            return StepResult.fail("QR invalid or expired", Map.of("reason", "invalidOrExpired"));
        }

        boolean reused = attendance.findFirstBySessionAndStudentAndQrSlot(s, sp, slot).isPresent();
        if (reused) {
            return StepResult.fail("QR token already used", Map.of("slot", slot, "reused", true));
        }

        return StepResult.ok("QR OK", Map.of("slot", slot));
    }

    /* ------------ Final commit (re-validates quickly to protect integrity) ------------ */
    @Transactional
    public Attendance mark(String studentUser,
                           String studentPass,
                           AttendanceMarkRequest req,
                           String clientPublicIpForAudit,
                           byte[] selfieBytes) {

        StudentProfile sp = requireStudent(studentUser, studentPass);
        Session s = requireSession(req.getSessionId());
        if (!s.isActive()) throw new IllegalStateException("Session not active");

        boolean wifiOk = true, geoOk = true, faceOk = true, qrOk = true;
        Double dist = null;
        Long qrSlot = null;

        if (s.getFlow() != null) {
            for (ValidationStep step : s.getFlow()) {
                switch (step) {
                    case WIFI -> {
                        if (s.getWifiPolicy() == WifiPolicy.PUBLIC_IP || s.getWifiPolicy() == WifiPolicy.BOTH) {
                            String teacherIp = sessionPublicIp(s);
                            if (teacherIp == null || req.getPublicIp() == null)
                                throw new IllegalStateException("Missing public IPs");
                            wifiOk = teacherIp.trim().equals(req.getPublicIp().trim());
                            if (!wifiOk) throw new IllegalStateException("Wi-Fi mismatch");
                        }
                    }
                    case GEO -> {
                        if (s.getLatitude() == null || s.getLongitude() == null || s.getRadiusMeters() == null)
                            throw new IllegalStateException("Session geofence not configured");
                        if (req.getStudentLat() == null || req.getStudentLng() == null)
                            throw new IllegalStateException("Missing student lat/lng");
                        dist = GeoUtil.distanceMeters(
                                s.getLatitude(), s.getLongitude(),
                                req.getStudentLat(), req.getStudentLng());
                        geoOk = dist <= s.getRadiusMeters();
                        if (!geoOk) throw new IllegalStateException("Outside geofence");
                    }
                    case FACE -> {
                        if (selfieBytes == null || selfieBytes.length == 0)
                            throw new IllegalStateException("Selfie required");
                        faceOk = faceVerify.verify(sp, selfieBytes);
                        if (!faceOk) throw new IllegalStateException("Face not match");
                    }
                    case QR -> {
                        if (req.getQrToken() == null || req.getQrToken().isBlank())
                            throw new IllegalStateException("QR token missing");
                        long slot = qr.validateToken(s, req.getQrToken(), System.currentTimeMillis());
                        boolean reused = attendance.findFirstBySessionAndStudentAndQrSlot(s, sp, slot).isPresent();
                        if (reused) throw new IllegalStateException("QR token already used");
                        qrSlot = slot;
                        qrOk = true;
                    }
                }
            }
        }

        Attendance a = new Attendance();
        a.setSession(s);
        a.setStudent(sp);
        a.setMarkedAt(LocalDateTime.now());
        a.setClientPublicIp(clientPublicIpForAudit);
        a.setDistanceMeters(dist == null ? 0.0 : dist);
        a.setQrSlot(qrSlot);
        a.setGeoOk(geoOk);
        a.setWifiOk(wifiOk);
        a.setFaceOk(faceOk);
        a.setQrOk(qrOk);
        a.setSuccess(true);

        Attendance saved = attendance.save(a);

        // ✅ broadcast live update
        publishLiveMark(s, sp);

        return saved;
    }

    /* ------------ Manual mark ------------ */
    @Transactional
    public Attendance manualMark(String teacherUser, String teacherPass,
                                 ManualMarkRequest req, HttpServletRequest http) {
        User tu = userService.login(teacherUser, teacherPass)
                .filter(x -> x.getRole() == UserRole.TEACHER)
                .orElseThrow(() -> new IllegalArgumentException("Invalid teacher credentials"));

        Session s = requireSession(req.getSessionId());
        if (!s.isActive()) throw new IllegalStateException("Session not active");
        if (!s.getTeacher().getUser().getId().equals(tu.getId())) {
            throw new IllegalStateException("This session does not belong to the teacher");
        }

        StudentProfile sp = students.findById(req.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        boolean classMatch = Objects.equals(sp.getYear(), s.getYear())
                && Objects.equals(sp.getDepartment(), s.getDepartment())
                && Objects.equals(sp.getClassName(), s.getClassName());
        if (!classMatch) throw new IllegalStateException("Student is not in this class");

        if (attendance.existsBySessionAndStudentAndSuccessIsTrue(s, sp)) {
            throw new IllegalStateException("Student already marked present for this session");
        }

        Attendance a = new Attendance();
        a.setSession(s);
        a.setStudent(sp);
        a.setMarkedAt(LocalDateTime.now());
        a.setClientPublicIp(ClientIpUtil.getClientIp(http));
        a.setGeoOk(false);
        a.setWifiOk(false);
        a.setFaceOk(false);
        a.setQrOk(false);
        a.setManual(true);
        a.setManualReason(req.getReason());
        a.setMarkedBy(tu.getUsername());
        a.setSuccess(true);

        Attendance saved = attendance.save(a);

        // ✅ broadcast live update for manual mark as well
        publishLiveMark(s, sp);

        return saved;
    }

    /* ------------ Live SSE payload ------------ */
    private void publishLiveMark(Session s, StudentProfile sp) {
        try {
            long present = attendance.countBySessionAndSuccessIsTrue(s);
            long total = classSize(s);

            Map<String, Object> payload = Map.of(
                    "sessionId", s.getId(),
                    "present", present,
                    "total", total,
                    "ratio", total > 0 ? (present * 1.0 / total) : 0.0,
                    "at", System.currentTimeMillis(),
                    "student", Map.of(
                            "id", sp.getId(),
                            "rollNo", sp.getRollNo(),
                            "name", sp.getName(),
                            "username", sp.getUser() != null ? sp.getUser().getUsername() : null
                    )
            );

            // frontend listens to `attendance-marked`
            liveStream.send(s.getId(), "attendance-marked", payload);
        } catch (Exception ignored) {
            // best-effort: don't block core flow
        }
    }
}
