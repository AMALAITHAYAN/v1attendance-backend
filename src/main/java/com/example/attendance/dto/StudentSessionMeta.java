// src/main/java/com/example/attendance/dto/StudentSessionMeta.java
package com.example.attendance.dto;

import com.example.attendance.model.Session;
import com.example.attendance.model.ValidationStep;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

public class StudentSessionMeta {
    public Long id;
    public boolean active;
    public Instant startTime;
    public Instant endTime;

    public List<String> flow;          // ["WIFI","GEO","FACE","QR"]
    public String wifiPolicy;          // e.g. "PUBLIC_IP"
    public String teacherPublicIp;     // teacher's public IP saved on start

    public Double latitude;
    public Double longitude;
    public Integer radiusMeters;
    public Integer qrIntervalSeconds;

    public static StudentSessionMeta from(Session s) {
        StudentSessionMeta m = new StudentSessionMeta();
        m.id = s.getId();
        m.active = s.isActive();

        // LocalDateTime -> Instant requires a ZoneId
        if (s.getStartTime() != null) {
            m.startTime = s.getStartTime()
                    .atZone(ZoneId.systemDefault())
                    .toInstant();
        }
        if (s.getEndTime() != null) {
            m.endTime = s.getEndTime()
                    .atZone(ZoneId.systemDefault())
                    .toInstant();
        }

        // Null-safe flow mapping
        m.flow = (s.getFlow() == null)
                ? List.of()
                : s.getFlow().stream().filter(Objects::nonNull).map(ValidationStep::name).toList();

        m.wifiPolicy = (s.getWifiPolicy() == null) ? null : s.getWifiPolicy().name();
        m.teacherPublicIp = s.getTeacherPublicIp();

        m.latitude = s.getLatitude();
        m.longitude = s.getLongitude();

        // Double -> Integer (meters) – cast safely
        m.radiusMeters = (s.getRadiusMeters() == null) ? null : s.getRadiusMeters().intValue();

        // If your getter is Long/Double, cast similarly:
        // e.g., m.qrIntervalSeconds = (s.getQrIntervalSeconds() == null) ? null : s.getQrIntervalSeconds().intValue();
        m.qrIntervalSeconds = s.getQrIntervalSeconds();

        return m;
    }
}
