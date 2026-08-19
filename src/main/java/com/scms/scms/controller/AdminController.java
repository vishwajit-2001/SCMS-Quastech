package com.scms.scms.controller;

import com.scms.scms.model.*;
import com.scms.scms.repository.*;
import com.scms.scms.service.AcademicStructureService;
import com.scms.scms.service.EmployeeIdService;
import com.scms.scms.service.PasswordProtectionService;
import com.scms.scms.service.TeacherAcademicMappingService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Controller
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);
    @Autowired private StudentRepository   studentRepo;
    @Autowired private TeacherRepository   teacherRepo;
    @Autowired private ClassRoomRepository classRepo;
    @Autowired private TimetableRepository timetableRepo;
    @Autowired private StudyMaterialRepository studyMaterialRepo;
    @Autowired private CampusEventRepository campusEventRepo;
    @Autowired private CampusEventApplicationRepository campusEventApplicationRepo;
    @Autowired private AdminRepository     adminRepo;
    @Autowired private CourseRepository    courseRepo;
    @Autowired private BatchRepository     batchRepo;
    @Autowired private AcademicStructureRepository academicStructureRepo;
    @Autowired private SubjectRepository    subjectRepo;
    @Autowired private AssignmentRepository assignmentRepo;
    @Autowired private ExamAttendanceRepository examAttendanceRepo;
    @Autowired private ExamSessionRepository examSessionRepo;
    @Autowired private SubjectMasterRepository subjectMasterRepo;
    @Autowired private FeesRepository       feesRepo;
    @Autowired private PlacementTpoRepository placementTpoRepo;
    @Autowired private PlacementDriveRepository placementDriveRepo;
    @Autowired private PlacementApplicationRepository placementApplicationRepo;
    @Autowired private PlacementInterviewRepository placementInterviewRepo;
    @Autowired private StudentDocumentRepository studentDocumentRepo;
    @Autowired private TeacherAttendanceSessionRepository teacherAttendanceSessionRepo;
    @Autowired private TeacherAttendanceEntryRepository teacherAttendanceEntryRepo;
    @Autowired private TeacherAcademicMappingRepository teacherAcademicMappingRepo;
    @Autowired private PortalNotificationRepository portalNotificationRepo;
    @Autowired private AcademicStructureService academicStructureService;
    @Autowired private EmployeeIdService employeeIdService;
    @Autowired private PasswordProtectionService passwordProtectionService;
    @Autowired private TeacherAcademicMappingService teacherAcademicMappingService;

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
        } catch (IOException e) {
            log.warn("Failed to delete stored upload {}: {}", target, e.getMessage());
        }
    }

    private void deleteStudentDependencies(Student student) {
        campusEventApplicationRepo.deleteByStudent(student);
        examAttendanceRepo.deleteByStudent(student);
        placementApplicationRepo.deleteByStudent(student);
        teacherAttendanceEntryRepo.deleteByStudent(student);
        studentDocumentRepo.findByStudentIdOrderByDocumentNameAsc(student.getId()).forEach(document -> {
            deleteStoredUpload(document.getStoredPath());
            studentDocumentRepo.delete(document);
        });

        placementInterviewRepo.findForSelectedStudent(student).forEach(interview -> {
            interview.getSelectedStudents().removeIf(selected ->
                    selected != null && selected.getId() != null && selected.getId().equals(student.getId()));
            placementInterviewRepo.save(interview);
        });

        deleteStoredUpload(student.getPhoto());
        deleteStoredUpload(student.getSignature());
    }

    private void deleteTeacherDependencies(Teacher teacher, Admin admin) {
        teacherAcademicMappingRepo.deleteByTeacher(teacher);

        timetableRepo.findByTeacher(teacher).forEach(entry -> {
            entry.setTeacher(null);
            timetableRepo.save(entry);
        });

        studyMaterialRepo.findByTeacherOrderByUploadedAtDesc(teacher).forEach(material -> {
            material.setTeacher(null);
            studyMaterialRepo.save(material);
        });

        campusEventRepo.findByTeacherOrderByCreatedAtDesc(teacher).forEach(event -> {
            event.setTeacher(null);
            campusEventRepo.save(event);
        });

        assignmentRepo.findByTeacherOrderByCreatedAtDesc(teacher).forEach(assignment -> {
            assignment.setTeacher(null);
            assignmentRepo.save(assignment);
        });

        examSessionRepo.findByTeacherOrderByExamDateTimeDesc(teacher).forEach(examSession -> {
            examSession.setTeacher(null);
            examSessionRepo.save(examSession);
        });

        teacherAttendanceSessionRepo.findByTeacherOrderByAttendanceDateDescCreatedAtDesc(teacher).forEach(session -> {
            session.setTeacher(null);
            teacherAttendanceSessionRepo.save(session);
        });

        deleteStoredUpload(teacher.getPhoto());
    }

    private void deleteBatchDependencies(Admin admin, Batch batch) {
        teacherAcademicMappingRepo.deleteByAdminAndBatch(admin, batch);
        subjectRepo.findByAdminAndBatchRef(admin, batch).forEach(subjectRepo::delete);
        academicStructureRepo.findByAdminAndBatch(admin, batch).forEach(academicStructureRepo::delete);
        studentRepo.findByAdminAndBatch(admin, batch).forEach(student -> {
            student.setBatch(null);
            studentRepo.save(student);
        });
        feesRepo.findByAdminAndBatch(admin, batch).forEach(fee -> {
            fee.setBatch(null);
            fee.setBatchName(null);
            feesRepo.save(fee);
        });
    }

    private void deleteCourseDependencies(Admin admin, Course course) {
        teacherAcademicMappingRepo.deleteByAdminAndCourse(admin, course);
        batchRepo.findByAdminAndCourseOrderByDisplayNameAsc(admin, course).forEach(batch -> {
            deleteBatchDependencies(admin, batch);
            batchRepo.delete(batch);
        });
        subjectRepo.findByAdminAndCourseRef(admin, course).forEach(subjectRepo::delete);
        subjectMasterRepo.findByAdminAndCourse(admin, course).forEach(subjectMasterRepo::delete);
        academicStructureRepo.findByAdminAndCourse(admin, course).forEach(academicStructureRepo::delete);
        feesRepo.findByAdmin(admin).stream()
                .filter(fee -> fee.getCourse() != null && fee.getCourse().equalsIgnoreCase(course.getCode()))
                .forEach(feesRepo::delete);
    }

    private static final String UPLOAD_DIR_STUDENTS =
            System.getProperty("user.dir") + "/src/main/resources/static/uploads/students/";

    private static final String UPLOAD_DIR_TEACHERS =
            System.getProperty("user.dir") + "/src/main/resources/static/uploads/teachers/";

    private static final List<String> STUDENT_IMPORT_HEADERS = List.of(
            "Full Name",
            "Email",
            "Password",
            "Gender",
            "Date of Birth (dd/mm/yyyy)",
            "Blood Group",
            "Phone Number",
            "Course Code",
            "Batch",
            "Semester",
            "Section",
            "Roll No",
            "Enrollment No",
            "PRN Number",
            "Registration No",
            "Medium",
            "Religion",
            "Category",
            "Father Name",
            "Mother Name",
            "Guardian Name",
            "Admission Date (dd/mm/yyyy)",
            "Aadhar Number",
            "Bank Name",
            "Bank Account No",
            "IFSC Code",
            "Photo Path"
    );

    private static final List<String> TEACHER_IMPORT_HEADERS = List.of(
            "Full Name",
            "Email",
            "Password",
            "Gender",
            "Date of Birth (dd/mm/yyyy)",
            "Blood Group",
            "Phone Number",
            "Alternate Phone",
            "Religion",
            "Caste",
            "Category",
            "Address",
            "City",
            "State",
            "Department",
            "Designation",
            "Employment Type",
            "Salary",
            "Joining Date (dd/mm/yyyy)",
            "Specialization",
            "Experience (1/3/5/10/10+)",
            "Highest Qualification",
            "Degree Specialization",
            "University",
            "Year of Passing",
            "Publications Count",
            "Aadhar Number",
            "PAN Number",
            "Voter ID",
            "Passport Number",
            "Bank Name",
            "Bank Account No",
            "IFSC Code",
            "PF UAN Number",
            "Emergency Contact Name",
            "Emergency Contact Phone",
            "Emergency Contact Relation",
            "Photo Path"
    );

    // ── Auto academic year e.g. "2025-26" ──
    private String currentAcademicYear() {
        int year  = LocalDate.now().getYear();
        int month = LocalDate.now().getMonthValue();
        int start = (month >= 6) ? year : year - 1;
        return start + "-" + String.valueOf(start + 1).substring(2);
    }

    private List<String> academicYearOptions() {
        int year = LocalDate.now().getYear();
        List<String> options = new ArrayList<>();
        for (int offset = -2; offset <= 2; offset++) {
            int start = year + offset;
            options.add(start + "-" + String.valueOf(start + 1).substring(2));
        }
        String current = currentAcademicYear();
        if (!options.contains(current)) {
            options.add(current);
        }
        return options.stream().distinct().sorted().collect(Collectors.toList());
    }

    private List<String> academicYearsFromBatches(Admin admin) {
        List<String> years = new ArrayList<>();
        for (Batch batch : batchRepo.findByAdminOrderByDisplayNameAsc(admin)) {
            if (batch.getStartYear() == null || batch.getEndYear() == null) {
                continue;
            }
            for (int year = batch.getStartYear(); year < batch.getEndYear(); year++) {
                years.add(year + "-" + String.valueOf(year + 1).substring(2));
            }
        }
        if (years.isEmpty()) {
            years.add(currentAcademicYear());
        }
        return years.stream().distinct().sorted().collect(Collectors.toList());
    }

    private Integer academicYearStart(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) return null;
        String[] parts = normalized.replace('–', '-').split("-");
        if (parts.length < 2) return null;
        try {
            int start = Integer.parseInt(parts[0].replaceAll("[^0-9]", ""));
            String endPart = parts[1].replaceAll("[^0-9]", "");
            if (endPart.isBlank()) return null;
            int end = Integer.parseInt(endPart);
            if (start < 100) {
                start = 2000 + start;
            }
            if (end < 100) {
                end = (start / 100) * 100 + end;
                if (end < start) {
                    end += 100;
                }
            }
            return start;
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean academicYearMatches(String left, String right) {
        String a = blankToNull(left);
        String b = blankToNull(right);
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

    // ── Get logged-in admin from session ──
    private Admin getLoggedAdmin(HttpSession session) {
        String email = (String) session.getAttribute("loggedInUser");
        if (email == null) return null;
        return adminRepo.findByEmail(email);
    }

    // ── Helper: add all admin attributes to model ──
    private void addAdminAttributes(Model model, Admin admin) {
        model.addAttribute("adminName",   admin.getName());
        model.addAttribute("adminEmail",  admin.getEmail());
        model.addAttribute("collegeName", admin.getCollegeName());
        model.addAttribute("collegeLogoPath", admin.getCollege() != null ? admin.getCollege().getLogoPath() : null);
        model.addAttribute("collegeCode", admin.getCollegeCode());
        model.addAttribute("adminRole",   admin.getRole());
        model.addAttribute("adminPhone",  admin.getPhone());
    }

    private synchronized String nextTeacherEmployeeId() {
        String candidate = nextEmployeeId("TCH", teacherRepo.findAll().stream()
                .map(Teacher::getEmployeeId)
                .toList());
        while (teacherRepo.existsByEmployeeIdIgnoreCase(candidate)) {
            candidate = incrementEmployeeId("TCH", candidate);
        }
        return candidate;
    }

    private synchronized String nextTpoEmployeeId() {
        String candidate = nextEmployeeId("TPO", placementTpoRepo.findAll().stream()
                .map(PlacementTpo::getEmployeeId)
                .toList());
        while (placementTpoRepo.existsByEmployeeIdIgnoreCase(candidate)) {
            candidate = incrementEmployeeId("TPO", candidate);
        }
        return candidate;
    }

    private String nextEmployeeId(String prefix, List<String> existingIds) {
        int max = existingIds == null ? 0 : existingIds.stream()
                .mapToInt(value -> employeeIdNumber(prefix, value))
                .max()
                .orElse(0);
        return formatEmployeeId(prefix, max + 1);
    }

    private String incrementEmployeeId(String prefix, String currentId) {
        return formatEmployeeId(prefix, employeeIdNumber(prefix, currentId) + 1);
    }

    private int employeeIdNumber(String prefix, String employeeId) {
        String value = blankToNull(employeeId);
        if (value == null || !value.toUpperCase(Locale.ENGLISH).startsWith(prefix)) {
            return 0;
        }
        String suffix = value.substring(prefix.length()).replaceAll("[^0-9]", "");
        if (suffix.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(suffix);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String formatEmployeeId(String prefix, int number) {
        return prefix + String.format("%03d", Math.max(number, 1));
    }

    private List<ClassRoom> getSortedAdminClasses(Admin admin) {
        return classRepo.findByAdmin(admin).stream()
                .sorted(Comparator.comparing(
                        (ClassRoom c) -> c.getName() != null ? c.getName() : "",
                        String.CASE_INSENSITIVE_ORDER
                ).thenComparing(ClassRoom::getId))
                .toList();
    }

    private ClassOptionView toClassOptionView(ClassRoom classRoom) {
        return new ClassOptionView(
                classRoom.getId(),
                classRoom.getName(),
                classRoom.getCourse(),
                classRoom.getAcademicYear(),
                classRoom.getSemester(),
                classRoom.getSection(),
                classRoom.getTotalFees(),
                classRoom.getBatch(),
                classRoom.getBatchName(),
                classRoom.getStatus()
        );
    }

    private void addAddStudentModel(Model model, Admin admin) {
        List<Course> courseDetails = courseRepo.findByAdminOrderByCodeAsc(admin);
        List<String> courses = courseDetails.stream()
                .map(course -> course.getCode() != null ? course.getCode().strip() : null)
                .filter(c -> c != null && !c.isBlank())
                .distinct().collect(Collectors.toList());
        if (courses.isEmpty()) {
            courses = batchRepo.findByAdminOrderByDisplayNameAsc(admin).stream()
                    .map(batch -> batch.getCourse() != null ? batch.getCourse().getCode() : null)
                    .filter(c -> c != null && !c.isBlank())
                    .distinct()
                    .collect(Collectors.toList());
        }
        model.addAttribute("courses", courses);
        model.addAttribute("courseDetails", courseDetails);
        model.addAttribute("batches", batchRepo.findByAdminOrderByDisplayNameAsc(admin));
        model.addAttribute("academicYears", academicYearsFromBatches(admin));
        model.addAttribute("academicYear", currentAcademicYear());
        model.addAttribute("feeRules", feeRulesForAdmin(admin));
        addAdminAttributes(model, admin);
    }

    private List<Teacher> activeTeachersForAdmin(Admin admin) {
        return teacherRepo.findByAdmin(admin).stream()
                .filter(teacher -> teacher.getStatus() == null || teacher.getStatus().equalsIgnoreCase("Active"))
                .sorted(Comparator.comparing(
                        (Teacher teacher) -> teacher.getName() != null ? teacher.getName() : "",
                        String.CASE_INSENSITIVE_ORDER
                ).thenComparing(Teacher::getId))
                .toList();
    }

    private void addAddClassModel(Model model, Admin admin) {
        model.addAttribute("courses", courseRepo.findByAdminOrderByCodeAsc(admin));
        model.addAttribute("batches", batchRepo.findByAdminOrderByDisplayNameAsc(admin));
        model.addAttribute("feeRules", feeRulesForAdmin(admin));
        addAdminAttributes(model, admin);
    }

    private void addAdminProfileMetrics(Model model, Admin admin) {
        model.addAttribute("totalStudents", studentRepo.findByAdmin(admin).size());
        model.addAttribute("totalTeachers", teacherRepo.findByAdmin(admin).size());
        model.addAttribute("totalCourses", courseRepo.findByAdminOrderByCodeAsc(admin).size());
        model.addAttribute("totalBatches", batchRepo.findByAdminOrderByDisplayNameAsc(admin).size());
    }

    private List<AcademicStructure> academicStructureRowsForView(Admin admin) {
        LinkedHashMap<String, AcademicStructure> uniqueRows = new LinkedHashMap<>();
        for (AcademicStructure structure : academicStructureRepo.findByAdminOrderByBatch_DisplayNameAscYearLabelAscSemesterNumberAscSectionAsc(admin)) {
            if (structure == null) continue;
            String key = String.join("|",
                    structure.getCourse() != null && structure.getCourse().getId() != null ? String.valueOf(structure.getCourse().getId()) : "",
                    structure.getBatch() != null && structure.getBatch().getId() != null ? String.valueOf(structure.getBatch().getId()) : "",
                    blankToNull(structure.getYearLabel()) != null ? structure.getYearLabel().strip().toUpperCase(Locale.ENGLISH) : "",
                    structure.getSemesterNumber() != null ? String.valueOf(structure.getSemesterNumber()) : "");
            uniqueRows.putIfAbsent(key, structure);
        }
        return new ArrayList<>(uniqueRows.values());
    }

    private String batchAcademicYearsSummary(Batch batch) {
        if (batch == null || batch.getStartYear() == null || batch.getEndYear() == null) {
            return "-";
        }
        List<String> years = new ArrayList<>();
        for (int year = batch.getStartYear(); year < batch.getEndYear(); year++) {
            years.add(year + "-" + String.valueOf(year + 1).substring(2));
        }
        return String.join(" / ", years);
    }

    private String batchYearLabelsSummary(Batch batch) {
        if (batch == null || batch.getCourse() == null || batch.getCourse().getCode() == null) {
            return "-";
        }
        List<String> labels = new ArrayList<>();
        int phases = batchAcademicYearPhaseCount(batch);
        String code = batch.getCourse().getCode().trim().toUpperCase(Locale.ENGLISH);
        for (int i = 0; i < phases; i++) {
            labels.add((i == 0 ? "FY" : i == 1 ? "SY" : i == 2 ? "TY" : "Y" + (i + 1)) + code);
        }
        return String.join(" / ", labels);
    }

    private int batchAcademicYearPhaseCount(Batch batch) {
        if (batch == null || batch.getCourse() == null || batch.getCourse().getTotalSemesters() == null) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(batch.getCourse().getTotalSemesters() / 2.0));
    }

    private void addAcademicStructureModel(Model model, Admin admin) {
        List<AcademicStructureService.StructureMatrixRow> structureRows = academicStructureService.listAdminStructureRows(admin);
        model.addAttribute("courses", courseRepo.findByAdminOrderByCodeAsc(admin));
        model.addAttribute("batches", batchRepo.findByAdminOrderByDisplayNameAsc(admin));
        model.addAttribute("structureRows", structureRows);
        model.addAttribute("structureTotalRows", structureRows.size());
        model.addAttribute("structureTotalSemesters", structureRows.stream()
                .map(AcademicStructureService.StructureMatrixRow::semesterNumber)
                .filter(Objects::nonNull)
                .distinct()
                .count());
        model.addAttribute("structureYearOptions", structureRows.stream()
                .map(AcademicStructureService.StructureMatrixRow::yearLabel)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        model.addAttribute("structureAcademicYearOptions", structureRows.stream()
                .map(AcademicStructureService.StructureMatrixRow::academicYear)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
    }

    private void addStudentFormError(Model model, Admin admin, String error) {
        model.addAttribute("error", error);
        addAddStudentModel(model, admin);
    }

    private String renderTimetablePage(Model model,
                                       Admin admin,
                                       Long batchId,
                                       Long classId,
                                       String course,
                                       String academicYear,
                                       Integer semester,
                                       String error,
                                       List<String> bulkValidationErrors) {
        ClassRoom selectedClass = classId != null ? classRepo.findById(classId).orElse(null) : null;
        if (selectedClass != null && (selectedClass.getAdmin() == null || !selectedClass.getAdmin().getId().equals(admin.getId()))) {
            selectedClass = null;
        }

        syncClassRoomsFromAcademicStructure(admin);

        Batch selectedBatch = getAdminBatch(admin, batchId);
        if (selectedBatch == null && selectedClass != null) {
            selectedBatch = resolveBatchForClass(admin, selectedClass);
            if (selectedBatch != null) {
                batchId = selectedBatch.getId();
            }
        }

        String resolvedCourse = blankToNull(course);
        if (resolvedCourse == null && selectedClass != null) {
            resolvedCourse = blankToNull(selectedClass.getCourse());
        }

        String resolvedAcademicYear = blankToNull(academicYear);
        if (resolvedAcademicYear == null && selectedClass != null) {
            resolvedAcademicYear = blankToNull(selectedClass.getAcademicYear());
            if (resolvedAcademicYear == null) {
                resolvedAcademicYear = blankToNull(selectedClass.getYear());
            }
        }

        Integer resolvedSemester = semester;
        if (resolvedSemester == null && selectedClass != null) {
            resolvedSemester = selectedClass.getSemester();
        }

        List<Timetable> timetable = timetableEntriesForView(admin, classId, batchId, resolvedCourse, resolvedAcademicYear, resolvedSemester);
        populateTimetableGridModel(model, admin, timetable, classId, batchId, resolvedCourse, resolvedAcademicYear, resolvedSemester);
        model.addAttribute("classes", timetableClassesForScope(admin, batchId, resolvedCourse, resolvedAcademicYear, resolvedSemester));
        model.addAttribute("teachers", activeTeachersForAdmin(admin));
        List<Subject> scopedSubjects = timetableSubjectsForScope(admin, batchId, resolvedCourse, resolvedSemester);
        model.addAttribute("selectedBatch", selectedBatch);
        model.addAttribute("selectedClass", selectedClass);
        model.addAttribute("subjectOptions", scopedSubjects);
        model.addAttribute("scopeSubjects", scopedSubjects);
        model.addAttribute("scopeSubjectsLoaded", selectedBatch != null && blankToNull(resolvedCourse) != null && resolvedSemester != null);
        model.addAttribute("timetableSubjectOptions", scopedSubjects.stream()
                .map(this::toTimetableSubjectOptionView)
                .toList());
        model.addAttribute("manageMode", true);
        if (error != null) {
            model.addAttribute("error", error);
        }
        if (bulkValidationErrors != null && !bulkValidationErrors.isEmpty()) {
            model.addAttribute("bulkValidationErrors", bulkValidationErrors);
        }
        addAdminAttributes(model, admin);
        return "admin/add-timetable";
    }

    private void syncClassRoomsFromAcademicStructure(Admin admin) {
        if (admin == null) return;
        List<AcademicStructureService.StructureMatrixRow> structures = academicStructureService.listAdminStructureRows(admin);
        if (structures == null || structures.isEmpty()) {
            return;
        }

        List<ClassRoom> existingRooms = classRepo.findByAdmin(admin);
        for (AcademicStructureService.StructureMatrixRow structure : structures) {
            if (structure == null || structure.courseCode() == null || structure.batchName() == null || structure.semesterNumber() == null) {
                continue;
            }
            String normalizedSection = blankToNull(structure.section()) == null ? null : structure.section().strip().toUpperCase(Locale.ENGLISH);
            boolean matched = existingRooms.stream().anyMatch(classRoom -> {
                if (classRoom == null) return false;
                boolean courseMatch = blankToNull(classRoom.getCourseCode()) != null && blankToNull(structure.courseCode()) != null
                        && classRoom.getCourseCode().equalsIgnoreCase(structure.courseCode());
                boolean batchMatch = blankToNull(classRoom.getBatchName()) != null && blankToNull(structure.batchName()) != null
                        && classRoom.getBatchName().equalsIgnoreCase(structure.batchName());
                boolean yearMatch = academicYearMatches(classRoom.getAcademicYear(), structure.academicYear());
                boolean semesterMatch = classRoom.getSemester() != null && classRoom.getSemester().equals(structure.semesterNumber());
                boolean sectionMatch = normalizedSection != null && classRoom.getSection() != null
                        && normalizedSection.equalsIgnoreCase(classRoom.getSection());
                return courseMatch && batchMatch && yearMatch && semesterMatch && sectionMatch;
            });
            if (matched) {
                continue;
            }

            ClassRoom classRoom = new ClassRoom();
            classRoom.setAdmin(admin);
            classRoom.setName(buildClassroomName(structure));
            classRoom.setCourse(structure.courseCode());
            classRoom.setCourseCode(structure.courseCode());
            classRoom.setYear(structure.yearLabel());
            classRoom.setSemester(structure.semesterNumber());
            classRoom.setSection(normalizedSection);
            classRoom.setRoom(null);
            classRoom.setBatch(structure.batchName());
            classRoom.setBatchName(structure.batchName());
            classRoom.setAcademicYear(structure.academicYear());
            classRoom.setStatus("Active");
            classRepo.save(classRoom);
            existingRooms.add(classRoom);
        }
    }

    private String buildClassroomName(AcademicStructureService.StructureMatrixRow structure) {
        if (structure == null) return "Section";
        List<String> parts = new ArrayList<>();
        if (blankToNull(structure.courseCode()) != null) {
            parts.add(structure.courseCode().strip());
        }
        if (blankToNull(structure.batchName()) != null) {
            parts.add(structure.batchName().strip());
        }
        if (blankToNull(structure.yearLabel()) != null) {
            parts.add(structure.yearLabel().strip());
        }
        if (structure.semesterNumber() != null) {
            parts.add("Sem " + structure.semesterNumber());
        }
        if (blankToNull(structure.section()) != null) {
            parts.add("Sec " + structure.section().strip().toUpperCase(Locale.ENGLISH));
        }
        return String.join(" | ", parts);
    }

    private Course resolveTimetableScopeCourse(Admin admin, Batch selectedBatch, String course) {
        if (selectedBatch != null && selectedBatch.getCourse() != null) {
            if (blankToNull(course) == null) {
                return selectedBatch.getCourse();
            }
            String normalizedCourse = normalizeCourseKey(course);
            String batchCode = normalizeCourseKey(selectedBatch.getCourse().getCode());
            String batchName = normalizeCourseKey(selectedBatch.getCourse().getName());
            if (normalizedCourse != null && (normalizedCourse.equals(batchCode) || normalizedCourse.equals(batchName))) {
                return selectedBatch.getCourse();
            }
            return null;
        }
        return resolveAdminCourseByInput(admin, course);
    }

    private List<Subject> timetableSubjectsForScope(Admin admin, Long batchId, String course, Integer semester) {
        if (admin == null || batchId == null || semester == null || blankToNull(course) == null) {
            return List.of();
        }

        Batch selectedBatch = getAdminBatch(admin, batchId);
        Course selectedCourse = resolveTimetableScopeCourse(admin, selectedBatch, course);
        if (selectedBatch == null || selectedCourse == null) {
            return List.of();
        }

        return subjectRepo.findByAdminAndBatchRefAndCourseRefAndSemesterAndStatusIgnoreCaseOrderByNameAscCodeAsc(
                admin,
                selectedBatch,
                selectedCourse,
                semester,
                "active"
        );
    }

    private Batch resolveBatchForClass(Admin admin, ClassRoom classRoom) {
        if (admin == null || classRoom == null) {
            return null;
        }
        for (Batch batch : batchRepo.findByAdminOrderByDisplayNameAsc(admin)) {
            if (batchMatchesClass(classRoom, batch)) {
                return batch;
            }
        }
        return null;
    }

    private boolean isProjectCategory(String category) {
        String normalized = normalizeSubjectCategory(category);
        return "PROJECT".equalsIgnoreCase(normalized);
    }

    private TimetableSubjectOptionView toTimetableSubjectOptionView(Subject subject) {
        String category = normalizeSubjectCategory(subject != null ? subject.getCourseCategory() : null);
        return new TimetableSubjectOptionView(
                subject != null ? subject.getId() : null,
                subject != null ? blankToNull(subject.getName()) : null,
                subject != null ? blankToNull(subject.getCode()) : null,
                category,
                null,
                null,
                isLabCategory(category) ? 2 : 1,
                isProjectCategory(category)
        );
    }

    // ── Helper: blank → null ──
    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.strip();
    }

    private String normalizeEmail(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ENGLISH);
    }

    private String defaultText(String value, String fallback) {
        String normalized = blankToNull(value);
        return normalized == null ? fallback : normalized;
    }

    private String csvValue(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n") || escaped.contains("\r")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private String importPhotoFromPath(String rawPath, String uploadDir, List<String> errors, String fieldLabel) {
        String value = blankToNull(rawPath);
        if (value == null) {
            return null;
        }
        try {
            Path source = Path.of(value).normalize();
            if (!Files.exists(source) || !Files.isRegularFile(source)) {
                errors.add(fieldLabel + " does not exist");
                return null;
            }
            long size = Files.size(source);
            if (size > 2L * 1024 * 1024) {
                errors.add(fieldLabel + " must be 2 MB or smaller");
                return null;
            }
            String filename = source.getFileName() != null ? source.getFileName().toString().toLowerCase(Locale.ENGLISH) : "";
            boolean validExtension = filename.endsWith(".jpg")
                    || filename.endsWith(".jpeg")
                    || filename.endsWith(".png")
                    || filename.endsWith(".webp")
                    || filename.endsWith(".gif");
            if (!validExtension) {
                errors.add(fieldLabel + " must be JPG, JPEG, PNG, WEBP, or GIF");
                return null;
            }

            File dir = new File(uploadDir);
            if (!dir.exists() && !dir.mkdirs()) {
                errors.add("Could not create photo upload directory");
                return null;
            }

            String extension = filename.contains(".") ? filename.substring(filename.lastIndexOf('.')) : ".jpg";
            String storedName = UUID.randomUUID() + extension;
            Path target = Path.of(uploadDir, storedName);
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            String folderName = Path.of(uploadDir).getFileName().toString();
            return "/uploads/" + folderName + "/" + storedName;
        } catch (Exception ex) {
            errors.add("Could not import " + fieldLabel.toLowerCase(Locale.ENGLISH));
            return null;
        }
    }

    private String normalizeHeaderKey(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private boolean isValidEmail(String email) {
        String value = blankToNull(email);
        return value != null && value.matches("(?i)^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$");
    }

    private List<List<String>> parseDelimitedRows(String rawText, char delimiter) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        if (rawText == null) {
            return rows;
        }

        List<String> currentRow = new ArrayList<>();
        StringBuilder currentCell = new StringBuilder();
        boolean inQuotes = false;

        try (StringReader reader = new StringReader(rawText)) {
            int next;
            while ((next = reader.read()) != -1) {
                char ch = (char) next;
                if (ch == '"') {
                    if (inQuotes) {
                        reader.mark(1);
                        int peek = reader.read();
                        if (peek == '"') {
                            currentCell.append('"');
                        } else {
                            inQuotes = false;
                            if (peek != -1) {
                                reader.reset();
                            }
                        }
                    } else {
                        inQuotes = true;
                    }
                } else if (ch == delimiter && !inQuotes) {
                    currentRow.add(currentCell.toString());
                    currentCell.setLength(0);
                } else if ((ch == '\n' || ch == '\r') && !inQuotes) {
                    if (ch == '\r') {
                        reader.mark(1);
                        int peek = reader.read();
                        if (peek != '\n' && peek != -1) {
                            reader.reset();
                        }
                    }
                    currentRow.add(currentCell.toString());
                    currentCell.setLength(0);
                    rows.add(new ArrayList<>(currentRow));
                    currentRow.clear();
                } else {
                    currentCell.append(ch);
                }
            }
        }

        if (currentCell.length() > 0 || !currentRow.isEmpty()) {
            currentRow.add(currentCell.toString());
            rows.add(currentRow);
        }
        return rows;
    }

    private List<List<String>> parseCsvRows(String rawCsv) throws IOException {
        return parseDelimitedRows(rawCsv, ',');
    }

    private List<List<String>> parseWorkbookRows(MultipartFile file) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            if (workbook.getNumberOfSheets() == 0) {
                return rows;
            }
            Sheet sheet = workbook.getSheetAt(0);
            int maxColumns = 0;
            for (Row row : sheet) {
                if (row != null && row.getLastCellNum() > maxColumns) {
                    maxColumns = row.getLastCellNum();
                }
            }
            for (Row row : sheet) {
                List<String> values = new ArrayList<>();
                for (int i = 0; i < maxColumns; i++) {
                    Cell cell = row.getCell(i);
                    values.add(cell == null ? "" : formatter.formatCellValue(cell));
                }
                rows.add(values);
            }
        } catch (Exception ex) {
            throw new IOException("Could not read spreadsheet file", ex);
        }
        return rows;
    }

    private List<List<String>> parseImportRows(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase(Locale.ENGLISH) : "";
        if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
            return parseWorkbookRows(file);
        }
        String rawText = new String(file.getBytes(), StandardCharsets.UTF_8);
        if (filename.endsWith(".tsv")) {
            return parseDelimitedRows(rawText, '\t');
        }
        if (filename.endsWith(".txt")) {
            return rawText.contains("\t") ? parseDelimitedRows(rawText, '\t') : parseDelimitedRows(rawText, ',');
        }
        return parseCsvRows(rawText);
    }

    private String importFileErrorMessage(String filename) {
        String lower = filename == null ? "" : filename.toLowerCase(Locale.ENGLISH);
        if (lower.endsWith(".numbers")) {
            return "Numbers file direct supported nahi hai. Please export it as CSV or Excel (.xlsx) and upload again.";
        }
        return "Supported files: CSV, TSV, TXT, XLSX, XLS.";
    }

    private boolean isSupportedImportFilename(String filename) {
        String lower = filename == null ? "" : filename.toLowerCase(Locale.ENGLISH);
        return lower.endsWith(".csv")
                || lower.endsWith(".tsv")
                || lower.endsWith(".txt")
                || lower.endsWith(".xlsx")
                || lower.endsWith(".xls");
    }

    private Map<String, Object> processStudentImport(Admin admin, MultipartFile file, String mode) throws IOException {
        if (admin == null) {
            return Map.of("error", "Your session has expired. Please log in again.");
        }
        if (file == null || file.isEmpty()) {
            return Map.of("error", "Please choose a student file.");
        }
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase(Locale.ENGLISH) : "";
        if (!isSupportedImportFilename(filename)) {
            return Map.of("error", importFileErrorMessage(filename));
        }

        List<List<String>> rows = parseImportRows(file);
        if (rows.isEmpty()) {
            return Map.of("error", "The uploaded file is empty.");
        }

        List<String> headers = rows.get(0).stream().map(value -> value == null ? "" : value.strip()).toList();
        if (!hasExpectedHeaders(headers, STUDENT_IMPORT_HEADERS)) {
            return Map.of("error", "Template columns do not match the required student CSV format.");
        }

        LinkedHashMap<String, Course> courseCatalog = buildCourseCatalog(courseRepo.findByAdminOrderByCodeAsc(admin));
        Map<String, Batch> batchByName = batchRepo.findByAdminOrderByDisplayNameAsc(admin).stream()
                .collect(Collectors.toMap(
                        batch -> normalizeBatchDisplayKey(batch.getDisplayName()),
                        batch -> batch,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        Set<String> existingEmails = studentRepo.findByAdmin(admin).stream()
                .map(Student::getEmail)
                .map(this::normalizeCourseKey)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.toCollection(HashSet::new));
        Set<String> importEmails = new HashSet<>();
        Set<String> importEnrollments = new HashSet<>();

        List<Map<String, Object>> previewRows = new ArrayList<>();
        List<Student> readyStudents = new ArrayList<>();

        for (int i = 1; i < rows.size(); i++) {
            List<String> rowValues = rows.get(i);
            if (isCsvRowBlank(rowValues)) {
                continue;
            }

            Map<String, String> values = mapCsvRow(headers, rowValues);
            List<String> errors = new ArrayList<>();

            String fullName = blankToNull(values.get("Full Name"));
            String email = blankToNull(values.get("Email"));
            String password = blankToNull(values.get("Password"));
            String gender = normalizeGenderValue(values.get("Gender"));
            String courseCode = resolveCourseCode(values.get("Course Code"), courseCatalog);
            String batchName = values.get("Batch");
            Batch batch = batchByName.get(normalizeBatchDisplayKey(batchName));
            Integer semesterNumber = parseFlexibleInteger(values.get("Semester"));
            String semester = semesterNumber != null ? "Sem" + semesterNumber : null;
            String section = blankToNull(values.get("Section"));
            String importedPhotoPath = importPhotoFromPath(values.get("Photo Path"), UPLOAD_DIR_STUDENTS, errors, "Photo Path");

            if (fullName == null) errors.add("Full Name is required");
            if (email == null) {
                errors.add("Email is required");
            } else if (!isValidEmail(email)) {
                errors.add("Email format is invalid");
            } else {
                String normalizedEmail = normalizeCourseKey(email);
                if (existingEmails.contains(normalizedEmail) || !importEmails.add(normalizedEmail)) {
                    errors.add("Email already exists");
                }
            }
            if (password == null || password.length() < 6) errors.add("Password must be at least 6 characters");
            if (gender == null) errors.add("Gender must be Male, Female, or Other");
            if (courseCode == null) errors.add("Course Code does not exist");
            if (batch == null) {
                errors.add("Batch does not exist");
            }
            if (semester == null) errors.add("Semester must be a valid number");
            if (section == null) errors.add("Section is required");
            if (batch != null && courseCode != null && batch.getCourse() != null && batch.getCourse().getCode() != null
                    && !batch.getCourse().getCode().equalsIgnoreCase(courseCode)) {
                errors.add("Batch does not belong to the selected course");
            }

            String enrollmentNo = blankToNull(values.get("Enrollment No"));
            if (enrollmentNo != null) {
                if (studentRepo.existsByAdminAndEnrollmentNoIgnoreCase(admin, enrollmentNo) || !importEnrollments.add(normalizeCourseKey(enrollmentNo))) {
                    errors.add("Enrollment No already exists");
                }
            }

            String errorReason = errors.isEmpty() ? null : String.join("; ", errors);
            previewRows.add(buildPreviewRow(i + 1, values, errorReason));
            if (errorReason != null) {
                continue;
            }

            Student student = new Student();
            student.setAdmin(admin);
            student.setName(fullName);
            student.setEmail(normalizeEmail(email));
            student.setPassword(passwordProtectionService.encode(password));
            student.setGender(gender);
            student.setDob(parseFlexibleDate(values.get("Date of Birth (dd/mm/yyyy)")));
            student.setBloodGroup(blankToNull(values.get("Blood Group")));
            student.setCourse(courseCode);
            student.setBatch(batch);
            student.setSemester(semester);
            student.setSectionName(section);
            student.setRollNo(blankToNull(values.get("Roll No")));
            student.setEnrollmentNo(enrollmentNo);
            student.setPrnNumber(blankToNull(values.get("PRN Number")));
            student.setRegistrationNo(blankToNull(values.get("Registration No")));
            student.setMedium(blankToNull(values.get("Medium")));
            student.setReligion(blankToNull(values.get("Religion")));
            student.setCategory(blankToNull(values.get("Category")));
            student.setFatherName(blankToNull(values.get("Father Name")));
            student.setMotherName(blankToNull(values.get("Mother Name")));
            student.setGuardianName(blankToNull(values.get("Guardian Name")));
            student.setAdmissionDate(parseFlexibleDate(values.get("Admission Date (dd/mm/yyyy)")));
            student.setAadharNumber(blankToNull(values.get("Aadhar Number")));
            student.setBankName(blankToNull(values.get("Bank Name")));
            student.setBankAccNo(blankToNull(values.get("Bank Account No")));
            student.setIfscCode(blankToNull(values.get("IFSC Code")) != null ? values.get("IFSC Code").strip().toUpperCase(Locale.ENGLISH) : null);
            student.setPhoto(importedPhotoPath);
            if (batch != null && batch.getStartYear() != null) {
                student.setAcademicYear(batch.getStartYear() + "-" + String.valueOf(batch.getStartYear() + 1).substring(2));
            } else {
                student.setAcademicYear(currentAcademicYear());
            }
            Course selectedCourse = resolveAdminCourseByInput(admin, courseCode);
            student.setDegree(blankToNull(degreeHintForCourse(selectedCourse)));
            ClassRoom resolvedClassRoom = resolveStudentClassRoom(admin, batch, courseCode, student.getAcademicYear(), semester, section);
            if (resolvedClassRoom != null) {
                student.setClassRoom(resolvedClassRoom);
            }
            double total = feesForStudent(admin, student.getCourse(), batch != null ? batch.getId() : null,
                    batch != null ? batch.getDisplayName() : null, student.getAcademicYear(), student.getSemester());
            student.setTotalFees(total);
            student.setPaidFees(0.0);
            student.setPendingFees(total);
            student.setAdmissionStatus("ADMITTED");
            readyStudents.add(student);
        }

        if (!"import".equalsIgnoreCase(mode)) {
            return buildImportResponse("students", previewRows, 0, STUDENT_IMPORT_HEADERS);
        }

        readyStudents.forEach(student -> {
            studentRepo.save(student);
            ensureApplicationNumber(student);
        });
        return buildImportResponse("students", previewRows, readyStudents.size(), STUDENT_IMPORT_HEADERS);
    }

    private Map<String, Object> processTeacherImport(Admin admin, MultipartFile file, String mode) throws IOException {
        if (admin == null) {
            return Map.of("error", "Your session has expired. Please log in again.");
        }
        if (file == null || file.isEmpty()) {
            return Map.of("error", "Please choose a teacher file.");
        }
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase(Locale.ENGLISH) : "";
        if (!isSupportedImportFilename(filename)) {
            return Map.of("error", importFileErrorMessage(filename));
        }

        List<List<String>> rows = parseImportRows(file);
        if (rows.isEmpty()) {
            return Map.of("error", "The uploaded file is empty.");
        }

        List<String> headers = rows.get(0).stream().map(value -> value == null ? "" : value.strip()).toList();
        if (!hasExpectedHeaders(headers, TEACHER_IMPORT_HEADERS)) {
            return Map.of("error", "Template columns do not match the required teacher CSV format.");
        }

        Set<String> existingEmails = teacherRepo.findByAdmin(admin).stream()
                .map(Teacher::getEmail)
                .map(this::normalizeCourseKey)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.toCollection(HashSet::new));
        Set<String> importEmails = new HashSet<>();
        List<Map<String, Object>> previewRows = new ArrayList<>();
        List<Teacher> readyTeachers = new ArrayList<>();

        for (int i = 1; i < rows.size(); i++) {
            List<String> rowValues = rows.get(i);
            if (isCsvRowBlank(rowValues)) {
                continue;
            }

            Map<String, String> values = mapCsvRow(headers, rowValues);
            List<String> errors = new ArrayList<>();

            String fullName = blankToNull(values.get("Full Name"));
            String email = blankToNull(values.get("Email"));
            String password = blankToNull(values.get("Password"));
            String importedPhotoPath = importPhotoFromPath(values.get("Photo Path"), UPLOAD_DIR_TEACHERS, errors, "Photo Path");

            if (fullName == null) errors.add("Full Name is required");
            if (email == null) {
                errors.add("Email is required");
            } else if (!isValidEmail(email)) {
                errors.add("Email format is invalid");
            } else {
                String normalizedEmail = normalizeCourseKey(email);
                if (existingEmails.contains(normalizedEmail) || !importEmails.add(normalizedEmail)) {
                    errors.add("Email already exists");
                }
            }
            if (password == null || password.length() < 6) errors.add("Password must be at least 6 characters");

            String errorReason = errors.isEmpty() ? null : String.join("; ", errors);
            previewRows.add(buildPreviewRow(i + 1, values, errorReason));
            if (errorReason != null) {
                continue;
            }

            Teacher teacher = new Teacher();
            teacher.setAdmin(admin);
            teacher.setName(fullName);
            teacher.setEmail(normalizeEmail(email));
            teacher.setPassword(passwordProtectionService.encode(password));
            teacher.setGender(normalizeGenderValue(values.get("Gender")));
            teacher.setDob(parseFlexibleDate(values.get("Date of Birth (dd/mm/yyyy)")));
            teacher.setBloodGroup(blankToNull(values.get("Blood Group")));
            teacher.setPhone(blankToNull(values.get("Phone Number")));
            teacher.setAltPhone(blankToNull(values.get("Alternate Phone")));
            teacher.setReligion(blankToNull(values.get("Religion")));
            teacher.setCasteName(blankToNull(values.get("Caste")));
            teacher.setCategory(blankToNull(values.get("Category")));
            teacher.setAddress(blankToNull(values.get("Address")));
            teacher.setCity(blankToNull(values.get("City")));
            teacher.setState(blankToNull(values.get("State")));
            teacher.setDepartment(blankToNull(values.get("Department")));
            teacher.setDesignation(blankToNull(values.get("Designation")));
            teacher.setEmploymentType(blankToNull(values.get("Employment Type")));
            teacher.setSalary(parseFlexibleDouble(values.get("Salary")));
            teacher.setJoiningDate(parseFlexibleDate(values.get("Joining Date (dd/mm/yyyy)")));
            teacher.setSpecialization(blankToNull(values.get("Specialization")));
            teacher.setExperience(normalizeTeacherExperience(values.get("Experience (1/3/5/10/10+)")));
            teacher.setQualification(blankToNull(values.get("Highest Qualification")));
            teacher.setDegreeSpecialization(blankToNull(values.get("Degree Specialization")));
            teacher.setUniversity(blankToNull(values.get("University")));
            teacher.setYearOfPassing(parseFlexibleInteger(values.get("Year of Passing")));
            teacher.setPublications(parseFlexibleInteger(values.get("Publications Count")));
            teacher.setAadharNumber(blankToNull(values.get("Aadhar Number")));
            teacher.setPanCardNumber(blankToNull(values.get("PAN Number")) != null ? values.get("PAN Number").strip().toUpperCase(Locale.ENGLISH) : null);
            teacher.setVoterId(blankToNull(values.get("Voter ID")));
            teacher.setPassportNumber(blankToNull(values.get("Passport Number")) != null ? values.get("Passport Number").strip().toUpperCase(Locale.ENGLISH) : null);
            teacher.setBankName(blankToNull(values.get("Bank Name")));
            teacher.setBankAccNo(blankToNull(values.get("Bank Account No")));
            teacher.setIfscCode(blankToNull(values.get("IFSC Code")) != null ? values.get("IFSC Code").strip().toUpperCase(Locale.ENGLISH) : null);
            String pfUan = blankToNull(values.get("PF UAN Number"));
            teacher.setPfNumber(pfUan);
            teacher.setUanNumber(pfUan);
            teacher.setEmergencyContactName(blankToNull(values.get("Emergency Contact Name")));
            teacher.setEmergencyPhone(blankToNull(values.get("Emergency Contact Phone")));
            teacher.setEmergencyRelation(blankToNull(values.get("Emergency Contact Relation")));
            teacher.setStatus("Active");
            teacher.setPhoto(importedPhotoPath);
            readyTeachers.add(teacher);
        }

        if (!"import".equalsIgnoreCase(mode)) {
            return buildImportResponse("teachers", previewRows, 0, TEACHER_IMPORT_HEADERS);
        }

        readyTeachers.forEach(teacher -> employeeIdService.ensureTeacherEmployeeId(teacherRepo.saveAndFlush(teacher)));
        return buildImportResponse("teachers", previewRows, readyTeachers.size(), TEACHER_IMPORT_HEADERS);
    }

    private boolean isCsvRowBlank(List<String> row) {
        if (row == null || row.isEmpty()) {
            return true;
        }
        return row.stream().allMatch(cell -> blankToNull(cell) == null);
    }

    private Map<String, String> mapCsvRow(List<String> headerRow, List<String> dataRow) {
        Map<String, String> values = new LinkedHashMap<>();
        for (int i = 0; i < headerRow.size(); i++) {
            String header = headerRow.get(i);
            String cell = i < dataRow.size() ? blankToNull(dataRow.get(i)) : null;
            values.put(header, cell);
        }
        return values;
    }

    private boolean hasExpectedHeaders(List<String> actualHeaders, List<String> expectedHeaders) {
        if (actualHeaders == null || actualHeaders.size() != expectedHeaders.size()) {
            return false;
        }
        for (int i = 0; i < expectedHeaders.size(); i++) {
            if (!normalizeHeaderKey(expectedHeaders.get(i)).equals(normalizeHeaderKey(actualHeaders.get(i)))) {
                return false;
            }
        }
        return true;
    }

    private LocalDate parseFlexibleDate(String rawValue) {
        String value = blankToNull(rawValue);
        if (value == null) {
            return null;
        }
        List<DateTimeFormatter> formats = List.of(
                DateTimeFormatter.ofPattern("d/M/uuuu"),
                DateTimeFormatter.ofPattern("dd/MM/uuuu"),
                DateTimeFormatter.ofPattern("d-M-uuuu"),
                DateTimeFormatter.ofPattern("dd-MM-uuuu"),
                DateTimeFormatter.ofPattern("uuuu-MM-dd"),
                DateTimeFormatter.ofPattern("d MMM uuuu", Locale.ENGLISH),
                DateTimeFormatter.ofPattern("dd MMM uuuu", Locale.ENGLISH)
        );
        for (DateTimeFormatter format : formats) {
            try {
                return LocalDate.parse(value, format);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private Integer parseFlexibleInteger(String rawValue) {
        String value = blankToNull(rawValue);
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value.replaceAll("[^0-9]", ""));
        } catch (Exception ignored) {
            return null;
        }
    }

    private Double parseFlexibleDouble(String rawValue) {
        String value = blankToNull(rawValue);
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("[^0-9.\\-]", "");
        if (normalized.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(normalized);
        } catch (Exception ignored) {
            return null;
        }
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
        student.setCategory(blankToNull(request.getParameter("category")));
        student.setCategoryType(blankToNull(request.getParameter("categoryType")));
        student.setCasteCategory(blankToNull(request.getParameter("casteCategory")));
        student.setSubCaste(blankToNull(request.getParameter("subCaste")));
        student.setFatherOccupation(blankToNull(request.getParameter("fatherOccupation")));
        student.setFatherQualification(blankToNull(request.getParameter("fatherQualification")));
        student.setMotherQualification(blankToNull(request.getParameter("motherQualification")));
        student.setTotalFamilyMember(parseFlexibleInteger(request.getParameter("totalFamilyMember")));
        student.setFamilyAnnualIncome(parseFlexibleDouble(request.getParameter("familyAnnualIncome")));
        student.setDifferentlyAbled(parseBoolean(request.getParameter("differentlyAbled")));
        student.setSportsPerson(parseBoolean(request.getParameter("sportsPerson")));
        student.setSportsAchievement(blankToNull(request.getParameter("sportsAchievement")));
        student.setHobbies(blankToNull(request.getParameter("hobbies")));
        student.setUniversityPreAdmRegNo(blankToNull(request.getParameter("universityPreAdmRegNo")));
        student.setNoOfAttempt(parseFlexibleInteger(request.getParameter("noOfAttempt")));
        student.setInhouse(blankToNull(request.getParameter("inhouse")));
        student.setMediumOfInstruction(blankToNull(request.getParameter("mediumOfInstruction")));
        student.setSocialReservation(blankToNull(request.getParameter("socialReservation")));
        student.setAcademicBankOfCredits(blankToNull(request.getParameter("academicBankOfCredits")));
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
        if (!expected.equals(current)) {
            student.setRegistrationNo(expected);
            studentRepo.save(student);
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
            academicYear = currentAcademicYear();
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

    private String formatAcademicYear(Integer startYear) {
        if (startYear == null) {
            return null;
        }
        return startYear + "-" + String.valueOf(startYear + 1).substring(2);
    }

    private Boolean parseBoolean(String rawValue) {
        String value = blankToNull(rawValue);
        if (value == null) {
            return null;
        }
        return "true".equalsIgnoreCase(value)
                || "yes".equalsIgnoreCase(value)
                || "on".equalsIgnoreCase(value)
                || "1".equalsIgnoreCase(value);
    }

    private String normalizeBatchDisplayKey(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : normalized.replaceAll("\\s+", " ").strip().toUpperCase(Locale.ENGLISH);
    }

    private String normalizeGenderValue(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.equalsIgnoreCase("male")) return "Male";
        if (normalized.equalsIgnoreCase("female")) return "Female";
        if (normalized.equalsIgnoreCase("other")) return "Other";
        return null;
    }

    private String normalizeTeacherExperience(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            return null;
        }
        String key = normalized.replaceAll("[^0-9+]", "");
        return switch (key) {
            case "1" -> "0-1 Years";
            case "3" -> "1-3 Years";
            case "5" -> "3-5 Years";
            case "10" -> "5-10 Years";
            case "10+" -> "10+ Years";
            default -> normalized;
        };
    }

    private String buildFailedRowsCsv(List<String> headers, List<Map<String, Object>> failedRows) {
        StringBuilder csv = new StringBuilder();
        List<String> exportHeaders = new ArrayList<>(headers);
        exportHeaders.add("Error Reason");
        csv.append(exportHeaders.stream().map(this::csvValue).collect(Collectors.joining(","))).append("\n");
        for (Map<String, Object> row : failedRows) {
            @SuppressWarnings("unchecked")
            Map<String, String> values = (Map<String, String>) row.get("values");
            for (int i = 0; i < headers.size(); i++) {
                if (i > 0) {
                    csv.append(",");
                }
                csv.append(csvValue(values.get(headers.get(i))));
            }
            csv.append(",").append(csvValue((String) row.get("errorReason"))).append("\n");
        }
        return csv.toString();
    }

    private Map<String, Object> buildPreviewRow(int rowNumber, Map<String, String> values, String errorReason) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("rowNumber", rowNumber);
        row.put("values", values);
        row.put("valid", errorReason == null);
        row.put("errorReason", errorReason);
        return row;
    }

    private Map<String, Object> buildImportResponse(String entityName,
                                                    List<Map<String, Object>> previewRows,
                                                    int successCount,
                                                    List<String> headers) {
        long errorCount = previewRows.stream().filter(row -> !(Boolean) row.get("valid")).count();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("entity", entityName);
        response.put("rows", previewRows);
        response.put("successCount", successCount);
        response.put("readyCount", previewRows.stream().filter(row -> (Boolean) row.get("valid")).count());
        response.put("errorCount", errorCount);
        List<Map<String, Object>> failedRows = previewRows.stream()
                .filter(row -> !(Boolean) row.get("valid"))
                .collect(Collectors.toList());
        response.put("failedRowsCsv", failedRows.isEmpty() ? "" : buildFailedRowsCsv(headers, failedRows));
        return response;
    }

    private Course getAdminCourse(Admin admin, Long courseId) {
        if (admin == null || courseId == null) return null;
        return courseRepo.findById(courseId)
                .filter(course -> course.getAdmin() != null && course.getAdmin().getId().equals(admin.getId()))
                .orElse(null);
    }

    private Batch getAdminBatch(Admin admin, Long batchId) {
        if (admin == null || batchId == null) return null;
        return batchRepo.findById(batchId)
                .filter(batch -> batch.getAdmin() != null && batch.getAdmin().getId().equals(admin.getId()))
                .orElse(null);
    }

    private Integer parseSemesterNumber(String semester) {
        String digits = blankToNull(semester);
        if (digits == null) return null;
        digits = digits.replaceAll("[^0-9]", "");
        if (digits.isBlank()) return null;
        try {
            return Integer.parseInt(digits);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String formatSemester(Integer semester) {
        return semester == null ? null : "Sem " + semester;
    }

    private String normalizeSemesterLabel(String semester) {
        String value = blankToNull(semester);
        if (value == null) {
            return null;
        }
        Integer semesterNumber = parseSemesterNumber(value);
        if (semesterNumber != null) {
            return formatSemester(semesterNumber);
        }
        return value.strip();
    }

    private String normalizeAcademicYearValue(String academicYear) {
        String value = blankToNull(academicYear);
        if (value == null) {
            return null;
        }
        return value.replace("–", "-")
                .replace("—", "-")
                .replace("Ã¢â‚¬â€œ", "-")
                .replaceAll("\\s+", "")
                .strip();
    }

    private String normalizeSectionValue(String sectionName) {
        String value = blankToNull(sectionName);
        return value == null ? null : value.strip().toUpperCase(Locale.ENGLISH);
    }

    private Course resolveStudentEditCourse(Admin admin, String rawCourse) {
        String value = blankToNull(rawCourse);
        if (admin == null || value == null) {
            return null;
        }
        Course byCode = courseRepo.findByAdminAndCodeIgnoreCase(admin, value);
        if (byCode != null) {
            return byCode;
        }
        return courseRepo.findByAdminAndNameIgnoreCase(admin, value);
    }

    private Batch resolveStudentEditBatch(Admin admin, Course course, String academicYear, Batch currentBatch) {
        if (admin == null || course == null) {
            return null;
        }
        List<Batch> batches = batchRepo.findByAdminAndCourseOrderByDisplayNameAsc(admin, course);
        if (batches.isEmpty()) {
            return null;
        }

        String normalizedAcademicYear = normalizeAcademicYearValue(academicYear);
        if (normalizedAcademicYear != null) {
            Integer targetStart = parseAcademicYearStart(normalizedAcademicYear);
            for (Batch batch : batches) {
                if (batch == null || batch.getStartYear() == null) {
                    continue;
                }
                String batchAcademicYear = formatAcademicYear(batch.getStartYear());
                if (normalizedAcademicYear.equalsIgnoreCase(normalizeAcademicYearValue(batchAcademicYear))) {
                    return batch;
                }
                if (targetStart != null) {
                    Integer endYear = batch.getEndYear();
                    if (endYear != null && targetStart >= batch.getStartYear() && targetStart < endYear) {
                        return batch;
                    }
                    if (endYear == null && targetStart.equals(batch.getStartYear())) {
                        return batch;
                    }
                }
            }
        }

        if (currentBatch != null && currentBatch.getId() != null) {
            for (Batch batch : batches) {
                if (batch != null && currentBatch.getId().equals(batch.getId())) {
                    return batch;
                }
            }
        }

        return batches.get(0);
    }

    private String formatDuration(Course course) {
        if (course == null || course.getDurationYears() == null) {
            return null;
        }
        return course.getDurationYears() + (course.getDurationYears() == 1 ? " Year" : " Years");
    }

    private String inferCourseType(Course course) {
        if (course == null || course.getName() == null) {
            return null;
        }
        String value = course.getName().toLowerCase(Locale.ENGLISH);
        if (value.contains("master") || value.startsWith("m")) {
            return "Postgraduate";
        }
        if (value.contains("diploma")) {
            return "Diploma";
        }
        return "Undergraduate";
    }

    private boolean isValidWeekDay(String value) {
        return value != null && WEEK_DAYS.stream().anyMatch(day -> day.equalsIgnoreCase(value));
    }

    private boolean isValidTimeValue(String value) {
        return parseTimeOrNull(value) != null;
    }

    private ClassRoom resolveStudentClassRoom(Admin admin, Batch selectedBatch, String course, String academicYear, String semester, String sectionName) {
        if (admin == null) return null;

        Integer semesterNumber = parseSemesterNumber(semester);
        String normCourse = blankToNull(course);
        String normYear = blankToNull(academicYear);
        String normSection = blankToNull(sectionName);

        List<ClassRoom> candidates = classRepo.findByAdmin(admin).stream()
                .filter(classRoom -> {
                    if (selectedBatch != null && !batchMatchesClass(classRoom, selectedBatch)) {
                        return false;
                    }
                    if (normCourse != null && !courseMatchesStudentClassScope(classRoom, normCourse)) {
                        return false;
                    }
                    if (normYear != null
                            && (classRoom.getAcademicYear() == null || !academicYearMatches(normYear, classRoom.getAcademicYear()))) {
                        return false;
                    }
                    if (semesterNumber != null
                            && (classRoom.getSemester() == null || !semesterNumber.equals(classRoom.getSemester()))) {
                        return false;
                    }
                    if (normSection != null
                            && (classRoom.getSection() == null || !normSection.equalsIgnoreCase(classRoom.getSection()))) {
                        return false;
                    }
                    return true;
                })
                .toList();

        if (candidates.isEmpty()) {
            return null;
        }

        if (normSection != null) {
            for (ClassRoom classRoom : candidates) {
                if (classRoom.getSection() != null && normSection.equalsIgnoreCase(classRoom.getSection())) {
                    return classRoom;
                }
            }
        }

        if (semesterNumber != null) {
            for (ClassRoom classRoom : candidates) {
                if (classRoom.getSemester() != null && semesterNumber.equals(classRoom.getSemester())) {
                    return classRoom;
                }
            }
        }

        return candidates.get(0);
    }

    private boolean courseMatchesStudentClassScope(ClassRoom classRoom, String course) {
        String normalizedCourse = blankToNull(course);
        if (classRoom == null || normalizedCourse == null) {
            return true;
        }
        String classCourse = blankToNull(classRoom.getCourse());
        String classCourseCode = blankToNull(classRoom.getCourseCode());
        return normalizedCourse.equalsIgnoreCase(classCourse != null ? classCourse : "")
                || normalizedCourse.equalsIgnoreCase(classCourseCode != null ? classCourseCode : "");
    }

    private boolean classRoomMatchesStudentEditScope(ClassRoom classRoom,
                                                     Batch selectedBatch,
                                                     String course,
                                                     String academicYear,
                                                     String semester,
                                                     String sectionName) {
        if (classRoom == null) {
            return false;
        }

        Integer semesterNumber = parseSemesterNumber(semester);
        String normYear = blankToNull(academicYear);
        String normSection = blankToNull(sectionName);

        if (selectedBatch != null && !batchMatchesClass(classRoom, selectedBatch)) {
            return false;
        }
        if (!courseMatchesStudentClassScope(classRoom, course)) {
            return false;
        }
        if (normYear != null
                && (classRoom.getAcademicYear() == null || !academicYearMatches(normYear, classRoom.getAcademicYear()))) {
            return false;
        }
        if (semesterNumber != null
                && (classRoom.getSemester() == null || !semesterNumber.equals(classRoom.getSemester()))) {
            return false;
        }
        if (normSection != null
                && (classRoom.getSection() == null || !normSection.equalsIgnoreCase(classRoom.getSection()))) {
            return false;
        }
        return true;
    }

    private Subject getAdminSubject(Admin admin, Long subjectId) {
        if (admin == null || subjectId == null) return null;
        return subjectRepo.findById(subjectId)
                .filter(subject -> subject.getAdmin() != null && subject.getAdmin().getId().equals(admin.getId()))
                .orElse(null);
    }

    private SubjectMaster getAdminSubjectMaster(Admin admin, Long subjectMasterId) {
        if (admin == null || subjectMasterId == null) return null;
        return subjectMasterRepo.findById(subjectMasterId)
                .filter(subjectMaster -> subjectMaster.getAdmin() != null && subjectMaster.getAdmin().getId().equals(admin.getId()))
                .orElse(null);
    }

    private Teacher getAdminTeacher(Admin admin, Long teacherId) {
        if (admin == null || teacherId == null) return null;
        return teacherRepo.findById(teacherId)
                .filter(teacher -> teacher.getAdmin() != null && teacher.getAdmin().getId().equals(admin.getId()))
                .orElse(null);
    }

    private String normalizeSubjectCategory(String category) {
        String value = blankToNull(category);
        if (value == null) return "COMPULSORY (CORE)";
        String normalized = value.strip().toUpperCase(Locale.ENGLISH);
        if (normalized.contains("LAB") || normalized.contains("PRACTICAL")) return "LAB / PRACTICAL";
        if (normalized.contains("ELECTIVE")) return "ELECTIVE";
        if (normalized.contains("PROJECT")) return "PROJECT";
        return "COMPULSORY (CORE)";
    }

    private boolean isLabCategory(String category) {
        return normalizeSubjectCategory(category).equals("LAB / PRACTICAL");
    }

    private List<String> subjectRegistryYearLabels(Course course) {
        int phases = Math.max(1, (int) Math.ceil(semesterOptions(course).size() / 2.0));
        List<String> labels = new ArrayList<>();
        for (int index = 0; index < phases; index++) {
            labels.add(switch (index) {
                case 0 -> "FY";
                case 1 -> "SY";
                case 2 -> "TY";
                case 3 -> "LY";
                default -> "Y" + (index + 1);
            });
        }
        return labels;
    }

    private String deriveYearLabelForSemester(Integer semesterNumber) {
        if (semesterNumber == null || semesterNumber < 1) {
            return null;
        }
        int index = (semesterNumber - 1) / 2;
        return switch (index) {
            case 0 -> "FY";
            case 1 -> "SY";
            case 2 -> "TY";
            case 3 -> "LY";
            default -> "Y" + (index + 1);
        };
    }

    private List<Integer> semesterOptions(Course course) {
        int total = course != null && course.getTotalSemesters() != null && course.getTotalSemesters() > 0
                ? course.getTotalSemesters()
                : 6;
        List<Integer> values = new ArrayList<>();
        for (int semester = 1; semester <= total; semester++) {
            values.add(semester);
        }
        return values;
    }

    private List<Batch> activeBatchesForCourse(Admin admin, Course course) {
        if (admin == null || course == null) return List.of();
        return batchRepo.findByAdminAndCourseOrderByDisplayNameAsc(admin, course);
    }

    private int copySubjectMasterToBatch(Admin admin, Batch batch, SubjectMaster subjectMaster) {
        if (admin == null || batch == null || subjectMaster == null) return 0;
        Subject existing = subjectRepo.findByAdminAndBatchRefAndSemesterAndCodeIgnoreCase(
                admin, batch, subjectMaster.getSemesterNumber(), subjectMaster.getCode());
        boolean isNew = existing == null;

        Subject subject = existing != null ? existing : new Subject();
        subject.setAdmin(admin);
        subject.setCourseRef(subjectMaster.getCourse());
        subject.setBatchRef(batch);
        subject.setSubjectMasterRef(subjectMaster);
        subject.setName(subjectMaster.getName());
        subject.setCode(subjectMaster.getCode());
        subject.setSemester(subjectMaster.getSemesterNumber());
        subject.setCredits(subjectMaster.getCredits() != null ? subjectMaster.getCredits() : 4.0);
        subject.setCourseCategory(normalizeSubjectCategory(subjectMaster.getCategory()));
        subject.setIsOverride(Boolean.FALSE);
        subject.setStatus(blankToNull(subjectMaster.getStatus()) != null ? subjectMaster.getStatus() : "active");
        subject.setTerm(subjectMaster.getSemesterNumber() + " SEMESTER");
        subject.setCycle(blankToNull(subjectMaster.getYearLabel()) != null ? subjectMaster.getYearLabel() : currentAcademicYear());
        subjectRepo.save(subject);
        return isNew ? 1 : 0;
    }

    private int syncSubjectRegistryToExistingBatches(Admin admin, Course course, SubjectMaster subjectMaster) {
        if (admin == null || course == null || subjectMaster == null) return 0;
        int created = 0;
        for (Batch batch : activeBatchesForCourse(admin, course)) {
            created += copySubjectMasterToBatch(admin, batch, subjectMaster);
        }
        return created;
    }

    private int copySubjectMasterToExistingBatches(Admin admin, Course course, SubjectMaster subjectMaster) {
        return syncSubjectRegistryToExistingBatches(admin, course, subjectMaster);
    }

    private int copyAllSubjectMastersToBatch(Admin admin, Batch batch) {
        if (admin == null || batch == null || batch.getCourse() == null) return 0;
        int copied = 0;
        for (SubjectMaster subjectMaster : subjectMasterRepo.findByAdminAndCourseAndStatusIgnoreCaseOrderBySemesterNumberAscNameAsc(
                admin, batch.getCourse(), "active")) {
            copied += copySubjectMasterToBatch(admin, batch, subjectMaster);
        }
        return copied;
    }

    private String resolveTimetableEntryType(Admin admin, Batch batch, Course course, Integer semester, String subject) {
        if (blankToNull(subject) == null) {
            return "LECTURE";
        }
        String normalizedSubject = subject.strip();
        if (batch != null) {
            for (Subject batchSubject : subjectRepo.findByAdminAndBatchRefOrderBySemesterAscNameAsc(admin, batch)) {
                boolean sameSemester = semester == null || semester.equals(batchSubject.getSemester());
                boolean sameSubject = normalizedSubject.equalsIgnoreCase(blankToNull(batchSubject.getName()))
                        || normalizedSubject.equalsIgnoreCase(blankToNull(batchSubject.getCode()));
                if (sameSemester && sameSubject && isLabCategory(batchSubject.getCourseCategory())) {
                    return "LAB";
                }
            }
        }
        if (course != null) {
            for (SubjectMaster subjectMaster : subjectMasterRepo.findByAdminAndCourseOrderBySemesterNumberAscNameAsc(admin, course)) {
                boolean sameSemester = semester == null || semester.equals(subjectMaster.getSemesterNumber());
                boolean sameSubject = normalizedSubject.equalsIgnoreCase(blankToNull(subjectMaster.getName()))
                        || normalizedSubject.equalsIgnoreCase(blankToNull(subjectMaster.getCode()));
                if (sameSemester && sameSubject && isLabCategory(subjectMaster.getCategory())) {
                    return "LAB";
                }
            }
        }
        return "LECTURE";
    }

    private boolean teacherCanTeachScopedSubject(Teacher teacher, List<Subject> scopedSubjects, String subject) {
        if (teacher == null || blankToNull(subject) == null || scopedSubjects == null || scopedSubjects.isEmpty()) {
            return false;
        }

        String normalizedSubject = subject.strip();
        return scopedSubjects.stream()
                .anyMatch(row -> normalizedSubject.equalsIgnoreCase(blankToNull(row.getName()))
                        || normalizedSubject.equalsIgnoreCase(blankToNull(row.getCode())));
    }

    private Subject findScopedSubject(List<Subject> scopedSubjects, String subject) {
        if (scopedSubjects == null || scopedSubjects.isEmpty() || blankToNull(subject) == null) {
            return null;
        }

        String normalizedSubject = subject.strip();
        return scopedSubjects.stream()
                .filter(row -> normalizedSubject.equalsIgnoreCase(blankToNull(row.getName()))
                        || normalizedSubject.equalsIgnoreCase(blankToNull(row.getCode())))
                .findFirst()
                .orElse(null);
    }

    private Teacher findFallbackTeacherForSubject(Admin admin, String subject, List<Teacher> teachers) {
        if (admin == null || blankToNull(subject) == null) {
            return null;
        }

        String normalizedSubject = subject.strip().toLowerCase(Locale.ENGLISH);
        List<Teacher> source = teachers != null ? teachers : activeTeachersForAdmin(admin);

        return source.stream()
                .filter(teacher -> blankToNull(teacher.getSubject()) != null)
                .filter(teacher -> {
                    String teacherSubject = teacher.getSubject().strip().toLowerCase(Locale.ENGLISH);
                    return normalizedSubject.equals(teacherSubject)
                            || normalizedSubject.contains(teacherSubject)
                            || teacherSubject.contains(normalizedSubject);
                })
                .findFirst()
                .orElse(null);
    }

    private Teacher resolveTimetableRowTeacher(Admin admin, String teacherToken, List<Teacher> teachers) {
        String normalized = blankToNull(teacherToken);
        if (admin == null || normalized == null) {
            return null;
        }

        List<Teacher> source = teachers != null ? teachers : activeTeachersForAdmin(admin);
        try {
            Long id = Long.valueOf(normalized);
            return source.stream()
                    .filter(teacher -> teacher.getId() != null && teacher.getId().equals(id))
                    .findFirst()
                    .orElse(null);
        } catch (NumberFormatException ignored) {
            String query = normalized.toLowerCase(Locale.ENGLISH);
            return source.stream()
                    .filter(teacher -> query.equalsIgnoreCase(blankToNull(teacher.getName()))
                            || query.equalsIgnoreCase(blankToNull(teacher.getEmail()))
                            || query.equalsIgnoreCase(blankToNull(teacher.getEmployeeId())))
                    .findFirst()
                    .orElse(null);
        }
    }

    private static final List<String> WEEK_DAYS = List.of(
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday");

    public record ClassOptionView(
            Long id,
            String name,
            String course,
            String academicYear,
            Integer semester,
            String section,
            Double totalFees,
            String batch,
            String batchName,
            String status
    ) {}

    public record TimetableSubjectOptionView(
            Long id,
            String name,
            String code,
            String courseCategory,
            Long teacherId,
            String teacherName,
            int durationHours,
            boolean project
    ) {}

    public record CourseDistributionView(
            String courseName,
            long count,
            int percentage,
            String colorClass
    ) {}

    private static class TimetableSlotView {
        private final String key;
        private final String label;
        private final LocalTime start;
        private final LocalTime end;

        private TimetableSlotView(LocalTime start, LocalTime end) {
            this.start = start;
            this.end = end;
            this.key = start + "|" + end;
            this.label = start + " - " + end;
        }

        public String getKey() { return key; }
        public String getLabel() { return label; }
        public LocalTime getStart() { return start; }
        public LocalTime getEnd() { return end; }
    }

    private static class WeekGridCellView {
        private final String day;
        private final String slotKey;
        private final String slotLabel;
        private final LocalTime start;
        private final LocalTime end;
        private final List<Timetable> entries;

        private WeekGridCellView(String day, TimetableSlotView slot, List<Timetable> entries) {
            this.day = day;
            this.slotKey = slot.getKey();
            this.slotLabel = slot.getLabel();
            this.start = slot.getStart();
            this.end = slot.getEnd();
            this.entries = entries;
        }

        public String getDay() { return day; }
        public String getSlotKey() { return slotKey; }
        public String getSlotLabel() { return slotLabel; }
        public LocalTime getStart() { return start; }
        public LocalTime getEnd() { return end; }
        public List<Timetable> getEntries() { return entries; }
        public boolean isEmpty() { return entries == null || entries.isEmpty(); }
    }

    private static class WeekGridRowView {
        private final String day;
        private final List<WeekGridCellView> cells;

        private WeekGridRowView(String day, List<WeekGridCellView> cells) {
            this.day = day;
            this.cells = cells;
        }

        public String getDay() { return day; }
        public List<WeekGridCellView> getCells() { return cells; }
    }

    private static class DayColumnView {
        private final String day;

        private DayColumnView(String day) {
            this.day = day;
        }

        public String getDay() { return day; }
    }

    private static class TimeGridRowView {
        private final TimetableSlotView slot;
        private final List<WeekGridCellView> cells;

        private TimeGridRowView(TimetableSlotView slot, List<WeekGridCellView> cells) {
            this.slot = slot;
            this.cells = cells;
        }

        public TimetableSlotView getSlot() { return slot; }
        public List<WeekGridCellView> getCells() { return cells; }
    }

    private LocalTime parseTimeOrNull(String value) {
        try {
            return value == null || value.isBlank() ? null : LocalTime.parse(value.strip());
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean overlaps(LocalTime start, LocalTime end, Timetable entry) {
        LocalTime existingStart = parseTimeOrNull(entry.getStartTime());
        LocalTime existingEnd = parseTimeOrNull(entry.getEndTime());
        if (existingStart == null || existingEnd == null) return false;
        return start.isBefore(existingEnd) && end.isAfter(existingStart);
    }

    private List<TimetableSlotView> buildTimeSlots(List<Timetable> entries) {
        Map<String, TimetableSlotView> slots = new LinkedHashMap<>();
        String[][] defaultSlots = {
                {"09:00", "10:00"},
                {"10:00", "11:00"},
                {"11:00", "12:00"},
                {"12:00", "13:00"},
                {"13:00", "14:00"},
                {"14:00", "15:00"},
                {"15:00", "16:00"},
                {"16:00", "17:00"}
        };
        for (String[] defaultSlot : defaultSlots) {
            LocalTime start = parseTimeOrNull(defaultSlot[0]);
            LocalTime end = parseTimeOrNull(defaultSlot[1]);
            if (start == null || end == null) continue;
            TimetableSlotView slot = new TimetableSlotView(start, end);
            slots.putIfAbsent(slot.getKey(), slot);
        }
        for (Timetable entry : entries) {
            LocalTime start = parseTimeOrNull(entry.getStartTime());
            LocalTime end = parseTimeOrNull(entry.getEndTime());
            if (start == null || end == null) continue;
            boolean coveredByExistingSlots = slots.values().stream()
                    .anyMatch(slot -> start.isBefore(slot.getEnd()) && end.isAfter(slot.getStart()));
            if (coveredByExistingSlots) {
                continue;
            }
            TimetableSlotView slot = new TimetableSlotView(start, end);
            slots.putIfAbsent(slot.getKey(), slot);
        }
        return slots.values().stream()
                .sorted((a, b) -> {
                    int byStart = a.getStart().compareTo(b.getStart());
                    return byStart != 0 ? byStart : a.getEnd().compareTo(b.getEnd());
                })
                .collect(Collectors.toList());
    }

    private Map<String, Map<String, List<Timetable>>> buildWeeklyGrid(List<Timetable> entries, List<TimetableSlotView> slots) {
        Map<String, Map<String, List<Timetable>>> grid = new LinkedHashMap<>();
        for (String day : WEEK_DAYS) {
            Map<String, List<Timetable>> row = new LinkedHashMap<>();
            for (TimetableSlotView slot : slots) {
                row.put(slot.getKey(), new ArrayList<>());
            }
            grid.put(day, row);
        }

        for (Timetable entry : entries) {
            if (entry.getDay() == null) continue;
            LocalTime start = parseTimeOrNull(entry.getStartTime());
            LocalTime end = parseTimeOrNull(entry.getEndTime());
            if (start == null || end == null) continue;

            Map<String, List<Timetable>> row = grid.get(entry.getDay());
            if (row != null) {
                for (TimetableSlotView slot : slots) {
                    if (start.isBefore(slot.getEnd()) && end.isAfter(slot.getStart())) {
                        row.get(slot.getKey()).add(entry);
                    }
                }
            }
        }

        return grid;
    }

    private List<WeekGridRowView> buildWeekRows(List<Timetable> entries, List<TimetableSlotView> slots) {
        Map<String, Map<String, List<Timetable>>> grid = buildWeeklyGrid(entries, slots);
        List<WeekGridRowView> rows = new ArrayList<>();
        for (String day : WEEK_DAYS) {
            List<WeekGridCellView> cells = new ArrayList<>();
            Map<String, List<Timetable>> row = grid.get(day);
            for (TimetableSlotView slot : slots) {
                List<Timetable> cellEntries = row != null ? row.getOrDefault(slot.getKey(), List.of()) : List.of();
                cells.add(new WeekGridCellView(day, slot, cellEntries));
            }
            rows.add(new WeekGridRowView(day, cells));
        }
        return rows;
    }

    private List<TimeGridRowView> buildTimeRows(List<Timetable> entries, List<TimetableSlotView> slots) {
        Map<String, Map<String, List<Timetable>>> grid = buildWeeklyGrid(entries, slots);
        List<TimeGridRowView> rows = new ArrayList<>();
        for (TimetableSlotView slot : slots) {
            List<WeekGridCellView> cells = new ArrayList<>();
            for (String day : WEEK_DAYS) {
                Map<String, List<Timetable>> row = grid.get(day);
                List<Timetable> cellEntries = row != null ? row.getOrDefault(slot.getKey(), List.of()) : List.of();
                cells.add(new WeekGridCellView(day, slot, cellEntries));
            }
            rows.add(new TimeGridRowView(slot, cells));
        }
        return rows;
    }

    private String normalizeScopeKey(String value) {
        if (value == null || value.isBlank()) return "";
        return value.trim().toUpperCase(Locale.ENGLISH).replaceAll("[^A-Z0-9]", "");
    }

    private boolean batchMatchesClass(ClassRoom classRoom, Batch batch) {
        if (classRoom == null || batch == null) return false;

        String classBatchName = normalizeScopeKey(classRoom.getBatchName());
        String batchDisplayName = normalizeScopeKey(batch.getDisplayName());
        if (!classBatchName.isBlank() && classBatchName.equals(batchDisplayName)) {
            return true;
        }

        Integer classStart = classRoom.getBatchStartYear();
        Integer classEnd = classRoom.getBatchEndYear();
        if (classStart != null && classEnd != null && classStart.equals(batch.getStartYear()) && classEnd.equals(batch.getEndYear())) {
            return true;
        }

        String classCourse = normalizeScopeKey(classRoom.getCourse());
        String batchCourseCode = batch.getCourse() != null ? normalizeScopeKey(batch.getCourse().getCode()) : "";
        String batchCourseName = batch.getCourse() != null ? normalizeScopeKey(batch.getCourse().getName()) : "";
        return !classCourse.isBlank() && (classCourse.equals(batchCourseCode) || classCourse.equals(batchCourseName));
    }

    private boolean classMatchesTimetableScope(ClassRoom classRoom, Batch batch, String course, String academicYear, Integer semester) {
        if (classRoom == null) return false;

        if (batch != null && !batchMatchesClass(classRoom, batch)) {
            return false;
        }

        if (course != null && !course.isBlank()) {
            if (!courseMatchesStudentClassScope(classRoom, course)) {
                return false;
            }
        }

        if (academicYear != null && !academicYear.isBlank()) {
            String classYear = classRoom.getAcademicYear();
            if (classYear == null || !academicYearMatches(classYear, academicYear)) {
                return false;
            }
        }

        if (semester != null) {
            Integer classSemester = classRoom.getSemester();
            if (classSemester == null || !classSemester.equals(semester)) {
                return false;
            }
        }

        return true;
    }

    private List<ClassRoom> timetableClassesForScope(Admin admin, Long batchId, String course, String academicYear, Integer semester) {
        Batch selectedBatch = getAdminBatch(admin, batchId);
        return classRepo.findByAdmin(admin).stream()
                .filter(classRoom -> classMatchesTimetableScope(classRoom, selectedBatch, course, academicYear, semester))
                .collect(Collectors.toList());
    }

    private List<Timetable> timetableEntriesForView(Admin admin, Long classId, Long batchId, String course, String academicYear, Integer semester) {
        Batch selectedBatch = getAdminBatch(admin, batchId);
        return timetableRepo.findByAdmin(admin).stream()
                .filter(entry -> {
                    ClassRoom classRoom = entry.getClassRoom();
                    if (classRoom == null) {
                        if (classId != null || batchId != null || (course != null && !course.isBlank()) || semester != null) {
                            return false;
                        }
                        String entryYear = blankToNull(entry.getAcademicYear());
                        if (academicYear != null && !academicYear.isBlank()) {
                            return entryYear != null && entryYear.equalsIgnoreCase(academicYear);
                        }
                        return true;
                    }
                    if (classId != null && !classId.equals(classRoom.getId())) return false;
                    return classMatchesTimetableScope(classRoom, selectedBatch, course, academicYear, semester);
                })
                .sorted((a, b) -> {
                    int dayCompare = Integer.compare(
                            WEEK_DAYS.indexOf(a.getDay() == null ? "" : a.getDay()),
                            WEEK_DAYS.indexOf(b.getDay() == null ? "" : b.getDay()));
                    if (dayCompare != 0) return dayCompare;
                    LocalTime aStart = parseTimeOrNull(a.getStartTime());
                    LocalTime bStart = parseTimeOrNull(b.getStartTime());
                    if (aStart == null && bStart == null) return 0;
                    if (aStart == null) return 1;
                    if (bStart == null) return -1;
                    int startCompare = aStart.compareTo(bStart);
                    if (startCompare != 0) return startCompare;
                    LocalTime aEnd = parseTimeOrNull(a.getEndTime());
                    LocalTime bEnd = parseTimeOrNull(b.getEndTime());
                    if (aEnd == null && bEnd == null) return 0;
                    if (aEnd == null) return 1;
                    if (bEnd == null) return -1;
                    return aEnd.compareTo(bEnd);
                })
                .collect(Collectors.toList());
    }

    private String resolveTimetableSubjectLabel(Timetable entry) {
        if (entry == null) {
            return "Subject";
        }
        String subject = blankToNull(entry.getSubject());
        if (subject != null) {
            return subject;
        }
        Subject subjectRef = entry.getSubjectRef();
        if (subjectRef != null) {
            String subjectName = blankToNull(subjectRef.getName());
            if (subjectName != null) {
                return subjectName;
            }
            String subjectCode = blankToNull(subjectRef.getCode());
            if (subjectCode != null) {
                return subjectCode;
            }
        }
        return "Subject";
    }

    private String buildClassIdentityLabel(ClassRoom classRoom) {
        if (classRoom == null) {
            return null;
        }

        List<String> parts = new ArrayList<>();
        if (blankToNull(classRoom.getCourse()) != null) {
            parts.add(classRoom.getCourse().strip());
        }
        if (blankToNull(classRoom.getBatchName()) != null) {
            parts.add(classRoom.getBatchName().strip());
        } else if (classRoom.getBatchStartYear() != null && classRoom.getBatchEndYear() != null) {
            parts.add(classRoom.getBatchStartYear() + "-" + classRoom.getBatchEndYear());
        }
        if (blankToNull(classRoom.getYear()) != null) {
            parts.add(classRoom.getYear().strip());
        } else if (blankToNull(classRoom.getAcademicYear()) != null) {
            parts.add(classRoom.getAcademicYear().strip());
        }
        if (classRoom.getSemester() != null) {
            parts.add("Sem " + classRoom.getSemester());
        }
        if (blankToNull(classRoom.getSection()) != null) {
            parts.add(classRoom.getSection().strip());
        }
        if (parts.isEmpty() && blankToNull(classRoom.getName()) != null) {
            parts.add(classRoom.getName().strip());
        }
        return String.join(" / ", parts);
    }

    private String buildTimetableScopeLabel(ClassRoom classRoom, Batch selectedBatch, Course scopedCourse, String academicYear, Integer semester) {
        if (classRoom != null) {
            String label = buildClassIdentityLabel(classRoom);
            if (label != null && !label.isBlank()) {
                return label;
            }
        }

        List<String> parts = new ArrayList<>();
        if (scopedCourse != null) {
            String courseLabel = blankToNull(scopedCourse.getCode());
            if (courseLabel == null) {
                courseLabel = blankToNull(scopedCourse.getName());
            }
            if (courseLabel != null) {
                parts.add(courseLabel.strip());
            }
        }
        if (selectedBatch != null) {
            String batchLabel = blankToNull(selectedBatch.getDisplayName());
            if (batchLabel == null && selectedBatch.getStartYear() != null && selectedBatch.getEndYear() != null) {
                batchLabel = selectedBatch.getStartYear() + "-" + selectedBatch.getEndYear();
            }
            if (batchLabel != null) {
                parts.add(batchLabel.strip());
            }
        }
        if (blankToNull(academicYear) != null) {
            parts.add(academicYear.strip());
        }
        if (semester != null) {
            parts.add("Sem " + semester);
        }
        parts.add("All Sections");
        return String.join(" / ", parts);
    }

    private void populateTimetableGridModel(Model model, Admin admin, List<Timetable> timetable, Long classId, Long batchId, String course, String academicYear, Integer semester) {
        List<ClassRoom> allClasses = classRepo.findByAdmin(admin);
        List<Teacher> allTeachers = teacherRepo.findByAdmin(admin);
        Batch selectedBatch = getAdminBatch(admin, batchId);
        Course scopedCourse = resolveTimetableScopeCourse(admin, selectedBatch, course);
        ClassRoom selectedClass = classId != null ? classRepo.findById(classId).orElse(null) : null;
        if (selectedClass != null && (selectedClass.getAdmin() == null || !selectedClass.getAdmin().getId().equals(admin.getId()))) {
            selectedClass = null;
        }
        String effectiveCourse = scopedCourse != null && blankToNull(scopedCourse.getCode()) != null
                ? scopedCourse.getCode().strip()
                : course;
        String effectiveAcademicYear = blankToNull(academicYear);
        if (effectiveAcademicYear == null && selectedBatch != null && selectedBatch.getStartYear() != null) {
            effectiveAcademicYear = selectedBatch.getStartYear() + "-" + String.valueOf(selectedBatch.getStartYear() + 1).substring(2);
        }
        String selectedScopeLabel = buildTimetableScopeLabel(selectedClass, selectedBatch, scopedCourse, effectiveAcademicYear, semester);
        String selectedClassLabel = buildClassIdentityLabel(selectedClass);
        List<ClassRoom> scopedClasses = timetableClassesForScope(admin, batchId, effectiveCourse, academicYear, semester);
        List<TimetableSlotView> timeSlots = buildTimeSlots(timetable);
        Map<String, Map<String, List<Timetable>>> weeklyGrid = buildWeeklyGrid(timetable, timeSlots);
        List<WeekGridRowView> weekRows = buildWeekRows(timetable, timeSlots);
        List<TimeGridRowView> timeRows = buildTimeRows(timetable, timeSlots);
        List<DayColumnView> dayColumns = WEEK_DAYS.stream().map(DayColumnView::new).collect(Collectors.toList());
        List<String> courses = courseRepo.findByAdminOrderByCodeAsc(admin).stream()
                .map(courseRow -> blankToNull(courseRow.getCode()) != null ? courseRow.getCode().strip() : blankToNull(courseRow.getName()))
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());
        LinkedHashSet<String> academicYears = allClasses.stream()
                .map(ClassRoom::getAcademicYear)
                .filter(y -> y != null && !y.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (selectedBatch != null && selectedBatch.getStartYear() != null && selectedBatch.getEndYear() != null) {
            for (int year = selectedBatch.getStartYear(); year < selectedBatch.getEndYear(); year++) {
                academicYears.add(year + "-" + String.valueOf(year + 1).substring(2));
            }
        }
        if (effectiveAcademicYear != null) {
            academicYears.add(effectiveAcademicYear);
        }
        if (academicYears.isEmpty()) {
            academicYears.addAll(academicYearsFromBatches(admin));
        }
        List<String> academicYearOptions = academicYears.stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());

        model.addAttribute("timetable", timetable);
        model.addAttribute("timeSlots", timeSlots);
        model.addAttribute("weeklyGrid", weeklyGrid);
        model.addAttribute("weekRows", weekRows);
        model.addAttribute("timeRows", timeRows);
        model.addAttribute("weeklyTimetableRows", weekRows);
        model.addAttribute("dayColumns", dayColumns);
        model.addAttribute("weekDays", WEEK_DAYS);
        model.addAttribute("workingDayNames", WEEK_DAYS);
        model.addAttribute("classes", scopedClasses);
        model.addAttribute("allClasses", allClasses);
        model.addAttribute("scopeClasses", scopedClasses);
        model.addAttribute("selectedBatch", selectedBatch);
        model.addAttribute("selectedClass", selectedClass);
        model.addAttribute("batches", batchRepo.findByAdminOrderByDisplayNameAsc(admin));
        model.addAttribute("semesters", List.of("Sem 1", "Sem 2", "Sem 3", "Sem 4", "Sem 5", "Sem 6"));
        model.addAttribute("courses", courses);
        model.addAttribute("academicYears", academicYearOptions);
        model.addAttribute("selectedClassId", classId);
        model.addAttribute("selectedBatchId", batchId);
        model.addAttribute("selectedCourse", effectiveCourse);
        model.addAttribute("selectedAcademicYear", effectiveAcademicYear);
        model.addAttribute("selectedSemester", semester);
        model.addAttribute("selectedScopeLabel", selectedScopeLabel);
        model.addAttribute("selectedClassLabel", selectedClassLabel);
        model.addAttribute("selectedBatchLabel", selectedBatch != null ? selectedBatch.getDisplayName() : "All Batches");
        model.addAttribute("selectedCourseLabel", scopedCourse != null
                ? defaultText(scopedCourse.getName(), scopedCourse.getCode())
                : defaultText(course, "All Courses"));
        model.addAttribute("selectedAcademicYearLabel", defaultText(effectiveAcademicYear, "All Years"));
        model.addAttribute("selectedSemesterLabel", semester != null ? "Sem " + semester : "All Semesters");
        model.addAttribute("totalEntries", timetable.size());
        model.addAttribute("totalDays", WEEK_DAYS.size());
        model.addAttribute("totalSlots", timeSlots.size());
        model.addAttribute("totalSubjects", timetable.stream()
                .map(this::resolveTimetableSubjectLabel)
                .filter(s -> s != null && !s.isBlank())
                .map(String::toLowerCase)
                .distinct()
                .count());
        long registeredSubjectsCount = 0;
        for (Course courseRow : courseRepo.findByAdminOrderByCodeAsc(admin)) {
            registeredSubjectsCount += subjectMasterRepo.findByAdminAndCourse(admin, courseRow).size();
        }
        model.addAttribute("registeredSubjectsCount", registeredSubjectsCount);
        model.addAttribute("totalTeachers", allTeachers.size());
        model.addAttribute("totalClasses", scopedClasses.size());
    }

    // ── Helper: total fees by course ──
    private String normalizeCourseKey(String course) {
        return course == null ? null : course.strip().toUpperCase(Locale.ROOT);
    }

    private LinkedHashMap<String, Course> buildCourseCatalog(List<Course> courses) {
        LinkedHashMap<String, Course> catalog = new LinkedHashMap<>();
        if (courses == null) return catalog;
        for (Course course : courses) {
            if (course == null) continue;
            String codeKey = normalizeCourseKey(course.getCode());
            if (codeKey != null) {
                catalog.putIfAbsent(codeKey, course);
            }
            String nameKey = normalizeCourseKey(course.getName());
            if (nameKey != null) {
                catalog.putIfAbsent(nameKey, course);
            }
        }
        return catalog;
    }

    private String resolveCourseCode(String rawCourse, Map<String, Course> catalog) {
        String normalized = normalizeCourseKey(rawCourse);
        if (normalized == null || catalog == null || catalog.isEmpty()) return null;
        Course match = catalog.get(normalized);
        return match != null && blankToNull(match.getCode()) != null ? match.getCode().strip() : null;
    }

    private String resolveCourseLabel(String rawCourse, Map<String, Course> catalog) {
        String code = resolveCourseCode(rawCourse, catalog);
        return code != null ? code : blankToNull(rawCourse);
    }

    private Course resolveAdminCourseByInput(Admin admin, String rawCourse) {
        if (admin == null) return null;
        String normalized = blankToNull(rawCourse);
        if (normalized == null) return null;

        List<Course> courses = courseRepo.findByAdminOrderByCodeAsc(admin);
        LinkedHashMap<String, Course> catalog = buildCourseCatalog(courses);
        Course matched = catalog.get(normalizeCourseKey(normalized));
        if (matched != null) {
            return matched;
        }

        return courses.stream()
                .filter(course -> normalized.equalsIgnoreCase(blankToNull(course.getCode()))
                        || normalized.equalsIgnoreCase(blankToNull(course.getName())))
                .findFirst()
                .orElse(null);
    }

    private String degreeHintForCourse(Course course) {
        String type = course != null ? blankToNull(course.getCourseType()) : null;
        if (type == null) return "";

        String normalized = type.strip().toLowerCase(Locale.ENGLISH);
        if (normalized.contains("post")) return "PG";
        if (normalized.contains("under")) return "UG";
        if (normalized.contains("diploma")) return "Diploma";
        if (normalized.contains("certificate")) return "Certificate";
        return "";
    }

    private List<Map<String, Object>> buildStudentCourseSummaries(List<Course> courses, List<Student> students) {
        LinkedHashMap<String, Course> catalog = buildCourseCatalog(courses);
        LinkedHashMap<String, Long> counts = new LinkedHashMap<>();
        if (courses != null) {
            for (Course course : courses) {
                if (course != null && blankToNull(course.getCode()) != null) {
                    counts.putIfAbsent(course.getCode().strip(), 0L);
                }
            }
        }
        if (students != null) {
            for (Student student : students) {
                String courseCode = resolveCourseCode(student.getCourse(), catalog);
                if (courseCode != null) {
                    counts.merge(courseCode, 1L, Long::sum);
                }
            }
        }

        List<String> iconClasses = List.of("fa-graduation-cap", "fa-laptop-code", "fa-book-open-reader", "fa-flask", "fa-building-columns");
        List<Map<String, Object>> summaries = new ArrayList<>();
        int index = 0;
        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            Map<String, Object> row = new HashMap<>();
            row.put("code", entry.getKey());
            row.put("count", entry.getValue());
            row.put("iconClass", iconClasses.get(index % iconClasses.size()));
            row.put("trendClass", index == 0 ? "trend-up" : "");
            summaries.add(row);
            index++;
        }
        return summaries;
    }

    private Map<String, Number> buildCourseMetricMap(List<Course> courses,
                                                     List<String> rawLabels,
                                                     List<? extends Number> rawValues) {
        LinkedHashMap<String, Course> catalog = buildCourseCatalog(courses);
        LinkedHashMap<String, Number> metrics = new LinkedHashMap<>();
        if (courses != null) {
            for (Course course : courses) {
                if (course != null && blankToNull(course.getCode()) != null) {
                    metrics.putIfAbsent(course.getCode().strip(), 0);
                }
            }
        }
        if (rawLabels == null || rawValues == null) return metrics;

        for (int i = 0; i < Math.min(rawLabels.size(), rawValues.size()); i++) {
            String label = resolveCourseLabel(rawLabels.get(i), catalog);
            Number value = rawValues.get(i);
            if (label == null || value == null) continue;
            Number existing = metrics.get(label);
            if (existing == null) {
                metrics.put(label, value);
            } else if (existing instanceof Double || value instanceof Double || existing instanceof Float || value instanceof Float) {
                metrics.put(label, existing.doubleValue() + value.doubleValue());
            } else {
                metrics.put(label, existing.longValue() + value.longValue());
            }
        }
        return metrics;
    }

    private List<String> chartLabels(Map<String, ? extends Number> values) {
        return values.entrySet().stream()
                .filter(entry -> blankToNull(entry.getKey()) != null)
                .filter(entry -> entry.getValue() != null && entry.getValue().doubleValue() > 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private List<Number> chartValues(Map<String, ? extends Number> values) {
        return values.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().doubleValue() > 0)
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
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

    private record StudentFeeResolution(
            boolean assigned,
            Double totalAmount
    ) {}

    private List<Fees> feeRulesForAdmin(Admin admin) {
        return feesRepo.findByAdmin(admin).stream()
                .sorted(Comparator.comparing((Fees fee) -> fee.getCourse() != null ? fee.getCourse() : "", String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(fee -> fee.getAcademicYear() != null ? fee.getAcademicYear() : "", String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(fee -> fee.getSemester() != null ? fee.getSemester() : "", String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    private Fees findFeeRule(Admin admin, String course, Long batchId, String batchName, String academicYear, String semester) {
        if (admin == null || course == null || course.isBlank()) return null;

        String normCourse = course.strip();
        Long normBatchId = batchId;
        String normBatch = blankToNull(batchName);
        String normYear = blankToNull(academicYear);
        String normSem = normalizeSemesterKey(semester);
        List<Fees> rules = feesRepo.findByAdmin(admin);

        if (normBatchId != null) {
            for (Fees row : rules) {
                if (!normCourse.equalsIgnoreCase(blankToNull(row.getCourse()))) continue;
                if (row.getBatch() == null || !normBatchId.equals(row.getBatch().getId())) continue;
                if (normYear != null && !academicYearMatches(row.getAcademicYear(), normYear)) continue;
                if (isYearWiseFeeRule(row)) return row;
            }
            for (Fees row : rules) {
                if (!normCourse.equalsIgnoreCase(blankToNull(row.getCourse()))) continue;
                if (row.getBatch() == null || !normBatchId.equals(row.getBatch().getId())) continue;
                if (normYear != null && !academicYearMatches(row.getAcademicYear(), normYear)) continue;
                String rowSem = normalizeSemesterKey(row.getSemester());
                if (rowSem != null && normSem != null && !normSem.equalsIgnoreCase(rowSem)) continue;
                return row;
            }
        }

        if (normBatch != null) {
            for (Fees row : rules) {
                if (!normCourse.equalsIgnoreCase(blankToNull(row.getCourse()))) continue;
                if (!normBatch.equalsIgnoreCase(blankToNull(row.getBatchName()))) continue;
                if (normYear != null && !academicYearMatches(row.getAcademicYear(), normYear)) continue;
                if (isYearWiseFeeRule(row)) return row;
            }
            for (Fees row : rules) {
                if (!normCourse.equalsIgnoreCase(blankToNull(row.getCourse()))) continue;
                if (!normBatch.equalsIgnoreCase(blankToNull(row.getBatchName()))) continue;
                if (normYear != null && !academicYearMatches(row.getAcademicYear(), normYear)) continue;
                String rowSem = normalizeSemesterKey(row.getSemester());
                if (rowSem != null && normSem != null && !normSem.equalsIgnoreCase(rowSem)) continue;
                return row;
            }
        }

        if (normYear != null && normSem != null) {
            Fees row = feesRepo.findByAdminAndCourseIgnoreCaseAndAcademicYearIgnoreCaseAndSemesterIgnoreCase(admin, normCourse, normYear, normSem);
            if (row != null) return row;
        }

        if (normYear != null) {
            Fees row = feesRepo.findByAdminAndCourseIgnoreCaseAndAcademicYearIgnoreCaseAndSemesterIsNull(admin, normCourse, normYear);
            if (row != null) return row;
        }

        if (normBatch != null) {
            for (Fees row : rules) {
                if (!normCourse.equalsIgnoreCase(blankToNull(row.getCourse()))) continue;
                if (!normBatch.equalsIgnoreCase(blankToNull(row.getBatchName()))) continue;
                if (normYear != null && !normYear.equalsIgnoreCase(blankToNull(row.getAcademicYear()))) continue;
                String rowSem = normalizeSemesterKey(row.getSemester());
                if (rowSem != null && normSem != null && !normSem.equalsIgnoreCase(rowSem)) continue;
                return row;
            }
        }

        return feesRepo.findByAdminAndCourseIgnoreCaseAndAcademicYearIsNullAndSemesterIsNull(admin, normCourse);
    }

    private double feesForStudent(Admin admin, String course, Long batchId, String batchName, String academicYear, String semester) {
        Fees fee = findFeeRule(admin, course, batchId, batchName, academicYear, semester);
        if (fee != null) {
            return fee.getTotalAmount();
        }

        Double classFee = resolveClassFeeFallback(admin, batchId, course, academicYear, semester);
        return classFee != null ? classFee : 0.0;
    }

    private StudentFeeResolution resolveStudentFeeResolution(Admin admin, Student student) {
        if (admin == null || student == null) {
            return new StudentFeeResolution(false, null);
        }
        ClassRoom classRoom = student.getClassRoom();
        Batch resolvedBatch = student.getBatch();
        if (resolvedBatch == null && classRoom != null) {
            resolvedBatch = resolveBatchForClass(admin, classRoom, null);
        }
        String courseCode = blankToNull(student.getCourse());
        if (classRoom != null) {
            String classCourse = blankToNull(classRoom.getCourseCode()) != null ? classRoom.getCourseCode() : classRoom.getCourse();
            String resolvedClassCourse = resolveCourseCode(classCourse, buildCourseCatalog(courseRepo.findByAdminOrderByCodeAsc(admin)));
            if (resolvedClassCourse != null) {
                courseCode = resolvedClassCourse;
            }
        }
        Course resolvedCourse = resolveAdminCourseByInput(admin, courseCode);
        if (resolvedCourse != null && blankToNull(resolvedCourse.getCode()) != null) {
            courseCode = resolvedCourse.getCode().strip();
        }
        String academicYear = blankToNull(student.getAcademicYear());
        if (academicYear == null && classRoom != null) {
            academicYear = blankToNull(classRoom.getAcademicYear());
        }
        String semester = blankToNull(student.getSemester());
        if (semester == null && classRoom != null && classRoom.getSemester() != null) {
            semester = formatSemester(classRoom.getSemester());
        }

        Fees fee = findFeeRule(
                admin,
                courseCode,
                resolvedBatch != null ? resolvedBatch.getId() : null,
                resolvedBatch != null ? resolvedBatch.getDisplayName() : (classRoom != null ? classRoom.getBatchName() : null),
                academicYear,
                semester
        );
        if (fee != null) {
            return new StudentFeeResolution(true, fee.getTotalAmount());
        }

        Double classFee = resolveClassFeeFallback(
                admin,
                resolvedBatch != null ? resolvedBatch.getId() : null,
                courseCode,
                academicYear,
                semester
        );
        if (classFee != null && classFee > 0) {
            return new StudentFeeResolution(true, classFee);
        }

        return new StudentFeeResolution(false, null);
    }

    private void syncStudentFeeTotals(Admin admin, Student student) {
        if (admin == null || student == null) {
            return;
        }
        StudentFeeResolution resolution = resolveStudentFeeResolution(admin, student);
        if (!resolution.assigned() || resolution.totalAmount() == null || resolution.totalAmount() <= 0) {
            student.setTotalFees(null);
            student.setPendingFees(null);
            return;
        }
        double paid = student.getPaidFees() != null ? student.getPaidFees() : 0.0;
        student.setTotalFees(resolution.totalAmount());
        student.setPendingFees(Math.max(0, resolution.totalAmount() - paid));
    }

    private boolean studentMatchesFeeRule(Admin admin, Student student, Fees fee) {
        if (admin == null || student == null || fee == null) {
            return false;
        }
        String feeCourse = blankToNull(fee.getCourse());
        Course studentCourse = resolveAdminCourseByInput(admin, student.getCourse());
        String studentCourseCode = blankToNull(studentCourse != null ? studentCourse.getCode() : student.getCourse());
        if (feeCourse == null || studentCourseCode == null || !feeCourse.equalsIgnoreCase(studentCourseCode)) {
            return false;
        }

        if (fee.getBatch() != null) {
            if (student.getBatch() == null || student.getBatch().getId() == null || !fee.getBatch().getId().equals(student.getBatch().getId())) {
                return false;
            }
        } else if (blankToNull(fee.getBatchName()) != null) {
            String studentBatch = student.getBatch() != null ? blankToNull(student.getBatch().getDisplayName()) : null;
            if (studentBatch == null || !fee.getBatchName().equalsIgnoreCase(studentBatch)) {
                return false;
            }
        }

        if (blankToNull(fee.getAcademicYear()) != null
                && !academicYearMatches(blankToNull(fee.getAcademicYear()), blankToNull(student.getAcademicYear()))) {
            return false;
        }

        if (!isYearWiseFeeRule(fee)) {
            String feeSemester = normalizeSemesterKey(fee.getSemester());
            String studentSemester = normalizeSemesterKey(student.getSemester());
            if (feeSemester != null && studentSemester != null && !feeSemester.equalsIgnoreCase(studentSemester)) {
                return false;
            }
            if (feeSemester != null && studentSemester == null) {
                return false;
            }
        }

        return true;
    }

    private Double resolveClassFeeFallback(Admin admin, Long batchId, String course, String academicYear, String semester) {
        Batch selectedBatch = getAdminBatch(admin, batchId);
        Integer semesterNumber = parseSemesterNumber(semester);

        for (ClassRoom classRoom : classRepo.findByAdmin(admin)) {
            if (!classMatchesTimetableScope(classRoom, selectedBatch, course, academicYear, semesterNumber)) {
                continue;
            }
            if (classRoom.getTotalFees() != null && classRoom.getTotalFees() > 0) {
                return classRoom.getTotalFees();
            }
        }
        return null;
    }

    private Batch resolveBatchForClass(Admin admin, ClassRoom classRoom, List<Batch> availableBatches) {
        if (admin == null || classRoom == null) {
            return null;
        }

        List<Batch> batches = availableBatches != null ? availableBatches : batchRepo.findByAdminOrderByDisplayNameAsc(admin);
        for (Batch batch : batches) {
            if (batchMatchesClass(classRoom, batch)) {
                return batch;
            }
        }
        return null;
    }

    private Double resolveDisplayedClassFee(Admin admin,
                                           ClassRoom classRoom,
                                           Map<String, Course> courseCatalog,
                                           List<Batch> availableBatches) {
        if (admin == null || classRoom == null) {
            return null;
        }

        String courseCode = resolveCourseCode(
                blankToNull(classRoom.getCourseCode()) != null ? classRoom.getCourseCode() : classRoom.getCourse(),
                courseCatalog
        );
        if (courseCode == null) {
            courseCode = blankToNull(classRoom.getCourseCode()) != null ? classRoom.getCourseCode().strip() : null;
        }
        if (courseCode == null) {
            return classRoom.getTotalFees();
        }

        Batch resolvedBatch = resolveBatchForClass(admin, classRoom, availableBatches);
        Fees feeRule = findFeeRule(
                admin,
                courseCode,
                resolvedBatch != null ? resolvedBatch.getId() : null,
                resolvedBatch != null ? resolvedBatch.getDisplayName() : classRoom.getBatchName(),
                classRoom.getAcademicYear(),
                formatSemester(classRoom.getSemester())
        );
        if (feeRule != null) {
            return feeRule.getTotalAmount();
        }

        return classRoom.getTotalFees();
    }

    // ── Helper: save photo ──
    private String savePhoto(MultipartFile file, String uploadDir) {
        String ct = file.getContentType();
        if (ct == null || !(ct.equals("image/jpeg") || ct.equals("image/png")
                || ct.equals("image/webp") || ct.equals("image/gif")))
            return null;
        if (file.getSize() > 2 * 1024 * 1024) return null;

        File dir = new File(uploadDir);
        if (!dir.exists() && !dir.mkdirs()) {
            log.error("Failed to create upload directory: {}", uploadDir);
            return null;
        }

        String original = file.getOriginalFilename();
        String ext = (original != null && original.contains("."))
                ? original.substring(original.lastIndexOf(".")) : ".jpg";
        String fileName = UUID.randomUUID() + ext;

        try {
            Files.copy(file.getInputStream(),
                    Paths.get(uploadDir + fileName),
                    StandardCopyOption.REPLACE_EXISTING);
            return uploadDir.contains("teachers")
                    ? "/uploads/teachers/" + fileName
                    : "/uploads/students/" + fileName;
        } catch (IOException e) {
            log.error("Failed to save photo to {}: {}", uploadDir, e.getMessage(), e);
            return null;
        }
    }

    // ═══════════════════════════════════════════════
    // DASHBOARD
    // ═══════════════════════════════════════════════
    @GetMapping("/admin-dashboard")
    public String dashboard(Model model, HttpSession session, HttpServletResponse response) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        List<Student>   students = studentRepo.findByAdmin(admin);
        List<Teacher>   teachers = teacherRepo.findByAdmin(admin);
        List<ClassRoom> classes  = classRepo.findByAdmin(admin);
        List<Course>    courses  = courseRepo.findByAdminOrderByCodeAsc(admin);
        List<Batch>     batches  = batchRepo.findByAdminOrderByDisplayNameAsc(admin);

        model.addAttribute("totalStudents", students.size());
        model.addAttribute("totalTeachers", teachers.size());
        model.addAttribute("totalClasses",  classes.size());
        model.addAttribute("totalCourses",  courses.size());
        model.addAttribute("totalBatches",  batches.size());
        model.addAttribute("students",      students);
        model.addAttribute("teachers",      teachers);
        model.addAttribute("courses",       courses);
        model.addAttribute("batches",       batches);
        model.addAttribute("courseDistribution", buildCourseDistribution(courses, batches));
        List<AcademicStructureService.StructureMatrixRow> structureRows = academicStructureService.listAdminStructureRows(admin);
        model.addAttribute("summaryStructures", structureRows);
        model.addAttribute("summarySemesters", structureRows.stream()
                .map(AcademicStructureService.StructureMatrixRow::semesterNumber)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        model.addAttribute("totalAcademicStructures", structureRows.size());
        model.addAttribute("totalStructureSemesters", structureRows.stream()
                .map(AcademicStructureService.StructureMatrixRow::semesterNumber)
                .filter(Objects::nonNull)
                .distinct()
                .count());
        model.addAttribute("dashboardStructureYears", structureRows.stream()
                .map(AcademicStructureService.StructureMatrixRow::yearLabel)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        addAdminAttributes(model, admin);
        return "admin/admin-dashboard";
    }

    private List<CourseDistributionView> buildCourseDistribution(List<Course> courses,
                                                                 List<Batch> batches) {
        if (courses == null || courses.isEmpty() || batches == null || batches.isEmpty()) {
            return List.of();
        }

        Set<Long> activeCourseIds = courses.stream()
                .filter(Objects::nonNull)
                .map(Course::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (activeCourseIds.isEmpty()) {
            return List.of();
        }

        Map<String, Long> counts = new LinkedHashMap<>();
        for (Batch batch : batches) {
            Course course = batch != null ? batch.getCourse() : null;
            Long courseId = course != null ? course.getId() : null;
            if (courseId == null || !activeCourseIds.contains(courseId)) {
                continue;
            }
            String courseName = blankToNull(course.getName());
            String courseCode = blankToNull(course.getCode());
            String label = courseName != null ? courseName : courseCode;
            if (label == null) {
                continue;
            }
            counts.merge(label.strip(), 1L, Long::sum);
        }

        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        if (total <= 0) {
            return List.of();
        }

        List<String> colorClasses = List.of("", "green", "blue", "gold");
        List<CourseDistributionView> distribution = new ArrayList<>();
        int index = 0;

        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            int percentage = (int) Math.round((entry.getValue() * 100.0) / total);
            distribution.add(new CourseDistributionView(
                    entry.getKey(),
                    entry.getValue(),
                    percentage,
                    colorClasses.get(index % colorClasses.size())
            ));
            index++;
        }

        return distribution;
    }

    // ═══════════════════════════════════════════════
    // ACADEMIC SETUP
    // ═══════════════════════════════════════════════
    @GetMapping("/admin-courses")
    public String adminCourses(Model model, HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";
        model.addAttribute("courses", courseRepo.findByAdminOrderByCodeAsc(admin));
        addAdminAttributes(model, admin);
        return "admin/admin-courses";
    }

    @PostMapping("/admin-courses")
    public String saveCourse(@RequestParam String name,
                             @RequestParam String code,
                             @RequestParam(required = false) Integer durationYears,
                             @RequestParam(required = false) Integer totalSemesters,
                             @RequestParam(required = false) String department,
                             @RequestParam(required = false) String courseType,
                             Model model,
                             HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        String normalizedCode = blankToNull(code) != null ? code.strip().toUpperCase(Locale.ENGLISH) : null;
        String normalizedName = blankToNull(name);
        if (normalizedCode == null || normalizedName == null) {
            model.addAttribute("error", "Course name and code are required.");
            model.addAttribute("courses", courseRepo.findByAdminOrderByCodeAsc(admin));
            addAdminAttributes(model, admin);
            return "admin/admin-courses";
        }

        if (courseRepo.findByAdminAndCodeIgnoreCase(admin, normalizedCode) != null) {
            model.addAttribute("error", "Course code already exists for this admin.");
            model.addAttribute("courses", courseRepo.findByAdminOrderByCodeAsc(admin));
            addAdminAttributes(model, admin);
            return "admin/admin-courses";
        }

        Course course = new Course();
        course.setAdmin(admin);
        course.setName(normalizedName);
        course.setCode(normalizedCode);
        Integer resolvedDurationYears = durationYears;
        if (resolvedDurationYears == null && totalSemesters != null && totalSemesters > 0) {
            resolvedDurationYears = Math.max(1, (int) Math.ceil(totalSemesters / 2.0));
        }
        Integer resolvedTotalSemesters = totalSemesters;
        if (resolvedTotalSemesters == null && resolvedDurationYears != null && resolvedDurationYears > 0) {
            resolvedTotalSemesters = resolvedDurationYears * 2;
        }
        course.setDurationYears(resolvedDurationYears);
        course.setTotalSemesters(resolvedTotalSemesters);
        course.setDepartment(blankToNull(department));
        course.setCourseType(blankToNull(courseType));
        course.setStatus("Active");
        courseRepo.save(course);
        return "redirect:/admin-courses?success=Course created successfully";
    }

    @GetMapping("/admin-batches")
    public String adminBatches(Model model, HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";
        model.addAttribute("courses", courseRepo.findByAdminOrderByCodeAsc(admin));
        model.addAttribute("batches", batchRepo.findByAdminOrderByDisplayNameAsc(admin));
        addAdminAttributes(model, admin);
        return "admin/admin-batches";
    }

    @PostMapping("/admin-batches")
    public String saveBatch(@RequestParam Long courseId,
                            @RequestParam Integer startYear,
                            @RequestParam Integer endYear,
                            @RequestParam(required = false) String displayName,
                            Model model,
                            HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        Course course = getAdminCourse(admin, courseId);
        if (course == null) return "redirect:/admin-batches?error=Invalid course";

        Batch existingRange = batchRepo.findByAdminAndCourseAndStartYearAndEndYear(admin, course, startYear, endYear);
        if (existingRange != null) {
            model.addAttribute("error", "A batch already exists for this course and year range.");
            model.addAttribute("courses", courseRepo.findByAdminOrderByCodeAsc(admin));
            model.addAttribute("batches", batchRepo.findByAdminOrderByDisplayNameAsc(admin));
            addAdminAttributes(model, admin);
            return "admin/admin-batches";
        }

        String baseName = blankToNull(displayName);
        if (baseName == null || baseName.equalsIgnoreCase(course.getCode())) {
            baseName = course.getCode();
        }
        String name = String.format("%s (%02d-%02d)", baseName, startYear % 100, endYear % 100);

        Batch batch = new Batch();
        batch.setAdmin(admin);
        batch.setCourse(course);
        batch.setDisplayName(name);
        batch.setStartYear(startYear);
        batch.setEndYear(endYear);
        batchRepo.save(batch);
        int copiedSubjects = copyAllSubjectMastersToBatch(admin, batch);
        if (copiedSubjects == 0) {
            return "redirect:/admin-batches?success=Batch created successfully&warning=No subjects found in Subject Master for this course. Please open the course and add subjects first.";
        }
        return "redirect:/admin-batches?success=Batch created successfully with " + copiedSubjects + " subject(s) auto-assigned";
    }

    @Transactional
    @PostMapping("/admin-batches/{batchId}/update")
    public String updateBatch(@PathVariable Long batchId,
                              @RequestParam Long courseId,
                              @RequestParam Integer startYear,
                              @RequestParam Integer endYear,
                              @RequestParam(required = false) String displayName,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        Batch batch = getAdminBatch(admin, batchId);
        Course course = getAdminCourse(admin, courseId);
        if (batch == null || course == null) {
            redirectAttributes.addAttribute("error", "Invalid batch update request");
            return "redirect:/admin-batches";
        }

        Batch existingRange = batchRepo.findByAdminAndCourseAndStartYearAndEndYear(admin, course, startYear, endYear);
        if (existingRange != null && !existingRange.getId().equals(batch.getId())) {
            redirectAttributes.addAttribute("error", "A batch already exists for this course and year range.");
            return "redirect:/admin-batches";
        }

        boolean courseChanged = batch.getCourse() == null || !batch.getCourse().getId().equals(course.getId());
        if (courseChanged) {
            subjectRepo.findByAdminAndBatchRef(admin, batch).forEach(subjectRepo::delete);
            academicStructureRepo.findByAdminAndBatch(admin, batch).forEach(academicStructureRepo::delete);
        }

        String baseName = blankToNull(displayName);
        if (baseName == null || baseName.equalsIgnoreCase(course.getCode())) {
            baseName = course.getCode();
        }
        batch.setCourse(course);
        batch.setStartYear(startYear);
        batch.setEndYear(endYear);
        batch.setDisplayName(String.format("%s (%02d-%02d)", baseName, startYear % 100, endYear % 100));
        batchRepo.save(batch);

        if (courseChanged) {
            copyAllSubjectMastersToBatch(admin, batch);
        }

        redirectAttributes.addAttribute("success", "Batch updated. Academic structure and linked subject mappings were refreshed.");
        return "redirect:/admin-batches";
    }

    @Transactional
    @PostMapping("/admin-batches/{batchId}/delete")
    public String deleteBatch(@PathVariable Long batchId,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        Batch batch = getAdminBatch(admin, batchId);
        if (batch == null) {
            redirectAttributes.addAttribute("error", "Invalid batch");
            return "redirect:/admin-batches";
        }

        String label = batch.getDisplayName();
        deleteBatchDependencies(admin, batch);
        batchRepo.delete(batch);
        redirectAttributes.addAttribute("success", "Batch " + label + " deleted. Subjects, filters, counters and academic structure were refreshed.");
        return "redirect:/admin-batches";
    }

    @GetMapping("/admin-academic-structure")
    public String adminAcademicStructure(Model model, HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";
        addAcademicStructureModel(model, admin);
        addAdminAttributes(model, admin);
        return "admin/admin-academic-structure";
    }

    @PostMapping("/admin-academic-structure")
    public String saveAcademicStructure(@RequestParam Long courseId,
                                        @RequestParam Long batchId,
                                        @RequestParam(required = false) String year,
                                        @RequestParam(required = false) Integer semester,
                                        @RequestParam(required = false) String section,
                                        @RequestParam(required = false) String action,
                                        Model model,
                                        HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        Course course = getAdminCourse(admin, courseId);
        Batch batch = getAdminBatch(admin, batchId);
        if (course == null || batch == null || batch.getCourse() == null || !batch.getCourse().getId().equals(course.getId())) {
            return "redirect:/admin-academic-structure?error=Invalid academic structure selection";
        }

        if ("sync".equalsIgnoreCase(blankToNull(action))) {
            AcademicStructureService.StructureSyncResult syncResult = academicStructureService.syncBatchStructure(admin, course, batch);
            return "redirect:/admin-academic-structure?success=Academic year, year type, and semester options refreshed for "
                    + syncResult.batchPlan().rows().size() + " semester option(s). Add sections manually.";
        }

        AcademicStructureService.ValidationResult validation = academicStructureService
                .validateSelection(admin, course, batch, year, semester, section);
        if (!validation.valid()) {
            model.addAttribute("error", validation.message());
            addAcademicStructureModel(model, admin);
            addAdminAttributes(model, admin);
            return "admin/admin-academic-structure";
        }

        AcademicStructure structure = new AcademicStructure();
        structure.setAdmin(admin);
        structure.setCourse(course);
        structure.setBatch(batch);
        structure.setYearLabel(validation.normalizedYearLabel());
        structure.setSemesterNumber(semester);
        structure.setSection(section.strip().toUpperCase(Locale.ENGLISH));
        academicStructureRepo.save(structure);
        return "redirect:/admin-academic-structure?success=Academic structure section saved successfully";
    }

    @PostMapping("/admin-academic-structure/{structureId}/edit")
    public String editAcademicStructure(@PathVariable Long structureId,
                                        @RequestParam Long courseId,
                                        @RequestParam Long batchId,
                                        @RequestParam(required = false) String year,
                                        @RequestParam(required = false) Integer semester,
                                        @RequestParam(required = false) String section,
                                        RedirectAttributes redirectAttributes,
                                        HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        Course course = getAdminCourse(admin, courseId);
        Batch batch = getAdminBatch(admin, batchId);
        AcademicStructureService.StructureMutationResult result = academicStructureService
                .updateStructure(admin, structureId, course, batch, year, semester, section);
        redirectAttributes.addAttribute(result.success() ? "success" : "error", result.message());
        if (result.success()) {
            redirectAttributes.addAttribute("info",
                    "Synced " + result.studentsUpdated() + " student(s), "
                            + result.timetableUpdated() + " timetable row(s), "
                            + result.feesUpdated() + " fee rule(s), and "
                            + result.subjectsUpdated() + " academic mapping(s).");
        }
        return "redirect:/admin-academic-structure";
    }

    @PostMapping("/admin-academic-structure/{structureId}/delete")
    public String deleteAcademicStructure(@PathVariable Long structureId,
                                          RedirectAttributes redirectAttributes,
                                          HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        AcademicStructureService.StructureMutationResult result = academicStructureService.deleteStructure(admin, structureId);
        redirectAttributes.addAttribute(result.success() ? "success" : "error", result.message());
        if (result.success()) {
            redirectAttributes.addAttribute("info",
                    "Updated " + result.studentsUpdated() + " student link(s), "
                            + result.timetableUpdated() + " timetable row(s), "
                            + result.feesUpdated() + " fee rule(s), and "
                            + result.subjectsUpdated() + " academic mapping(s).");
        }
        return "redirect:/admin-academic-structure";
    }

    @GetMapping("/admin-academic-structure/options")
    @ResponseBody
    public Map<String, Object> academicStructureOptions(@RequestParam Long courseId,
                                                        @RequestParam Long batchId,
                                                        HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) {
            return Map.of("ok", false, "message", "Unauthorized");
        }

        Course course = getAdminCourse(admin, courseId);
        Batch batch = getAdminBatch(admin, batchId);
        if (course == null || batch == null || batch.getCourse() == null || !batch.getCourse().getId().equals(course.getId())) {
            return Map.of("ok", false, "message", "Invalid course or batch selection");
        }

        AcademicStructureService.BatchPlan batchPlan = academicStructureService.buildBatchPlan(admin, course, batch);
        LinkedHashMap<String, List<Integer>> semesterMap = new LinkedHashMap<>();
        for (AcademicStructureService.StructureMatrixRow row : batchPlan.rows()) {
            semesterMap.computeIfAbsent(row.yearLabel(), key -> new ArrayList<>()).add(row.semesterNumber());
        }

        return Map.of(
                "ok", true,
                "batchName", batchPlan.batchName(),
                "courseCode", batchPlan.courseCode(),
                "courseName", batchPlan.courseName(),
                "rows", batchPlan.rows(),
                "academicYears", batchPlan.rows().stream()
                        .map(AcademicStructureService.StructureMatrixRow::academicYear)
                        .distinct()
                        .toList(),
                "yearLabels", batchPlan.rows().stream()
                        .map(AcademicStructureService.StructureMatrixRow::yearLabel)
                        .distinct()
                        .toList(),
                "semesterMap", semesterMap
        );
    }

    @GetMapping("/admin-subjects")
    public String adminSubjects(HttpSession session) {
        if (getLoggedAdmin(session) == null) return "redirect:/login-admin";
        return "redirect:/admin-subject-registry";
    }

    @GetMapping("/admin-subject-master")
    public String adminSubjectMaster(HttpSession session) {
        if (getLoggedAdmin(session) == null) return "redirect:/login-admin";
        return "redirect:/admin-subject-registry";
    }

    @GetMapping("/admin-subject-registry")
    public String adminSubjectRegistry(@RequestParam(required = false) Long courseId,
                                       @RequestParam(required = false) Integer semesterFilter,
                                       Model model,
                                       HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        List<Course> courses = courseRepo.findByAdminOrderByCodeAsc(admin);
        Course selectedCourse = null;
        if (!courses.isEmpty()) {
            selectedCourse = courseId != null
                    ? courses.stream()
                    .filter(course -> course.getId() != null && course.getId().equals(courseId))
                    .findFirst()
                    .orElse(courses.get(0))
                    : courses.get(0);
        }

        List<SubjectMaster> allSubjectMasters = selectedCourse != null
                ? subjectMasterRepo.findByAdminAndCourseOrderBySemesterNumberAscNameAsc(admin, selectedCourse)
                : List.of();
        List<SubjectMaster> subjectMasters = semesterFilter != null
                ? allSubjectMasters.stream()
                .filter(subjectMaster -> semesterFilter.equals(subjectMaster.getSemesterNumber()))
                .toList()
                : allSubjectMasters;

        Map<Long, Long> subjectUsageCounts = new HashMap<>();
        for (SubjectMaster subjectMaster : allSubjectMasters) {
            subjectUsageCounts.put(
                    subjectMaster.getId(),
                    subjectRepo.countDistinctBatchesByAdminAndSubjectMasterRefAndStatus(admin, subjectMaster, "active")
            );
        }

        long totalSubjectMasters = 0;
        for (Course course : courses) {
            totalSubjectMasters += subjectMasterRepo.findByAdminAndCourse(admin, course).size();
        }

        model.addAttribute("courses", courses);
        model.addAttribute("selectedCourse", selectedCourse);
        model.addAttribute("semesterFilter", semesterFilter);
        model.addAttribute("semesterOptions", selectedCourse != null ? semesterOptions(selectedCourse) : List.of());
        model.addAttribute("subjectMasters", subjectMasters);
        model.addAttribute("registryYearLabels", selectedCourse != null ? subjectRegistryYearLabels(selectedCourse) : List.of());
        model.addAttribute("teachers", activeTeachersForAdmin(admin));
        model.addAttribute("totalSubjectMasters", totalSubjectMasters);
        model.addAttribute("activeBatchCount", selectedCourse != null ? activeBatchesForCourse(admin, selectedCourse).size() : 0);
        model.addAttribute("subjectUsageCounts", subjectUsageCounts);
        addAdminAttributes(model, admin);
        return "admin/admin-subject-registry";
    }

    @GetMapping("/admin-courses/{courseId}")
    public String courseDetail(@PathVariable Long courseId,
                               @RequestParam(required = false, defaultValue = "info") String tab,
                               @RequestParam(required = false) Integer semesterFilter,
                               Model model,
                               HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        Course course = getAdminCourse(admin, courseId);
        if (course == null) return "redirect:/admin-courses?error=Invalid course";

        List<SubjectMaster> allSubjectMasters = subjectMasterRepo.findByAdminAndCourseOrderBySemesterNumberAscNameAsc(admin, course);
        List<SubjectMaster> subjectMasters = semesterFilter != null
                ? allSubjectMasters.stream()
                .filter(subjectMaster -> semesterFilter.equals(subjectMaster.getSemesterNumber()))
                .toList()
                : allSubjectMasters;
        Map<Long, Long> subjectUsageCounts = new HashMap<>();
        for (SubjectMaster subjectMaster : allSubjectMasters) {
            subjectUsageCounts.put(
                    subjectMaster.getId(),
                    subjectRepo.countDistinctBatchesByAdminAndSubjectMasterRefAndStatus(admin, subjectMaster, "active")
            );
        }

        model.addAttribute("course", course);
        model.addAttribute("activeTab", "subjects".equalsIgnoreCase(tab) ? "subjects" : "info");
        model.addAttribute("semesterFilter", semesterFilter);
        model.addAttribute("semesterOptions", semesterOptions(course));
        model.addAttribute("subjectMasters", subjectMasters);
        model.addAttribute("registryYearLabels", subjectRegistryYearLabels(course));
        model.addAttribute("teachers", activeTeachersForAdmin(admin));
        model.addAttribute("totalSubjectMasters", allSubjectMasters.size());
        model.addAttribute("activeBatchCount", activeBatchesForCourse(admin, course).size());
        model.addAttribute("subjectUsageCounts", subjectUsageCounts);
        addAdminAttributes(model, admin);
        return "admin/course-detail";
    }

    @GetMapping("/admin-courses/{courseId}/subjects/check-code")
    @ResponseBody
    public Map<String, Object> checkSubjectMasterCode(@PathVariable Long courseId,
                                                      @RequestParam Integer semesterNumber,
                                                      @RequestParam String code,
                                                      @RequestParam(required = false) Long excludeId,
                                                      HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) {
            return Map.of("available", false, "message", "Please sign in again.");
        }

        Course course = getAdminCourse(admin, courseId);
        String normalizedCode = blankToNull(code) != null ? code.strip().toUpperCase(Locale.ENGLISH) : null;
        if (course == null || normalizedCode == null || semesterNumber == null) {
            return Map.of("available", false, "message", "Choose semester and subject code first.");
        }

        SubjectMaster duplicate = excludeId != null
                ? subjectMasterRepo.findByAdminAndCourseAndSemesterNumberAndCodeIgnoreCaseAndIdNot(admin, course, semesterNumber, normalizedCode, excludeId)
                : subjectMasterRepo.findByAdminAndCourseAndSemesterNumberAndCodeIgnoreCase(admin, course, semesterNumber, normalizedCode);

        return Map.of(
                "available", duplicate == null,
                "message", duplicate == null
                        ? "Subject code is available."
                        : "This subject code is already used for the selected course and semester."
        );
    }

    @PostMapping("/admin-courses/{courseId}/update")
    public String updateCourse(@PathVariable Long courseId,
                               @RequestParam String name,
                               @RequestParam String code,
                               @RequestParam(required = false) Integer durationYears,
                               @RequestParam(required = false) Integer totalSemesters,
                               @RequestParam(required = false) String status,
                               @RequestParam(required = false) String department,
                               @RequestParam(required = false) String courseType,
                               HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        Course course = getAdminCourse(admin, courseId);
        if (course == null) return "redirect:/admin-courses?error=Invalid course";

        String normalizedCode = blankToNull(code) != null ? code.strip().toUpperCase(Locale.ENGLISH) : null;
        String normalizedName = blankToNull(name);
        if (normalizedCode == null || normalizedName == null) {
            return "redirect:/admin-courses/" + courseId + "?tab=info&error=Course name and code are required";
        }

        Course duplicate = courseRepo.findByAdminAndCodeIgnoreCase(admin, normalizedCode);
        if (duplicate != null && !duplicate.getId().equals(course.getId())) {
            return "redirect:/admin-courses/" + courseId + "?tab=info&error=Course code already exists";
        }

        course.setName(normalizedName);
        course.setCode(normalizedCode);
        course.setDurationYears(durationYears);
        course.setTotalSemesters(totalSemesters);
        course.setStatus(blankToNull(status) != null ? status.strip() : "Active");
        course.setDepartment(blankToNull(department));
        course.setCourseType(blankToNull(courseType));
        courseRepo.save(course);
        return "redirect:/admin-courses/" + courseId + "?tab=info&success=Course updated successfully";
    }

    @Transactional
    @PostMapping("/admin-courses/{courseId}/delete")
    public String deleteCourse(@PathVariable Long courseId,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        Course course = getAdminCourse(admin, courseId);
        if (course == null) {
            redirectAttributes.addAttribute("error", "Invalid course");
            return "redirect:/admin-courses";
        }

        String label = course.getCode();
        deleteCourseDependencies(admin, course);
        courseRepo.delete(course);
        redirectAttributes.addAttribute("success", "Course " + label + " deleted. Related batches, subjects and academic structure rows were refreshed.");
        return "redirect:/admin-courses";
    }

    @PostMapping("/admin-courses/{courseId}/subject-master")
    public String saveSubjectMaster(@PathVariable Long courseId,
                                    @RequestParam(required = false) Long id,
                                    @RequestParam String name,
                                    @RequestParam String code,
                                    @RequestParam String yearLabel,
                                    @RequestParam Integer semesterNumber,
                                    @RequestParam(required = false) Double credits,
                                    @RequestParam(required = false) String category,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(required = false) String returnTo,
                                    HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        Course course = getAdminCourse(admin, courseId);
        if (course == null) return subjectRegistryRedirect(returnTo, courseId, "error", "Invalid course");

        String normalizedName = blankToNull(name);
        String normalizedCode = blankToNull(code) != null ? code.strip().toUpperCase(Locale.ENGLISH) : null;
        String normalizedYearLabel = blankToNull(yearLabel) != null ? yearLabel.strip().toUpperCase(Locale.ENGLISH) : null;
        if (normalizedName == null || normalizedCode == null || semesterNumber == null || normalizedYearLabel == null) {
            return subjectRegistryRedirect(returnTo, courseId, "error", "All required subject registry fields must be filled");
        }

        if (semesterNumber < 1 || (course.getTotalSemesters() != null && semesterNumber > course.getTotalSemesters())) {
            return subjectRegistryRedirect(returnTo, courseId, "error", "Please choose a valid semester for this course");
        }

        String expectedYearLabel = deriveYearLabelForSemester(semesterNumber);
        if (expectedYearLabel != null && !expectedYearLabel.equalsIgnoreCase(normalizedYearLabel)) {
            return subjectRegistryRedirect(returnTo, courseId, "error", normalizedYearLabel + " only supports semester mapping for " + expectedYearLabel);
        }

        SubjectMaster duplicate = id != null
                ? subjectMasterRepo.findByAdminAndCourseAndSemesterNumberAndCodeIgnoreCaseAndIdNot(admin, course, semesterNumber, normalizedCode, id)
                : subjectMasterRepo.findByAdminAndCourseAndSemesterNumberAndCodeIgnoreCase(admin, course, semesterNumber, normalizedCode);
        if (duplicate != null) {
            return subjectRegistryRedirect(returnTo, courseId, "error", "Subject code already exists for this course and semester");
        }

        SubjectMaster subjectMaster = id != null ? getAdminSubjectMaster(admin, id) : new SubjectMaster();
        if (subjectMaster == null) {
            return subjectRegistryRedirect(returnTo, courseId, "error", "Invalid subject");
        }

        boolean isNew = subjectMaster.getId() == null;
        subjectMaster.setAdmin(admin);
        subjectMaster.setCourse(course);
        subjectMaster.setName(normalizedName);
        subjectMaster.setCode(normalizedCode);
        subjectMaster.setYearLabel(normalizedYearLabel);
        subjectMaster.setSemesterNumber(semesterNumber);
        subjectMaster.setCredits(credits != null ? credits : 4.0);
        subjectMaster.setCategory(normalizeSubjectCategory(category));
        subjectMaster.setStatus(blankToNull(status) != null ? status.strip().toLowerCase(Locale.ENGLISH) : "active");
        subjectMasterRepo.save(subjectMaster);

        int synced = syncSubjectRegistryToExistingBatches(admin, course, subjectMaster);
        String success = isNew
                ? "Subject added to Subject Registry and synced to " + activeBatchesForCourse(admin, course).size() + " active batch(es)"
                : "Subject Registry updated and synced to current batch subjects";
        if (synced > 0) {
            success += " (" + synced + " new batch subject row(s) created)";
        }
        return subjectRegistryRedirect(returnTo, courseId, "success", success);
    }

    @PostMapping("/admin-courses/{courseId}/subject-master/{subjectMasterId}/apply-existing")
    public String applySubjectMasterToExistingBatches(@PathVariable Long courseId,
                                                      @PathVariable Long subjectMasterId,
                                                      HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        Course course = getAdminCourse(admin, courseId);
        SubjectMaster subjectMaster = getAdminSubjectMaster(admin, subjectMasterId);
        if (course == null || subjectMaster == null || subjectMaster.getCourse() == null
                || !subjectMaster.getCourse().getId().equals(course.getId())) {
            return "redirect:/admin-courses?error=Invalid subject master request";
        }

        int copied = copySubjectMasterToExistingBatches(admin, course, subjectMaster);
        return "redirect:/admin-courses/" + courseId + "?tab=subjects&success=Subject Registry synced to " + copied + " batch row(s)";
    }

    @PostMapping("/admin-courses/{courseId}/subject-master/{subjectMasterId}/delete")
    public String deleteSubjectMaster(@PathVariable Long courseId,
                                      @PathVariable Long subjectMasterId,
                                      @RequestParam(required = false) String returnTo,
                                      HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        Course course = getAdminCourse(admin, courseId);
        SubjectMaster subjectMaster = getAdminSubjectMaster(admin, subjectMasterId);
        if (course == null || subjectMaster == null || subjectMaster.getCourse() == null
                || !subjectMaster.getCourse().getId().equals(course.getId())) {
            return subjectRegistryRedirect(returnTo, courseId, "error", "Invalid subject master request");
        }

        subjectRepo.findByAdminAndSubjectMasterRefOrderByBatchRef_DisplayNameAscSemesterAscNameAsc(admin, subjectMaster)
                .forEach(subjectRepo::delete);
        subjectMasterRepo.delete(subjectMaster);
        return subjectRegistryRedirect(returnTo, courseId, "success", "Subject removed from Subject Registry. Linked batch subject copies were refreshed.");
    }

    private String subjectRegistryRedirect(String returnTo, Long courseId, String messageType, String message) {
        String encodedMessage = URLEncoder.encode(message != null ? message : "", StandardCharsets.UTF_8);
        String normalizedReturnTo = blankToNull(returnTo);
        if ("registry".equalsIgnoreCase(normalizedReturnTo)) {
            return "redirect:/admin-subject-registry?courseId=" + courseId + "&" + messageType + "=" + encodedMessage;
        }
        return "redirect:/admin-courses/" + courseId + "?tab=subjects&" + messageType + "=" + encodedMessage;
    }

    @PostMapping("/admin-courses/{courseId}/subject-assignment")
    public String updateCourseSubjectAssignment(@PathVariable Long courseId,
                                                @RequestParam Long subjectId,
                                                @RequestParam(required = false) Long teacherId,
                                                HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        Course course = getAdminCourse(admin, courseId);
        if (course == null) return "redirect:/admin-courses?error=Invalid course";

        Subject subject = subjectRepo.findById(subjectId).orElse(null);
        if (subject == null || subject.getAdmin() == null || !subject.getAdmin().getId().equals(admin.getId())
                || subject.getCourseRef() == null || !subject.getCourseRef().getId().equals(course.getId())) {
            return "redirect:/admin-courses/" + courseId + "?tab=subjects&error=Invalid subject assignment request";
        }

        return "redirect:/admin-courses/" + courseId + "?tab=subjects&error=Teacher assignment is handled only during timetable creation.";
    }

    @GetMapping("/admin-batches/{batchId}")
    public String batchDetail(@PathVariable Long batchId,
                              @RequestParam(required = false, defaultValue = "info") String tab,
                              Model model,
                              HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        Batch batch = getAdminBatch(admin, batchId);
        if (batch == null) return "redirect:/admin-batches?error=Invalid batch";

        model.addAttribute("batch", batch);
        model.addAttribute("courses", courseRepo.findByAdminOrderByCodeAsc(admin));
        model.addAttribute("batchAcademicYearsSummary", batchAcademicYearsSummary(batch));
        model.addAttribute("batchYearLabelsSummary", batchYearLabelsSummary(batch));
        model.addAttribute("batchTotalSemesters", batch.getCourse() != null && batch.getCourse().getTotalSemesters() != null ? batch.getCourse().getTotalSemesters() : null);
        model.addAttribute("activeTab", "subjects".equalsIgnoreCase(tab) ? "subjects" : "info");
        model.addAttribute("batchSubjects", subjectRepo.findByAdminAndBatchRefOrderBySemesterAscNameAsc(admin, batch));
        model.addAttribute("semesterOptions", semesterOptions(batch.getCourse()));
        addAdminAttributes(model, admin);
        return "admin/batch-detail";
    }

    @PostMapping("/admin-batches/{batchId}/subjects/add-extra")
    public String addExtraBatchSubject(@PathVariable Long batchId,
                                       @RequestParam String name,
                                       @RequestParam String code,
                                       @RequestParam Integer semesterNumber,
                                       @RequestParam(required = false) Double credits,
                                       @RequestParam(required = false) String category,
                                       HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        Batch batch = getAdminBatch(admin, batchId);
        if (batch == null) return "redirect:/admin-batches?error=Invalid batch";

        String normalizedName = blankToNull(name);
        String normalizedCode = blankToNull(code) != null ? code.strip().toUpperCase(Locale.ENGLISH) : null;
        if (normalizedName == null || normalizedCode == null || semesterNumber == null) {
            return "redirect:/admin-batches/" + batchId + "?tab=subjects&error=All extra subject fields are required";
        }

        Subject duplicate = subjectRepo.findByAdminAndBatchRefAndSemesterAndCodeIgnoreCase(admin, batch, semesterNumber, normalizedCode);
        if (duplicate != null) {
            return "redirect:/admin-batches/" + batchId + "?tab=subjects&error=Subject code already exists in this batch and semester";
        }

        Subject subject = new Subject();
        subject.setAdmin(admin);
        subject.setCourseRef(batch.getCourse());
        subject.setBatchRef(batch);
        subject.setSubjectMasterRef(null);
        subject.setName(normalizedName);
        subject.setCode(normalizedCode);
        subject.setSemester(semesterNumber);
        subject.setCredits(credits != null ? credits : 4.0);
        subject.setCourseCategory(normalizeSubjectCategory(category));
        subject.setIsOverride(Boolean.TRUE);
        subject.setStatus("active");
        subject.setTerm(semesterNumber + " SEMESTER");
        subject.setCycle(currentAcademicYear());
        subjectRepo.save(subject);
        return "redirect:/admin-batches/" + batchId + "?tab=subjects&success=Batch-only subject added successfully";
    }

    @PostMapping("/admin-batches/{batchId}/subjects/{subjectId}/remove")
    public String removeBatchSubject(@PathVariable Long batchId,
                                     @PathVariable Long subjectId,
                                     HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        Batch batch = getAdminBatch(admin, batchId);
        Subject subject = getAdminSubject(admin, subjectId);
        if (batch == null || subject == null || subject.getBatchRef() == null
                || !subject.getBatchRef().getId().equals(batch.getId())) {
            return "redirect:/admin-batches?error=Invalid batch subject";
        }

        subjectRepo.delete(subject);
        return "redirect:/admin-batches/" + batchId + "?tab=subjects&success=Subject removed from this batch only";
    }

    // ═══════════════════════════════════════════════
    // ADD STUDENT
    // ═══════════════════════════════════════════════
    @GetMapping("/add-student")
    public String addStudentPage(Model model, HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        addAddStudentModel(model, admin);
        return "admin/add-student";
    }

    @PostMapping("/add-student/import-multiple")
    public String importMultipleStudentFiles(@RequestParam("files") MultipartFile[] files,
                                             HttpSession session,
                                             RedirectAttributes redirectAttributes) throws IOException {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        if (files == null || files.length == 0 || Arrays.stream(files).allMatch(file -> file == null || file.isEmpty())) {
            redirectAttributes.addFlashAttribute("importError", "At least one student import file select karein.");
            return "redirect:/add-student";
        }

        List<String> messages = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            Map<String, Object> result = processStudentImport(admin, file, "import");
            String label = file.getOriginalFilename() != null ? file.getOriginalFilename() : "student file";
            if (result.containsKey("error")) {
                errors.add(label + ": " + result.get("error"));
                continue;
            }
            messages.add(label + " -> " + result.get("successCount") + " students imported");
            Number errorCount = (Number) result.get("errorCount");
            if (errorCount != null && errorCount.longValue() > 0) {
                errors.add(label + ": " + errorCount.longValue() + " row(s) skipped");
            }
        }

        if (!messages.isEmpty()) {
            redirectAttributes.addFlashAttribute("importSuccess", String.join(" | ", messages));
        }
        if (!errors.isEmpty()) {
            redirectAttributes.addFlashAttribute("importError", String.join(" | ", errors));
        }
        return "redirect:/add-student";
    }

    @PostMapping("/add-student")
    public String saveStudent(
            @RequestParam("name")                                     String name,
            @RequestParam("email")                                    String email,
            @RequestParam("password")                                 String password,
            @RequestParam("course")                                   String course,

            @RequestParam(value = "academicYear",   required = false) String academicYear,
            @RequestParam(value = "semester",       required = false) String semester,
            @RequestParam(value = "degree",         required = false) String degree,
            @RequestParam(value = "sectionName",    required = false) String sectionName,
            @RequestParam(value = "medium",         required = false) String medium,
            @RequestParam(value = "batchId",        required = false) Long batchId,

            @RequestParam(value = "rollNo",          required = false) String rollNo,
            @RequestParam(value = "enrollmentNo",    required = false) String enrollmentNo,
            @RequestParam(value = "registrationNo",  required = false) String registrationNo,
            @RequestParam(value = "prnNumber",       required = false) String prnNumber,
            @RequestParam(value = "abcNumber",       required = false) String abcNumber,

            @RequestParam(value = "gender",          required = false) String gender,
            @RequestParam(value = "dob",             required = false) String dobStr,
            @RequestParam(value = "bloodGroup",      required = false) String bloodGroup,
            @RequestParam(value = "religion",        required = false) String religion,
            @RequestParam(value = "admissionDate",   required = false) String admissionDateStr,
            @RequestParam(value = "casteName",       required = false) String casteName,
            @RequestParam(value = "category",        required = false) String category,

            @RequestParam(value = "fatherName",      required = false) String fatherName,
            @RequestParam(value = "motherName",      required = false) String motherName,
            @RequestParam(value = "guardianName",    required = false) String guardianName,

            @RequestParam(value = "aadharNumber",    required = false) String aadharNumber,
            @RequestParam(value = "panCardNumber",   required = false) String panCardNumber,
            @RequestParam(value = "voterId",         required = false) String voterId,
            @RequestParam(value = "eidNumber",       required = false) String eidNumber,

            @RequestParam(value = "bankName",        required = false) String bankName,
            @RequestParam(value = "bankAccNo",       required = false) String bankAccNo,
            @RequestParam(value = "ifscCode",        required = false) String ifscCode,
            @RequestParam(value = "micrNumber",      required = false) String micrNumber,

            @RequestParam(value = "photo",           required = false) MultipartFile photoFile,
            @RequestParam(value = "signature",       required = false) MultipartFile signatureFile,

            HttpServletRequest request,
            Model model,
            HttpSession session) {

        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        if (studentRepo.existsByAdminAndEmailIgnoreCase(admin, email)) {
            addStudentFormError(model, admin, "Email already exists");
            return "admin/add-student";
        }

        String normalizedEnrollment = blankToNull(enrollmentNo);
        if (normalizedEnrollment != null && studentRepo.existsByAdminAndEnrollmentNoIgnoreCase(admin, normalizedEnrollment)) {
            addStudentFormError(model, admin, "Enrollment number already taken");
            return "admin/add-student";
        }

        Course selectedCourse = resolveAdminCourseByInput(admin, course);
        Student s = new Student();
        s.setName(name);
        s.setEmail(normalizeEmail(email));
        s.setPassword(passwordProtectionService.encode(password));
        s.setCourse(selectedCourse != null ? selectedCourse.getCode().strip() : blankToNull(course));

        s.setRollNo(blankToNull(rollNo));
        s.setEnrollmentNo(normalizedEnrollment);
        s.setRegistrationNo(blankToNull(registrationNo));
        s.setPrnNumber(blankToNull(prnNumber));
        s.setAbcNumber(blankToNull(abcNumber));

        s.setGender(blankToNull(gender));
        s.setBloodGroup(blankToNull(bloodGroup));
        s.setReligion(blankToNull(religion));
        s.setCasteName(blankToNull(casteName));
        s.setCategory(blankToNull(category));

        if (dobStr != null && !dobStr.isBlank()) {
            try { s.setDob(LocalDate.parse(dobStr)); } catch (Exception ignored) {}
        }
        if (admissionDateStr != null && !admissionDateStr.isBlank()) {
            try { s.setAdmissionDate(LocalDate.parse(admissionDateStr)); } catch (Exception ignored) {}
        }

        s.setFatherName(blankToNull(fatherName));
        s.setMotherName(blankToNull(motherName));
        s.setGuardianName(blankToNull(guardianName));

        s.setAadharNumber(blankToNull(aadharNumber));
        s.setPanCardNumber(panCardNumber != null ? panCardNumber.toUpperCase().strip() : null);
        s.setVoterId(blankToNull(voterId));
        s.setEidNumber(blankToNull(eidNumber));

        s.setBankName(blankToNull(bankName));
        s.setBankAccNo(blankToNull(bankAccNo));
        s.setIfscCode(ifscCode != null ? ifscCode.toUpperCase().strip() : null);
        s.setMicrNumber(blankToNull(micrNumber));

        Batch selectedBatch = getAdminBatch(admin, batchId);
        if (selectedBatch != null) {
            s.setBatch(selectedBatch);
            if (selectedBatch.getCourse() != null && selectedBatch.getCourse().getCode() != null) {
                s.setCourse(selectedBatch.getCourse().getCode());
            }
        } else if (selectedCourse == null) {
            addStudentFormError(model, admin, "Please select a valid course from the course list.");
            return "admin/add-student";
        }

        String selectedAcademicYear = blankToNull(academicYear);
        if (selectedAcademicYear == null && selectedBatch != null && selectedBatch.getStartYear() != null) {
            selectedAcademicYear = selectedBatch.getStartYear() + "-" + String.valueOf(selectedBatch.getStartYear() + 1).substring(2);
        }
        s.setAcademicYear(selectedAcademicYear != null ? selectedAcademicYear : currentAcademicYear());
        s.setSemester(blankToNull(semester));
        String selectedDegree = blankToNull(degree);
        if (selectedDegree == null && selectedCourse != null) {
            selectedDegree = blankToNull(degreeHintForCourse(selectedCourse));
        }
        s.setDegree(selectedDegree);
        s.setSectionName(blankToNull(sectionName));
        s.setMedium(blankToNull(medium));

        ClassRoom resolvedClassRoom = resolveStudentClassRoom(
                admin,
                selectedBatch,
                s.getCourse(),
                s.getAcademicYear(),
                s.getSemester(),
                s.getSectionName()
        );
        if (resolvedClassRoom != null) {
            s.setClassRoom(resolvedClassRoom);
        }

        if (photoFile != null && !photoFile.isEmpty()) {
            String savedPath = savePhoto(photoFile, UPLOAD_DIR_STUDENTS);
            if (savedPath == null) {
                addStudentFormError(model, admin, "Photo upload failed - only JPG/PNG/WEBP allowed (max 2 MB).");
                return "admin/add-student";
            }
            s.setPhoto(savedPath);
        }

        if (signatureFile != null && !signatureFile.isEmpty()) {
            String savedPath = savePhoto(signatureFile, UPLOAD_DIR_STUDENTS);
            if (savedPath == null) {
                addStudentFormError(model, admin, "Signature upload failed - only JPG/PNG/WEBP allowed (max 2 MB).");
                return "admin/add-student";
            }
            s.setSignature(savedPath);
        }

        applyExtendedStudentFields(s, request);

        double total = feesForStudent(
                admin,
                s.getCourse(),
                selectedBatch != null ? selectedBatch.getId() : null,
                selectedBatch != null ? selectedBatch.getDisplayName() : null,
                s.getAcademicYear(),
                s.getSemester()
        );
        s.setTotalFees(total);
        s.setPaidFees(0.0);
        s.setPendingFees(total);
        s.setAdmissionStatus("ADMITTED");

        s.setAdmin(admin);
        studentRepo.save(s);
        ensureApplicationNumber(s);
        return "redirect:/add-student?success=Student " + s.getName() + " added successfully&studentId=" + s.getId();
    }

    // ═══════════════════════════════════════════════
    // VIEW STUDENTS
    // ═══════════════════════════════════════════════
    @GetMapping("/admin/classes/live")
    @ResponseBody
    public List<ClassOptionView> liveClasses(HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return List.of();
        return getSortedAdminClasses(admin).stream()
                .map(this::toClassOptionView)
                .collect(Collectors.toList());
    }

    @GetMapping("/admin-students")
    public String viewStudents(
            @RequestParam(required = false) String course,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) String search,
            Model model,
            HttpSession session,
            HttpServletResponse response) {

        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        List<Student> all      = studentRepo.findByAdmin(admin);
        all.forEach(student -> {
            academicStructureService.syncStudentProgression(student);
            syncStudentFeeTotals(admin, student);
            ensureApplicationNumber(student);
        });
        List<Course> availableCourses = courseRepo.findByAdminOrderByCodeAsc(admin);
        LinkedHashMap<String, Course> courseCatalog = buildCourseCatalog(availableCourses);
        List<Student> filtered = all;
        Batch selectedBatch = getAdminBatch(admin, batchId);
        List<Batch> batches = batchRepo.findByAdminOrderByDisplayNameAsc(admin);
        String selectedCourseCode = resolveCourseCode(course, courseCatalog);

        if (selectedCourseCode != null)
            filtered = filtered.stream()
                    .filter(s -> selectedCourseCode.equalsIgnoreCase(resolveCourseCode(s.getCourse(), courseCatalog)))
                    .collect(Collectors.toList());

        if (selectedBatch != null)
            filtered = filtered.stream()
                    .filter(s -> s.getBatch() != null
                            && s.getBatch().getId() != null
                            && s.getBatch().getId().equals(selectedBatch.getId()))
                    .collect(Collectors.toList());

        if (search != null && !search.isBlank()) {
            String query = search.strip().toLowerCase(Locale.ENGLISH);
            filtered = filtered.stream()
                    .filter(s -> (s.getName() != null && s.getName().toLowerCase(Locale.ENGLISH).contains(query))
                            || (s.getEnrollmentNo() != null && s.getEnrollmentNo().toLowerCase(Locale.ENGLISH).contains(query))
                            || (s.getPrnNumber() != null && s.getPrnNumber().toLowerCase(Locale.ENGLISH).contains(query))
                            || (s.getRollNo() != null && s.getRollNo().toLowerCase(Locale.ENGLISH).contains(query)))
                    .collect(Collectors.toList());
        }

        model.addAttribute("students",         filtered);
        model.addAttribute("totalStudents",    all.size());
        model.addAttribute("classes",          classRepo.findByAdmin(admin));
        model.addAttribute("courseSummaries",  buildStudentCourseSummaries(availableCourses, all));
        model.addAttribute("selectedCourse",   selectedCourseCode);
        model.addAttribute("selectedBatchId",  batchId);
        model.addAttribute("selectedBatch",    selectedBatch);
        model.addAttribute("search",           search);
        model.addAttribute("batchCounts",      all.stream()
                .filter(s -> s.getBatch() != null && s.getBatch().getId() != null)
                .collect(Collectors.groupingBy(s -> s.getBatch().getId(), Collectors.counting())));
        model.addAttribute("batches",          batches);
        model.addAttribute("courseOptions",    availableCourses);
        addAdminAttributes(model, admin);
        return "admin/admin-students";
    }

    @GetMapping("/admin-students/{id}/documents")
    public String viewStudentDocuments(@PathVariable Long id,
                                       Model model,
                                       HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        Student student = studentRepo.findById(id).orElse(null);
        if (student == null || student.getAdmin() == null || !student.getAdmin().getId().equals(admin.getId())) {
            return "redirect:/admin-students";
        }

        List<StudentDocument> documents = studentDocumentRepo.findByStudentIdOrderByDocumentNameAsc(student.getId());
        model.addAttribute("student", student);
        model.addAttribute("documents", documents);
        model.addAttribute("pendingReuploadDocs", documents.stream()
                .filter(doc -> "REUPLOAD_REQUESTED".equalsIgnoreCase(blankToNull(doc.getStatus())))
                .collect(Collectors.toList()));
        addAdminAttributes(model, admin);
        return "admin/student-documents";
    }

    @PostMapping("/admin-students/{studentId}/documents/{docId}/verify")
    public String verifyStudentDocument(@PathVariable Long studentId,
                                       @PathVariable Long docId,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        studentDocumentRepo.findById(docId).ifPresent(document -> {
            if (document.getStudent() == null || document.getStudent().getAdmin() == null
                    || !document.getStudent().getAdmin().getId().equals(admin.getId())
                    || !document.getStudent().getId().equals(studentId)) {
                return;
            }
            document.setStatus("VERIFIED");
            document.setReviewedBy(admin);
            document.setReviewedAt(LocalDateTime.now());
            studentDocumentRepo.save(document);
        });

        redirectAttributes.addFlashAttribute("studentSuccess", "Document verified successfully.");
        return "redirect:/admin-students/" + studentId + "/documents";
    }

    @PostMapping("/admin-students/{studentId}/documents/{docId}/request-reupload")
    public String requestStudentDocumentReupload(@PathVariable Long studentId,
                                                 @PathVariable Long docId,
                                                 @RequestParam(required = false) String remarks,
                                                 HttpSession session,
                                                 RedirectAttributes redirectAttributes) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        studentDocumentRepo.findById(docId).ifPresent(document -> {
            if (document.getStudent() == null || document.getStudent().getAdmin() == null
                    || !document.getStudent().getAdmin().getId().equals(admin.getId())
                    || !document.getStudent().getId().equals(studentId)) {
                return;
            }
            document.setStatus("REUPLOAD_REQUESTED");
            document.setRemarks(blankToNull(remarks));
            document.setReviewedBy(admin);
            document.setReviewedAt(LocalDateTime.now());
            studentDocumentRepo.save(document);
        });

        redirectAttributes.addFlashAttribute("studentSuccess", "Reupload request sent.");
        return "redirect:/admin-students/" + studentId + "/documents";
    }

    @GetMapping("/admin/students/import/template")
    public void downloadStudentImportTemplate(HttpSession session, HttpServletResponse response) throws IOException {
        if (getLoggedAdmin(session) == null) {
            response.sendRedirect("/login-admin");
            return;
        }
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=student-import-template.csv");
        String csv = String.join(",", STUDENT_IMPORT_HEADERS.stream().map(this::csvValue).toList()) + "\n";
        response.getOutputStream().write(csv.getBytes(StandardCharsets.UTF_8));
    }

    @PostMapping("/admin/students/import")
    @ResponseBody
    public Map<String, Object> importStudents(@RequestParam("file") MultipartFile file,
                                              @RequestParam(value = "mode", defaultValue = "preview") String mode,
                                              HttpSession session) throws IOException {
        Admin admin = getLoggedAdmin(session);
        return processStudentImport(admin, file, mode);
    }

    @PostMapping("/update-student/{id}")
    public Object updateStudent(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam(required = false) String course,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) String rollNo,
            @RequestParam(required = false) String enrollmentNo,
            @RequestParam(required = false) String registrationNo,
            @RequestParam(required = false) String prnNumber,
            @RequestParam(required = false) String degree,
            @RequestParam(required = false) String sectionName,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long classRoomId,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            @RequestHeader(value = "X-Requested-With", required = false) String requestedWith) {
        Admin admin = getLoggedAdmin(session);
        boolean ajaxRequest = "XMLHttpRequest".equalsIgnoreCase(requestedWith);
        if (admin == null) {
            return ajaxRequest
                    ? ResponseEntity.status(401).body(Map.of("success", false, "message", "Admin session expired. Please log in again."))
                    : "redirect:/login-admin";
        }

        String normalizedEmail = blankToNull(email);
        if (normalizedEmail == null) {
            return ajaxRequest
                    ? ResponseEntity.badRequest().body(Map.of("success", false, "message", "Email is required"))
                    : "redirect:/admin-students";
        }
        if (studentRepo.existsByAdminAndEmailIgnoreCaseAndIdNot(admin, normalizedEmail, id)) {
            return ajaxRequest
                    ? ResponseEntity.badRequest().body(Map.of("success", false, "message", "Email already exists"))
                    : "redirect:/admin-students";
        }

        String normalizedEnrollment = blankToNull(enrollmentNo);
        if (normalizedEnrollment != null && studentRepo.existsByAdminAndEnrollmentNoIgnoreCaseAndIdNot(admin, normalizedEnrollment, id)) {
            return ajaxRequest
                    ? ResponseEntity.badRequest().body(Map.of("success", false, "message", "Enrollment number already exists"))
                    : "redirect:/admin-students";
        }

        Student persistedStudent = studentRepo.findById(id).map(student -> {
            if (student.getAdmin() != null && !student.getAdmin().getId().equals(admin.getId())) {
                return null;
            }
            Course resolvedCourse = resolveStudentEditCourse(admin, course);
            String normalizedCourse = blankToNull(resolvedCourse != null ? resolvedCourse.getName() : course);
            String normalizedSemester = normalizeSemesterLabel(semester);
            String normalizedAcademicYear = normalizeAcademicYearValue(academicYear);
            String normalizedSection = normalizeSectionValue(sectionName);
            Batch selectedBatch = resolveStudentEditBatch(admin, resolvedCourse, normalizedAcademicYear, student.getBatch());

            student.setName(blankToNull(name));
            student.setEmail(normalizeEmail(normalizedEmail));
            student.setCourse(normalizedCourse);
            student.setSemester(normalizedSemester);
            student.setAcademicYear(normalizedAcademicYear);
            student.setRollNo(blankToNull(rollNo));
            student.setEnrollmentNo(normalizedEnrollment);
            student.setRegistrationNo(blankToNull(registrationNo));
            student.setPrnNumber(blankToNull(prnNumber));
            student.setDegree(blankToNull(degree));
            student.setSectionName(normalizedSection);
            student.setGender(blankToNull(gender));
            student.setCategory(blankToNull(category));
            student.setBatch(selectedBatch);
            if (classRoomId != null) {
                ClassRoom selectedClassRoom = classRepo.findById(classRoomId).orElse(null);
                if (selectedClassRoom != null && classRoomMatchesStudentEditScope(
                        selectedClassRoom,
                        selectedBatch,
                        student.getCourse(),
                        student.getAcademicYear(),
                        student.getSemester(),
                        student.getSectionName()
                )) {
                    student.setClassRoom(selectedClassRoom);
                } else {
                    ClassRoom resolvedClassRoom = resolveStudentClassRoom(
                            admin,
                            selectedBatch,
                            student.getCourse(),
                            student.getAcademicYear(),
                            student.getSemester(),
                            student.getSectionName()
                    );
                    student.setClassRoom(resolvedClassRoom);
                }
            } else {
                ClassRoom resolvedClassRoom = resolveStudentClassRoom(
                        admin,
                        selectedBatch,
                        student.getCourse(),
                        student.getAcademicYear(),
                        student.getSemester(),
                        student.getSectionName()
                );
                student.setClassRoom(resolvedClassRoom);
            }
            studentRepo.save(student);
            ensureApplicationNumber(student);
            academicStructureService.syncStudentProgression(student);
            return studentRepo.findById(student.getId()).orElse(student);
        }).orElse(null);

        if (persistedStudent == null) {
            return ajaxRequest
                    ? ResponseEntity.badRequest().body(Map.of("success", false, "message", "Student record could not be updated."))
                    : "redirect:/admin-students";
        }

        if (ajaxRequest) {
            return ResponseEntity.ok(buildStudentUpdateResponse(persistedStudent));
        }

        redirectAttributes.addFlashAttribute("studentSuccess", "Student record updated successfully.");
        return "redirect:/admin-students";
    }

    private Map<String, Object> buildStudentUpdateResponse(Student student) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Student record updated successfully.");
        response.put("student", Map.ofEntries(
                Map.entry("id", student.getId()),
                Map.entry("name", defaultText(student.getName(), "Student")),
                Map.entry("email", defaultText(student.getEmail(), "Not available")),
                Map.entry("course", defaultText(student.getCourse(), "Not assigned")),
                Map.entry("semester", defaultText(student.getSemester(), "Not assigned")),
                Map.entry("academicYear", defaultText(student.getAcademicYear(), "Not assigned")),
                Map.entry("rollNo", defaultText(student.getRollNo(), "Not assigned")),
                Map.entry("enrollmentNo", defaultText(student.getEnrollmentNo(), "Not assigned")),
                Map.entry("registrationNo", defaultText(student.getRegistrationNo(), "Not assigned")),
                Map.entry("prnNumber", defaultText(student.getPrnNumber(), "Not assigned")),
                Map.entry("degree", defaultText(student.getDegree(), "Not assigned")),
                Map.entry("section", defaultText(student.getSectionName(), "Not assigned")),
                Map.entry("gender", defaultText(student.getGender(), "Not provided")),
                Map.entry("category", defaultText(student.getCategory(), "Not provided")),
                Map.entry("status", defaultText(student.getAdmissionStatus(), "ADMITTED")),
                Map.entry("photo", defaultText(student.getPhoto(), "")),
                Map.entry("classRoomId", student.getClassRoom() != null && student.getClassRoom().getId() != null ? String.valueOf(student.getClassRoom().getId()) : ""),
                Map.entry("academicMapping", buildStudentAcademicMappingLabel(student))
        ));
        return response;
    }

    private String buildStudentAcademicMappingLabel(Student student) {
        if (student == null) {
            return "Not assigned";
        }
        if (student.getClassRoom() != null
                && classRoomMatchesStudentEditScope(
                        student.getClassRoom(),
                        student.getBatch(),
                        student.getCourse(),
                        student.getAcademicYear(),
                        student.getSemester(),
                        student.getSectionName()
                )
                && blankToNull(student.getClassRoom().getName()) != null) {
            return student.getClassRoom().getName();
        }
        List<String> parts = new ArrayList<>();
        if (blankToNull(student.getCourse()) != null) {
            parts.add(student.getCourse());
        }
        if (blankToNull(student.getSemester()) != null) {
            parts.add(student.getSemester());
        }
        if (blankToNull(student.getSectionName()) != null) {
            parts.add(student.getSectionName());
        }
        return parts.isEmpty() ? "Not assigned" : String.join(" - ", parts);
    }

    // ═══════════════════════════════════════════════
    // DELETE STUDENT
    // ═══════════════════════════════════════════════
    @PostMapping("/admin-students/{id}/approve")
    public String approveStudent(@PathVariable Long id,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        studentRepo.findById(id).ifPresent(student -> {
            if (student.getAdmin() != null && !student.getAdmin().getId().equals(admin.getId())) {
                return;
            }
            student.setAdmin(admin);
            student.setAdmissionStatus("ADMITTED");
            if (student.getAdmissionDate() == null) {
                student.setAdmissionDate(LocalDate.now());
            }
            if (student.getClassRoom() == null) {
                ClassRoom resolvedClassRoom = resolveStudentClassRoom(
                        admin,
                        student.getBatch(),
                        student.getCourse(),
                        student.getAcademicYear(),
                        student.getSemester(),
                        student.getSectionName()
                );
                if (resolvedClassRoom != null) {
                    student.setClassRoom(resolvedClassRoom);
                }
            }
            syncStudentFeeTotals(admin, student);
            studentRepo.save(student);
        });

        redirectAttributes.addFlashAttribute("studentSuccess", "Student admission approved.");
        return "redirect:/admin-students";
    }

    @PostMapping("/admin-students/{id}/reject")
    public String rejectStudent(@PathVariable Long id,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        studentRepo.findById(id).ifPresent(student -> {
            if (student.getAdmin() != null && !student.getAdmin().getId().equals(admin.getId())) {
                return;
            }
            student.setAdmin(admin);
            student.setAdmissionStatus("REJECTED");
            studentRepo.save(student);
        });

        redirectAttributes.addFlashAttribute("studentSuccess", "Student admission rejected.");
        return "redirect:/admin-students";
    }

    @Transactional
    @GetMapping("/delete-student/{id}")
    public String deleteStudent(@PathVariable Long id, HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        studentRepo.findById(id).ifPresent(student -> {
            if (student.getAdmin() != null && !student.getAdmin().getId().equals(admin.getId())) {
                return;
            }

            deleteStudentDependencies(student);
            studentRepo.delete(student);
        });
        return "redirect:/admin-students";
    }

    // ═══════════════════════════════════════════════
    // ADD TEACHER
    // ═══════════════════════════════════════════════
    @GetMapping("/add-teacher")
    public String addTeacherPage(Model model, HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";
        model.addAttribute("nextTeacherEmployeeId", employeeIdService.nextTeacherEmployeeId(admin));
        addAdminAttributes(model, admin);
        return "admin/add-teacher";
    }

    @GetMapping("/admin/employee-ids/next")
    @ResponseBody
    public ResponseEntity<Map<String, String>> nextAvailableEmployeeId(@RequestParam String type,
                                                                       HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Login required"));
        }
        String normalizedType = type == null ? "" : type.trim().toLowerCase(Locale.ENGLISH);
        if ("teacher".equals(normalizedType)) {
            return ResponseEntity.ok(Map.of("employeeId", employeeIdService.nextTeacherEmployeeId(admin)));
        }
        if ("tpo".equals(normalizedType)) {
            return ResponseEntity.ok(Map.of("employeeId", employeeIdService.nextTpoEmployeeId(admin)));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Unsupported employee type"));
    }

    @PostMapping("/add-teacher/import-multiple")
    public String importMultipleTeacherFiles(@RequestParam("files") MultipartFile[] files,
                                             HttpSession session,
                                             RedirectAttributes redirectAttributes) throws IOException {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        if (files == null || files.length == 0 || Arrays.stream(files).allMatch(file -> file == null || file.isEmpty())) {
            redirectAttributes.addFlashAttribute("importError", "At least one teacher import file select karein.");
            return "redirect:/add-teacher";
        }

        List<String> messages = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            Map<String, Object> result = processTeacherImport(admin, file, "import");
            String label = file.getOriginalFilename() != null ? file.getOriginalFilename() : "teacher file";
            if (result.containsKey("error")) {
                errors.add(label + ": " + result.get("error"));
                continue;
            }
            messages.add(label + " -> " + result.get("successCount") + " teachers imported");
            Number errorCount = (Number) result.get("errorCount");
            if (errorCount != null && errorCount.longValue() > 0) {
                errors.add(label + ": " + errorCount.longValue() + " row(s) skipped");
            }
        }

        if (!messages.isEmpty()) {
            redirectAttributes.addFlashAttribute("importSuccess", String.join(" | ", messages));
        }
        if (!errors.isEmpty()) {
            redirectAttributes.addFlashAttribute("importError", String.join(" | ", errors));
        }
        return "redirect:/add-teacher";
    }

    @PostMapping("/add-teacher")
    public String saveTeacher(
            @RequestParam("name")                                         String name,
            @RequestParam("email")                                        String email,
            @RequestParam("password")                                     String password,

            @RequestParam(value = "gender",            required = false)  String gender,
            @RequestParam(value = "dob",               required = false)  String dobStr,
            @RequestParam(value = "bloodGroup",        required = false)  String bloodGroup,
            @RequestParam(value = "phone",             required = false)  String phone,
            @RequestParam(value = "altPhone",          required = false)  String altPhone,
            @RequestParam(value = "maritalStatus",     required = false)  String maritalStatus,
            @RequestParam(value = "religion",          required = false)  String religion,
            @RequestParam(value = "casteName",         required = false)  String casteName,
            @RequestParam(value = "category",          required = false)  String category,
            @RequestParam(value = "address",           required = false)  String address,
            @RequestParam(value = "city",              required = false)  String city,
            @RequestParam(value = "state",             required = false)  String state,

            @RequestParam(value = "designation",       required = false)  String designation,
            @RequestParam(value = "joiningDate",       required = false)  String joiningDateStr,
            @RequestParam(value = "experience",        required = false)  String experience,
            @RequestParam(value = "employmentType",    required = false)  String employmentType,
            @RequestParam(value = "salary",            required = false)  Double salary,
            @RequestParam(value = "specialization",    required = false)  String specialization,
            @RequestParam(value = "department",        required = false)  String department,
            @RequestParam(value = "status",            required = false)  String status,

            @RequestParam(value = "qualification",         required = false) String qualification,
            @RequestParam(value = "degreeSpecialization",  required = false) String degreeSpecialization,
            @RequestParam(value = "university",            required = false) String university,
            @RequestParam(value = "yearOfPassing",         required = false) Integer yearOfPassing,
            @RequestParam(value = "publications",          required = false) Integer publications,

            @RequestParam(value = "aadharNumber",      required = false)  String aadharNumber,
            @RequestParam(value = "panCardNumber",     required = false)  String panCardNumber,
            @RequestParam(value = "voterId",           required = false)  String voterId,
            @RequestParam(value = "passportNumber",    required = false)  String passportNumber,

            @RequestParam(value = "bankName",          required = false)  String bankName,
            @RequestParam(value = "bankAccNo",         required = false)  String bankAccNo,
            @RequestParam(value = "ifscCode",          required = false)  String ifscCode,
            @RequestParam(value = "pfNumber",          required = false)  String pfNumber,

            @RequestParam(value = "emergencyContactName", required = false) String emergencyContactName,
            @RequestParam(value = "emergencyRelation",    required = false) String emergencyRelation,
            @RequestParam(value = "emergencyPhone",       required = false) String emergencyPhone,

            @RequestParam(value = "employeeId",        required = false)  String employeeId,
            @RequestParam(value = "photo",             required = false)  MultipartFile photoFile,

            Model model,
            HttpSession session) {

        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        if (teacherRepo.findByEmail(email) != null) {
            model.addAttribute("error", "A teacher with this email already exists!");
            model.addAttribute("nextTeacherEmployeeId", employeeIdService.nextTeacherEmployeeId(admin));
            addAdminAttributes(model, admin);
            return "admin/add-teacher";
        }

        Teacher t = new Teacher();
        t.setName(name);
        t.setEmail(email);
        t.setPassword(passwordProtectionService.encode(password));

        t.setGender(blankToNull(gender));
        t.setBloodGroup(blankToNull(bloodGroup));
        t.setPhone(blankToNull(phone));
        t.setAltPhone(blankToNull(altPhone));
        t.setMaritalStatus(blankToNull(maritalStatus));
        t.setReligion(blankToNull(religion));
        t.setCasteName(blankToNull(casteName));
        t.setCategory(blankToNull(category));
        t.setAddress(blankToNull(address));
        t.setCity(blankToNull(city));
        t.setState(blankToNull(state));

        if (dobStr != null && !dobStr.isBlank()) {
            try { t.setDob(LocalDate.parse(dobStr)); } catch (Exception ignored) {}
        }

        t.setDesignation(blankToNull(designation));
        t.setEmploymentType(blankToNull(employmentType));
        t.setSpecialization(blankToNull(specialization));
        t.setExperience(blankToNull(experience));
        t.setDepartment(blankToNull(department));
        t.setStatus(status != null && !status.isBlank() ? status : "Active");
        if (salary != null) t.setSalary(salary);

        if (joiningDateStr != null && !joiningDateStr.isBlank()) {
            try { t.setJoiningDate(LocalDate.parse(joiningDateStr)); } catch (Exception ignored) {}
        }

        t.setQualification(blankToNull(qualification));
        t.setDegreeSpecialization(blankToNull(degreeSpecialization));
        t.setUniversity(blankToNull(university));
        if (yearOfPassing != null) t.setYearOfPassing(yearOfPassing);
        if (publications  != null) t.setPublications(publications);

        t.setAadharNumber(blankToNull(aadharNumber));
        t.setPanCardNumber(panCardNumber != null ? panCardNumber.toUpperCase().strip() : null);
        t.setVoterId(blankToNull(voterId));
        t.setPassportNumber(passportNumber != null ? passportNumber.toUpperCase().strip() : null);

        t.setBankName(blankToNull(bankName));
        t.setBankAccNo(blankToNull(bankAccNo));
        t.setIfscCode(ifscCode != null ? ifscCode.toUpperCase().strip() : null);
        t.setPfNumber(blankToNull(pfNumber));

        t.setEmergencyContactName(blankToNull(emergencyContactName));
        t.setEmergencyRelation(blankToNull(emergencyRelation));
        t.setEmergencyPhone(blankToNull(emergencyPhone));

        if (photoFile != null && !photoFile.isEmpty()) {
            String savedPath = savePhoto(photoFile, UPLOAD_DIR_TEACHERS);
            if (savedPath == null) {
                model.addAttribute("error", "Photo upload failed - only JPG/PNG/WEBP allowed (max 2 MB).");
                model.addAttribute("nextTeacherEmployeeId", employeeIdService.nextTeacherEmployeeId(admin));
                addAdminAttributes(model, admin);
                return "admin/add-teacher";
            }
            t.setPhoto(savedPath);
        }

        t.setAdmin(admin);
        employeeIdService.saveNewTeacherWithEmployeeId(t, employeeId);
        return "redirect:/admin-teachers";
    }

    // ═══════════════════════════════════════════════
    // VIEW TEACHERS
    // ═══════════════════════════════════════════════
    @GetMapping("/admin-teachers")
    public String viewTeachers(
            Model model,
            HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";
        List<Teacher> allTeachers = employeeIdService.ensureTeacherEmployeeIds(admin);
        model.addAttribute("classes", classRepo.findByAdmin(admin));
        model.addAttribute("teachers",      allTeachers);
        model.addAttribute("totalTeachers", allTeachers.size());
        addAdminAttributes(model, admin);
        return "admin/admin-teachers";
    }

    @GetMapping("/admin-teacher-assignment")
    public String teacherAssignmentPage(Model model, HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";
        model.addAttribute("teachers", employeeIdService.ensureTeacherEmployeeIds(admin));
        model.addAttribute("courses", courseRepo.findByAdminOrderByCodeAsc(admin));
        model.addAttribute("batches", batchRepo.findByAdminOrderByDisplayNameAsc(admin));
        model.addAttribute("academicYears", academicYearsFromBatches(admin));
        model.addAttribute("mappings", teacherAcademicMappingService.findByAdmin(admin));
        addAdminAttributes(model, admin);
        return "admin/teacher-assignment";
    }

    @PostMapping("/teacher-assignment")
    public Object saveTeacherAssignment(
            @RequestParam Long teacherId,
            @RequestParam Long courseId,
            @RequestParam Long batchId,
            @RequestParam String academicYear,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) {
            return wantsJson(request)
                    ? ResponseEntity.status(401).body(Map.of("success", false, "message", "Admin session expired. Please log in again."))
                    : "redirect:/login-admin";
        }
        Teacher teacher = teacherRepo.findById(teacherId).orElse(null);
        Course course = courseRepo.findById(courseId).orElse(null);
        Batch batch = batchRepo.findById(batchId).orElse(null);
        TeacherAcademicMappingService.AssignmentResult result = teacherAcademicMappingService.assign(
                admin, teacher, course, batch, academicYear);
        if (wantsJson(request)) {
            if (result.savedCount() <= 0 || !result.errors().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", result.errors().isEmpty() ? "Unable to save teacher assignment." : String.join(" ", result.errors()),
                        "assignments", teacherAcademicMappingService.findByAdmin(admin).stream().map(this::teacherMappingView).toList()
                ));
            }
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Teacher assignment saved. Subjects, semesters, sections, students, and timetable will sync automatically.",
                    "assignments", teacherAcademicMappingService.findByAdmin(admin).stream().map(this::teacherMappingView).toList()
            ));
        }
        if (result.savedCount() > 0) {
            redirectAttributes.addFlashAttribute("teacherAssignmentSuccess", "Teacher assignment saved. Subjects, semesters, sections, students, and timetable will sync automatically.");
        }
        if (!result.errors().isEmpty()) {
            redirectAttributes.addFlashAttribute("teacherAssignmentError", String.join(" ", result.errors()));
        }
        return "redirect:/admin-teacher-assignment";
    }

    @PostMapping("/teacher-assignment/{mappingId}/delete")
    public Object deleteTeacherAssignment(@PathVariable Long mappingId, HttpSession session, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) {
            return wantsJson(request)
                    ? ResponseEntity.status(401).body(Map.of("success", false, "message", "Admin session expired. Please log in again."))
                    : "redirect:/login-admin";
        }
        TeacherAcademicMapping mapping = teacherAcademicMappingRepo.findById(mappingId).orElse(null);
        if (mapping == null || mapping.getAdmin() == null || !mapping.getAdmin().getId().equals(admin.getId())) {
            if (wantsJson(request)) {
                return ResponseEntity.status(404).body(Map.of(
                        "success", false,
                        "message", "Teacher assignment was not found.",
                        "assignments", teacherAcademicMappingService.findByAdmin(admin).stream().map(this::teacherMappingView).toList()
                ));
            }
            redirectAttributes.addFlashAttribute("teacherAssignmentError", "Teacher assignment was not found.");
            return "redirect:/admin-teacher-assignment";
        }
        teacherAcademicMappingRepo.delete(mapping);
        if (wantsJson(request)) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Teacher assignment removed. Access updates automatically.",
                    "assignments", teacherAcademicMappingService.findByAdmin(admin).stream().map(this::teacherMappingView).toList()
            ));
        }
        redirectAttributes.addFlashAttribute("teacherAssignmentSuccess", "Teacher assignment removed. Access updates automatically.");
        return "redirect:/admin-teacher-assignment";
    }

    @GetMapping("/teacher-assignment/{teacherId}")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> teacherAssignmentsApi(@PathVariable Long teacherId, HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return ResponseEntity.status(401).build();
        Teacher teacher = teacherRepo.findById(teacherId).orElse(null);
        if (teacher == null || teacher.getAdmin() == null || !teacher.getAdmin().getId().equals(admin.getId())) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(teacherAcademicMappingService.findByTeacher(teacher).stream()
                .map(this::teacherMappingView)
                .toList());
    }

    @GetMapping("/teacher-subjects/{teacherId}")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> teacherSubjectsApi(@PathVariable Long teacherId, HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return ResponseEntity.status(401).build();
        Teacher teacher = teacherRepo.findById(teacherId).orElse(null);
        if (teacher == null || teacher.getAdmin() == null || !teacher.getAdmin().getId().equals(admin.getId())) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(teacherAcademicMappingService.subjectsForTeacher(teacher).stream()
                .map(this::subjectView)
                .toList());
    }

    @GetMapping("/teacher-students/{teacherId}")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> teacherStudentsApi(@PathVariable Long teacherId, HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return ResponseEntity.status(401).build();
        Teacher teacher = teacherRepo.findById(teacherId).orElse(null);
        if (teacher == null || teacher.getAdmin() == null || !teacher.getAdmin().getId().equals(admin.getId())) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(teacherAcademicMappingService.studentsForTeacher(teacher).stream()
                .map(this::studentView)
                .toList());
    }

    @GetMapping("/teacher-dashboard/{teacherId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> teacherDashboardApi(@PathVariable Long teacherId, HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return ResponseEntity.status(401).build();
        Teacher teacher = teacherRepo.findById(teacherId).orElse(null);
        if (teacher == null || teacher.getAdmin() == null || !teacher.getAdmin().getId().equals(admin.getId())) {
            return ResponseEntity.notFound().build();
        }
        TeacherAcademicMappingService.TeacherScopeView scope = teacherAcademicMappingService.primaryScope(teacher);
        List<Student> students = teacherAcademicMappingService.studentsForTeacher(teacher);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("teacherId", teacher.getId());
        body.put("teacherName", teacher.getName());
        body.put("course", scope.course() != null ? scope.course().getCode() : null);
        body.put("faculty", scope.course() != null ? scope.course().getName() : null);
        body.put("batch", scope.batch() != null ? scope.batch().getDisplayName() : null);
        body.put("academicYear", scope.academicYear());
        body.put("semester", "All semesters");
        body.put("section", "All sections");
        body.put("roomNumber", null);
        body.put("subjectCount", scope.subjects().size());
        body.put("studentStrength", students.size());
        body.put("subjects", scope.subjects().stream().map(this::subjectView).toList());
        body.put("academicStructure", teacherAcademicMappingService.academicStructuresForTeacher(teacher).stream()
                .map(row -> Map.<String, Object>of(
                        "semester", row.getSemesterNumber(),
                        "section", row.getSection(),
                        "academicYear", row.getYearLabel()
                ))
                .toList());
        body.put("students", students.stream().map(this::studentView).toList());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/teacher-assignment/subjects")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> teacherAssignmentSubjects(
            @RequestParam Long courseId,
            @RequestParam Long batchId,
            @RequestParam Integer semester,
            HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return ResponseEntity.status(401).build();
        Course course = courseRepo.findById(courseId).orElse(null);
        Batch batch = batchRepo.findById(batchId).orElse(null);
        if (course == null || batch == null || course.getAdmin() == null || batch.getAdmin() == null
                || !course.getAdmin().getId().equals(admin.getId()) || !batch.getAdmin().getId().equals(admin.getId())) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(teacherAcademicMappingService.subjectsForScope(admin, course, batch, semester).stream()
                .map(this::subjectView)
                .toList());
    }

    private Map<String, Object> teacherMappingView(TeacherAcademicMapping mapping) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", mapping.getId());
        row.put("teacherId", mapping.getTeacher() != null ? mapping.getTeacher().getId() : null);
        row.put("teacherName", mapping.getTeacher() != null ? mapping.getTeacher().getName() : null);
        row.put("courseId", mapping.getCourse() != null ? mapping.getCourse().getId() : null);
        row.put("course", mapping.getCourse() != null ? mapping.getCourse().getCode() : null);
        row.put("courseName", mapping.getCourse() != null ? mapping.getCourse().getName() : null);
        row.put("batchId", mapping.getBatch() != null ? mapping.getBatch().getId() : null);
        row.put("batch", mapping.getBatch() != null ? mapping.getBatch().getDisplayName() : null);
        row.put("academicYear", mapping.getAcademicYear());
        row.put("semester", mapping.getSemester());
        row.put("section", mapping.getSection());
        row.put("roomNumber", mapping.getRoomNumber());
        row.put("subject", mapping.getSubject() != null ? subjectView(mapping.getSubject()) : null);
        return row;
    }

    private Map<String, Object> subjectView(Subject subject) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", subject.getId());
        row.put("code", subject.getCode());
        row.put("name", subject.getName());
        row.put("semester", subject.getSemester());
        row.put("credits", subject.getCredits());
        return row;
    }

    private Map<String, Object> studentView(Student student) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", student.getId());
        row.put("studentId", student.getEnrollmentNo() != null && !student.getEnrollmentNo().isBlank() ? student.getEnrollmentNo() : student.getId());
        row.put("fullName", student.getName());
        row.put("email", student.getEmail());
        row.put("course", student.getCourse());
        row.put("semester", student.getSemester());
        row.put("section", student.getSectionName());
        row.put("status", student.getAdmissionStatus() != null ? student.getAdmissionStatus() : "Active");
        return row;
    }

    private boolean wantsJson(HttpServletRequest request) {
        if (request == null) return false;
        String requestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        return "XMLHttpRequest".equalsIgnoreCase(requestedWith)
                || (accept != null && accept.toLowerCase(Locale.ENGLISH).contains("application/json"));
    }

    @GetMapping("/admin/tpos/add")
    public String addTpoPage(Model model, HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";
        model.addAttribute("nextTpoEmployeeId", employeeIdService.nextTpoEmployeeId(admin));
        addAdminAttributes(model, admin);
        return "admin/add-tpo";
    }

    @PostMapping("/admin/tpos")
    public String saveTpo(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam String password,
            @RequestParam String department,
            @RequestParam(value = "employeeId", required = false) String employeeId,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        String cleanName = blankToNull(name);
        String cleanEmail = blankToNull(email);
        String cleanPhone = blankToNull(phone);
        String cleanPassword = blankToNull(password);
        String cleanDepartment = blankToNull(department);

        if (cleanName == null || cleanEmail == null || cleanPhone == null || cleanPassword == null
                || cleanDepartment == null) {
            model.addAttribute("error", "All TPO fields are required.");
            model.addAttribute("nextTpoEmployeeId", employeeIdService.nextTpoEmployeeId(admin));
            addAdminAttributes(model, admin);
            return "admin/add-tpo";
        }

        if (placementTpoRepo.existsByEmailIgnoreCase(cleanEmail)) {
            model.addAttribute("error", "A TPO account with this email already exists.");
            model.addAttribute("nextTpoEmployeeId", employeeIdService.nextTpoEmployeeId(admin));
            addAdminAttributes(model, admin);
            return "admin/add-tpo";
        }

        PlacementTpo tpo = new PlacementTpo();
        tpo.setName(cleanName);
        tpo.setEmail(cleanEmail);
        tpo.setPhone(cleanPhone);
        tpo.setPassword(passwordProtectionService.encode(cleanPassword));
        tpo.setDepartment(cleanDepartment);
        tpo.setDesignation("Training and Placement Officer");
        tpo.setStatus("Active");
        tpo.setAdmin(admin);
        tpo.setCollege(admin.getCollege());
        tpo.setCollegeName(admin.getCollegeName());
        employeeIdService.saveNewTpoWithEmployeeId(tpo, employeeId);

        redirectAttributes.addFlashAttribute("tpoSuccess", "TPO account created. They can now use Institute Login.");
        return "redirect:/admin/tpos";
    }

    @GetMapping("/admin/tpos")
    public String viewTpos(Model model, HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        List<PlacementTpo> tpos = employeeIdService.ensureTpoEmployeeIds(admin);
        model.addAttribute("tpos", tpos);
        model.addAttribute("totalTpos", tpos.size());
        model.addAttribute("activeTpos", tpos.stream()
                .filter(tpo -> tpo.getStatus() == null || !"Inactive".equalsIgnoreCase(tpo.getStatus()))
                .count());
        model.addAttribute("departments", tpos.stream()
                .map(PlacementTpo::getDepartment)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .count());
        addAdminAttributes(model, admin);
        return "admin/admin-tpos";
    }

    @GetMapping("/admin/teachers/import/template")
    public void downloadTeacherImportTemplate(HttpSession session, HttpServletResponse response) throws IOException {
        if (getLoggedAdmin(session) == null) {
            response.sendRedirect("/login-admin");
            return;
        }
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=teacher-import-template.csv");
        String csv = String.join(",", TEACHER_IMPORT_HEADERS.stream().map(this::csvValue).toList()) + "\n";
        response.getOutputStream().write(csv.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/admin-import-center")
    public String adminImportCenter(Model model, HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";
        addAdminAttributes(model, admin);
        return "admin/admin-import-center";
    }

    @PostMapping("/admin-import-center/import")
    public String importCenterSubmit(@RequestParam(value = "studentFile", required = false) MultipartFile studentFile,
                                     @RequestParam(value = "teacherFile", required = false) MultipartFile teacherFile,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) throws IOException {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        boolean hasStudentFile = studentFile != null && !studentFile.isEmpty();
        boolean hasTeacherFile = teacherFile != null && !teacherFile.isEmpty();
        if (!hasStudentFile && !hasTeacherFile) {
            redirectAttributes.addFlashAttribute("importError", "At least one student or teacher file upload karein.");
            return "redirect:/admin-import-center";
        }

        List<String> messages = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        if (hasStudentFile) {
            Map<String, Object> result = processStudentImport(admin, studentFile, "import");
            if (result.containsKey("error")) {
                errors.add("Students: " + result.get("error"));
            } else {
                messages.add(result.get("successCount") + " students imported");
                Number errorCount = (Number) result.get("errorCount");
                if (errorCount != null && errorCount.longValue() > 0) {
                    errors.add("Students: " + errorCount.longValue() + " row(s) skipped due to validation errors");
                }
            }
        }

        if (hasTeacherFile) {
            Map<String, Object> result = processTeacherImport(admin, teacherFile, "import");
            if (result.containsKey("error")) {
                errors.add("Teachers: " + result.get("error"));
            } else {
                messages.add(result.get("successCount") + " teachers imported");
                Number errorCount = (Number) result.get("errorCount");
                if (errorCount != null && errorCount.longValue() > 0) {
                    errors.add("Teachers: " + errorCount.longValue() + " row(s) skipped due to validation errors");
                }
            }
        }

        if (!messages.isEmpty()) {
            redirectAttributes.addFlashAttribute("importSuccess", String.join(" | ", messages));
        }
        if (!errors.isEmpty()) {
            redirectAttributes.addFlashAttribute("importError", String.join(" | ", errors));
        }
        return "redirect:/admin-import-center";
    }

    @PostMapping("/admin/teachers/import")
    @ResponseBody
    public Map<String, Object> importTeachers(@RequestParam("file") MultipartFile file,
                                              @RequestParam(value = "mode", defaultValue = "preview") String mode,
                                              HttpSession session) throws IOException {
        Admin admin = getLoggedAdmin(session);
        return processTeacherImport(admin, file, mode);
    }

    @GetMapping("/admin/teachers/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTeacherDetails(@PathVariable Long id,
                                                                 HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Admin session expired. Please log in again."));
        }

        Teacher teacher = teacherRepo.findById(id).orElse(null);
        if (teacher == null || teacher.getAdmin() == null || !teacher.getAdmin().getId().equals(admin.getId())) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Teacher record was not found."));
        }

        return ResponseEntity.ok(buildTeacherResponse(employeeIdService.ensureTeacherEmployeeId(teacher), "Teacher record loaded."));
    }

    @Transactional
    @PostMapping("/update-teacher/{id}")
    public Object updateTeacher(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String designation,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String experience,
            @RequestParam(required = false) String academicYear,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            @RequestHeader(value = "X-Requested-With", required = false) String requestedWith) {
        Admin admin = getLoggedAdmin(session);
        boolean ajaxRequest = "XMLHttpRequest".equalsIgnoreCase(requestedWith);
        if (admin == null) {
            return ajaxRequest
                    ? ResponseEntity.status(401).body(Map.of("success", false, "message", "Admin session expired. Please log in again."))
                    : "redirect:/login-admin";
        }

        Teacher persistedTeacher = teacherRepo.findById(id).map(teacher -> {
            if (teacher.getAdmin() != null && !teacher.getAdmin().getId().equals(admin.getId())) {
                return null;
            }
            Teacher existingEmailOwner = teacherRepo.findByEmail(email);
            if (existingEmailOwner != null && !existingEmailOwner.getId().equals(teacher.getId())) {
                return null;
            }

            teacher.setName(blankToNull(name));
            teacher.setEmail(blankToNull(email));
            teacher.setPhone(blankToNull(phone));
            teacher.setDesignation(blankToNull(designation));
            teacher.setDepartment(blankToNull(department));
            teacher.setStatus(blankToNull(status) != null ? blankToNull(status) : "Active");
            teacher.setExperience(blankToNull(experience));
            teacher.setAcademicYear(normalizeAcademicYearValue(academicYear));
            return teacherRepo.saveAndFlush(teacher);
        }).orElse(null);

        if (persistedTeacher == null) {
            return ajaxRequest
                    ? ResponseEntity.badRequest().body(Map.of("success", false, "message", "Teacher record could not be updated. Check the teacher ID and email."))
                    : "redirect:/admin-teachers";
        }

        Teacher refreshedTeacher = employeeIdService.ensureTeacherEmployeeId(
                teacherRepo.findById(persistedTeacher.getId()).orElse(persistedTeacher));
        if (ajaxRequest) {
            return ResponseEntity.ok(buildTeacherResponse(refreshedTeacher, "Teacher record updated successfully."));
        }

        redirectAttributes.addFlashAttribute("teacherSuccess", "Teacher record updated successfully.");
        return "redirect:/admin-teachers";
    }

    private Map<String, Object> buildTeacherResponse(Teacher teacher, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("teacher", Map.ofEntries(
                Map.entry("id", teacher.getId()),
                Map.entry("teacherCode", defaultText(teacher.getEmployeeId(), "TCH" + (100 + teacher.getId()))),
                Map.entry("name", defaultText(teacher.getName(), "Teacher")),
                Map.entry("email", defaultText(teacher.getEmail(), "Not available")),
                Map.entry("phone", defaultText(teacher.getPhone(), "")),
                Map.entry("designation", defaultText(teacher.getDesignation(), "")),
                Map.entry("department", defaultText(teacher.getDepartment(), "")),
                Map.entry("status", defaultText(teacher.getStatus(), "Active")),
                Map.entry("experience", defaultText(teacher.getExperience(), "")),
                Map.entry("academicYear", defaultText(teacher.getAcademicYear(), "")),
                Map.entry("qualification", defaultText(teacher.getQualification(), "")),
                Map.entry("photo", defaultText(teacher.getPhoto(), "")),
                Map.entry("gender", defaultText(teacher.getGender(), "")),
                Map.entry("employeeId", defaultText(teacher.getEmployeeId(), "")),
                Map.entry("joiningDate", teacher.getJoiningDate() != null ? teacher.getJoiningDate().toString() : "")
        ));
        return response;
    }

    @Transactional
    @GetMapping("/delete-teacher/{id}")
    public String deleteTeacher(@PathVariable Long id, HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        teacherRepo.findById(id).ifPresent(teacher -> {
            if (teacher.getAdmin() != null && !teacher.getAdmin().getId().equals(admin.getId())) {
                return;
            }

            deleteTeacherDependencies(teacher, admin);
            teacherRepo.delete(teacher);
        });
        return "redirect:/admin-teachers";
    }

    @Transactional
    @GetMapping("/delete-class/{id}")
    public String deleteClass(@PathVariable Long id, HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        classRepo.findById(id).ifPresent(cls -> {
            if (cls.getAdmin() != null && !cls.getAdmin().getId().equals(admin.getId())) {
                return;
            }

            studentRepo.findByAdminAndClassRoom(admin, cls).forEach(student -> {
                student.setClassRoom(null);
                studentRepo.save(student);
            });

            timetableRepo.findByAdminAndClassRoom(admin, cls).forEach(entry -> {
                entry.setClassRoom(null);
                timetableRepo.save(entry);
            });

            studyMaterialRepo.findByClassRoomOrderByUploadedAtDesc(cls).forEach(material -> {
                material.setClassRoom(null);
                studyMaterialRepo.save(material);
            });

            teacherRepo.findByAdmin(admin).stream()
                    .filter(teacher -> teacher.getClassRoom() != null && teacher.getClassRoom().getId().equals(id))
                    .forEach(teacher -> {
                        teacher.setClassRoom(null);
                        teacherRepo.save(teacher);
                    });

            classRepo.delete(cls);
        });

        return "redirect:/view-classes";
    }

    // ═══════════════════════════════════════════════
    // ADD CLASS
    // ═══════════════════════════════════════════════
    @GetMapping("/add-class")
    public String addClassPage(Model model, HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";
        addAddClassModel(model, admin);
        return "admin/add-class";
    }

    @PostMapping("/add-class")
    public String saveClass(
            @RequestParam("courseId")                               Long courseId,
            @RequestParam("batchId")                                Long batchId,
            @RequestParam(value = "department", required = false)   String department,
            @RequestParam(value = "courseType", required = false)   String courseType,
            @RequestParam(value = "duration", required = false)     String duration,
            @RequestParam(value = "totalSemesters", required = false) Integer totalSemesters,
            @RequestParam("name")                                   String className,
            @RequestParam(value = "year",      required = false)  String year,
            @RequestParam(value = "semester",  required = false)  Integer semester,
            @RequestParam(value = "section",   required = false)  String section,
            @RequestParam(value = "room",      required = false)  String room,
            @RequestParam(value = "batch",     required = false)  String batch,
            @RequestParam(value = "batchName", required = false)    String batchName,
            @RequestParam(value = "batchStartYear", required = false) Integer batchStartYear,
            @RequestParam(value = "batchEndYear", required = false) Integer batchEndYear,
            @RequestParam(value = "academicYear", required = false) String academicYear,
            @RequestParam(value = "intakeCapacity", required = false) Integer intakeCapacity,
            @RequestParam(value = "shift", required = false)        String shift,
            @RequestParam(value = "description", required = false)  String description,
            @RequestParam(value = "status", required = false)       String status,
            @RequestParam(value = "remarks", required = false)      String remarks,
            Model model,
            HttpSession session) {

        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        Course selectedCourse = getAdminCourse(admin, courseId);
        if (selectedCourse == null) {
            model.addAttribute("error", "Please select a valid course.");
            addAddClassModel(model, admin);
            return "admin/add-class";
        }

        Batch selectedBatch = getAdminBatch(admin, batchId);
        if (selectedBatch == null || selectedBatch.getCourse() == null
                || !selectedCourse.getId().equals(selectedBatch.getCourse().getId())) {
            model.addAttribute("error", "Please select a valid batch for the chosen course.");
            addAddClassModel(model, admin);
            return "admin/add-class";
        }

        String normalizedClassName = blankToNull(className);
        String normalizedYear = blankToNull(year);
        String normalizedSection = blankToNull(section);

        if (normalizedClassName == null) {
            model.addAttribute("error", "Class name is required.");
            addAddClassModel(model, admin);
            return "admin/add-class";
        }
        if (normalizedYear == null) {
            model.addAttribute("error", "Please choose a year.");
            addAddClassModel(model, admin);
            return "admin/add-class";
        }
        if (semester == null) {
            model.addAttribute("error", "Please choose a semester.");
            addAddClassModel(model, admin);
            return "admin/add-class";
        }
        if (normalizedSection == null) {
            model.addAttribute("error", "Please choose or enter a section.");
            addAddClassModel(model, admin);
            return "admin/add-class";
        }

        boolean duplicateExists = classRepo.findByAdmin(admin).stream().anyMatch(existing ->
                existing.getCourse() != null
                        && existing.getBatchName() != null
                        && existing.getYear() != null
                        && existing.getSemester() != null
                        && existing.getSection() != null
                        && existing.getCourse().equalsIgnoreCase(selectedCourse.getName())
                        && existing.getBatchName().equalsIgnoreCase(selectedBatch.getDisplayName())
                        && existing.getYear().equalsIgnoreCase(normalizedYear)
                        && existing.getSemester().equals(semester)
                        && existing.getSection().equalsIgnoreCase(normalizedSection));
        if (duplicateExists) {
            model.addAttribute("error", "Class already exists for this combination.");
            addAddClassModel(model, admin);
            return "admin/add-class";
        }

        ClassRoom c = new ClassRoom();
        c.setName(normalizedClassName);
        c.setCourse(selectedCourse.getName());
        c.setCourseCode(selectedCourse.getCode());
        c.setDepartment(blankToNull(selectedCourse.getDepartment()) != null
                ? selectedCourse.getDepartment().strip()
                : blankToNull(department));
        c.setCourseType(blankToNull(selectedCourse.getCourseType()) != null
                ? selectedCourse.getCourseType().strip()
                : (blankToNull(courseType) != null ? courseType.strip() : inferCourseType(selectedCourse)));
        c.setDuration(blankToNull(duration) != null ? duration.strip() : formatDuration(selectedCourse));
        c.setTotalSemesters(totalSemesters != null ? totalSemesters : selectedCourse.getTotalSemesters());
        c.setYear(normalizedYear);
        c.setSemester(semester);
        c.setSection(normalizedSection);
        c.setRoom(blankToNull(room));
        c.setAcademicYear(blankToNull(academicYear));
        if (c.getAcademicYear() == null && selectedBatch.getStartYear() != null) {
            c.setAcademicYear(selectedBatch.getStartYear() + "-" + String.valueOf(selectedBatch.getStartYear() + 1).substring(2));
        } else if (c.getAcademicYear() == null && batchStartYear != null) {
            c.setAcademicYear(batchStartYear + "-" + String.valueOf(batchStartYear + 1).substring(2));
        }
        c.setIntakeCapacity(intakeCapacity);
        c.setShift(blankToNull(shift));
        c.setDescription(blankToNull(description));
        c.setStatus((status == null || status.isBlank()) ? "Active" : status.strip());
        c.setRemarks(blankToNull(remarks));

        c.setBatch(selectedBatch.getDisplayName());
        c.setBatchName(selectedBatch.getDisplayName());
        c.setBatchStartYear(selectedBatch.getStartYear() != null ? selectedBatch.getStartYear() : batchStartYear);
        c.setBatchEndYear(selectedBatch.getEndYear() != null ? selectedBatch.getEndYear() : batchEndYear);

        c.setAdmin(admin);
        classRepo.save(c);
        return "redirect:/add-class?success=Class created successfully";
    }

    @PostMapping("/edit-class/{id}")
    public String editClass(@PathVariable Long id,
                            @RequestParam("name") String className,
                            @RequestParam(value = "year", required = false) String year,
                            @RequestParam(value = "semester", required = false) Integer semester,
                            @RequestParam(value = "section", required = false) String section,
                            @RequestParam(value = "room", required = false) String room,
                            @RequestParam(value = "shift", required = false) String shift,
                            @RequestParam(value = "status", required = false) String status,
                            @RequestParam(value = "intakeCapacity", required = false) Integer intakeCapacity,
                            @RequestParam(value = "remarks", required = false) String remarks,
                            @RequestParam(value = "description", required = false) String description,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        ClassRoom classRoom = classRepo.findById(id).orElse(null);
        if (classRoom == null || classRoom.getAdmin() == null || !classRoom.getAdmin().getId().equals(admin.getId())) {
            return "redirect:/view-classes?error=Class not found";
        }

        String normalizedClassName = blankToNull(className);
        String normalizedYear = blankToNull(year);
        String normalizedSection = blankToNull(section);
        if (normalizedClassName == null || normalizedYear == null || semester == null || normalizedSection == null) {
            return "redirect:/view-classes?error=Class name, year, semester, and section are required";
        }

        boolean duplicateExists = classRepo.findByAdmin(admin).stream().anyMatch(existing ->
                !existing.getId().equals(classRoom.getId())
                        && existing.getCourse() != null
                        && existing.getBatchName() != null
                        && existing.getYear() != null
                        && existing.getSemester() != null
                        && existing.getSection() != null
                        && existing.getCourse().equalsIgnoreCase(classRoom.getCourse())
                        && existing.getBatchName().equalsIgnoreCase(classRoom.getBatchName())
                        && existing.getYear().equalsIgnoreCase(normalizedYear)
                        && existing.getSemester().equals(semester)
                        && existing.getSection().equalsIgnoreCase(normalizedSection));
        if (duplicateExists) {
            return "redirect:/view-classes?error=Another class already exists for this batch, year, semester, and section";
        }

        classRoom.setName(normalizedClassName);
        classRoom.setYear(normalizedYear);
        classRoom.setSemester(semester);
        classRoom.setSection(normalizedSection);
        classRoom.setRoom(blankToNull(room));
        classRoom.setShift(blankToNull(shift));
        classRoom.setStatus((status == null || status.isBlank()) ? "Active" : status.strip());
        classRoom.setIntakeCapacity(intakeCapacity);
        classRoom.setRemarks(blankToNull(remarks));
        classRoom.setDescription(blankToNull(description));
        classRepo.save(classRoom);

        redirectAttributes.addAttribute("success", "Class updated successfully");
        return "redirect:/view-classes";
    }

    // ═══════════════════════════════════════════════
    // VIEW CLASSES
    // ═══════════════════════════════════════════════
    @GetMapping("/view-classes")
    public String viewClasses(
            @RequestParam(required = false) String course,
            Model model,
            HttpSession session) {

        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        List<ClassRoom> all = classRepo.findByAdmin(admin);
        List<Course> availableCourses = courseRepo.findByAdminOrderByCodeAsc(admin);
        LinkedHashMap<String, Course> courseCatalog = buildCourseCatalog(availableCourses);
        List<Batch> availableBatches = batchRepo.findByAdminOrderByDisplayNameAsc(admin);

        all.forEach(classRoom -> classRoom.setTotalFees(
                resolveDisplayedClassFee(admin, classRoom, courseCatalog, availableBatches)
        ));

        List<ClassRoom> filtered = (course != null && !course.isBlank())
                ? all.stream().filter(c -> course.equalsIgnoreCase(c.getCourse())).collect(Collectors.toList())
                : all;

        List<String> courses = all.stream()
                .map(ClassRoom::getCourse).filter(c -> c != null && !c.isBlank())
                .distinct().sorted().collect(Collectors.toList());

        model.addAttribute("classes",        filtered);
        model.addAttribute("courses",        courses);
        model.addAttribute("selectedCourse", course);
        addAdminAttributes(model, admin);
        return "admin/view-classes";
    }

    // ═══════════════════════════════════════════════
    // TIMETABLE
    // ═══════════════════════════════════════════════
    @GetMapping("/add-timetable")
    public String addTimetablePage(
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) Long editId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String course,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Integer semester,
            @RequestParam(required = false) String success,
            Model model,
            HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";
        if (success != null && !success.isBlank()) {
            model.addAttribute("success", success);
        }
        if (editId != null) {
            timetableRepo.findById(editId).ifPresent(entry -> {
                if (entry.getAdmin() != null && entry.getAdmin().getId().equals(admin.getId())) {
                    model.addAttribute("entry", entry);
                }
            });
        }
        return renderTimetablePage(model, admin, batchId, classId, course, academicYear, semester, null, null);
    }

    @PostMapping("/add-timetable")
    public String saveTimetable(
            @RequestParam String day,
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam String subject,
            @RequestParam String room,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) String entryType,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Integer semester,
            @RequestParam(required = false) Long entryId,
            Model model,
            HttpSession session) {

        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        LocalTime start;
        LocalTime end;
        try {
            start = LocalTime.parse(startTime);
            end = LocalTime.parse(endTime);
        } catch (Exception ex) {
            return renderTimetablePage(model, admin, batchId, classId, null, academicYear, semester,
                    "Please enter a valid start and end time.", null);
        }

        if (!end.isAfter(start)) {
            return renderTimetablePage(model, admin, batchId, classId, null, academicYear, semester,
                    "End time must be later than start time.", null);
        }

        boolean isBreak = "BREAK".equalsIgnoreCase(blankToNull(entryType));
        ClassRoom selectedClass = null;
        Teacher selectedTeacher = null;
        Batch selectedBatch = getAdminBatch(admin, batchId);
        List<Subject> scopedSubjects = List.of();
        Subject selectedSubject = null;

        if (classId == null) {
            return renderTimetablePage(model, admin, batchId, classId, null, academicYear, semester,
                    "Please choose a valid class scope before saving timetable entries.", null);
        }
        selectedClass = classRepo.findById(classId).orElse(null);
        if (selectedClass == null || selectedClass.getAdmin() == null || !selectedClass.getAdmin().getId().equals(admin.getId())) {
            return renderTimetablePage(model, admin, batchId, classId, null, academicYear, semester,
                    "Please choose a valid class from your admin account.", null);
        }
        if (selectedBatch == null) {
            selectedBatch = resolveBatchForClass(admin, selectedClass);
            if (selectedBatch != null) {
                batchId = selectedBatch.getId();
            }
        }
        if (selectedBatch != null && !batchMatchesClass(selectedClass, selectedBatch)) {
            return renderTimetablePage(model, admin, batchId, classId, selectedClass.getCourse(), academicYear, semester,
                    "Selected class does not belong to the chosen batch.", null);
        }
        if (semester != null && selectedClass.getSemester() != null && !selectedClass.getSemester().equals(semester)) {
            return renderTimetablePage(model, admin, batchId, classId, selectedClass.getCourse(), academicYear, semester,
                    "Selected class does not match the chosen semester.", null);
        }
        String academic = blankToNull(academicYear);
        if (academic == null) {
            academic = selectedClass.getAcademicYear();
        }
        if (academic != null && selectedClass.getAcademicYear() != null && !academicYearMatches(selectedClass.getAcademicYear(), academic)) {
            return renderTimetablePage(model, admin, batchId, classId, selectedClass.getCourse(), academic, semester,
                    "Selected class does not match the chosen academic year.", null);
        }
        if (batchId == null && selectedBatch != null) {
            batchId = selectedBatch.getId();
        }

        if (!isBreak) {
            if (teacherId == null) {
                return renderTimetablePage(model, admin, batchId, classId, null, academicYear, semester,
                        "Please choose a valid teacher.", null);
            }

            selectedTeacher = teacherRepo.findById(teacherId).orElse(null);

            if (selectedTeacher == null) {
                return renderTimetablePage(model, admin, batchId, classId, null, academicYear, semester,
                        "Please choose a valid teacher.", null);
            }

            if (selectedTeacher.getAdmin() == null || !selectedTeacher.getAdmin().getId().equals(admin.getId())) {
                return renderTimetablePage(model, admin, batchId, classId, null, academicYear, semester,
                        "You can only schedule teachers assigned to your admin account.", null);
            }

            Integer effectiveSemester = selectedClass.getSemester() != null ? selectedClass.getSemester() : semester;
            String effectiveCourse = blankToNull(selectedClass.getCourse()) != null
                    ? selectedClass.getCourse()
                    : (selectedBatch != null && selectedBatch.getCourse() != null ? blankToNull(selectedBatch.getCourse().getCode()) : null);
            if (batchId != null && effectiveSemester != null) {
                scopedSubjects = timetableSubjectsForScope(admin, batchId, effectiveCourse, effectiveSemester);
                if (!teacherCanTeachScopedSubject(selectedTeacher, scopedSubjects, subject)) {
                    return renderTimetablePage(model, admin, batchId, classId, selectedClass.getCourse(), academicYear, semester,
                            "Please choose a subject from the Subject Registry for this batch and semester.", null);
                }
                selectedSubject = findScopedSubject(scopedSubjects, subject);
            }
        }

        for (Timetable entry : timetableRepo.findByAdmin(admin)) {
            if (entryId != null && entryId.equals(entry.getId())) continue;
            if (entry.getDay() == null || entry.getStartTime() == null || entry.getEndTime() == null) continue;
            if (!entry.getDay().equalsIgnoreCase(day)) continue;
            if (!overlaps(start, end, entry)) continue;

            boolean existingBreak = "BREAK".equalsIgnoreCase(entry.getEntryType());
            boolean sameClassScope = entry.getClassRoom() != null && entry.getClassRoom().getId().equals(selectedClass.getId());
            if (isBreak || existingBreak) {
                if (sameClassScope) {
                    return renderTimetablePage(model, admin, batchId, classId, selectedClass.getCourse(), academic, semester,
                            "This break overlaps with an existing timetable entry for the selected class.", null);
                }
                continue;
            }

            if (teacherId != null && entry.getTeacher() != null && entry.getTeacher().getId().equals(teacherId)) {
                return renderTimetablePage(model, admin, batchId, classId, selectedClass.getCourse(), academic, semester,
                        "This teacher is already assigned during this time slot for another course/batch/section.", null);
            }
            if (sameClassScope) {
                return renderTimetablePage(model, admin, batchId, classId, selectedClass.getCourse(), academic, semester,
                        "This section already has a scheduled lecture during this time.", null);
            }
            if (blankToNull(room) != null && entry.getRoom() != null && entry.getRoom().equalsIgnoreCase(room)) {
                return renderTimetablePage(model, admin, batchId, classId, selectedClass.getCourse(), academic, semester,
                        "This room is already occupied during this time slot.", null);
            }
        }

        if (isBreak && room != null && !room.isBlank()) {
            // Break entries can still keep a room label if the admin wants one.
        }

        Timetable t = (entryId != null) ? timetableRepo.findById(entryId).orElse(new Timetable()) : new Timetable();
        if (t.getId() != null && t.getAdmin() != null && !t.getAdmin().getId().equals(admin.getId())) {
            return renderTimetablePage(model, admin, batchId, classId, selectedClass != null ? selectedClass.getCourse() : null, academic, semester,
                    "You can only edit timetable entries from your admin account.", null);
        }
        t.setDay(day);
        t.setStartTime(startTime);
        t.setEndTime(endTime);
        t.setSubject(subject);
        t.setRoom(blankToNull(room));
        Integer effectiveSemester = selectedClass.getSemester() != null ? selectedClass.getSemester() : semester;
        String effectiveCourse = blankToNull(selectedClass.getCourse()) != null
                ? selectedClass.getCourse()
                : (selectedBatch != null && selectedBatch.getCourse() != null ? blankToNull(selectedBatch.getCourse().getCode()) : null);
        t.setEntryType(isBreak ? "BREAK" : resolveTimetableEntryType(admin, selectedBatch, selectedBatch != null ? selectedBatch.getCourse() : resolveAdminCourseByInput(admin, effectiveCourse), effectiveSemester, subject));
        t.setAcademicYear(academic);
        t.setCourse(selectedBatch != null ? selectedBatch.getCourse() : resolveAdminCourseByInput(admin, effectiveCourse));
        t.setBatch(selectedBatch);
        t.setSemesterNumber(effectiveSemester);
        t.setSection(blankToNull(selectedClass.getSection()));
        t.setSubjectRef(isBreak ? null : selectedSubject);
        t.setClassRoom(selectedClass);
        t.setTeacher(isBreak ? null : selectedTeacher);
        t.setAdmin(admin);
        timetableRepo.save(t);
        String redirect = "/add-timetable?batchId=" + (batchId != null ? batchId : "")
                + "&classId=" + (classId != null ? classId : "")
                + "&academicYear=" + (academic != null ? academic : "")
                + "&semester=" + (effectiveSemester != null ? effectiveSemester : "")
                + "&success=1 entry saved successfully";
        return "redirect:" + redirect;
    }

    @PostMapping("/add-timetable/bulk")
    public String saveTimetableBulk(
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String course,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Integer semester,
            @RequestParam String bulkRows,
            Model model,
            HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        ClassRoom selectedClass = classId != null ? classRepo.findById(classId).orElse(null) : null;
        Batch selectedBatch = getAdminBatch(admin, batchId);
        List<Teacher> activeTeachers = activeTeachersForAdmin(admin);
        String effectiveCourse = blankToNull(course);
        Integer effectiveSemester = semester;

        if (selectedClass != null) {
            if (selectedBatch == null) {
                selectedBatch = resolveBatchForClass(admin, selectedClass);
                if (selectedBatch != null) {
                    batchId = selectedBatch.getId();
                }
            }
            if (selectedClass.getCourse() != null && !selectedClass.getCourse().isBlank()) {
                effectiveCourse = selectedClass.getCourse();
            }
            if (selectedClass.getSemester() != null) {
                effectiveSemester = selectedClass.getSemester();
            }
        }

        List<ClassRoom> scopedClasses = timetableClassesForScope(admin, batchId, effectiveCourse, academicYear, effectiveSemester);

        if (selectedClass == null && scopedClasses.size() == 1) {
            selectedClass = scopedClasses.get(0);
            classId = selectedClass.getId();
            if (selectedBatch == null) {
                selectedBatch = resolveBatchForClass(admin, selectedClass);
                if (selectedBatch != null) {
                    batchId = selectedBatch.getId();
                }
            }
            if (selectedClass.getCourse() != null && !selectedClass.getCourse().isBlank()) {
                effectiveCourse = selectedClass.getCourse();
            }
            if (selectedClass.getSemester() != null) {
                effectiveSemester = selectedClass.getSemester();
            }
        }

        if (selectedClass == null) {
            return renderTimetablePage(model, admin, batchId, classId, course, academicYear, semester,
                    "Select one exact class scope before saving bulk timetable rows.", null);
        }
        if (selectedClass.getAdmin() == null || !selectedClass.getAdmin().getId().equals(admin.getId())) {
            return renderTimetablePage(model, admin, batchId, classId, course, academicYear, semester,
                    "You can only use classes from your admin account.", null);
        }
        if (selectedBatch != null && selectedClass != null && !batchMatchesClass(selectedClass, selectedBatch)) {
            return renderTimetablePage(model, admin, batchId, classId, selectedClass.getCourse(), academicYear, semester,
                    "Selected class does not belong to the chosen batch.", null);
        }
        if (semester != null && selectedClass.getSemester() != null && !selectedClass.getSemester().equals(semester)) {
            return renderTimetablePage(model, admin, batchId, classId, selectedClass.getCourse(), academicYear, semester,
                    "Selected class does not match the chosen semester.", null);
        }

        List<Subject> scopedSubjects = timetableSubjectsForScope(admin, batchId, effectiveCourse, effectiveSemester);

        String defaultAcademic = blankToNull(academicYear);
        if (defaultAcademic == null && selectedClass != null) {
            defaultAcademic = selectedClass.getAcademicYear();
        }
        if (defaultAcademic != null && selectedClass.getAcademicYear() != null
                && !academicYearMatches(selectedClass.getAcademicYear(), defaultAcademic)) {
            return renderTimetablePage(model, admin, batchId, classId, selectedClass.getCourse(), defaultAcademic, semester,
                    "Selected class does not match the chosen academic year.", null);
        }

        String[] lines = bulkRows.split("\\R+");
        int saved = 0;
        int lineNo = 0;
        List<String> validationErrors = new ArrayList<>();
        for (String rawLine : lines) {
            lineNo++;
            String line = rawLine != null ? rawLine.trim() : "";
            if (line.isBlank()) continue;

            String[] parts = line.split("\\s*[,|]\\s*");
            if (parts.length < 5) {
                validationErrors.add("Row " + lineNo + ": Missing room or other required values.");
                continue;
            }

            String day = blankToNull(parts[0]);
            String startTime = blankToNull(parts[1]);
            String endTime = blankToNull(parts[2]);
            String subject = blankToNull(parts[3]);
            boolean rowWiseTeacherFormat = parts.length >= 8;
            String teacherToken = rowWiseTeacherFormat ? blankToNull(parts[4]) : null;
            String room = blankToNull(rowWiseTeacherFormat ? parts[5] : parts[4]);
            String type = parts.length >= (rowWiseTeacherFormat ? 7 : 6) ? blankToNull(parts[rowWiseTeacherFormat ? 6 : 5]) : null;
            String rowAcademic = parts.length >= (rowWiseTeacherFormat ? 8 : 7) ? blankToNull(parts[rowWiseTeacherFormat ? 7 : 6]) : defaultAcademic;

            if (day == null || startTime == null || endTime == null || subject == null) {
                validationErrors.add("Row " + lineNo + ": Day, start time, end time and subject are required.");
                continue;
            }
            if (!isValidWeekDay(day)) {
                validationErrors.add("Row " + lineNo + ": Invalid day name.");
                continue;
            }

            boolean isBreak = "BREAK".equalsIgnoreCase(type);
            LocalTime start;
            LocalTime end;
            try {
                start = LocalTime.parse(startTime);
                end = LocalTime.parse(endTime);
            } catch (Exception ex) {
                validationErrors.add("Row " + lineNo + ": Start time or end time is not in HH:MM format.");
                continue;
            }
            if (!end.isAfter(start)) {
                validationErrors.add("Row " + lineNo + ": End time must be later than start time.");
                continue;
            }

            Teacher rowTeacher = resolveTimetableRowTeacher(admin, teacherToken, activeTeachers);
            if (!isBreak && rowTeacher == null) {
                validationErrors.add("Row " + lineNo + ": Teacher is required for timetable lectures. Use the row-wise teacher column.");
                continue;
            }
            if (!isBreak && !teacherCanTeachScopedSubject(rowTeacher, scopedSubjects, subject)) {
                validationErrors.add("Row " + lineNo + ": Please choose a subject from the Subject Registry for this batch and semester.");
                continue;
            }

            Timetable t = new Timetable();
            t.setAdmin(admin);
            t.setDay(day);
            t.setStartTime(startTime);
            t.setEndTime(endTime);
            t.setSubject(subject);
            t.setRoom(room);
            t.setEntryType(isBreak ? "BREAK" : resolveTimetableEntryType(admin, selectedBatch, selectedBatch != null ? selectedBatch.getCourse() : resolveAdminCourseByInput(admin, effectiveCourse), effectiveSemester, subject));
            t.setAcademicYear(rowAcademic);
            t.setCourse(selectedBatch != null ? selectedBatch.getCourse() : resolveAdminCourseByInput(admin, effectiveCourse));
            t.setBatch(selectedBatch);
            t.setSemesterNumber(effectiveSemester);
            t.setSection(blankToNull(selectedClass.getSection()));
            t.setSubjectRef(isBreak ? null : findScopedSubject(scopedSubjects, subject));
            t.setClassRoom(selectedClass);
            t.setTeacher(isBreak ? null : rowTeacher);

            boolean invalidRow = false;
            for (Timetable existing : timetableRepo.findByAdmin(admin)) {
                if (existing.getDay() == null || existing.getStartTime() == null || existing.getEndTime() == null) continue;
                if (!existing.getDay().equalsIgnoreCase(day)) continue;
                if (!overlaps(start, end, existing)) continue;
                boolean existingBreak = "BREAK".equalsIgnoreCase(existing.getEntryType());
                boolean sameClassScope = existing.getClassRoom() != null && existing.getClassRoom().getId().equals(selectedClass.getId());
                if (isBreak || existingBreak) {
                    if (sameClassScope) {
                        validationErrors.add("Row " + lineNo + ": Break overlaps with an existing timetable entry for this class.");
                        invalidRow = true;
                        break;
                    }
                    continue;
                }
                if (rowTeacher != null && existing.getTeacher() != null && existing.getTeacher().getId().equals(rowTeacher.getId())) {
                    validationErrors.add("Row " + lineNo + ": This teacher is already assigned during this time slot for another course/batch/section.");
                    invalidRow = true;
                    break;
                }
                if (sameClassScope) {
                    validationErrors.add("Row " + lineNo + ": This section already has a scheduled lecture during this time.");
                    invalidRow = true;
                    break;
                }
                if (existing.getRoom() != null && room != null && existing.getRoom().equalsIgnoreCase(room)) {
                    validationErrors.add("Row " + lineNo + ": This room is already occupied during this time slot.");
                    invalidRow = true;
                    break;
                }
            }

            if (invalidRow) {
                continue;
            }

            timetableRepo.save(t);
            saved++;
        }
        if (!validationErrors.isEmpty()) {
            return renderTimetablePage(model, admin, batchId, classId, selectedClass != null ? selectedClass.getCourse() : effectiveCourse, defaultAcademic, effectiveSemester,
                    "Please fix the highlighted bulk rows and try again.", validationErrors);
        }

        String redirect = "/add-timetable?batchId=" + (batchId != null ? batchId : "")
                + "&classId=" + (classId != null ? classId : "")
                + "&course=" + URLEncoder.encode(selectedClass != null ? selectedClass.getCourse() : (effectiveCourse != null ? effectiveCourse : ""), StandardCharsets.UTF_8)
                + "&academicYear=" + (defaultAcademic != null ? defaultAcademic : "")
                + "&semester=" + (effectiveSemester != null ? effectiveSemester : "")
                + "&success=" + saved + " entries saved successfully";
        return "redirect:" + redirect;
    }

    @GetMapping("/view-timetable")
    public String viewTimetable(
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String course,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Integer semester,
            @RequestParam(required = false, defaultValue = "false") boolean printMode,
            Model model,
            HttpSession session) {

        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";
        model.addAttribute("printMode", printMode);
        return renderTimetablePage(model, admin, batchId, classId, course, academicYear, semester, null, null);
    }

    @GetMapping("/view-timetable/export/excel")
    public void exportTimetableExcel(
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String course,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Integer semester,
            HttpSession session,
            HttpServletResponse response) throws IOException {

        Admin admin = getLoggedAdmin(session);
        if (admin == null) {
            response.sendRedirect("/login-admin");
            return;
        }
        syncClassRoomsFromAcademicStructure(admin);

        List<Timetable> timetable = timetableEntriesForView(admin, classId, batchId, course, academicYear, semester);
        List<TimetableSlotView> timeSlots = buildTimeSlots(timetable);
        Map<String, Map<String, List<Timetable>>> weeklyGrid = buildWeeklyGrid(timetable, timeSlots);
        List<WeekGridRowView> weekRows = buildWeekRows(timetable, timeSlots);
        List<TimeGridRowView> timeRows = buildTimeRows(timetable, timeSlots);
        List<DayColumnView> dayColumns = WEEK_DAYS.stream().map(DayColumnView::new).collect(Collectors.toList());

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Timetable");
            int rowIndex = 0;

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor((short) 44);
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            Row header = sheet.createRow(rowIndex++);
            header.createCell(0).setCellValue("Day");
            header.getCell(0).setCellStyle(headerStyle);
            for (int i = 0; i < timeSlots.size(); i++) {
                Cell cell = header.createCell(i + 1);
                cell.setCellValue(timeSlots.get(i).getLabel());
                cell.setCellStyle(headerStyle);
            }

            for (String day : WEEK_DAYS) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(day);
                Map<String, List<Timetable>> rowData = weeklyGrid.get(day);
                for (int i = 0; i < timeSlots.size(); i++) {
                    TimetableSlotView slot = timeSlots.get(i);
                    List<Timetable> entriesForCell = rowData != null ? rowData.get(slot.getKey()) : List.of();
                    String text = entriesForCell == null || entriesForCell.isEmpty()
                            ? ""
                            : entriesForCell.stream()
                                    .map(entry -> {
                                        String label = "BREAK".equalsIgnoreCase(entry.getEntryType())
                                                ? (blankToNull(entry.getSubject()) != null ? entry.getSubject() : "Break")
                                                : resolveTimetableSubjectLabel(entry);
                                        String scope = entry.getClassRoom() != null
                                                ? buildClassIdentityLabel(entry.getClassRoom())
                                                : buildTimetableScopeLabel(null, entry.getBatch(), entry.getCourse(), entry.getAcademicYear(), entry.getSemesterNumber());
                                        String room = blankToNull(entry.getRoom()) != null ? entry.getRoom() : "General";
                                        String suffix = scope != null && !scope.isBlank() ? " [" + scope + "]" : "";
                                        return label + " · " + room + suffix;
                                    })
                                    .collect(Collectors.joining(" | "));
                    row.createCell(i + 1).setCellValue(text);
                }
            }

            for (int i = 0; i <= timeSlots.size(); i++) {
                sheet.autoSizeColumn(i);
            }

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=weekly-timetable.xlsx");
            try (OutputStream os = response.getOutputStream()) {
                workbook.write(os);
            }
        }
    }

    @GetMapping("/view-timetable/export/pdf")
    public String exportTimetablePdf(
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) String course,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) Integer semester,
            Model model,
            HttpSession session) {
        return viewTimetable(batchId, classId, course, academicYear, semester, true, model, session);
    }

    @GetMapping("/delete-timetable/{id}")
    public String deleteTimetable(@PathVariable Long id, HttpSession session) {
        if (getLoggedAdmin(session) == null) return "redirect:/login-admin";
        timetableRepo.deleteById(id);
        return "redirect:/view-timetable";
    }

    @GetMapping("/smart-timetable")
    public String smartTimetable(Model model, HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";
        model.addAttribute("timetable", timetableRepo.findByAdmin(admin));
        model.addAttribute("classes",   classRepo.findByAdmin(admin));
        model.addAttribute("teachers",  teacherRepo.findByAdmin(admin));
        addAdminAttributes(model, admin);
        return "admin/smart-timetable";
    }

    // ═══════════════════════════════════════════════
    // FEES
    // ═══════════════════════════════════════════════
    @GetMapping("/admin-fees")
    public String adminFees(
            @RequestParam(required = false) String course,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) String search,
            Model model,
            HttpSession session) {

        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        List<Student> all      = studentRepo.findByAdmin(admin);
        all.forEach(student -> {
            syncStudentFeeTotals(admin, student);
            studentRepo.save(student);
        });
        List<Student> filtered = all;
        List<Course> availableCourses = courseRepo.findByAdminOrderByCodeAsc(admin);
        LinkedHashMap<String, Course> courseCatalog = buildCourseCatalog(availableCourses);
        String selectedCourseCode = resolveCourseCode(course, courseCatalog);

        if (selectedCourseCode != null)
            filtered = filtered.stream()
                    .filter(s -> selectedCourseCode.equalsIgnoreCase(resolveCourseCode(s.getCourse(), courseCatalog)))
                    .collect(Collectors.toList());

        if (semester != null && !semester.isEmpty())
            filtered = filtered.stream()
                    .filter(s -> s.getSemester() != null && s.getSemester().equalsIgnoreCase(semester))
                    .collect(Collectors.toList());

        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase().strip();
            filtered = filtered.stream()
                    .filter(s -> (s.getName()  != null && s.getName().toLowerCase().contains(q))
                            || (s.getEmail() != null && s.getEmail().toLowerCase().contains(q)))
                    .collect(Collectors.toList());
        }

        double totalCollected = all.stream().mapToDouble(s -> s.getPaidFees()    != null ? s.getPaidFees()    : 0.0).sum();
        double totalPending   = all.stream().mapToDouble(s -> s.getPendingFees() != null ? s.getPendingFees() : 0.0).sum();
        double totalFees      = all.stream().mapToDouble(s -> s.getTotalFees()   != null ? s.getTotalFees()   : 0.0).sum();
        int    collectionRate = (totalFees > 0) ? (int) ((totalCollected / totalFees) * 100) : 0;

        List<String> courses = availableCourses.stream()
                .map(Course::getCode)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        List<String> semesters = all.stream().map(Student::getSemester)
                .filter(s -> s != null && !s.isBlank()).distinct().sorted().collect(Collectors.toList());
        List<String> academicYears = academicYearsFromBatches(admin);

        model.addAttribute("students",         filtered);
        model.addAttribute("totalStudents",    all.size());
        model.addAttribute("totalCollected",   (long) totalCollected);
        model.addAttribute("totalPending",     (long) totalPending);
        model.addAttribute("totalFees",        (long) totalFees);
        model.addAttribute("collectionRate",   collectionRate);
        model.addAttribute("courses",          courses);
        model.addAttribute("semesters",        semesters);
        model.addAttribute("academicYears",    academicYears);
        model.addAttribute("selectedCourse",   selectedCourseCode);
        model.addAttribute("selectedSemester", semester);
        model.addAttribute("search",           search);
        model.addAttribute("feeRules",         feeRulesForAdmin(admin));
        model.addAttribute("batches",          batchRepo.findByAdminOrderByDisplayNameAsc(admin));
        addAdminAttributes(model, admin);
        return "admin/admin-fees";
    }

    @PostMapping("/save-fee-rule")
    public String saveFeeRule(
            @RequestParam String courseName,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) String feeScope,
            @RequestParam(required = false) String semester,
            @RequestParam double totalAmount,
            HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        Course selectedCourse = resolveAdminCourseByInput(admin, courseName);
        if (selectedCourse == null || blankToNull(selectedCourse.getCode()) == null) {
            return "redirect:/admin-fees?error=Please select a course";
        }
        String normalized = selectedCourse.getCode().strip();

        Batch selectedBatch = getAdminBatch(admin, batchId);
        Fees fee = findFeeRule(admin, normalized, selectedBatch != null ? selectedBatch.getId() : null, selectedBatch != null ? selectedBatch.getDisplayName() : null, academicYear, semester);
        if (fee == null) {
            fee = new Fees();
        }
        fee.setAdmin(admin);
        fee.setCourse(normalized);
        if (selectedBatch != null) {
            fee.setBatch(selectedBatch);
            fee.setBatchName(selectedBatch.getDisplayName());
        } else {
            fee.setBatch(null);
            fee.setBatchName(null);
        }
        fee.setAcademicYear(blankToNull(academicYear));
        fee.setFeeScope(blankToNull(feeScope));
        fee.setSemester(blankToNull(semester));
        fee.setTotalAmount(totalAmount);
        feesRepo.save(fee);

        studentRepo.findByAdmin(admin).forEach(s -> {
            syncStudentFeeTotals(admin, s);
            studentRepo.save(s);
        });

        return "redirect:/admin-fees?success=Fee rule saved successfully";
    }

    @PostMapping("/update-fees/{id}")
    public String updateFees(@PathVariable Long id,
                             @RequestParam double paidAmount,
                             HttpSession session) {
        if (getLoggedAdmin(session) == null) return "redirect:/login-admin";
        studentRepo.findById(id).ifPresent(student -> {
            syncStudentFeeTotals(student.getAdmin(), student);
            double newPaid = (student.getPaidFees() != null ? student.getPaidFees() : 0.0) + paidAmount;
            double total   =  student.getTotalFees() != null ? student.getTotalFees() : 0.0;
            student.setPaidFees(newPaid);
            student.setPendingFees(Math.max(0, total - newPaid));
            studentRepo.save(student);
        });
        return "redirect:/admin-fees";
    }

    @PostMapping("/pay-fees")
    public String payFees(@RequestParam Long studentId,
                          @RequestParam double amount,
                          HttpSession session) {
        if (getLoggedAdmin(session) == null) return "redirect:/login-admin";
        studentRepo.findById(studentId).ifPresent(student -> {
            syncStudentFeeTotals(student.getAdmin(), student);
            double newPaid = (student.getPaidFees() != null ? student.getPaidFees() : 0.0) + amount;
            double total   =  student.getTotalFees() != null ? student.getTotalFees() : 0.0;
            student.setPaidFees(newPaid);
            student.setPendingFees(Math.max(0, total - newPaid));
            studentRepo.save(student);
        });
        return "redirect:/admin-fees?success=Payment Successful";
    }

    @PostMapping("/set-fees")
    public String setFees(@RequestParam String courseName, HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";
        double fee = feesForStudent(admin, courseName, null, null, null, null);
        Fees feeRow = feesRepo.findByAdminAndCourseIgnoreCaseAndAcademicYearIsNullAndSemesterIsNull(admin, courseName);
        if (feeRow == null) {
            feeRow = new Fees();
        }
        feeRow.setAdmin(admin);
        feeRow.setCourse(courseName);
        feeRow.setTotalAmount(fee);
        feesRepo.save(feeRow);
        studentRepo.findByAdmin(admin).stream()
                .filter(s -> s.getCourse() != null && s.getCourse().equalsIgnoreCase(courseName))
                .filter(s -> s.getTotalFees() == null || s.getTotalFees() == 0)
                .forEach(s -> {
                    s.setTotalFees(fee);
                    s.setPaidFees(0.0);
                    s.setPendingFees(fee);
                    studentRepo.save(s);
        });
        return "redirect:/admin-fees?success=Fees Applied Successfully";
    }

    @GetMapping("/student-fee-preview")
    @ResponseBody
    public double studentFeePreview(
            @RequestParam String course,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) String semester,
            HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return 0.0;
        Batch batch = getAdminBatch(admin, batchId);
        String batchName = batch != null ? batch.getDisplayName() : null;
        return feesForStudent(admin, course, batchId, batchName, academicYear, semester);
    }

    // ═══════════════════════════════════════════════
    // NOTIFICATIONS
    // ═══════════════════════════════════════════════
    @GetMapping("/admin-notifications")
    public String adminNotifications(Model model, HttpSession session,
                                     @RequestParam(value = "notificationId", required = false) Long notificationId) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        List<PortalNotification> notifications = portalNotificationRepo
                .findByRecipientRoleAndRecipientEmailOrderByCreatedAtDesc("ADMIN", admin.getEmail());

        PortalNotification selectedNotification = null;
        if (notificationId != null) {
            selectedNotification = notifications.stream()
                    .filter(notification -> notification.getId() != null && notification.getId().equals(notificationId))
                    .findFirst()
                    .orElse(null);
            if (selectedNotification != null && selectedNotification.getReadAt() == null) {
                selectedNotification.setReadAt(LocalDateTime.now());
                portalNotificationRepo.save(selectedNotification);
            }
        }

        notifications = portalNotificationRepo
                .findByRecipientRoleAndRecipientEmailOrderByCreatedAtDesc("ADMIN", admin.getEmail());

        if (selectedNotification == null && !notifications.isEmpty()) {
            selectedNotification = notifications.get(0);
        }

        long unreadCount = portalNotificationRepo.countByRecipientRoleAndRecipientEmailAndScheduledForLessThanEqualAndReadAtIsNull(
                "ADMIN", admin.getEmail(), LocalDateTime.now());

        model.addAttribute("notifications", notifications);
        model.addAttribute("selectedNotification", selectedNotification);
        model.addAttribute("notificationCount", notifications.size());
        model.addAttribute("unreadCount", unreadCount);
        addAdminAttributes(model, admin);
        return "admin/admin-notifications";
    }

    // ═══════════════════════════════════════════════
    // PROFILE — View Only (no edit form)
    // ═══════════════════════════════════════════════
    @GetMapping("/admin-profile")
    public String adminProfile(Model model, HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";
        model.addAttribute("admin",         admin);
        model.addAttribute("adminId",       admin.getId());
        addAdminProfileMetrics(model, admin);
        addAdminAttributes(model, admin);
        return "admin/admin-profile";
    }

    // ═══════════════════════════════════════════════
    // CHANGE PASSWORD — Admin can ONLY change password
    // ═══════════════════════════════════════════════
    @PostMapping("/change-admin-password")
    public String changeAdminPassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            HttpSession session,
            Model model) {

        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        // 1. Current password must be correct
        if (!passwordProtectionService.matches(currentPassword, admin.getPassword())) {
            model.addAttribute("passError", "Current password is incorrect!");
            model.addAttribute("admin",         admin);
            model.addAttribute("adminId",       admin.getId());
            addAdminProfileMetrics(model, admin);
            addAdminAttributes(model, admin);
            return "admin/admin-profile";
        }

        // 2. New password must meet minimum length
        if (newPassword.length() < 8) {
            model.addAttribute("passError", "New password must be at least 8 characters!");
            model.addAttribute("admin",         admin);
            model.addAttribute("adminId",       admin.getId());
            addAdminProfileMetrics(model, admin);
            addAdminAttributes(model, admin);
            return "admin/admin-profile";
        }

        // 3. New password and confirm password must match
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("passError", "New passwords do not match!");
            model.addAttribute("admin",         admin);
            model.addAttribute("adminId",       admin.getId());
            addAdminProfileMetrics(model, admin);
            addAdminAttributes(model, admin);
            return "admin/admin-profile";
        }

        // 4. New password must not be the same as current
        if (passwordProtectionService.isSamePassword(newPassword, admin.getPassword())) {
            model.addAttribute("passError", "New password cannot be the same as current password!");
            model.addAttribute("admin",         admin);
            model.addAttribute("adminId",       admin.getId());
            addAdminProfileMetrics(model, admin);
            addAdminAttributes(model, admin);
            return "admin/admin-profile";
        }

        // ✅ All checks passed — save new password
        admin.setPassword(passwordProtectionService.encode(newPassword));
        adminRepo.save(admin);
        return "redirect:/admin-profile?success=Password changed successfully!";
    }

    // ═══════════════════════════════════════════════
    // REPORTS
    // ═══════════════════════════════════════════════
    @GetMapping("/admin-reports")
    public String adminReports(Model model, HttpSession session) {
        Admin admin = getLoggedAdmin(session);
        if (admin == null) return "redirect:/login-admin";

        List<Student> students = studentRepo.findByAdmin(admin);
        List<Teacher> teachers = teacherRepo.findByAdmin(admin);
        List<ClassRoom> classes = classRepo.findByAdmin(admin);
        List<Fees> feeRules = feesRepo.findByAdmin(admin);
        List<Course> courses = courseRepo.findByAdminOrderByCodeAsc(admin);
        List<PlacementTpo> tpos = placementTpoRepo.findAll();
        List<PlacementDrive> drives = placementDriveRepo.findAll();
        List<PlacementApplication> applications = placementApplicationRepo.findAll();

        double totalCollectedFees = students.stream()
                .mapToDouble(s -> s.getPaidFees() != null ? s.getPaidFees() : 0.0)
                .sum();
        double totalPendingFees = students.stream()
                .mapToDouble(s -> s.getPendingFees() != null ? s.getPendingFees() : 0.0)
                .sum();
        double totalFeesDemand = students.stream()
                .mapToDouble(s -> s.getTotalFees() != null ? s.getTotalFees() : 0.0)
                .sum();

        Map<String, Long> studentByCourseRaw = students.stream()
                .collect(Collectors.groupingBy(
                        s -> blankToNull(s.getCourse()) != null ? s.getCourse().strip() : "",
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
        Map<String, Double> feesByCourseRaw = feeRules.stream()
                .collect(Collectors.groupingBy(
                        fee -> blankToNull(fee.getCourse()) != null ? fee.getCourse().strip() : "",
                        LinkedHashMap::new,
                        Collectors.summingDouble(Fees::getTotalAmount)
                ));
        Map<String, Long> studentByCourse = buildCourseMetricMap(
                courses,
                new ArrayList<>(studentByCourseRaw.keySet()),
                new ArrayList<>(studentByCourseRaw.values())
        ).entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().longValue(),
                (a, b) -> a,
                LinkedHashMap::new
        ));
        Map<String, Double> feesByCourse = buildCourseMetricMap(
                courses,
                new ArrayList<>(feesByCourseRaw.keySet()),
                new ArrayList<>(feesByCourseRaw.values())
        ).entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().doubleValue(),
                (a, b) -> a,
                LinkedHashMap::new
        ));
        Map<String, Long> tpoStatusCounts = tpos.stream()
                .collect(Collectors.groupingBy(
                        tpo -> tpo.getStatus() != null ? tpo.getStatus() : "Unknown",
                        LinkedHashMap::new,
                        Collectors.counting()
                ));
        Map<String, Long> driveCourseCounts = drives.stream()
                .collect(Collectors.groupingBy(
                        drive -> drive.getEligibilityCourse() != null ? drive.getEligibilityCourse() : "Open",
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        model.addAttribute("students", students);
        model.addAttribute("teachers", teachers);
        model.addAttribute("classes", classes);
        model.addAttribute("feeRules", feeRules);
        model.addAttribute("tpos", tpos);
        model.addAttribute("drives", drives);
        model.addAttribute("applications", applications);

        model.addAttribute("totalStudents", students.size());
        model.addAttribute("totalTeachers", teachers.size());
        model.addAttribute("totalClasses", classes.size());
        model.addAttribute("totalFeeRules", feeRules.size());
        model.addAttribute("totalTpos", tpos.size());
        model.addAttribute("totalDrives", drives.size());
        model.addAttribute("totalApplications", applications.size());
        model.addAttribute("publishedDrives", drives.stream().filter(PlacementDrive::isPublished).count());
        model.addAttribute("activeTpos", tpos.stream().filter(t -> t.getStatus() != null && t.getStatus().equalsIgnoreCase("Active")).count());
        model.addAttribute("totalCollectedFees", (long) totalCollectedFees);
        model.addAttribute("totalPendingFees", (long) totalPendingFees);
        model.addAttribute("totalFeesDemand", (long) totalFeesDemand);
        model.addAttribute("feeCollectionRate", totalFeesDemand > 0 ? (int) ((totalCollectedFees / totalFeesDemand) * 100) : 0);

        List<String> studentCourseLabels = chartLabels(studentByCourse);
        List<Number> studentCourseCounts = chartValues(studentByCourse);
        List<String> academicMixLabels = chartLabels(studentByCourse);
        List<Number> academicMixCounts = chartValues(studentByCourse);
        List<String> feeCourseLabels = chartLabels(feesByCourse);
        List<Number> feeCourseTotals = chartValues(feesByCourse);

        model.addAttribute("studentCourseLabels", studentCourseLabels);
        model.addAttribute("studentCourseCounts", studentCourseCounts);
        model.addAttribute("academicMixLabels", academicMixLabels);
        model.addAttribute("academicMixCounts", academicMixCounts);
        model.addAttribute("feeCourseLabels", feeCourseLabels);
        model.addAttribute("feeCourseTotals", feeCourseTotals);
        model.addAttribute("studentCourseHasData", !studentCourseLabels.isEmpty());
        model.addAttribute("academicMixHasData", !academicMixLabels.isEmpty());
        model.addAttribute("feeCourseHasData", !feeCourseLabels.isEmpty());
        model.addAttribute("tpoStatusLabels", new ArrayList<>(tpoStatusCounts.keySet()));
        model.addAttribute("tpoStatusCounts", new ArrayList<>(tpoStatusCounts.values()));
        model.addAttribute("driveCourseLabels", new ArrayList<>(driveCourseCounts.keySet()));
        model.addAttribute("driveCourseCounts", new ArrayList<>(driveCourseCounts.values()));

        addAdminAttributes(model, admin);
        return "admin/admin-reports";
    }
}//phone number remove kerna hai dasbhord se admin ke abhi admin_profile.html me me phone number show ho rha hai
