package com.scms.scms.repository;

import com.scms.scms.model.ExamAttendance;
import com.scms.scms.model.ExamSession;
import com.scms.scms.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamAttendanceRepository extends JpaRepository<ExamAttendance, Long> {
    List<ExamAttendance> findByExamSessionOrderByStudent_NameAsc(ExamSession examSession);
    List<ExamAttendance> findByExamSessionAndStatusIgnoreCase(ExamSession examSession, String status);
    ExamAttendance findByExamSessionAndStudent(ExamSession examSession, Student student);
    long deleteByStudent(Student student);
    long countByExamSessionAndStatusIgnoreCase(ExamSession examSession, String status);
}
