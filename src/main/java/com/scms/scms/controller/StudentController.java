package com.scms.scms.controller;

import com.scms.scms.model.Admin;
import com.scms.scms.model.Assignment;
import com.scms.scms.model.AssignmentSubmission;
import com.scms.scms.model.College;
import com.scms.scms.model.ClassRoom;
import com.scms.scms.model.Course;
import com.scms.scms.model.Batch;
import com.scms.scms.model.Fees;
import com.scms.scms.model.PlacementApplication;
import com.scms.scms.model.PlacementDrive;
import com.scms.scms.model.PlacementInterview;
import com.scms.scms.model.Student;
import com.scms.scms.model.StudentDocument;
import com.scms.scms.model.Subject;
import com.scms.scms.model.Teacher;
import com.scms.scms.model.TeacherAttendanceEntry;
import com.scms.scms.model.Timetable;
import com.scms.scms.repository.CourseRepository;
import com.scms.scms.repository.ClassRoomRepository;
import com.scms.scms.repository.AssignmentRepository;
import com.scms.scms.repository.AssignmentSubmissionRepository;
import com.scms.scms.repository.FeesRepository;
import com.scms.scms.repository.PlacementApplicationRepository;
import com.scms.scms.repository.PlacementInterviewRepository;
import com.scms.scms.repository.StudentDocumentRepository;
import com.scms.scms.repository.StudentRepository;
import com.scms.scms.repository.SubjectRepository;
import com.scms.scms.repository.TeacherAttendanceEntryRepository;
import com.scms.scms.repository.TimetableRepository;
import com.scms.scms.service.AcademicStructureService;
import com.scms.scms.service.ExamAutomationService;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
public class StudentController {

    private static final Locale INDIA = new Locale("en", "IN");
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getNumberInstance(INDIA);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATE_WITH_DAY_FORMAT = DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy");
    private static final DateTimeFormatter TIME_DISPLAY_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    private static final String LUNCH_BREAK_START = "13:00";
    private static final String LUNCH_BREAK_END = "14:00";
    private static final String ASSIGNMENT_SUBMISSION_DIR = "assignment-submissions";
    private static final List<String> INDIA_STATES = List.of(
            "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh", "Goa", "Gujarat",
            "Haryana", "Himachal Pradesh", "Jharkhand", "Karnataka", "Kerala", "Madhya Pradesh",
            "Maharashtra", "Manipur", "Meghalaya", "Mizoram", "Nagaland", "Odisha", "Punjab",
            "Rajasthan", "Sikkim", "Tamil Nadu", "Telangana", "Tripura", "Uttar Pradesh",
            "Uttarakhand", "West Bengal", "Andaman and Nicobar Islands", "Chandigarh",
            "Dadra and Nagar Haveli and Daman and Diu", "Delhi", "Jammu and Kashmir", "Ladakh",
            "Lakshadweep", "Puducherry"
    );
    private static final List<String> WORKING_DAYS = List.of(
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    );

    @Autowired private StudentRepository studentRepository;
    @Autowired private TimetableRepository timetableRepository;
    @Autowired private ClassRoomRepository classRoomRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private AssignmentSubmissionRepository assignmentSubmissionRepository;
    @Autowired private FeesRepository feesRepository;
    @Autowired private PlacementApplicationRepository placementApplicationRepository;
    @Autowired private PlacementInterviewRepository placementInterviewRepository;
    @Autowired private StudentDocumentRepository studentDocumentRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private TeacherAttendanceEntryRepository teacherAttendanceEntryRepository;
    @Autowired private AcademicStructureService academicStructureService;
    @Autowired private ExamAutomationService examAutomationService;

    @GetMapping("/student-dashboard")
    public String dashboard(Model model, HttpSession session) {
        Student student = prepareStudentPage(model, session);
        if (student == null) return "redirect:/login";
        if (requiresOnboarding(student)) return "redirect:/student-onboarding/" + resolveCollegeCode(student);
        return "student/connected/dashboard";
    }

    @GetMapping({"/student-onboarding", "/student-onboarding/{collegeCode}"})
    public String onboarding(@PathVariable(value = "collegeCode", required = false) String collegeCode,
                             Model model,
                             HttpSession session) {
        Student student = getLoggedStudent(session);
        if (student == null) return "redirect:/login";
        if (!requiresOnboarding(student) && !hasDocumentReuploadRequest(student)) {
            return "redirect:/student-dashboard";
        }

        if (collegeCode != null && !collegeCode.isBlank()) {
            String resolvedCollegeCode = resolveCollegeCode(student);
            if (resolvedCollegeCode != null && !resolvedCollegeCode.equalsIgnoreCase(collegeCode.trim())) {
                return "redirect:/student-onboarding/" + resolvedCollegeCode;
            }
        }

        ensureApplicationNumber(student);

        List<Course> availableCourses = courseRepository.findByAdminOrderByCodeAsc(student.getAdmin());
        model.addAttribute("student", student);
        model.addAttribute("studentName", defaultText(student.getName(), "Student"));
        model.addAttribute("studentEmail", defaultText(student.getEmail(), "Not available"));
        model.addAttribute("collegeName", resolveCollegeName(student.getAdmin()));
        model.addAttribute("collegeCode", resolveCollegeCode(student));
        model.addAttribute("collegeLogoPath", resolveCollegeLogo(student.getAdmin()));
        model.addAttribute("programLevels", List.of("UNDERGRADUATE", "GRADUATE", "POSTGRADUATE", "DIPLOMA"));
        model.addAttribute("availableCourses", availableCourses);
        model.addAttribute("selectedCourseLabel", resolveCourseLabel(student, availableCourses));
        model.addAttribute("availableCourseCount", availableCourses.size());
        model.addAttribute("dashboardGreeting", dashboardGreeting());
        model.addAttribute("studentMobile", defaultText(student.getMobileNumber(), ""));
        model.addAttribute("indiaStates", INDIA_STATES);
        model.addAttribute("studentCurrentHouseNumber", defaultText(student.getCorrespondingHouseNumber(), ""));
        model.addAttribute("studentCurrentCountry", defaultText(student.getCorrespondingCountry(), "INDIA"));
        model.addAttribute("studentCurrentState", defaultText(student.getCorrespondingState(), ""));
        model.addAttribute("studentCurrentDistrictOptions", districtOptionsForState(student.getCorrespondingState()));
        model.addAttribute("studentCurrentDistrict", defaultText(student.getCorrespondingDistrict(), ""));
        model.addAttribute("studentCurrentCity", defaultText(student.getCorrespondingCity(), ""));
        model.addAttribute("studentCurrentPinCode", defaultText(student.getCorrespondingPinCode(), ""));
        model.addAttribute("studentCurrentAddress", defaultText(student.getCorrespondingAddress(), ""));
        model.addAttribute("studentPermanentHouseNumber", defaultText(student.getResidenceHouseNumber(), ""));
        model.addAttribute("studentPermanentCountry", defaultText(student.getResidenceCountry(), "INDIA"));
        model.addAttribute("studentPermanentState", defaultText(student.getResidenceState(), ""));
        model.addAttribute("studentPermanentDistrictOptions", districtOptionsForState(student.getResidenceState()));
        model.addAttribute("studentPermanentDistrict", defaultText(student.getResidenceDistrict(), ""));
        model.addAttribute("studentPermanentCity", defaultText(student.getResidenceCity(), ""));
        model.addAttribute("studentPermanentPinCode", defaultText(student.getResidencePinCode(), ""));
        model.addAttribute("studentPermanentAddress", defaultText(student.getResidenceAddress(), ""));
        model.addAttribute("studentProgramLevel", defaultText(student.getProgramLevel(), ""));
        model.addAttribute("studentPhoto", student.getPhoto());
        model.addAttribute("studentSignature", student.getSignature());
        List<StudentDocument> studentDocuments = loadStudentDocuments(student);
        model.addAttribute("studentDocuments", studentDocuments);
        model.addAttribute("studentDocumentReviewRequests", studentDocuments.stream()
                .filter(doc -> "REUPLOAD_REQUESTED".equalsIgnoreCase(defaultText(doc.getStatus(), "")))
                .collect(Collectors.toList()));
        model.addAttribute("hasDocumentReuploadRequest", hasDocumentReuploadRequest(student));
        return "student/connected/onboarding";
    }

