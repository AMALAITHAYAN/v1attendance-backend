// src/main/java/com/example/attendance/dto/SessionStartRequest.java
package com.example.attendance.dto;

import com.example.attendance.model.ValidationStep;
import com.example.attendance.model.WifiPolicy;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

public class SessionStartRequest {
    @NotBlank private String year;
    @NotBlank private String department;
    @NotBlank private String className;
    @NotBlank private String subject;

    @NotNull private LocalDateTime startTime;   // frontend sends ISO_LOCAL_DATE_TIME
    @NotNull private LocalDateTime endTime;

    @NotNull private WifiPolicy wifiPolicy;     // PUBLIC_IP | NETWORK_SIGNATURE | BOTH
    private String networkSignature;            // optional
    private String publicIp;                    // optional, but required if wifiPolicy == PUBLIC_IP or BOTH

    private Integer qrIntervalSeconds;          // default if null

    // geofence (optional as a whole – only required if GEO in flow)
    private Double latitude;
    private Double longitude;
    private Integer radiusMeters;

    @NotNull @Size(min = 1)
    private List<ValidationStep> flow;          // e.g. ["WIFI","GEO","FACE","QR"]

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

    public WifiPolicy getWifiPolicy() { return wifiPolicy; }
    public void setWifiPolicy(WifiPolicy wifiPolicy) { this.wifiPolicy = wifiPolicy; }

    public String getNetworkSignature() { return networkSignature; }
    public void setNetworkSignature(String networkSignature) { this.networkSignature = networkSignature; }

    public String getPublicIp() { return publicIp; }
    public void setPublicIp(String publicIp) { this.publicIp = publicIp; }

    public Integer getQrIntervalSeconds() { return qrIntervalSeconds; }
    public void setQrIntervalSeconds(Integer qrIntervalSeconds) { this.qrIntervalSeconds = qrIntervalSeconds; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Integer getRadiusMeters() { return radiusMeters; }
    public void setRadiusMeters(Integer radiusMeters) { this.radiusMeters = radiusMeters; }

    public List<ValidationStep> getFlow() { return flow; }
    public void setFlow(List<ValidationStep> flow) { this.flow = flow; }
}
