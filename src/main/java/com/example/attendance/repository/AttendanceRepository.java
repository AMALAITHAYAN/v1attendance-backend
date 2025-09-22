package com.example.attendance.repository;

import com.example.attendance.model.Attendance;
import com.example.attendance.model.Session;
import com.example.attendance.model.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    boolean existsBySessionAndStudentAndSuccessIsTrue(Session s, StudentProfile st);
    Optional<Attendance> findFirstBySessionAndStudentAndQrSlot(Session s, StudentProfile st, Long qrSlot);

    long countBySession(Session session);
    long countBySessionAndSuccessIsTrue(Session session);


    @Query("""
      select count(distinct a.session.id)
      from Attendance a join a.session s
      where a.student.id=:studentId and a.success=true
        and s.startTime between :from and :to
    """)
    long countStudentPresent(@Param("studentId") Long studentId,
                             @Param("from") LocalDateTime from,
                             @Param("to") LocalDateTime to);

    interface SubjectPresent {
        String getSubject();
        long getPresent();
    }

    @Query("""
      select s.subject as subject, count(distinct a.session.id) as present
      from Attendance a join a.session s
      where a.student.id=:studentId and a.success=true
        and s.startTime between :from and :to
      group by s.subject
      order by s.subject
    """)
    List<SubjectPresent> countStudentPresentBySubject(@Param("studentId") Long studentId,
                                                      @Param("from") LocalDateTime from,
                                                      @Param("to") LocalDateTime to);

    @Query("select count(a) from Attendance a where a.session.id=:sessionId and a.success=true")
    long countPresentForSession(@Param("sessionId") Long sessionId);

    @Query("select count(a) from Attendance a where a.session.id=:sessionId")
    long countMarksForSession(@Param("sessionId") Long sessionId);

    List<Attendance> findByStudentIdAndMarkedAtBetweenOrderByMarkedAtDesc(Long studentId, LocalDateTime from, LocalDateTime to);

    long countBySession_Id(Long sessionId);
    List<Attendance> findAllBySession_IdOrderByMarkedAtAsc(Long sessionId);
}
