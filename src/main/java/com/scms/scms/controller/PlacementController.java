package com.scms.scms.controller;

import com.scms.scms.model.*;
import com.scms.scms.repository.*;
import com.scms.scms.service.EmployeeIdService;
import com.scms.scms.service.PasswordProtectionService;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.stream.Collectors;

@Controller
public class PlacementController {

    private static final String UPLOAD_DIR_RESUMES =
            System.getProperty("user.dir") + "/src/main/resources/static/uploads/placements/resumes/";

    @Autowired private PlacementTpoRepository placementTpoRepository;
    @Autowired private PlacementDriveRepository placementDriveRepository;
    @Autowired private PlacementApplicationRepository placementApplicationRepository;
    @Autowired private PlacementInterviewRepository placementInterviewRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private EmployeeIdService employeeIdService;
    @Autowired private PasswordProtectionService passwordProtectionService;
    @Autowired(required = false) private JavaMailSender javaMailSender;

    @GetMapping("/login-placement")
    public String loginPage(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("showLogin", true);
        return "redirect:/";
    }

    @PostMapping("/placement-login")
    public String login(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String cleanEmail = email == null ? "" : email.trim();
        String cleanPassword = password == null ? "" : password.trim();

        if (cleanEmail.isBlank() || cleanPassword.isBlank()) {
            redirectAttributes.addFlashAttribute("errorLogin", "Email and password are required!");
            redirectAttributes.addFlashAttribute("showLogin", true);
            return "redirect:/";
        }

        PlacementTpo tpo = resolvePlacementTpoForLogin(cleanEmail, cleanPassword);
        if (tpo != null) {
            tpo = employeeIdService.ensureTpoEmployeeId(tpo);
            upgradeLegacyPasswordIfNeeded(tpo, cleanPassword);
            session.setAttribute("loggedInUser", tpo.getEmail());
            session.setAttribute("loggedInUserId", tpo.getId());
            session.setAttribute("collegeCode", resolveTpoCollegeCode(tpo));
            session.setAttribute("userRole", "PLACEMENT_TPO");
            return "redirect:/placement-dashboard";
        }

        redirectAttributes.addFlashAttribute("errorLogin", "Invalid email or password!");
        redirectAttributes.addFlashAttribute("showLogin", true);
        return "redirect:/";
    }

    @GetMapping("/placement-logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        String collegeCode = session != null ? firstNonBlank((String) session.getAttribute("collegeCode")) : null;
        if (session != null) {
            session.invalidate();
        }
        redirectAttributes.addFlashAttribute("showLogin", true);
        if (collegeCode == null) {
            return "redirect:/";
        }
        return "redirect:/Registration/Apply/" + collegeCode + "?mode=login";
    }

    @GetMapping("/placement-dashboard")
    public String dashboard(Model model, HttpSession session) {
        PlacementTpo tpo = getLoggedTpo(session);
        if (tpo == null) return "redirect:/login-placement";
        addWorkspaceModel(model, tpo);
        return "placement/placement-dashboard";
    }

    @GetMapping("/placement-company-drives")
    public String companyDrives(Model model, HttpSession session) {
        PlacementTpo tpo = getLoggedTpo(session);
        if (tpo == null) return "redirect:/login-placement";
        addWorkspaceModel(model, tpo);
        return "placement/placement-company-drives";
    }

    @GetMapping("/placement-applications")
    public String applications(Model model, HttpSession session, @RequestParam(required = false) String company) {
        PlacementTpo tpo = getLoggedTpo(session);
        if (tpo == null) return "redirect:/login-placement";
        addWorkspaceModel(model, tpo);
        model.addAttribute("applicationCompanyFilter", company == null ? "" : company.trim());
        model.addAttribute("filteredPlacementApplications", filterApplicationsByCompany((String) model.getAttribute("applicationCompanyFilter")));
        return "placement/placement-applications";
    }

