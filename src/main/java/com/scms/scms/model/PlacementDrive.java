package com.scms.scms.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "placement_drive")
public class PlacementDrive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String companyName;

    private String jobTitle;

    private String packageOffered;

    private String location;

    private String driveType;

    private String experience;

    @Column(name = "salary_range")
    private String salaryRange;

    private Integer openings;

    private String eligibilityCourse;

    private String eligibilitySemester;

    private String eligibilityDegree;

    private String skillsRequired;

    @Column(length = 2000)
    private String description;

    private LocalDate driveDate;

    private LocalDate lastApplyDate;

    @Column(name = "application_deadline")
    private java.time.LocalDateTime applicationDeadline;

    private boolean published;

    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "tpo_id")
    private PlacementTpo placementTpo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getPackageOffered() { return packageOffered; }
    public void setPackageOffered(String packageOffered) { this.packageOffered = packageOffered; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDriveType() { return driveType; }
    public void setDriveType(String driveType) { this.driveType = driveType; }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }

    public String getSalaryRange() { return salaryRange; }
    public void setSalaryRange(String salaryRange) { this.salaryRange = salaryRange; }

    public Integer getOpenings() { return openings; }
    public void setOpenings(Integer openings) { this.openings = openings; }

    public String getEligibilityCourse() { return eligibilityCourse; }
    public void setEligibilityCourse(String eligibilityCourse) { this.eligibilityCourse = eligibilityCourse; }

    public String getEligibilitySemester() { return eligibilitySemester; }
    public void setEligibilitySemester(String eligibilitySemester) { this.eligibilitySemester = eligibilitySemester; }

    public String getEligibilityDegree() { return eligibilityDegree; }
    public void setEligibilityDegree(String eligibilityDegree) { this.eligibilityDegree = eligibilityDegree; }

    public String getSkillsRequired() { return skillsRequired; }
    public void setSkillsRequired(String skillsRequired) { this.skillsRequired = skillsRequired; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDriveDate() { return driveDate; }
    public void setDriveDate(LocalDate driveDate) { this.driveDate = driveDate; }

    public LocalDate getLastApplyDate() { return lastApplyDate; }
    public void setLastApplyDate(LocalDate lastApplyDate) { this.lastApplyDate = lastApplyDate; }

    public java.time.LocalDateTime getApplicationDeadline() { return applicationDeadline; }
    public void setApplicationDeadline(java.time.LocalDateTime applicationDeadline) { this.applicationDeadline = applicationDeadline; }

    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public PlacementTpo getPlacementTpo() { return placementTpo; }
    public void setPlacementTpo(PlacementTpo placementTpo) { this.placementTpo = placementTpo; }

    @Transient
    public boolean isApplicationOpen() {
        return applicationDeadline == null || applicationDeadline.isAfter(java.time.LocalDateTime.now());
    }

    @Transient
    public String getDeadlineLabel() {
        return applicationDeadline == null
                ? "No deadline set"
                : applicationDeadline.toString().replace('T', ' ');
    }
}
