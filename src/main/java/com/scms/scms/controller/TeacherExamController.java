package com.scms.scms.controller;

import com.scms.scms.model.ExamAttendance;
import com.scms.scms.model.ExamQuizAttempt;
import com.scms.scms.model.ExamQuizQuestion;
import com.scms.scms.model.ExamQuizResponse;
import com.scms.scms.model.ExamSession;
import com.scms.scms.model.ClassRoom;
import com.scms.scms.model.Student;
import com.scms.scms.model.Teacher;
import com.scms.scms.repository.StudentRepository;
import com.scms.scms.repository.TeacherRepository;
import com.scms.scms.repository.TimetableRepository;
import com.scms.scms.service.ExamAutomationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class TeacherExamController {

    @Autowired private ExamAutomationService examAutomationService;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private TimetableRepository timetableRepository;

    @GetMapping("/teacher-exams")
    public String teacherExams(Model model, HttpSession session) {
        Teacher teacher = getLoggedTeacher(session);
        if (teacher == null) return "redirect:/login";

        List<ExamSession> exams = examAutomationService.findTeacherStandardExams(teacher);
        List<ExamSession> quizzes = examAutomationService.findTeacherQuizExams(teacher);
        ClassRoom classRoom = resolveTeacherClassRoom(teacher);
        model.addAttribute("teacher", teacher);
        model.addAttribute("teacherName", teacher.getName());
        model.addAttribute("teacherSubject", teacher.getSubject());
        model.addAttribute("teacherClassLabel", classRoom != null ? classRoom.getDisplayLabel() : "No class assigned");
        model.addAttribute("teacherExams", exams);
        model.addAttribute("teacherQuizExams", quizzes);
        model.addAttribute("teacherUpcomingExams", exams.stream()
                .filter(exam -> exam.getExamDateTime() != null && exam.getExamDateTime().isAfter(LocalDateTime.now()))
                .toList());
        model.addAttribute("teacherUpcomingQuizzes", quizzes.stream()
                .filter(exam -> exam.getExamDateTime() != null && exam.getExamDateTime().plusMinutes(exam.getDurationMinutes() != null ? exam.getDurationMinutes() : 20).isAfter(LocalDateTime.now()))
                .toList());
        model.addAttribute("teacherExamRiskViews", examAutomationService.buildRiskViews(teacher));
        model.addAttribute("teacherExamCount", exams.size());
        model.addAttribute("teacherQuizCount", quizzes.size());
        model.addAttribute("teacherUnreadNotifications", examAutomationService.countUnreadNotifications("TEACHER", teacher.getEmail()));
        return "teacher/teacher-exams";
    }

    @PostMapping("/teacher-exams/create")
    public String createTeacherExam(@RequestParam String title,
                                    @RequestParam(required = false) String subject,
                                    @RequestParam(required = false) String syllabusTopics,
                                    @RequestParam(required = false) String instructions,
                                    @RequestParam(required = false) String seatingInfo,
                                    @RequestParam(required = false) String examDateTime,
                                    @RequestParam(required = false) Integer durationMinutes,
                                    @RequestParam(required = false) String room,
                                    @RequestParam(required = false) MultipartFile syllabusFile,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        Teacher teacher = getLoggedTeacher(session);
        if (teacher == null) return "redirect:/login";

        LocalDateTime scheduledAt;
        try {
            scheduledAt = examDateTime != null && !examDateTime.isBlank() ? LocalDateTime.parse(examDateTime) : null;
        } catch (Exception ex) {
            scheduledAt = null;
        }

        ExamSession exam = examAutomationService.createExam(
                teacher,
                title,
                subject,
                syllabusTopics,
                instructions,
                seatingInfo,
                scheduledAt,
                durationMinutes,
                room,
                syllabusFile
        );

        redirectAttributes.addFlashAttribute("examSuccess", "Exam scheduled successfully.");
        return "redirect:/teacher-exams";
    }

    @PostMapping("/teacher-exams/create-quiz")
    public String createTeacherQuiz(@RequestParam String title,
                                    @RequestParam(required = false) String subject,
                                    @RequestParam(required = false) String instructions,
                                    @RequestParam(required = false) String examDateTime,
                                    @RequestParam(required = false) Integer durationMinutes,
                                    @RequestParam(required = false) Integer questionCount,
                                    @RequestParam("quizPdfFile") MultipartFile quizPdfFile,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        Teacher teacher = getLoggedTeacher(session);
        if (teacher == null) return "redirect:/login";

        LocalDateTime scheduledAt;
        try {
            scheduledAt = examDateTime != null && !examDateTime.isBlank() ? LocalDateTime.parse(examDateTime) : null;
        } catch (Exception ex) {
            scheduledAt = null;
        }

        ExamSession quiz = examAutomationService.createQuizExam(
                teacher,
                title,
                subject,
                instructions,
                scheduledAt,
                durationMinutes,
                quizPdfFile,
                questionCount
        );
        if (quiz == null) {
            redirectAttributes.addFlashAttribute("examError", "Quiz generation failed. Upload a readable PDF and try again.");
            return "redirect:/teacher-exams";
        }

        redirectAttributes.addFlashAttribute("examSuccess", "Online quiz generated and published for students.");
        return "redirect:/teacher-exams";
    }

    @GetMapping("/teacher-exams/{examId}/attendance")
    public String examAttendance(@PathVariable Long examId, Model model, HttpSession session) {
        Teacher teacher = getLoggedTeacher(session);
        if (teacher == null) return "redirect:/login";

        ExamSession exam = examAutomationService.findExam(examId);
        if (exam == null) return "redirect:/teacher-exams";

        List<ExamAttendance> attendance = examAutomationService.getAttendance(exam);
        List<Student> students = exam.getClassRoom() != null ? studentRepository.findByClassRoom(exam.getClassRoom()) : List.of();
        Map<Long, String> statusByStudent = new LinkedHashMap<>();
        for (ExamAttendance row : attendance) {
            if (row.getStudent() != null) {
                statusByStudent.put(row.getStudent().getId(), row.getStatus());
            }
        }

        model.addAttribute("teacher", teacher);
        model.addAttribute("teacherName", teacher.getName());
        model.addAttribute("teacherSubject", teacher.getSubject());
        model.addAttribute("teacherClassLabel", resolveTeacherClassRoom(teacher) != null ? resolveTeacherClassRoom(teacher).getDisplayLabel() : "No class assigned");
        model.addAttribute("exam", exam);
        model.addAttribute("examAttendance", attendance);
        model.addAttribute("examStudents", students);
        model.addAttribute("examStatusByStudent", statusByStudent);
        model.addAttribute("seatViews", examAutomationService.buildSeatingPlan(exam));
        return "teacher/teacher-exam-attendance";
    }

    @PostMapping("/teacher-exams/{examId}/attendance")
    public String saveExamAttendance(@PathVariable Long examId,
                                     @RequestParam Map<String, String> params,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        Teacher teacher = getLoggedTeacher(session);
        if (teacher == null) return "redirect:/login";

        ExamSession exam = examAutomationService.findExam(examId);
        if (exam == null) {
            redirectAttributes.addFlashAttribute("examError", "Exam not found.");
            return "redirect:/teacher-exams";
        }

        Map<Long, String> statuses = new LinkedHashMap<>();
        params.forEach((key, value) -> {
            if (key.startsWith("status_")) {
                try {
                    Long studentId = Long.parseLong(key.substring("status_".length()));
                    statuses.put(studentId, value);
                } catch (Exception ignored) {
                }
            }
        });

        examAutomationService.saveAttendance(exam, statuses);
        exam.setStatus("COMPLETED");
        redirectAttributes.addFlashAttribute("examSuccess", "Exam attendance saved.");
        return "redirect:/teacher-exams/" + examId + "/attendance";
    }

    @GetMapping("/teacher-exams/{examId}/seating")
    public String examSeating(@PathVariable Long examId, Model model, HttpSession session) {
        Teacher teacher = getLoggedTeacher(session);
        if (teacher == null) return "redirect:/login";

        ExamSession exam = examAutomationService.findExam(examId);
        if (exam == null) return "redirect:/teacher-exams";

        model.addAttribute("teacher", teacher);
        model.addAttribute("exam", exam);
        model.addAttribute("seatViews", examAutomationService.buildSeatingPlan(exam));
        model.addAttribute("teacherClassLabel", resolveTeacherClassRoom(teacher) != null ? resolveTeacherClassRoom(teacher).getDisplayLabel() : "No class assigned");
        model.addAttribute("teacherSubject", teacher.getSubject());
        return "teacher/teacher-exam-seating";
    }

    @GetMapping("/exams")
    public String studentExams(Model model, HttpSession session) {
        Student student = getLoggedStudent(session);
        if (student == null) return "redirect:/login";

        List<ExamSession> exams = examAutomationService.findVisibleUpcomingExams(student);
        List<ExamSession> quizzes = examAutomationService.findVisibleUpcomingQuizzes(student);
        model.addAttribute("student", student);
        model.addAttribute("studentName", student.getName());
        model.addAttribute("studentCourse", student.getCourse());
        model.addAttribute("studentInitials", student.getName() != null && !student.getName().isBlank()
                ? student.getName().substring(0, Math.min(2, student.getName().length())).toUpperCase()
                : "ST");
        model.addAttribute("collegeName", resolveStudentCollegeName(student));
        model.addAttribute("collegeLogoPath", resolveStudentCollegeLogo(student));
        model.addAttribute("collegeInitials", buildCollegeInitials(resolveStudentCollegeName(student)));
        model.addAttribute("studentExamCount", exams.size());
        model.addAttribute("studentExams", exams);
        model.addAttribute("studentQuizCount", quizzes.size());
        model.addAttribute("studentQuizzes", quizzes);
        model.addAttribute("studentUnreadNotifications", examAutomationService.countUnreadNotifications("STUDENT", student.getEmail()));
        return "student/connected/exams";
    }

    @GetMapping("/student-exams/{examId}")
    public String studentExamDetails(@PathVariable Long examId, Model model, HttpSession session) {
        Student student = getLoggedStudent(session);
        if (student == null) return "redirect:/login";

        ExamSession exam = examAutomationService.findExam(examId);
        if (exam == null || !studentCanAccessExam(student, exam)) return "redirect:/exams";

        model.addAttribute("student", student);
        model.addAttribute("studentName", student.getName());
        model.addAttribute("studentCourse", student.getCourse());
        model.addAttribute("studentInitials", student.getName() != null && !student.getName().isBlank()
                ? student.getName().substring(0, Math.min(2, student.getName().length())).toUpperCase()
                : "ST");
        model.addAttribute("collegeName", resolveStudentCollegeName(student));
        model.addAttribute("collegeLogoPath", resolveStudentCollegeLogo(student));
        model.addAttribute("collegeInitials", buildCollegeInitials(resolveStudentCollegeName(student)));
        model.addAttribute("exam", exam);
        model.addAttribute("seatViews", examAutomationService.buildSeatingPlan(exam));
        if (exam.isQuiz()) {
            ExamQuizAttempt attempt = examAutomationService.findQuizAttempt(exam, student);
            List<ExamQuizQuestion> questions = examAutomationService.findQuizQuestions(exam);
            List<ExamQuizResponse> responses = examAutomationService.findQuizResponses(attempt);
            Map<Long, ExamQuizResponse> responseByQuestionId = responses.stream()
                    .filter(response -> response.getQuestion() != null && response.getQuestion().getId() != null)
                    .collect(Collectors.toMap(response -> response.getQuestion().getId(), response -> response, (left, right) -> left, LinkedHashMap::new));
            model.addAttribute("quizQuestions", questions);
            model.addAttribute("quizAttempt", attempt);
            model.addAttribute("quizResponsesByQuestionId", responseByQuestionId);
            model.addAttribute("quizAvailable", examAutomationService.isQuizAvailableForStudent(exam, student));
            model.addAttribute("quizRemainingSeconds", examAutomationService.quizRemainingSeconds(exam, attempt));
        }
        return "student/connected/exam-details";
    }

    @PostMapping("/student-exams/{examId}/quiz/start")
    public String startQuiz(@PathVariable Long examId, HttpSession session, RedirectAttributes redirectAttributes) {
        Student student = getLoggedStudent(session);
        if (student == null) return "redirect:/login";
        ExamSession exam = examAutomationService.findExam(examId);
        if (exam == null || !studentCanAccessExam(student, exam) || !exam.isQuiz()) {
            return "redirect:/exams";
        }
        ExamQuizAttempt attempt = examAutomationService.startQuizAttempt(exam, student);
        if (attempt == null) {
            redirectAttributes.addFlashAttribute("examError", "Quiz is not available right now.");
        }
        return "redirect:/student-exams/" + examId;
    }

    @PostMapping("/student-exams/{examId}/quiz/submit")
    public String submitQuiz(@PathVariable Long examId,
                             @RequestParam Map<String, String> params,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        Student student = getLoggedStudent(session);
        if (student == null) return "redirect:/login";
        ExamSession exam = examAutomationService.findExam(examId);
        if (exam == null || !studentCanAccessExam(student, exam) || !exam.isQuiz()) {
            return "redirect:/exams";
        }
        Map<Long, String> answers = new LinkedHashMap<>();
        params.forEach((key, value) -> {
            if (key.startsWith("answer_")) {
                try {
                    answers.put(Long.parseLong(key.substring("answer_".length())), value);
                } catch (Exception ignored) {
                }
            }
        });
        boolean autoSubmitted = "true".equalsIgnoreCase(params.get("autoSubmit"))
                || examAutomationService.quizRemainingSeconds(exam, examAutomationService.findQuizAttempt(exam, student)) <= 0;
        examAutomationService.submitQuizAttempt(exam, student, answers, autoSubmitted);
        redirectAttributes.addFlashAttribute("examSuccess", autoSubmitted ? "Quiz auto-submitted when the timer ended." : "Quiz submitted successfully.");
        return "redirect:/student-exams/" + examId;
    }

    @GetMapping("/teacher-exams/{examId}/quiz-results")
    public String teacherQuizResults(@PathVariable Long examId, Model model, HttpSession session) {
        Teacher teacher = getLoggedTeacher(session);
        if (teacher == null) return "redirect:/login";
        ExamSession exam = examAutomationService.findExam(examId);
        if (exam == null || !exam.isQuiz() || exam.getTeacher() == null || !exam.getTeacher().getId().equals(teacher.getId())) {
            return "redirect:/teacher-exams";
        }
        List<ExamQuizAttempt> attempts = examAutomationService.findQuizAttemptsForTeacher(exam, teacher);
        Map<Long, List<ExamQuizResponse>> responsesByAttemptId = new LinkedHashMap<>();
        for (ExamQuizAttempt attempt : attempts) {
            responsesByAttemptId.put(attempt.getId(), examAutomationService.findQuizResponses(attempt));
        }
        model.addAttribute("teacher", teacher);
        model.addAttribute("teacherName", teacher.getName());
        model.addAttribute("teacherSubject", teacher.getSubject());
        model.addAttribute("teacherClassLabel", resolveTeacherClassRoom(teacher) != null ? resolveTeacherClassRoom(teacher).getDisplayLabel() : "No class assigned");
        model.addAttribute("exam", exam);
        model.addAttribute("quizQuestions", examAutomationService.findQuizQuestions(exam));
        model.addAttribute("quizAttempts", attempts);
        model.addAttribute("quizResponsesByAttemptId", responsesByAttemptId);
        return "teacher/teacher-quiz-results";
    }

    private Teacher getLoggedTeacher(HttpSession session) {
        String email = (String) session.getAttribute("loggedInUser");
        String role = (String) session.getAttribute("userRole");
        if (email == null || (role != null && !"TEACHER".equalsIgnoreCase(role))) {
            return null;
        }
        return teacherRepository.findByEmail(email);
    }

    private Student getLoggedStudent(HttpSession session) {
        String email = (String) session.getAttribute("loggedInUser");
        String role = (String) session.getAttribute("userRole");
        if (email == null || (role != null && !"STUDENT".equalsIgnoreCase(role))) {
            return null;
        }
        return studentRepository.findByEmail(email);
    }

    private ClassRoom resolveTeacherClassRoom(Teacher teacher) {
        if (teacher == null) {
            return null;
        }
        if (teacher.getClassRoom() != null && teacher.getClassRoom().getId() != null) {
            return teacher.getClassRoom();
        }
        return timetableRepository.findByTeacher(teacher).stream()
                .map(ExamSessionIgnored -> ExamSessionIgnored.getClassRoom())
                .filter(classRoom -> classRoom != null && classRoom.getId() != null)
                .findFirst()
                .orElse(null);
    }

    private boolean studentCanAccessExam(Student student, ExamSession exam) {
        if (student == null || exam == null) {
            return false;
        }
        Set<Long> visibleIds = new HashSet<>();
        examAutomationService.findStudentExams(student).forEach(entry -> {
            if (entry != null && entry.getId() != null) {
                visibleIds.add(entry.getId());
            }
        });
        return exam.getId() != null && visibleIds.contains(exam.getId());
    }

    private String resolveStudentCollegeName(Student student) {
        if (student != null && student.getAdmin() != null && student.getAdmin().getCollegeName() != null
                && !student.getAdmin().getCollegeName().isBlank()) {
            return student.getAdmin().getCollegeName().trim();
        }
        return "AI Campus Institute";
    }

    private String resolveStudentCollegeLogo(Student student) {
        if (student != null && student.getAdmin() != null && student.getAdmin().getCollege() != null) {
            return student.getAdmin().getCollege().getLogoPath();
        }
        return null;
    }

    private String buildCollegeInitials(String collegeName) {
        if (collegeName == null || collegeName.isBlank()) {
            return "AI";
        }
        String[] parts = collegeName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }
}
