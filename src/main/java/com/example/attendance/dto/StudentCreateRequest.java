package com.example.attendance.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class StudentCreateRequest {
    @Email @NotBlank
    private String username;   // mail id
    @NotBlank
    private String password;   // can be "1234" if you want to set default here
    @NotBlank
    private String name;
    @NotBlank
    private String rollNo;
    @NotBlank
    private String className;
    @NotBlank
    private String year;
    @NotBlank
    private String department;

    // Photo is sent as multipart file part in controller

    // Getters/Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRollNo() { return rollNo; }
    public void setRollNo(String rollNo) { this.rollNo = rollNo; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
}
