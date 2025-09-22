package com.example.attendance.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(
        name = "sessions",
        indexes = {
                @Index(
                        name = "idx_sessions_class_time_active",
                        columnList = "year,department,class_name,active,startTime,endTime"
                )
        }
)
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Who started it
    @ManyToOne(optional = false)
    @JoinColumn(name = "teacher_id", nullable = false)
    private TeacherProfile teacher;

    // Class identity
    @Column(nullable = false, length = 8)
    private String year;                 // e.g., "1","2","3","4" or "2025"
    @Column(nullable = false, length = 64)
    private String department;           // e.g., "CSE"
    @Column(name = "class_name", nullable = false, length = 64)
    private String className;
    @Column(nullable = false)
    private String subject;

    // Planned time window (used for conflict checks)
    @Column(nullable = false)
    private LocalDateTime startTime;
    @Column(nullable = false)
    private LocalDateTime endTime;

    // ---- Phase 3 additions ----
    // Geofence (optional)
    private Double latitude;
    private Double longitude;
    private Double radiusMeters;

    // Teacher's network info captured at start
    private String teacherPublicIp;      // from request
    private String networkSignature;     // optional teacher-provided string
    @Enumerated(EnumType.STRING)
    private WifiPolicy wifiPolicy = WifiPolicy.PUBLIC_IP;

    // QR settings per session
    private Integer qrIntervalSeconds = 5;   // default 5s
    @Lob
    @Basic(fetch = FetchType.LAZY)
    private byte[] qrSecret;                 // 32 random bytes recommended

    // Flow configuration (Phase 3/4)
    @Column(name = "flow_csv", length = 64)
    private String flowCsv;                  // e.g. "WIFI,GEO,FACE,QR"

    // Mark as active once created (can be used to stop later)
    @Column(nullable = false)
    private boolean active = true;

    public Session() {}

    // ---- derived flow helpers ----
    public List<ValidationStep> getFlow() {
        return ValidationStep.parseCsv(flowCsv);
    }
    public void setFlow(List<ValidationStep> steps) {
        this.flowCsv = ValidationStep.toCsv(steps);
    }
    public String getFlowCsv() { return flowCsv; }
    public void setFlowCsv(String csv) { this.flowCsv = csv; }

    @PrePersist
    public void ensureDefaults() {
        if (this.flowCsv == null || this.flowCsv.isBlank()) {
            this.flowCsv = ValidationStep.toCsv(ValidationStep.defaultFlow());
        }
    }

    // ---- getters/setters ----
    public Long getId() { return id; }

    public TeacherProfile getTeacher() { return teacher; }
    public void setTeacher(TeacherProfile teacher) { this.teacher = teacher; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getRadiusMeters() { return radiusMeters; }
    public void setRadiusMeters(Double radiusMeters) { this.radiusMeters = radiusMeters; }

    public String getTeacherPublicIp() { return teacherPublicIp; }
    public void setTeacherPublicIp(String teacherPublicIp) { this.teacherPublicIp = teacherPublicIp; }

    public String getNetworkSignature() { return networkSignature; }
    public void setNetworkSignature(String networkSignature) { this.networkSignature = networkSignature; }

    public WifiPolicy getWifiPolicy() { return wifiPolicy; }
    public void setWifiPolicy(WifiPolicy wifiPolicy) { this.wifiPolicy = wifiPolicy; }

    public Integer getQrIntervalSeconds() { return qrIntervalSeconds; }
    public void setQrIntervalSeconds(Integer qrIntervalSeconds) { this.qrIntervalSeconds = qrIntervalSeconds; }

    public byte[] getQrSecret() { return qrSecret; }
    public void setQrSecret(byte[] qrSecret) { this.qrSecret = qrSecret; }
}
