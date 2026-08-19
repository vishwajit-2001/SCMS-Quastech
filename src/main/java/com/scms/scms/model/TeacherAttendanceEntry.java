package com.scms.scms.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "teacher_attendance_entry")
public class TeacherAttendanceEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private TeacherAttendanceSession session;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    private String status;

    @Column(name = "marked_at")
    private LocalDateTime markedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TeacherAttendanceSession getSession() { return session; }
    public void setSession(TeacherAttendanceSession session) { this.session = session; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getMarkedAt() { return markedAt; }
    public void setMarkedAt(LocalDateTime markedAt) { this.markedAt = markedAt; }
}
