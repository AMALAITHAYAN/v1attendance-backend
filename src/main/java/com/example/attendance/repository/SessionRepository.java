package com.example.attendance.repository;

import com.example.attendance.model.Session;
import com.example.attendance.model.TeacherProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Long> {

    /**
     * Returns true if there exists any session for the same (year, department, className)
     * whose time window overlaps with the requested [start, end).
     *
     * Overlap rule: existing.start < :end AND existing.end > :start
     */
    @Query("""
           SELECT COUNT(s) > 0
           FROM Session s
           WHERE s.year = :year
             AND s.department = :department
             AND s.className = :className
             AND s.active = true
             AND s.startTime < :end
             AND s.endTime > :start
           """)
    boolean hasTimeConflict(
            @Param("year") String year,
            @Param("department") String department,
            @Param("className") String className,
            @Param("start") java.time.LocalDateTime start,
            @Param("end") java.time.LocalDateTime end
    );


    List<Session> findByActiveTrueOrderByStartTimeDesc();

    @Query("""
      select s from Session s
      where s.teacher = :teacher
        and s.startTime between :from and :to
      order by s.startTime desc
    """)
    List<Session> findByTeacherAndRange(@Param("teacher") TeacherProfile teacher,
                                        @Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to);

    @Query("""
      select count(distinct s.id)
      from Session s
      where s.year=:year and s.department=:dept and s.className=:cls
        and s.startTime between :from and :to
    """)
    long countClassSessions(@Param("year") String year,
                            @Param("dept") String dept,
                            @Param("cls") String cls,
                            @Param("from") LocalDateTime from,
                            @Param("to") LocalDateTime to);

    interface SubjectTotal {
        String getSubject();
        long getTotal();
    }

    @Query("""
      select s.subject as subject, count(distinct s.id) as total
      from Session s
      where s.year=:year and s.department=:dept and s.className=:cls
        and s.startTime between :from and :to
      group by s.subject
      order by s.subject
    """)
    List<SubjectTotal> countClassSessionsBySubject(@Param("year") String year,
                                                   @Param("dept") String dept,
                                                   @Param("cls") String cls,
                                                   @Param("from") LocalDateTime from,
                                                   @Param("to") LocalDateTime to);
}
