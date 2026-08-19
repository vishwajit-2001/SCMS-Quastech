package com.scms.scms.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "subject",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_subject_admin_course_batch_sem_code",
                        columnNames = {"admin_id", "course_id", "batch_id", "semester", "code"})
        }
)
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false)
    private Integer semester;

    @Column(name = "course_category")
    private String courseCategory;

    private Double credits;

    private String term;

    private String cycle;

    @ManyToOne
    @JoinColumn(name = "subject_master_id")
    private SubjectMaster subjectMasterRef;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course courseRef;

    @ManyToOne
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batchRef;

    @Column(name = "is_override")
    private Boolean isOverride;

    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin admin;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null || status.isBlank()) {
            status = "active";
        }
        if (isOverride == null) {
            isOverride = Boolean.FALSE;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Integer getSemester() { return semester; }
    public void setSemester(Integer semester) { this.semester = semester; }

    public String getCourseCategory() { return courseCategory; }
    public void setCourseCategory(String courseCategory) { this.courseCategory = courseCategory; }

    public Double getCredits() { return credits; }
    public void setCredits(Double credits) { this.credits = credits; }

    public String getTerm() { return term; }
    public void setTerm(String term) { this.term = term; }

    public String getCycle() { return cycle; }
    public void setCycle(String cycle) { this.cycle = cycle; }

    public SubjectMaster getSubjectMasterRef() { return subjectMasterRef; }
    public void setSubjectMasterRef(SubjectMaster subjectMasterRef) { this.subjectMasterRef = subjectMasterRef; }

    public Course getCourseRef() { return courseRef; }
    public void setCourseRef(Course courseRef) { this.courseRef = courseRef; }

    public Batch getBatchRef() { return batchRef; }
    public void setBatchRef(Batch batchRef) { this.batchRef = batchRef; }

    public Boolean getIsOverride() { return isOverride; }
    public boolean isOverride() { return Boolean.TRUE.equals(isOverride); }
    public void setIsOverride(Boolean isOverride) { this.isOverride = isOverride; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Admin getAdmin() { return admin; }
    public void setAdmin(Admin admin) { this.admin = admin; }
}
