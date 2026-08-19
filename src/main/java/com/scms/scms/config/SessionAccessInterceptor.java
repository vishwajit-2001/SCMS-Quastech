package com.scms.scms.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@Component
public class SessionAccessInterceptor implements HandlerInterceptor {

    private static final Set<String> STUDENT_PATHS = Set.of(
            "/student-dashboard",
            "/exam-results",
            "/timetable",
            "/subjects",
            "/attendance",
            "/exams",
            "/assignments",
            "/study-materials",
            "/events",
            "/placements",
            "/profile",
            "/student-profile",
            "/fees",
            "/notifications",
            "/student-marks",
            "/student-attendance"
    );

    private static final Set<String> ADMIN_EXACT_PATHS = Set.of(
            "/admin-dashboard",
            "/add-student",
            "/admin-students",
            "/add-teacher",
            "/admin-teachers",
            "/add-class",
            "/view-classes",
            "/add-timetable",
            "/view-timetable",
            "/smart-timetable",
            "/admin-fees",
            "/pay-fees",
            "/set-fees",
            "/admin-notifications",
            "/admin-profile",
            "/change-admin-password",
            "/admin-reports",
            "/admin-teacher-assignment"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        String requiredRole = resolveRequiredRole(path);

        if (requiredRole == null) {
            return true;
        }

        HttpSession session = request.getSession(false);
        String loggedInUser = session != null ? (String) session.getAttribute("loggedInUser") : null;
        String userRole = session != null ? (String) session.getAttribute("userRole") : null;

        if (loggedInUser == null || userRole == null) {
            reject(request, response, requiredRole);
            return false;
        }

        if (!"AUTHENTICATED".equals(requiredRole) && !requiredRole.equalsIgnoreCase(userRole)) {
            reject(request, response, requiredRole);
            return false;
        }

        return true;
    }

    private String resolveRequiredRole(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }

        if (path.equals("/teacher-assignment")
                || path.startsWith("/teacher-assignment/")
                || path.matches("^/teacher-(students|subjects|dashboard)/\\d+$")) {
            return "ADMIN";
        }

        if (path.startsWith("/teacher-")) {
            return "TEACHER";
        }

        if (path.startsWith("/placement-")) {
            return "PLACEMENT_TPO";
        }

        if (path.startsWith("/study-materials/view/")
                || path.startsWith("/study-materials/download/")
                || path.startsWith("/teacher-assignments/file/")
                || path.startsWith("/teacher-assignment-submissions/")
                || path.startsWith("/assignments/file/")
                || path.startsWith("/assignments/submission/")) {
            return "AUTHENTICATED";
        }

        if (STUDENT_PATHS.contains(path) || path.startsWith("/assignments/") || path.startsWith("/student-exams/")) {
            return "STUDENT";
        }

        if (ADMIN_EXACT_PATHS.contains(path)
                || path.startsWith("/delete-student/")
                || path.startsWith("/delete-teacher/")
                || path.startsWith("/delete-timetable/")
                || path.startsWith("/update-fees/")) {
            return "ADMIN";
        }

        return null;
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, String requiredRole) throws Exception {
        if (expectsJson(request)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Session expired. Please log in again.\"}");
            return;
        }
        response.sendRedirect(request.getContextPath() + loginPath(requiredRole));
    }

    private String loginPath(String requiredRole) {
        if ("ADMIN".equalsIgnoreCase(requiredRole)) {
            return "/login-admin";
        }
        if ("TEACHER".equalsIgnoreCase(requiredRole)) {
            return "/login-teacher";
        }
        if ("STUDENT".equalsIgnoreCase(requiredRole)) {
            return "/login-student";
        }
        if ("PLACEMENT_TPO".equalsIgnoreCase(requiredRole)) {
            return "/login";
        }
        return "/login";
    }

    private boolean expectsJson(HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");
        return "XMLHttpRequest".equalsIgnoreCase(requestedWith)
                || (accept != null && accept.toLowerCase().contains("application/json"));
    }
}
