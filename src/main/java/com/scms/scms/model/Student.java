package com.scms.scms.model;

import jakarta.persistence.*;
import com.scms.scms.security.SensitiveStringConverter;
import java.time.LocalDate;

@Entity
@Table(name = "student")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Core Login ──
    private String name;
    private String email;
    private String password;
    @Column(name = "mobile_number", length = 512)
    private String mobileNumber;
    @Column(columnDefinition = "TEXT")
    private String title;
    @Column(columnDefinition = "TEXT")
    private String firstName;
    @Column(columnDefinition = "TEXT")
    private String middleName;
    @Column(columnDefinition = "TEXT")
    private String lastName;
    @Column(columnDefinition = "TEXT")
    private String nameAsPerAadhaar;
    @Column(columnDefinition = "TEXT")
    private String lnameAs12thStd;
    @Column(columnDefinition = "TEXT")
    private String fnameAs12thStd;
    @Column(columnDefinition = "TEXT")
    private String mnameAs12thStd;
    @Column(columnDefinition = "TEXT")
    private String phoneNumber;
    @Column(columnDefinition = "TEXT")
    private String parentMobile;
    @Column(columnDefinition = "TEXT")
    private String maritalStatus;
    @Column(columnDefinition = "TEXT")
    private String motherTongue;
    @Column(columnDefinition = "TEXT")
    private String nativePlace;
    @Column(columnDefinition = "TEXT")
    private String birthPlace;
    @Column(columnDefinition = "TEXT")
    private String birthCountry;
    @Column(columnDefinition = "TEXT")
    private String region;
    @Column(columnDefinition = "TEXT")
    private String nationality;
    @Column(columnDefinition = "TEXT")
    private String categoryType;
    @Column(columnDefinition = "TEXT")
    private String casteCategory;
    @Column(columnDefinition = "TEXT")
    private String subCaste;
    @Column(columnDefinition = "TEXT")
    private String fatherOccupation;
    @Column(columnDefinition = "TEXT")
    private String fatherQualification;
    @Column(columnDefinition = "TEXT")
    private String motherQualification;
    private Integer totalFamilyMember;
    private Double familyAnnualIncome;
    private Boolean differentlyAbled;
    private Boolean sportsPerson;
    @Column(columnDefinition = "TEXT")
    private String sportsAchievement;
    @Column(columnDefinition = "TEXT")
    private String hobbies;
    @Column(columnDefinition = "TEXT")
    private String universityPreAdmRegNo;
    private Integer noOfAttempt;
    @Column(columnDefinition = "TEXT")
    private String inhouse;
    @Column(columnDefinition = "TEXT")
    private String mediumOfInstruction;
    @Column(columnDefinition = "TEXT")
    private String socialReservation;
    @Column(columnDefinition = "TEXT")
    private String academicBankOfCredits;
    @Column(name = "last_qualifying_exam_name", columnDefinition = "TEXT")
    private String lastQualifyingExamName;
    @Column(name = "board_university", columnDefinition = "TEXT")
    private String boardUniversity;
    @Column(name = "school_college", columnDefinition = "TEXT")
    private String schoolCollege;
    @Column(name = "date_of_passing")
    private LocalDate dateOfPassing;
    @Column(columnDefinition = "TEXT")
    private String result;
    @Column(name = "exam_seat_no", columnDefinition = "TEXT")
    private String examSeatNo;
    @Column(name = "obtained_marks")
    private Double obtainedMarks;
    @Column(name = "total_marks")
    private Double totalMarks;
    @Column(name = "percentage")
    private Double percentage;
    @Column(name = "passing_month", columnDefinition = "TEXT")
    private String passingMonth;
    @Column(name = "passing_year")
    private Integer passingYear;
    @Column(columnDefinition = "TEXT")
    private String stream;
    @Column(name = "education_gap")
    private Boolean educationGap;

    // ── Academic ──
    private String course;
    private String semester;
    private String academicYear;
    private String degree;       // UG / PG / Diploma
    private String sectionName;  // A, B, C
    private String medium;       // English / Hindi / Marathi

    // ── Identification Numbers ──
    private String rollNo;
    private String enrollmentNo;
    private String registrationNo;
    private String prnNumber;
    private String abcNumber;

    // ── Personal Info ──
    private String gender;
    private LocalDate dob;
    private String bloodGroup;
    private String religion;
    private LocalDate admissionDate;
    private String casteName;
    private String category;

    // ── Family Info ──
    private String fatherName;
    private String motherName;
    private String guardianName;

    // ── Government IDs ──
    @Column(length = 512)
    @Convert(converter = SensitiveStringConverter.class)
    private String aadharNumber;
    @Column(length = 512)
    @Convert(converter = SensitiveStringConverter.class)
    private String panCardNumber;
    @Column(length = 512)
    @Convert(converter = SensitiveStringConverter.class)
    private String voterId;
    @Column(length = 512)
    @Convert(converter = SensitiveStringConverter.class)
    private String eidNumber;

    // ── Bank Details ──
    private String bankName;
    @Column(length = 512)
    @Convert(converter = SensitiveStringConverter.class)
    private String bankAccNo;
    @Column(length = 512)
    @Convert(converter = SensitiveStringConverter.class)
    private String ifscCode;
    @Column(length = 512)
    @Convert(converter = SensitiveStringConverter.class)
    private String micrNumber;

    // ── Photo (web path stored, not the file itself) ──
    private String photo;
    @Column(columnDefinition = "TEXT")
    private String signature;
    @Column(columnDefinition = "TEXT")
    private String currentAddress;
    @Column(columnDefinition = "TEXT")
    private String permanentAddress;
    @Column(name = "corresponding_house_number", columnDefinition = "TEXT")
    private String correspondingHouseNumber;
    @Column(name = "corresponding_address", columnDefinition = "TEXT")
    private String correspondingAddress;
    @Column(name = "corresponding_country", columnDefinition = "TEXT")
    private String correspondingCountry;
    @Column(name = "corresponding_state", columnDefinition = "TEXT")
    private String correspondingState;
    @Column(name = "corresponding_district", columnDefinition = "TEXT")
    private String correspondingDistrict;
    @Column(name = "corresponding_city", columnDefinition = "TEXT")
    private String correspondingCity;
    @Column(name = "corresponding_pin_code", columnDefinition = "TEXT")
    private String correspondingPinCode;
    @Column(name = "residence_house_number", columnDefinition = "TEXT")
    private String residenceHouseNumber;
    @Column(name = "residence_address", columnDefinition = "TEXT")
    private String residenceAddress;
    @Column(name = "residence_country", columnDefinition = "TEXT")
    private String residenceCountry;
    @Column(name = "residence_state", columnDefinition = "TEXT")
    private String residenceState;
    @Column(name = "residence_district", columnDefinition = "TEXT")
    private String residenceDistrict;
    @Column(name = "residence_city", columnDefinition = "TEXT")
    private String residenceCity;
    @Column(name = "residence_pin_code", columnDefinition = "TEXT")
    private String residencePinCode;

    // ── Fees ──
    private Double totalFees;
    private Double paidFees;
    private Double pendingFees;
    @Column(name = "admission_status")
    private String admissionStatus;
    @Column(name = "program_level")
    private String programLevel;
    @Column(name = "onboarding_completed")
    private Boolean onboardingCompleted;

    // ── Relations ──
    @ManyToOne
    @JoinColumn(name = "class_id")
    private ClassRoom classRoom;

    @ManyToOne
    @JoinColumn(name = "batch_id")
    private Batch batch;

    @ManyToOne
    @JoinColumn(name = "admin_id")
    private Admin admin;

    // ════════════════════════════════
    //  Constructors
    // ════════════════════════════════
    public Student() {}

    // ════════════════════════════════
    //  Getters & Setters
    // ════════════════════════════════

    public Long getId() { return id; }

    // Core Login
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getNameAsPerAadhaar() { return nameAsPerAadhaar; }
    public void setNameAsPerAadhaar(String nameAsPerAadhaar) { this.nameAsPerAadhaar = nameAsPerAadhaar; }

    public String getLnameAs12thStd() { return lnameAs12thStd; }
    public void setLnameAs12thStd(String lnameAs12thStd) { this.lnameAs12thStd = lnameAs12thStd; }

    public String getFnameAs12thStd() { return fnameAs12thStd; }
    public void setFnameAs12thStd(String fnameAs12thStd) { this.fnameAs12thStd = fnameAs12thStd; }

    public String getMnameAs12thStd() { return mnameAs12thStd; }
    public void setMnameAs12thStd(String mnameAs12thStd) { this.mnameAs12thStd = mnameAs12thStd; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getParentMobile() { return parentMobile; }
    public void setParentMobile(String parentMobile) { this.parentMobile = parentMobile; }

    public String getMaritalStatus() { return maritalStatus; }
    public void setMaritalStatus(String maritalStatus) { this.maritalStatus = maritalStatus; }

    public String getMotherTongue() { return motherTongue; }
    public void setMotherTongue(String motherTongue) { this.motherTongue = motherTongue; }

    public String getNativePlace() { return nativePlace; }
    public void setNativePlace(String nativePlace) { this.nativePlace = nativePlace; }

    public String getBirthPlace() { return birthPlace; }
    public void setBirthPlace(String birthPlace) { this.birthPlace = birthPlace; }

    public String getBirthCountry() { return birthCountry; }
    public void setBirthCountry(String birthCountry) { this.birthCountry = birthCountry; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getNationality() { return nationality; }
    public void setNationality(String nationality) { this.nationality = nationality; }

    public String getCategoryType() { return categoryType; }
    public void setCategoryType(String categoryType) { this.categoryType = categoryType; }

    public String getCasteCategory() { return casteCategory; }
    public void setCasteCategory(String casteCategory) { this.casteCategory = casteCategory; }

    public String getSubCaste() { return subCaste; }
    public void setSubCaste(String subCaste) { this.subCaste = subCaste; }

    public String getFatherOccupation() { return fatherOccupation; }
    public void setFatherOccupation(String fatherOccupation) { this.fatherOccupation = fatherOccupation; }

    public String getFatherQualification() { return fatherQualification; }
    public void setFatherQualification(String fatherQualification) { this.fatherQualification = fatherQualification; }

    public String getMotherQualification() { return motherQualification; }
    public void setMotherQualification(String motherQualification) { this.motherQualification = motherQualification; }

    public Integer getTotalFamilyMember() { return totalFamilyMember; }
    public void setTotalFamilyMember(Integer totalFamilyMember) { this.totalFamilyMember = totalFamilyMember; }

    public Double getFamilyAnnualIncome() { return familyAnnualIncome; }
    public void setFamilyAnnualIncome(Double familyAnnualIncome) { this.familyAnnualIncome = familyAnnualIncome; }

    public Boolean getDifferentlyAbled() { return differentlyAbled; }
    public void setDifferentlyAbled(Boolean differentlyAbled) { this.differentlyAbled = differentlyAbled; }

    public Boolean getSportsPerson() { return sportsPerson; }
    public void setSportsPerson(Boolean sportsPerson) { this.sportsPerson = sportsPerson; }

    public String getSportsAchievement() { return sportsAchievement; }
    public void setSportsAchievement(String sportsAchievement) { this.sportsAchievement = sportsAchievement; }

    public String getHobbies() { return hobbies; }
    public void setHobbies(String hobbies) { this.hobbies = hobbies; }

    public String getUniversityPreAdmRegNo() { return universityPreAdmRegNo; }
    public void setUniversityPreAdmRegNo(String universityPreAdmRegNo) { this.universityPreAdmRegNo = universityPreAdmRegNo; }

    public Integer getNoOfAttempt() { return noOfAttempt; }
    public void setNoOfAttempt(Integer noOfAttempt) { this.noOfAttempt = noOfAttempt; }

    public String getInhouse() { return inhouse; }
    public void setInhouse(String inhouse) { this.inhouse = inhouse; }

    public String getMediumOfInstruction() { return mediumOfInstruction; }
    public void setMediumOfInstruction(String mediumOfInstruction) { this.mediumOfInstruction = mediumOfInstruction; }

    public String getSocialReservation() { return socialReservation; }
    public void setSocialReservation(String socialReservation) { this.socialReservation = socialReservation; }

    public String getAcademicBankOfCredits() { return academicBankOfCredits; }
    public void setAcademicBankOfCredits(String academicBankOfCredits) { this.academicBankOfCredits = academicBankOfCredits; }

    public String getLastQualifyingExamName() { return lastQualifyingExamName; }
    public void setLastQualifyingExamName(String lastQualifyingExamName) { this.lastQualifyingExamName = lastQualifyingExamName; }

    public String getBoardUniversity() { return boardUniversity; }
    public void setBoardUniversity(String boardUniversity) { this.boardUniversity = boardUniversity; }

    public String getSchoolCollege() { return schoolCollege; }
    public void setSchoolCollege(String schoolCollege) { this.schoolCollege = schoolCollege; }

    public LocalDate getDateOfPassing() { return dateOfPassing; }
    public void setDateOfPassing(LocalDate dateOfPassing) { this.dateOfPassing = dateOfPassing; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getExamSeatNo() { return examSeatNo; }
    public void setExamSeatNo(String examSeatNo) { this.examSeatNo = examSeatNo; }

    public Double getObtainedMarks() { return obtainedMarks; }
    public void setObtainedMarks(Double obtainedMarks) { this.obtainedMarks = obtainedMarks; }

    public Double getTotalMarks() { return totalMarks; }
    public void setTotalMarks(Double totalMarks) { this.totalMarks = totalMarks; }

    public Double getPercentage() { return percentage; }
    public void setPercentage(Double percentage) { this.percentage = percentage; }

    public String getPassingMonth() { return passingMonth; }
    public void setPassingMonth(String passingMonth) { this.passingMonth = passingMonth; }

    public Integer getPassingYear() { return passingYear; }
    public void setPassingYear(Integer passingYear) { this.passingYear = passingYear; }

    public String getStream() { return stream; }
    public void setStream(String stream) { this.stream = stream; }

    public Boolean getEducationGap() { return educationGap; }
    public void setEducationGap(Boolean educationGap) { this.educationGap = educationGap; }

    // Academic
    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree; }

    public String getSectionName() { return sectionName; }
    public void setSectionName(String sectionName) { this.sectionName = sectionName; }

    public String getMedium() { return medium; }
    public void setMedium(String medium) { this.medium = medium; }

    // Identification Numbers
    public String getRollNo() { return rollNo; }
    public void setRollNo(String rollNo) { this.rollNo = rollNo; }

    public String getEnrollmentNo() { return enrollmentNo; }
    public void setEnrollmentNo(String enrollmentNo) { this.enrollmentNo = enrollmentNo; }

    public String getRegistrationNo() { return registrationNo; }
    public void setRegistrationNo(String registrationNo) { this.registrationNo = registrationNo; }

    public String getPrnNumber() { return prnNumber; }
    public void setPrnNumber(String prnNumber) { this.prnNumber = prnNumber; }

    public String getAbcNumber() { return abcNumber; }
    public void setAbcNumber(String abcNumber) { this.abcNumber = abcNumber; }

    // Personal Info
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public LocalDate getDob() { return dob; }
    public void setDob(LocalDate dob) { this.dob = dob; }

    public String getBloodGroup() { return bloodGroup; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }

    public String getReligion() { return religion; }
    public void setReligion(String religion) { this.religion = religion; }

    public LocalDate getAdmissionDate() { return admissionDate; }
    public void setAdmissionDate(LocalDate admissionDate) { this.admissionDate = admissionDate; }

    public String getCasteName() { return casteName; }
    public void setCasteName(String casteName) { this.casteName = casteName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    // Family Info
    public String getFatherName() { return fatherName; }
    public void setFatherName(String fatherName) { this.fatherName = fatherName; }

    public String getMotherName() { return motherName; }
    public void setMotherName(String motherName) { this.motherName = motherName; }

    public String getGuardianName() { return guardianName; }
    public void setGuardianName(String guardianName) { this.guardianName = guardianName; }

    // Government IDs
    public String getAadharNumber() { return aadharNumber; }
    public void setAadharNumber(String aadharNumber) { this.aadharNumber = aadharNumber; }

    public String getPanCardNumber() { return panCardNumber; }
    public void setPanCardNumber(String panCardNumber) { this.panCardNumber = panCardNumber; }

    public String getVoterId() { return voterId; }
    public void setVoterId(String voterId) { this.voterId = voterId; }

    public String getEidNumber() { return eidNumber; }
    public void setEidNumber(String eidNumber) { this.eidNumber = eidNumber; }

    // Bank Details
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }

    public String getBankAccNo() { return bankAccNo; }
    public void setBankAccNo(String bankAccNo) { this.bankAccNo = bankAccNo; }

    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }

    public String getMicrNumber() { return micrNumber; }
    public void setMicrNumber(String micrNumber) { this.micrNumber = micrNumber; }

    // Photo
    public String getPhoto() { return photo; }
    public void setPhoto(String photo) { this.photo = photo; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public String getCurrentAddress() { return currentAddress; }
    public void setCurrentAddress(String currentAddress) { this.currentAddress = currentAddress; }

    public String getPermanentAddress() { return permanentAddress; }
    public void setPermanentAddress(String permanentAddress) { this.permanentAddress = permanentAddress; }

    public String getCorrespondingHouseNumber() { return correspondingHouseNumber; }
    public void setCorrespondingHouseNumber(String correspondingHouseNumber) { this.correspondingHouseNumber = correspondingHouseNumber; }

    public String getCorrespondingAddress() { return correspondingAddress; }
    public void setCorrespondingAddress(String correspondingAddress) { this.correspondingAddress = correspondingAddress; }

    public String getCorrespondingCountry() { return correspondingCountry; }
    public void setCorrespondingCountry(String correspondingCountry) { this.correspondingCountry = correspondingCountry; }

    public String getCorrespondingState() { return correspondingState; }
    public void setCorrespondingState(String correspondingState) { this.correspondingState = correspondingState; }

    public String getCorrespondingDistrict() { return correspondingDistrict; }
    public void setCorrespondingDistrict(String correspondingDistrict) { this.correspondingDistrict = correspondingDistrict; }

    public String getCorrespondingCity() { return correspondingCity; }
    public void setCorrespondingCity(String correspondingCity) { this.correspondingCity = correspondingCity; }

    public String getCorrespondingPinCode() { return correspondingPinCode; }
    public void setCorrespondingPinCode(String correspondingPinCode) { this.correspondingPinCode = correspondingPinCode; }

    public String getResidenceHouseNumber() { return residenceHouseNumber; }
    public void setResidenceHouseNumber(String residenceHouseNumber) { this.residenceHouseNumber = residenceHouseNumber; }

    public String getResidenceAddress() { return residenceAddress; }
    public void setResidenceAddress(String residenceAddress) { this.residenceAddress = residenceAddress; }

    public String getResidenceCountry() { return residenceCountry; }
    public void setResidenceCountry(String residenceCountry) { this.residenceCountry = residenceCountry; }

    public String getResidenceState() { return residenceState; }
    public void setResidenceState(String residenceState) { this.residenceState = residenceState; }

    public String getResidenceDistrict() { return residenceDistrict; }
    public void setResidenceDistrict(String residenceDistrict) { this.residenceDistrict = residenceDistrict; }

    public String getResidenceCity() { return residenceCity; }
    public void setResidenceCity(String residenceCity) { this.residenceCity = residenceCity; }

    public String getResidencePinCode() { return residencePinCode; }
    public void setResidencePinCode(String residencePinCode) { this.residencePinCode = residencePinCode; }

    // Fees
    public Double getTotalFees() { return totalFees; }
    public void setTotalFees(Double totalFees) { this.totalFees = totalFees; }

    public Double getPaidFees() { return paidFees; }
    public void setPaidFees(Double paidFees) { this.paidFees = paidFees; }

    public Double getPendingFees() { return pendingFees; }
    public void setPendingFees(Double pendingFees) { this.pendingFees = pendingFees; }

    public String getAdmissionStatus() { return admissionStatus; }
    public void setAdmissionStatus(String admissionStatus) { this.admissionStatus = admissionStatus; }

    public String getProgramLevel() { return programLevel; }
    public void setProgramLevel(String programLevel) { this.programLevel = programLevel; }

    public Boolean getOnboardingCompleted() { return onboardingCompleted; }
    public void setOnboardingCompleted(Boolean onboardingCompleted) { this.onboardingCompleted = onboardingCompleted; }

    // Relations
    public ClassRoom getClassRoom() { return classRoom; }
    public void setClassRoom(ClassRoom classRoom) { this.classRoom = classRoom; }

    public Batch getBatch() { return batch; }
    public void setBatch(Batch batch) { this.batch = batch; }

    public Admin getAdmin() { return admin; }
    public void setAdmin(Admin admin) { this.admin = admin; }
}
