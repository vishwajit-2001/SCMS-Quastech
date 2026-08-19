package com.scms.scms.service;

import com.scms.scms.model.AcademicStructure;
import com.scms.scms.model.Admin;
import com.scms.scms.model.Batch;
import com.scms.scms.model.ClassRoom;
import com.scms.scms.model.Course;
import com.scms.scms.model.Student;
import com.scms.scms.model.Subject;
import com.scms.scms.model.Teacher;
import com.scms.scms.model.Fees;
import com.scms.scms.model.Timetable;
import com.scms.scms.repository.AcademicStructureRepository;
import com.scms.scms.repository.BatchRepository;
import com.scms.scms.repository.ClassRoomRepository;
import com.scms.scms.repository.CourseRepository;
import com.scms.scms.repository.FeesRepository;
import com.scms.scms.repository.StudentRepository;
import com.scms.scms.repository.SubjectRepository;
import com.scms.scms.repository.TeacherRepository;
import com.scms.scms.repository.TimetableRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AcademicStructureService {

    private static final String DEFAULT_SECTION = "";
    private static final int ACADEMIC_YEAR_START_MONTH = 6;
    private static final int EVEN_SEMESTER_SWITCH_MONTH = 12;

    @Autowired private AcademicStructureRepository academicStructureRepository;
    @Autowired private BatchRepository batchRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private ClassRoomRepository classRoomRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private TimetableRepository timetableRepository;
    @Autowired private FeesRepository feesRepository;

    public record StructureMatrixRow(
            Long structureId,
            Long courseId,
            String courseCode,
            String courseName,
            Long batchId,
            String batchName,
            String academicYear,
            String yearLabel,
            String yearDisplay,
            Integer semesterNumber,
            String semesterLabel,
            String section
    ) {}

    public record BatchPlan(
            Long batchId,
            Long courseId,
            String batchName,
            String courseCode,
            String courseName,
            List<StructureMatrixRow> rows
    ) {}

    public record StructureSyncResult(
            BatchPlan batchPlan,
            int createdRows
    ) {}

    public record StructureMutationResult(
            boolean success,
            String message,
            int studentsUpdated,
            int classesUpdated,
            int timetableUpdated,
            int feesUpdated,
            int subjectsUpdated
    ) {}

    public record ValidationResult(
            boolean valid,
            String message,
            String normalizedYearLabel
    ) {}

    public record StudentProgression(
            String courseLabel,
            String batchLabel,
            String academicYear,
            String yearLabel,
            Integer semesterNumber,
            String semesterLabel
    ) {}

    public record TeacherScopeSummary(
            String batchLabel,
            String yearLabel,
            Integer semesterNumber,
            String semesterLabel,
            List<String> assignedSubjects
    ) {}

    public List<StructureMatrixRow> syncAdminStructures(Admin admin) {
        return listAdminStructureRows(admin);
    }

    public List<StructureMatrixRow> listAdminStructureRows(Admin admin) {
        if (admin == null) {
            return List.of();
        }
        return academicStructureRepository.findByAdminOrderByBatch_DisplayNameAscYearLabelAscSemesterNumberAscSectionAsc(admin)
                .stream()
                .filter(structure -> structure != null && structure.getCourse() != null && structure.getBatch() != null)
                .map(this::toMatrixRow)
                .sorted(Comparator
                        .comparing(StructureMatrixRow::courseCode, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(StructureMatrixRow::batchName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(StructureMatrixRow::academicYear, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(StructureMatrixRow::semesterNumber)
                        .thenComparing(StructureMatrixRow::section, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public StructureSyncResult syncBatchStructure(Admin admin, Course course, Batch batch) {
        BatchPlan batchPlan = buildBatchPlan(admin, course, batch);
        return new StructureSyncResult(batchPlan, 0);
    }

    public ValidationResult validateSelection(Admin admin, Course course, Batch batch, String yearLabel, Integer semester, String section) {
        String normalizedYear = normalizeYearLabel(yearLabel);
        String normalizedSection = normalizeSection(section);
        if (course == null || batch == null || normalizedYear == null || semester == null || normalizedSection == null) {
            return new ValidationResult(false, "Course, batch, year, semester, and section are required.", normalizedYear);
        }

        Map<String, Set<Integer>> validMap = buildYearSemesterMap(course);
        Set<Integer> allowedSemesters = validMap.get(normalizedYear);
        if (allowedSemesters == null || !allowedSemesters.contains(semester)) {
            return new ValidationResult(false,
                    normalizedYear + " only allows " + formatSemesterList(allowedSemesters) + ".",
                    normalizedYear);
        }

        List<AcademicStructure> existing = academicStructureRepository
                .findByAdminAndCourseAndBatchOrderByYearLabelAscSemesterNumberAscSectionAsc(admin, course, batch);

        for (AcademicStructure row : existing) {
            if (row == null || row.getSemesterNumber() == null) {
                continue;
            }
            String existingYear = normalizeYearLabel(row.getYearLabel());
            String existingSection = normalizeSection(row.getSection());
            if (semester.equals(row.getSemesterNumber()) && Objects.equals(existingSection, normalizedSection)
                    && !Objects.equals(existingYear, normalizedYear)) {
                return new ValidationResult(false,
                        "Semester " + semester + " is already mapped to " + existingYear + " for Section " + normalizedSection + ".",
                        normalizedYear);
            }
            if (semester.equals(row.getSemesterNumber()) && Objects.equals(existingYear, normalizedYear)
                    && Objects.equals(existingSection, normalizedSection)) {
                return new ValidationResult(false,
                        normalizedYear + ", Sem " + semester + ", Section " + normalizedSection + " already exists for this batch.",
                        normalizedYear);
            }
        }

        return new ValidationResult(true, null, normalizedYear);
    }

    public ValidationResult validateSelectionForUpdate(Admin admin,
                                                       AcademicStructure current,
                                                       Course course,
                                                       Batch batch,
                                                       String yearLabel,
                                                       Integer semester,
                                                       String section) {
        String normalizedYear = normalizeYearLabel(yearLabel);
        String normalizedSection = normalizeSection(section);
        if (current == null || course == null || batch == null || normalizedYear == null || semester == null || normalizedSection == null) {
            return new ValidationResult(false, "Course, batch, year, semester, and section are required.", normalizedYear);
        }
        if (batch.getCourse() == null || !Objects.equals(batch.getCourse().getId(), course.getId())) {
            return new ValidationResult(false, "Batch does not belong to the selected course.", normalizedYear);
        }

        Map<String, Set<Integer>> validMap = buildYearSemesterMap(course);
        Set<Integer> allowedSemesters = validMap.get(normalizedYear);
        if (allowedSemesters == null || !allowedSemesters.contains(semester)) {
            return new ValidationResult(false,
                    normalizedYear + " only allows " + formatSemesterList(allowedSemesters) + ".",
                    normalizedYear);
        }

        List<AcademicStructure> existing = academicStructureRepository
                .findByAdminAndCourseAndBatchOrderByYearLabelAscSemesterNumberAscSectionAsc(admin, course, batch);

        for (AcademicStructure row : existing) {
            if (row == null || Objects.equals(row.getId(), current.getId()) || row.getSemesterNumber() == null) {
                continue;
            }
            String existingYear = normalizeYearLabel(row.getYearLabel());
            String existingSection = normalizeSection(row.getSection());
            if (semester.equals(row.getSemesterNumber()) && Objects.equals(existingYear, normalizedYear)
                    && Objects.equals(existingSection, normalizedSection)) {
                return new ValidationResult(false,
                        normalizedYear + ", Sem " + semester + ", Section " + normalizedSection + " already exists for this batch.",
                        normalizedYear);
            }
            if (semester.equals(row.getSemesterNumber()) && Objects.equals(existingSection, normalizedSection)) {
                return new ValidationResult(false,
                        "Semester " + semester + " is already mapped to " + existingYear + " for Section " + normalizedSection + ".",
                        normalizedYear);
            }
        }

        return new ValidationResult(true, null, normalizedYear);
    }

    @Transactional
    public StructureMutationResult updateStructure(Admin admin,
                                                   Long structureId,
                                                   Course newCourse,
                                                   Batch newBatch,
                                                   String yearLabel,
                                                   Integer semester,
                                                   String section) {
        AcademicStructure structure = academicStructureRepository.findById(structureId).orElse(null);
        if (structure == null || structure.getAdmin() == null || admin == null
                || !Objects.equals(structure.getAdmin().getId(), admin.getId())) {
            return new StructureMutationResult(false, "Academic structure row not found.", 0, 0, 0, 0, 0);
        }

        ValidationResult validation = validateSelectionForUpdate(admin, structure, newCourse, newBatch, yearLabel, semester, section);
        if (!validation.valid()) {
            return new StructureMutationResult(false, validation.message(), 0, 0, 0, 0, 0);
        }

        StructureMatrixRow oldRow = toMatrixRow(structure);
        structure.setCourse(newCourse);
        structure.setBatch(newBatch);
        structure.setYearLabel(validation.normalizedYearLabel());
        structure.setSemesterNumber(semester);
        structure.setSection(normalizeSection(section));
        academicStructureRepository.save(structure);

        StructureMatrixRow newRow = toMatrixRow(structure);
        return cascadeStructureChange(admin, oldRow, newRow, "Academic structure updated successfully.");
    }

    @Transactional
    public StructureMutationResult deleteStructure(Admin admin, Long structureId) {
        AcademicStructure structure = academicStructureRepository.findById(structureId).orElse(null);
        if (structure == null || structure.getAdmin() == null || admin == null
                || !Objects.equals(structure.getAdmin().getId(), admin.getId())) {
            return new StructureMutationResult(false, "Academic structure row not found.", 0, 0, 0, 0, 0);
        }

        StructureMatrixRow oldRow = toMatrixRow(structure);
        academicStructureRepository.delete(structure);
        StructureMutationResult cleared = cascadeStructureChange(admin, oldRow, null, "Academic structure deleted successfully.");
        return new StructureMutationResult(true, cleared.message(), cleared.studentsUpdated(), cleared.classesUpdated(),
                cleared.timetableUpdated(), cleared.feesUpdated(), cleared.subjectsUpdated());
    }

    public BatchPlan buildBatchPlan(Admin admin, Course course, Batch batch) {
        if (course == null || batch == null) {
            return new BatchPlan(
                    batch != null ? batch.getId() : null,
                    course != null ? course.getId() : null,
                    batch != null ? defaultText(batch.getDisplayName(), "Batch") : "Batch",
                    course != null ? defaultText(course.getCode(), "COURSE") : "COURSE",
                    course != null ? defaultText(course.getName(), "Course") : "Course",
                    List.of()
            );
        }

        int totalSemesters = resolveTotalSemesters(course);
        int phaseCount = Math.max(1, (int) Math.ceil(totalSemesters / 2.0));
        int batchStartYear = resolveBatchStartYear(batch);
        List<StructureMatrixRow> rows = new ArrayList<>();

        for (int phaseIndex = 0; phaseIndex < phaseCount; phaseIndex++) {
            String yearLabel = yearLabelForIndex(phaseIndex);
            String academicYear = formatAcademicYear(batchStartYear + phaseIndex);
            for (int semester = phaseIndex * 2 + 1; semester <= Math.min(totalSemesters, phaseIndex * 2 + 2); semester++) {
                rows.add(new StructureMatrixRow(
                        null,
                        course.getId(),
                        defaultText(course.getCode(), "COURSE"),
                        defaultText(course.getName(), "Course"),
                        batch.getId(),
                        defaultText(batch.getDisplayName(), buildBatchLabel(batch, course)),
                        academicYear,
                        yearLabel,
                        yearLabel + defaultText(course.getCode(), "").toUpperCase(Locale.ENGLISH),
                        semester,
                        "Sem " + semester,
                        ""
                ));
            }
        }

        return new BatchPlan(
                batch.getId(),
                course.getId(),
                defaultText(batch.getDisplayName(), buildBatchLabel(batch, course)),
                defaultText(course.getCode(), "COURSE"),
                defaultText(course.getName(), "Course"),
                rows
        );
    }

    private List<StructureMatrixRow> listRowsForBatch(Admin admin, Course course, Batch batch) {
        return academicStructureRepository
                .findByAdminAndCourseAndBatchOrderByYearLabelAscSemesterNumberAscSectionAsc(admin, course, batch)
                .stream()
                .map(this::toMatrixRow)
                .toList();
    }

    private StructureMatrixRow toMatrixRow(AcademicStructure structure) {
        Course course = structure.getCourse();
        Batch batch = structure.getBatch();
        String courseCode = defaultText(course != null ? course.getCode() : null, "COURSE");
        String courseName = defaultText(course != null ? course.getName() : null, "Course");
        String yearLabel = defaultText(normalizeYearLabel(structure.getYearLabel()), "FY");
        Integer semester = structure.getSemesterNumber();
        return new StructureMatrixRow(
                structure.getId(),
                course != null ? course.getId() : null,
                courseCode,
                courseName,
                batch != null ? batch.getId() : null,
                defaultText(batch != null ? batch.getDisplayName() : null, buildBatchLabel(batch, course)),
                academicYearForStructure(batch, yearLabel, semester),
                yearLabel,
                yearLabel + courseCode.toUpperCase(Locale.ENGLISH),
                semester,
                "Sem " + semester,
                defaultText(structure.getSection(), DEFAULT_SECTION).toUpperCase(Locale.ENGLISH)
        );
    }

    private StructureMutationResult cascadeStructureChange(Admin admin,
                                                           StructureMatrixRow oldRow,
                                                           StructureMatrixRow newRow,
                                                           String message) {
        int students = cascadeStudents(admin, oldRow, newRow);
        int timetable = cascadeTimetable(admin, oldRow, newRow);
        int fees = cascadeFees(admin, oldRow, newRow);
        int subjects = cascadeSubjects(admin, oldRow, newRow);
        int classes = cascadeClasses(admin, oldRow, newRow);
        return new StructureMutationResult(true, message, students, classes, timetable, fees, subjects);
    }

    private int cascadeStudents(Admin admin, StructureMatrixRow oldRow, StructureMatrixRow newRow) {
        int changed = 0;
        for (Student student : studentRepository.findByAdmin(admin)) {
            if (!studentMatchesRow(student, oldRow)) {
                continue;
            }
            if (newRow == null) {
                student.setClassRoom(null);
            } else {
                student.setBatch(batchRepository.findById(newRow.batchId()).orElse(null));
                student.setCourse(newRow.courseCode());
                student.setAcademicYear(newRow.academicYear());
                student.setSemester(newRow.semesterLabel());
                student.setSectionName(newRow.section());
                if (student.getClassRoom() == null || !classRoomMatchesRow(student.getClassRoom(), oldRow)) {
                    student.setClassRoom(resolveMatchingClassRoom(admin,
                            batchRepository.findById(newRow.batchId()).orElse(null),
                            courseRepository.findById(newRow.courseId()).orElse(null),
                            newRow,
                            newRow.section()));
                }
            }
            studentRepository.save(student);
            changed++;
        }
        return changed;
    }

    private int cascadeClasses(Admin admin, StructureMatrixRow oldRow, StructureMatrixRow newRow) {
        int changed = 0;
        for (ClassRoom classRoom : classRoomRepository.findByAdmin(admin)) {
            if (!classRoomMatchesRow(classRoom, oldRow)) {
                continue;
            }
            if (newRow == null) {
                continue;
            }
            classRoom.setCourse(newRow.courseName());
            classRoom.setCourseCode(newRow.courseCode());
            classRoom.setBatch(newRow.batchName());
            classRoom.setBatchName(newRow.batchName());
            Batch batch = batchRepository.findById(newRow.batchId()).orElse(null);
            classRoom.setBatchStartYear(batch != null ? batch.getStartYear() : classRoom.getBatchStartYear());
            classRoom.setBatchEndYear(batch != null ? batch.getEndYear() : classRoom.getBatchEndYear());
            classRoom.setAcademicYear(newRow.academicYear());
            classRoom.setYear(newRow.yearLabel());
            classRoom.setSemester(newRow.semesterNumber());
            classRoom.setSection(newRow.section());
            classRoomRepository.save(classRoom);
            changed++;
        }
        return changed;
    }

    private int cascadeTimetable(Admin admin, StructureMatrixRow oldRow, StructureMatrixRow newRow) {
        int changed = 0;
        for (Timetable entry : timetableRepository.findByAdmin(admin)) {
            ClassRoom classRoom = entry.getClassRoom();
            boolean matches = classRoom != null
                    ? classRoomMatchesRow(classRoom, oldRow)
                    : academicYearEquals(entry.getAcademicYear(), oldRow.academicYear());
            if (!matches) {
                continue;
            }
            if (newRow == null) {
                entry.setClassRoom(null);
            } else {
                entry.setAcademicYear(newRow.academicYear());
            }
            timetableRepository.save(entry);
            changed++;
        }
        return changed;
    }

    private int cascadeFees(Admin admin, StructureMatrixRow oldRow, StructureMatrixRow newRow) {
        int changed = 0;
        for (Fees fee : feesRepository.findByAdmin(admin)) {
            if (!feeMatchesRow(fee, oldRow)) {
                continue;
            }
            if (newRow == null) {
                continue;
            }
            fee.setCourse(newRow.courseCode());
            fee.setBatch(batchRepository.findById(newRow.batchId()).orElse(null));
            fee.setBatchName(newRow.batchName());
            fee.setAcademicYear(newRow.academicYear());
            fee.setSemester(newRow.semesterLabel());
            feesRepository.save(fee);
            changed++;
        }
        return changed;
    }

    private int cascadeSubjects(Admin admin, StructureMatrixRow oldRow, StructureMatrixRow newRow) {
        int changed = 0;
        if (newRow == null) {
            return 0;
        }
        Course newCourse = courseRepository.findById(newRow.courseId()).orElse(null);
        Batch newBatch = batchRepository.findById(newRow.batchId()).orElse(null);
        Batch oldBatch = batchRepository.findById(oldRow.batchId()).orElse(null);
        if (newCourse == null || newBatch == null) {
            return 0;
        }
        if (oldBatch == null) {
            return 0;
        }
        for (Subject subject : subjectRepository.findByAdminAndBatchRefOrderBySemesterAscNameAsc(admin, oldBatch)) {
            if (subject == null || subject.getCourseRef() == null || subject.getSemester() == null
                    || !Objects.equals(subject.getCourseRef().getId(), oldRow.courseId())
                    || !Objects.equals(subject.getSemester(), oldRow.semesterNumber())) {
                continue;
            }
            Subject duplicate = subjectRepository.findByAdminAndCourseRefAndBatchRefAndSemesterAndCodeIgnoreCase(
                    admin, newCourse, newBatch, newRow.semesterNumber(), subject.getCode());
            if (duplicate != null && !Objects.equals(duplicate.getId(), subject.getId())) {
                continue;
            }
            subject.setCourseRef(newCourse);
            subject.setBatchRef(newBatch);
            subject.setSemester(newRow.semesterNumber());
            subjectRepository.save(subject);
            changed++;
        }
        return changed;
    }

    private boolean studentMatchesRow(Student student, StructureMatrixRow row) {
        if (student == null || row == null) {
            return false;
        }
        if (student.getBatch() != null && row.batchId() != null && !Objects.equals(student.getBatch().getId(), row.batchId())) {
            return false;
        }
        if (student.getBatch() == null && !textEquals(student.getCourse(), row.courseCode()) && !textEquals(student.getCourse(), row.courseName())) {
            return false;
        }
        return academicYearEquals(student.getAcademicYear(), row.academicYear())
                && Objects.equals(parseSemesterNumber(student.getSemester()), row.semesterNumber())
                && textEquals(defaultText(student.getSectionName(), row.section()), row.section());
    }

    private boolean classRoomMatchesRow(ClassRoom classRoom, StructureMatrixRow row) {
        if (classRoom == null || row == null) {
            return false;
        }
        boolean courseMatch = textEquals(classRoom.getCourseCode(), row.courseCode())
                || textEquals(classRoom.getCourse(), row.courseName())
                || textEquals(classRoom.getCourse(), row.courseCode());
        boolean batchMatch = textEquals(classRoom.getBatchName(), row.batchName())
                || textEquals(classRoom.getBatch(), row.batchName());
        return courseMatch
                && batchMatch
                && academicYearEquals(classRoom.getAcademicYear(), row.academicYear())
                && Objects.equals(classRoom.getSemester(), row.semesterNumber())
                && textEquals(defaultText(classRoom.getSection(), row.section()), row.section());
    }

    private boolean feeMatchesRow(Fees fee, StructureMatrixRow row) {
        if (fee == null || row == null) {
            return false;
        }
        boolean courseMatch = textEquals(fee.getCourse(), row.courseCode()) || textEquals(fee.getCourse(), row.courseName());
        boolean batchMatch = fee.getBatch() != null
                ? Objects.equals(fee.getBatch().getId(), row.batchId())
                : textEquals(fee.getBatchName(), row.batchName());
        boolean academicYearMatch = blankToNull(fee.getAcademicYear()) == null
                || academicYearEquals(fee.getAcademicYear(), row.academicYear());
        boolean semesterMatch = blankToNull(fee.getSemester()) == null
                || Objects.equals(parseSemesterNumber(fee.getSemester()), row.semesterNumber());
        return courseMatch && batchMatch && academicYearMatch && semesterMatch;
    }

    private String academicYearForStructure(Batch batch, String yearLabel, Integer semesterNumber) {
        int startYear = resolveBatchStartYear(batch);
        int phaseIndex = phaseIndexForYearLabel(yearLabel);
        if (phaseIndex < 0 && semesterNumber != null && semesterNumber > 0) {
            phaseIndex = (semesterNumber - 1) / 2;
        }
        return formatAcademicYear(startYear + Math.max(0, phaseIndex));
    }

    private int phaseIndexForYearLabel(String yearLabel) {
        String normalized = normalizeYearLabel(yearLabel);
        if (normalized == null) {
            return -1;
        }
        return switch (normalized) {
            case "FY" -> 0;
            case "SY" -> 1;
            case "TY" -> 2;
            case "LY" -> 3;
            default -> {
                if (normalized.startsWith("Y")) {
                    try {
                        yield Math.max(0, Integer.parseInt(normalized.substring(1)) - 1);
                    } catch (NumberFormatException ignored) {
                        yield -1;
                    }
                }
                yield -1;
            }
        };
    }

    private String normalizeSection(String section) {
        String value = blankToNull(section);
        return value == null ? null : value.strip().toUpperCase(Locale.ENGLISH);
    }

    private boolean textEquals(String left, String right) {
        String normalizedLeft = blankToNull(left);
        String normalizedRight = blankToNull(right);
        if (normalizedLeft == null || normalizedRight == null) {
            return normalizedLeft == null && normalizedRight == null;
        }
        return normalizedLeft.equalsIgnoreCase(normalizedRight);
    }

    public List<String> semesterOptionsForCourse(Course course) {
        int total = resolveTotalSemesters(course);
        List<String> labels = new ArrayList<>();
        for (int semester = 1; semester <= total; semester++) {
            labels.add("Sem " + semester);
        }
        return labels;
    }

    public StudentProgression syncStudentProgression(Student student) {
        if (student == null || student.getAdmin() == null) {
            return new StudentProgression(
                    defaultText(student != null ? student.getCourse() : null, "Course not assigned"),
                    "Batch not assigned",
                    defaultText(student != null ? student.getAcademicYear() : null, "Academic year not assigned"),
                    "Year not assigned",
                    parseSemesterNumber(student != null ? student.getSemester() : null),
                    formatSemesterLabel(parseSemesterNumber(student != null ? student.getSemester() : null))
            );
        }

        boolean changed = false;
        Batch batch = student.getBatch();
        Course course = batch != null && batch.getCourse() != null
                ? batch.getCourse()
                : resolveCourseForStudent(student.getAdmin(), student);

        if (batch == null && course != null) {
            batch = resolveBatchForStudent(student.getAdmin(), student, course);
            if (batch != null) {
                student.setBatch(batch);
                changed = true;
            }
        }

        if (batch == null || course == null) {
            return new StudentProgression(
                    defaultText(course != null ? course.getName() : student.getCourse(), "Course not assigned"),
                    "Batch not assigned",
                    defaultText(student.getAcademicYear(), "Academic year not assigned"),
                    "Year not assigned",
                    parseSemesterNumber(student.getSemester()),
                    formatSemesterLabel(parseSemesterNumber(student.getSemester()))
            );
        }

        BatchPlan plan = buildBatchPlan(student.getAdmin(), course, batch);
        StructureMatrixRow currentRow = resolveCurrentRow(plan.rows(), batch, student.getAcademicYear(), student.getSemester());

        if (currentRow == null) {
            return new StudentProgression(
                    defaultText(course.getName(), defaultText(student.getCourse(), "Course")),
                    defaultText(batch.getDisplayName(), buildBatchLabel(batch, course)),
                    defaultText(student.getAcademicYear(), "Academic year not assigned"),
                    "Year not assigned",
                    parseSemesterNumber(student.getSemester()),
                    formatSemesterLabel(parseSemesterNumber(student.getSemester()))
            );
        }

        String expectedCourse = defaultText(course.getName(), student.getCourse());
        if (!Objects.equals(blankToNull(student.getCourse()), blankToNull(expectedCourse))) {
            student.setCourse(expectedCourse);
            changed = true;
        }
        if (!academicYearEquals(student.getAcademicYear(), currentRow.academicYear())) {
            student.setAcademicYear(currentRow.academicYear());
            changed = true;
        }
        String expectedSemester = currentRow.semesterLabel();
        if (!Objects.equals(normalizeSemester(student.getSemester()), normalizeSemester(expectedSemester))) {
            student.setSemester(expectedSemester);
            changed = true;
        }
        if (blankToNull(student.getSectionName()) == null && blankToNull(currentRow.section()) != null) {
            student.setSectionName(currentRow.section());
            changed = true;
        }

        ClassRoom currentClassRoom = student.getClassRoom();
        ClassRoom matchingClassRoom = resolveMatchingClassRoom(student.getAdmin(), batch, course, currentRow, student.getSectionName());
        if (matchingClassRoom != null && !Objects.equals(student.getClassRoom(), matchingClassRoom)) {
            student.setClassRoom(matchingClassRoom);
            changed = true;
        } else if (matchingClassRoom == null && currentClassRoom != null
                && !classRoomMatchesStudentScope(currentClassRoom, batch, course, currentRow, student.getSectionName())) {
            student.setClassRoom(null);
            changed = true;
        }

        if (changed) {
            studentRepository.save(student);
        }

        return new StudentProgression(
                expectedCourse,
                defaultText(batch.getDisplayName(), buildBatchLabel(batch, course)),
                currentRow.academicYear(),
                currentRow.yearLabel(),
                currentRow.semesterNumber(),
                currentRow.semesterLabel()
        );
    }

    private Course resolveCourseForStudent(Admin admin, Student student) {
        if (admin == null || student == null) {
            return null;
        }

        String rawCourse = blankToNull(student.getCourse());
        if (rawCourse == null) {
            ClassRoom classRoom = student.getClassRoom();
            if (classRoom != null) {
                rawCourse = blankToNull(classRoom.getCourseCode());
                if (rawCourse == null) {
                    rawCourse = blankToNull(classRoom.getCourse());
                }
            }
        }

        if (rawCourse == null) {
            return null;
        }

        Course byCode = courseRepository.findByAdminAndCodeIgnoreCase(admin, rawCourse);
        if (byCode != null) {
            return byCode;
        }

        return courseRepository.findByAdminAndNameIgnoreCase(admin, rawCourse);
    }

    private Batch resolveBatchForStudent(Admin admin, Student student, Course course) {
        if (admin == null || course == null) {
            return null;
        }

        List<Batch> batches = batchRepository.findByAdminAndCourseOrderByDisplayNameAsc(admin, course);
        if (batches.isEmpty()) {
            return null;
        }
        if (batches.size() == 1) {
            return batches.get(0);
        }

        String studentAcademicYear = blankToNull(student != null ? student.getAcademicYear() : null);
        if (studentAcademicYear != null) {
            for (Batch batch : batches) {
                if (batch == null || batch.getStartYear() == null) {
                    continue;
                }
                String batchAcademicYear = formatAcademicYear(batch.getStartYear());
                if (academicYearEquals(batchAcademicYear, studentAcademicYear)) {
                    return batch;
                }
            }
        }

        int currentAcademicStart = currentAcademicYearStart();
        for (Batch batch : batches) {
            if (batch == null || batch.getStartYear() == null) {
                continue;
            }
            Integer batchEnd = batch.getEndYear();
            if (batchEnd != null && currentAcademicStart >= batch.getStartYear() && currentAcademicStart < batchEnd) {
                return batch;
            }
            if (batchEnd == null && currentAcademicStart == batch.getStartYear()) {
                return batch;
            }
        }

        return batches.get(0);
    }

    public TeacherScopeSummary syncTeacherProgression(Teacher teacher) {
        if (teacher == null || teacher.getAdmin() == null) {
            return new TeacherScopeSummary("Batch not assigned", "Year not assigned", null, "Semester not assigned", List.of());
        }

        ClassRoom classRoom = teacher.getClassRoom();
        Batch batch = resolveBatchForClassRoom(teacher.getAdmin(), classRoom);
        Course course = resolveCourseForClassRoom(teacher.getAdmin(), classRoom, batch);

        String yearLabel = "Year not assigned";
        Integer semesterNumber = classRoom != null ? classRoom.getSemester() : null;
        if (batch != null && course != null) {
            StructureMatrixRow row = resolveTeacherRow(buildBatchPlan(teacher.getAdmin(), course, batch).rows(), classRoom);
            if (row != null) {
                yearLabel = row.yearLabel();
                semesterNumber = row.semesterNumber();
            }
        }

        List<String> assignedSubjects = resolveAssignedSubjectNames(teacher, batch, course, semesterNumber);
        return new TeacherScopeSummary(
                batch != null ? defaultText(batch.getDisplayName(), "Batch assigned") : "Batch not assigned",
                yearLabel,
                semesterNumber,
                formatSemesterLabel(semesterNumber),
                assignedSubjects
        );
    }

    private StructureMatrixRow resolveTeacherRow(List<StructureMatrixRow> rows, ClassRoom classRoom) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        if (classRoom != null && classRoom.getSemester() != null) {
            for (StructureMatrixRow row : rows) {
                if (Objects.equals(row.semesterNumber(), classRoom.getSemester())) {
                    return row;
                }
            }
        }
        return rows.get(0);
    }

    private List<String> resolveAssignedSubjectNames(Teacher teacher, Batch batch, Course course, Integer semesterNumber) {
        if (teacher == null || teacher.getAdmin() == null || batch == null || course == null || semesterNumber == null) {
            return List.of();
        }

        return timetableRepository.findByTeacher(teacher)
                .stream()
                .filter(entry -> entry != null && entry.getClassRoom() != null)
                .filter(entry -> classRoomMatchesBatchCourseSemester(entry.getClassRoom(), batch, course, semesterNumber))
                .map(Timetable::getSubject)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private boolean classRoomMatchesBatchCourseSemester(ClassRoom classRoom, Batch batch, Course course, Integer semesterNumber) {
        if (classRoom == null || batch == null || course == null || semesterNumber == null) {
            return false;
        }
        boolean batchMatch = defaultText(classRoom.getBatchName(), null) != null
                && defaultText(batch.getDisplayName(), null) != null
                && classRoom.getBatchName().equalsIgnoreCase(batch.getDisplayName());
        if (!batchMatch && classRoom.getBatchStartYear() != null && classRoom.getBatchEndYear() != null
                && batch.getStartYear() != null && batch.getEndYear() != null) {
            batchMatch = classRoom.getBatchStartYear().equals(batch.getStartYear())
                    && classRoom.getBatchEndYear().equals(batch.getEndYear());
        }
        if (!batchMatch) {
            return false;
        }
        boolean courseMatch = defaultText(classRoom.getCourseCode(), null) != null
                && defaultText(course.getCode(), null) != null
                && classRoom.getCourseCode().equalsIgnoreCase(course.getCode());
        if (!courseMatch && defaultText(classRoom.getCourse(), null) != null
                && defaultText(course.getName(), null) != null) {
            courseMatch = classRoom.getCourse().equalsIgnoreCase(course.getName());
        }
        return courseMatch && semesterNumber.equals(classRoom.getSemester());
    }

    private ClassRoom resolveMatchingClassRoom(Admin admin,
                                               Batch batch,
                                               Course course,
                                               StructureMatrixRow currentRow,
                                               String sectionName) {
        if (admin == null || batch == null || course == null || currentRow == null) {
            return null;
        }

        String targetSection = defaultText(sectionName, currentRow.section());
        for (ClassRoom classRoom : classRoomRepository.findByAdmin(admin)) {
            if (classRoom == null || classRoom.getSemester() == null) {
                continue;
            }
            boolean semesterMatch = currentRow.semesterNumber().equals(classRoom.getSemester());
            boolean sectionMatch = targetSection == null
                    || targetSection.equalsIgnoreCase(defaultText(classRoom.getSection(), targetSection));
            boolean courseMatch = courseMatches(course, classRoom);
            boolean batchMatch = batchMatches(batch, classRoom);
            if (semesterMatch && sectionMatch && courseMatch && batchMatch) {
                return classRoom;
            }
        }
        return null;
    }

    private boolean classRoomMatchesStudentScope(ClassRoom classRoom,
                                                 Batch batch,
                                                 Course course,
                                                 StructureMatrixRow currentRow,
                                                 String sectionName) {
        if (classRoom == null || batch == null || course == null || currentRow == null || classRoom.getSemester() == null) {
            return false;
        }

        String targetSection = defaultText(sectionName, currentRow.section());
        boolean semesterMatch = currentRow.semesterNumber().equals(classRoom.getSemester());
        boolean sectionMatch = targetSection == null
                || targetSection.equalsIgnoreCase(defaultText(classRoom.getSection(), targetSection));
        boolean courseMatch = courseMatches(course, classRoom);
        boolean batchMatch = batchMatches(batch, classRoom);
        return semesterMatch && sectionMatch && courseMatch && batchMatch;
    }

    private Batch resolveBatchForClassRoom(Admin admin, ClassRoom classRoom) {
        if (admin == null || classRoom == null) {
            return null;
        }

        Course classCourse = resolveCourseForClassRoom(admin, classRoom, null);
        List<Batch> batches = classCourse != null
                ? batchRepository.findByAdminAndCourseOrderByDisplayNameAsc(admin, classCourse)
                : batchRepository.findByAdminOrderByDisplayNameAsc(admin);

        for (Batch batch : batches) {
            if (batchMatches(batch, classRoom)) {
                return batch;
            }
        }
        return null;
    }

    private Course resolveCourseForClassRoom(Admin admin, ClassRoom classRoom, Batch batch) {
        if (batch != null && batch.getCourse() != null) {
            return batch.getCourse();
        }
        if (admin == null || classRoom == null) {
            return null;
        }

        String courseCode = blankToNull(classRoom.getCourseCode());
        if (courseCode != null) {
            Course byCode = courseRepository.findByAdminAndCodeIgnoreCase(admin, courseCode);
            if (byCode != null) {
                return byCode;
            }
        }

        String courseLabel = blankToNull(classRoom.getCourse());
        if (courseLabel != null) {
            Course byName = courseRepository.findByAdminAndNameIgnoreCase(admin, courseLabel);
            if (byName != null) {
                return byName;
            }
            Course byCode = courseRepository.findByAdminAndCodeIgnoreCase(admin, courseLabel);
            if (byCode != null) {
                return byCode;
            }
        }
        return null;
    }

    private boolean courseMatches(Course course, ClassRoom classRoom) {
        if (course == null || classRoom == null) {
            return false;
        }
        String classCourse = defaultText(classRoom.getCourse(), "");
        String classCourseCode = defaultText(classRoom.getCourseCode(), "");
        return course.getName().equalsIgnoreCase(classCourse)
                || course.getCode().equalsIgnoreCase(classCourse)
                || course.getCode().equalsIgnoreCase(classCourseCode);
    }

    private boolean batchMatches(Batch batch, ClassRoom classRoom) {
        if (batch == null || classRoom == null) {
            return false;
        }

        boolean yearMatch = batch.getStartYear() != null && batch.getEndYear() != null
                && Objects.equals(batch.getStartYear(), classRoom.getBatchStartYear())
                && Objects.equals(batch.getEndYear(), classRoom.getBatchEndYear());
        if (yearMatch) {
            return true;
        }

        String batchName = defaultText(batch.getDisplayName(), buildBatchLabel(batch, batch.getCourse()));
        return batchName.equalsIgnoreCase(defaultText(classRoom.getBatchName(), ""))
                || batchName.equalsIgnoreCase(defaultText(classRoom.getBatch(), ""));
    }

    private StructureMatrixRow resolveCurrentRow(List<StructureMatrixRow> rows,
                                                 Batch batch,
                                                 String storedAcademicYear,
                                                 String storedSemester) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }

        Integer semesterNumber = parseSemesterNumber(storedSemester);
        String normalizedStoredAcademicYear = blankToNull(storedAcademicYear);

        if (normalizedStoredAcademicYear != null && semesterNumber != null) {
            for (StructureMatrixRow row : rows) {
                if (academicYearEquals(row.academicYear(), normalizedStoredAcademicYear)
                        && Objects.equals(row.semesterNumber(), semesterNumber)) {
                    return row;
                }
            }
        }

        if (semesterNumber != null) {
            for (StructureMatrixRow row : rows) {
                if (semesterNumber.equals(row.semesterNumber())) {
                    return row;
                }
            }
        }

        if (normalizedStoredAcademicYear != null) {
            for (StructureMatrixRow row : rows) {
                if (academicYearEquals(row.academicYear(), normalizedStoredAcademicYear)) {
                    return row;
                }
            }
        }

        int currentStartYear = currentAcademicYearStart();

        if (batch != null && batch.getStartYear() != null) {
            if (currentStartYear < batch.getStartYear()) {
                return rows.get(0);
            }
            Integer batchEnd = batch.getEndYear();
            if (batchEnd != null && currentStartYear >= batchEnd) {
                return rows.get(rows.size() - 1);
            }
        }

        String currentAcademicYear = currentAcademicYear();
        List<StructureMatrixRow> currentYearRows = rows.stream()
                .filter(row -> academicYearEquals(row.academicYear(), currentAcademicYear))
                .sorted(Comparator.comparing(StructureMatrixRow::semesterNumber))
                .toList();
        if (!currentYearRows.isEmpty()) {
            boolean evenSemesterWindow = isEvenSemesterWindow();
            for (StructureMatrixRow row : currentYearRows) {
                boolean evenSemester = row.semesterNumber() != null && row.semesterNumber() % 2 == 0;
                if (evenSemesterWindow == evenSemester) {
                    return row;
                }
            }
            return currentYearRows.get(0);
        }

        return rows.get(0);
    }

    private Map<String, Set<Integer>> buildYearSemesterMap(Course course) {
        Map<String, Set<Integer>> map = new LinkedHashMap<>();
        int totalSemesters = resolveTotalSemesters(course);
        int phaseCount = Math.max(1, (int) Math.ceil(totalSemesters / 2.0));
        for (int phaseIndex = 0; phaseIndex < phaseCount; phaseIndex++) {
            Set<Integer> semesters = new LinkedHashSet<>();
            int startSemester = phaseIndex * 2 + 1;
            semesters.add(startSemester);
            if (startSemester + 1 <= totalSemesters) {
                semesters.add(startSemester + 1);
            }
            map.put(yearLabelForIndex(phaseIndex), semesters);
        }
        return map;
    }

    private String formatSemesterList(Set<Integer> semesters) {
        if (semesters == null || semesters.isEmpty()) {
            return "valid semesters";
        }
        return semesters.stream()
                .sorted()
                .map(value -> "Sem " + value)
                .collect(Collectors.joining(" & "));
    }

    private String toStructureKey(AcademicStructure structure) {
        return toStructureKey(structure.getYearLabel(), structure.getSemesterNumber(), structure.getSection());
    }

    private String toStructureKey(String yearLabel, Integer semesterNumber, String section) {
        String normalizedSection = normalizeSection(section);
        return normalizeYearLabel(yearLabel) + "|" + semesterNumber + "|" + defaultText(normalizedSection, "");
    }

    private String yearLabelForIndex(int phaseIndex) {
        return switch (phaseIndex) {
            case 0 -> "FY";
            case 1 -> "SY";
            case 2 -> "TY";
            case 3 -> "LY";
            default -> "Y" + (phaseIndex + 1);
        };
    }

    private int resolveTotalSemesters(Course course) {
        if (course == null) {
            return 0;
        }
        if (course.getTotalSemesters() != null && course.getTotalSemesters() > 0) {
            return course.getTotalSemesters();
        }
        if (course.getDurationYears() != null && course.getDurationYears() > 0) {
            return course.getDurationYears() * 2;
        }
        return 2;
    }

    private int resolveBatchStartYear(Batch batch) {
        if (batch != null && batch.getStartYear() != null) {
            return batch.getStartYear();
        }
        return currentAcademicYearStart();
    }

    public String currentAcademicYear() {
        return formatAcademicYear(currentAcademicYearStart());
    }

    private int currentAcademicYearStart() {
        LocalDate now = LocalDate.now();
        return now.getMonthValue() >= ACADEMIC_YEAR_START_MONTH ? now.getYear() : now.getYear() - 1;
    }

    private boolean isEvenSemesterWindow() {
        int month = LocalDate.now().getMonthValue();
        return month >= EVEN_SEMESTER_SWITCH_MONTH || month < ACADEMIC_YEAR_START_MONTH;
    }

    private String formatAcademicYear(int startYear) {
        return startYear + "-" + String.valueOf(startYear + 1).substring(2);
    }

    private Integer parseSemesterNumber(String value) {
        String normalized = defaultText(value, "");
        if (normalized.isBlank()) {
            return null;
        }
        String digits = normalized.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String normalizeSemester(String value) {
        Integer semesterNumber = parseSemesterNumber(value);
        return semesterNumber == null ? defaultText(value, "").trim().toUpperCase(Locale.ENGLISH) : "SEM " + semesterNumber;
    }

    private String normalizeYearLabel(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : normalized.strip().toUpperCase(Locale.ENGLISH);
    }

    private boolean academicYearEquals(String left, String right) {
        return defaultText(left, "").replace('–', '-').equalsIgnoreCase(defaultText(right, "").replace('–', '-'));
    }

    private String buildBatchLabel(Batch batch, Course course) {
        if (batch == null) {
            return "Batch";
        }
        if (batch.getStartYear() != null && batch.getEndYear() != null) {
            return defaultText(course != null ? course.getName() : null, "Batch")
                    + " " + batch.getStartYear() + "-" + batch.getEndYear();
        }
        return defaultText(batch.getDisplayName(), "Batch");
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String defaultText(String value, String fallback) {
        String normalized = blankToNull(value);
        return normalized == null ? fallback : normalized;
    }

    private String formatSemesterLabel(Integer semesterNumber) {
        return semesterNumber == null ? "Semester not assigned" : "Sem " + semesterNumber;
    }
}
