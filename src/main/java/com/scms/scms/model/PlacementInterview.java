package com.scms.scms.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "placement_interview")
public class PlacementInterview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "drive_id")
    private PlacementDrive placementDrive;

    @ManyToOne
    @JoinColumn(name = "tpo_id")
    private PlacementTpo placementTpo;

    private String interviewRound;

    private LocalDateTime interviewDateTime;

    private String venue;

    private String meetingLink;

    @Column(length = 2000)
    private String notes;

    private String status;

    private LocalDateTime createdAt;

    @ManyToMany
    @JoinTable(
            name = "placement_interview_student",
            joinColumns = @JoinColumn(name = "interview_id"),
            inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    private List<Student> selectedStudents = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PlacementDrive getPlacementDrive() { return placementDrive; }
    public void setPlacementDrive(PlacementDrive placementDrive) { this.placementDrive = placementDrive; }

    public PlacementTpo getPlacementTpo() { return placementTpo; }
    public void setPlacementTpo(PlacementTpo placementTpo) { this.placementTpo = placementTpo; }

    public String getInterviewRound() { return interviewRound; }
    public void setInterviewRound(String interviewRound) { this.interviewRound = interviewRound; }

    public LocalDateTime getInterviewDateTime() { return interviewDateTime; }
    public void setInterviewDateTime(LocalDateTime interviewDateTime) { this.interviewDateTime = interviewDateTime; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public String getMeetingLink() { return meetingLink; }
    public void setMeetingLink(String meetingLink) { this.meetingLink = meetingLink; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<Student> getSelectedStudents() { return selectedStudents; }
    public void setSelectedStudents(List<Student> selectedStudents) { this.selectedStudents = selectedStudents; }
}
