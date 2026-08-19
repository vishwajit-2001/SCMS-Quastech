package com.scms.scms.model;

import jakarta.persistence.*;
import com.scms.scms.security.SensitiveStringConverter;
import java.time.LocalDate;

@Entity
@Table(name = "teacher")
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String password;
    private String subject;
    private String photo;

    // ── Personal ──
    private String gender;
    private LocalDate dob;

    @Column(name = "blood_group")
    private String bloodGroup;

    @Column(length = 512)
    @Convert(converter = SensitiveStringConverter.class)
    private String phone;

    @Column(name = "alt_phone", length = 512)
    @Convert(converter = SensitiveStringConverter.class)
    private String altPhone;

    @Column(name = "marital_status")
    private String maritalStatus;

    private String religion;

    @Column(name = "caste_name")
    private String casteName;

    private String category;

    // ── Address ──
    private String address;

    @Column(name = "permanent_address")
    private String permanentAddress;

    private String city;
    private String state;

    @Column(name = "pin_code")
    private String pinCode;

    // ── Professional ──
    private String designation;

    @Column(name = "employee_id")
    private String employeeId;

    @Column(name = "joining_date")
    private LocalDate joiningDate;

    // VARCHAR(20) in DB — stores "1-3 Years", "10+ Years" etc.
    private String experience;

    @Column(name = "employment_type")
    private String employmentType;

    private Double salary;
    private String specialization;
    private String department;
    private String status;

    @Column(name = "academic_year")
    private String academicYear;

    // ── Qualification ──
    private String qualification;

    @Column(name = "degree_specialization")
    private String degreeSpecialization;

    private String university;

    @Column(name = "year_of_passing")
    private Integer yearOfPassing;

    private Integer publications;

    // ── Government IDs ──
    @Column(name = "aadhar_number", length = 512)
    @Convert(converter = SensitiveStringConverter.class)
    private String aadharNumber;

    @Column(name = "pan_card_number", length = 512)
    @Convert(converter = SensitiveStringConverter.class)
    private String panCardNumber;

    @Column(name = "voter_id", length = 512)
    @Convert(converter = SensitiveStringConverter.class)
    private String voterId;

    @Column(name = "passport_number", length = 512)
    @Convert(converter = SensitiveStringConverter.class)
    private String passportNumber;

    // ── Bank Details ──
    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "bank_acc_no", length = 512)
    @Convert(converter = SensitiveStringConverter.class)
    private String bankAccNo;

    @Column(name = "ifsc_code", length = 512)
    @Convert(converter = SensitiveStringConverter.class)
    private String ifscCode;

    @Column(name = "pf_number", length = 512)
    @Convert(converter = SensitiveStringConverter.class)
    private String pfNumber;

    @Column(name = "uan_number", length = 512)
    @Convert(converter = SensitiveStringConverter.class)
    private String uanNumber;

    @Column(name = "micr_number", length = 512)
    @Convert(converter = SensitiveStringConverter.class)
    private String micrNumber;

    // ── Emergency Contact ──
    @Column(name = "emergency_contact_name")
    private String emergencyContactName;

    @Column(name = "emergency_phone", length = 512)
    @Convert(converter = SensitiveStringConverter.class)
    private String emergencyPhone;

    @Column(name = "emergency_relation")
    private String emergencyRelation;

    // ── Relationships ──
    @ManyToOne
    @JoinColumn(name = "admin_id")
    private Admin admin;

    @ManyToOne
    @JoinColumn(name = "class_room_id")
    private ClassRoom classRoom;

    // ════════════════════════════════
    // Getters & Setters
    // ════════════════════════════════

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }

    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAltPhone() { return altPhone; }
    public void setAltPhone(String altPhone) { this.altPhone = altPhone; }

    public String getMaritalStatus() { return maritalStatus; }
    public void setMaritalStatus(String maritalStatus) { this.maritalStatus = maritalStatus; }

    public String getReligion() { return religion; }
    public void setReligion(String religion) { this.religion = religion; }

    public String getCasteName() { return casteName; }
    public void setCasteName(String casteName) { this.casteName = casteName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPermanentAddress() { return permanentAddress; }
    public void setPermanentAddress(String permanentAddress) { this.permanentAddress = permanentAddress; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPinCode() { return pinCode; }
    public void setPinCode(String pinCode) { this.pinCode = pinCode; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public LocalDate getJoiningDate() { return joiningDate; }
    public void setJoiningDate(LocalDate joiningDate) { this.joiningDate = joiningDate; }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }

    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }

    public Double getSalary() { return salary; }
    public void setSalary(Double salary) { this.salary = salary; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAcademicYear() {
        return academicYear != null ? academicYear : (classRoom != null ? classRoom.getAcademicYear() : null);
    }

    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }

    public String getDegreeSpecialization() { return degreeSpecialization; }
    public void setDegreeSpecialization(String degreeSpecialization) { this.degreeSpecialization = degreeSpecialization; }

    public String getUniversity() { return university; }
    public void setUniversity(String university) { this.university = university; }

    public Integer getYearOfPassing() { return yearOfPassing; }
    public void setYearOfPassing(Integer yearOfPassing) { this.yearOfPassing = yearOfPassing; }

    public Integer getPublications() { return publications; }
    public void setPublications(Integer publications) { this.publications = publications; }

    public String getAadharNumber() { return aadharNumber; }
    public void setAadharNumber(String aadharNumber) { this.aadharNumber = aadharNumber; }

    public String getPanCardNumber() { return panCardNumber; }
    public void setPanCardNumber(String panCardNumber) { this.panCardNumber = panCardNumber; }

    public String getVoterId() { return voterId; }
    public void setVoterId(String voterId) { this.voterId = voterId; }

    public String getPassportNumber() { return passportNumber; }
    public void setPassportNumber(String passportNumber) { this.passportNumber = passportNumber; }

    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getBankAccNo() { return bankAccNo; }
    public void setBankAccNo(String bankAccNo) { this.bankAccNo = bankAccNo; }

    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }

    public String getPfNumber() { return pfNumber; }
    public void setPfNumber(String pfNumber) { this.pfNumber = pfNumber; }

    public String getUanNumber() { return uanNumber; }
    public void setUanNumber(String uanNumber) { this.uanNumber = uanNumber; }

    public String getMicrNumber() { return micrNumber; }
    public void setMicrNumber(String micrNumber) { this.micrNumber = micrNumber; }

    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }

    public String getEmergencyPhone() { return emergencyPhone; }
    public void setEmergencyPhone(String emergencyPhone) { this.emergencyPhone = emergencyPhone; }

    public String getEmergencyRelation() { return emergencyRelation; }
    public void setEmergencyRelation(String emergencyRelation) { this.emergencyRelation = emergencyRelation; }

    public Admin getAdmin() { return admin; }
    public void setAdmin(Admin admin) { this.admin = admin; }

    public ClassRoom getClassRoom() { return classRoom; }
    public void setClassRoom(ClassRoom classRoom) { this.classRoom = classRoom; }
}
