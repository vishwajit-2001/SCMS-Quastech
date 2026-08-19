package com.scms.scms.model;

import jakarta.persistence.*;

@Entity
@Table(name = "placement_tpo")
public class PlacementTpo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String email;

    private String password;

    @Column(name = "college_name")
    private String collegeName;

    @ManyToOne
    @JoinColumn(name = "college_id")
    private College college;

    @ManyToOne
    @JoinColumn(name = "admin_id")
    private Admin admin;

    private String phone;

    private String department;

    @Column(name = "employee_id")
    private String employeeId;

    private String designation;

    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getCollegeName() { return collegeName; }
    public void setCollegeName(String collegeName) { this.collegeName = collegeName; }

    public College getCollege() { return college; }
    public void setCollege(College college) {
        this.college = college;
        if (college != null) {
            this.collegeName = college.getName();
        }
    }

    public Admin getAdmin() { return admin; }
    public void setAdmin(Admin admin) {
        this.admin = admin;
        if (admin != null) {
            this.college = admin.getCollege();
            this.collegeName = admin.getCollegeName();
        }
    }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
