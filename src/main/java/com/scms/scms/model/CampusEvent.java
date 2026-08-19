package com.scms.scms.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "campus_event")
public class CampusEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String organizerName;

    private String organizationName;

    private String venue;

    private String category;

    @Column(length = 2000)
    private String description;

    @Column(name = "event_date_time")
    private LocalDateTime eventDateTime;

    @Column(name = "registration_deadline")
    private LocalDateTime registrationDeadline;

    private String targetAudience;

    private boolean published;

    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getOrganizerName() { return organizerName; }
    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }

    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getEventDateTime() { return eventDateTime; }
    public void setEventDateTime(LocalDateTime eventDateTime) { this.eventDateTime = eventDateTime; }

    public LocalDateTime getRegistrationDeadline() { return registrationDeadline; }
    public void setRegistrationDeadline(LocalDateTime registrationDeadline) { this.registrationDeadline = registrationDeadline; }

    public String getTargetAudience() { return targetAudience; }
    public void setTargetAudience(String targetAudience) { this.targetAudience = targetAudience; }

    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Teacher getTeacher() { return teacher; }
    public void setTeacher(Teacher teacher) { this.teacher = teacher; }

    @Transient
    public boolean isRegistrationOpen() {
        return registrationDeadline == null || registrationDeadline.isAfter(LocalDateTime.now());
    }

    @Transient
    public String getEventLabel() {
        return eventDateTime == null ? "Date not set" : eventDateTime.toString().replace('T', ' ');
    }

    @Transient
    public String getDeadlineLabel() {
        return registrationDeadline == null ? "No deadline set" : registrationDeadline.toString().replace('T', ' ');
    }
}
