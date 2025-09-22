// src/main/java/com/example/attendance/controller/TeacherQrController.java
package com.example.attendance.controller;

import com.example.attendance.model.Session;
import com.example.attendance.model.User;
import com.example.attendance.model.UserRole;
import com.example.attendance.repository.SessionRepository;
import com.example.attendance.service.QrTokenService;
import com.example.attendance.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;

@RestController
@RequestMapping("/api/teacher")
public class TeacherQrController {

    private final UserService users;
    private final SessionRepository sessions;
    private final QrTokenService qr;

    public TeacherQrController(UserService users, SessionRepository sessions, QrTokenService qr) {
        this.users = users;
        this.sessions = sessions;
        this.qr = qr;
    }

    /** Helper: auth as TEACHER and ensure the session belongs to them */
    private Session loadOwnedActiveSession(String username, String password, Long sessionId) {
        User t = users.login(username, password)
                .filter(u -> u.getRole() == UserRole.TEACHER)
                .orElseThrow(() -> new IllegalArgumentException("Invalid teacher credentials"));

        Session s = sessions.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        if (s.getTeacher() == null || s.getTeacher().getUser() == null ||
                !s.getTeacher().getUser().getId().equals(t.getId())) {
            throw new IllegalStateException("This session does not belong to the teacher");
        }
        if (!s.isActive()) {
            throw new IllegalStateException("Session not active");
        }
        return s;
    }

    /** Ensure QR fields exist for legacy rows and return a token. */
    private String ensureAndIssueToken(Session s) {
        boolean mutated = false;

        if (s.getQrIntervalSeconds() == null || s.getQrIntervalSeconds() <= 0) {
            s.setQrIntervalSeconds(5); // sane default
            mutated = true;
        }
        if (s.getQrSecret() == null || s.getQrSecret().length == 0) {
            byte[] secret = new byte[32];
            new SecureRandom().nextBytes(secret);
            s.setQrSecret(secret);
            mutated = true;
        }
        if (mutated) {
            sessions.save(s); // persist fixes so future calls are safe
        }

        return qr.currentToken(s, System.currentTimeMillis());
    }

    // Primary route your UI calls
    @GetMapping(value = "/qrs/{sessionId}/current-token", produces = "text/plain")
    @Transactional
    public ResponseEntity<String> currentTokenA(
            @RequestHeader("X-Auth-Username") String user,
            @RequestHeader("X-Auth-Password") String pass,
            @PathVariable Long sessionId
    ) {
        Session s = loadOwnedActiveSession(user, pass, sessionId);
        return ResponseEntity.ok(ensureAndIssueToken(s));
    }

    // Back-compat alias if frontend uses /sessions/{id}/qr-token
    @GetMapping(value = "/sessions/{sessionId}/qr-token", produces = "text/plain")
    @Transactional
    public ResponseEntity<String> currentTokenB(
            @RequestHeader("X-Auth-Username") String user,
            @RequestHeader("X-Auth-Password") String pass,
            @PathVariable Long sessionId
    ) {
        Session s = loadOwnedActiveSession(user, pass, sessionId);
        return ResponseEntity.ok(ensureAndIssueToken(s));
    }
}
