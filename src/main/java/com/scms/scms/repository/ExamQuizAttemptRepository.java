package com.scms.scms.repository;

import com.scms.scms.model.ExamQuizAttempt;
import com.scms.scms.model.ExamSession;
import com.scms.scms.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamQuizAttemptRepository extends JpaRepository<ExamQuizAttempt, Long> {
    Optional<ExamQuizAttempt> findByExamSessionAndStudent(ExamSession examSession, Student student);
    List<ExamQuizAttempt> findByExamSessionOrderBySubmittedAtDesc(ExamSession examSession);
    List<ExamQuizAttempt> findByStudentOrderBySubmittedAtDesc(Student student);
}
