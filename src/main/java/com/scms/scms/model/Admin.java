package com.scms.scms.model;

import jakarta.persistence.*;
import com.scms.scms.security.SensitiveStringConverter;

@Entity
@Table(name = "admin")
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String password;

    @Column(name = "college_name")
    private String collegeName;

    @Column(name = "college_code")
    private String collegeCode;

    @Column(name = "phone", length = 512)
    @Convert(converter = SensitiveStringConverter.class)
    private String phone;

    @Column(name = "role")
    private String role;

    @Column(name = "college_status")
    private String collegeStatus;

    @ManyToOne
    @JoinColumn(name = "college_id")
    private College college;

    // ── Constructors ──
    public Admin() {}

    public Admin(String name, String email, String password) {
        this.name     = name;
        this.email    = email;
        this.password = password;
    }

    // ── Getters & Setters ──

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getCollegeName() {
        if (college != null && college.getName() != null && !college.getName().isBlank()) {
            return college.getName();
        }
        return (collegeName != null && !collegeName.isBlank()) ? collegeName : "AI Campus Institute";
    }
    public void setCollegeName(String collegeName) { this.collegeName = collegeName; }

    public String getCollegeCode() {
        if (college != null && college.getCode() != null && !college.getCode().isBlank()) {
            return college.getCode();
        }
        return (collegeCode != null && !collegeCode.isBlank()) ? collegeCode : null;
    }
    public void setCollegeCode(String collegeCode) { this.collegeCode = collegeCode; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole() {
        return (role != null && !role.isBlank()) ? role : "Institute Admin";
    }
    public void setRole(String role) { this.role = role; }

    public boolean isSuperAdmin() {
        return "SUPER_ADMIN".equalsIgnoreCase(role);
    }

    public String getCollegeStatus() {
        if (college != null && college.getStatus() != null && !college.getStatus().isBlank()) {
            return college.getStatus();
        }
        return (collegeStatus != null && !collegeStatus.isBlank()) ? collegeStatus : "ACTIVE";
    }

    public void setCollegeStatus(String collegeStatus) {
        this.collegeStatus = collegeStatus;
    }

    public boolean isCollegeActive() {
        return !"INACTIVE".equalsIgnoreCase(getCollegeStatus());
    }

    public College getCollege() {
        return college;
    }

    public void setCollege(College college) {
        this.college = college;
        if (college != null) {
            this.collegeName = college.getName();
            this.collegeCode = college.getCode();
            this.collegeStatus = college.getStatus();
        }
    }
}
