package com.scms.scms.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "exam_attendance")
public class ExamAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "exam_id")
    private ExamSession examSession;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    private String status;

    @Column(name = "auto_marked")
    private boolean autoMarked;

    @Column(name = "marked_at")
    private LocalDateTime markedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ExamSession getExamSession() { return examSession; }
    public void setExamSession(ExamSession examSession) { this.examSession = examSession; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isAutoMarked() { return autoMarked; }
    public void setAutoMarked(boolean autoMarked) { this.autoMarked = autoMarked; }
    public LocalDateTime getMarkedAt() { return markedAt; }
    public void setMarkedAt(LocalDateTime markedAt) { this.markedAt = markedAt; }
}
