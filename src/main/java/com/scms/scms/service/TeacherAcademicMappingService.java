package com.scms.scms.service;

import com.scms.scms.model.*;
import com.scms.scms.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TeacherAcademicMappingService {

    private final TeacherAcademicMappingRepository mappingRepository;
    private final AcademicStructureRepository academicStructureRepository;
    private final StudentRepository studentRepository;
    private final SubjectRepository subjectRepository;
    private final TimetableRepository timetableRepository;
    private final AcademicStructureService academicStructureService;

    public TeacherAcademicMappingService(
            TeacherAcademicMappingRepository mappingRepository,
            AcademicStructureRepository academicStructureRepository,
            StudentRepository studentRepository,
            SubjectRepository subjectRepository,
            TimetableRepository timetableRepository,
            AcademicStructureService academicStructureService) {
        this.mappingRepository = mappingRepository;
        this.academicStructureRepository = academicStructureRepository;
        this.studentRepository = studentRepository;
        this.subjectRepository = subjectRepository;
        this.timetableRepository = timetableRepository;
        this.academicStructureService = academicStructureService;
    }

    public List<TeacherAcademicMapping> findByTeacher(Teacher teacher) {
        if (teacher == null) return List.of();
        Admin admin = teacher.getAdmin();
        return admin != null
                ? mappingRepository.findByAdminAndTeacherOrderByCourse_CodeAscBatch_DisplayNameAscAcademicYearAsc(admin, teacher)
                : mappingRepository.findByTeacherOrderByCourse_CodeAscBatch_DisplayNameAscAcademicYearAsc(teacher);
    }

    public List<TeacherAcademicMapping> findByAdmin(Admin admin) {
        if (admin == null) return List.of();
        return mappingRepository.findByAdminOrderByTeacher_NameAscCourse_CodeAscBatch_DisplayNameAscAcademicYearAsc(admin);
    }

    @Transactional
    public AssignmentResult assign(
            Admin admin,
            Teacher teacher,
            Course course,
            Batch batch,
            String academicYear) {
        List<String> errors = new ArrayList<>();
        if (admin == null) errors.add("Admin session is required.");
        if (teacher == null) errors.add("Select a valid teacher.");
        if (course == null) errors.add("Select a valid course.");
        if (batch == null) errors.add("Select a valid batch.");
        if (academicYear == null || academicYear.isBlank()) errors.add("Academic year is required.");
        if (!errors.isEmpty()) return new AssignmentResult(0, errors);

        if (!sameId(admin, teacher.getAdmin()) || !sameId(admin, course.getAdmin()) || !sameId(admin, batch.getAdmin())) {
            errors.add("Teacher, course, and batch must belong to the same college admin.");
            return new AssignmentResult(0, errors);
        }
        if (batch.getCourse() == null || !sameId(course, batch.getCourse())) {
            errors.add("Selected batch does not belong to the selected course.");
            return new AssignmentResult(0, errors);
        }

        boolean duplicate = mappingRepository.existsByTeacherAndCourseAndBatchAndAcademicYearIgnoreCase(
                teacher, course, batch, academicYear.trim());
        if (duplicate) {
            errors.add("This teacher is already assigned to the selected course, batch, and academic year.");
            return new AssignmentResult(0, errors);
        }

        TeacherAcademicMapping mapping = new TeacherAcademicMapping();
        mapping.setAdmin(admin);
        mapping.setTeacher(teacher);
        mapping.setCourse(course);
        mapping.setBatch(batch);
        mapping.setAcademicYear(academicYear.trim());
        mappingRepository.save(mapping);
        return new AssignmentResult(1, errors);
    }

    public List<Subject> subjectsForScope(Admin admin, Course course, Batch batch, Integer semester) {
        if (admin == null || course == null || batch == null || semester == null) return List.of();
        return subjectRepository.findByAdminAndBatchRefAndCourseRefAndSemesterAndStatusIgnoreCaseOrderByNameAscCodeAsc(
                admin, batch, course, semester, "active");
    }

    public List<Subject> subjectsForTeacher(Teacher teacher) {
        LinkedHashMap<Long, Subject> subjects = new LinkedHashMap<>();
        for (TeacherAcademicMapping mapping : findByTeacher(teacher)) {
            for (Subject subject : subjectRepository.findByAdminAndCourseRefAndBatchRefOrderBySemesterAscNameAsc(
                    mapping.getAdmin(), mapping.getCourse(), mapping.getBatch())) {
                if (subject != null && subject.getId() != null) {
                    subjects.putIfAbsent(subject.getId(), subject);
                }
            }
        }
        return new ArrayList<>(subjects.values());
    }

    public List<AcademicStructure> academicStructuresForTeacher(Teacher teacher) {
        LinkedHashMap<Long, AcademicStructure> structures = new LinkedHashMap<>();
        for (TeacherAcademicMapping mapping : findByTeacher(teacher)) {
            academicStructureRepository.findByAdminAndCourseAndBatchOrderByYearLabelAscSemesterNumberAscSectionAsc(
                            mapping.getAdmin(), mapping.getCourse(), mapping.getBatch())
                    .stream()
                    .filter(row -> academicYearMatches(row.getYearLabel(), mapping.getAcademicYear()))
                    .forEach(row -> structures.putIfAbsent(row.getId(), row));
        }
        return new ArrayList<>(structures.values());
    }

    public List<Student> studentsForTeacher(Teacher teacher) {
        if (teacher == null || teacher.getAdmin() == null) return List.of();
        List<TeacherAcademicMapping> mappings = findByTeacher(teacher);
        if (mappings.isEmpty()) return List.of();
        return studentRepository.findByAdmin(teacher.getAdmin()).stream()
                .peek(academicStructureService::syncStudentProgression)
                .filter(student -> mappings.stream().anyMatch(mapping -> studentMatchesMapping(student, mapping)))
                .sorted(Comparator.comparing(Student::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    public List<Timetable> timetableForTeacher(Teacher teacher) {
        if (teacher == null || teacher.getAdmin() == null) return List.of();
        List<TeacherAcademicMapping> mappings = findByTeacher(teacher);
        LinkedHashMap<Long, Timetable> entries = new LinkedHashMap<>();
        for (Timetable entry : timetableRepository.findByTeacher(teacher)) {
            if (entry != null && entry.getId() != null) {
                entries.put(entry.getId(), entry);
            }
        }
        for (Timetable entry : timetableRepository.findByAdmin(teacher.getAdmin())) {
            if (entry == null || entry.getId() == null) continue;
            if (mappings.stream().anyMatch(mapping -> timetableMatchesMapping(entry, mapping))) {
                entries.putIfAbsent(entry.getId(), entry);
            }
        }
        return new ArrayList<>(entries.values());
    }

    public TeacherScopeView primaryScope(Teacher teacher) {
        List<TeacherAcademicMapping> mappings = findByTeacher(teacher);
        if (mappings.isEmpty()) {
            return TeacherScopeView.empty();
        }
        TeacherAcademicMapping first = mappings.get(0);
        List<Subject> subjects = subjectsForTeacher(teacher);
        String subjectText = subjects.stream()
                .map(subject -> subject.getName() != null ? subject.getName() : subject.getCode())
                .filter(Objects::nonNull)
                .collect(Collectors.joining(", "));
        return new TeacherScopeView(
                first.getCourse(),
                first.getBatch(),
                first.getAcademicYear(),
                null,
                null,
                null,
                subjects,
                subjectText
        );
    }

    private boolean subjectBelongsToScope(Admin admin, Subject subject, Course course, Batch batch, Integer semester) {
        return subject.getAdmin() != null && sameId(admin, subject.getAdmin())
                && subject.getCourseRef() != null && sameId(course, subject.getCourseRef())
                && subject.getBatchRef() != null && sameId(batch, subject.getBatchRef())
                && Objects.equals(subject.getSemester(), semester);
    }

    private boolean studentMatchesMapping(Student student, TeacherAcademicMapping mapping) {
        if (student == null || mapping == null) return false;
        if (student.getBatch() != null && mapping.getBatch() != null && !sameId(student.getBatch(), mapping.getBatch())) {
            return false;
        }
        if (student.getBatch() == null && mapping.getBatch() != null && !sameText(studentBatchLabel(student), mapping.getBatch().getDisplayName())) {
            return false;
        }
        Course course = mapping.getCourse();
        if (course != null && !courseMatchesStudent(student, course)) {
            return false;
        }
        String academicYear = firstNonBlank(student.getAcademicYear(), student.getClassRoom() != null ? student.getClassRoom().getAcademicYear() : null);
        if (academicYear != null && !academicYearMatches(academicYear, mapping.getAcademicYear())) {
            return false;
        }
        return true;
    }

    private boolean timetableMatchesMapping(Timetable entry, TeacherAcademicMapping mapping) {
        if (entry == null || mapping == null) return false;
        if (entry.getCourse() != null && !sameId(entry.getCourse(), mapping.getCourse())) return false;
        if (entry.getBatch() != null && !sameId(entry.getBatch(), mapping.getBatch())) return false;
        if (entry.getAcademicYear() != null && !entry.getAcademicYear().isBlank() && !academicYearMatches(entry.getAcademicYear(), mapping.getAcademicYear())) return false;
        if (entry.getClassRoom() != null) {
            ClassRoom room = entry.getClassRoom();
            if (room.getAcademicYear() != null && !room.getAcademicYear().isBlank() && !academicYearMatches(room.getAcademicYear(), mapping.getAcademicYear())) return false;
            if (mapping.getCourse() != null && !sameText(room.getCourse(), mapping.getCourse().getCode()) && !sameText(room.getCourseCode(), mapping.getCourse().getCode()) && !sameText(room.getCourse(), mapping.getCourse().getName())) return false;
            if (mapping.getBatch() != null && !sameText(room.getBatchName(), mapping.getBatch().getDisplayName())) return false;
        }
        return true;
    }

    private boolean courseMatchesStudent(Student student, Course course) {
        return sameText(student.getCourse(), course.getCode())
                || sameText(student.getCourse(), course.getName())
                || (student.getClassRoom() != null
                    && (sameText(student.getClassRoom().getCourse(), course.getCode())
                        || sameText(student.getClassRoom().getCourseCode(), course.getCode())
                        || sameText(student.getClassRoom().getCourse(), course.getName())));
    }

    private String studentBatchLabel(Student student) {
        if (student == null || student.getClassRoom() == null) return null;
        return firstNonBlank(student.getClassRoom().getBatchName(), student.getClassRoom().getBatch());
    }

    private Integer parseSemester(String value) {
        if (value == null) return null;
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.isBlank()) return null;
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean academicYearMatches(String left, String right) {
        return left == null || right == null || left.isBlank() || right.isBlank() || left.trim().equalsIgnoreCase(right.trim());
    }

    private boolean sameId(Object left, Object right) {
        Long leftId = idOf(left);
        Long rightId = idOf(right);
        return leftId != null && leftId.equals(rightId);
    }

    private Long idOf(Object value) {
        if (value instanceof Teacher teacher) return teacher.getId();
        if (value instanceof Course course) return course.getId();
        if (value instanceof Batch batch) return batch.getId();
        if (value instanceof Subject subject) return subject.getId();
        if (value instanceof Admin admin) return admin.getId();
        return null;
    }

    private boolean sameText(String left, String right) {
        return left != null && right != null && !left.isBlank() && !right.isBlank() && left.trim().equalsIgnoreCase(right.trim());
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value != null && !value.isBlank()) return value.trim();
        }
        return null;
    }

    public record AssignmentResult(int savedCount, List<String> errors) {}

    public record TeacherScopeView(
            Course course,
            Batch batch,
            String academicYear,
            Integer semester,
            String section,
            String roomNumber,
            List<Subject> subjects,
            String subjectText
    ) {
        static TeacherScopeView empty() {
            return new TeacherScopeView(null, null, null, null, null, null, List.of(), "");
        }
    }
}
