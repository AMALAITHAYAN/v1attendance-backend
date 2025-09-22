package com.example.attendance.repository;

import com.example.attendance.model.TeacherProfile;
import com.example.attendance.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface TeacherProfileRepository extends JpaRepository<TeacherProfile, Long> {
    Optional<TeacherProfile> findByUser(User user);
    @Query("""
        select t from TeacherProfile t
        left join t.user u
        where lower(t.name) like concat('%', :q, '%')
           or lower(coalesce(u.username, '')) like concat('%', :q, '%')
    """)
    Page<TeacherProfile> search(@Param("q") String q, Pageable pageable);
}