    @GetMapping(value = "/placement-applications/export.xlsx")
    public void exportApplicationsExcel(
            HttpSession session,
            HttpServletResponse response,
            @RequestParam(required = false) String company) throws IOException {
        PlacementTpo tpo = getLoggedTpo(session);
        if (tpo == null) {
            response.sendRedirect("/login-placement");
            return;
        }

        List<PlacementApplication> applications = filterApplicationsByCompany(company);
        String fileName = "placement-applications-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")) + ".xlsx";

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        try (Workbook workbook = new XSSFWorkbook(); OutputStream outputStream = response.getOutputStream()) {
            Sheet sheet = workbook.createSheet("Applications");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = {
                    "#", "Student", "Email", "Phone", "Company", "Course", "Semester",
                    "Profile", "Skills", "Status", "Applied At", "Resume"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            DateTimeFormatter appliedAtFormat = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
            int rowIndex = 1;
            for (PlacementApplication application : applications) {
                Row row = sheet.createRow(rowIndex);
                row.createCell(0).setCellValue(rowIndex);
                row.createCell(1).setCellValue(safe(application.getFullName(), "Student"));
                row.createCell(2).setCellValue(safe(application.getEmail(), ""));
                row.createCell(3).setCellValue(safe(application.getPhone(), ""));
                row.createCell(4).setCellValue(application.getPlacementDrive() != null ? safe(application.getPlacementDrive().getCompanyName(), "Company") : "Company");
                row.createCell(5).setCellValue(safe(application.getCourse(), ""));
                row.createCell(6).setCellValue(safe(application.getSemester(), ""));
                row.createCell(7).setCellValue(joinProfileLabel(application));
                row.createCell(8).setCellValue(safe(application.getSkills(), ""));
                row.createCell(9).setCellValue(safe(application.getStatus(), "Submitted"));
                row.createCell(10).setCellValue(application.getAppliedAt() != null ? application.getAppliedAt().format(appliedAtFormat) : "");
                row.createCell(11).setCellValue(safe(application.getResumeName(), "Not uploaded"));
                rowIndex++;
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(outputStream);
        }
    }

    @GetMapping("/placement-applications/export-resumes.zip")
    public void exportApplicationResumes(
            HttpSession session,
            HttpServletResponse response,
            @RequestParam(required = false) String company) throws IOException {
        PlacementTpo tpo = getLoggedTpo(session);
        if (tpo == null) {
            response.sendRedirect("/login-placement");
            return;
        }

        List<PlacementApplication> applications = filterApplicationsByCompany(company);
        String fileName = "placement-resumes-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")) + ".zip";

        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream())) {
            for (PlacementApplication application : applications) {
                String resumePath = application.getResumePath();
                if (resumePath == null || resumePath.isBlank()) {
                    continue;
                }

                Path absolutePath = resolveResumePath(resumePath);
                if (!Files.exists(absolutePath) || Files.isDirectory(absolutePath)) {
                    continue;
                }

                String entryName = safeFileName(
                        safe(application.getPlacementDrive() != null ? application.getPlacementDrive().getCompanyName() : "Company", "Company")
                                + "-" + safe(application.getFullName(), "Student")
                                + "-" + safe(application.getResumeName(), "resume"));
                zipOutputStream.putNextEntry(new ZipEntry(entryName));
                Files.copy(absolutePath, zipOutputStream);
                zipOutputStream.closeEntry();
            }
        }
    }

    @GetMapping("/placement-students")
    public String students(Model model, HttpSession session) {
        PlacementTpo tpo = getLoggedTpo(session);
        if (tpo == null) return "redirect:/login-placement";
        addWorkspaceModel(model, tpo);
        return "placement/placement-students";
    }

    @GetMapping("/placement-companies")
    public String companies(Model model, HttpSession session) {
        PlacementTpo tpo = getLoggedTpo(session);
        if (tpo == null) return "redirect:/login-placement";
        addWorkspaceModel(model, tpo);
        return "placement/placement-companies";
    }

    @GetMapping("/placement-interviews")
    public String interviews(Model model, HttpSession session) {
        PlacementTpo tpo = getLoggedTpo(session);
        if (tpo == null) return "redirect:/login-placement";
        addWorkspaceModel(model, tpo);
        addInterviewModel(model, tpo);
        return "placement/placement-interviews";
    }

    @GetMapping("/placement-analytics")
    public String analytics(Model model, HttpSession session) {
        PlacementTpo tpo = getLoggedTpo(session);
        if (tpo == null) return "redirect:/login-placement";
        addWorkspaceModel(model, tpo);
        return "placement/placement-analytics";
    }

    @GetMapping("/placement-reports")
    public String reports(Model model, HttpSession session) {
        PlacementTpo tpo = getLoggedTpo(session);
        if (tpo == null) return "redirect:/login-placement";
        addWorkspaceModel(model, tpo);
        return "placement/placement-reports";
    }

    @GetMapping("/placement-announcements")
    public String announcements(Model model, HttpSession session) {
        PlacementTpo tpo = getLoggedTpo(session);
        if (tpo == null) return "redirect:/login-placement";
        addWorkspaceModel(model, tpo);
        return "placement/placement-announcements";
    }

    @GetMapping("/placement-profile")
    public String profile(Model model, HttpSession session) {
        PlacementTpo tpo = getLoggedTpo(session);
        if (tpo == null) return "redirect:/login-placement";
        addWorkspaceModel(model, tpo);
        return "placement/placement-profile";
    }

    @GetMapping("/placement-settings")
    public String settings(Model model, HttpSession session) {
        return "redirect:/placement-profile";
    }

    @GetMapping("/placements")
    public String studentPlacements(Model model, HttpSession session) {
        Student student = getLoggedStudent(session);
        if (student == null) return "redirect:/login";

        List<PlacementDrive> drives = placementDriveRepository.findByPublishedTrueOrderByCreatedAtDesc().stream()
                .filter(drive -> driveVisibleToStudent(drive, student))
                .toList();

        List<Long> appliedDriveIds = placementApplicationRepository.findByStudentOrderByAppliedAtDesc(student).stream()
                .map(app -> app.getPlacementDrive().getId())
                .toList();
        List<PlacementInterview> studentInterviews = loadStudentInterviews(student);

        model.addAttribute("student", student);
        model.addAttribute("studentName", safe(student.getName(), "Student"));
        model.addAttribute("studentCourse", safe(student.getCourse(), "Not assigned"));
        model.addAttribute("studentSemesterLabel", safe(student.getSemester(), "Not assigned"));
        model.addAttribute("studentEmail", safe(student.getEmail(), "Not available"));
        model.addAttribute("studentInitials", buildInitials(student.getName()));
        model.addAttribute("collegeName", resolveStudentCollegeName(student));
        model.addAttribute("collegeLogoPath", resolveStudentCollegeLogo(student));
        model.addAttribute("collegeInitials", buildCollegeInitials(resolveStudentCollegeName(student)));
        model.addAttribute("notificationCount", 0);
        model.addAttribute("placementDrives", drives);
        model.addAttribute("placementDriveCount", drives.size());
        model.addAttribute("placementAppliedDriveIds", appliedDriveIds);
        model.addAttribute("studentPlacementInterviews", studentInterviews);
        model.addAttribute("placementOpenDriveIds", drives.stream()
                .filter(PlacementDrive::isApplicationOpen)
                .map(PlacementDrive::getId)
                .toList());
        model.addAttribute("placementEmptyMessage", "No placement drives are live for your profile yet.");
        return "student/connected/placements-v2";
    }

    @PostMapping("/placement-drives")
    public String createDrive(
            @RequestParam String companyName,
            @RequestParam String jobTitle,
            @RequestParam(required = false) String packageOffered,
            @RequestParam String location,
            @RequestParam(required = false) String driveType,
            @RequestParam(required = false) String eligibilityCourse,
            @RequestParam(required = false) String eligibilitySemester,
            @RequestParam(required = false) String eligibilityDegree,
            @RequestParam(required = false) String experience,
            @RequestParam(required = false) String salaryRange,
            @RequestParam(required = false) String skillsRequired,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String driveDate,
            @RequestParam(required = false) String applicationDeadlineDate,
            @RequestParam(required = false) String applicationDeadlineTime,
            @RequestParam(required = false) Integer openings,
            @RequestParam(defaultValue = "true") boolean published,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        PlacementTpo tpo = getLoggedTpo(session);
        if (tpo == null) return "redirect:/login-placement";

        PlacementDrive drive = new PlacementDrive();
        drive.setCompanyName(safe(companyName, "Company"));
        drive.setJobTitle(safe(jobTitle, "Hiring"));
        drive.setPackageOffered(safe(packageOffered, "Not disclosed"));
        drive.setLocation(safe(location, "On campus"));
        drive.setDriveType(safe(driveType, "On Campus"));
        drive.setExperience(normalizeBlank(experience));
        drive.setSalaryRange(normalizeBlank(salaryRange != null ? salaryRange : packageOffered));
        drive.setEligibilityCourse(normalizeBlank(eligibilityCourse));
        drive.setEligibilitySemester(normalizeBlank(eligibilitySemester));
        drive.setEligibilityDegree(normalizeBlank(eligibilityDegree));
        drive.setSkillsRequired(normalizeBlank(skillsRequired));
        drive.setDescription(normalizeBlank(description));
        drive.setOpenings(openings != null ? openings : 1);
        drive.setDriveDate(parseDate(driveDate));
        drive.setApplicationDeadline(parseDeadline(applicationDeadlineDate, applicationDeadlineTime));
        drive.setPublished(published);
        drive.setCreatedAt(LocalDateTime.now());
        drive.setPlacementTpo(tpo);
        placementDriveRepository.save(drive);

        redirectAttributes.addFlashAttribute("placementSuccess", "Placement drive published successfully.");
        return "redirect:/placement-company-drives";
    }

    @PostMapping("/placements/apply/{driveId}")
    public String apply(
            @PathVariable Long driveId,
            @RequestParam String fullName,
            @RequestParam String phone,
            @RequestParam String email,
            @RequestParam String course,
            @RequestParam String semester,
            @RequestParam(required = false) String specialization,
            @RequestParam(required = false) String currentLocation,
            @RequestParam(required = false) String skills,
            @RequestParam(required = false) String coverNote,
            @RequestParam(value = "resume", required = false) MultipartFile resumeFile,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Student student = getLoggedStudent(session);
        if (student == null) return "redirect:/login";

        PlacementDrive drive = placementDriveRepository.findById(driveId).orElse(null);
        if (drive == null || !drive.isPublished() || !driveVisibleToStudent(drive, student)) {
            redirectAttributes.addFlashAttribute("placementError", "This drive is not available for your profile.");
            return "redirect:/placements";
        }

        if (!drive.isApplicationOpen()) {
            redirectAttributes.addFlashAttribute("placementError", "Application deadline has closed for this drive.");
            return "redirect:/placements";
        }

        if (placementApplicationRepository.existsByPlacementDriveAndEmail(drive, email.trim())) {
            redirectAttributes.addFlashAttribute("placementError", "You have already applied for this company.");
            return "redirect:/placements";
        }

        String resumePath = saveResume(resumeFile);
        if (resumeFile != null && !resumeFile.isEmpty() && resumePath == null) {
            redirectAttributes.addFlashAttribute("placementError", "Resume upload failed. Use PDF/DOC/DOCX under 10 MB.");
            return "redirect:/placements";
        }

        PlacementApplication application = new PlacementApplication();
        application.setPlacementDrive(drive);
        application.setStudent(student);
        application.setFullName(safe(fullName, student.getName()));
        application.setPhone(normalizeBlank(phone));
        application.setEmail(normalizeBlank(email));
        application.setCourse(normalizeBlank(course));
        application.setSemester(normalizeBlank(semester));
        application.setSpecialization(normalizeBlank(specialization));
        application.setCurrentLocation(normalizeBlank(currentLocation));
        application.setSkills(normalizeBlank(skills));
        application.setCoverNote(normalizeBlank(coverNote));
        application.setResumePath(resumePath);
        application.setResumeName(resumeFile != null ? normalizeBlank(resumeFile.getOriginalFilename()) : null);
        application.setStatus("Submitted");
        application.setAppliedAt(LocalDateTime.now());
        placementApplicationRepository.save(application);

        redirectAttributes.addFlashAttribute("placementSuccess", "Your application has been sent to the placement TPO.");
        return "redirect:/placements";
    }

    @PostMapping("/placement-interviews")
    public String createInterview(
            @RequestParam Long placementDriveId,
            @RequestParam(required = false) String interviewRound,
            @RequestParam String interviewDate,
            @RequestParam String interviewTime,
            @RequestParam(required = false) String venue,
            @RequestParam(required = false) String meetingLink,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) List<Long> selectedStudentIds,
            @RequestParam(defaultValue = "Scheduled") String status,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        PlacementTpo tpo = getLoggedTpo(session);
        if (tpo == null) return "redirect:/login-placement";

        PlacementDrive drive = placementDriveRepository.findById(placementDriveId).orElse(null);
        if (drive == null || drive.getPlacementTpo() == null || !drive.getPlacementTpo().getId().equals(tpo.getId())) {
            redirectAttributes.addFlashAttribute("placementInterviewError", "Select one of your own published drives.");
            return "redirect:/placement-interviews";
        }

        LocalDateTime interviewDateTime = parseDateTime(interviewDate, interviewTime);
        if (interviewDateTime == null) {
            redirectAttributes.addFlashAttribute("placementInterviewError", "Enter a valid interview date and time.");
            return "redirect:/placement-interviews";
        }

        List<PlacementApplication> driveApplications = placementApplicationRepository.findByPlacementDriveOrderByAppliedAtDesc(drive);
        List<Student> selectedStudents = resolveSelectedStudents(selectedStudentIds, driveApplications);
        if (selectedStudents.isEmpty()) {
            redirectAttributes.addFlashAttribute("placementInterviewError", "Select at least one applicant from this company drive.");
            return "redirect:/placement-interviews";
        }

        PlacementInterview interview = new PlacementInterview();
        interview.setPlacementDrive(drive);
        interview.setPlacementTpo(tpo);
        interview.setInterviewRound(safe(interviewRound, "Interview Round"));
        interview.setInterviewDateTime(interviewDateTime);
        interview.setVenue(normalizeBlank(venue));
        interview.setMeetingLink(normalizeBlank(meetingLink));
        interview.setNotes(normalizeBlank(notes));
        interview.setStatus(safe(status, "Scheduled"));
        interview.setCreatedAt(LocalDateTime.now());
        interview.setSelectedStudents(selectedStudents);
        placementInterviewRepository.save(interview);

        notifyInterviewCandidates(interview);

        redirectAttributes.addFlashAttribute("placementInterviewSuccess", "Interview scheduled successfully.");
        return "redirect:/placement-interviews";
    }

    private void addWorkspaceModel(Model model, PlacementTpo tpo) {
        List<PlacementDrive> drives = placementDriveRepository.findByPlacementTpoOrderByCreatedAtDesc(tpo);
        List<PlacementApplication> applications = collectApplications(drives);
        List<PlacementInterview> interviews = placementInterviewRepository.findByPlacementTpoOrderByInterviewDateTimeDesc(tpo);
        List<Student> students = resolveStudentsForDashboard(tpo);
        List<String> companies = drives.stream()
                .map(PlacementDrive::getCompanyName)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        model.addAttribute("placementTpo", tpo);
        model.addAttribute("placementTpoEmployeeId", tpo.getEmployeeId());
        model.addAttribute("placementTpoName", tpo.getName());
        model.addAttribute("placementTpoEmail", tpo.getEmail());
        model.addAttribute("placementTpoCollege", tpo.getCollegeName());
        model.addAttribute("collegeName", safe(tpo.getCollegeName(), "AI Campus Institute"));
        model.addAttribute("collegeInitials", buildCollegeInitials(safe(tpo.getCollegeName(), "AI Campus Institute")));
        model.addAttribute("dashboardGreeting", dashboardGreeting());
        model.addAttribute("dashboardAcademicYear", currentAcademicYear());
        model.addAttribute("placementTpoPhone", tpo.getPhone());
        model.addAttribute("placementTpoDepartment", tpo.getDepartment());
        model.addAttribute("placementTpoDesignation", tpo.getDesignation());
        model.addAttribute("placementTpoStatus", tpo.getStatus());
        model.addAttribute("placementTpoInitials", buildInitials(tpo.getName()));
        model.addAttribute("placementDriveCount", drives.size());
        model.addAttribute("placementApplicationCount", applications.size());
        model.addAttribute("placementInterviewCount", interviews.size());
        model.addAttribute("placementPublishedCount", drives.stream().filter(PlacementDrive::isPublished).count());
        model.addAttribute("placementStudentCount", students.size());
        model.addAttribute("placementPlacedCount", applications.size());
        model.addAttribute("placementCompanyCount", companies.size());
        model.addAttribute("placementDrives", drives);
        model.addAttribute("placementApplications", applications);
        model.addAttribute("placementInterviews", interviews);
        model.addAttribute("placementStudents", students);
        model.addAttribute("placementCompanies", companies);
        model.addAttribute("recentPlacementApplications", applications.stream().limit(8).toList());
        model.addAttribute("recentPlacementApplicationsCount", applications.stream().limit(8).count());
        model.addAttribute("recentPlacementInterviews", interviews.stream().limit(8).toList());
        model.addAttribute("recentPlacementInterviewsCount", interviews.stream().limit(8).count());
        model.addAttribute("placementDriveOpenCount", drives.stream().filter(PlacementDrive::isApplicationOpen).count());
        model.addAttribute("placementDriveClosedCount", drives.stream().filter(drive -> !drive.isApplicationOpen()).count());
    }

    private void addInterviewModel(Model model, PlacementTpo tpo) {
        List<PlacementDrive> drives = placementDriveRepository.findByPlacementTpoOrderByCreatedAtDesc(tpo);
        List<PlacementInterview> interviews = placementInterviewRepository.findByPlacementTpoOrderByInterviewDateTimeDesc(tpo);
        List<PlacementDriveApplicantGroup> applicantGroups = buildDriveApplicantGroups(drives);
        Map<Long, Integer> applicantCounts = new java.util.HashMap<>();
        for (PlacementInterview interview : interviews) {
            applicantCounts.put(interview.getId(), interview.getSelectedStudents() == null ? 0 : interview.getSelectedStudents().size());
        }

        model.addAttribute("placementInterviewDrives", drives);
        model.addAttribute("placementInterviewApplicantCounts", applicantCounts);
        model.addAttribute("placementInterviews", interviews);
        model.addAttribute("placementDriveApplicantGroups", applicantGroups);
    }

    private List<PlacementApplication> collectApplications(List<PlacementDrive> drives) {
        List<PlacementApplication> applications = new ArrayList<>();
        for (PlacementDrive drive : drives) {
            applications.addAll(placementApplicationRepository.findByPlacementDriveOrderByAppliedAtDesc(drive));
        }
        if (applications.isEmpty()) {
            applications.addAll(placementApplicationRepository.findAll());
        }
        applications.sort((a, b) -> {
            LocalDateTime left = a.getAppliedAt();
            LocalDateTime right = b.getAppliedAt();
            if (left == null && right == null) return 0;
            if (left == null) return 1;
            if (right == null) return -1;
            return right.compareTo(left);
        });
        return applications;
    }

    private List<PlacementApplication> filterApplicationsByCompany(String company) {
        String normalized = normalize(company);
        if (normalized.isBlank()) {
            return placementApplicationRepository.findAll().stream()
                    .sorted((a, b) -> {
                        LocalDateTime left = a.getAppliedAt();
                        LocalDateTime right = b.getAppliedAt();
                        if (left == null && right == null) return 0;
                        if (left == null) return 1;
                        if (right == null) return -1;
                        return right.compareTo(left);
                    })
                    .toList();
        }
        return placementApplicationRepository.findAll().stream()
                .filter(application -> application.getPlacementDrive() != null
                        && normalize(application.getPlacementDrive().getCompanyName()).contains(normalized))
                .sorted((a, b) -> {
                    LocalDateTime left = a.getAppliedAt();
                    LocalDateTime right = b.getAppliedAt();
                    if (left == null && right == null) return 0;
                    if (left == null) return 1;
                    if (right == null) return -1;
                    return right.compareTo(left);
                })
                .toList();
    }

    private List<PlacementInterview> loadStudentInterviews(Student student) {
        return placementInterviewRepository.findForSelectedStudent(student);
    }

    private String joinProfileLabel(PlacementApplication application) {
        String specialization = safe(application.getSpecialization(), "Specialization n/a");
        String location = safe(application.getCurrentLocation(), "Location n/a");
        return specialization + " | " + location;
    }

    private Path resolveResumePath(String resumePath) {
        String normalized = resumePath.startsWith("/") ? resumePath.substring(1) : resumePath;
        return Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "static", normalized);
    }

    private String safeFileName(String value) {
        return value == null ? "file" : value.replaceAll("[^a-zA-Z0-9._-]+", "_");
    }

    private PlacementTpo getLoggedTpo(HttpSession session) {
        Object loggedInUserId = session.getAttribute("loggedInUserId");
        if (loggedInUserId instanceof Long id) {
            return placementTpoRepository.findById(id)
                    .map(employeeIdService::ensureTpoEmployeeId)
                    .orElse(null);
        }
        if (loggedInUserId instanceof Number number) {
            return placementTpoRepository.findById(number.longValue())
                    .map(employeeIdService::ensureTpoEmployeeId)
                    .orElse(null);
        }

        String email = (String) session.getAttribute("loggedInUser");
        String role = (String) session.getAttribute("userRole");
        if (email == null || (role != null && !"PLACEMENT_TPO".equalsIgnoreCase(role))) {
            return null;
        }
        List<PlacementTpo> candidates = placementTpoRepository.findByEmail(email);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1) {
            return employeeIdService.ensureTpoEmployeeId(candidates.get(0));
        }
        return candidates.stream()
                .filter(candidate -> candidate != null
                        && candidate.getEmail() != null
                        && candidate.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .map(employeeIdService::ensureTpoEmployeeId)
                .orElseGet(() -> employeeIdService.ensureTpoEmployeeId(candidates.get(0)));
    }

    private Student getLoggedStudent(HttpSession session) {
        String email = (String) session.getAttribute("loggedInUser");
        String role = (String) session.getAttribute("userRole");
        if (email == null || (role != null && !"STUDENT".equalsIgnoreCase(role))) {
            return null;
        }
        return studentRepository.findByEmail(email);
    }

    private boolean driveVisibleToStudent(PlacementDrive drive, Student student) {
        String studentCourse = normalize(student.getCourse());
        String studentSemester = normalize(student.getSemester());
        String eligibleCourse = normalize(drive.getEligibilityCourse());
        String eligibleSemester = normalize(drive.getEligibilitySemester());
        String eligibleDegree = normalize(drive.getEligibilityDegree());

        boolean courseOk = eligibleCourse.isBlank() || studentCourse.contains(eligibleCourse) || eligibleCourse.contains(studentCourse);
        boolean semesterOk = eligibleSemester.isBlank() || studentSemester.equals(eligibleSemester) || studentSemester.contains(eligibleSemester);
        boolean degreeOk = eligibleDegree.isBlank() || normalize(student.getDegree()).contains(eligibleDegree) || eligibleDegree.contains(normalize(student.getDegree()));
        return courseOk && semesterOk && degreeOk;
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ENGLISH);
    }

    private LocalDateTime parseDeadline(String dateValue, String timeValue) {
        if (dateValue == null || dateValue.isBlank()) return null;
        try {
            LocalDate date = LocalDate.parse(dateValue.trim());
            LocalTime time = (timeValue == null || timeValue.isBlank()) ? LocalTime.of(23, 59) : LocalTime.parse(timeValue.trim());
            return LocalDateTime.of(date, time);
        } catch (Exception ignored) {
            return null;
        }
    }

    private LocalDateTime parseDateTime(String dateValue, String timeValue) {
        if (dateValue == null || dateValue.isBlank() || timeValue == null || timeValue.isBlank()) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(dateValue.trim());
            LocalTime time = LocalTime.parse(timeValue.trim());
            return LocalDateTime.of(date, time);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String saveResume(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;

        String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase(Locale.ENGLISH) : "";
        boolean allowed = name.endsWith(".pdf") || name.endsWith(".doc") || name.endsWith(".docx");
        if (!allowed || file.getSize() > 10L * 1024 * 1024) {
            return null;
        }

        File dir = new File(UPLOAD_DIR_RESUMES);
        if (!dir.exists() && !dir.mkdirs()) return null;

        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "resume.pdf";
        String ext = original.contains(".") ? original.substring(original.lastIndexOf(".")) : ".pdf";
        String stored = UUID.randomUUID() + ext;
        try {
            Files.copy(file.getInputStream(), Paths.get(UPLOAD_DIR_RESUMES + stored), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/placements/resumes/" + stored;
        } catch (IOException e) {
            return null;
        }
    }

    private List<Student> resolveStudentsForDashboard(PlacementTpo tpo) {
        if (tpo == null) return List.of();
        if (tpo.getAdmin() != null) {
            return studentRepository.findByAdmin(tpo.getAdmin());
        }
        return studentRepository.findAll().stream()
                .filter(student -> student.getAdmin() != null
                        && student.getAdmin().getCollegeName() != null
                        && student.getAdmin().getCollegeName().equalsIgnoreCase(safe(tpo.getCollegeName(), "")))
                .toList();
    }

    private String resolveTpoCollegeCode(PlacementTpo tpo) {
        if (tpo == null) {
            return null;
        }
        if (tpo.getCollege() != null && tpo.getCollege().getCode() != null && !tpo.getCollege().getCode().isBlank()) {
            return tpo.getCollege().getCode().trim();
        }
        if (tpo.getAdmin() != null && tpo.getAdmin().getCollegeCode() != null && !tpo.getAdmin().getCollegeCode().isBlank()) {
            return tpo.getAdmin().getCollegeCode().trim();
        }
        return null;
    }

    private String buildInitials(String name) {
        if (name == null || name.isBlank()) return "ST";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ENGLISH);
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ENGLISH);
    }

    private String buildCollegeInitials(String collegeName) {
        if (collegeName == null || collegeName.isBlank()) {
            return "AI";
        }
        String[] parts = collegeName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ENGLISH);
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ENGLISH);
    }

    private String firstNonBlank(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String resolveStudentCollegeName(Student student) {
        if (student != null && student.getAdmin() != null) {
            return safe(student.getAdmin().getCollegeName(), "AI Campus Institute");
        }
        return "AI Campus Institute";
    }

    private String resolveStudentCollegeLogo(Student student) {
        if (student != null && student.getAdmin() != null && student.getAdmin().getCollege() != null) {
            return student.getAdmin().getCollege().getLogoPath();
        }
        return null;
    }

    private String dashboardGreeting() {
        int hour = LocalTime.now().getHour();
        if (hour < 12) {
            return "Good Morning";
        }
        if (hour < 17) {
            return "Good Afternoon";
        }
        return "Good Evening";
    }

    private String currentAcademicYear() {
        int year = LocalDate.now().getYear();
        int start = LocalDate.now().getMonthValue() >= 6 ? year : year - 1;
        return start + "-" + String.valueOf(start + 1).substring(2);
    }

    private void upgradeLegacyPasswordIfNeeded(PlacementTpo tpo, String rawPassword) {
        if (passwordProtectionService.needsUpgrade(tpo.getPassword())) {
            tpo.setPassword(passwordProtectionService.encode(rawPassword));
            placementTpoRepository.save(tpo);
        }
    }

    private PlacementTpo resolvePlacementTpoForLogin(String email, String password) {
        List<PlacementTpo> candidates = placementTpoRepository.findByEmail(email);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        return candidates.stream()
                .filter(candidate -> candidate != null
                        && candidate.getEmail() != null
                        && candidate.getEmail().equalsIgnoreCase(email)
                        && passwordProtectionService.matches(password, candidate.getPassword()))
                .findFirst()
                .orElse(null);
    }

    private void notifyInterviewCandidates(PlacementInterview interview) {
        if (interview == null || interview.getSelectedStudents() == null || interview.getSelectedStudents().isEmpty()) {
            return;
        }

        String companyName = safe(interview.getPlacementDrive() != null ? interview.getPlacementDrive().getCompanyName() : null, "Company");
        String roundName = safe(interview.getInterviewRound(), "Interview Round");
        String dateLabel = interview.getInterviewDateTime() != null
                ? interview.getInterviewDateTime().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))
                : "To be announced";
        String details = buildInterviewDetails(interview);

        for (Student student : interview.getSelectedStudents()) {
            if (student == null) {
                continue;
            }
            sendInterviewEmail(student.getEmail(), student.getName(), companyName, roundName, dateLabel, details);
        }
    }

    private String buildInterviewDetails(PlacementInterview interview) {
        List<String> parts = new ArrayList<>();
        if (interview.getVenue() != null && !interview.getVenue().isBlank()) {
            parts.add("Venue: " + interview.getVenue());
        }
        if (interview.getMeetingLink() != null && !interview.getMeetingLink().isBlank()) {
            parts.add("Link: " + interview.getMeetingLink());
        }
        if (interview.getNotes() != null && !interview.getNotes().isBlank()) {
            parts.add("Notes: " + interview.getNotes());
        }
        return String.join("\n", parts);
    }

    private void sendInterviewEmail(
            String toEmail,
            String studentName,
            String companyName,
            String roundName,
            String dateLabel,
            String details) {

        if (toEmail == null || toEmail.isBlank() || javaMailSender == null) {
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail.trim());
            message.setSubject("Interview scheduled for " + companyName);
            message.setText(
                    "Hello " + safe(studentName, "Student") + ",\n\n"
                            + "Your interview has been scheduled.\n\n"
                            + "Company: " + companyName + "\n"
                            + "Round: " + roundName + "\n"
                            + "Date and Time: " + dateLabel + "\n"
                            + (details.isBlank() ? "" : details + "\n")
                            + "\nPlease check your placement notifications for the latest updates.\n");
            javaMailSender.send(message);
        } catch (Exception ignored) {
            // Keep scheduling working even when SMTP is not configured.
        }
    }

    private List<PlacementDriveApplicantGroup> buildDriveApplicantGroups(List<PlacementDrive> drives) {
        List<PlacementDriveApplicantGroup> groups = new ArrayList<>();
        for (PlacementDrive drive : drives) {
            List<PlacementApplication> applications = placementApplicationRepository.findByPlacementDriveOrderByAppliedAtDesc(drive);
            groups.add(new PlacementDriveApplicantGroup(drive, applications));
        }
        return groups;
    }

    private List<Student> resolveSelectedStudents(List<Long> selectedStudentIds, List<PlacementApplication> driveApplications) {
        if (selectedStudentIds == null || selectedStudentIds.isEmpty() || driveApplications == null || driveApplications.isEmpty()) {
            return List.of();
        }

        Map<Long, Student> applicantsById = new java.util.LinkedHashMap<>();
        for (PlacementApplication application : driveApplications) {
            if (application == null || application.getStudent() == null || application.getStudent().getId() == null) {
                continue;
            }
            applicantsById.put(application.getStudent().getId(), application.getStudent());
        }

        List<Student> selectedStudents = new ArrayList<>();
        for (Long selectedId : selectedStudentIds) {
            if (selectedId == null) {
                continue;
            }
            Student applicant = applicantsById.get(selectedId);
            if (applicant != null && selectedStudents.stream().noneMatch(student -> student.getId().equals(applicant.getId()))) {
                selectedStudents.add(applicant);
            }
        }
        return selectedStudents;
    }

    private record PlacementDriveApplicantGroup(PlacementDrive drive, List<PlacementApplication> applications) {}
}
