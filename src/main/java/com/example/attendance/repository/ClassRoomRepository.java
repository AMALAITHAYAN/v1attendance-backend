package com.example.attendance.repository;

import com.example.attendance.model.ClassRoom;
import com.example.attendance.model.TeacherProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassRoomRepository extends JpaRepository<ClassRoom, Long> {
    Optional<ClassRoom> findByYearAndDepartmentAndSectionAndBlockAndName(
            String year, String department, String section, String block, String name);

    List<ClassRoom> findByCreatedBy(TeacherProfile createdBy);
}
