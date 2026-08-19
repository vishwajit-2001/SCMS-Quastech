package com.scms.scms.controller;

import com.scms.scms.model.CampusEvent;
import com.scms.scms.model.CampusEventApplication;
import com.scms.scms.model.Student;
import com.scms.scms.model.Teacher;
import com.scms.scms.repository.CampusEventApplicationRepository;
import com.scms.scms.repository.CampusEventRepository;
import com.scms.scms.repository.StudentRepository;
import com.scms.scms.repository.TeacherRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.PrintWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Controller
public class EventController {

    @Autowired private CampusEventRepository campusEventRepository;
    @Autowired private CampusEventApplicationRepository campusEventApplicationRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private TeacherRepository teacherRepository;

    @GetMapping("/events")
    public String studentEvents(Model model, HttpSession session) {
        Student student = getLoggedStudent(session);
        if (student == null) return "redirect:/login";

        List<CampusEvent> events = campusEventRepository.findByPublishedTrueOrderByCreatedAtDesc().stream()
                .sorted(Comparator.comparing(CampusEvent::getEventDateTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        List<Long> appliedEventIds = campusEventApplicationRepository.findByStudentOrderByAppliedAtDesc(student).stream()
                .map(application -> application.getCampusEvent().getId())
                .toList();

        model.addAttribute("student", student);
        model.addAttribute("studentName", safe(student.getName(), "Student"));
        model.addAttribute("studentCourse", safe(student.getCourse(), "Not assigned"));
        model.addAttribute("studentSemesterLabel", safe(student.getSemester(), "Not assigned"));
        model.addAttribute("studentEmail", safe(student.getEmail(), "Not available"));
        model.addAttribute("studentInitials", buildInitials(student.getName()));
        model.addAttribute("collegeName", resolveCollegeName(student));
        model.addAttribute("collegeLogoPath", resolveCollegeLogo(student));
        model.addAttribute("collegeInitials", buildCollegeInitials(resolveCollegeName(student)));
        model.addAttribute("notificationCount", 0);
        model.addAttribute("events", events);
        model.addAttribute("eventCount", events.size());
        model.addAttribute("eventAppliedIds", appliedEventIds);
        model.addAttribute("eventEmptyMessage", "No events or activities are live for your profile yet.");
        return "student/connected/events";
    }

    @PostMapping("/events/apply/{eventId}")
    public String apply(@PathVariable Long eventId,
                        @RequestParam String fullName,
                        @RequestParam String phone,
                        @RequestParam String email,
                        @RequestParam String course,
                        @RequestParam String semester,
                        @RequestParam(required = false) String specialization,
                        @RequestParam(required = false) String sectionName,
                        @RequestParam(required = false) String notes,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        Student student = getLoggedStudent(session);
        if (student == null) return "redirect:/login";

        CampusEvent campusEvent = campusEventRepository.findById(eventId).orElse(null);
        if (campusEvent == null || !campusEvent.isPublished()) {
            redirectAttributes.addFlashAttribute("eventError", "This event is not available.");
            return "redirect:/events";
        }

        if (!campusEvent.isRegistrationOpen()) {
            redirectAttributes.addFlashAttribute("eventError", "Registration deadline has closed for this event.");
            return "redirect:/events";
        }

        if (campusEventApplicationRepository.existsByCampusEventAndEmail(campusEvent, email.trim())) {
            redirectAttributes.addFlashAttribute("eventError", "You have already registered for this event.");
            return "redirect:/events";
        }

        CampusEventApplication application = new CampusEventApplication();
        application.setCampusEvent(campusEvent);
        application.setStudent(student);
        application.setFullName(safe(fullName, student.getName()));
        application.setPhone(normalizeBlank(phone));
        application.setEmail(normalizeBlank(email));
        application.setCourse(normalizeBlank(course));
        application.setSemester(normalizeBlank(semester));
        application.setSpecialization(normalizeBlank(specialization));
        application.setSectionName(normalizeBlank(sectionName));
        application.setNotes(normalizeBlank(notes));
        application.setAppliedAt(LocalDateTime.now());
        campusEventApplicationRepository.save(application);

        redirectAttributes.addFlashAttribute("eventSuccess", "Your event registration was submitted successfully.");
        return "redirect:/events";
    }

    @GetMapping("/teacher-events")
    public String teacherEvents(Model model, HttpSession session) {
        Teacher teacher = getLoggedTeacher(session);
        if (teacher == null) return "redirect:/login";

        addTeacherEventsModel(model, teacher);
        return "teacher/teacher-events";
    }

    @PostMapping("/teacher-events")
    public String createEvent(@RequestParam String title,
                              @RequestParam(required = false) String organizationName,
                              @RequestParam(required = false) String venue,
                              @RequestParam(required = false) String category,
                              @RequestParam(required = false) String description,
                              @RequestParam(required = false) String eventDate,
                              @RequestParam(required = false) String eventTime,
                              @RequestParam(required = false) String registrationDeadlineDate,
                              @RequestParam(required = false) String registrationDeadlineTime,
                              @RequestParam(required = false) String targetAudience,
                              @RequestParam(defaultValue = "true") boolean published,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        Teacher teacher = getLoggedTeacher(session);
        if (teacher == null) return "redirect:/login";

        CampusEvent campusEvent = new CampusEvent();
        campusEvent.setTitle(safe(title, "Event"));
        campusEvent.setOrganizationName(normalizeBlank(organizationName));
        campusEvent.setOrganizerName(teacher.getName());
        campusEvent.setVenue(normalizeBlank(venue));
        campusEvent.setCategory(normalizeBlank(category));
        campusEvent.setDescription(normalizeBlank(description));
        campusEvent.setEventDateTime(parseDateTime(eventDate, eventTime));
        campusEvent.setRegistrationDeadline(parseDateTime(registrationDeadlineDate, registrationDeadlineTime));
        campusEvent.setTargetAudience(normalizeBlank(targetAudience));
        campusEvent.setPublished(published);
        campusEvent.setCreatedAt(LocalDateTime.now());
        campusEvent.setTeacher(teacher);
        campusEventRepository.save(campusEvent);

        redirectAttributes.addFlashAttribute("eventSuccess", "Event published successfully.");
        return "redirect:/teacher-events";
    }

    @GetMapping("/teacher-events/download/{eventId}")
    public void downloadEventApplications(@PathVariable Long eventId, HttpSession session, HttpServletResponse response) throws IOException {
        Teacher teacher = getLoggedTeacher(session);
        if (teacher == null) {
            response.sendRedirect("/login");
            return;
        }

        CampusEvent campusEvent = campusEventRepository.findById(eventId).orElse(null);
        if (campusEvent == null || campusEvent.getTeacher() == null || !campusEvent.getTeacher().getId().equals(teacher.getId())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        List<CampusEventApplication> applications = campusEventApplicationRepository.findByCampusEventOrderByAppliedAtDesc(campusEvent);

        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"event-" + eventId + "-applications.csv\"");

        try (PrintWriter writer = response.getWriter()) {
            writer.println("fullName,email,phone,course,semester,specialization,sectionName,notes,appliedAt");
            for (CampusEventApplication application : applications) {
                writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        csv(application.getFullName()),
                        csv(application.getEmail()),
                        csv(application.getPhone()),
                        csv(application.getCourse()),
                        csv(application.getSemester()),
                        csv(application.getSpecialization()),
                        csv(application.getSectionName()),
                        csv(application.getNotes()),
                        application.getAppliedAt() != null ? application.getAppliedAt().toString() : "");
            }
        }
    }

    private void addTeacherEventsModel(Model model, Teacher teacher) {
        TeacherEventsViewData data = buildTeacherEventsData(teacher);
        model.addAttribute("teacher", teacher);
        model.addAttribute("teacherName", teacher.getName());
        model.addAttribute("teacherEmail", teacher.getEmail());
        model.addAttribute("teacherSubject", teacher.getSubject());
        model.addAttribute("teacherInitials", buildInitials(teacher.getName()));
        model.addAttribute("campusEvents", data.events());
        model.addAttribute("campusEventApplications", data.applications());
        model.addAttribute("campusEventCount", data.events().size());
        model.addAttribute("campusEventApplicationCount", data.applications().size());
        model.addAttribute("campusEventOpenCount", data.events().stream().filter(CampusEvent::isRegistrationOpen).count());
        model.addAttribute("recentCampusEventApplications", data.applications().stream().limit(8).toList());
    }

    private TeacherEventsViewData buildTeacherEventsData(Teacher teacher) {
        List<CampusEvent> events = campusEventRepository.findByTeacherOrderByCreatedAtDesc(teacher);
        List<CampusEventApplication> applications = new ArrayList<>();
        for (CampusEvent event : events) {
            applications.addAll(campusEventApplicationRepository.findByCampusEventOrderByAppliedAtDesc(event));
        }
        return new TeacherEventsViewData(events, applications);
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private record TeacherEventsViewData(List<CampusEvent> events, List<CampusEventApplication> applications) {}

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

    private LocalDateTime parseDateTime(String dateValue, String timeValue) {
        if (dateValue == null || dateValue.isBlank()) return null;
        try {
            java.time.LocalDate date = java.time.LocalDate.parse(dateValue.trim());
            java.time.LocalTime time = (timeValue == null || timeValue.isBlank()) ? java.time.LocalTime.of(23, 59) : java.time.LocalTime.parse(timeValue.trim());
            return LocalDateTime.of(date, time);
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

    private String buildInitials(String name) {
        if (name == null || name.isBlank()) return "EV";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ENGLISH);
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ENGLISH);
    }

    private String buildCollegeInitials(String collegeName) {
        if (collegeName == null || collegeName.isBlank()) return "AI";
        String[] parts = collegeName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ENGLISH);
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ENGLISH);
    }

    private String resolveCollegeName(Student student) {
        if (student != null && student.getAdmin() != null) {
            return safe(student.getAdmin().getCollegeName(), "AI Campus Institute");
        }
        return "AI Campus Institute";
    }

    private String resolveCollegeLogo(Student student) {
        if (student != null && student.getAdmin() != null && student.getAdmin().getCollege() != null) {
            return student.getAdmin().getCollege().getLogoPath();
        }
        return null;
    }

    private String csv(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
