package com.scms.scms.service.chatbot;

import com.scms.scms.chatbot.ChatbotConversationResponse;
import com.scms.scms.chatbot.ChatbotMessageView;
import com.scms.scms.model.*;
import com.scms.scms.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    private static final Locale INDIA = new Locale("en", "IN");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final TimetableRepository timetableRepository;
    private final FeesRepository feesRepository;
    private final ClassRoomRepository classRoomRepository;
    private final SubjectRepository subjectRepository;
    private final StudyMaterialRepository studyMaterialRepository;
    private final AssignmentRepository assignmentRepository;
    private final TeacherAttendanceSessionRepository attendanceSessionRepository;

    public ChatbotService(
            StudentRepository studentRepository,
            TeacherRepository teacherRepository,
            TimetableRepository timetableRepository,
            FeesRepository feesRepository,
            ClassRoomRepository classRoomRepository,
            SubjectRepository subjectRepository,
            StudyMaterialRepository studyMaterialRepository,
            AssignmentRepository assignmentRepository,
            TeacherAttendanceSessionRepository attendanceSessionRepository
    ) {
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.timetableRepository = timetableRepository;
        this.feesRepository = feesRepository;
        this.classRoomRepository = classRoomRepository;
        this.subjectRepository = subjectRepository;
        this.studyMaterialRepository = studyMaterialRepository;
        this.assignmentRepository = assignmentRepository;
        this.attendanceSessionRepository = attendanceSessionRepository;
    }

    public ChatbotConversationResponse buildContext(String role, String email, List<ChatbotMessageView> history) {
        UserContext context = resolveContext(role, email);
        String greeting = context.loggedIn()
                ? welcomeText(context)
                : "Please log in again to continue.";
        List<String> suggestions = suggestionsForRole(role);
        return new ChatbotConversationResponse(
                context.role(),
                context.title(),
                context.subtitle(),
                greeting,
                suggestions,
                history
        );
    }

    public ChatbotConversationResponse handleMessage(String role, String email, String message, List<ChatbotMessageView> history) {
        UserContext context = resolveContext(role, email);
        if (!context.loggedIn()) {
            List<ChatbotMessageView> messages = new ArrayList<>(history);
            messages.add(new ChatbotMessageView("assistant", "Your session expired. Please log in again.", nowLabel()));
            return new ChatbotConversationResponse(
                    context.role(),
                    context.title(),
                    context.subtitle(),
                    "Your session expired. Please log in again.",
                    suggestionsForRole(role),
                    trimHistory(messages)
            );
        }

        String reply = generateReply(context, normalize(message));
        List<ChatbotMessageView> messages = new ArrayList<>(history);
        messages.add(new ChatbotMessageView("user", message == null ? "" : message.trim(), nowLabel()));
        messages.add(new ChatbotMessageView("assistant", reply, nowLabel()));
        return new ChatbotConversationResponse(
                context.role(),
                context.title(),
                context.subtitle(),
                reply,
                suggestionsForRole(role),
                trimHistory(messages)
        );
    }

    private String generateReply(UserContext context, String normalizedMessage) {
        if (normalizedMessage.isBlank() || containsAny(normalizedMessage, "help", "what can you do", "options")) {
            return helpReply(context);
        }

        if (containsAny(normalizedMessage, "hello", "hi", "hey", "good morning", "good afternoon", "good evening")) {
            return welcomeText(context);
        }

        if (containsAny(normalizedMessage, "college", "institution", "campus", "school")) {
            return context.collegeLine();
        }

        if (containsAny(normalizedMessage, "profile", "who am i", "my details", "my info")) {
            return context.profileLine();
        }

        if (context.role().equals("STUDENT")) {
            return generateStudentReply(context.student(), normalizedMessage);
        }

        if (context.role().equals("TEACHER")) {
            return generateTeacherReply(context.teacher(), normalizedMessage);
        }

        return "I can help with profile, timetable, and common academic questions.";
    }

    private String generateStudentReply(Student student, String normalizedMessage) {
        if (student == null) {
            return "I could not find your student record.";
        }

        if (containsAny(normalizedMessage, "fee", "fees", "payment", "pending")) {
            return buildStudentFeesReply(student);
        }

        if (containsAny(normalizedMessage, "timetable", "schedule", "class", "today", "next class", "next lecture")) {
            return buildStudentTimetableReply(student, normalizedMessage);
        }

        if (containsAny(normalizedMessage, "subject", "subjects", "syllabus")) {
            return buildStudentSubjectsReply(student);
        }

        if (containsAny(normalizedMessage, "attendance")) {
            return "Student attendance tracking is not stored in this module yet. You can still ask for timetable, fees, subjects, or profile.";
        }

        if (containsAny(normalizedMessage, "assignment", "assignments", "homework")) {
            return buildStudentAssignmentsReply(student);
        }

        if (containsAny(normalizedMessage, "roll", "id", "registration", "prn", "enrollment")) {
            return buildStudentProfileReply(student);
        }

        return helpReplyForStudent();
    }

    private String generateTeacherReply(Teacher teacher, String normalizedMessage) {
        if (teacher == null) {
            return "I could not find your teacher record.";
        }

        if (containsAny(normalizedMessage, "students", "class strength", "roster", "class list")) {
            return buildTeacherStudentsReply(teacher);
        }

        if (containsAny(normalizedMessage, "timetable", "schedule", "class", "today", "next lecture")) {
            return buildTeacherTimetableReply(teacher, normalizedMessage);
        }

        if (containsAny(normalizedMessage, "assignment", "assignments", "homework")) {
            return buildTeacherAssignmentsReply(teacher);
        }

        if (containsAny(normalizedMessage, "study material", "study materials", "material", "materials")) {
            return buildTeacherMaterialsReply(teacher);
        }

        if (containsAny(normalizedMessage, "attendance", "mark attendance", "session")) {
            return buildTeacherAttendanceReply(teacher);
        }

        if (containsAny(normalizedMessage, "subject", "course", "department", "room")) {
            return buildTeacherProfileReply(teacher);
        }

        return helpReplyForTeacher();
    }

    private String buildStudentProfileReply(Student student) {
        ClassRoom classRoom = resolveStudentClassRoom(student);
        StringBuilder sb = new StringBuilder();
        sb.append("Student profile:\n");
        sb.append("Name: ").append(text(student.getName(), "Student")).append('\n');
        sb.append("Course: ").append(text(student.getCourse(), "Not assigned")).append('\n');
        sb.append("Semester: ").append(text(student.getSemester(), "Not assigned")).append('\n');
        sb.append("Section: ").append(text(student.getSectionName(), classRoom != null ? text(classRoom.getSection(), "Not assigned") : "Not assigned")).append('\n');
        sb.append("Roll No: ").append(text(student.getRollNo(), "Not assigned")).append('\n');
        sb.append("Email: ").append(text(student.getEmail(), "Not available")).append('\n');
        sb.append("College: ").append(collegeName(student)).append('\n');
        sb.append("Academic year: ").append(text(student.getAcademicYear(), "Not assigned"));
        return sb.toString();
    }

    private String buildStudentFeesReply(Student student) {
        ClassRoom classRoom = resolveStudentClassRoom(student);
        Fees fees = resolveStudentFees(student, classRoom);
        double total = safeAmount(student.getTotalFees());
        double paid = safeAmount(student.getPaidFees());
        double pending = student.getPendingFees() != null ? student.getPendingFees() : Math.max(total - paid, 0);

        if (fees != null) {
            total = fees.getTotalAmount();
        }

        return "Fee status:\n"
                + "Total fees: Rs " + formatNumber(total) + "\n"
                + "Paid: Rs " + formatNumber(paid) + "\n"
                + "Pending: Rs " + formatNumber(pending) + "\n"
                + "College: " + collegeName(student);
    }

    private String buildStudentSubjectsReply(Student student) {
        List<Subject> subjects = loadStudentSubjects(student);
        if (subjects.isEmpty()) {
            return "No live subject mapping was found for your class yet.";
        }

        StringBuilder sb = new StringBuilder("Your current subjects:\n");
        int limit = Math.min(subjects.size(), 6);
        for (int i = 0; i < limit; i++) {
            Subject subject = subjects.get(i);
            sb.append(i + 1).append(". ").append(subject.getCode()).append(" - ").append(subject.getName());
            if (subject.getSemester() != null) {
                sb.append(" (Sem ").append(subject.getSemester()).append(")");
            }
            sb.append('\n');
        }
        if (subjects.size() > limit) {
            sb.append("... and ").append(subjects.size() - limit).append(" more.");
        }
        return sb.toString().trim();
    }

    private String buildStudentAssignmentsReply(Student student) {
        ClassRoom classRoom = resolveStudentClassRoom(student);
        if (classRoom == null) {
            return "I could not resolve your class, so assignments cannot be loaded yet.";
        }
        List<Assignment> assignments = assignmentRepository.findByClassRoomOrderByDueDateAscCreatedAtDesc(classRoom);
        if (assignments.isEmpty()) {
            return "No assignments are currently published for your class.";
        }
        StringBuilder sb = new StringBuilder("Recent assignments:\n");
        int limit = Math.min(assignments.size(), 5);
        for (int i = 0; i < limit; i++) {
            Assignment assignment = assignments.get(i);
            sb.append(i + 1).append(". ").append(text(assignment.getTitle(), "Assignment"))
                    .append(" - ").append(text(assignment.getSubject(), "Subject"))
                    .append(" (Due ").append(assignment.getDueDateLabel()).append(")\n");
        }
        return sb.toString().trim();
    }

    private String buildStudentTimetableReply(Student student, String normalizedMessage) {
        ClassRoom classRoom = resolveStudentClassRoom(student);
        if (classRoom == null) {
            return "I could not resolve your class timetable yet.";
        }

        List<Timetable> timetable = timetableRepository.findByAdminAndClassRoom(student.getAdmin(), classRoom)
                .stream()
                .sorted(Comparator.<Timetable>comparingInt(t -> dayRank(t.getDay()))
                        .thenComparing(t -> parseTime(t.getStartTime()).orElse(LocalTime.MIN)))
                .toList();

        if (timetable.isEmpty()) {
            return "No timetable entries were found for your class.";
        }

        if (containsAny(normalizedMessage, "today", "now", "current")) {
            String todayName = LocalDate.now().getDayOfWeek().getDisplayName(TextStyle.FULL, INDIA);
            List<Timetable> today = timetable.stream()
                    .filter(entry -> todayName.equalsIgnoreCase(entry.getDay()))
                    .toList();
            if (!today.isEmpty()) {
                return "Today's timetable for " + classRoom.getDisplayLabel() + ":\n" + buildTimetableLines(today, 4);
            }
            return "There are no timetable entries today for " + classRoom.getDisplayLabel() + ".";
        }

        if (containsAny(normalizedMessage, "next", "upcoming")) {
            List<Timetable> today = timetable.stream()
                    .filter(entry -> LocalDate.now().getDayOfWeek().getDisplayName(TextStyle.FULL, INDIA)
                            .equalsIgnoreCase(entry.getDay()))
                    .toList();
            String nextSlot = firstUpcomingSlot(today);
            if (nextSlot != null) {
                return "Your next class is:\n" + nextSlot;
            }
        }

        return "Timetable for " + classRoom.getDisplayLabel() + ":\n" + buildTimetableLines(timetable, 6);
    }

    private String buildTeacherProfileReply(Teacher teacher) {
        ClassRoom classRoom = resolveTeacherClassRoom(teacher);
        return "Teacher profile:\n"
                + "Name: " + text(teacher.getName(), "Teacher") + "\n"
                + "Designation: " + text(teacher.getDesignation(), "Faculty") + "\n"
                + "Subject: " + text(teacher.getSubject(), "Not assigned") + "\n"
                + "Employee ID: " + text(teacher.getEmployeeId(), "Not assigned") + "\n"
                + "Class: " + (classRoom != null ? classRoom.getDisplayLabel() : "Not assigned") + "\n"
                + "College: " + collegeName(teacher) + "\n"
                + "Academic year: " + (classRoom != null && classRoom.getAcademicYear() != null ? classRoom.getAcademicYear() : "Current academic year");
    }

    private String buildTeacherStudentsReply(Teacher teacher) {
        ClassRoom classRoom = resolveTeacherClassRoom(teacher);
        if (classRoom == null) {
            return "I could not resolve your class room yet.";
        }
        List<Student> students = studentRepository.findByClassRoom(classRoom);
        if (students.isEmpty()) {
            return "No students are linked to " + classRoom.getDisplayLabel() + ".";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Class strength: ").append(students.size()).append('\n');
        sb.append("First students:\n");
        int limit = Math.min(students.size(), 5);
        for (int i = 0; i < limit; i++) {
            Student student = students.get(i);
            sb.append(i + 1).append(". ").append(text(student.getName(), "Student"))
                    .append(" | Roll ").append(text(student.getRollNo(), "N/A"))
                    .append(" | ").append(text(student.getSectionName(), classRoom.getSection()))
                    .append('\n');
        }
        return sb.toString().trim();
    }

    private String buildTeacherTimetableReply(Teacher teacher, String normalizedMessage) {
        ClassRoom classRoom = resolveTeacherClassRoom(teacher);
        List<Timetable> timetable = classRoom != null
                ? timetableRepository.findByClassRoom(classRoom)
                : timetableRepository.findByTeacher(teacher);
        timetable = timetable.stream()
                .sorted(Comparator.<Timetable>comparingInt(t -> dayRank(t.getDay()))
                        .thenComparing(t -> parseTime(t.getStartTime()).orElse(LocalTime.MIN)))
                .toList();

        if (timetable.isEmpty()) {
            return "No timetable entries were found for your teacher profile.";
        }

        if (containsAny(normalizedMessage, "today", "now", "current")) {
            String todayName = LocalDate.now().getDayOfWeek().getDisplayName(TextStyle.FULL, INDIA);
            List<Timetable> today = timetable.stream()
                    .filter(entry -> todayName.equalsIgnoreCase(entry.getDay()))
                    .toList();
            if (!today.isEmpty()) {
                return "Today's timetable:\n" + buildTimetableLines(today, 4);
            }
        }

        return "Your timetable:\n" + buildTimetableLines(timetable, 6);
    }

    private String buildTeacherAssignmentsReply(Teacher teacher) {
        List<Assignment> assignments = assignmentRepository.findByTeacherOrderByCreatedAtDesc(teacher);
        if (assignments.isEmpty()) {
            return "No assignments have been posted yet.";
        }
        StringBuilder sb = new StringBuilder("Recent assignments:\n");
        int limit = Math.min(assignments.size(), 5);
        for (int i = 0; i < limit; i++) {
            Assignment assignment = assignments.get(i);
            sb.append(i + 1).append(". ").append(text(assignment.getTitle(), "Assignment"))
                    .append(" | ").append(text(assignment.getSubject(), "Subject"))
                    .append(" | Due ").append(assignment.getDueDateLabel())
                    .append('\n');
        }
        return sb.toString().trim();
    }

    private String buildTeacherMaterialsReply(Teacher teacher) {
        List<StudyMaterial> materials = studyMaterialRepository.findByTeacherOrderByUploadedAtDesc(teacher);
        if (materials.isEmpty()) {
            return "No study materials have been uploaded yet.";
        }
        StringBuilder sb = new StringBuilder("Recent study materials:\n");
        int limit = Math.min(materials.size(), 5);
        for (int i = 0; i < limit; i++) {
            StudyMaterial material = materials.get(i);
            sb.append(i + 1).append(". ").append(text(material.getTitle(), "Material"))
                    .append(" | ").append(text(material.getSubject(), "Subject"))
                    .append(" | ").append(text(material.getAcademicYear(), "Academic year"))
                    .append('\n');
        }
        return sb.toString().trim();
    }

    private String buildTeacherAttendanceReply(Teacher teacher) {
        List<TeacherAttendanceSession> sessions = attendanceSessionRepository.findByTeacherOrderByAttendanceDateDescCreatedAtDesc(teacher);
        if (sessions.isEmpty()) {
            return "No attendance sessions have been recorded yet for your classes.";
        }
        TeacherAttendanceSession latest = sessions.get(0);
        String classLabel = latest.getClassRoom() != null ? latest.getClassRoom().getDisplayLabel() : "Unassigned class";
        return "Latest attendance session:\n"
                + "Class: " + classLabel + "\n"
                + "Subject: " + text(latest.getSubject(), "Subject") + "\n"
                + "Date: " + (latest.getAttendanceDate() != null ? latest.getAttendanceDate() : "N/A") + "\n"
                + "Lecture: " + text(latest.getLectureNo(), "N/A") + "\n"
                + "Sessions recorded: " + sessions.size();
    }

    private String buildTimetableLines(List<Timetable> timetable, int limit) {
        StringBuilder sb = new StringBuilder();
        int count = Math.min(timetable.size(), limit);
        for (int i = 0; i < count; i++) {
            Timetable entry = timetable.get(i);
            sb.append(i + 1).append(". ")
                    .append(text(entry.getDay(), "Day"))
                    .append(" | ")
                    .append(text(entry.getStartTime(), "--"))
                    .append(" - ")
                    .append(text(entry.getEndTime(), "--"))
                    .append(" | ")
                    .append(text(entry.getSubject(), "Subject"))
                    .append(" | ")
                    .append(text(entry.getRoom(), "Room"))
                    .append('\n');
        }
        return sb.toString().trim();
    }

    private String firstUpcomingSlot(List<Timetable> entries) {
        if (entries == null || entries.isEmpty()) {
            return null;
        }
        String nowText = LocalTime.now().format(TIME_FORMAT);
        Optional<LocalTime> now = parseTime(nowText);
        for (Timetable entry : entries) {
            Optional<LocalTime> start = parseTime(entry.getStartTime());
            if (start.isPresent() && now.isPresent() && start.get().isAfter(now.get())) {
                return formatEntry(entry);
            }
        }
        return formatEntry(entries.get(0));
    }

    private String formatEntry(Timetable entry) {
        return text(entry.getDay(), "Day")
                + " | " + text(entry.getStartTime(), "--")
                + " - " + text(entry.getEndTime(), "--")
                + " | " + text(entry.getSubject(), "Subject")
                + " | " + text(entry.getRoom(), "Room");
    }

    private UserContext resolveContext(String role, String email) {
        if (role != null && role.equalsIgnoreCase("STUDENT")) {
            Student student = email == null ? null : studentRepository.findByEmail(email);
            if (student == null) {
                return UserContext.missing("STUDENT");
            }
            return UserContext.forStudent(student);
        }
        if (role != null && role.equalsIgnoreCase("TEACHER")) {
            Teacher teacher = email == null ? null : teacherRepository.findByEmail(email);
            if (teacher == null) {
                return UserContext.missing("TEACHER");
            }
            return UserContext.forTeacher(teacher);
        }
        return UserContext.missing(role == null ? "UNKNOWN" : role.toUpperCase(Locale.ENGLISH));
    }

    private String welcomeText(UserContext context) {
        if (!context.loggedIn()) {
            return "Please log in again to continue.";
        }
        if (context.role().equals("STUDENT")) {
            return "Hi " + text(context.student().getName(), "Student")
                    + ". I can help with timetable, fees, subjects, assignments, and profile details.";
        }
        if (context.role().equals("TEACHER")) {
            return "Hi " + text(context.teacher().getName(), "Teacher")
                    + ". I can help with students, timetable, assignments, materials, and attendance.";
        }
        return "I can help with student and teacher academic questions.";
    }

    private String helpReply(UserContext context) {
        if (context.role().equals("STUDENT")) {
            return "You can ask me about:\n"
                    + "• timetable\n"
                    + "• fees\n"
                    + "• subjects\n"
                    + "• assignments\n"
                    + "• profile";
        }
        if (context.role().equals("TEACHER")) {
            return "You can ask me about:\n"
                    + "• students / class strength\n"
                    + "• timetable\n"
                    + "• assignments\n"
                    + "• study materials\n"
                    + "• attendance";
        }
        return "Try asking about timetable, profile, or other academic details.";
    }

    private String helpReplyForStudent() {
        return "I can help with your timetable, fees, subjects, assignments, and profile. Try one of the quick actions.";
    }

    private String helpReplyForTeacher() {
        return "I can help with students, timetable, assignments, study materials, attendance, and profile. Try one of the quick actions.";
    }

    private List<String> suggestionsForRole(String role) {
        if ("TEACHER".equalsIgnoreCase(role)) {
            return List.of("Show my students", "Today's timetable", "Recent assignments", "Study materials");
        }
        return List.of("My timetable", "Pending fees", "My subjects", "My profile");
    }

    private ClassRoom resolveStudentClassRoom(Student student) {
        if (student == null) {
            return null;
        }
        if (student.getClassRoom() != null) {
            return student.getClassRoom();
        }
        if (student.getAdmin() == null) {
            return null;
        }

        List<ClassRoom> candidates = classRoomRepository.findByAdmin(student.getAdmin());
        String course = text(student.getCourse(), "").trim();
        String semester = text(student.getSemester(), "").trim();
        String year = text(student.getAcademicYear(), "").trim();
        String section = text(student.getSectionName(), "").trim();
        String batch = student.getBatch() != null ? text(student.getBatch().getDisplayName(), "").trim() : "";

        for (ClassRoom classRoom : candidates) {
            if (!course.isBlank() && classRoom.getCourse() != null && !classRoom.getCourse().equalsIgnoreCase(course)) {
                continue;
            }
            if (!year.isBlank() && classRoom.getAcademicYear() != null && !classRoom.getAcademicYear().equalsIgnoreCase(year)) {
                continue;
            }
            if (!section.isBlank() && classRoom.getSection() != null && !classRoom.getSection().equalsIgnoreCase(section)) {
                continue;
            }
            if (!semester.isBlank()) {
                Integer semesterNumber = parseSemesterNumber(semester);
                if (semesterNumber != null && classRoom.getSemester() != null && !semesterNumber.equals(classRoom.getSemester())) {
                    continue;
                }
            }
            if (!batch.isBlank()) {
                String classBatch = text(classRoom.getBatchName(), "");
                if (!classBatch.isBlank() && !classBatch.equalsIgnoreCase(batch)) {
                    continue;
                }
            }
            return classRoom;
        }
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    private ClassRoom resolveTeacherClassRoom(Teacher teacher) {
        if (teacher == null) {
            return null;
        }
        if (teacher.getClassRoom() != null) {
            return teacher.getClassRoom();
        }
        List<Timetable> timetable = timetableRepository.findByTeacher(teacher);
        return timetable.stream()
                .map(Timetable::getClassRoom)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private List<Subject> loadStudentSubjects(Student student) {
        if (student == null || student.getAdmin() == null) {
            return List.of();
        }
        List<Subject> subjects = subjectRepository.findByAdminOrderByCourseRef_CodeAscBatchRef_DisplayNameAscSemesterAscNameAsc(student.getAdmin());
        Batch batch = student.getBatch();
        Integer semester = parseSemesterNumber(student.getSemester());

        if (batch == null && semester == null) {
            return subjects;
        }

        return subjects.stream()
                .filter(subject -> batch == null || subject.getBatchRef() == null || Objects.equals(subject.getBatchRef().getId(), batch.getId()))
                .filter(subject -> semester == null || subject.getSemester() == null || semester.equals(subject.getSemester()))
                .collect(Collectors.toList());
    }

    private Fees resolveStudentFees(Student student, ClassRoom classRoom) {
        if (student == null || student.getAdmin() == null) {
            return null;
        }
        String course = text(student.getCourse(), "").trim();
        String academicYear = classRoom != null ? text(classRoom.getAcademicYear(), "").trim() : text(student.getAcademicYear(), "").trim();
        String semester = classRoom != null && classRoom.getSemester() != null
                ? "Sem " + classRoom.getSemester()
                : text(student.getSemester(), "").trim();
        if (!course.isBlank() && !academicYear.isBlank() && !semester.isBlank()) {
            Fees fee = feesRepository.findByAdminAndCourseIgnoreCaseAndAcademicYearIgnoreCaseAndSemesterIgnoreCase(student.getAdmin(), course, academicYear, semester);
            if (fee != null) {
                return fee;
            }
        }
        if (!course.isBlank() && !academicYear.isBlank()) {
            Fees fee = feesRepository.findByAdminAndCourseIgnoreCaseAndAcademicYearIgnoreCaseAndSemesterIsNull(student.getAdmin(), course, academicYear);
            if (fee != null) {
                return fee;
            }
        }
        return !course.isBlank()
                ? feesRepository.findByAdminAndCourseIgnoreCaseAndAcademicYearIsNullAndSemesterIsNull(student.getAdmin(), course)
                : null;
    }

    private List<ChatbotMessageView> trimHistory(List<ChatbotMessageView> messages) {
        if (messages.size() <= 20) {
            return messages;
        }
        return new ArrayList<>(messages.subList(messages.size() - 20, messages.size()));
    }

    private String collegeName(Student student) {
        return student != null && student.getAdmin() != null && student.getAdmin().getCollegeName() != null
                ? student.getAdmin().getCollegeName()
                : "AI Campus";
    }

    private String collegeName(Teacher teacher) {
        return teacher != null && teacher.getAdmin() != null && teacher.getAdmin().getCollegeName() != null
                ? teacher.getAdmin().getCollegeName()
                : "AI Campus";
    }

    private boolean containsAny(String message, String... terms) {
        for (String term : terms) {
            if (message.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String message) {
        if (message == null) {
            return "";
        }
        return message.toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9\\s]", " ").replaceAll("\\s+", " ").trim();
    }

    private String text(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private double safeAmount(Double value) {
        return value == null ? 0 : value;
    }

    private String formatNumber(double value) {
        long rounded = Math.round(value);
        return String.format(INDIA, "%d", rounded);
    }

    private Optional<LocalTime> parseTime(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String clean = value.trim().toUpperCase(Locale.ENGLISH);
        try {
            return Optional.of(LocalTime.parse(clean, DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)));
        } catch (Exception ignored) {
            try {
                return Optional.of(LocalTime.parse(clean));
            } catch (Exception ignoredAgain) {
                return Optional.empty();
            }
        }
    }

    private int dayRank(String day) {
        if (day == null) {
            return 99;
        }
        return switch (day.trim().toLowerCase(Locale.ENGLISH)) {
            case "monday" -> 1;
            case "tuesday" -> 2;
            case "wednesday" -> 3;
            case "thursday" -> 4;
            case "friday" -> 5;
            case "saturday" -> 6;
            case "sunday" -> 7;
            default -> 99;
        };
    }

    private Integer parseSemesterNumber(String semester) {
        if (semester == null) {
            return null;
        }
        String digits = semester.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String nowLabel() {
        return LocalDate.now().toString();
    }

    private record UserContext(
            String role,
            Student student,
            Teacher teacher,
            boolean loggedIn,
            String title,
            String subtitle,
            String profileLine,
            String collegeLine
    ) {
        static UserContext forStudent(Student student) {
            String title = text(student.getName(), "Student");
            return new UserContext(
                    "STUDENT",
                    student,
                    null,
                    true,
                    title,
                    text(student.getCourse(), "Student portal"),
                    "Name: " + text(student.getName(), "Student")
                            + "\nCourse: " + text(student.getCourse(), "Not assigned")
                            + "\nRoll No: " + text(student.getRollNo(), "Not assigned")
                            + "\nEmail: " + text(student.getEmail(), "Not available"),
                    "College: " + (student.getAdmin() != null && student.getAdmin().getCollegeName() != null ? student.getAdmin().getCollegeName() : "AI Campus")
            );
        }

        static UserContext forTeacher(Teacher teacher) {
            String title = text(teacher.getName(), "Teacher");
            return new UserContext(
                    "TEACHER",
                    null,
                    teacher,
                    true,
                    title,
                    text(teacher.getSubject(), "Teacher portal"),
                    "Name: " + text(teacher.getName(), "Teacher")
                            + "\nDesignation: " + text(teacher.getDesignation(), "Faculty")
                            + "\nEmployee ID: " + text(teacher.getEmployeeId(), "Not assigned")
                            + "\nEmail: " + text(teacher.getEmail(), "Not available"),
                    "College: " + (teacher.getAdmin() != null && teacher.getAdmin().getCollegeName() != null ? teacher.getAdmin().getCollegeName() : "AI Campus")
            );
        }

        static UserContext missing(String role) {
            return new UserContext(
                    role,
                    null,
                    null,
                    false,
                    role,
                    "Sign in",
                    "",
                    ""
            );
        }

        private static String text(String value, String fallback) {
            return value == null || value.isBlank() ? fallback : value;
        }
    }
}
