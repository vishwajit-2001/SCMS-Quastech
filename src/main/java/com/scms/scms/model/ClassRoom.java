package com.scms.scms.model;

import jakarta.persistence.*;

@Entity
@Table(name = "classroom")
public class ClassRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // e.g. "COURSE-FY-Sem1", "COURSE-SY-Sem3"
    private String name;

    // e.g. course code from the course table
    private String course;

    // e.g. course-specific identifier
    private String courseCode;

    // e.g. "Computer Science", "Management"
    private String department;

    // e.g. "Undergraduate", "Postgraduate", "Diploma"
    private String courseType;

    // e.g. "3 Years", "2 Years"
    private String duration;

    // e.g. 4, 6
    private Integer totalSemesters;

    // e.g. "FY", "SY", "TY"
    private String year;

    // e.g. 1, 2, 3, 4, 5, 6
    private Integer semester;

    // e.g. "A", "B"
    private String section;

    // e.g. "101", "Lab A"
    private String room;

    // e.g. batch identifier
    private String batch;

    // e.g. "Course 2025-2028"
    private String batchName;

    private Integer batchStartYear;
    private Integer batchEndYear;

    // e.g. "2025-2026"
    private String academicYear;

    // Maximum number of students
    private Integer intakeCapacity;

    // Morning / Afternoon / Evening
    private String shift;

    private Double totalFees;

    @Column(length = 2000)
    private String description;

    private String status;

    @Column(length = 2000)
    private String remarks;

    // Which admin owns this classroom
    @ManyToOne
    @JoinColumn(name = "admin_id")
    private Admin admin;

    // ── Helpers ──

    /**
     * Returns a display label like "Course – FY – Sem 1 (Section A)"
     */
    public String getDisplayLabel() {
        StringBuilder sb = new StringBuilder();
        if (course  != null) sb.append(course);
        if (year    != null) sb.append(" – ").append(year);
        if (semester!= null) sb.append(" – Sem ").append(semester);
        if (section != null && !section.isBlank()) sb.append(" (Sec ").append(section).append(")");
        return sb.toString();
    }

    // ════════════════════════════════
    // Getters & Setters
    // ════════════════════════════════

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getCourseType() { return courseType; }
    public void setCourseType(String courseType) { this.courseType = courseType; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public Integer getTotalSemesters() { return totalSemesters; }
    public void setTotalSemesters(Integer totalSemesters) { this.totalSemesters = totalSemesters; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }

    public Integer getSemester() { return semester; }
    public void setSemester(Integer semester) { this.semester = semester; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }

    public String getBatch() { return batch; }
    public void setBatch(String batch) { this.batch = batch; }

    public String getBatchName() { return batchName; }
    public void setBatchName(String batchName) { this.batchName = batchName; }

    public Integer getBatchStartYear() { return batchStartYear; }
    public void setBatchStartYear(Integer batchStartYear) { this.batchStartYear = batchStartYear; }

    public Integer getBatchEndYear() { return batchEndYear; }
    public void setBatchEndYear(Integer batchEndYear) { this.batchEndYear = batchEndYear; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public Integer getIntakeCapacity() { return intakeCapacity; }
    public void setIntakeCapacity(Integer intakeCapacity) { this.intakeCapacity = intakeCapacity; }

    public String getShift() { return shift; }
    public void setShift(String shift) { this.shift = shift; }

    public Double getTotalFees() { return totalFees; }
    public void setTotalFees(Double totalFees) { this.totalFees = totalFees; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public Admin getAdmin() { return admin; }
    public void setAdmin(Admin admin) { this.admin = admin; }
}
