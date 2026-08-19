package com.scms.scms.model;

import jakarta.persistence.*;

@Entity
@Table(name = "exam_quiz_response", uniqueConstraints = {
        @UniqueConstraint(name = "uq_exam_quiz_response_attempt_question", columnNames = {"attempt_id", "question_id"})
})
public class ExamQuizResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "attempt_id", nullable = false)
    private ExamQuizAttempt attempt;

    @ManyToOne
    @JoinColumn(name = "question_id", nullable = false)
    private ExamQuizQuestion question;

    @Column(name = "selected_option")
    private String selectedOption;

    @Column(name = "correct_option")
    private String correctOption;

    @Column(name = "is_correct")
    private Boolean correct;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ExamQuizAttempt getAttempt() { return attempt; }
    public void setAttempt(ExamQuizAttempt attempt) { this.attempt = attempt; }
    public ExamQuizQuestion getQuestion() { return question; }
    public void setQuestion(ExamQuizQuestion question) { this.question = question; }
    public String getSelectedOption() { return selectedOption; }
    public void setSelectedOption(String selectedOption) { this.selectedOption = selectedOption; }
    public String getCorrectOption() { return correctOption; }
    public void setCorrectOption(String correctOption) { this.correctOption = correctOption; }
    public Boolean getCorrect() { return correct; }
    public void setCorrect(Boolean correct) { this.correct = correct; }
}
