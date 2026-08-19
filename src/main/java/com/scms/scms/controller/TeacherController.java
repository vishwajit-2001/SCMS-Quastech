package com.scms.scms.controller;

import com.scms.scms.model.Admin;
import com.scms.scms.model.Assignment;
import com.scms.scms.model.AssignmentSubmission;
import com.scms.scms.model.ClassRoom;
import com.scms.scms.model.ExamSession;
import com.scms.scms.model.Student;
import com.scms.scms.model.StudyMaterial;
import com.scms.scms.model.Subject;
import com.scms.scms.model.SubjectMaster;
import com.scms.scms.model.Teacher;
import com.scms.scms.model.TeacherAttendanceEntry;
import com.scms.scms.model.TeacherAttendanceSession;
import com.scms.scms.model.Timetable;
import com.scms.scms.repository.AssignmentRepository;
import com.scms.scms.repository.AssignmentSubmissionRepository;
import com.scms.scms.repository.StudentRepository;
import com.scms.scms.repository.StudyMaterialRepository;
import com.scms.scms.repository.SubjectMasterRepository;
import com.scms.scms.repository.SubjectRepository;
import com.scms.scms.repository.TeacherRepository;
import com.scms.scms.repository.TeacherAttendanceEntryRepository;
import com.scms.scms.repository.TeacherAttendanceSessionRepository;
import com.scms.scms.repository.TimetableRepository;
import com.scms.scms.service.AcademicStructureService;
import com.scms.scms.service.EmployeeIdService;
import com.scms.scms.service.ExamAutomationService;
import com.scms.scms.service.TeacherAcademicMappingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
public class TeacherController {

    @Autowired private StudentRepository studentRepository;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private AssignmentSubmissionRepository assignmentSubmissionRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private StudyMaterialRepository studyMaterialRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private SubjectMasterRepository subjectMasterRepository;
    @Autowired private TimetableRepository timetableRepository;
    @Autowired private TeacherAttendanceSessionRepository teacherAttendanceSessionRepository;
    @Autowired private TeacherAttendanceEntryRepository teacherAttendanceEntryRepository;
    @Autowired private AcademicStructureService academicStructureService;
    @Autowired private EmployeeIdService employeeIdService;
    @Autowired private ExamAutomationService examAutomationService;
    @Autowired private TeacherAcademicMappingService teacherAcademicMappingService;

    private static final List<String> WORKING_DAYS = List.of(
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    );
    private static final String UPLOAD_DIR_ASSIGNMENTS =
            System.getProperty("user.dir") + "/src/main/resources/static/uploads/assignments/";
    private static final String UPLOAD_DIR_ASSIGNMENT_SUBMISSIONS =
            System.getProperty("user.dir") + "/src/main/resources/static/uploads/assignment-submissions/";

    @GetMapping("/teacher-dashboard")
    public String dashboard(Model model, HttpSession session) {
        Teacher teacher = getLoggedTeacher(session);
        if (teacher == null) return "redirect:/login";

        List<Student> students = loadVisibleStudents(teacher);
        model.addAttribute("totalStudents", students.size());
        model.addAttribute("students", students);
        addStudyMaterialAttributes(model, teacher);
        addTeacherAttributes(model, teacher);
        addTeacherTimetableAttributes(model, teacher);
        addTeacherDashboardAttributes(model, teacher, students);
        addTeacherExamAttributes(model, teacher);
        return "teacher/teacher-dashboard";
    }

    @GetMapping("/teacher-students")
    public String students(Model model, HttpSession session) {
        Teacher teacher = getLoggedTeacher(session);
        if (teacher == null) return "redirect:/login";

        List<Student> students = loadVisibleStudents(teacher);
        model.addAttribute("students", students);
        model.addAttribute("totalStudents", students.size());
        addTeacherAttributes(model, teacher);
        addTeacherExamAttributes(model, teacher);
        return "teacher/teacher-students";
    }

    @GetMapping("/teacher-subjects")
    public String subjects(Model model, HttpSession session) {
        Teacher teacher = getLoggedTeacher(session);
        if (teacher == null) return "redirect:/login";
        addTeacherAttributes(model, teacher);
        addTeacherExamAttributes(model, teacher);
        return "teacher/teacher-subjects";
    }

    @GetMapping("/teacher-attendance")
    public String attendance(@RequestParam Map<String, String> filters, Model model, HttpSession session) {
        Teacher teacher = getLoggedTeacher(session);
        if (teacher == null) return "redirect:/login";

        model.addAttribute("students", loadVisibleStudents(teacher));
        addTeacherAttendanceAttributes(model, teacher, filters);
        addTeacherAttributes(model, teacher);
        addTeacherExamAttributes(model, teacher);
        return "teacher/teacher-attendance";
    }

    @PostMapping("/teacher-attendance-submit")
    @Transactional
    public String submitAttendance(@RequestParam Map<String, String> params,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        Teacher teacher = getLoggedTeacher(session);
        if (teacher == null) return "redirect:/login";

        Long slotId = asLong(params.get("timetableSlotId"));
        Timetable slot = slotId != null ? timetableRepository.findById(slotId).orElse(null) : null;
        if (slot == null || slot.getTeacher() == null || !Objects.equals(slot.getTeacher().getId(), teacher.getId())) {
            redirectAttributes.addFlashAttribute("attendanceError", "Select one of your timetable slots before saving attendance.");
            return "redirect:/teacher-attendance";
        }
        if ("BREAK".equalsIgnoreCase(slot.getEntryType())) {
            redirectAttributes.addFlashAttribute("attendanceError", "Attendance can be marked only for teaching slots.");
            return "redirect:/teacher-attendance";
        }

        LocalDate attendanceDate = parseDate(params.get("attendanceDate"), LocalDate.now());
        String lectureNo = firstNonBlank(params.get("lectureNo"), slot.getDay() + " " + slot.getStartTime() + "-" + slot.getEndTime());
        String subject = resolveTimetableSubjectLabel(slot);

        TeacherAttendanceSession attendanceSession = teacherAttendanceSessionRepository
                .findFirstByTeacherAndClassRoomAndSubjectAndAttendanceDateAndLectureNoOrderByCreatedAtDesc(
                        teacher,
                        slot.getClassRoom(),
                        subject,
                        attendanceDate,
                        lectureNo
                );
        if (attendanceSession == null) {
            attendanceSession = new TeacherAttendanceSession();
            attendanceSession.setTeacher(teacher);
            attendanceSession.setClassRoom(slot.getClassRoom());
            attendanceSession.setSubject(subject);
            attendanceSession.setAttendanceDate(attendanceDate);
            attendanceSession.setLectureNo(lectureNo);
            attendanceSession.setCreatedAt(LocalDateTime.now());
            attendanceSession = teacherAttendanceSessionRepository.save(attendanceSession);
        } else {
            teacherAttendanceEntryRepository.deleteBySession(attendanceSession);
        }

        List<Student> eligibleStudents = loadStudentsForTimetableSlot(teacher, slot);
        LocalDateTime markedAt = LocalDateTime.now();
        for (Student student : eligibleStudents) {
            String status = normalizeAttendanceStatus(params.get("status_" + student.getId()));
            TeacherAttendanceEntry entry = new TeacherAttendanceEntry();
            entry.setSession(attendanceSession);
            entry.setStudent(student);
            entry.setStatus(status);
            entry.setMarkedAt(markedAt);
            teacherAttendanceEntryRepository.save(entry);
        }

        redirectAttributes.addFlashAttribute("success", "Attendance saved for " + subject + " (" + eligibleStudents.size() + " students).");
        return "redirect:/teacher-attendance";
    }

    @PostMapping("/teacher-attendance-entry/{entryId}/status")
    @Transactional
    public String updateAttendanceEntry(@PathVariable Long entryId,
                                        @RequestParam String status,
                                        HttpSession session,
                                        RedirectAttributes redirectAttributes) {
        Teacher teacher = getLoggedTeacher(session);
        if (teacher == null) return "redirect:/login";

        TeacherAttendanceEntry entry = teacherAttendanceEntryRepository.findById(entryId).orElse(null);
        if (entry == null || entry.getSession() == null || entry.getSession().getTeacher() == null
                || !Objects.equals(entry.getSession().getTeacher().getId(), teacher.getId())) {
            redirectAttributes.addFlashAttribute("attendanceError", "Attendance row was not found for your account.");
            return "redirect:/teacher-attendance";
        }
        entry.setStatus(normalizeAttendanceStatus(status));
        entry.setMarkedAt(LocalDateTime.now());
        teacherAttendanceEntryRepository.save(entry);
        redirectAttributes.addFlashAttribute("success", "Attendance updated for roll no. " + studentRollLabel(entry.getStudent()) + ".");
        return "redirect:/teacher-attendance";
    }

