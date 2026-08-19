package com.scms.scms.repository;

import com.scms.scms.model.ExamQuizQuestion;
import com.scms.scms.model.ExamSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExamQuizQuestionRepository extends JpaRepository<ExamQuizQuestion, Long> {
    List<ExamQuizQuestion> findByExamSessionOrderByQuestionOrderAsc(ExamSession examSession);
    long countByExamSession(ExamSession examSession);
}
