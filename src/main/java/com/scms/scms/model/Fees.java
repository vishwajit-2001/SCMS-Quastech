package com.scms.scms.model;

import jakarta.persistence.*;

@Entity
@Table(name = "fees")  // ← matches your DB table name exactly
public class Fees {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course")
    private String course;          // course code from the course table

    @Column(name = "batch_name")
    private String batchName;

    @ManyToOne
    @JoinColumn(name = "batch_id")
    private Batch batch;

    @Column(name = "academic_year")
    private String academicYear;

    @Column(name = "fee_scope")
    private String feeScope;

    @Column(name = "semester")
    private String semester;

    @Column(name = "total_amount")
    private double totalAmount;     // 80000, 60000, 50000

    @ManyToOne
    @JoinColumn(name = "admin_id")
    private Admin admin;

    // ── Getters & Setters ──

    public Long getId() { return id; }

    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }

    public String getBatchName() { return batchName; }
    public void setBatchName(String batchName) { this.batchName = batchName; }

    public Batch getBatch() { return batch; }
    public void setBatch(Batch batch) { this.batch = batch; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public String getFeeScope() { return feeScope; }
    public void setFeeScope(String feeScope) { this.feeScope = feeScope; }

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public Admin getAdmin() { return admin; }
    public void setAdmin(Admin admin) { this.admin = admin; }
}
