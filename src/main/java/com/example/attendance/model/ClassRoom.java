package com.example.attendance.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(
        name = "classes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_class_unique",
                columnNames = {"year","department","section","block","name"}
        ),
        indexes = {
                @Index(name = "idx_class_year_dept", columnList = "year,department"),
                @Index(name = "idx_class_all", columnList = "year,department,section,block,name")
        }
)
public class ClassRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank private String year;
    @NotBlank private String department;
    @NotBlank private String section;
    @NotBlank private String block;
    @NotBlank private String name; // human display name for the class

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_teacher_id")
    private TeacherProfile createdBy; // optional: who created it

    // -------- getters/setters --------
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public TeacherProfile getCreatedBy() { return createdBy; }
    public void setCreatedBy(TeacherProfile createdBy) { this.createdBy = createdBy; }

    @Transient
    public String getDisplay() {
        // e.g. "Y2 CSE / Sec A / Block B / CSA"
        return String.format("Y%s %s / Sec %s / Block %s / %s",
                year, department, section, block, name);
    }
}
