// src/main/java/com/example/attendance/service/SessionService.java
package com.example.attendance.service;

import com.example.attendance.dto.SessionStartRequest;
import com.example.attendance.live.LiveSessionHub;
import com.example.attendance.model.Session;
import com.example.attendance.model.TeacherProfile;
import com.example.attendance.model.User;
import com.example.attendance.model.UserRole;
import com.example.attendance.model.ValidationStep;
import com.example.attendance.model.WifiPolicy;
import com.example.attendance.repository.SessionRepository;
import com.example.attendance.repository.TeacherProfileRepository;
import com.example.attendance.util.ClientIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Objects;

@Service
public class SessionService {

    private final UserService userService;
    private final TeacherProfileRepository teacherRepo;
    private final SessionRepository sessions;
    private final LiveSessionHub live;

    public SessionService(UserService userService,
                          TeacherProfileRepository teacherRepo,
                          SessionRepository sessions,
                          LiveSessionHub live) {
        this.userService = userService;
        this.teacherRepo = teacherRepo;
        this.sessions = sessions;
        this.live = live;
    }

    /** Back-compat shim (e.g., tests or older controllers) */
    @Transactional
    public Session startSession(String teacherUsername, String teacherPassword, SessionStartRequest req) {
        return startSession(teacherUsername, teacherPassword, req, null);
    }

    /** Phase-3 aware start with flow + wifi/geo validation */
    @Transactional
    public Session startSession(String teacherUsername,
                                String teacherPassword,
                                SessionStartRequest req,
                                HttpServletRequest http) {

        // 1) Authenticate as TEACHER
        User teacherUser = userService.login(teacherUsername, teacherPassword)
                .filter(u -> u.getRole() == UserRole.TEACHER)
                .orElseThrow(() -> new IllegalArgumentException("Invalid teacher credentials"));

        TeacherProfile teacher = teacherRepo.findByUser(teacherUser)
                .orElseThrow(() -> new IllegalStateException("Teacher profile not found"));

        // 2) Basic time validation
        if (!req.getEndTime().isAfter(req.getStartTime())) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }

        // 3) Conflict check: same class window overlapping
        boolean conflict = sessions.hasTimeConflict(
                req.getYear(), req.getDepartment(), req.getClassName(),
                req.getStartTime(), req.getEndTime()
        );
        if (conflict) {
            throw new IllegalStateException("Conflict: another session for this class overlaps the requested time window");
        }

        // 4) Persist session
        Session s = new Session();
        s.setTeacher(teacher);

        s.setYear(req.getYear());
        s.setDepartment(req.getDepartment());
        s.setClassName(req.getClassName());
        s.setSubject(req.getSubject());

        s.setStartTime(req.getStartTime());
        s.setEndTime(req.getEndTime());
        s.setActive(true);

        // Wi-Fi policy + signatures (enum is already typed)
        WifiPolicy policy = req.getWifiPolicy();
        if (policy != null) {
            s.setWifiPolicy(policy);
        }
        s.setNetworkSignature(req.getNetworkSignature());

        // Teacher public IP: prefer the one React sent; fall back to request-derived IP
        String bodyIp = req.getPublicIp();
        String httpIp = (http == null) ? null : ClientIpUtil.getClientIp(http);
        s.setTeacherPublicIp((bodyIp != null && !bodyIp.isBlank()) ? bodyIp : httpIp);

        // Geofence (DTO gives Double lat/lng, Integer radius → convert to Double)
        s.setLatitude(req.getLatitude());
        s.setLongitude(req.getLongitude());
        s.setRadiusMeters(req.getRadiusMeters() == null ? null : Double.valueOf(req.getRadiusMeters()));

        // Flow (DTO already provides List<ValidationStep>)
        if (req.getFlow() != null && !req.getFlow().isEmpty()) {
            s.setFlow(req.getFlow());
        }
        // QR cadence (default 5s)
        Integer qrInt = req.getQrIntervalSeconds();
        s.setQrIntervalSeconds((qrInt == null || qrInt <= 0) ? 5 : qrInt);

        // Per-session QR secret
        byte[] secret = new byte[32];
        new SecureRandom().nextBytes(secret);
        s.setQrSecret(secret);

        Session saved = sessions.save(s);

        // 5) Notify live dashboard (super admin)
        live.broadcastSessionStarted(saved);

        return saved;
    }
}
