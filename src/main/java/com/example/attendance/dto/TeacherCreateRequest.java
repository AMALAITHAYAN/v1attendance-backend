package com.example.attendance.dto;

import com.example.attendance.model.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public class TeacherCreateRequest {
    @NotBlank
    private String name;

    private Gender gender;

    @NotEmpty
    private Set<String> subjects;

    private String phoneNumber;

    private String idNumber;

    @Email @NotBlank
    private String gmailId; // login id

    @NotBlank
    private String password;

    // Getters/Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }

    public Set<String> getSubjects() { return subjects; }
    public void setSubjects(Set<String> subjects) { this.subjects = subjects; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getIdNumber() { return idNumber; }
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }

    public String getGmailId() { return gmailId; }
    public void setGmailId(String gmailId) { this.gmailId = gmailId; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