    @GetMapping("/teacher-marks")
    public String marks(Model model, HttpSession session) {
        Teacher teacher = getLoggedTeacher(session);
        if (teacher == null) return "redirect:/login";

        model.addAttribute("students", loadVisibleStudents(teacher));
        addTeacherAttributes(model, teacher);
        addTeacherExamAttributes(model, teacher);
        return "teacher/teacher-marks";
    }

    @PostMapping("/teacher-marks-submit")
    public String submitMarks(Model model, HttpSession session) {
        Teacher teacher = getLoggedTeacher(session);
        if (teacher == null) return "redirect:/login";

        model.addAttribute("success", "Marks saved!");
        model.addAttribute("students", loadVisibleStudents(teacher));
        addTeacherAttributes(model, teacher);
        addTeacherExamAttributes(model, teacher);
        return "teacher/teacher-marks";
    }

    @GetMapping("/teacher-assignments")
    public String assignments(Model model, HttpSession session) {
        Teacher teacher = getLoggedTeacher(session);
        if (teacher == null) return "redirect:/login";
        addTeacherAssignmentAttributes(model, teacher);
        addTeacherAttributes(model, teacher);
        addTeacherExamAttributes(model, teacher);
        return "teacher/teacher-assignments";
    }

    @PostMapping("/teacher-assignment-add")
    public String addAssign(@RequestParam("title") String title,
                            @RequestParam(value = "description", required = false) String description,
                            @RequestParam("subject") String subject,
                            @RequestParam("dueDate") String dueDate,
                            @RequestParam(value = "maxMarks", required = false) Integer maxMarks,
                            @RequestParam(value = "assignmentFile", required = false) MultipartFile assignmentFile,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        Teacher teacher = getLoggedTeacher(session);
        if (teacher == null) return "redirect:/login";
        ClassRoom classRoom = resolveTeacherAssignmentClassRoom(teacher);
        if (classRoom == null) {
            redirectAttributes.addFlashAttribute("teacherAssignmentError", "Assign a class to this teacher before creating assignments.");
            return "redirect:/teacher-assignments";
        }
        if (title == null || title.isBlank() || subject == null || subject.isBlank() || dueDate == null || dueDate.isBlank()) {
            redirectAttributes.addFlashAttribute("teacherAssignmentError", "Title, subject, and due date are required.");
            return "redirect:/teacher-assignments";
        }

        LocalDate parsedDueDate;
        try {
            parsedDueDate = LocalDate.parse(dueDate);
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("teacherAssignmentError", "Use a valid due date.");
            return "redirect:/teacher-assignments";
        }

        String filePath = null;
        if (assignmentFile != null && !assignmentFile.isEmpty()) {
            filePath = saveAssignmentFile(assignmentFile, UPLOAD_DIR_ASSIGNMENTS);
            if (filePath == null) {
                redirectAttributes.addFlashAttribute("teacherAssignmentError", "Assignment file upload failed. Use PDF, DOC, DOCX, PPT, PPTX, XLS, XLSX, TXT, ZIP, PNG, or JPG under 25 MB.");
                return "redirect:/teacher-assignments";
            }
        }

        Assignment assignment = new Assignment();
        assignment.setTitle(title.trim());
        assignment.setDescription(description != null ? description.trim() : "");
        assignment.setSubject(subject.trim());
        assignment.setDueDate(parsedDueDate);
        assignment.setMaxMarks(maxMarks != null ? maxMarks : 20);
        assignment.setCreatedAt(LocalDateTime.now());
        assignment.setPublished(true);
        assignment.setTeacher(teacher);
        assignment.setClassRoom(classRoom);
        assignment.setAdmin(teacher.getAdmin());
        assignment.setCourse(firstNonBlank(classRoom.getCourse(), classRoom.getCourseCode()));
        assignment.setSemester(classRoom.getSemester() != null ? "Sem " + classRoom.getSemester() : null);
        assignment.setAcademicYear(classRoom.getAcademicYear());
        assignment.setSectionName(classRoom.getSection());
        assignment.setFilePath(filePath);
        if (filePath != null) {
            assignment.setOriginalFileName(assignmentFile.getOriginalFilename());
            assignment.setStoredFileName(filePath.substring(filePath.lastIndexOf('/') + 1));
            assignment.setContentType(assignmentFile.getContentType());
            assignment.setFileSize(assignmentFile.getSize());
        }
        assignmentRepository.save(assignment);

        redirectAttributes.addFlashAttribute("teacherAssignmentSuccess", "Assignment created and published for students.");
        return "redirect:/teacher-assignments";
    }

