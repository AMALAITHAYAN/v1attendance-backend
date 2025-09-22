package com.example.attendance.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "student_profiles")
public class StudentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // NEW: keep a plain copy of the username to satisfy the DB column
    @Column(name = "username", nullable = false, unique = true, length = 191)
    private String username;

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

    // Photo used later for face verification (Phase 3), stored now for Phase 1
    @Lob
    @Basic(fetch = FetchType.LAZY)
    private byte[] photo;

    public StudentProfile() {}

    // Getters/Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

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

    public byte[] getPhoto() { return photo; }
    public void setPhoto(byte[] photo) { this.photo = photo; }
}
