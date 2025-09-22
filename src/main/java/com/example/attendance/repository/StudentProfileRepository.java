package com.example.attendance.repository;

import com.example.attendance.model.StudentProfile;
import com.example.attendance.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {

    // Works when StudentProfile has: @OneToOne User user;
    Optional<StudentProfile> findByUser(User user);

    // Handy alternative if you want to search by user id
    Optional<StudentProfile> findByUserId(Long userId);
    List<StudentProfile> findByYearAndDepartmentAndClassName(String year, String department, String className);
    long countByYearAndDepartmentAndClassName(String year, String department, String className);


    Optional<StudentProfile> findByUser_Username(String username);
}
