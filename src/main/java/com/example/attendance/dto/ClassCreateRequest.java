package com.example.attendance.dto;

import jakarta.validation.constraints.NotBlank;

public class ClassCreateRequest {
    @NotBlank private String year;
    @NotBlank private String department;
    @NotBlank private String section;
    @NotBlank private String block;
    @NotBlank private String name;

    // getters/setters
    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
    public String getBlock() { return block; }
    public void setBlock(String block) { this.block = block; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
