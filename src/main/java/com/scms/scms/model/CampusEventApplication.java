package com.scms.scms.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "campus_event_application")
public class CampusEventApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "event_id")
    private CampusEvent campusEvent;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    private String fullName;
    private String email;
    private String phone;
    private String course;
    private String semester;
    private String specialization;
    private String sectionName;

    @Column(length = 1500)
    private String notes;

    private LocalDateTime appliedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CampusEvent getCampusEvent() { return campusEvent; }
    public void setCampusEvent(CampusEvent campusEvent) { this.campusEvent = campusEvent; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getSectionName() { return sectionName; }
    public void setSectionName(String sectionName) { this.sectionName = sectionName; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
}
