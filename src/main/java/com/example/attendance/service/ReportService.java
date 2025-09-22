package com.example.attendance.service;

import com.example.attendance.dto.ReportDtos.*;
import com.example.attendance.model.Session;
import com.example.attendance.model.StudentProfile;
import com.example.attendance.model.TeacherProfile;
import com.example.attendance.model.User;
import com.example.attendance.model.UserRole;
import com.example.attendance.repository.AttendanceRepository;
import com.example.attendance.repository.SessionRepository;
import com.example.attendance.repository.StudentProfileRepository;
import com.example.attendance.repository.TeacherProfileRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final UserService users;
    private final StudentProfileRepository students;
    private final TeacherProfileRepository teachers;
    private final SessionRepository sessions;
    private final AttendanceRepository attendance;

    public ReportService(UserService users,
                         StudentProfileRepository students,
                         TeacherProfileRepository teachers,
                         SessionRepository sessions,
                         AttendanceRepository attendance) {
        this.users = users;
        this.students = students;
        this.teachers = teachers;
        this.sessions = sessions;
        this.attendance = attendance;
    }

    // ---------- Super Admin ----------

    public List<SessionBrief> adminActiveSessions(String adminUser, String adminPass) {
        User admin = users.login(adminUser, adminPass)
                .orElseThrow(() -> new IllegalArgumentException("Invalid admin credentials"));
        if (admin.getRole() != UserRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("Forbidden: requires SUPER_ADMIN");
        }

        return sessions.findByActiveTrueOrderByStartTimeDesc().stream()
                .map(s -> new SessionBrief(
                        s.getId(),
                        s.getSubject(),
                        s.getClassName(),
                        s.getStartTime(),
                        s.getEndTime(),
                        s.getTeacher().getName()))
                .collect(Collectors.toList());
    }

    public StudentSummary adminStudentSummary(Long studentId, LocalDateTime from, LocalDateTime to,
                                              String adminUser, String adminPass) {
        User admin = users.login(adminUser, adminPass)
                .orElseThrow(() -> new IllegalArgumentException("Invalid admin credentials"));
        if (admin.getRole() != UserRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("Forbidden: requires SUPER_ADMIN");
        }

        StudentProfile sp = students.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        return computeStudentSummary(sp, from, to);
    }

    // ---------- Teacher ----------

    public List<SessionBrief> teacherSessions(String teacherUser, String teacherPass,
                                              LocalDateTime from, LocalDateTime to) {
        User u = users.login(teacherUser, teacherPass)
                .filter(x -> x.getRole() == UserRole.TEACHER)
                .orElseThrow(() -> new IllegalArgumentException("Invalid teacher credentials"));

        TeacherProfile tp = teachers.findByUser(u)
                .orElseThrow(() -> new IllegalStateException("Teacher profile not found"));

        return sessions.findByTeacherAndRange(tp, from, to).stream()
                .map(s -> new SessionBrief(
                        s.getId(),
                        s.getSubject(),
                        s.getClassName(),
                        s.getStartTime(),
                        s.getEndTime(),
                        tp.getName()))
                .collect(Collectors.toList());
    }

    public SessionSummary teacherSessionSummary(String teacherUser, String teacherPass, Long sessionId) {
        User u = users.login(teacherUser, teacherPass)
                .filter(x -> x.getRole() == UserRole.TEACHER)
                .orElseThrow(() -> new IllegalArgumentException("Invalid teacher credentials"));

        // (Optional) verify session belongs to this teacher
        Optional<Session> opt = sessions.findById(sessionId);
        if (opt.isEmpty()) throw new IllegalArgumentException("Session not found");
        Session s = opt.get();
        if (!Objects.equals(s.getTeacher().getUser().getId(), u.getId())) {
            throw new IllegalArgumentException("Forbidden: session not owned by teacher");
        }

        long present = attendance.countPresentForSession(sessionId);
        long total = attendance.countMarksForSession(sessionId); // all marks (success or not)
        return new SessionSummary(sessionId, present, total);
    }

    // ---------- Student ----------

    public StudentSummary mySummary(String studentUser, String studentPass,
                                    LocalDateTime from, LocalDateTime to) {
        User u = users.login(studentUser, studentPass)
                .filter(x -> x.getRole() == UserRole.STUDENT)
                .orElseThrow(() -> new IllegalArgumentException("Invalid student credentials"));

        StudentProfile sp = students.findByUser(u)
                .orElseThrow(() -> new IllegalStateException("Student profile not found"));

        return computeStudentSummary(sp, from, to);
    }

    public List<AttendanceLog> myLogs(String studentUser, String studentPass,
                                      LocalDateTime from, LocalDateTime to) {
        User u = users.login(studentUser, studentPass)
                .filter(x -> x.getRole() == UserRole.STUDENT)
                .orElseThrow(() -> new IllegalArgumentException("Invalid student credentials"));

        StudentProfile sp = students.findByUser(u)
                .orElseThrow(() -> new IllegalStateException("Student profile not found"));

        return attendance
                .findByStudentIdAndMarkedAtBetweenOrderByMarkedAtDesc(sp.getId(), from, to)
                .stream()
                .map(a -> new AttendanceLog(
                        a.getId(),
                        a.getSession().getId(),
                        a.getSession().getSubject(),
                        a.getMarkedAt(),
                        a.isSuccess(),
                        a.isGeoOk(),
                        a.isWifiOk(),
                        a.isFaceOk(),
                        a.isQrOk()))
                .collect(Collectors.toList());
    }

    // ---------- Internal helpers ----------

    private StudentSummary computeStudentSummary(StudentProfile sp,
                                                 LocalDateTime from,
                                                 LocalDateTime to) {
        long total = sessions.countClassSessions(sp.getYear(), sp.getDepartment(), sp.getClassName(), from, to);
        long present = attendance.countStudentPresent(sp.getId(), from, to);

        Map<String, Long> totalsBySubj = new HashMap<>();
        sessions.countClassSessionsBySubject(sp.getYear(), sp.getDepartment(), sp.getClassName(), from, to)
                .forEach(p -> totalsBySubj.put(p.getSubject(), p.getTotal()));

        Map<String, Long> presentBySubj = new HashMap<>();
        attendance.countStudentPresentBySubject(sp.getId(), from, to)
                .forEach(p -> presentBySubj.put(p.getSubject(), p.getPresent()));

        List<SubjectPercentage> bySubject = totalsBySubj.entrySet().stream()
                .map(e -> {
                    long t = e.getValue();
                    long p = presentBySubj.getOrDefault(e.getKey(), 0L);
                    double pct = t == 0 ? 0.0 : (p * 100.0) / t;
                    return new SubjectPercentage(e.getKey(), p, t, round2(pct));
                })
                .sorted(Comparator.comparing(SubjectPercentage::subject))
                .collect(Collectors.toList());

        double pct = total == 0 ? 0.0 : (present * 100.0) / total;
        return new StudentSummary(present, total, round2(pct), bySubject);
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