    @GetMapping("/teacher-assignments/file/{id}")
    public ResponseEntity<Resource> downloadTeacherAssignmentFile(@PathVariable Long id, HttpSession session) {
        Teacher teacher = getLoggedTeacher(session);
        if (teacher == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Assignment assignment = assignmentRepository.findById(id).orElse(null);
        if (assignment == null || assignment.getTeacher() == null || !Objects.equals(assignment.getTeacher().getId(), teacher.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return serveAssignmentFile(assignment.getFilePath(), assignment.getOriginalFileName(), assignment.getContentType(), true);
    }

    @GetMapping("/teacher-assignment-submissions/{submissionId}/download")
    public ResponseEntity<Resource> downloadTeacherSubmissionFile(@PathVariable Long submissionId, HttpSession session) {
        Teacher teacher = getLoggedTeacher(session);
        if (teacher == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        AssignmentSubmission submission = assignmentSubmissionRepository.findById(submissionId).orElse(null);
        if (submission == null || submission.getAssignment() == null || submission.getAssignment().getTeacher() == null
                || !Objects.equals(submission.getAssignment().getTeacher().getId(), teacher.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return serveAssignmentFile(submission.getFilePath(), submission.getOriginalFileName(), submission.getContentType(), true);
    }

    @GetMapping("/teacher-timetable")
    public String timetable(Model model, HttpSession session) {
        Teacher teacher = getLoggedTeacher(session);
        if (teacher == null) return "redirect:/login";
        addTeacherTimetableAttributes(model, teacher);
        addTeacherAttributes(model, teacher);
        addTeacherExamAttributes(model, teacher);
        return "teacher/teacher-timetable";
    }

    @GetMapping("/teacher-profile")
    public String profile(Model model, HttpSession session) {
        Teacher teacher = getLoggedTeacher(session);
        if (teacher == null) return "redirect:/login";
        addTeacherAttributes(model, teacher);
        addTeacherExamAttributes(model, teacher);
        return "teacher/teacher-profile";
    }

    @GetMapping("/teacher-notifications")
    public String notifications(Model model, HttpSession session) {
        Teacher teacher = getLoggedTeacher(session);
        if (teacher == null) return "redirect:/login";
        addTeacherAttributes(model, teacher);
        addTeacherExamAttributes(model, teacher);
        model.addAttribute("teacherNotifications", examAutomationService.findVisibleNotifications("TEACHER", teacher.getEmail()));
        model.addAttribute("teacherUnreadNotifications", examAutomationService.countUnreadNotifications("TEACHER", teacher.getEmail()));
        return "teacher/teacher-notifications";
    }

    private Teacher getLoggedTeacher(HttpSession session) {
        String email = (String) session.getAttribute("loggedInUser");
        String role = (String) session.getAttribute("userRole");
        if (email == null || (role != null && !"TEACHER".equalsIgnoreCase(role))) {
            return null;
        }
        Long teacherId = firstLong(session.getAttribute("loggedTeacherId"), session.getAttribute("loggedInUserId"));
        Long adminId = firstLong(session.getAttribute("loggedTeacherAdminId"));
        if (teacherId != null) {
            Optional<Teacher> byIdAndEmail = teacherRepository.findByIdAndEmailIgnoreCase(teacherId, email);
            if (byIdAndEmail.isPresent() && sameTeacherTenant(byIdAndEmail.get(), adminId, session)) {
                return employeeIdService.ensureTeacherEmployeeId(byIdAndEmail.get());
            }
        }
        Teacher teacher = teacherRepository.findByEmail(email);
        if (teacher != null && sameTeacherTenant(teacher, adminId, session)) {
            session.setAttribute("loggedTeacherId", teacher.getId());
            session.setAttribute("loggedInUserId", teacher.getId());
            session.setAttribute("loggedTeacherAdminId", teacher.getAdmin() != null ? teacher.getAdmin().getId() : null);
            return employeeIdService.ensureTeacherEmployeeId(teacher);
        }
        return null;
    }

    private List<Student> loadVisibleStudents(Teacher teacher) {
        if (teacher == null || teacher.getAdmin() == null) {
            return List.of();
        }
        Admin admin = teacher.getAdmin();
        LinkedHashMap<Long, Student> visibleStudents = new LinkedHashMap<>();

        if (teacher.getClassRoom() != null && teacher.getClassRoom().getId() != null) {
            addStudentsToRoster(visibleStudents, studentRepository.findByAdminAndClassRoom(admin, teacher.getClassRoom()));
        }

        List<Timetable> timetableEntries = loadTeacherTimetable(teacher);
        if (!timetableEntries.isEmpty()) {
            addStudentsToRoster(
                    visibleStudents,
                    studentRepository.findByAdmin(admin).stream()
                            .filter(student -> studentMatchesTeacherTimetableScope(student, timetableEntries))
                            .toList()
            );
        }

        if (!teacherAcademicMappingService.findByTeacher(teacher).isEmpty()) {
            addStudentsToRoster(visibleStudents, teacherAcademicMappingService.studentsForTeacher(teacher));
        }

        List<ClassRoom> scopes = loadTeacherClassRooms(teacher);
        if (!scopes.isEmpty()) {
            addStudentsToRoster(
                    visibleStudents,
                    studentRepository.findByAdmin(admin).stream()
                            .filter(student -> studentMatchesTeacherScope(student, scopes))
                            .toList()
            );
        }

        return visibleStudents.values().stream()
                .sorted(Comparator.comparing(
                        (Student student) -> defaultText(student.getName(), ""),
                        String.CASE_INSENSITIVE_ORDER
                ).thenComparing(Student::getId))
                .toList();
    }

    private void addStudentsToRoster(LinkedHashMap<Long, Student> visibleStudents, List<Student> students) {
        if (visibleStudents == null || students == null) {
            return;
        }
        for (Student student : students) {
            if (student == null || student.getId() == null) {
                continue;
            }
            academicStructureService.syncStudentProgression(student);
            visibleStudents.putIfAbsent(student.getId(), student);
        }
    }

    private List<Student> loadStudentsForTimetableSlot(Teacher teacher, Timetable slot) {
        if (teacher == null || teacher.getAdmin() == null || slot == null) {
            return List.of();
        }
        return studentRepository.findByAdmin(teacher.getAdmin()).stream()
                .filter(student -> studentMatchesTeacherTimetableScope(student, List.of(slot)))
                .sorted(Comparator.comparing(
                        (Student student) -> defaultText(student.getName(), ""),
                        String.CASE_INSENSITIVE_ORDER
                ).thenComparing(Student::getId))
                .toList();
    }

    private LocalDate parseDate(String value, LocalDate fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String normalizeAttendanceStatus(String value) {
        if (value == null) {
            return "present";
        }
        String normalized = value.trim().toLowerCase(Locale.ENGLISH);
        return switch (normalized) {
            case "absent" -> "absent";
            case "late" -> "late";
            default -> "present";
        };
    }

    private String studentRollLabel(Student student) {
        if (student == null) {
            return "-";
        }
        return firstNonBlank(student.getRollNo(), student.getEnrollmentNo(), student.getRegistrationNo(), student.getId() != null ? String.valueOf(student.getId()) : null, "-");
    }

    private Long firstLong(Object... values) {
        for (Object value : values) {
            Long parsed = asLong(value);
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private boolean sameTeacherTenant(Teacher teacher, Long adminId, HttpSession session) {
        if (teacher == null || teacher.getAdmin() == null) {
            return false;
        }
        if (adminId != null && !adminId.equals(teacher.getAdmin().getId())) {
            return false;
        }
        Object portalCode = session.getAttribute("collegeCode");
        if (portalCode instanceof String code && !code.isBlank()) {
            String teacherCode = teacher.getAdmin().getCollegeCode();
            return teacherCode == null || teacherCode.isBlank() || code.equalsIgnoreCase(teacherCode);
        }
        return true;
    }

    private void addTeacherAttributes(Model model, Teacher teacher) {
        TeacherDashboardScope dashboardScope = resolveTeacherDashboardScope(teacher);
        List<Timetable> timetableEntries = loadTeacherTimetable(teacher);
        List<Subject> subjectObjects = loadTeacherSubjects(teacher, timetableEntries);
        String subjectLabel = subjectObjects.stream()
                .map(subject -> firstNonBlank(subject.getName(), subject.getCode()))
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(teacher.getSubject());
        ClassRoom classRoom = dashboardScope.primaryClassRoom();
        AcademicStructureService.TeacherScopeSummary scopeSummary = academicStructureService.syncTeacherProgression(teacher);
        model.addAttribute("teacher", teacher);
        model.addAttribute("teacherName", teacher.getName());
        model.addAttribute("teacherEmail", teacher.getEmail());
        model.addAttribute("teacherSubject", subjectLabel);
        model.addAttribute("teacherPhoto", teacher.getPhoto());
        model.addAttribute("teacherInitials", buildInitials(teacher.getName()));
        model.addAttribute("collegeName", resolveCollegeName(teacher.getAdmin()));
        model.addAttribute("collegeLogoPath", resolveCollegeLogo(teacher.getAdmin()));
        model.addAttribute("collegeInitials", buildCollegeInitials(resolveCollegeName(teacher.getAdmin())));
        model.addAttribute("dashboardGreeting", dashboardGreeting());
        model.addAttribute("dashboardAcademicYear", dashboardScope.academicYearLabel());
        model.addAttribute("teacherDesignationLabel", normalizeText(teacher.getDesignation(), "Faculty"));
        model.addAttribute("teacherDepartmentLabel", normalizeText(teacher.getDepartment(), normalizeText(teacher.getSubject(), "Academic Department")));
        model.addAttribute("teacherStatusLabel", normalizeText(teacher.getStatus(), "Active Faculty"));
        model.addAttribute("teacherExperienceLabel", normalizeText(teacher.getExperience(), "Experience not added"));
        model.addAttribute("teacherEmployeeIdLabel", normalizeText(teacher.getEmployeeId(), "ID not assigned"));
        model.addAttribute("teacherJoiningDateLabel", teacher.getJoiningDate() != null
                ? teacher.getJoiningDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                : "Not available");
        model.addAttribute("teacherAcademicYearLabel", dashboardScope.academicYearLabel());
        model.addAttribute("teacherCourseLabel", dashboardScope.courseLabel());
        model.addAttribute("teacherSectionLabel", dashboardScope.sectionLabel());
        model.addAttribute("teacherRoomLabel", dashboardScope.roomLabel());
        model.addAttribute("teacherCurrentDateLabel", LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH)));
        model.addAttribute("teacherAssignedBatchLabel", dashboardScope.batchLabel());
        model.addAttribute("teacherAssignedYearLabel", normalizeText(scopeSummary.yearLabel(), "Year not assigned"));
        model.addAttribute("teacherAssignedSemesterLabel", dashboardScope.semesterLabel());
        model.addAttribute("teacherAssignedSemesterNumber", dashboardScope.semesterNumber());
        model.addAttribute("teacherAssignedSubjects", dashboardScope.assignedSubjects());
        model.addAttribute("teacherAssignedSubjectCount", dashboardScope.assignedSubjects().size());
        model.addAttribute("teacherAssignedSubjectObjects", subjectObjects);
        model.addAttribute("teacherClassLabel", dashboardScope.classLabel());
        model.addAttribute("teacherWorkloadLabel", dashboardScope.workloadLabel());
    }

    private void addTeacherAttendanceAttributes(Model model, Teacher teacher, Map<String, String> filters) {
        List<AttendanceSlotView> slots = loadTeacherTimetable(teacher).stream()
                .filter(entry -> entry != null && !"BREAK".equalsIgnoreCase(entry.getEntryType()))
                .sorted(Comparator
                        .comparing((Timetable entry) -> WORKING_DAYS.indexOf(normalizeDay(entry.getDay())))
                        .thenComparing(entry -> parseTime(entry.getStartTime())))
                .map(entry -> new AttendanceSlotView(
                        entry.getId(),
                        normalizeDay(entry.getDay()),
                        defaultText(entry.getStartTime(), "--"),
                        defaultText(entry.getEndTime(), "--"),
                        resolveTimetableSubjectLabel(entry),
                        resolveTimetableScopeLabel(entry),
                        defaultText(entry.getRoom(), "Room")
                ))
                .toList();
        String dateFilter = filters != null ? defaultText(filters.get("historyDate"), null) : null;
        String subjectFilter = filters != null ? defaultText(filters.get("historySubject"), null) : null;
        String rollFilter = filters != null ? defaultText(filters.get("historyRollNo"), null) : null;
        LocalDate parsedDateFilter = parseDate(dateFilter, null);
        List<AttendanceHistorySessionView> historySessions = teacherAttendanceSessionRepository
                .findByTeacherOrderByAttendanceDateDescCreatedAtDesc(teacher)
                .stream()
                .filter(session -> parsedDateFilter == null || Objects.equals(session.getAttendanceDate(), parsedDateFilter))
                .filter(session -> subjectFilter == null || defaultText(session.getSubject(), "").toLowerCase(Locale.ENGLISH).contains(subjectFilter.toLowerCase(Locale.ENGLISH)))
                .map(session -> buildAttendanceHistorySession(session, rollFilter))
                .filter(view -> !view.getEntries().isEmpty())
                .limit(20)
                .toList();
        model.addAttribute("teacherAttendanceSlots", slots);
        model.addAttribute("teacherAttendanceSlotCount", slots.size());
        model.addAttribute("teacherAttendanceHistory", historySessions);
        model.addAttribute("teacherAttendanceHistoryCount", historySessions.size());
        model.addAttribute("historyDateFilter", dateFilter);
        model.addAttribute("historySubjectFilter", subjectFilter);
        model.addAttribute("historyRollNoFilter", rollFilter);
    }

    private AttendanceHistorySessionView buildAttendanceHistorySession(TeacherAttendanceSession session, String rollFilter) {
        List<AttendanceHistoryEntryView> entries = teacherAttendanceEntryRepository.findBySessionOrderByStudent_NameAsc(session).stream()
                .filter(entry -> rollFilter == null || studentRollLabel(entry.getStudent()).toLowerCase(Locale.ENGLISH).contains(rollFilter.toLowerCase(Locale.ENGLISH)))
                .map(entry -> new AttendanceHistoryEntryView(
                        entry.getId(),
                        studentRollLabel(entry.getStudent()),
                        entry.getStudent() != null ? defaultText(entry.getStudent().getName(), "Student") : "Student",
                        normalizeAttendanceStatus(entry.getStatus()),
                        entry.getMarkedAt() != null ? entry.getMarkedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH)) : "Not marked"
                ))
                .toList();
        long present = entries.stream().filter(entry -> "present".equalsIgnoreCase(entry.getStatus())).count();
        long absent = entries.stream().filter(entry -> "absent".equalsIgnoreCase(entry.getStatus())).count();
        long late = entries.stream().filter(entry -> "late".equalsIgnoreCase(entry.getStatus())).count();
        return new AttendanceHistorySessionView(
                session.getId(),
                session.getAttendanceDate() != null ? session.getAttendanceDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)) : "Date not set",
                defaultText(session.getSubject(), "Subject"),
                defaultText(session.getLectureNo(), "Slot"),
                session.getClassRoom() != null ? session.getClassRoom().getDisplayLabel() : "Class",
                entries,
                present,
                absent,
                late
        );
    }

    private void addStudyMaterialAttributes(Model model, Teacher teacher) {
        List<StudyMaterial> materials = loadTeacherMaterials(teacher);
        List<Timetable> timetableEntries = loadTeacherTimetable(teacher);
        List<Subject> subjectObjects = loadTeacherSubjects(teacher, timetableEntries);
        model.addAttribute("teacherStudyMaterials", materials);
        model.addAttribute("teacherStudyMaterialsBySubject", materials.stream()
                .collect(Collectors.groupingBy(
                        material -> material.getSubject() != null && !material.getSubject().isBlank()
                                ? material.getSubject()
                                : "Unassigned",
                        java.util.LinkedHashMap::new,
                        Collectors.toList()
                )));
        model.addAttribute("teacherStudyMaterialCount", materials.size());
        model.addAttribute("teacherStudyMaterialSubjectCount", materials.stream()
                .map(material -> material.getSubject() != null && !material.getSubject().isBlank()
                        ? material.getSubject()
                        : "Unassigned")
                .distinct()
                .count());
        model.addAttribute("teacherStudyMaterialStudents", materials.stream()
                .map(this::resolveMaterialScopeLabel)
                .distinct()
                .count());
        String defaultMappedSubject = subjectObjects.stream()
                .map(subject -> firstNonBlank(subject.getName(), subject.getCode()))
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
        model.addAttribute("teacherDefaultSubject", defaultMappedSubject != null
                ? defaultMappedSubject
                : teacher.getSubject() != null && !teacher.getSubject().isBlank()
                ? teacher.getSubject()
                : "General");
        model.addAttribute("teacherDefaultSemester", teacher.getClassRoom() != null && teacher.getClassRoom().getSemester() != null
                ? String.valueOf(teacher.getClassRoom().getSemester())
                : "");
        model.addAttribute("teacherClassLabel", resolveTeacherDashboardScope(teacher).classLabel());
    }

    private void addTeacherAssignmentAttributes(Model model, Teacher teacher) {
        List<Assignment> assignments = loadTeacherAssignments(teacher);
        LinkedHashMap<Long, List<AssignmentSubmission>> submissionsByAssignment = new LinkedHashMap<>();
        for (Assignment assignment : assignments) {
            submissionsByAssignment.put(
                    assignment.getId(),
                    assignmentSubmissionRepository.findByAssignmentOrderBySubmittedAtDesc(assignment)
            );
        }
        long activeAssignments = assignments.stream()
                .filter(assignment -> assignment.isPublished()
                        && (assignment.getDueDate() == null || !assignment.getDueDate().isBefore(LocalDate.now())))
                .count();
        long gradedAssignments = submissionsByAssignment.values().stream()
                .flatMap(List::stream)
                .filter(submission -> "GRADED".equalsIgnoreCase(defaultText(submission.getStatus(), "")))
                .count();
        long totalSubmissions = submissionsByAssignment.values().stream().mapToLong(List::size).sum();
        long maxPossibleSubmissions = (long) loadVisibleStudents(teacher).size() * Math.max(assignments.size(), 1);
        long submissionRate = maxPossibleSubmissions > 0 ? Math.round((double) totalSubmissions * 100 / maxPossibleSubmissions) : 0;

        model.addAttribute("teacherAssignments", assignments);
        model.addAttribute("teacherAssignmentSubmissions", submissionsByAssignment);
        model.addAttribute("teacherAssignmentCount", assignments.size());
        model.addAttribute("teacherActiveAssignmentCount", activeAssignments);
        model.addAttribute("teacherGradedAssignmentCount", gradedAssignments);
        model.addAttribute("teacherAssignmentSubmissionRate", submissionRate);
        model.addAttribute("teacherAssignmentSubjects", loadTeacherSubjects(teacher, loadTeacherTimetable(teacher)));
    }

    private List<StudyMaterial> loadTeacherMaterials(Teacher teacher) {
        Admin admin = teacher != null ? teacher.getAdmin() : null;
        List<StudyMaterial> materials = admin != null
                ? studyMaterialRepository.findByAdminOrderByUploadedAtDesc(admin)
                : studyMaterialRepository.findAllByOrderByUploadedAtDesc();

        Long teacherId = teacher.getId();

        return materials.stream()
                .filter(material -> (material.getTeacher() != null && teacherId != null && teacherId.equals(material.getTeacher().getId()))
                        || materialMatchesTeacherScope(material, teacher))
                .toList();
    }

    private List<Assignment> loadTeacherAssignments(Teacher teacher) {
        if (teacher == null) {
            return List.of();
        }
        LinkedHashMap<Long, Assignment> assignments = new LinkedHashMap<>();
        for (Assignment assignment : assignmentRepository.findByTeacherOrderByCreatedAtDesc(teacher)) {
            if (assignment != null && assignment.getId() != null) {
                assignments.putIfAbsent(assignment.getId(), assignment);
            }
        }
        ClassRoom classRoom = resolveTeacherAssignmentClassRoom(teacher);
        if (classRoom != null) {
            for (Assignment assignment : assignmentRepository.findByClassRoomOrderByDueDateAscCreatedAtDesc(classRoom)) {
                if (assignment != null && assignment.getTeacher() != null && Objects.equals(assignment.getTeacher().getId(), teacher.getId())) {
                    assignments.putIfAbsent(assignment.getId(), assignment);
                }
            }
        }
        return assignments.values().stream()
                .sorted(Comparator.comparing(Assignment::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
    }

    private void addTeacherDashboardAttributes(Model model, Teacher teacher, List<Student> students) {
        ClassRoom classRoom = teacher.getClassRoom();

        double classTotalFees = students.stream()
                .map(Student::getTotalFees)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();
        double classPaidFees = students.stream()
                .map(Student::getPaidFees)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();
        double classPendingFees = students.stream()
                .map(Student::getPendingFees)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();

        model.addAttribute("teacherStudentWithPendingFees", students.stream()
                .filter(student -> student.getPendingFees() != null && student.getPendingFees() > 0)
                .count());
        model.addAttribute("teacherStudentWithPhotos", students.stream()
                .filter(student -> student.getPhoto() != null && !student.getPhoto().isBlank())
                .count());
        model.addAttribute("teacherClassTotalFeesFormatted", formatCurrency(classTotalFees));
        model.addAttribute("teacherClassPaidFeesFormatted", formatCurrency(classPaidFees));
        model.addAttribute("teacherClassPendingFeesFormatted", formatCurrency(classPendingFees));
        model.addAttribute("teacherClassFeeProgress", classTotalFees > 0
                ? Math.round((classPaidFees / classTotalFees) * 100)
                : 0L);
    }

    private void addTeacherExamAttributes(Model model, Teacher teacher) {
        List<ExamSession> exams = examAutomationService.findTeacherExams(teacher);
        List<ExamSession> upcoming = exams.stream()
                .filter(exam -> exam.getExamDateTime() != null && exam.getExamDateTime().isAfter(LocalDateTime.now()))
                .limit(3)
                .toList();
        model.addAttribute("teacherUpcomingExams", upcoming);
        model.addAttribute("teacherExamCount", exams.size());
        model.addAttribute("teacherUnreadNotifications", examAutomationService.countUnreadNotifications("TEACHER", teacher.getEmail()));
        model.addAttribute("teacherExamRiskViews", examAutomationService.buildRiskViews(teacher).stream().limit(5).toList());
    }

    private void addTeacherTimetableAttributes(Model model, Teacher teacher) {
        List<Timetable> entries = loadTeacherTimetable(teacher);
        LinkedHashMap<String, List<TimetableSlotView>> weeklySchedule = buildWeeklySchedule(entries);
        String todayName = currentDayName();
        List<TimetableSlotView> todaySchedule = new ArrayList<>(weeklySchedule.getOrDefault(todayName, List.of()));
        DayScheduleInsight insight = buildDayScheduleInsight(todaySchedule);

        model.addAttribute("teacherWeeklySchedule", weeklySchedule);
        model.addAttribute("teacherWeeklyTimetableRows", buildWeeklyTimetableRows(weeklySchedule));
        model.addAttribute("teacherTodaySchedule", todaySchedule);
        model.addAttribute("teacherWeeklyEntries", entries);
        model.addAttribute("workingDayNames", WORKING_DAYS);
        model.addAttribute("teacherTodayLabel", todayName);
        model.addAttribute("teacherTodayFullDate", LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy", Locale.ENGLISH)));
        model.addAttribute("teacherTodayCurrentSlot", insight.currentSlot());
        model.addAttribute("teacherTodayNextSlot", insight.nextSlot());
        model.addAttribute("teacherTodayCompletedCount", insight.completedCount());
        model.addAttribute("teacherTodayUpcomingCount", insight.upcomingCount());
        model.addAttribute("teacherWeeklyCount", entries.size());
        model.addAttribute("teacherTodayCount", todaySchedule.size());
    }

    private List<Timetable> loadTeacherTimetable(Teacher teacher) {
        if (teacher == null || teacher.getAdmin() == null) return List.of();
        LinkedHashMap<Long, Timetable> scopedEntries = new LinkedHashMap<>();
        for (Timetable entry : timetableRepository.findByTeacher(teacher)) {
            if (entry != null && entry.getId() != null && timetableAssignedToTeacher(entry, teacher)) {
                scopedEntries.put(entry.getId(), entry);
            }
        }
        for (Timetable entry : timetableRepository.findByAdmin(teacher.getAdmin())) {
            if (entry == null || entry.getId() == null) {
                continue;
            }
            if (timetableAssignedToTeacher(entry, teacher)) {
                scopedEntries.put(entry.getId(), entry);
            }
        }
        List<Timetable> entries = new ArrayList<>(scopedEntries.values());
        return entries.stream()
                .filter(t -> t.getDay() != null && t.getStartTime() != null && t.getEndTime() != null)
                .sorted(Comparator
                        .comparingInt((Timetable t) -> dayOrderIndex(t.getDay()))
                        .thenComparing(t -> parseTime(t.getStartTime())))
                .toList();
    }

    private boolean timetableAssignedToTeacher(Timetable entry, Teacher teacher) {
        return entry != null
                && teacher != null
                && teacher.getId() != null
                && entry.getTeacher() != null
                && teacher.getId().equals(entry.getTeacher().getId())
                && (teacher.getAdmin() == null
                    || entry.getAdmin() == null
                    || Objects.equals(teacher.getAdmin().getId(), entry.getAdmin().getId()));
    }

    private boolean studentMatchesTeacherTimetableScope(Student student, List<Timetable> timetableEntries) {
        if (student == null || timetableEntries == null || timetableEntries.isEmpty()) {
            return false;
        }
        return timetableEntries.stream().anyMatch(entry -> {
            if (entry == null || entry.getTeacher() == null) {
                return false;
            }
            ClassRoom classRoom = entry.getClassRoom();
            if (classRoom != null && classRoomMatchesExactStudentTimetableScope(classRoom, student, entry)) {
                return true;
            }
            if (student.getBatch() != null && entry != null && entry.getBatch() != null
                    && !Objects.equals(student.getBatch().getId(), entry.getBatch().getId())) {
                return false;
            }
            if (defaultText(student.getCourse(), null) != null && entry.getCourse() != null) {
                String course = firstNonBlank(entry.getCourse().getCode(), entry.getCourse().getName());
                if (course != null && !student.getCourse().equalsIgnoreCase(course)) {
                    return false;
                }
            }
            if (defaultText(student.getAcademicYear(), null) != null
                    && entry != null
                    && defaultText(entry.getAcademicYear(), null) != null
                    && !academicYearMatches(student.getAcademicYear(), entry.getAcademicYear())) {
                return false;
            }
            Integer semesterNumber = parseSemesterNumber(student.getSemester());
            if (semesterNumber != null && entry != null && entry.getSemesterNumber() != null
                    && !semesterNumber.equals(entry.getSemesterNumber())) {
                return false;
            }
            if (defaultText(student.getSectionName(), null) != null
                    && entry != null
                    && defaultText(entry.getSection(), null) != null
                    && !student.getSectionName().equalsIgnoreCase(entry.getSection())) {
                return false;
            }
            return true;
        });
    }

    private List<Subject> loadTeacherSubjects(Teacher teacher, List<Timetable> timetableEntries) {
        LinkedHashMap<Long, Subject> subjects = new LinkedHashMap<>();
        if (timetableEntries != null) {
            for (Timetable entry : timetableEntries) {
                Subject subject = entry != null ? entry.getSubjectRef() : null;
                if (subject != null && subject.getId() != null) {
                    subjects.putIfAbsent(subject.getId(), subject);
                }
            }
        }
        if (subjects.isEmpty()) {
            for (Subject subject : teacherAcademicMappingService.subjectsForTeacher(teacher)) {
                if (subject != null && subject.getId() != null) {
                    subjects.putIfAbsent(subject.getId(), subject);
                }
            }
        }
        return new ArrayList<>(subjects.values());
    }

    private List<ClassRoom> loadTeacherClassRooms(Teacher teacher) {
        LinkedHashMap<Long, ClassRoom> classRooms = new LinkedHashMap<>();
        if (teacher == null) {
            return List.of();
        }
        putClassRoom(classRooms, teacher.getClassRoom());
        for (Timetable entry : loadTeacherTimetable(teacher)) {
            putClassRoom(classRooms, entry.getClassRoom());
        }
        return new ArrayList<>(classRooms.values());
    }

    private void putClassRoom(LinkedHashMap<Long, ClassRoom> classRooms, ClassRoom classRoom) {
        if (classRooms == null || classRoom == null || classRoom.getId() == null) {
            return;
        }
        classRooms.putIfAbsent(classRoom.getId(), classRoom);
    }

    private ClassRoom resolveTeacherAssignmentClassRoom(Teacher teacher) {
        if (teacher == null) {
            return null;
        }
        if (teacher.getClassRoom() != null && teacher.getClassRoom().getId() != null) {
            return teacher.getClassRoom();
        }
        List<ClassRoom> classRooms = loadTeacherClassRooms(teacher);
        if (!classRooms.isEmpty()) {
            return classRooms.get(0);
        }
        return null;
    }

    private TeacherDashboardScope resolveTeacherDashboardScope(Teacher teacher) {
        List<Timetable> timetableEntries = loadTeacherTimetable(teacher);
        List<ClassRoom> classRooms = loadTeacherClassRooms(teacher);
        ClassRoom primary = teacher != null && teacher.getClassRoom() != null
                ? teacher.getClassRoom()
                : classRooms.stream().findFirst().orElse(null);

        TeacherAcademicMappingService.TeacherScopeView mappedScope = teacherAcademicMappingService.primaryScope(teacher);
        boolean hasTimetableScope = !timetableEntries.isEmpty();

        List<String> assignedSubjects = collectAssignedSubjects(teacher, timetableEntries);
        if (assignedSubjects.isEmpty()) {
            assignedSubjects = mappedScope.subjects().stream()
                    .map(subject -> firstNonBlank(subject.getName(), subject.getCode()))
                    .filter(value -> value != null && !value.isBlank())
                    .toList();
        }
        Integer semesterNumber = primary != null && primary.getSemester() != null
                ? primary.getSemester()
                : mappedScope.semester() != null
                ? mappedScope.semester()
                : firstSemesterFromSubjects(teacher);
        String academicYear = firstNonBlank(
                firstTimetableValue(timetableEntries, entry -> entry != null ? entry.getAcademicYear() : null),
                primary != null ? primary.getAcademicYear() : null,
                teacher != null ? teacher.getAcademicYear() : null,
                mappedScope.academicYear(),
                timetableEntries.stream().map(Timetable::getAcademicYear).filter(value -> value != null && !value.isBlank()).findFirst().orElse(null),
                academicStructureService.currentAcademicYear()
        );
        String course = firstNonBlank(
                firstTimetableValue(timetableEntries, entry -> entry != null && entry.getCourse() != null
                        ? firstNonBlank(entry.getCourse().getName(), entry.getCourse().getCode())
                        : null),
                primary != null ? primary.getCourse() : null,
                primary != null ? primary.getCourseCode() : null,
                mappedScope.course() != null ? mappedScope.course().getName() : null,
                mappedScope.course() != null ? mappedScope.course().getCode() : null,
                firstCourseFromSubjects(teacher)
        );
        String batch = firstNonBlank(
                firstTimetableValue(timetableEntries, entry -> entry != null && entry.getBatch() != null
                        ? entry.getBatch().getDisplayName()
                        : null),
                primary != null ? primary.getBatchName() : null,
                primary != null ? primary.getBatch() : null,
                mappedScope.batch() != null ? mappedScope.batch().getDisplayName() : null,
                firstBatchFromSubjects(teacher)
        );
        String section = summarizeTimetableValue(
                timetableEntries,
                entry -> entry != null ? entry.getSection() : null,
                primary != null ? normalizeText(primary.getSection(), "No section") : "No section"
        );
        if ("Multiple sections".equals(section) && mappedScope.section() != null && !mappedScope.section().isBlank()) {
            section = "Multiple sections";
        } else if ((section == null || section.isBlank() || "No section".equals(section)) && mappedScope.section() != null && !mappedScope.section().isBlank()) {
            section = mappedScope.section();
        }
        String room = firstNonBlank(
                firstTimetableValue(timetableEntries, entry -> entry != null ? entry.getRoom() : null),
                primary != null ? primary.getRoom() : null,
                mappedScope.roomNumber(),
                "Room not assigned"
        );
        String classLabel = hasTimetableScope && primary != null && defaultText(primary.getDisplayLabel(), null) != null
                ? primary.getDisplayLabel()
                : String.join(" | ", List.of(
                        normalizeText(course, "Course not assigned"),
                        normalizeText(batch, "Batch not assigned"),
                        semesterNumber != null ? "Sem " + semesterNumber : "Semester not assigned",
                        section
                ));
        return new TeacherDashboardScope(
                primary,
                normalizeText(academicYear, "Current academic year"),
                normalizeText(course, "Course not assigned"),
                normalizeText(batch, "Batch not assigned"),
                section,
                room,
                semesterNumber,
                semesterNumber != null ? "SEM " + semesterNumber : "Semester not assigned",
                classLabel,
                assignedSubjects,
                timetableEntries.size() + " weekly slot(s)"
        );
    }

    private List<String> collectAssignedSubjects(Teacher teacher, List<Timetable> timetableEntries) {
        LinkedHashSet<String> subjects = new LinkedHashSet<>();
        if (teacher == null) {
            return List.of();
        }
        if (timetableEntries != null) {
            timetableEntries.stream()
                    .map(entry -> entry != null ? firstNonBlank(
                            entry.getSubjectRef() != null ? entry.getSubjectRef().getName() : null,
                            entry.getSubjectRef() != null ? entry.getSubjectRef().getCode() : null,
                            entry.getSubject()
                    ) : null)
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .forEach(subjects::add);
        }
        if (teacher.getSubject() != null && !teacher.getSubject().isBlank()) {
            subjects.add(teacher.getSubject().trim());
        }
        return new ArrayList<>(subjects);
    }

    private String firstTimetableValue(List<Timetable> entries, java.util.function.Function<Timetable, String> extractor) {
        if (entries == null || extractor == null) {
            return null;
        }
        for (Timetable entry : entries) {
            String value = extractor.apply(entry);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String summarizeTimetableValue(List<Timetable> entries, java.util.function.Function<Timetable, String> extractor, String fallback) {
        if (entries == null || extractor == null) {
            return fallback;
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (Timetable entry : entries) {
            String value = extractor.apply(entry);
            if (value != null && !value.isBlank()) {
                values.add(value.trim());
            }
        }
        if (values.isEmpty()) {
            return fallback;
        }
        if (values.size() == 1) {
            return values.iterator().next();
        }
        return "Multiple sections";
    }

    private LinkedHashMap<String, List<TimetableSlotView>> buildWeeklySchedule(List<Timetable> entries) {
        LinkedHashMap<String, List<TimetableSlotView>> weekly = new LinkedHashMap<>();
        for (String day : WORKING_DAYS) {
            weekly.put(day, new ArrayList<>());
        }
        for (Timetable entry : entries) {
            String day = normalizeDay(entry.getDay());
            if (!WORKING_DAYS.contains(day)) continue;
            String subjectLabel = resolveTimetableSubjectLabel(entry);
            weekly.get(day).add(new TimetableSlotView(
                    day,
                    defaultText(entry.getStartTime(), "--"),
                    defaultText(entry.getEndTime(), "--"),
                    subjectLabel,
                    resolveTimetableScopeLabel(entry),
                    defaultText(entry.getRoom(), "Room"),
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
        return rows.values().stream()
                .sorted(Comparator.comparing(row -> parseTime(row.startTime())))
                .map(WeeklyTimetableRowBuilder::build)
                .toList();
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
            if (current == null && !now.isBefore(start) && now.isBefore(end)) {
                current = slot;
                continue;
            }
            if (now.compareTo(end) >= 0) {
                completed++;
                continue;
            }
            if (now.isBefore(start)) {
                upcoming++;
                if (next == null) next = slot;
            }
        }
        if (current != null) {
            upcoming = Math.max(upcoming - 1, 0);
        }
        return new DayScheduleInsight(completed, upcoming, current, next);
    }

    private String currentDayName() {
        return LocalDate.now().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    private int dayOrderIndex(String day) {
        return WORKING_DAYS.indexOf(normalizeDay(day));
    }

    private String normalizeDay(String day) {
        if (day == null || day.isBlank()) return "Monday";
        return switch (day.trim().toLowerCase(Locale.ENGLISH)) {
            case "mon", "monday" -> "Monday";
            case "tue", "tues", "tuesday" -> "Tuesday";
            case "wed", "wednesday" -> "Wednesday";
            case "thu", "thur", "thurs", "thursday" -> "Thursday";
            case "fri", "friday" -> "Friday";
            case "sat", "saturday" -> "Saturday";
            default -> "Monday";
        };
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) return LocalTime.MAX;
        try {
            return LocalTime.parse(value.trim());
        } catch (Exception ignored) {
            return LocalTime.MAX;
        }
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private Integer parseSemesterNumber(String value) {
        String normalized = defaultText(value, "").replaceAll("[^0-9]", "");
        if (normalized.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(normalized);
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

    private boolean classRoomMatchesExactStudentTimetableScope(ClassRoom classRoom, Student student, Timetable entry) {
        return classRoom != null && student != null && studentMatchesClassRoomScope(student, classRoom);
    }

    private boolean studentMatchesTeacherScope(Student student, List<ClassRoom> scopes) {
        if (student == null || scopes == null || scopes.isEmpty()) {
            return false;
        }
        return scopes.stream().anyMatch(scope -> studentMatchesClassRoomScope(student, scope));
    }

    private boolean studentMatchesClassRoomScope(Student student, ClassRoom scope) {
        if (student == null || scope == null) {
            return false;
        }
        if (student.getClassRoom() != null && student.getClassRoom().getId() != null && scope.getId() != null
                && student.getClassRoom().getId().equals(scope.getId())) {
            return true;
        }
        if (student.getBatch() != null && !batchMatchesClassRoom(scope, student.getBatch())) {
            return false;
        }
        if (defaultText(student.getCourse(), null) != null && !courseMatchesClassRoom(scope, student.getCourse())) {
            return false;
        }
        Integer semesterNumber = parseSemesterNumber(student.getSemester());
        if (semesterNumber != null && scope.getSemester() != null && !semesterNumber.equals(scope.getSemester())) {
            return false;
        }
        if (defaultText(student.getAcademicYear(), null) != null
                && defaultText(scope.getAcademicYear(), null) != null
                && !academicYearMatches(student.getAcademicYear(), scope.getAcademicYear())) {
            return false;
        }
        if (defaultText(student.getSectionName(), null) != null
                && defaultText(scope.getSection(), null) != null
                && !student.getSectionName().equalsIgnoreCase(scope.getSection())) {
            return false;
        }
        return true;
    }

    private boolean materialMatchesTeacherScope(StudyMaterial material, Teacher teacher) {
        if (material == null || teacher == null) {
            return false;
        }
        return loadTeacherClassRooms(teacher).stream().anyMatch(scope -> materialMatchesClassRoomScope(material, scope));
    }

    private boolean materialMatchesClassRoomScope(StudyMaterial material, ClassRoom scope) {
        if (material == null || scope == null) {
            return false;
        }
        if (material.getClassRoom() != null) {
            if (!courseMatchesClassRoom(scope, defaultText(material.getClassRoom().getCourseCode(), material.getClassRoom().getCourse()))) {
                return false;
            }
            if (scope.getSemester() != null && material.getClassRoom().getSemester() != null && !scope.getSemester().equals(material.getClassRoom().getSemester())) {
                return false;
            }
            if (defaultText(scope.getAcademicYear(), null) != null
                    && defaultText(material.getClassRoom().getAcademicYear(), null) != null
                    && !academicYearMatches(scope.getAcademicYear(), material.getClassRoom().getAcademicYear())) {
                return false;
            }
            if (defaultText(scope.getSection(), null) != null
                    && defaultText(material.getClassRoom().getSection(), null) != null
                    && !scope.getSection().equalsIgnoreCase(material.getClassRoom().getSection())) {
                return false;
            }
            return true;
        }
        if (defaultText(material.getCourse(), null) != null && !courseMatchesClassRoom(scope, material.getCourse())) {
            return false;
        }
        Integer semesterNumber = parseSemesterNumber(material.getSemester());
        if (semesterNumber != null && scope.getSemester() != null && !semesterNumber.equals(scope.getSemester())) {
            return false;
        }
        if (defaultText(material.getAcademicYear(), null) != null
                && defaultText(scope.getAcademicYear(), null) != null
                && !academicYearMatches(material.getAcademicYear(), scope.getAcademicYear())) {
            return false;
        }
        if (defaultText(material.getSection(), null) != null
                && defaultText(scope.getSection(), null) != null
                && !material.getSection().equalsIgnoreCase(scope.getSection())) {
            return false;
        }
        return true;
    }

    private String saveAssignmentFile(MultipartFile file, String uploadDir) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "assignment";
        String ext = org.springframework.util.StringUtils.getFilenameExtension(original);
        String normalizedExt = ext != null ? ext.toLowerCase(Locale.ENGLISH) : "";
        Set<String> allowed = Set.of("pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "zip", "png", "jpg", "jpeg");
        if (!allowed.contains(normalizedExt) || file.getSize() > 25L * 1024 * 1024) {
            return null;
        }
        try {
            File dir = new File(uploadDir);
            if (!dir.exists() && !dir.mkdirs()) {
                return null;
            }
            String storedName = UUID.randomUUID() + "." + normalizedExt;
            Files.copy(file.getInputStream(), Paths.get(uploadDir + storedName), StandardCopyOption.REPLACE_EXISTING);
            return uploadDir.contains("assignment-submissions")
                    ? "/uploads/assignment-submissions/" + storedName
                    : "/uploads/assignments/" + storedName;
        } catch (IOException ex) {
            return null;
        }
    }

    private ResponseEntity<Resource> serveAssignmentFile(String filePath, String fileName, String contentType, boolean download) {
        try {
            Path resolved = resolveUploadPath(filePath);
            if (resolved == null || !Files.exists(resolved)) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new UrlResource(resolved.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            String safeContentType = defaultText(contentType, Files.probeContentType(resolved));
            if (safeContentType == null) {
                safeContentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
            String dispositionType = download ? "attachment" : "inline";
            String safeName = defaultText(fileName, resolved.getFileName().toString());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(safeContentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.builder(dispositionType).filename(safeName).build().toString())
                    .body(resource);
        } catch (MalformedURLException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private Path resolveUploadPath(String storedPath) {
        String normalizedPath = defaultText(storedPath, null);
        if (normalizedPath == null || !normalizedPath.startsWith("/uploads/")) {
            return null;
        }
        String relative = normalizedPath.substring("/uploads/".length()).replace('/', File.separatorChar);
        return Path.of(System.getProperty("user.dir"), "src", "main", "resources", "static", "uploads", relative);
    }

    private boolean timetableMatchesTeacherScope(Timetable entry, Teacher teacher) {
        if (timetableAssignedToTeacher(entry, teacher)) {
            return true;
        }
        if (entry == null || teacher == null || teacher.getClassRoom() == null) {
            return false;
        }
        ClassRoom scope = teacher.getClassRoom();
        if (entry.getClassRoom() != null) {
            return classRoomsMatchExactAcademicScope(scope, entry.getClassRoom());
        }
        return false;
    }

    private boolean classRoomsMatchExactAcademicScope(ClassRoom scope, ClassRoom entryScope) {
        if (scope == null || entryScope == null) {
            return false;
        }
        boolean sameBatch = defaultText(scope.getBatchName(), null) != null
                && defaultText(entryScope.getBatchName(), null) != null
                && scope.getBatchName().equalsIgnoreCase(entryScope.getBatchName());
        if (!sameBatch && scope.getBatchStartYear() != null && scope.getBatchEndYear() != null
                && entryScope.getBatchStartYear() != null && entryScope.getBatchEndYear() != null) {
            sameBatch = scope.getBatchStartYear().equals(entryScope.getBatchStartYear())
                    && scope.getBatchEndYear().equals(entryScope.getBatchEndYear());
        }
        if (!sameBatch) {
            return false;
        }
        if (!courseMatchesClassRoom(entryScope, defaultText(scope.getCourseCode(), scope.getCourse()))) {
            return false;
        }
        if (scope.getSemester() == null || entryScope.getSemester() == null || !scope.getSemester().equals(entryScope.getSemester())) {
            return false;
        }
        if (defaultText(scope.getAcademicYear(), null) == null
                || defaultText(entryScope.getAcademicYear(), null) == null
                || !academicYearMatches(scope.getAcademicYear(), entryScope.getAcademicYear())) {
            return false;
        }
        return defaultText(scope.getSection(), null) != null
                && defaultText(entryScope.getSection(), null) != null
                && scope.getSection().equalsIgnoreCase(entryScope.getSection());
    }

    private String resolveMaterialScopeLabel(StudyMaterial material) {
        if (material == null) {
            return "Unassigned academic scope";
        }
        if (material.getClassRoom() != null && defaultText(material.getClassRoom().getDisplayLabel(), null) != null) {
            return material.getClassRoom().getDisplayLabel();
        }
        List<String> parts = new ArrayList<>();
        if (defaultText(material.getCourse(), null) != null) parts.add(material.getCourse());
        Integer semesterNumber = parseSemesterNumber(material.getSemester());
        if (semesterNumber != null) parts.add("Sem " + semesterNumber);
        if (defaultText(material.getSection(), null) != null) parts.add("Section " + material.getSection());
        return parts.isEmpty() ? "Unassigned academic scope" : String.join(" - ", parts);
    }

    private String resolveTimetableScopeLabel(Timetable entry) {
        if (entry == null) {
            return "Academic Scope";
        }
        if (entry.getClassRoom() != null && defaultText(entry.getClassRoom().getDisplayLabel(), null) != null) {
            return entry.getClassRoom().getDisplayLabel();
        }
        return defaultText(entry.getAcademicYear(), "Academic Scope");
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

    private String firstCourseFromSubjects(Teacher teacher) {
        if (teacher == null || teacher.getAdmin() == null) {
            return null;
        }
        return loadTeacherTimetable(teacher)
                .stream()
                .map(Timetable::getClassRoom)
                .filter(Objects::nonNull)
                .map(classRoom -> firstNonBlank(classRoom.getCourse(), classRoom.getCourseCode()))
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    private String firstBatchFromSubjects(Teacher teacher) {
        if (teacher == null || teacher.getAdmin() == null) {
            return null;
        }
        return loadTeacherTimetable(teacher)
                .stream()
                .map(Timetable::getClassRoom)
                .filter(Objects::nonNull)
                .map(classRoom -> firstNonBlank(classRoom.getBatchName(), classRoom.getBatch()))
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    private Integer firstSemesterFromSubjects(Teacher teacher) {
        if (teacher == null || teacher.getAdmin() == null) {
            return null;
        }
        return loadTeacherTimetable(teacher)
                .stream()
                .map(Timetable::getClassRoom)
                .filter(Objects::nonNull)
                .map(ClassRoom::getSemester)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String buildSubjectClass(String subject) {
        if (subject == null) return "general";
        String normalized = subject.toLowerCase(Locale.ENGLISH);
        if (normalized.contains("lab")) return "lab";
        if (normalized.contains("project")) return "lab";
        if (normalized.contains("network")) return "cn";
        if (normalized.contains("database")) return "db";
        if (normalized.contains("java")) return "java";
        if (normalized.contains("ai") || normalized.contains("machine")) return "ai";
        if (normalized.contains("cloud")) return "cn";
        return "se";
    }

    private String resolveTimetableSubjectLabel(Timetable entry) {
        if (entry == null) {
            return "Subject";
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
        return "Subject";
    }

    private record TeacherDashboardScope(
            ClassRoom primaryClassRoom,
            String academicYearLabel,
            String courseLabel,
            String batchLabel,
            String sectionLabel,
            String roomLabel,
            Integer semesterNumber,
            String semesterLabel,
            String classLabel,
            List<String> assignedSubjects,
            String workloadLabel
    ) {}

    private static final class TimetableSlotView {
        private final String dayName;
        private final String startTime;
        private final String endTime;
        private final String subject;
        private final String classLabel;
        private final String room;
        private final String subjectClass;

        private TimetableSlotView(String dayName, String startTime, String endTime, String subject, String classLabel, String room, String subjectClass) {
            this.dayName = dayName;
            this.startTime = startTime;
            this.endTime = endTime;
            this.subject = subject;
            this.classLabel = classLabel;
            this.room = room;
            this.subjectClass = subjectClass;
        }

        public String getDayName() { return dayName; }
        public String getStartTime() { return startTime; }
        public String getEndTime() { return endTime; }
        public String getSubject() { return subject; }
        public String getClassLabel() { return classLabel; }
        public String getRoom() { return room; }
        public String getSubjectClass() { return subjectClass; }
    }

    public static final class AttendanceSlotView {
        private final Long id;
        private final String dayName;
        private final String startTime;
        private final String endTime;
        private final String subject;
        private final String classLabel;
        private final String room;

        public AttendanceSlotView(Long id, String dayName, String startTime, String endTime, String subject, String classLabel, String room) {
            this.id = id;
            this.dayName = dayName;
            this.startTime = startTime;
            this.endTime = endTime;
            this.subject = subject;
            this.classLabel = classLabel;
            this.room = room;
        }

        public Long getId() { return id; }
        public String getDayName() { return dayName; }
        public String getStartTime() { return startTime; }
        public String getEndTime() { return endTime; }
        public String getSubject() { return subject; }
        public String getClassLabel() { return classLabel; }
        public String getRoom() { return room; }
        public String getDisplayLabel() {
            return dayName + " " + startTime + "-" + endTime + " | " + subject + " | " + classLabel;
        }
    }

    public static final class AttendanceHistorySessionView {
        private final Long id;
        private final String dateLabel;
        private final String subject;
        private final String lectureNo;
        private final String classLabel;
        private final List<AttendanceHistoryEntryView> entries;
        private final long presentCount;
        private final long absentCount;
        private final long lateCount;

        public AttendanceHistorySessionView(Long id, String dateLabel, String subject, String lectureNo, String classLabel,
                                            List<AttendanceHistoryEntryView> entries, long presentCount, long absentCount, long lateCount) {
            this.id = id;
            this.dateLabel = dateLabel;
            this.subject = subject;
            this.lectureNo = lectureNo;
            this.classLabel = classLabel;
            this.entries = entries;
            this.presentCount = presentCount;
            this.absentCount = absentCount;
            this.lateCount = lateCount;
        }

        public Long getId() { return id; }
        public String getDateLabel() { return dateLabel; }
        public String getSubject() { return subject; }
        public String getLectureNo() { return lectureNo; }
        public String getClassLabel() { return classLabel; }
        public List<AttendanceHistoryEntryView> getEntries() { return entries; }
        public long getPresentCount() { return presentCount; }
        public long getAbsentCount() { return absentCount; }
        public long getLateCount() { return lateCount; }
        public int getTotalCount() { return entries != null ? entries.size() : 0; }
    }

    public static final class AttendanceHistoryEntryView {
        private final Long id;
        private final String rollNo;
        private final String studentName;
        private final String status;
        private final String markedAtLabel;

        public AttendanceHistoryEntryView(Long id, String rollNo, String studentName, String status, String markedAtLabel) {
            this.id = id;
            this.rollNo = rollNo;
            this.studentName = studentName;
            this.status = status;
            this.markedAtLabel = markedAtLabel;
        }

        public Long getId() { return id; }
        public String getRollNo() { return rollNo; }
        public String getStudentName() { return studentName; }
        public String getStatus() { return status; }
        public String getMarkedAtLabel() { return markedAtLabel; }
    }

    private static final class WeeklyTimetableRowView {
        private final String startTime;
        private final String endTime;
        private final boolean breakRow;
        private final LinkedHashMap<String, TimetableSlotView> slotsByDay = new LinkedHashMap<>();

        private WeeklyTimetableRowView(String startTime, String endTime) {
            this(startTime, endTime, false);
        }

        private WeeklyTimetableRowView(String startTime, String endTime, boolean breakRow) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.breakRow = breakRow;
        }

        private void put(String day, TimetableSlotView slot) {
            slotsByDay.put(day, slot);
        }

        public String getStartTime() { return startTime; }
        public String getEndTime() { return endTime; }
        public boolean isBreakRow() { return breakRow; }
        public TimetableSlotView getSlotForDay(String day) { return slotsByDay.get(day); }
    }

    private static final class WeeklyTimetableRowBuilder {
        private final String startTime;
        private final String endTime;
        private final LinkedHashMap<String, TimetableSlotView> slotsByDay = new LinkedHashMap<>();

        private WeeklyTimetableRowBuilder(String startTime, String endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        private void put(String day, TimetableSlotView slot) {
            slotsByDay.put(day, slot);
        }

        private String startTime() { return startTime; }

        private WeeklyTimetableRowView build() {
            WeeklyTimetableRowView view = new WeeklyTimetableRowView(startTime, endTime);
            for (String day : WORKING_DAYS) {
                view.put(day, slotsByDay.get(day));
            }
            return view;
        }
    }

    private record DayScheduleInsight(int completedCount, int upcomingCount, TimetableSlotView currentSlot, TimetableSlotView nextSlot) {}

    private String buildInitials(String name) {
        if (name == null || name.isBlank()) {
            return "TE";
        }
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

    private String normalizeText(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
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

    private String resolveCollegeName(Admin admin) {
        return admin != null ? normalizeText(admin.getCollegeName(), "AI Campus Institute") : "AI Campus Institute";
    }

    private String resolveCollegeLogo(Admin admin) {
        if (admin != null && admin.getCollege() != null) {
            return admin.getCollege().getLogoPath();
        }
        return null;
    }

    private String formatCurrency(double value) {
        return String.format(Locale.ENGLISH, "Rs %,.0f", value);
    }
}
