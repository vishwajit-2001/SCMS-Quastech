package com.scms.scms.model;

import jakarta.persistence.*;

@Entity
@Table(name = "exam_quiz_question")
public class ExamQuizQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "exam_session_id", nullable = false)
    private ExamSession examSession;

    @Column(name = "question_order")
    private Integer questionOrder;

    @Column(name = "question_text", length = 2000)
    private String questionText;

    @Column(name = "option_a", length = 1000)
    private String optionA;

    @Column(name = "option_b", length = 1000)
    private String optionB;

    @Column(name = "option_c", length = 1000)
    private String optionC;

    @Column(name = "option_d", length = 1000)
    private String optionD;

    @Column(name = "correct_option")
    private String correctOption;

    @Column(name = "explanation", length = 2000)
    private String explanation;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ExamSession getExamSession() { return examSession; }
    public void setExamSession(ExamSession examSession) { this.examSession = examSession; }
    public Integer getQuestionOrder() { return questionOrder; }
    public void setQuestionOrder(Integer questionOrder) { this.questionOrder = questionOrder; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public String getOptionA() { return optionA; }
    public void setOptionA(String optionA) { this.optionA = optionA; }
    public String getOptionB() { return optionB; }
    public void setOptionB(String optionB) { this.optionB = optionB; }
    public String getOptionC() { return optionC; }
    public void setOptionC(String optionC) { this.optionC = optionC; }
    public String getOptionD() { return optionD; }
    public void setOptionD(String optionD) { this.optionD = optionD; }
    public String getCorrectOption() { return correctOption; }
    public void setCorrectOption(String correctOption) { this.correctOption = correctOption; }
    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    @Transient
    public String getCorrectAnswerText() {
        if ("A".equalsIgnoreCase(correctOption)) return optionA;
        if ("B".equalsIgnoreCase(correctOption)) return optionB;
        if ("C".equalsIgnoreCase(correctOption)) return optionC;
        if ("D".equalsIgnoreCase(correctOption)) return optionD;
        return "";
    }
}
