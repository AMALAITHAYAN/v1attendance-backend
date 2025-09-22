package com.example.attendance.dto;

import jakarta.validation.constraints.NotBlank;

public class StudentCreateInClassRequest {
    @NotBlank private String username;
    @NotBlank private String name;
    @NotBlank private String rollNo;
    // optional; default in controller if null/blank
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRollNo() { return rollNo; }
    public void setRollNo(String rollNo) { this.rollNo = rollNo; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