    @PostMapping({"/student-onboarding", "/student-onboarding/{collegeCode}"})
    public String submitOnboarding(
            @PathVariable(value = "collegeCode", required = false) String collegeCode,
            @RequestParam(required = false) String programLevel,
            @RequestParam(required = false) String course,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) String sectionName,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String middleName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String lnameAs12thStd,
            @RequestParam(required = false) String fnameAs12thStd,
            @RequestParam(required = false) String mnameAs12thStd,
            @RequestParam(required = false) String nameAsPerAadhaar,
            @RequestParam(required = false) String mobileNumber,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String dob,
            @RequestParam(required = false) String bloodGroup,
            @RequestParam(required = false) String maritalStatus,
            @RequestParam(required = false) String motherTongue,
            @RequestParam(required = false) String nativePlace,
            @RequestParam(required = false) String birthPlace,
            @RequestParam(required = false) String birthCountry,
            @RequestParam(required = false) String nationality,
            @RequestParam(required = false) String religion,
            @RequestParam(required = false) String categoryType,
            @RequestParam(required = false) String casteCategory,
            @RequestParam(required = false) String subCaste,
            @RequestParam(required = false) String fatherName,
            @RequestParam(required = false) String fatherOccupation,
            @RequestParam(required = false) String motherName,
            @RequestParam(required = false) Integer totalFamilyMember,
            @RequestParam(required = false) Double familyAnnualIncome,
            @RequestParam(required = false) String parentMobile,
            @RequestParam(required = false) Boolean differentlyAbled,
            @RequestParam(required = false) String aadharNumber,
            @RequestParam(required = false) Boolean sportsPerson,
            @RequestParam(required = false) String sportsAchievement,
            @RequestParam(required = false) String hobbies,
            @RequestParam(required = false) String universityPreAdmRegNo,
            @RequestParam(required = false) Integer noOfAttempt,
            @RequestParam(required = false) String inhouse,
            @RequestParam(required = false) String mediumOfInstruction,
            @RequestParam(required = false) String socialReservation,
            @RequestParam(required = false) String academicBankOfCredits,
            @RequestParam(required = false) String currentHouseNumber,
            @RequestParam(required = false) String currentAddress,
            @RequestParam(required = false) String currentCountry,
            @RequestParam(required = false) String currentState,
            @RequestParam(required = false) String currentDistrict,
            @RequestParam(required = false) String currentCity,
            @RequestParam(required = false) String currentPinCode,
            @RequestParam(required = false) Boolean sameAsPermanent,
            @RequestParam(required = false) String permanentHouseNumber,
            @RequestParam(required = false) String permanentAddress,
            @RequestParam(required = false) String permanentCountry,
            @RequestParam(required = false) String permanentState,
            @RequestParam(required = false) String permanentDistrict,
            @RequestParam(required = false) String permanentCity,
            @RequestParam(required = false) String permanentPinCode,
            @RequestParam(required = false) String rollNo,
            @RequestParam(required = false) String registrationNo,
            @RequestParam(required = false) String prnNumber,
            @RequestParam(required = false) String abcNumber,
            @RequestParam(required = false) MultipartFile photo,
            @RequestParam(required = false) MultipartFile signature,
            @RequestParam(value = "documentNames", required = false) String[] documentNames,
            @RequestParam(value = "documentFiles", required = false) MultipartFile[] documentFiles,
            HttpServletRequest request,
            HttpSession session,
            RedirectAttributes redirectAttributes) throws IOException {

        Student student = getLoggedStudent(session);
        if (student == null) return "redirect:/login";

        Course selectedCourse = null;
        String selectedCourseCode = blankToNull(course);
        if (selectedCourseCode == null) {
            redirectAttributes.addFlashAttribute("error", "Please choose a course before continuing.");
            return redirectToOnboarding(collegeCode, student);
        }
        selectedCourse = courseRepository.findByAdminAndCodeIgnoreCase(student.getAdmin(), selectedCourseCode);
        if (selectedCourse == null) {
            redirectAttributes.addFlashAttribute("error", "Selected course is not valid for this college.");
            return redirectToOnboarding(collegeCode, student);
        }

        student.setProgramLevel(blankToNull(programLevel));
        student.setCourse(selectedCourse.getCode());
        student.setDegree(blankToNull(programLevel));
        student.setSemester(blankToNull(semester));
        student.setSectionName(blankToNull(sectionName));
        student.setTitle(blankToNull(title));
        student.setFirstName(blankToNull(firstName));
        student.setMiddleName(blankToNull(middleName));
        student.setLastName(blankToNull(lastName));
        student.setLnameAs12thStd(blankToNull(lnameAs12thStd));
        student.setFnameAs12thStd(blankToNull(fnameAs12thStd));
        student.setMnameAs12thStd(blankToNull(mnameAs12thStd));
        student.setNameAsPerAadhaar(blankToNull(nameAsPerAadhaar));
        student.setMobileNumber(blankToNull(mobileNumber));
        student.setPhoneNumber(blankToNull(phoneNumber));
        student.setGender(blankToNull(gender));
        student.setBloodGroup(blankToNull(bloodGroup));
        student.setMaritalStatus(blankToNull(maritalStatus));
        student.setMotherTongue(blankToNull(motherTongue));
        student.setNativePlace(blankToNull(nativePlace));
        student.setBirthPlace(blankToNull(birthPlace));
        student.setBirthCountry(blankToNull(birthCountry));
        student.setNationality(blankToNull(nationality));
        student.setReligion(blankToNull(religion));
        student.setCategoryType(blankToNull(categoryType));
        student.setCategory(blankToNull(categoryType));
        student.setCasteCategory(blankToNull(casteCategory));
        student.setSubCaste(blankToNull(subCaste));
        student.setFatherName(blankToNull(fatherName));
        student.setFatherOccupation(blankToNull(fatherOccupation));
        student.setMotherName(blankToNull(motherName));
        student.setTotalFamilyMember(totalFamilyMember);
        student.setFamilyAnnualIncome(familyAnnualIncome);
        student.setParentMobile(blankToNull(parentMobile));
        student.setDifferentlyAbled(Boolean.TRUE.equals(differentlyAbled));
        student.setAadharNumber(blankToNull(aadharNumber));
        student.setSportsPerson(Boolean.TRUE.equals(sportsPerson));
        student.setSportsAchievement(blankToNull(sportsAchievement));
        student.setHobbies(blankToNull(hobbies));
        student.setUniversityPreAdmRegNo(blankToNull(universityPreAdmRegNo));
        student.setNoOfAttempt(noOfAttempt);
        student.setInhouse(blankToNull(inhouse));
        student.setMediumOfInstruction(blankToNull(mediumOfInstruction));
        student.setSocialReservation(blankToNull(socialReservation));
        student.setAcademicBankOfCredits(blankToNull(academicBankOfCredits));
        student.setCorrespondingHouseNumber(blankToNull(currentHouseNumber));
        student.setCorrespondingAddress(blankToNull(currentAddress));
        student.setCorrespondingCountry(blankToNull(currentCountry));
        student.setCorrespondingState(blankToNull(currentState));
        student.setCorrespondingDistrict(blankToNull(currentDistrict));
        student.setCorrespondingCity(blankToNull(currentCity));
        student.setCorrespondingPinCode(blankToNull(currentPinCode));
        student.setResidenceHouseNumber(blankToNull(permanentHouseNumber));
        student.setResidenceAddress(blankToNull(permanentAddress));
        student.setResidenceCountry(blankToNull(permanentCountry));
        student.setResidenceState(blankToNull(permanentState));
        student.setResidenceDistrict(blankToNull(permanentDistrict));
        student.setResidenceCity(blankToNull(permanentCity));
        student.setResidencePinCode(blankToNull(permanentPinCode));

        boolean copyPermanentToCurrent = Boolean.TRUE.equals(sameAsPermanent);
        if (copyPermanentToCurrent) {
            currentHouseNumber = permanentHouseNumber;
            currentAddress = permanentAddress;
            currentCountry = permanentCountry;
            currentState = permanentState;
            currentDistrict = permanentDistrict;
            currentCity = permanentCity;
            currentPinCode = permanentPinCode;
            student.setCorrespondingHouseNumber(blankToNull(currentHouseNumber));
            student.setCorrespondingAddress(blankToNull(currentAddress));
            student.setCorrespondingCountry(blankToNull(currentCountry));
            student.setCorrespondingState(blankToNull(currentState));
            student.setCorrespondingDistrict(blankToNull(currentDistrict));
            student.setCorrespondingCity(blankToNull(currentCity));
            student.setCorrespondingPinCode(blankToNull(currentPinCode));
        }
        student.setCurrentAddress(composeAddress(currentHouseNumber, currentAddress, currentCity, currentDistrict, currentState, currentCountry, currentPinCode));
        student.setPermanentAddress(composeAddress(permanentHouseNumber, permanentAddress, permanentCity, permanentDistrict, permanentState, permanentCountry, permanentPinCode));
        student.setName(composeStudentName(firstName, middleName, lastName, student.getName()));
        student.setRollNo(blankToNull(rollNo));
        student.setRegistrationNo(blankToNull(registrationNo));
        student.setPrnNumber(blankToNull(prnNumber));
        student.setAbcNumber(blankToNull(abcNumber));
        if (dob != null && !dob.isBlank()) {
            try { student.setDob(LocalDate.parse(dob)); } catch (Exception ignored) {}
        }
        if (photo != null && !photo.isEmpty()) {
            String saved = saveStudentUpload(photo, "students");
            if (saved != null) student.setPhoto(saved);
        }
        if (signature != null && !signature.isEmpty()) {
            String saved = saveStudentUpload(signature, "students");
            if (saved != null) student.setSignature(saved);
        }

        persistStudentDocuments(student, documentNames, documentFiles);

        applyExtendedStudentFields(student, request);
        ensureApplicationNumber(student);

        student.setOnboardingCompleted(Boolean.TRUE);
        student.setAdmissionStatus("ADMITTED");
        studentRepository.save(student);
        session.invalidate();
        redirectAttributes.addFlashAttribute("success", "Registration completed successfully. Please login again.");
        return "redirect:/login";
    }

    private String redirectToOnboarding(String collegeCode, Student student) {
        String resolvedCollegeCode = blankToNull(collegeCode);
        if (resolvedCollegeCode == null) {
            resolvedCollegeCode = resolveCollegeCode(student);
        }
        return resolvedCollegeCode != null
                ? "redirect:/student-onboarding/" + resolvedCollegeCode
                : "redirect:/student-onboarding";
    }

    @GetMapping("/exam-results")
    public String examResults(Model model, HttpSession session) {
        Student student = prepareStudentPage(model, session);
        if (student == null) return "redirect:/login";
        model.addAttribute("resultsAvailable", false);
        return "student/connected/exam-results";
    }

    @GetMapping("/timetable")
    public String timetable(Model model, HttpSession session) {
        Student student = prepareStudentPage(model, session);
        if (student == null) return "redirect:/login";
        return "student/connected/timetable";
    }

    @GetMapping("/subjects")
    public String subjects(Model model, HttpSession session) {
        Student student = prepareStudentPage(model, session);
        if (student == null) return "redirect:/login";
        return "student/connected/subjects";
    }

    @GetMapping("/attendance")
    public String attendance(Model model, HttpSession session) {
        Student student = prepareStudentPage(model, session);
        if (student == null) return "redirect:/login";
        addStudentAttendanceAttributes(model, student);
        return "student/connected/attendance";
    }

    @GetMapping("/assignments")
    public String assignments(Model model, HttpSession session) {
        Student student = prepareStudentPage(model, session);
        if (student == null) return "redirect:/login";
        List<Assignment> assignments = loadStudentAssignments(student);
        Map<Long, AssignmentSubmission> submissionsByAssignment = loadStudentAssignmentSubmissionMap(student, assignments);
        model.addAttribute("studentAssignments", assignments);
        model.addAttribute("studentAssignmentSubmissions", submissionsByAssignment);
        model.addAttribute("assignmentsAvailable", !assignments.isEmpty());
        model.addAttribute("assignmentCount", assignments.size());
        model.addAttribute("assignmentOpenCount", assignments.stream()
                .filter(assignment -> assignment.getDueDate() == null || !assignment.getDueDate().isBefore(LocalDate.now()))
                .count());
        model.addAttribute("assignmentSubmittedCount", submissionsByAssignment.size());
        return "student/connected/assignments";
    }

    @PostMapping("/assignments/{assignmentId}/submit")
    public String submitAssignment(@PathVariable Long assignmentId,
                                   @RequestParam("submissionFile") MultipartFile submissionFile,
                                   @RequestParam(value = "notes", required = false) String notes,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        Student student = getLoggedStudent(session);
        if (student == null) return "redirect:/login";

        Assignment assignment = assignmentRepository.findById(assignmentId).orElse(null);
        if (assignment == null || !studentCanAccessAssignment(student, assignment)) {
            redirectAttributes.addFlashAttribute("assignmentError", "Assignment was not found for your class.");
            return "redirect:/assignments";
        }
        if (assignment.getDueDate() != null && assignment.getDueDate().isBefore(LocalDate.now())) {
            redirectAttributes.addFlashAttribute("assignmentError", "The due date has passed for this assignment.");
            return "redirect:/assignments";
        }
        if (submissionFile == null || submissionFile.isEmpty()) {
            redirectAttributes.addFlashAttribute("assignmentError", "Choose a file before submitting.");
            return "redirect:/assignments";
        }

        String savedPath;
        try {
            savedPath = saveStudentUpload(submissionFile, ASSIGNMENT_SUBMISSION_DIR);
        } catch (IOException ex) {
            redirectAttributes.addFlashAttribute("assignmentError", "Submission upload failed. Use a smaller PDF, DOC, DOCX, image, ZIP, or TXT file.");
            return "redirect:/assignments";
        }

        AssignmentSubmission submission = assignmentSubmissionRepository.findByAssignmentAndStudent(assignment, student)
                .orElseGet(AssignmentSubmission::new);
        submission.setAssignment(assignment);
        submission.setStudent(student);
        submission.setAdmin(student.getAdmin());
        submission.setOriginalFileName(submissionFile.getOriginalFilename());
        submission.setStoredFileName(savedPath.substring(savedPath.lastIndexOf('/') + 1));
        submission.setFilePath(savedPath);
        submission.setContentType(submissionFile.getContentType());
        submission.setFileSize(submissionFile.getSize());
        submission.setNotes(notes != null ? notes.trim() : "");
        submission.setStatus("SUBMITTED");
        submission.setSubmittedAt(LocalDateTime.now());
        assignmentSubmissionRepository.save(submission);

        redirectAttributes.addFlashAttribute("assignmentSuccess", "Assignment submitted successfully.");
        return "redirect:/assignments";
    }

    @GetMapping("/assignments/file/{assignmentId}")
    public ResponseEntity<Resource> downloadAssignmentFile(@PathVariable Long assignmentId, HttpSession session) {
        Student student = getLoggedStudent(session);
        if (student == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Assignment assignment = assignmentRepository.findById(assignmentId).orElse(null);
        if (assignment == null || !studentCanAccessAssignment(student, assignment)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return serveStudentFile(assignment.getFilePath(), assignment.getOriginalFileName(), assignment.getContentType());
    }

    @GetMapping("/assignments/submission/{submissionId}/download")
    public ResponseEntity<Resource> downloadAssignmentSubmission(@PathVariable Long submissionId, HttpSession session) {
        Student student = getLoggedStudent(session);
        if (student == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        AssignmentSubmission submission = assignmentSubmissionRepository.findById(submissionId).orElse(null);
        if (submission == null || submission.getStudent() == null || !Objects.equals(submission.getStudent().getId(), student.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return serveStudentFile(submission.getFilePath(), submission.getOriginalFileName(), submission.getContentType());
    }

    @GetMapping({"/profile", "/student-profile"})
    public String profile(Model model, HttpSession session) {
        Student student = prepareStudentPage(model, session);
        if (student == null) return "redirect:/login";
        return "student/connected/profile";
    }

    @GetMapping("/fees")
    public String fees(Model model, HttpSession session) {
        Student student = prepareStudentPage(model, session);
        if (student == null) return "redirect:/login";
        return "student/connected/fees";
    }

    @GetMapping("/notifications")
    public String notifications(Model model, HttpSession session) {
        Student student = prepareStudentPage(model, session);
        if (student == null) return "redirect:/login";
        return "student/connected/notifications";
    }

    @GetMapping("/student-marks")
    public String marks(Model model, HttpSession session) {
        Student student = prepareStudentPage(model, session);
        if (student == null) return "redirect:/login";
        model.addAttribute("resultsAvailable", false);
        return "student/connected/student-marks";
    }

    @GetMapping("/student-attendance")
    public String stuAtt(Model model, HttpSession session) {
        Student student = prepareStudentPage(model, session);
        if (student == null) return "redirect:/login";
        addStudentAttendanceAttributes(model, student);
        return "student/connected/student-attendance";
    }

    private List<Assignment> loadStudentAssignments(Student student) {
        if (student == null) {
            return List.of();
        }
        ClassRoom classRoom = student.getClassRoom();
        List<Assignment> assignments = classRoom != null
                ? assignmentRepository.findByClassRoomAndPublishedTrueOrderByDueDateAscCreatedAtDesc(classRoom)
                : List.of();
        if (assignments.isEmpty() && student.getAdmin() != null) {
            assignments = assignmentRepository.findByAdminOrderByCreatedAtDesc(student.getAdmin()).stream()
                    .filter(Assignment::isPublished)
                    .filter(assignment -> studentCanAccessAssignment(student, assignment))
                    .toList();
        }
        return assignments.stream()
                .filter(assignment -> studentCanAccessAssignment(student, assignment))
                .sorted(Comparator
                        .comparing(Assignment::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Assignment::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private Map<Long, AssignmentSubmission> loadStudentAssignmentSubmissionMap(Student student, List<Assignment> assignments) {
        LinkedHashMap<Long, AssignmentSubmission> submissions = new LinkedHashMap<>();
        if (student == null || assignments == null) {
            return submissions;
        }
        for (Assignment assignment : assignments) {
            assignmentSubmissionRepository.findByAssignmentAndStudent(assignment, student)
                    .ifPresent(submission -> submissions.put(assignment.getId(), submission));
        }
        return submissions;
    }

    private void addStudentAttendanceAttributes(Model model, Student student) {
        List<TeacherAttendanceEntry> entries = teacherAttendanceEntryRepository.findByStudentOrderByMarkedAtDesc(student);
        LinkedHashMap<String, AttendanceSummaryBuilder> grouped = new LinkedHashMap<>();
        for (TeacherAttendanceEntry entry : entries) {
            if (entry == null || entry.getSession() == null) {
                continue;
            }
            String subject = defaultText(entry.getSession().getSubject(), "Unassigned");
            grouped.computeIfAbsent(subject, AttendanceSummaryBuilder::new).add(entry);
        }

        List<StudentAttendanceSummaryView> subjectAttendanceSummaries = grouped.values().stream()
                .map(AttendanceSummaryBuilder::build)
                .sorted(Comparator.comparing(StudentAttendanceSummaryView::getSubject, String.CASE_INSENSITIVE_ORDER))
                .toList();
        int totalClasses = subjectAttendanceSummaries.stream().mapToInt(StudentAttendanceSummaryView::getTotal).sum();
        int attendedClasses = subjectAttendanceSummaries.stream().mapToInt(StudentAttendanceSummaryView::getAttended).sum();
        int absentClasses = subjectAttendanceSummaries.stream().mapToInt(StudentAttendanceSummaryView::getAbsent).sum();
        int lateClasses = subjectAttendanceSummaries.stream().mapToInt(StudentAttendanceSummaryView::getLate).sum();
        int overallPercentage = totalClasses > 0 ? Math.round((attendedClasses * 100f) / totalClasses) : 0;

        model.addAttribute("attendanceAvailable", totalClasses > 0);
        model.addAttribute("attendanceRecordCount", totalClasses);
        model.addAttribute("attendanceAttendedCount", attendedClasses);
        model.addAttribute("attendanceAbsentCount", absentClasses);
        model.addAttribute("attendanceLateCount", lateClasses);
        model.addAttribute("attendanceOverallPercentage", overallPercentage);
        model.addAttribute("subjectAttendanceSummaries", subjectAttendanceSummaries);
        model.addAttribute("recentAttendanceEntries", entries.stream().limit(12).toList());
    }

    private boolean studentCanAccessAssignment(Student student, Assignment assignment) {
        if (student == null || assignment == null || !assignment.isPublished()) {
            return false;
        }
        if (student.getClassRoom() != null && assignment.getClassRoom() != null) {
            return Objects.equals(student.getClassRoom().getId(), assignment.getClassRoom().getId());
        }
        if (defaultText(student.getCourse(), null) != null && defaultText(assignment.getCourse(), null) != null
                && !studentCourseMatchesAssignment(student, assignment)) {
            return false;
        }
        Integer studentSemester = parseSemesterNumber(student.getSemester());
        Integer assignmentSemester = parseSemesterNumber(assignment.getSemester());
        if (studentSemester != null && assignmentSemester != null && !studentSemester.equals(assignmentSemester)) {
            return false;
        }
        if (defaultText(student.getAcademicYear(), null) != null && defaultText(assignment.getAcademicYear(), null) != null
                && !academicYearMatches(student.getAcademicYear(), assignment.getAcademicYear())) {
            return false;
        }
        if (defaultText(student.getSectionName(), null) != null && defaultText(assignment.getSectionName(), null) != null
                && !student.getSectionName().equalsIgnoreCase(assignment.getSectionName())) {
            return false;
        }
        return true;
    }

    private boolean studentCourseMatchesAssignment(Student student, Assignment assignment) {
        String studentCourse = defaultText(student.getCourse(), null);
        String assignmentCourse = defaultText(assignment.getCourse(), null);
        if (studentCourse == null || assignmentCourse == null) {
            return true;
        }
        if (studentCourse.equalsIgnoreCase(assignmentCourse)) {
            return true;
        }
        if (student.getClassRoom() != null) {
            String classCourse = defaultText(student.getClassRoom().getCourse(), null);
            String classCourseCode = defaultText(student.getClassRoom().getCourseCode(), null);
            return assignmentCourse.equalsIgnoreCase(defaultText(classCourse, ""))
                    || assignmentCourse.equalsIgnoreCase(defaultText(classCourseCode, ""));
        }
        return false;
    }

    private ResponseEntity<Resource> serveStudentFile(String storedPath, String fileName, String contentType) {
        try {
            Path resolved = resolvePhysicalPath(storedPath);
            if (resolved == null || !Files.exists(resolved)) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new UrlResource(resolved.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            String safeType = defaultText(contentType, Files.probeContentType(resolved));
            if (safeType == null) {
                safeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(safeType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment()
                                    .filename(defaultText(fileName, resolved.getFileName().toString()))
                                    .build()
                                    .toString())
                    .body(resource);
        } catch (MalformedURLException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Path resolvePhysicalPath(String storedPath) {
        String normalizedPath = blankToNull(storedPath);
        if (normalizedPath == null || !normalizedPath.startsWith("/uploads/")) {
            return null;
        }
        Path staticRoot = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "static").normalize();
        Path target = staticRoot.resolve(normalizedPath.substring(1)).normalize();
        if (!target.startsWith(staticRoot)) {
            return null;
        }
        return target;
    }

    private Student prepareStudentPage(Model model, HttpSession session) {
        Student student = getLoggedStudent(session);
        if (student == null) return null;

        ensureApplicationNumber(student);
        AcademicStructureService.StudentProgression progression = academicStructureService.syncStudentProgression(student);
        StudentPortalData portalData = buildPortalData(student);

        model.addAttribute("student", student);
        model.addAttribute("studentName", defaultText(student.getName(), "Student"));
        model.addAttribute("studentCourse", defaultText(student.getCourse(), "Student"));
        model.addAttribute("studentEmail", defaultText(student.getEmail(), "Not available"));
        model.addAttribute("collegeName", resolveCollegeName(student.getAdmin()));
        model.addAttribute("collegeLogoPath", resolveCollegeLogo(student.getAdmin()));
        model.addAttribute("collegeInitials", buildCollegeInitials(resolveCollegeName(student.getAdmin())));
        model.addAttribute("dashboardGreeting", dashboardGreeting());
        model.addAttribute("dashboardAcademicYear", defaultText(progression.academicYear(), "Academic Year"));
        model.addAttribute("studentId", portalData.studentId());
        model.addAttribute("studentRollNo", portalData.rollNo());
        model.addAttribute("studentInitials", portalData.initials());
        model.addAttribute("studentSemesterLabel", defaultText(progression.semesterLabel(), portalData.semesterLabel()));
        model.addAttribute("studentSectionLabel", portalData.sectionLabel());
        model.addAttribute("studentAcademicYear", defaultText(progression.academicYear(), portalData.academicYear()));
        model.addAttribute("studentBatchYearLabel", portalData.batchYearLabel());
        model.addAttribute("studentBatchLabel", portalData.batchYearLabel());
        model.addAttribute("studentClassLabel", portalData.classLabel());
        model.addAttribute("studentClassMentor", portalData.classMentor());
        model.addAttribute("studentYearTypeLabel", progression.yearLabel());
        model.addAttribute("studentCurrentSemesterNumber", progression.semesterNumber());
        model.addAttribute("studentProgressCourseLabel", progression.courseLabel());
        model.addAttribute("studentProgressBatchLabel", progression.batchLabel());
        model.addAttribute("studentPrimaryRoom", portalData.primaryRoom());
        model.addAttribute("studentPhoto", student.getPhoto());
        model.addAttribute("collegeCode", resolveCollegeCode(student));
        model.addAttribute("studentMobile", defaultText(student.getMobileNumber(), "Not provided"));
        model.addAttribute("studentSignature", student.getSignature());
        model.addAttribute("studentCurrentAddress", defaultText(student.getCurrentAddress(), "Not provided"));
        model.addAttribute("studentPermanentAddress", defaultText(student.getPermanentAddress(), "Not provided"));
        model.addAttribute("studentProgramLevel", defaultText(student.getProgramLevel(), "Not provided"));
        model.addAttribute("studentDob", formatDate(student.getDob()));
        model.addAttribute("studentAdmissionDate", formatDate(student.getAdmissionDate()));
        model.addAttribute("studentGender", defaultText(student.getGender(), "Not provided"));
        model.addAttribute("studentBloodGroup", defaultText(student.getBloodGroup(), "Not provided"));
        model.addAttribute("studentCategory", defaultText(student.getCategory(), "Not provided"));
        model.addAttribute("studentReligion", defaultText(student.getReligion(), "Not provided"));
        model.addAttribute("studentAadharMasked", maskValue(student.getAadharNumber(), 4));
        model.addAttribute("studentFatherName", defaultText(student.getFatherName(), "Not provided"));
        model.addAttribute("studentMotherName", defaultText(student.getMotherName(), "Not provided"));
        model.addAttribute("studentGuardianName", defaultText(student.getGuardianName(), "Not provided"));
        model.addAttribute("studentBankName", defaultText(student.getBankName(), "Not provided"));
        model.addAttribute("studentBankAccMasked", maskValue(student.getBankAccNo(), 4));
        model.addAttribute("studentIfscCode", defaultText(student.getIfscCode(), "Not provided"));
        model.addAttribute("studentPrnNumber", defaultText(student.getPrnNumber(), "Not provided"));
        model.addAttribute("studentEnrollmentNo", defaultText(student.getEnrollmentNo(), "Not provided"));
        model.addAttribute("studentRegistrationNo", defaultText(student.getRegistrationNo(), "Not provided"));
        model.addAttribute("studentAbcNumber", defaultText(student.getAbcNumber(), "Not provided"));
        model.addAttribute("studentStatus", defaultText(student.getAdmissionStatus(), "ADMITTED"));
        List<StudentDocument> studentDocuments = loadStudentDocuments(student);
        model.addAttribute("studentDocuments", studentDocuments);
        model.addAttribute("studentDocumentReviewRequests", studentDocuments.stream()
                .filter(doc -> "REUPLOAD_REQUESTED".equalsIgnoreCase(defaultText(doc.getStatus(), "")))
                .collect(Collectors.toList()));
        model.addAttribute("hasDocumentReuploadRequest", hasDocumentReuploadRequest(student));

        model.addAttribute("subjectCount", portalData.subjectSummaries().size());
        model.addAttribute("subjectCatalogCount", portalData.subjectCatalogRows().size());
        model.addAttribute("weeklyClassCount", portalData.allTimetableEntries().size());
        model.addAttribute("todayScheduleCount", portalData.todaySchedule().size());
        model.addAttribute("activeAcademicDays", portalData.activeAcademicDays());
        model.addAttribute("todayCompletedCount", portalData.todayCompletedCount());
        model.addAttribute("todayUpcomingCount", portalData.todayUpcomingCount());
        model.addAttribute("todayCurrentSlot", portalData.currentSlot());
        model.addAttribute("todayNextSlot", portalData.nextSlot());
        model.addAttribute("subjectSummaries", portalData.subjectSummaries());
        model.addAttribute("subjectCatalogRows", portalData.subjectCatalogRows());
        model.addAttribute("todaySchedule", portalData.todaySchedule());
        model.addAttribute("weeklySchedule", portalData.weeklySchedule());
        model.addAttribute("weeklyTimetableRows", portalData.weeklyTimetableRows());
        model.addAttribute("workingDayNames", WORKING_DAYS);
        model.addAttribute("todayLabel", portalData.todayLabel());
        model.addAttribute("todayDayKey", portalData.todayDayKey());
        model.addAttribute("todayFullDate", portalData.todayFullDate());

        model.addAttribute("feesTotal", portalData.totalFees());
        model.addAttribute("feesPaid", portalData.paidFees());
        model.addAttribute("feesPending", portalData.pendingFees());
        model.addAttribute("feesTotalFormatted", portalData.totalFeesFormatted());
        model.addAttribute("feesPaidFormatted", portalData.paidFeesFormatted());
        model.addAttribute("feesPendingFormatted", portalData.pendingFeesFormatted());
        model.addAttribute("feesPaidPercentage", portalData.paidPercentage());
        model.addAttribute("feesDueStatus", portalData.feeStatus());

        model.addAttribute("studentNotifications", portalData.notifications());
        model.addAttribute("dashboardNotifications", portalData.notifications().stream().limit(4).toList());
        model.addAttribute("notificationCount", portalData.notifications().size());
        model.addAttribute("placementInterviewNotifications", portalData.placementNotifications());
        model.addAttribute("placementInterviewNotificationCount", portalData.placementNotifications().size());
        model.addAttribute("studentUpcomingExams", examAutomationService.findVisibleUpcomingExams(student).stream().limit(3).toList());
        model.addAttribute("studentExamCount", examAutomationService.findVisibleUpcomingExams(student).size());
        model.addAttribute("studentUnreadExamNotifications", examAutomationService.countUnreadNotifications("STUDENT", student.getEmail()));

        return student;
    }

    private StudentPortalData buildPortalData(Student student) {
        List<Timetable> timetableEntries = loadStudentTimetable(student);
        List<Subject> assignedSubjects = loadAssignedSubjects(student);
        LinkedHashMap<String, List<TimetableSlotView>> weeklySchedule = buildWeeklySchedule(timetableEntries);
        String todayName = currentDayName();
        List<TimetableSlotView> todaySchedule = new ArrayList<>(weeklySchedule.getOrDefault(todayName, List.of()));
        List<SubjectSummaryView> subjectSummaries = buildSubjectSummaries(timetableEntries, assignedSubjects);
        List<SubjectCatalogRowView> subjectCatalogRows = buildSubjectCatalogRows(assignedSubjects);
        DayScheduleInsight dayScheduleInsight = buildDayScheduleInsight(todaySchedule);

        StudentFeeView feeView = resolveStudentFeeView(student);
        double totalFees = feeView.totalFees();
        double paidFees = feeView.paidFees();
        double pendingFees = feeView.pendingFees();
        int paidPercentage = feeView.paidPercentage();
        long activeAcademicDays = weeklySchedule.values().stream()
                .filter(daySlots -> !daySlots.isEmpty())
                .count();

        List<StudentNotificationView> notifications =
                buildNotifications(student, todaySchedule, subjectSummaries, pendingFees, dayScheduleInsight);
        notifications.addAll(buildExamNotifications(student));
        List<StudentNotificationView> placementNotifications = buildPlacementNotifications(student);

        return new StudentPortalData(
                resolveStudentId(student),
                defaultText(student.getRollNo(), "Not assigned"),
                buildStudentInitials(student.getName()),
                buildSemesterLabel(student),
                buildSectionLabel(student),
                defaultText(student.getAcademicYear(), "Not assigned"),
                buildBatchYearLabel(student),
                buildBatchYearLabel(student),
                buildClassLabel(student),
                resolveClassMentor(student),
                resolvePrimaryRoom(student, dayScheduleInsight.currentSlot()),
                todayName,
                todayName.substring(0, 3).toLowerCase(Locale.ENGLISH),
                LocalDate.now().format(DATE_WITH_DAY_FORMAT),
                totalFees,
                paidFees,
                pendingFees,
                formatCurrency(totalFees),
                formatCurrency(paidFees),
                formatCurrency(pendingFees),
                paidPercentage,
                feeView.status(),
                (int) activeAcademicDays,
                dayScheduleInsight.completedCount(),
                dayScheduleInsight.upcomingCount(),
                dayScheduleInsight.currentSlot(),
                dayScheduleInsight.nextSlot(),
                weeklySchedule,
                buildWeeklyTimetableRows(weeklySchedule),
                todaySchedule,
                timetableEntries,
                subjectSummaries,
                subjectCatalogRows,
                notifications,
                placementNotifications
        );
    }

    private ClassRoom resolveStudentClassRoom(Student student) {
        if (student == null) {
            return null;
        }
        ClassRoom current = student.getClassRoom();
        if (current != null && classRoomMatchesStudentScope(current, student)) {
            return current;
        }
        if (student.getAdmin() == null) {
            return current;
        }
        return classRoomRepository.findByAdmin(student.getAdmin()).stream()
                .filter(classRoom -> classRoomMatchesStudentScope(classRoom, student))
                .findFirst()
                .orElse(current);
    }

    private boolean batchMatchesClassRoom(ClassRoom classRoom, com.scms.scms.model.Batch batch) {
        if (classRoom == null || batch == null) {
            return false;
        }

        String classBatchName = defaultText(classRoom.getBatchName(), "");
        String batchName = defaultText(batch.getDisplayName(), "");
        if (!classBatchName.isBlank() && !batchName.isBlank() && classBatchName.equalsIgnoreCase(batchName)) {
            return true;
        }

        Integer classStart = classRoom.getBatchStartYear();
        Integer classEnd = classRoom.getBatchEndYear();
        return classStart != null && classEnd != null
                && batch.getStartYear() != null && batch.getEndYear() != null
                && classStart.equals(batch.getStartYear())
                && classEnd.equals(batch.getEndYear());
    }

    private boolean courseMatchesClassRoom(ClassRoom classRoom, String course) {
        if (classRoom == null || course == null || course.isBlank()) {
            return false;
        }

        String normalized = course.strip();
        return normalized.equalsIgnoreCase(defaultText(classRoom.getCourse(), ""))
                || normalized.equalsIgnoreCase(defaultText(classRoom.getCourseCode(), ""));
    }

    private boolean classRoomMatchesStudentScope(ClassRoom classRoom, Student student) {
        if (classRoom == null || student == null) {
            return false;
        }
        Batch batch = student.getBatch();
        if (batch != null && !batchMatchesClassRoom(classRoom, batch)) {
            return false;
        }
        if (blankToNull(student.getCourse()) != null && !courseMatchesClassRoom(classRoom, student.getCourse())) {
            return false;
        }
        if (blankToNull(student.getAcademicYear()) != null
                && blankToNull(classRoom.getAcademicYear()) != null
                && !academicYearMatches(student.getAcademicYear(), classRoom.getAcademicYear())) {
            return false;
        }
        Integer semesterNumber = parseSemesterNumber(student.getSemester());
        if (semesterNumber != null && classRoom.getSemester() != null && !semesterNumber.equals(classRoom.getSemester())) {
            return false;
        }
        if (blankToNull(student.getSectionName()) != null
                && blankToNull(classRoom.getSection()) != null
                && !student.getSectionName().equalsIgnoreCase(classRoom.getSection())) {
            return false;
        }
        return true;
    }

    private boolean timetableMatchesStudentScope(Timetable entry, Student student) {
        if (entry == null || student == null) {
            return false;
        }
        ClassRoom classRoom = entry.getClassRoom();
        return classRoom != null && classRoomMatchesExactStudentTimetableScope(classRoom, student, entry);
    }

    private boolean classRoomMatchesExactStudentTimetableScope(ClassRoom classRoom, Student student, Timetable entry) {
        if (classRoom == null || student == null) {
            return false;
        }

        if (student.getBatch() == null || !batchMatchesClassRoom(classRoom, student.getBatch())) {
            return false;
        }
        if (blankToNull(student.getCourse()) == null || !courseMatchesClassRoom(classRoom, student.getCourse())) {
            return false;
        }
        if (blankToNull(student.getAcademicYear()) == null
                || blankToNull(classRoom.getAcademicYear()) == null
                || !academicYearMatches(student.getAcademicYear(), classRoom.getAcademicYear())) {
            return false;
        }
        if (blankToNull(entry != null ? entry.getAcademicYear() : null) != null
                && !academicYearMatches(student.getAcademicYear(), entry.getAcademicYear())) {
            return false;
        }
        Integer semesterNumber = parseSemesterNumber(student.getSemester());
        if (semesterNumber == null || classRoom.getSemester() == null || !semesterNumber.equals(classRoom.getSemester())) {
            return false;
        }
        return blankToNull(student.getSectionName()) != null
                && blankToNull(classRoom.getSection()) != null
                && student.getSectionName().equalsIgnoreCase(classRoom.getSection());
    }

    private Integer parseSemesterNumber(String semester) {
        String normalized = defaultText(semester, "").trim();
        if (normalized.isBlank()) {
            return null;
        }
        String digits = normalized.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(digits);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean academicYearMatches(String left, String right) {
        String a = defaultText(left, null);
        String b = defaultText(right, null);
        if (a == null || b == null) {
            return a == null && b == null;
        }
        if (a.equalsIgnoreCase(b)) {
            return true;
        }
        String[] leftParts = a.replace('–', '-').split("-");
        String[] rightParts = b.replace('–', '-').split("-");
        if (leftParts.length < 2 || rightParts.length < 2) {
            return false;
        }
        try {
            int leftStart = Integer.parseInt(leftParts[0].replaceAll("[^0-9]", ""));
            int leftEnd = Integer.parseInt(leftParts[1].replaceAll("[^0-9]", ""));
            int rightStart = Integer.parseInt(rightParts[0].replaceAll("[^0-9]", ""));
            int rightEnd = Integer.parseInt(rightParts[1].replaceAll("[^0-9]", ""));

            if (leftStart < 100) leftStart = 2000 + leftStart;
            if (rightStart < 100) rightStart = 2000 + rightStart;
            if (leftEnd < 100) {
                leftEnd = (leftStart / 100) * 100 + leftEnd;
                if (leftEnd < leftStart) leftEnd += 100;
            }
            if (rightEnd < 100) {
                rightEnd = (rightStart / 100) * 100 + rightEnd;
                if (rightEnd < rightStart) rightEnd += 100;
            }
            return leftStart == rightStart && leftEnd == rightEnd;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String normalizeSemesterKey(String semester) {
        String value = blankToNull(semester);
        return value == null ? null : value.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private boolean isYearWiseFeeRule(Fees fee) {
        return fee != null
                && fee.getFeeScope() != null
                && fee.getFeeScope().equalsIgnoreCase("Year-wise");
    }

    private String resolveFeeCourseCode(Admin admin, Student student, ClassRoom classRoom) {
        String rawCourse = blankToNull(student != null ? student.getCourse() : null);
        if (classRoom != null && blankToNull(classRoom.getCourseCode()) != null) {
            rawCourse = classRoom.getCourseCode();
        } else if (classRoom != null && blankToNull(classRoom.getCourse()) != null) {
            rawCourse = classRoom.getCourse();
        }
        if (admin == null || rawCourse == null) {
            return rawCourse;
        }
        String lookupCourse = rawCourse;
        return courseRepository.findByAdminOrderByCodeAsc(admin).stream()
                .filter(course -> lookupCourse.equalsIgnoreCase(defaultText(course.getCode(), null))
                        || lookupCourse.equalsIgnoreCase(defaultText(course.getName(), null)))
                .map(Course::getCode)
                .filter(code -> code != null && !code.isBlank())
                .findFirst()
                .orElse(lookupCourse);
    }

    private boolean feeBatchMatchesStudent(Fees fee, Student student, ClassRoom classRoom, String resolvedBatchName) {
        if (fee == null) {
            return false;
        }
        Batch feeBatch = fee.getBatch();
        Batch studentBatch = student != null ? student.getBatch() : null;
        if (feeBatch == null && blankToNull(fee.getBatchName()) == null) {
            return true;
        }
        if (feeBatch != null) {
            if (studentBatch != null && studentBatch.getId() != null && studentBatch.getId().equals(feeBatch.getId())) {
                return true;
            }
            if (classRoom != null
                    && classRoom.getBatchStartYear() != null
                    && classRoom.getBatchEndYear() != null
                    && feeBatch.getStartYear() != null
                    && feeBatch.getEndYear() != null
                    && classRoom.getBatchStartYear().equals(feeBatch.getStartYear())
                    && classRoom.getBatchEndYear().equals(feeBatch.getEndYear())) {
                return true;
            }
            String feeBatchName = blankToNull(feeBatch.getDisplayName());
            if (feeBatchName != null && resolvedBatchName != null && feeBatchName.equalsIgnoreCase(resolvedBatchName)) {
                return true;
            }
        }
        String feeBatchName = blankToNull(fee.getBatchName());
        return feeBatchName != null && resolvedBatchName != null && feeBatchName.equalsIgnoreCase(resolvedBatchName);
    }

    private StudentFeeView resolveStudentFeeView(Student student) {
        double totalFees = resolveStudentTotalFees(student);
        double paidFees = safeNumber(student != null ? student.getPaidFees() : null);
        double pendingFees = Math.max(totalFees - paidFees, 0);
        int paidPercentage = totalFees > 0 ? (int) Math.round((paidFees / totalFees) * 100) : 0;
        return new StudentFeeView(totalFees, paidFees, pendingFees, paidPercentage, feeStatus(totalFees, paidFees, pendingFees));
    }

    private String feeStatus(double totalFees, double paidFees, double pendingFees) {
        if (totalFees <= 0) {
            return "Not assigned";
        }
        if (pendingFees <= 0) {
            return "Cleared";
        }
        if (paidFees > 0) {
            return "Partial";
        }
        return "Pending";
    }

    private double resolveStudentTotalFees(Student student) {
        if (student == null) {
            return 0.0;
        }

        Admin admin = student.getAdmin();
        if (admin != null) {
            ClassRoom classRoom = resolveStudentClassRoom(student);
            String resolvedBatchName = student.getBatch() != null ? student.getBatch().getDisplayName() : null;
            if ((resolvedBatchName == null || resolvedBatchName.isBlank()) && classRoom != null) {
                resolvedBatchName = classRoom.getBatchName();
            }
            String resolvedAcademicYear = blankToNull(student.getAcademicYear());
            if (resolvedAcademicYear == null && classRoom != null) {
                resolvedAcademicYear = classRoom.getAcademicYear();
            }
            String resolvedSemester = blankToNull(student.getSemester());
            if (resolvedSemester == null && classRoom != null && classRoom.getSemester() != null) {
                resolvedSemester = "Sem " + classRoom.getSemester();
            }
            String resolvedCourse = resolveFeeCourseCode(admin, student, classRoom);
            Fees fee = findStudentFeeRule(
                    admin,
                    student,
                    classRoom,
                    resolvedCourse,
                    resolvedBatchName,
                    resolvedAcademicYear,
                    resolvedSemester
            );
            if (fee != null) {
                return fee.getTotalAmount();
            }
            if (classRoom != null && classRoom.getTotalFees() != null && classRoom.getTotalFees() > 0) {
                return classRoom.getTotalFees();
            }
        }

        return safeNumber(student.getTotalFees());
    }

    private Fees findStudentFeeRule(Admin admin, Student student, ClassRoom classRoom, String course, String batchName, String academicYear, String semester) {
        if (admin == null || course == null || course.isBlank()) {
            return null;
        }

        String normCourse = course.strip();
        String normBatch = defaultText(batchName, null);
        String normYear = defaultText(academicYear, null);
        String normSem = normalizeSemesterKey(semester);
        List<Fees> rules = feesRepository.findByAdmin(admin);

        if (normBatch != null && !normBatch.isBlank()) {
            for (Fees row : rules) {
                if (!normCourse.equalsIgnoreCase(defaultText(row.getCourse(), null))) continue;
                if (!feeBatchMatchesStudent(row, student, classRoom, normBatch)) continue;
                String rowYear = defaultText(row.getAcademicYear(), null);
                if (normYear != null && rowYear != null && !academicYearMatches(rowYear, normYear)) continue;
                if (isYearWiseFeeRule(row)) return row;
            }
            for (Fees row : rules) {
                if (!normCourse.equalsIgnoreCase(defaultText(row.getCourse(), null))) continue;
                if (!feeBatchMatchesStudent(row, student, classRoom, normBatch)) continue;
                String rowYear = defaultText(row.getAcademicYear(), null);
                if (normYear != null && rowYear != null && !academicYearMatches(rowYear, normYear)) continue;
                String rowSem = defaultText(row.getSemester(), null);
                String rowSemKey = normalizeSemesterKey(rowSem);
                if (rowSemKey != null && normSem != null && !normSem.equalsIgnoreCase(rowSemKey)) continue;
                return row;
            }
        }

        for (Fees row : rules) {
            if (!normCourse.equalsIgnoreCase(defaultText(row.getCourse(), null))) continue;
            if (!feeBatchMatchesStudent(row, student, classRoom, normBatch)) continue;
            String rowYear = defaultText(row.getAcademicYear(), null);
            if (normYear != null && rowYear != null && !academicYearMatches(rowYear, normYear)) continue;
            if (isYearWiseFeeRule(row)) return row;
        }

        if (normYear != null && normSem != null) {
            Fees row = feesRepository.findByAdminAndCourseIgnoreCaseAndAcademicYearIgnoreCaseAndSemesterIgnoreCase(
                    admin, normCourse, normYear, normSem
            );
            if (row != null) return row;
        }

        if (normYear != null) {
            Fees row = feesRepository.findByAdminAndCourseIgnoreCaseAndAcademicYearIgnoreCaseAndSemesterIsNull(
                    admin, normCourse, normYear
            );
            if (row != null) return row;
        }

        return feesRepository.findByAdminAndCourseIgnoreCaseAndAcademicYearIsNullAndSemesterIsNull(admin, normCourse);
    }

    private Student getLoggedStudent(HttpSession session) {
        String email = (String) session.getAttribute("loggedInUser");
        String role = (String) session.getAttribute("userRole");
        if (email == null || (role != null && !"STUDENT".equalsIgnoreCase(role))) {
            return null;
        }
        return studentRepository.findByEmail(email);
    }

    private boolean requiresOnboarding(Student student) {
        if (student == null) {
            return true;
        }
        if ("ADMITTED".equalsIgnoreCase(defaultText(student.getAdmissionStatus(), ""))) {
            return false;
        }
        return !Boolean.TRUE.equals(student.getOnboardingCompleted());
    }

    private List<Timetable> loadStudentTimetable(Student student) {
        if (student == null || student.getAdmin() == null) {
            return List.of();
        }

        ClassRoom studentClassRoom = resolveStudentClassRoom(student);
        List<Timetable> entries;
        if (studentClassRoom != null
                && studentClassRoom.getAdmin() != null
                && studentClassRoom.getAdmin().getId() != null
                && studentClassRoom.getAdmin().getId().equals(student.getAdmin().getId())) {
            entries = timetableRepository.findByAdminAndClassRoom(student.getAdmin(), studentClassRoom);
        } else {
            entries = timetableRepository.findByAdmin(student.getAdmin()).stream()
                    .filter(entry -> timetableMatchesStudentScope(entry, student))
                    .toList();
        }

        return entries.stream()
                .sorted(Comparator
                        .comparingInt((Timetable t) -> dayOrderIndex(t.getDay()))
                        .thenComparing(t -> parseTime(t.getStartTime())))
                .toList();
    }

    private LinkedHashMap<String, List<TimetableSlotView>> buildWeeklySchedule(List<Timetable> entries) {
        LinkedHashMap<String, List<TimetableSlotView>> weekly = new LinkedHashMap<>();
        for (String day : WORKING_DAYS) {
            weekly.put(day, new ArrayList<>());
        }

        for (Timetable entry : entries) {
            String day = normalizeDay(entry.getDay());
            if (!WORKING_DAYS.contains(day)) {
                continue;
            }
            String subjectLabel = resolveTimetableSubjectLabel(entry);
            weekly.get(day).add(new TimetableSlotView(
                    day,
                    formatTimeLabel(entry.getStartTime()),
                    formatTimeLabel(entry.getEndTime()),
                    subjectLabel,
                    resolveTeacherName(entry.getTeacher()),
                    defaultText(entry.getRoom(), "Room not assigned"),
                    buildSubjectClass(subjectLabel)
            ));
        }

        weekly.values().forEach(list -> list.sort(Comparator.comparing(slot -> parseTime(slot.getStartTime()))));
        return weekly;
    }

    private List<WeeklyTimetableRowView> buildWeeklyTimetableRows(LinkedHashMap<String, List<TimetableSlotView>> weeklySchedule) {
        LinkedHashMap<String, WeeklyTimetableRowBuilder> rows = new LinkedHashMap<>();

        for (String day : WORKING_DAYS) {
            for (TimetableSlotView slot : weeklySchedule.getOrDefault(day, List.of())) {
                String key = slot.getStartTime() + "|" + slot.getEndTime();
                WeeklyTimetableRowBuilder builder = rows.computeIfAbsent(
                        key,
                        value -> new WeeklyTimetableRowBuilder(slot.getStartTime(), slot.getEndTime())
                );
                builder.put(day, slot);
            }
        }

        String lunchStart = formatTimeLabel(LUNCH_BREAK_START);
        String lunchEnd = formatTimeLabel(LUNCH_BREAK_END);
        String lunchKey = lunchStart + "|" + lunchEnd;
        if (!rows.containsKey(lunchKey)) {
            WeeklyTimetableRowBuilder lunchBuilder = new WeeklyTimetableRowBuilder(lunchStart, lunchEnd, true);
            for (String day : WORKING_DAYS) {
                lunchBuilder.put(day, createLunchBreakSlot(day));
            }
            rows.put(lunchKey, lunchBuilder);
        }

        return rows.values().stream()
                .sorted(Comparator.comparing(row -> parseTime(row.startTime())))
                .map(WeeklyTimetableRowBuilder::build)
                .toList();
    }

    private List<SubjectSummaryView> buildSubjectSummaries(List<Timetable> entries, List<Subject> assignedSubjects) {
        Map<String, SubjectSummaryViewBuilder> grouped = new LinkedHashMap<>();

        for (Timetable entry : entries) {
            String subjectName = resolveTimetableSubjectLabel(entry);
            SubjectSummaryViewBuilder builder = grouped.computeIfAbsent(
                    subjectName + "|" + resolveTeacherName(entry.getTeacher()),
                    key -> new SubjectSummaryViewBuilder(
                            subjectName,
                            resolveTeacherName(entry.getTeacher()),
                            buildSubjectClass(subjectName)
                    )
            );
            builder.addEntry(
                    normalizeDay(entry.getDay()),
                    formatTimeLabel(entry.getStartTime()),
                    formatTimeLabel(entry.getEndTime())
            );
            builder.setRoom(defaultText(entry.getRoom(), "Room not assigned"));
        }

        List<SubjectSummaryView> subjects = new ArrayList<>();
        for (SubjectSummaryViewBuilder builder : grouped.values()) {
            subjects.add(builder.build());
        }
        return subjects;
    }

    private List<Subject> loadAssignedSubjects(Student student) {
        if (student == null || student.getAdmin() == null || student.getBatch() == null || student.getBatch().getCourse() == null) {
            return List.of();
        }

        Integer semesterNumber = parseSemesterNumber(student.getSemester());
        if (semesterNumber == null) {
            ClassRoom classRoom = resolveStudentClassRoom(student);
            semesterNumber = classRoom != null ? classRoom.getSemester() : null;
        }
        if (semesterNumber == null) {
            return List.of();
        }

        return subjectRepository.findByAdminAndBatchRefAndCourseRefAndSemesterAndStatusIgnoreCaseOrderByNameAscCodeAsc(
                student.getAdmin(),
                student.getBatch(),
                student.getBatch().getCourse(),
                semesterNumber,
                "active"
        );
    }

    private List<SubjectCatalogRowView> buildSubjectCatalogRows(List<Subject> subjects) {
        List<SubjectCatalogRowView> rows = new ArrayList<>();
        int index = 1;
        for (Subject subject : subjects) {
            rows.add(new SubjectCatalogRowView(
                    index++,
                    defaultText(subject.getCode(), "-"),
                    defaultText(subject.getName(), "Unnamed Subject"),
                    normalizeStudentSubjectCategory(subject),
                    formatSubjectCredits(subject.getCredits()),
                    categoryBadgeClass(subject)
            ));
        }
        return rows;
    }

    private List<StudentNotificationView> buildNotifications(
            Student student,
            List<TimetableSlotView> todaySchedule,
            List<SubjectSummaryView> subjectSummaries,
            double pendingFees,
            DayScheduleInsight dayScheduleInsight) {

        List<StudentNotificationView> items = new ArrayList<>();

        if (dayScheduleInsight.currentSlot() != null) {
            items.add(new StudentNotificationView(
                    "Current class is live",
                    dayScheduleInsight.currentSlot().getSubject() + " is running from "
                            + dayScheduleInsight.currentSlot().getStartTime() + " to "
                            + dayScheduleInsight.currentSlot().getEndTime() + " in "
                            + dayScheduleInsight.currentSlot().getRoom() + ".",
                    "Current slot",
                    "success",
                    "/timetable",
                    "fa-solid fa-bolt"
            ));
        } else if (dayScheduleInsight.nextSlot() != null) {
            items.add(new StudentNotificationView(
                    "Next class is lined up",
                    dayScheduleInsight.nextSlot().getSubject() + " starts at "
                            + dayScheduleInsight.nextSlot().getStartTime() + " with "
                            + dayScheduleInsight.nextSlot().getTeacher() + ".",
                    "Timetable",
                    "info",
                    "/timetable",
                    "fa-regular fa-clock"
            ));
        }

        if (!todaySchedule.isEmpty()) {
            items.add(new StudentNotificationView(
                    "Today's timetable is ready",
                    "You have " + todaySchedule.size() + " class(es) today with "
                            + dayScheduleInsight.upcomingCount() + " upcoming slot(s) left.",
                    "Timetable",
                    "info",
                    "/timetable",
                    "fa-solid fa-calendar-days"
            ));
        } else {
            items.add(new StudentNotificationView(
                    "No classes scheduled today",
                    "Your timetable has no classes for today, so this is a free academic window.",
                    "Timetable",
                    "info",
                    "/timetable",
                    "fa-regular fa-calendar-xmark"
            ));
        }

        if (pendingFees > 0) {
            items.add(new StudentNotificationView(
                    "Fee balance pending",
                    "Your remaining fee amount is " + formatCurrency(pendingFees) + ".",
                    "Accounts",
                    "warning",
                    "/fees",
                    "fa-solid fa-indian-rupee-sign"
            ));
        } else {
            items.add(new StudentNotificationView(
                    "Fees are fully paid",
                    "No pending fee balance is left for your account.",
                    "Accounts",
                    "success",
                    "/fees",
                    "fa-solid fa-circle-check"
            ));
        }

        if (student.getBatch() != null && parseSemesterNumber(student.getSemester()) != null) {
            items.add(new StudentNotificationView(
                    "Exam centre is linked",
                    "Exam and result updates for " + buildClassLabel(student) + " will appear in the exam section when published.",
                    "Exams",
                    "info",
                    "/exam-results",
                    "fa-solid fa-file-lines"
            ));
        } else {
            items.add(new StudentNotificationView(
                    "Academic mapping pending",
                    "Your batch or semester mapping is still missing, so exam and schedule pages cannot be fully assigned yet.",
                    "Academic setup",
                    "warning",
                    "/student-profile",
                    "fa-solid fa-user-graduate"
            ));
        }

        if (!subjectSummaries.isEmpty()) {
            items.add(new StudentNotificationView(
                    "Subjects assigned successfully",
                    "Your dashboard is showing " + subjectSummaries.size() + " assigned subject(s) from the weekly timetable.",
                    "Subjects",
                    "success",
                    "/subjects",
                    "fa-solid fa-book-open"
            ));
        }

        items.addAll(buildPlacementNotifications(student));

        return items;
    }

    private List<StudentNotificationView> buildExamNotifications(Student student) {
        List<StudentNotificationView> items = new ArrayList<>();
        for (var exam : examAutomationService.findVisibleUpcomingExams(student).stream().limit(3).toList()) {
            items.add(new StudentNotificationView(
                    "Exam scheduled: " + exam.getTitle(),
                    "Your class exam is on " + exam.getExamDateLabel() + " in " + defaultText(exam.getRoom(), "TBD") + ".",
                    "Exams",
                    "info",
                    "/student-exams/" + exam.getId(),
                    "fa-solid fa-graduation-cap"
            ));

            if (exam.getSyllabusFilePath() != null && !exam.getSyllabusFilePath().isBlank()) {
                items.add(new StudentNotificationView(
                        "Syllabus attached: " + exam.getTitle(),
                        "Open the syllabus file and review the topics before the exam.",
                        "Exams",
                        "success",
                        "/exams",
                        "fa-solid fa-file-pdf"
                ));
            }
        }
        return items;
    }

    private List<StudentNotificationView> buildPlacementNotifications(Student student) {
        List<PlacementApplication> applications = placementApplicationRepository.findByStudentOrderByAppliedAtDesc(student);
        if (applications.isEmpty()) {
            return List.of();
        }

        List<PlacementDrive> appliedDrives = applications.stream()
                .map(PlacementApplication::getPlacementDrive)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (appliedDrives.isEmpty()) {
            return List.of();
        }

        List<PlacementInterview> interviews = placementInterviewRepository.findByPlacementDriveInOrderByInterviewDateTimeAsc(appliedDrives);
        if (interviews.isEmpty()) {
            return List.of();
        }

        List<StudentNotificationView> items = new ArrayList<>();
        for (PlacementInterview interview : interviews) {
            PlacementDrive drive = interview.getPlacementDrive();
            String companyName = defaultText(drive != null ? drive.getCompanyName() : null, "Company");
            String roundName = defaultText(interview.getInterviewRound(), "Interview Round");
            String dateLabel = interview.getInterviewDateTime() != null
                    ? interview.getInterviewDateTime().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))
                    : "To be announced";
            String venue = defaultText(interview.getVenue(), "Venue to be announced");
            items.add(new StudentNotificationView(
                    "Interview scheduled",
                    roundName + " for " + companyName + " is scheduled on " + dateLabel + " at " + venue + ".",
                    "Placement",
                    "success",
                    "/placements",
                    "fa-solid fa-comments"
            ));
        }
        return items;
    }

    private DayScheduleInsight buildDayScheduleInsight(List<TimetableSlotView> todaySchedule) {
        if (todaySchedule.isEmpty()) {
            return new DayScheduleInsight(0, 0, null, null);
        }

        LocalTime now = LocalTime.now();
        int completed = 0;
        int upcoming = 0;
        TimetableSlotView current = null;
        TimetableSlotView next = null;

        for (TimetableSlotView slot : todaySchedule) {
            LocalTime start = parseTime(slot.getStartTime());
            LocalTime end = parseTime(slot.getEndTime());

            if (current == null && isValidTime(start) && isValidTime(end) && !now.isBefore(start) && now.isBefore(end)) {
                current = slot;
                continue;
            }

            if (isValidTime(end) && now.compareTo(end) >= 0) {
                completed++;
                continue;
            }

            if (isValidTime(start) && now.isBefore(start)) {
                upcoming++;
                if (next == null) {
                    next = slot;
                }
            }
        }

        if (current != null) {
            upcoming = Math.max(upcoming, 0);
        }

        return new DayScheduleInsight(completed, upcoming, current, next);
    }

    private String resolveStudentId(Student student) {
        return student.getId() != null ? String.valueOf(student.getId()) : "Not assigned";
    }

    private String buildStudentInitials(String name) {
        if (name == null || name.isBlank()) return "ST";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ENGLISH);
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1))
                .toUpperCase(Locale.ENGLISH);
    }

    private String buildCollegeInitials(String collegeName) {
        if (collegeName == null || collegeName.isBlank()) return "AI";
        String[] parts = collegeName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ENGLISH);
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1))
                .toUpperCase(Locale.ENGLISH);
    }

    private String buildSemesterLabel(Student student) {
        Integer semesterNumber = parseSemesterNumber(student.getSemester());
        if (semesterNumber != null) {
            return "Sem " + semesterNumber;
        }
        String semester = defaultText(student.getSemester(), "");
        if (!semester.isBlank()) {
            return semester;
        }
        return "Not assigned";
    }

    private String buildSectionLabel(Student student) {
        if (student.getSectionName() != null && !student.getSectionName().isBlank()) {
            return "Section " + student.getSectionName();
        }
        return "Section not assigned";
    }

    private String buildClassLabel(Student student) {
        ClassRoom classRoom = resolveStudentClassRoom(student);
        if (classRoom != null) {
            String label = classRoom.getDisplayLabel();
            if (label != null && !label.isBlank()) {
                return label.replace("â€“", "-");
            }
        }

        List<String> parts = new ArrayList<>();
        if (student.getBatch() != null && student.getBatch().getDisplayName() != null && !student.getBatch().getDisplayName().isBlank()) {
            parts.add(student.getBatch().getDisplayName());
        }
        if (student.getCourse() != null && !student.getCourse().isBlank()) parts.add(student.getCourse());
        String semester = buildSemesterLabel(student);
        if (!semester.equals("Not assigned")) parts.add(semester);
        String section = buildSectionLabel(student);
        if (!section.equals("Section not assigned")) parts.add(section);
        return parts.isEmpty() ? "Class not assigned" : String.join(" - ", parts);
    }

    private String buildBatchYearLabel(Student student) {
        if (student.getBatch() != null) {
            if (student.getBatch().getStartYear() != null && student.getBatch().getEndYear() != null) {
                return student.getBatch().getStartYear() + "-" + student.getBatch().getEndYear();
            }
            if (student.getBatch().getDisplayName() != null && !student.getBatch().getDisplayName().isBlank()) {
                return student.getBatch().getDisplayName().replace("Ã¢â‚¬â€œ", "-");
            }
        }
        ClassRoom classRoom = resolveStudentClassRoom(student);
        if (classRoom != null) {
            if (classRoom.getBatchStartYear() != null && classRoom.getBatchEndYear() != null) {
                return classRoom.getBatchStartYear() + "-" + classRoom.getBatchEndYear();
            }
            if (classRoom.getAcademicYear() != null && !classRoom.getAcademicYear().isBlank()) {
                return classRoom.getAcademicYear().replace("Ã¢â‚¬â€œ", "-");
            }
        }
        return "Batch not assigned";
    }

    private static String buildTermLabel(Student student) {
        String semester = defaultText(student.getSemester(), "");
        if (semester.isBlank()) {
            ClassRoom classRoom = student.getClassRoom();
            if (classRoom != null && classRoom.getSemester() != null) {
                semester = String.valueOf(classRoom.getSemester());
            }
        }
        if (semester.isBlank()) {
            return "NOT ASSIGNED";
        }

        String normalized = semester.trim().toUpperCase(Locale.ENGLISH);
        String digits = normalized.replaceAll("[^0-9]", "");
        if (!digits.isBlank()) {
            return digits + " SEMESTER";
        }
        return normalized.contains("SEMESTER") ? normalized : normalized + " SEMESTER";
    }

    private static String buildCycleLabel(Student student) {
        String cycle = defaultText(student.getAcademicYear(), "");
        if (!cycle.isBlank()) {
            return cycle.toUpperCase(Locale.ENGLISH);
        }
        return LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)).toUpperCase(Locale.ENGLISH);
    }

    private String normalizeStudentSubjectCategory(Subject subject) {
        if (subject == null) {
            return "COMPULSORY CORE";
        }

        String value = defaultText(subject.getCourseCategory(), "");
        String normalized = value.trim().toUpperCase(Locale.ENGLISH);
        if (normalized.contains("LAB") || normalized.contains("PRACTICAL")) {
            return "LAB PRACTICAL";
        }
        if (normalized.contains("ELECTIVE")) {
            return "ELECTIVE";
        }
        if (normalized.contains("PROJECT")) {
            return "PROJECT";
        }
        if (subject.isOverride() && subject.getSubjectMasterRef() == null) {
            return "CUSTOM";
        }
        return "COMPULSORY CORE";
    }

    private String categoryBadgeClass(Subject subject) {
        return switch (normalizeStudentSubjectCategory(subject)) {
            case "LAB PRACTICAL" -> "portal-badge--success";
            case "ELECTIVE" -> "portal-badge--warning";
            case "PROJECT" -> "portal-badge--project";
            case "CUSTOM" -> "portal-badge--custom";
            default -> "portal-badge--info";
        };
    }

    private String formatSubjectCredits(Double credits) {
        if (credits == null) {
            return "-";
        }
        return String.format(Locale.ENGLISH, "%.2f", credits);
    }

    private String resolveTeacherName(Teacher teacher) {
        return teacher != null ? defaultText(teacher.getName(), "Faculty not assigned") : "Faculty not assigned";
    }

    private String resolveClassMentor(Student student) {
        return "Faculty not assigned";
    }

    private String resolvePrimaryRoom(Student student, TimetableSlotView currentSlot) {
        if (currentSlot != null && currentSlot.getRoom() != null && !currentSlot.getRoom().isBlank()) {
            return currentSlot.getRoom();
        }
        ClassRoom classRoom = resolveStudentClassRoom(student);
        if (classRoom != null && classRoom.getRoom() != null
                && !classRoom.getRoom().isBlank()) {
            return classRoom.getRoom();
        }
        return "Room not assigned";
    }

    private String normalizeDay(String day) {
        if (day == null || day.isBlank()) return "Monday";
        String clean = day.trim().toLowerCase(Locale.ENGLISH);
        return switch (clean) {
            case "mon", "monday" -> "Monday";
            case "tue", "tues", "tuesday" -> "Tuesday";
            case "wed", "wednesday" -> "Wednesday";
            case "thu", "thur", "thurs", "thursday" -> "Thursday";
            case "fri", "friday" -> "Friday";
            case "sat", "saturday" -> "Saturday";
            case "sun", "sunday" -> "Sunday";
            default -> Character.toUpperCase(clean.charAt(0)) + clean.substring(1);
        };
    }

    private int dayOrderIndex(String day) {
        int index = WORKING_DAYS.indexOf(normalizeDay(day));
        return index >= 0 ? index : WORKING_DAYS.size();
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) return LocalTime.MAX;

        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ofPattern("H:mm"),
                DateTimeFormatter.ofPattern("HH:mm"),
                DateTimeFormatter.ofPattern("h:mm a"),
                DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)
        );

        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalTime.parse(value.trim().toUpperCase(Locale.ENGLISH), formatter);
            } catch (Exception ignored) {
            }
        }
        return LocalTime.MAX;
    }

    private boolean isValidTime(LocalTime value) {
        return value != null && !LocalTime.MAX.equals(value);
    }

    private String currentDayName() {
        DayOfWeek current = LocalDate.now().getDayOfWeek();
        return current.getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    private double safeNumber(Double value) {
        return value != null ? value : 0.0;
    }

    private String formatCurrency(double amount) {
        return "₹" + CURRENCY_FORMAT.format(Math.round(amount));
    }

    private String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMAT) : "Not provided";
    }

    private String dashboardGreeting() {
        int hour = LocalTime.now().getHour();
        if (hour < 12) return "Good Morning";
        if (hour < 17) return "Good Afternoon";
        return "Good Evening";
    }

    private String formatTimeLabel(String value) {
        LocalTime parsed = parseTime(value);
        if (!isValidTime(parsed)) {
            return defaultText(value, "--");
        }
        return parsed.format(TIME_DISPLAY_FORMAT);
    }

    private static String defaultText(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value.trim();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private String composeAddress(String houseNumber,
                                  String addressLine,
                                  String city,
                                  String district,
                                  String state,
                                  String country,
                                  String pinCode) {
        List<String> parts = new ArrayList<>();
        String house = blankToNull(houseNumber);
        String line = blankToNull(addressLine);
        String cityValue = blankToNull(city);
        String districtValue = blankToNull(district);
        String stateValue = blankToNull(state);
        String countryValue = blankToNull(country);
        String pinValue = blankToNull(pinCode);

        if (house != null) parts.add(house);
        if (line != null) parts.add(line);
        if (cityValue != null) parts.add(cityValue);
        if (districtValue != null) parts.add(districtValue);
        if (stateValue != null) parts.add(stateValue);
        if (countryValue != null) parts.add(countryValue);
        if (pinValue != null) parts.add(pinValue);

        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private String composeStudentName(String firstName, String middleName, String lastName, String fallback) {
        List<String> parts = new ArrayList<>();
        String first = blankToNull(firstName);
        String middle = blankToNull(middleName);
        String last = blankToNull(lastName);
        if (first != null) parts.add(first);
        if (middle != null) parts.add(middle);
        if (last != null) parts.add(last);
        if (!parts.isEmpty()) {
            return String.join(" ", parts);
        }
        return blankToNull(fallback);
    }

    private List<String> districtOptionsForState(String state) {
        String normalized = blankToNull(state);
        if (normalized == null) {
            return List.of();
        }
        if ("Maharashtra".equalsIgnoreCase(normalized)) {
            return List.of(
                    "Ahmednagar", "Akola", "Amravati", "Aurangabad", "Beed", "Bhandara", "Buldhana",
                    "Chandrapur", "Dhule", "Gadchiroli", "Gondia", "Hingoli", "Jalgaon", "Jalna",
                    "Kolhapur", "Latur", "Mumbai City", "Mumbai Suburban", "Nagpur", "Nanded",
                    "Nandurbar", "Nashik", "Palghar", "Parbhani", "Pune", "Raigad", "Ratnagiri",
                    "Sangli", "Satara", "Sindhudurg", "Solapur", "Thane", "Wardha", "Washim",
                    "Yavatmal", "Other"
            );
        }
        return List.of("Other");
    }

    private String saveStudentUpload(MultipartFile file, String subDir) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String root = System.getProperty("user.dir") + "/src/main/resources/static/uploads/" + subDir + "/";
        Files.createDirectories(Paths.get(root));
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload";
        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            ext = original.substring(dot);
        }
        String filename = UUID.randomUUID() + ext;
        Path target = Paths.get(root, filename);
        Files.copy(file.getInputStream(), target);
        return "/uploads/" + subDir + "/" + filename;
    }

    private void deleteStoredUpload(String storedPath) {
        String normalizedPath = blankToNull(storedPath);
        if (normalizedPath == null || !normalizedPath.startsWith("/uploads/")) {
            return;
        }
        Path staticRoot = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "static").normalize();
        Path target = staticRoot.resolve(normalizedPath.substring(1)).normalize();
        if (!target.startsWith(staticRoot)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
        }
    }

    private void persistStudentDocuments(Student student, String[] documentNames, MultipartFile[] documentFiles) throws IOException {
        if (student == null || documentNames == null || documentFiles == null) {
            return;
        }

        int count = Math.min(documentNames.length, documentFiles.length);
        for (int i = 0; i < count; i++) {
            String documentName = blankToNull(documentNames[i]);
            MultipartFile documentFile = documentFiles[i];
            if (documentName == null || documentFile == null || documentFile.isEmpty()) {
                continue;
            }

            StudentDocument document = studentDocumentRepository
                    .findByStudentIdAndDocumentNameIgnoreCase(student.getId(), documentName)
                    .orElseGet(StudentDocument::new);
            if (document.getId() != null) {
                deleteStoredUpload(document.getStoredPath());
            } else {
                document.setStudent(student);
            }

            String savedPath = saveStudentUpload(documentFile, "student-documents");
            if (savedPath == null) {
                continue;
            }

            document.setStudent(student);
            document.setDocumentName(documentName);
            document.setOriginalFilename(blankToNull(documentFile.getOriginalFilename()));
            document.setStoredPath(savedPath);
            document.setFileSize(documentFile.getSize());
            document.setStatus("PENDING");
            document.setRemarks(null);
            document.setReviewedBy(null);
            document.setReviewedAt(null);
            document.setUploadedAt(LocalDateTime.now());
            studentDocumentRepository.save(document);
        }
    }

    private List<StudentDocument> loadStudentDocuments(Student student) {
        if (student == null) {
            return List.of();
        }
        return studentDocumentRepository.findByStudentIdOrderByDocumentNameAsc(student.getId());
    }

    private boolean hasDocumentReuploadRequest(Student student) {
        return student != null && studentDocumentRepository.existsByStudentIdAndStatusIgnoreCase(student.getId(), "REUPLOAD_REQUESTED");
    }

    private void applyExtendedStudentFields(Student student, HttpServletRequest request) {
        student.setTitle(blankToNull(request.getParameter("title")));
        student.setFirstName(blankToNull(request.getParameter("firstName")));
        student.setMiddleName(blankToNull(request.getParameter("middleName")));
        student.setLastName(blankToNull(request.getParameter("lastName")));
        student.setNameAsPerAadhaar(blankToNull(request.getParameter("nameAsPerAadhaar")));
        student.setLnameAs12thStd(blankToNull(request.getParameter("lnameAs12thStd")));
        student.setFnameAs12thStd(blankToNull(request.getParameter("fnameAs12thStd")));
        student.setMnameAs12thStd(blankToNull(request.getParameter("mnameAs12thStd")));
        student.setPhoneNumber(blankToNull(request.getParameter("phoneNumber")));
        student.setMaritalStatus(blankToNull(request.getParameter("maritalStatus")));
        student.setMotherTongue(blankToNull(request.getParameter("motherTongue")));
        student.setNativePlace(blankToNull(request.getParameter("nativePlace")));
        student.setBirthPlace(blankToNull(request.getParameter("birthPlace")));
        student.setBirthCountry(blankToNull(request.getParameter("birthCountry")));
        student.setRegion(blankToNull(request.getParameter("region")));
        student.setNationality(blankToNull(request.getParameter("nationality")));
        String categoryValue = blankToNull(request.getParameter("category"));
        if (categoryValue == null) {
            categoryValue = blankToNull(request.getParameter("categoryType"));
        }
        student.setCategory(categoryValue);
        student.setCategoryType(blankToNull(request.getParameter("categoryType")));
        student.setCasteCategory(blankToNull(request.getParameter("casteCategory")));
        student.setSubCaste(blankToNull(request.getParameter("subCaste")));
        student.setFatherOccupation(blankToNull(request.getParameter("fatherOccupation")));
        student.setFatherQualification(blankToNull(request.getParameter("fatherQualification")));
        student.setMotherQualification(blankToNull(request.getParameter("motherQualification")));
        student.setTotalFamilyMember(parseInteger(request.getParameter("totalFamilyMember")));
        student.setFamilyAnnualIncome(parseDouble(request.getParameter("familyAnnualIncome")));
        student.setDifferentlyAbled(parseBoolean(request.getParameter("differentlyAbled")));
        student.setSportsPerson(parseBoolean(request.getParameter("sportsPerson")));
        student.setSportsAchievement(blankToNull(request.getParameter("sportsAchievement")));
        student.setHobbies(blankToNull(request.getParameter("hobbies")));
        student.setUniversityPreAdmRegNo(blankToNull(request.getParameter("universityPreAdmRegNo")));
        student.setLastQualifyingExamName(blankToNull(request.getParameter("lastQualifyingExamName")));
        student.setBoardUniversity(blankToNull(request.getParameter("boardUniversity")));
        student.setSchoolCollege(blankToNull(request.getParameter("schoolCollege")));
        student.setDateOfPassing(parseLocalDate(request.getParameter("dateOfPassing")));
        student.setResult(blankToNull(request.getParameter("result")));
        student.setExamSeatNo(blankToNull(request.getParameter("examSeatNo")));
        student.setObtainedMarks(parseDouble(request.getParameter("obtainedMarks")));
        student.setTotalMarks(parseDouble(request.getParameter("totalMarks")));
        student.setPercentage(parseDouble(request.getParameter("percentage")));
        student.setPassingMonth(blankToNull(request.getParameter("passingMonth")));
        student.setPassingYear(parseInteger(request.getParameter("passingYear")));
        student.setStream(blankToNull(request.getParameter("stream")));
        student.setEducationGap(parseBoolean(request.getParameter("educationGap")));
        student.setNoOfAttempt(parseInteger(request.getParameter("noOfAttempt")));
        student.setInhouse(blankToNull(request.getParameter("inhouse")));
        student.setMediumOfInstruction(blankToNull(request.getParameter("mediumOfInstruction")));
        student.setSocialReservation(blankToNull(request.getParameter("socialReservation")));
        student.setAcademicBankOfCredits(blankToNull(request.getParameter("academicBankOfCredits")));
        student.setParentMobile(blankToNull(request.getParameter("parentMobile")));
    }

    private Integer parseInteger(String value) {
        try {
            String normalized = blankToNull(value);
            return normalized == null ? null : Integer.valueOf(normalized);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Double parseDouble(String value) {
        try {
            String normalized = blankToNull(value);
            return normalized == null ? null : Double.valueOf(normalized.replace(",", ""));
        } catch (Exception ignored) {
            return null;
        }
    }

    private LocalDate parseLocalDate(String value) {
        try {
            String normalized = blankToNull(value);
            return normalized == null ? null : LocalDate.parse(normalized);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Boolean parseBoolean(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            return null;
        }
        return "true".equalsIgnoreCase(normalized)
                || "yes".equalsIgnoreCase(normalized)
                || "on".equalsIgnoreCase(normalized)
                || "1".equalsIgnoreCase(normalized);
    }

    private String maskValue(String value, int visibleDigits) {
        if (value == null || value.isBlank()) return "Not provided";
        String trimmed = value.replaceAll("\\s+", "");
        if (trimmed.length() <= visibleDigits) return trimmed;
        String hidden = "X".repeat(Math.max(0, trimmed.length() - visibleDigits));
        return hidden + trimmed.substring(trimmed.length() - visibleDigits);
    }

    private String resolveCollegeName(Admin admin) {
        return admin != null ? defaultText(admin.getCollegeName(), "AI Campus Institute") : "AI Campus Institute";
    }

    private String resolveCollegeCode(Student student) {
        if (student == null) {
            return null;
        }
        if (student.getAdmin() != null && student.getAdmin().getCollegeCode() != null && !student.getAdmin().getCollegeCode().isBlank()) {
            return student.getAdmin().getCollegeCode().trim();
        }
        College college = resolveCollege(student);
        return college != null && college.getCode() != null ? college.getCode().trim() : null;
    }

    private College resolveCollege(Student student) {
        if (student != null && student.getAdmin() != null && student.getAdmin().getCollege() != null) {
            return student.getAdmin().getCollege();
        }
        return null;
    }

    private String resolveCollegeLogo(Admin admin) {
        if (admin != null && admin.getCollege() != null) {
            return admin.getCollege().getLogoPath();
        }
        return null;
    }

    private String resolveCourseLabel(Student student, List<Course> availableCourses) {
        String courseCode = blankToNull(student != null ? student.getCourse() : null);
        if (courseCode == null || availableCourses == null || availableCourses.isEmpty()) {
            return courseCode;
        }
        return availableCourses.stream()
                .filter(course -> course != null && course.getCode() != null && course.getCode().equalsIgnoreCase(courseCode))
                .findFirst()
                .map(course -> course.getName() + " (" + course.getCode() + ")")
                .orElse(courseCode);
    }

    private String buildSubjectClass(String subject) {
        if (subject == null) return "free";
        String normalized = subject.toLowerCase(Locale.ENGLISH);
        if (normalized.contains("lunch") || normalized.contains("break")) return "break";
        if (normalized.contains("java")) return "java";
        if (normalized.contains("artificial") || normalized.contains("ai")) return "ai";
        if (normalized.contains("database")) return "db";
        if (normalized.contains("software")) return "se";
        if (normalized.contains("network")) return "cn";
        if (normalized.contains("lab") || normalized.contains("web")) return "lab";
        return "free";
    }

    private String resolveTimetableSubjectLabel(Timetable entry) {
        if (entry == null) {
            return "Subject not assigned";
        }
        String subject = defaultText(entry.getSubject(), null);
        if (subject != null) {
            return subject;
        }
        if (entry.getSubjectRef() != null) {
            String name = defaultText(entry.getSubjectRef().getName(), null);
            if (name != null) return name;
            String code = defaultText(entry.getSubjectRef().getCode(), null);
            if (code != null) return code;
        }
        return "Subject not assigned";
    }

    private TimetableSlotView createLunchBreakSlot(String day) {
        return new TimetableSlotView(
                day,
                formatTimeLabel(LUNCH_BREAK_START),
                formatTimeLabel(LUNCH_BREAK_END),
                "Lunch Break",
                "Campus break",
                "No classes scheduled",
                "break"
        );
    }

    private String shortDayName(String day) {
        return normalizeDay(day).substring(0, Math.min(3, normalizeDay(day).length()));
    }

    private void ensureApplicationNumber(Student student) {
        if (student == null) {
            return;
        }
        if (student.getId() == null) {
            return;
        }

        String expected = buildApplicationNumber(student);
        String current = blankToNull(student.getRegistrationNo());
        if (expected != null && !expected.equals(current)) {
            student.setRegistrationNo(expected);
            studentRepository.save(student);
        }
    }

    private String buildApplicationNumber(Student student) {
        String courseCode = blankToNull(student.getCourse());
        if (courseCode == null && student.getClassRoom() != null) {
            courseCode = blankToNull(student.getClassRoom().getCourse());
        }
        if (courseCode == null) {
            courseCode = "APP";
        }

        String academicYear = resolveAcademicYearForApplication(student);
        String phaseLabel = buildAcademicPhaseLabel(student, academicYear);
        return String.format("%s%s%s/%d",
                phaseLabel,
                courseCode.strip().toUpperCase(Locale.ENGLISH),
                academicYear,
                student.getId());
    }

    private String resolveAcademicYearForApplication(Student student) {
        String academicYear = blankToNull(student.getAcademicYear());
        if (academicYear == null && student.getBatch() != null && student.getBatch().getStartYear() != null) {
            academicYear = student.getBatch().getStartYear() + "-" + String.valueOf(student.getBatch().getStartYear() + 1).substring(2);
        }
        if (academicYear == null) {
            int year = LocalDate.now().getYear();
            int month = LocalDate.now().getMonthValue();
            int start = (month >= 6) ? year : year - 1;
            academicYear = start + "-" + String.valueOf(start + 1).substring(2);
        }
        return academicYear;
    }

    private String buildAcademicPhaseLabel(Student student, String academicYear) {
        Integer batchStart = student != null && student.getBatch() != null ? student.getBatch().getStartYear() : null;
        Integer academicStart = parseAcademicYearStart(academicYear);
        int phaseIndex = batchStart != null && academicStart != null ? Math.max(0, academicStart - batchStart) : 0;
        return switch (phaseIndex) {
            case 1 -> "SY";
            case 2 -> "TY";
            default -> "FY";
        };
    }

    private Integer parseAcademicYearStart(String academicYear) {
        String value = blankToNull(academicYear);
        if (value == null) {
            return null;
        }
        String[] parts = value.split("-");
        if (parts.length < 1) {
            return null;
        }
        try {
            return Integer.parseInt(parts[0].replaceAll("[^0-9]", ""));
        } catch (Exception ignored) {
            return null;
        }
    }

    private record StudentPortalData(
            String studentId,
            String rollNo,
            String initials,
            String semesterLabel,
            String sectionLabel,
            String academicYear,
            String batchYearLabel,
            String batchLabel,
            String classLabel,
            String classMentor,
            String primaryRoom,
            String todayLabel,
            String todayDayKey,
            String todayFullDate,
            double totalFees,
            double paidFees,
            double pendingFees,
            String totalFeesFormatted,
            String paidFeesFormatted,
            String pendingFeesFormatted,
            int paidPercentage,
            String feeStatus,
            int activeAcademicDays,
            int todayCompletedCount,
            int todayUpcomingCount,
            TimetableSlotView currentSlot,
            TimetableSlotView nextSlot,
            LinkedHashMap<String, List<TimetableSlotView>> weeklySchedule,
            List<WeeklyTimetableRowView> weeklyTimetableRows,
            List<TimetableSlotView> todaySchedule,
            List<Timetable> allTimetableEntries,
            List<SubjectSummaryView> subjectSummaries,
            List<SubjectCatalogRowView> subjectCatalogRows,
            List<StudentNotificationView> notifications,
            List<StudentNotificationView> placementNotifications
    ) {}

    private record StudentFeeView(
            double totalFees,
            double paidFees,
            double pendingFees,
            int paidPercentage,
            String status
    ) {}

    private record DayScheduleInsight(
            int completedCount,
            int upcomingCount,
            TimetableSlotView currentSlot,
            TimetableSlotView nextSlot
    ) {}

    public static final class TimetableSlotView {
        private final String dayName;
        private final String startTime;
        private final String endTime;
        private final String subject;
        private final String teacher;
        private final String room;
        private final String subjectClass;

        public TimetableSlotView(String dayName, String startTime, String endTime,
                                 String subject, String teacher, String room, String subjectClass) {
            this.dayName = dayName;
            this.startTime = startTime;
            this.endTime = endTime;
            this.subject = subject;
            this.teacher = teacher;
            this.room = room;
            this.subjectClass = subjectClass;
        }

        public String getDayName() { return dayName; }
        public String getStartTime() { return startTime; }
        public String getEndTime() { return endTime; }
        public String getSubject() { return subject; }
        public String getTeacher() { return teacher; }
        public String getRoom() { return room; }
        public String getSubjectClass() { return subjectClass; }
        public String getTimeRange() { return startTime + " - " + endTime; }
        public boolean isBreakSlot() { return "break".equals(subjectClass); }
    }

    public static final class SubjectSummaryView {
        private final String subjectName;
        private final String teacherName;
        private final String room;
        private final String daySummary;
        private final int classCount;
        private final String primaryTimeRange;
        private final List<String> scheduleMoments;
        private final String subjectClass;

        public SubjectSummaryView(String subjectName, String teacherName, String room, String daySummary,
                                  int classCount, String primaryTimeRange, List<String> scheduleMoments,
                                  String subjectClass) {
            this.subjectName = subjectName;
            this.teacherName = teacherName;
            this.room = room;
            this.daySummary = daySummary;
            this.classCount = classCount;
            this.primaryTimeRange = primaryTimeRange;
            this.scheduleMoments = scheduleMoments;
            this.subjectClass = subjectClass;
        }

        public String getSubjectName() { return subjectName; }
        public String getTeacherName() { return teacherName; }
        public String getRoom() { return room; }
        public String getDaySummary() { return daySummary; }
        public int getClassCount() { return classCount; }
        public String getPrimaryTimeRange() { return primaryTimeRange; }
        public List<String> getScheduleMoments() { return scheduleMoments; }
        public String getSubjectClass() { return subjectClass; }
    }

    public static final class WeeklyTimetableRowView {
        private final String startTime;
        private final String endTime;
        private final String timeLabel;
        private final boolean breakRow;
        private final Map<String, TimetableSlotView> slotsByDay;

        public WeeklyTimetableRowView(String startTime, String endTime, String timeLabel, Map<String, TimetableSlotView> slotsByDay) {
            this(startTime, endTime, timeLabel, false, slotsByDay);
        }

        public WeeklyTimetableRowView(String startTime, String endTime, String timeLabel, boolean breakRow, Map<String, TimetableSlotView> slotsByDay) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.timeLabel = timeLabel;
            this.breakRow = breakRow;
            this.slotsByDay = slotsByDay;
        }

        public String getStartTime() { return startTime; }
        public String getEndTime() { return endTime; }
        public String getTimeLabel() { return timeLabel; }
        public boolean isBreakRow() { return breakRow; }
        public Map<String, TimetableSlotView> getSlotsByDay() { return slotsByDay; }
        public TimetableSlotView getSlotForDay(String day) { return slotsByDay.get(day); }
    }

    public static final class StudentAttendanceSummaryView {
        private final String subject;
        private final int total;
        private final int attended;
        private final int present;
        private final int late;
        private final int absent;
        private final int percentage;
        private final String latestDateLabel;

        public StudentAttendanceSummaryView(String subject, int total, int attended, int present, int late, int absent, int percentage, String latestDateLabel) {
            this.subject = subject;
            this.total = total;
            this.attended = attended;
            this.present = present;
            this.late = late;
            this.absent = absent;
            this.percentage = percentage;
            this.latestDateLabel = latestDateLabel;
        }

        public String getSubject() { return subject; }
        public int getTotal() { return total; }
        public int getAttended() { return attended; }
        public int getPresent() { return present; }
        public int getLate() { return late; }
        public int getAbsent() { return absent; }
        public int getPercentage() { return percentage; }
        public String getLatestDateLabel() { return latestDateLabel; }
    }

    private static final class AttendanceSummaryBuilder {
        private final String subject;
        private int total;
        private int present;
        private int late;
        private int absent;
        private LocalDate latestDate;

        private AttendanceSummaryBuilder(String subject) {
            this.subject = subject;
        }

        private void add(TeacherAttendanceEntry entry) {
            total++;
            String status = entry.getStatus() != null ? entry.getStatus().trim().toLowerCase(Locale.ENGLISH) : "present";
            if ("absent".equals(status)) {
                absent++;
            } else if ("late".equals(status)) {
                late++;
            } else {
                present++;
            }
            if (entry.getSession() != null && entry.getSession().getAttendanceDate() != null
                    && (latestDate == null || entry.getSession().getAttendanceDate().isAfter(latestDate))) {
                latestDate = entry.getSession().getAttendanceDate();
            }
        }

        private StudentAttendanceSummaryView build() {
            int attended = present + late;
            int percentage = total > 0 ? Math.round((attended * 100f) / total) : 0;
            return new StudentAttendanceSummaryView(
                    subject,
                    total,
                    attended,
                    present,
                    late,
                    absent,
                    percentage,
                    latestDate != null ? latestDate.format(DATE_FORMAT) : "No date"
            );
        }
    }

    public static final class StudentNotificationView {
        private final String title;
        private final String description;
        private final String source;
        private final String tone;
        private final String targetUrl;
        private final String iconClass;

        public StudentNotificationView(String title, String description, String source, String tone,
                                       String targetUrl, String iconClass) {
            this.title = title;
            this.description = description;
            this.source = source;
            this.tone = tone;
            this.targetUrl = targetUrl;
            this.iconClass = iconClass;
        }

        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getSource() { return source; }
        public String getTone() { return tone; }
        public String getTargetUrl() { return targetUrl; }
        public String getIconClass() { return iconClass; }
    }

    private static final class SubjectSummaryViewBuilder {
        private final String subjectName;
        private final String teacherName;
        private final String subjectClass;
        private String room = "Room not assigned";
        private final LinkedHashSet<String> days = new LinkedHashSet<>();
        private final List<String> scheduleMoments = new ArrayList<>();
        private String primaryTimeRange = "--";
        private int classCount = 0;

        private SubjectSummaryViewBuilder(String subjectName, String teacherName, String subjectClass) {
            this.subjectName = subjectName;
            this.teacherName = teacherName;
            this.subjectClass = subjectClass;
        }

        private void addEntry(String day, String startTime, String endTime) {
            days.add(day);
            scheduleMoments.add(shortLabel(day, startTime, endTime));
            classCount++;
            if ("--".equals(primaryTimeRange)) {
                primaryTimeRange = startTime + " - " + endTime;
            }
        }

        private void setRoom(String room) {
            this.room = room;
        }

        private SubjectSummaryView build() {
            return new SubjectSummaryView(
                    subjectName,
                    teacherName,
                    room,
                    String.join(", ", days),
                    classCount,
                    primaryTimeRange,
                    List.copyOf(scheduleMoments),
                    subjectClass
            );
        }

        private String shortLabel(String day, String startTime, String endTime) {
            return day.substring(0, Math.min(3, day.length())) + " " + startTime + " - " + endTime;
        }
    }

    public static final class SubjectCatalogRowView {
        private final int rowNumber;
        private final String code;
        private final String title;
        private final String category;
        private final String credits;
        private final String categoryBadgeClass;

        public SubjectCatalogRowView(int rowNumber, String code, String title, String category,
                                     String credits, String categoryBadgeClass) {
            this.rowNumber = rowNumber;
            this.code = code;
            this.title = title;
            this.category = category;
            this.credits = credits;
            this.categoryBadgeClass = categoryBadgeClass;
        }

        public int getRowNumber() { return rowNumber; }
        public String getCode() { return code; }
        public String getTitle() { return title; }
        public String getCategory() { return category; }
        public String getCredits() { return credits; }
        public String getCategoryBadgeClass() { return categoryBadgeClass; }
    }

    private static final class WeeklyTimetableRowBuilder {
        private final String startTime;
        private final String endTime;
        private final boolean breakRow;
        private final LinkedHashMap<String, TimetableSlotView> slotsByDay = new LinkedHashMap<>();

        private WeeklyTimetableRowBuilder(String startTime, String endTime) {
            this(startTime, endTime, false);
        }

        private WeeklyTimetableRowBuilder(String startTime, String endTime, boolean breakRow) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.breakRow = breakRow;
        }

        private void put(String day, TimetableSlotView slot) {
            slotsByDay.put(day, slot);
        }

        private String startTime() {
            return startTime;
        }

        private WeeklyTimetableRowView build() {
            LinkedHashMap<String, TimetableSlotView> orderedSlots = new LinkedHashMap<>();
            for (String day : WORKING_DAYS) {
                orderedSlots.put(day, slotsByDay.get(day));
            }
            return new WeeklyTimetableRowView(
                    startTime,
                    endTime,
                    startTime + " - " + endTime,
                    breakRow,
                    orderedSlots
            );
        }
    }
}
