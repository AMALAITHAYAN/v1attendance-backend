package com.example.attendance.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "attendance",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_session_student_success",
                        columnNames = {"session_id", "student_id", "success"}
                )
        },
        indexes = {
                @Index(name = "idx_attendance_session", columnList = "session_id")
        }
)
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @ManyToOne(optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private StudentProfile student;

    private LocalDateTime markedAt;

    private String clientPublicIp;
    private Double distanceMeters;
    private Long qrSlot;

    private boolean geoOk;
    private boolean wifiOk;
    private boolean faceOk;
    private boolean qrOk;

    @Column(nullable = false)
    private boolean success; // true if all checks passed

    // --- Manual mark audit (Phase 4 mini feature) ---
    @Column(nullable = false)
    private boolean manual = false;

    @Column(length = 255)
    private String manualReason;

    @Column(length = 128)
    private String markedBy; // teacher username or name

    // ---- getters/setters ----
    public Long getId() { return id; }

    public Session getSession() { return session; }
    public void setSession(Session session) { this.session = session; }

    public StudentProfile getStudent() { return student; }
    public void setStudent(StudentProfile student) { this.student = student; }

    public LocalDateTime getMarkedAt() { return markedAt; }
    public void setMarkedAt(LocalDateTime markedAt) { this.markedAt = markedAt; }

    public String getClientPublicIp() { return clientPublicIp; }
    public void setClientPublicIp(String clientPublicIp) { this.clientPublicIp = clientPublicIp; }

    public Double getDistanceMeters() { return distanceMeters; }
    public void setDistanceMeters(Double distanceMeters) { this.distanceMeters = distanceMeters; }

    public Long getQrSlot() { return qrSlot; }
    public void setQrSlot(Long qrSlot) { this.qrSlot = qrSlot; }

    public boolean isGeoOk() { return geoOk; }
    public void setGeoOk(boolean geoOk) { this.geoOk = geoOk; }

    public boolean isWifiOk() { return wifiOk; }
    public void setWifiOk(boolean wifiOk) { this.wifiOk = wifiOk; }

    public boolean isFaceOk() { return faceOk; }
    public void setFaceOk(boolean faceOk) { this.faceOk = faceOk; }

    public boolean isQrOk() { return qrOk; }
    public void setQrOk(boolean qrOk) { this.qrOk = qrOk; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public boolean isManual() { return manual; }
    public void setManual(boolean manual) { this.manual = manual; }

    public String getManualReason() { return manualReason; }
    public void setManualReason(String manualReason) { this.manualReason = manualReason; }

    public String getMarkedBy() { return markedBy; }
    public void setMarkedBy(String markedBy) { this.markedBy = markedBy; }
}
