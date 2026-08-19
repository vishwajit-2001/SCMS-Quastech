package com.scms.scms.controller;

import com.scms.scms.model.*;
import com.scms.scms.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class StudyMaterialController {

    private static final String UPLOAD_DIR_STUDY_MATERIALS =
            System.getProperty("user.dir") + "/src/main/resources/static/uploads/study-materials/";

    @Autowired private StudentRepository studentRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private StudyMaterialRepository studyMaterialRepository;
    @Autowired private TimetableRepository timetableRepository;

    @GetMapping("/study-materials")
    public String studentStudyMaterials(
            Model model,
            HttpSession session,
            @RequestParam(value = "subject", required = false) String subjectFilter,
            @RequestParam(value = "semester", required = false) String semesterFilter) {

        Student student = getLoggedStudent(session);
        if (student == null) return "redirect:/login";

        addStudentShellAttributes(model, student);

        List<StudyMaterial> materials = filterMaterials(
                loadStudentMaterials(student),
                subjectFilter,
                semesterFilter
        );

        model.addAttribute("studentClassLabel", buildClassLabel(student));
        model.addAttribute("studyMaterials", materials);
        model.addAttribute("studyMaterialsBySubject", groupBySubject(materials));
        model.addAttribute("studyMaterialCount", materials.size());
        model.addAttribute("studyMaterialSubjectCount", materials.stream()
                .map(material -> normalizeText(material.getSubject(), "Unassigned"))
                .distinct()
                .count());
        model.addAttribute("studyMaterialTeachers", materials.stream()
                .map(material -> resolveTeacherName(material.getTeacher()))
                .distinct()
                .count());
        model.addAttribute("availableStudyMaterialSubjects", collectDistinctValues(materials, StudyMaterial::getSubject));
        model.addAttribute("availableStudyMaterialSemesters", collectDistinctValues(materials, StudyMaterial::getSemester));
        model.addAttribute("selectedStudyMaterialSubject", normalizeFilter(subjectFilter));
        model.addAttribute("selectedStudyMaterialSemester", normalizeFilter(semesterFilter));
        model.addAttribute("studyMaterialEmptyMessage",
                "No uploaded materials match the current filters.");

        return "student/connected/study-materials";
    }

    @GetMapping("/teacher-study-materials")
    public String teacherStudyMaterials(
            Model model,
            HttpSession session,
            @RequestParam(value = "subject", required = false) String subjectFilter,
            @RequestParam(value = "semester", required = false) String semesterFilter) {

        Teacher teacher = getLoggedTeacher(session);
        if (teacher == null) return "redirect:/login";

        addTeacherShellAttributes(model, teacher);

        List<StudyMaterial> materials = loadTeacherMaterials(teacher);
        ClassRoom classRoom = resolveTeacherClassRoom(teacher);
        model.addAttribute("teacherStudyMaterials", materials);
        model.addAttribute("teacherStudyMaterialCount", materials.size());
        model.addAttribute("teacherStudyMaterialSubjectCount", materials.stream()
                .map(material -> normalizeText(material.getSubject(), "Unassigned"))
                .distinct()
                .count());
        model.addAttribute("teacherDefaultSubject", teacher.getSubject() != null && !teacher.getSubject().isBlank()
                ? teacher.getSubject()
                : "General");
        model.addAttribute("teacherDefaultSemester", classRoom != null && classRoom.getSemester() != null
                ? String.valueOf(classRoom.getSemester())
                : "");
        return "teacher/teacher-study-materials";
    }

    @PostMapping("/teacher-study-materials/upload")
    public String uploadStudyMaterial(
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "subject", required = false) String subject,
            @RequestParam(value = "semester", required = false) String semester,
            @RequestParam("file") MultipartFile file,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Teacher teacher = getLoggedTeacher(session);
        if (teacher == null) return "redirect:/login";

        ClassRoom classRoom = resolveTeacherClassRoom(teacher);
        if (classRoom == null) {
            redirectAttributes.addFlashAttribute("teacherStudyMaterialError",
                    "Assign a class to this teacher before uploading resources.");
            return "redirect:/teacher-study-materials";
        }

        if (file == null || file.isEmpty()) {
            redirectAttributes.addFlashAttribute("teacherStudyMaterialError", "Please select a file to upload.");
            return "redirect:/teacher-study-materials";
        }

        String savedPath = saveStudyMaterial(file);
        if (savedPath == null) {
            redirectAttributes.addFlashAttribute("teacherStudyMaterialError",
                    "Upload failed. Use a PDF, DOC, DOCX, PPT, PPTX, TXT, XLS, or XLSX file under 25 MB.");
            return "redirect:/teacher-study-materials";
        }

        StudyMaterial material = new StudyMaterial();
        material.setTitle(normalizeText(title, "Untitled resource"));
        material.setDescription(normalizeText(description, ""));
        material.setSubject(normalizeText(subject, normalizeText(teacher.getSubject(), "General")));
        material.setSemester(normalizeText(semester,
                classRoom.getSemester() != null ? String.valueOf(classRoom.getSemester()) : ""));
        material.setCourse(classRoom.getCourse());
        material.setSection(classRoom.getSection());
        material.setAcademicYear(classRoom.getAcademicYear());
        material.setFilePath(savedPath);
        material.setOriginalFileName(file.getOriginalFilename());
        material.setStoredFileName(savedPath.substring(savedPath.lastIndexOf('/') + 1));
        material.setContentType(file.getContentType());
        material.setFileSize(file.getSize());
        material.setUploadedAt(LocalDateTime.now());
        material.setTeacher(teacher);
        material.setClassRoom(classRoom);
        material.setAdmin(teacher.getAdmin());
        studyMaterialRepository.save(material);

        redirectAttributes.addFlashAttribute("teacherStudyMaterialSuccess", "Study material uploaded successfully.");
        return "redirect:/teacher-dashboard#study-materials";
    }

    @GetMapping("/study-materials/view/{id}")
    public ResponseEntity<Resource> viewMaterial(@PathVariable Long id, HttpSession session) {
        return serveMaterial(id, session, false);
    }

    @GetMapping("/study-materials/download/{id}")
    public ResponseEntity<Resource> downloadMaterial(@PathVariable Long id, HttpSession session) {
        return serveMaterial(id, session, true);
    }

    private ResponseEntity<Resource> serveMaterial(Long id, HttpSession session, boolean download) {
        StudyMaterial material = studyMaterialRepository.findById(id).orElse(null);
        if (material == null) {
            return ResponseEntity.notFound().build();
        }

        if (!canAccessMaterial(material, session)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            Path filePath = resolvePhysicalPath(material.getFilePath());
            if (filePath == null || !Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = material.getContentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = Files.probeContentType(filePath);
            }
            if (contentType == null || contentType.isBlank()) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            String dispositionType = download ? "attachment" : "inline";
            String fileName = material.getOriginalFileName();
            if (fileName == null || fileName.isBlank()) {
                fileName = filePath.getFileName().toString();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.builder(dispositionType)
                                    .filename(fileName)
                                    .build()
                                    .toString())
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private List<StudyMaterial> loadStudentMaterials(Student student) {
        ClassRoom classRoom = student.getClassRoom();
        List<StudyMaterial> all = classRoom != null
                ? studyMaterialRepository.findByClassRoomOrderByUploadedAtDesc(classRoom)
                : studyMaterialRepository.findAllByOrderByUploadedAtDesc();

        if (all.isEmpty() && student.getAdmin() != null) {
            all = studyMaterialRepository.findByAdminOrderByUploadedAtDesc(student.getAdmin());
        }

        return all.stream()
                .filter(material -> materialMatchesStudent(material, student))
                .toList();
    }

    private List<StudyMaterial> loadTeacherMaterials(Teacher teacher) {
        ClassRoom classRoom = resolveTeacherClassRoom(teacher);
        List<StudyMaterial> all = classRoom != null
                ? studyMaterialRepository.findByClassRoomOrderByUploadedAtDesc(classRoom)
                : studyMaterialRepository.findAllByOrderByUploadedAtDesc();

        if (all.isEmpty() && teacher.getAdmin() != null) {
            all = studyMaterialRepository.findByAdminOrderByUploadedAtDesc(teacher.getAdmin());
        }

        return all.stream()
                .filter(material -> materialMatchesTeacher(material, teacher))
                .toList();
    }

    private boolean materialMatchesStudent(StudyMaterial material, Student student) {
        ClassRoom studentClass = student.getClassRoom();
        if (studentClass != null && material.getClassRoom() != null) {
            return Objects.equals(studentClass.getId(), material.getClassRoom().getId());
        }

        String studentCourse = normalizeFilter(student.getCourse());
        String studentSemester = normalizeFilter(student.getSemester());
        boolean courseMatches = studentCourse.isBlank() || studentCourse.equals(normalizeFilter(material.getCourse()));
        boolean semesterMatches = studentSemester.isBlank() || studentSemester.equals(normalizeFilter(material.getSemester()));
        return courseMatches && semesterMatches;
    }

    private boolean materialMatchesTeacher(StudyMaterial material, Teacher teacher) {
        if (teacher.getId() != null && material.getTeacher() != null) {
            return Objects.equals(teacher.getId(), material.getTeacher().getId());
        }
        ClassRoom classRoom = resolveTeacherClassRoom(teacher);
        if (classRoom != null && material.getClassRoom() != null) {
            return Objects.equals(classRoom.getId(), material.getClassRoom().getId());
        }
        if (teacher.getAdmin() != null && material.getAdmin() != null) {
            return Objects.equals(teacher.getAdmin().getId(), material.getAdmin().getId());
        }
        return true;
    }

    private List<StudyMaterial> filterMaterials(List<StudyMaterial> materials, String subjectFilter, String semesterFilter) {
        String subject = normalizeFilter(subjectFilter);
        String semester = normalizeFilter(semesterFilter);

        return materials.stream()
                .filter(material -> subject.isBlank() || subject.equals(normalizeFilter(material.getSubject())))
                .filter(material -> semester.isBlank() || semester.equals(normalizeFilter(material.getSemester())))
                .toList();
    }

    private Map<String, List<StudyMaterial>> groupBySubject(List<StudyMaterial> materials) {
        LinkedHashMap<String, List<StudyMaterial>> grouped = new LinkedHashMap<>();
        for (StudyMaterial material : materials) {
            String key = normalizeText(material.getSubject(), "Unassigned");
            grouped.computeIfAbsent(key, unused -> new ArrayList<>()).add(material);
        }
        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .sorted(Comparator.comparing(StudyMaterial::getUploadedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                                .toList(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private List<String> collectDistinctValues(List<StudyMaterial> materials,
                                               java.util.function.Function<StudyMaterial, String> mapper) {
        LinkedHashMap<String, String> valuesByKey = new LinkedHashMap<>();
        for (StudyMaterial material : materials) {
            String value = mapper.apply(material);
            if (value == null || value.isBlank()) {
                continue;
            }
            String normalized = value.trim().toLowerCase(Locale.ENGLISH);
            valuesByKey.putIfAbsent(normalized, value.trim());
        }
        return valuesByKey.values().stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private String saveStudyMaterial(MultipartFile file) {
        String contentType = file.getContentType();
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), ""));
        String extension = "";
        if (originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        if (!isAllowedMaterialFile(contentType, extension, originalFilename)) {
            return null;
        }

        if (file.getSize() > 25L * 1024 * 1024) {
            return null;
        }

        File dir = new File(UPLOAD_DIR_STUDY_MATERIALS);
        if (!dir.exists() && !dir.mkdirs()) {
            return null;
        }

        String storedName = UUID.randomUUID() + extension;
        try {
            Files.copy(file.getInputStream(), Paths.get(UPLOAD_DIR_STUDY_MATERIALS + storedName),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/study-materials/" + storedName;
        } catch (IOException e) {
            return null;
        }
    }

    private boolean isAllowedMaterialFile(String contentType, String extension, String originalFilename) {
        String normalizedExtension = normalizeFilter(extension);
        String normalizedName = normalizeFilter(originalFilename);

        Set<String> allowedExtensions = Set.of(
                ".pdf", ".ppt", ".pptx", ".doc", ".docx", ".txt", ".xls", ".xlsx", ".csv", ".png", ".jpg", ".jpeg", ".webp"
        );
        Set<String> allowedContentTypes = Set.of(
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "text/plain",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "text/csv",
                "image/png",
                "image/jpeg",
                "image/webp"
        );

        if (allowedExtensions.stream().noneMatch(normalizedExtension::endsWith)) {
            return false;
        }
        if (contentType == null || contentType.isBlank()) {
            return true;
        }
        return allowedContentTypes.contains(contentType.toLowerCase(Locale.ENGLISH))
                || normalizedName.endsWith(".pdf")
                || normalizedName.endsWith(".ppt")
                || normalizedName.endsWith(".pptx")
                || normalizedName.endsWith(".doc")
                || normalizedName.endsWith(".docx")
                || normalizedName.endsWith(".txt")
                || normalizedName.endsWith(".xls")
                || normalizedName.endsWith(".xlsx")
                || normalizedName.endsWith(".csv")
                || normalizedName.endsWith(".png")
                || normalizedName.endsWith(".jpg")
                || normalizedName.endsWith(".jpeg")
                || normalizedName.endsWith(".webp");
    }

    private Path resolvePhysicalPath(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return null;
        }

        String clean = filePath.startsWith("/") ? filePath.substring(1) : filePath;
        Path staticRoot = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "static");
        return staticRoot.resolve(clean).normalize();
    }

    private Student getLoggedStudent(HttpSession session) {
        String email = (String) session.getAttribute("loggedInUser");
        String role = (String) session.getAttribute("userRole");
        if (email == null || (role != null && !"STUDENT".equalsIgnoreCase(role))) {
            return null;
        }
        return studentRepository.findByEmail(email);
    }

    private Teacher getLoggedTeacher(HttpSession session) {
        String email = (String) session.getAttribute("loggedInUser");
        String role = (String) session.getAttribute("userRole");
        if (email == null || (role != null && !"TEACHER".equalsIgnoreCase(role))) {
            return null;
        }
        return teacherRepository.findByEmail(email);
    }

    private boolean canAccessMaterial(StudyMaterial material, HttpSession session) {
        String email = (String) session.getAttribute("loggedInUser");
        String role = (String) session.getAttribute("userRole");
        if (email == null || role == null) {
            return false;
        }

        if ("STUDENT".equalsIgnoreCase(role)) {
            Student student = studentRepository.findByEmail(email);
            return student != null && materialMatchesStudent(material, student);
        }

        if ("TEACHER".equalsIgnoreCase(role)) {
            Teacher teacher = teacherRepository.findByEmail(email);
            return teacher != null && materialMatchesTeacher(material, teacher);
        }

        return true;
    }

    private void addStudentShellAttributes(Model model, Student student) {
        model.addAttribute("student", student);
        model.addAttribute("studentName", normalizeText(student.getName(), "Student"));
        model.addAttribute("studentCourse", normalizeText(student.getCourse(), "Student"));
        model.addAttribute("studentEmail", normalizeText(student.getEmail(), "Not available"));
        model.addAttribute("collegeName", resolveCollegeName(student));
        model.addAttribute("collegeLogoPath", resolveCollegeLogo(student));
        model.addAttribute("collegeInitials", buildCollegeInitials(resolveCollegeName(student)));
        model.addAttribute("studentId", resolveStudentId(student));
        model.addAttribute("studentRollNo", normalizeText(student.getRollNo(), "Not assigned"));
        model.addAttribute("studentSemesterLabel", normalizeSemester(student));
        model.addAttribute("studentSectionLabel", normalizeSection(student));
        model.addAttribute("studentAcademicYear", normalizeText(student.getAcademicYear(), "Not assigned"));
        model.addAttribute("studentClassLabel", buildClassLabel(student));
        model.addAttribute("studentClassMentor", "Faculty not assigned");
        model.addAttribute("studentPhoto", student.getPhoto());
    }

    private void addTeacherShellAttributes(Model model, Teacher teacher) {
        ClassRoom classRoom = resolveTeacherClassRoom(teacher);
        model.addAttribute("teacher", teacher);
        model.addAttribute("teacherName", normalizeText(teacher.getName(), "Teacher"));
        model.addAttribute("teacherEmail", normalizeText(teacher.getEmail(), "Not available"));
        model.addAttribute("teacherSubject", normalizeText(teacher.getSubject(), "Subject not assigned"));
        model.addAttribute("teacherClassLabel", classRoom != null ? classRoom.getDisplayLabel() : "No class assigned");
        model.addAttribute("teacherPhoto", teacher.getPhoto());
        model.addAttribute("teacherInitials", buildTeacherInitials(teacher.getName()));
        model.addAttribute("collegeName", teacher.getAdmin() != null ? normalizeText(teacher.getAdmin().getCollegeName(), "AI Campus Institute") : "AI Campus Institute");
    }

    private ClassRoom resolveTeacherClassRoom(Teacher teacher) {
        if (teacher == null) {
            return null;
        }
        if (teacher.getClassRoom() != null && teacher.getClassRoom().getId() != null) {
            return teacher.getClassRoom();
        }
        for (Timetable entry : timetableRepository.findByTeacher(teacher)) {
            if (entry != null && entry.getClassRoom() != null && entry.getClassRoom().getId() != null) {
                return entry.getClassRoom();
            }
        }
        return null;
    }

    private String resolveStudentId(Student student) {
        if (student.getRegistrationNo() != null && !student.getRegistrationNo().isBlank()) return student.getRegistrationNo();
        if (student.getEnrollmentNo() != null && !student.getEnrollmentNo().isBlank()) return student.getEnrollmentNo();
        if (student.getRollNo() != null && !student.getRollNo().isBlank()) return student.getRollNo();
        return student.getId() != null ? "STU-" + student.getId() : "Not assigned";
    }

    private String buildClassLabel(Student student) {
        ClassRoom classRoom = student.getClassRoom();
        if (classRoom != null) {
            String label = classRoom.getDisplayLabel();
            if (label != null && !label.isBlank()) return label.replace("Ã¢â‚¬â€œ", "-");
        }
        List<String> parts = new ArrayList<>();
        if (student.getCourse() != null && !student.getCourse().isBlank()) parts.add(student.getCourse());
        if (student.getSemester() != null && !student.getSemester().isBlank()) parts.add("Sem " + student.getSemester());
        if (student.getSectionName() != null && !student.getSectionName().isBlank()) parts.add("Section " + student.getSectionName());
        return parts.isEmpty() ? "Class not assigned" : String.join(" - ", parts);
    }

    private String normalizeSemester(Student student) {
        if (student.getSemester() != null && !student.getSemester().isBlank()) {
            return "Sem " + student.getSemester();
        }
        if (student.getClassRoom() != null && student.getClassRoom().getSemester() != null) {
            return "Sem " + student.getClassRoom().getSemester();
        }
        return "Semester not assigned";
    }

    private String normalizeSection(Student student) {
        if (student.getSectionName() != null && !student.getSectionName().isBlank()) {
            return "Section " + student.getSectionName();
        }
        if (student.getClassRoom() != null && student.getClassRoom().getSection() != null
                && !student.getClassRoom().getSection().isBlank()) {
            return "Section " + student.getClassRoom().getSection();
        }
        return "Section not assigned";
    }

    private String resolveTeacherName(Teacher teacher) {
        return teacher != null ? normalizeText(teacher.getName(), "Faculty not assigned") : "Faculty not assigned";
    }

    private String buildTeacherInitials(String name) {
        if (name == null || name.isBlank()) {
            return "TE";
        }
        String[] parts = name.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (String part : parts) {
            if (!part.isBlank()) {
                initials.append(Character.toUpperCase(part.charAt(0)));
                if (initials.length() == 2) {
                    break;
                }
            }
        }
        return initials.length() > 0 ? initials.toString() : "TE";
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

    private String resolveCollegeName(Student student) {
        if (student != null && student.getAdmin() != null) {
            return normalizeText(student.getAdmin().getCollegeName(), "AI Campus Institute");
        }
        return "AI Campus Institute";
    }

    private String resolveCollegeLogo(Student student) {
        if (student != null && student.getAdmin() != null && student.getAdmin().getCollege() != null) {
            return student.getAdmin().getCollege().getLogoPath();
        }
        return null;
    }

    private String normalizeText(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value.trim();
    }

    private String normalizeFilter(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ENGLISH);
    }
}
