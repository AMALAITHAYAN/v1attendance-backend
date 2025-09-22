// src/main/java/com/example/attendance/dto/AdminDtos.java
package com.example.attendance.dto;

public class AdminDtos {
    public record TeacherRow(Long id, String name, String gmail, String subjects) {}
}
