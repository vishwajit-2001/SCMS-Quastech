package com.scms.scms.repository;

import com.scms.scms.model.ExamQuizAttempt;
import com.scms.scms.model.ExamQuizQuestion;
import com.scms.scms.model.ExamQuizResponse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExamQuizResponseRepository extends JpaRepository<ExamQuizResponse, Long> {
    List<ExamQuizResponse> findByAttemptOrderByQuestion_QuestionOrderAsc(ExamQuizAttempt attempt);
    Optional<ExamQuizResponse> findByAttemptAndQuestion(ExamQuizAttempt attempt, ExamQuizQuestion question);
}
