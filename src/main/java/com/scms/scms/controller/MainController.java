package com.scms.scms.controller;

import com.scms.scms.model.Admin;
import com.scms.scms.model.College;
import com.scms.scms.model.PlacementTpo;
import com.scms.scms.model.Student;
import com.scms.scms.model.Teacher;
import com.scms.scms.repository.AdminRepository;
import com.scms.scms.repository.CollegeRepository;
import com.scms.scms.repository.PlacementTpoRepository;
import com.scms.scms.repository.StudentRepository;
import com.scms.scms.repository.TeacherRepository;
import com.scms.scms.service.EmployeeIdService;
import com.scms.scms.service.PasswordProtectionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Locale;

@Controller
public class MainController {

    @Autowired private StudentRepository studentRepository;
    @Autowired private TeacherRepository teacherRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private CollegeRepository collegeRepository;
    @Autowired private PlacementTpoRepository placementTpoRepository;
    @Autowired private EmployeeIdService employeeIdService;
    @Autowired private PasswordProtectionService passwordProtectionService;

    @GetMapping("/")
    public String home(Model model) {
        List<CollegeOption> colleges = collegeOptions();
        model.addAttribute("colleges", colleges);
        model.addAttribute("defaultRegistrationUrl", colleges.isEmpty() ? null : "/Registration/Apply/" + colleges.get(0).getCode());
        return "home/home";
    }

    @PostMapping({"/register", "/register/{collegeCode}"})
    public String register(
            @PathVariable(value = "collegeCode", required = false) String collegeCodePath,
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(value = "confirmPassword", required = false) String confirmPassword,
            @RequestParam(value = "mobileNumber", required = false) String mobileNumber,
            @RequestParam(value = "collegeCode", required = false) String collegeCodeParam,
            RedirectAttributes redirectAttributes) {

        String cleanName = name == null ? "" : name.trim();
        String cleanEmail = email == null ? "" : email.trim();
        String cleanMobile = mobileNumber == null ? "" : mobileNumber.trim();
        String targetCollegeCode = firstNonBlank(collegeCodePath, collegeCodeParam);

        if (cleanName.isBlank() || cleanEmail.isBlank() || password == null || password.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "All fields are required!");
            redirectAttributes.addFlashAttribute("showRegister", true);
            return redirectToCollegePortalOrHome(targetCollegeCode, "register");
        }

        if (confirmPassword != null && !confirmPassword.isBlank() && !password.trim().equals(confirmPassword.trim())) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match!");
            redirectAttributes.addFlashAttribute("showRegister", true);
            return redirectToCollegePortalOrHome(targetCollegeCode, "register");
        }

        if (isDuplicateEmail(cleanEmail)) {
            redirectAttributes.addFlashAttribute("error", "Email already exists");
            redirectAttributes.addFlashAttribute("showRegister", true);
            return redirectToCollegePortalOrHome(targetCollegeCode, "register");
        }

        if (!cleanMobile.isBlank() && isDuplicateMobile(cleanMobile)) {
            redirectAttributes.addFlashAttribute("error", "Mobile number already exists");
            redirectAttributes.addFlashAttribute("showRegister", true);
            return redirectToCollegePortalOrHome(targetCollegeCode, "register");
        }

        try {
            String selectedCollegeCode = targetCollegeCode;
            if (selectedCollegeCode == null || selectedCollegeCode.isBlank()) {
                redirectAttributes.addFlashAttribute("error", "Open the college registration link first.");
                redirectAttributes.addFlashAttribute("showRegister", true);
                return redirectToCollegePortalOrHome(null, "register");
            }

            College selectedCollege = collegeRepository.findByCodeIgnoreCase(selectedCollegeCode);
            if (selectedCollege == null) {
                redirectAttributes.addFlashAttribute("error", "Selected college is not configured.");
                redirectAttributes.addFlashAttribute("showRegister", true);
                return redirectToCollegePortalOrHome(selectedCollegeCode, "register");
            }
            if (!selectedCollege.isActive()) {
                redirectAttributes.addFlashAttribute("error", "Selected college is currently inactive.");
                redirectAttributes.addFlashAttribute("showRegister", true);
                return redirectToCollegePortalOrHome(selectedCollegeCode, "register");
            }

            Admin owningAdmin = adminRepository.findFirstByCollege_Id(selectedCollege.getId());
            if (owningAdmin == null) {
                redirectAttributes.addFlashAttribute("error", "Selected college is not linked to an admin yet.");
                redirectAttributes.addFlashAttribute("showRegister", true);
                return redirectToCollegePortalOrHome(selectedCollegeCode, "register");
            }

            Student s = new Student();
            s.setName(cleanName);
            s.setEmail(normalizeEmail(cleanEmail));
            s.setPassword(passwordProtectionService.encode(password.trim()));
            s.setMobileNumber(cleanMobile.isBlank() ? null : cleanMobile);
            s.setAdmissionStatus("APPLIED");
            s.setOnboardingCompleted(Boolean.FALSE);
            s.setAdmin(owningAdmin);
            studentRepository.save(s);

            redirectAttributes.addFlashAttribute("success", "Registered successfully! Please login.");
            redirectAttributes.addFlashAttribute("showLogin", true);
        } catch (DataIntegrityViolationException e) {
            redirectAttributes.addFlashAttribute("error", registrationConflictMessage(e));
            redirectAttributes.addFlashAttribute("showRegister", true);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Registration failed: " + e.getMessage());
            redirectAttributes.addFlashAttribute("showRegister", true);
        }

        return redirectToCollegePortalOrHome(targetCollegeCode, "login");
    }

    @GetMapping({"/Registration/Apply/{collegeIdentifier}", "/register/{collegeIdentifier}"})
    public String applyForCollege(@PathVariable String collegeIdentifier,
                                  @RequestParam(value = "mode", required = false) String mode,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        College selectedCollege = resolveCollegeForRegistration(collegeIdentifier);
        if (selectedCollege == null) {
            redirectAttributes.addFlashAttribute("error", "Registration link is invalid.");
            return "redirect:/";
        }
        if (!selectedCollege.isActive()) {
            redirectAttributes.addFlashAttribute("error", "This college registration is currently closed.");
            return "redirect:/";
        }

        model.addAttribute("colleges", collegeOptions());
        populateRegistrationCollege(model, selectedCollege);
        model.addAttribute("defaultRegistrationUrl", "/Registration/Apply/" + selectedCollege.getCode());
        model.addAttribute("registrationCollegeLogoUrl", normalizePublicLogoPath(selectedCollege.getLogoPath()));
        model.addAttribute("registrationViewMode", firstNonBlank(mode, "register"));
        return "home/registration-apply";
    }

    @PostMapping("/login")
    public String login(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(value = "loginType", required = false) String loginType,
            @RequestParam(value = "collegeCode", required = false) String collegeCode,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String cleanEmail = email == null ? "" : email.trim();
        String cleanPassword = password == null ? "" : password.trim();
        String cleanLoginType = firstNonBlank(loginType);
        boolean studentOnly = "student".equalsIgnoreCase(cleanLoginType);
        boolean instituteOnly = "institute".equalsIgnoreCase(cleanLoginType);

        if (cleanEmail.isBlank() || cleanPassword.isBlank()) {
            redirectAttributes.addFlashAttribute("errorLogin", "Email and password are required!");
            redirectAttributes.addFlashAttribute("showLogin", true);
            return redirectToCollegePortalOrHome(collegeCode, "login");
        }

        Admin admin = adminRepository.findByEmail(cleanEmail);
        if (admin != null && passwordProtectionService.matches(cleanPassword, admin.getPassword())) {
            if (admin.isSuperAdmin()) {
                redirectAttributes.addFlashAttribute("error", "Use the Super Admin login page for this account.");
                if (!studentOnly) {
                    redirectAttributes.addFlashAttribute("showLogin", true);
                }
                return "redirect:/superadmin";
            }
            if (studentOnly) {
                redirectAttributes.addFlashAttribute("errorLogin", "This email is not registered. Please sign up first.");
                redirectAttributes.addFlashAttribute("showLogin", true);
                return redirectToCollegePortalOrHome(collegeCode, "login");
            }
            String adminCollegeCode = resolveAdminCollegeCode(admin);
            if (!isSameCollegePortal(collegeCode, adminCollegeCode)) {
                redirectAttributes.addFlashAttribute(
                        "errorLogin",
                        "No details found. Please contact administrator.");
                redirectAttributes.addFlashAttribute("showLogin", true);
                return redirectToCollegePortalOrHome(collegeCode, "login");
            }
            upgradeLegacyPasswordIfNeeded(admin, cleanPassword);
            if (!admin.isSuperAdmin() && !admin.isCollegeActive()) {
                redirectAttributes.addFlashAttribute("errorLogin", "This college account is inactive. Contact the super admin.");
                redirectAttributes.addFlashAttribute("showLogin", true);
                return redirectToCollegePortalOrHome(collegeCode, "login");
            }
            session.setAttribute("loggedInUser", admin.getEmail());
            session.setAttribute("loggedInUserId", admin.getId());
            session.setAttribute("collegeCode", resolveAdminCollegeCode(admin));
            if (admin.isSuperAdmin()) {
                session.setAttribute("userRole", "SUPER_ADMIN");
                return "redirect:/super-admin-dashboard";
            }
            session.setAttribute("userRole", "ADMIN");
            return "redirect:/admin-dashboard";
        }

        Teacher teacher = teacherRepository.findByEmail(cleanEmail);
        if (teacher != null && passwordProtectionService.matches(cleanPassword, teacher.getPassword())) {
            if (studentOnly) {
                redirectAttributes.addFlashAttribute("errorLogin", "This email is not registered. Please sign up first.");
                redirectAttributes.addFlashAttribute("showLogin", true);
                return redirectToCollegePortalOrHome(collegeCode, "login");
            }
            String teacherCollegeCode = resolveTeacherCollegeCode(teacher);
            if (!isSameCollegePortal(collegeCode, teacherCollegeCode)) {
                redirectAttributes.addFlashAttribute(
                        "errorLogin",
                        "No details found. Please contact administrator.");
                redirectAttributes.addFlashAttribute("showLogin", true);
                return redirectToCollegePortalOrHome(collegeCode, "login");
            }
            teacher = employeeIdService.ensureTeacherEmployeeId(teacher);
            upgradeLegacyPasswordIfNeeded(teacher, cleanPassword);
            session.setAttribute("loggedInUser", teacher.getEmail());
            session.setAttribute("loggedInUserId", teacher.getId());
            session.setAttribute("loggedTeacherId", teacher.getId());
            session.setAttribute("loggedTeacherAdminId", teacher.getAdmin() != null ? teacher.getAdmin().getId() : null);
            session.setAttribute("collegeCode", resolveTeacherCollegeCode(teacher));
            session.setAttribute("userRole", "TEACHER");
            return "redirect:/teacher-dashboard";
        }

        PlacementTpo placementTpo = resolvePlacementTpoForLogin(cleanEmail, cleanPassword);
        if (placementTpo != null) {
            if (studentOnly) {
                redirectAttributes.addFlashAttribute("errorLogin", "This email is not registered. Please sign up first.");
                redirectAttributes.addFlashAttribute("showLogin", true);
                return redirectToCollegePortalOrHome(collegeCode, "login");
            }
            String tpoCollegeCode = resolvePlacementTpoCollegeCode(placementTpo);
            if (!isSameCollegePortal(collegeCode, tpoCollegeCode)) {
                redirectAttributes.addFlashAttribute(
                        "errorLogin",
                        "No details found. Please contact administrator.");
                redirectAttributes.addFlashAttribute("showLogin", true);
                return redirectToCollegePortalOrHome(collegeCode, "login");
            }
            placementTpo = employeeIdService.ensureTpoEmployeeId(placementTpo);
            upgradeLegacyPasswordIfNeeded(placementTpo, cleanPassword);
            session.setAttribute("loggedInUser", placementTpo.getEmail());
            session.setAttribute("loggedInUserId", placementTpo.getId());
            session.setAttribute("collegeCode", resolvePlacementTpoCollegeCode(placementTpo));
            session.setAttribute("userRole", "PLACEMENT_TPO");
            return "redirect:/placement-dashboard";
        }

        Student student = findStudentByEmail(cleanEmail);
        if (student != null && passwordProtectionService.matches(cleanPassword, student.getPassword())) {
            if (instituteOnly) {
                redirectAttributes.addFlashAttribute("errorLogin", "No details found. Please contact administrator.");
                redirectAttributes.addFlashAttribute("showLogin", true);
                return redirectToCollegePortalOrHome(collegeCode, "login");
            }

            String studentCollegeCode = resolveStudentCollegeCode(student);
            if (!isSameCollegePortal(collegeCode, studentCollegeCode)) {
                redirectAttributes.addFlashAttribute("errorLogin", "This email is not registered. Please sign up first.");
                redirectAttributes.addFlashAttribute("showLogin", true);
                return redirectToCollegePortalOrHome(collegeCode, "login");
            }

            upgradeLegacyPasswordIfNeeded(student, cleanPassword);
            session.setAttribute("loggedInUser", student.getEmail());
            session.setAttribute("collegeCode", resolveStudentCollegeCode(student));
            session.setAttribute("userRole", "STUDENT");
            if (shouldCompleteStudentOnboarding(student)) {
                String targetCollegeCode = firstNonBlank(studentCollegeCode, collegeCode);
                return targetCollegeCode == null
                        ? "redirect:/student-onboarding"
                        : "redirect:/student-onboarding/" + targetCollegeCode;
            }
            return "redirect:/student-dashboard";
        }

        if (studentOnly) {
            redirectAttributes.addFlashAttribute("errorLogin", "This email is not registered. Please sign up first.");
        } else if (instituteOnly) {
            redirectAttributes.addFlashAttribute("errorLogin", "No details found. Please contact administrator.");
        } else {
            redirectAttributes.addFlashAttribute("errorLogin", "Invalid email or password!");
        }
        redirectAttributes.addFlashAttribute("showLogin", true);
        return redirectToCollegePortalOrHome(collegeCode, "login");
    }

    @GetMapping("/login")
    public String loginPage(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("showLogin", true);
        return "redirect:/";
    }

    @GetMapping("/register")
    public String registerPage(RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("showRegister", true);
        return "redirect:/";
    }

    @GetMapping("/login-student")
    public String logoutStudent(HttpSession session, RedirectAttributes redirectAttributes) {
        String collegeCode = resolveCollegeCodeForLogout(session);
        session.invalidate();
        redirectAttributes.addFlashAttribute("showLogin", true);
        return redirectToCollegePortalOrHome(collegeCode, "login");
    }

    @GetMapping("/login-teacher")
    public String logoutTeacher(HttpSession session, RedirectAttributes redirectAttributes) {
        String collegeCode = resolveCollegeCodeForLogout(session);
        session.invalidate();
        redirectAttributes.addFlashAttribute("showLogin", true);
        return redirectToCollegePortalOrHome(collegeCode, "login");
    }

    @GetMapping("/login-admin")
    public String logoutAdmin(HttpSession session, RedirectAttributes redirectAttributes) {
        String collegeCode = resolveCollegeCodeForLogout(session);
        session.invalidate();
        redirectAttributes.addFlashAttribute("showLogin", true);
        return redirectToCollegePortalOrHome(collegeCode, "login");
    }

    @GetMapping("/superadmin")
    public String superAdminLoginPage(HttpSession session) {
        String userRole = session != null ? (String) session.getAttribute("userRole") : null;
        if ("SUPER_ADMIN".equalsIgnoreCase(userRole)) {
            return "redirect:/super-admin-dashboard";
        }
        return "super-admin/super-admin-login";
    }

    @GetMapping("/super-admin/logout")
    public String logoutSuperAdmin(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/";
    }

    @PostMapping("/superadmin")
    public String superAdminLogin(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String cleanEmail = email == null ? "" : email.trim();
        String cleanPassword = password == null ? "" : password.trim();

        if (cleanEmail.isBlank() || cleanPassword.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Email and password are required.");
            return "redirect:/superadmin";
        }

        Admin superAdmin = adminRepository.findByEmail(cleanEmail);
        if (superAdmin == null || !superAdmin.isSuperAdmin() || !passwordProtectionService.matches(cleanPassword, superAdmin.getPassword())) {
            redirectAttributes.addFlashAttribute("error", "Invalid super admin credentials.");
            return "redirect:/superadmin";
        }

        upgradeLegacyPasswordIfNeeded(superAdmin, cleanPassword);
        session.setAttribute("loggedInUser", superAdmin.getEmail());
        session.setAttribute("loggedInUserId", superAdmin.getId());
        session.setAttribute("userRole", "SUPER_ADMIN");
        return "redirect:/super-admin-dashboard";
    }

    @GetMapping("/overview")
    public String overview() {
        return "redirect:/#overview";
    }

    @GetMapping("/courses")
    public String courses() {
        return "redirect:/#courses";
    }

    @GetMapping("/contact")
    public String contact() {
        return "redirect:/#contact";
    }

    private void upgradeLegacyPasswordIfNeeded(Admin admin, String rawPassword) {
        if (passwordProtectionService.needsUpgrade(admin.getPassword())) {
            admin.setPassword(passwordProtectionService.encode(rawPassword));
            adminRepository.save(admin);
        }
    }

    private void upgradeLegacyPasswordIfNeeded(Teacher teacher, String rawPassword) {
        if (passwordProtectionService.needsUpgrade(teacher.getPassword())) {
            teacher.setPassword(passwordProtectionService.encode(rawPassword));
            teacherRepository.save(teacher);
        }
    }

    private void upgradeLegacyPasswordIfNeeded(PlacementTpo placementTpo, String rawPassword) {
        if (passwordProtectionService.needsUpgrade(placementTpo.getPassword())) {
            placementTpo.setPassword(passwordProtectionService.encode(rawPassword));
            placementTpoRepository.save(placementTpo);
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

    private void upgradeLegacyPasswordIfNeeded(Student student, String rawPassword) {
        if (passwordProtectionService.needsUpgrade(student.getPassword())) {
            student.setPassword(passwordProtectionService.encode(rawPassword));
            studentRepository.save(student);
        }
    }

    private Student findStudentByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail == null) {
            return null;
        }
        Student student = studentRepository.findByEmailNormalized(normalizedEmail);
        if (student != null) {
            return student;
        }
        return studentRepository.findAll().stream()
                .filter(candidate -> candidate != null && normalizeEmail(candidate.getEmail()) != null
                        && normalizeEmail(candidate.getEmail()).equals(normalizedEmail))
                .findFirst()
                .orElse(null);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ENGLISH);
    }

    private boolean shouldCompleteStudentOnboarding(Student student) {
        if (student == null) {
            return true;
        }
        if ("ADMITTED".equalsIgnoreCase(firstNonBlank(student.getAdmissionStatus()))) {
            return false;
        }
        return !Boolean.TRUE.equals(student.getOnboardingCompleted());
    }

    private boolean isDuplicateEmail(String email) {
        String cleanEmail = firstNonBlank(email);
        if (cleanEmail == null) {
            return false;
        }

        return studentRepository.existsByEmailIgnoreCase(cleanEmail)
                || teacherRepository.findByEmail(cleanEmail) != null
                || adminRepository.findByEmail(cleanEmail) != null
                || placementTpoRepository.existsByEmailIgnoreCase(cleanEmail);
    }

    private boolean isDuplicateMobile(String mobileNumber) {
        String cleanMobile = firstNonBlank(mobileNumber);
        if (cleanMobile == null) {
            return false;
        }

        return studentRepository.existsByMobileNumberIgnoreCase(cleanMobile);
    }

    private String registrationConflictMessage(DataIntegrityViolationException exception) {
        String message = exception.getMostSpecificCause() != null
                ? exception.getMostSpecificCause().getMessage()
                : exception.getMessage();
        String normalizedMessage = message != null ? message.toLowerCase(Locale.ENGLISH) : "";
        if (normalizedMessage.contains("mobile")) {
            return "Mobile number already exists";
        }
        if (normalizedMessage.contains("enrollment")) {
            return "Enrollment number already exists";
        }
        return "Email already exists";
    }

    private List<CollegeOption> collegeOptions() {
        return collegeRepository.findAllByOrderByNameAsc().stream()
                .filter(College::isActive)
                .filter(college -> adminRepository.findFirstByCollege_Id(college.getId()) != null)
                .map(college -> new CollegeOption(
                        college.getCode(),
                        firstNonBlank(college.getName(), "College")))
                .sorted((left, right) -> left.getDisplayName().compareToIgnoreCase(right.getDisplayName()))
                .toList();
    }

    private void populateRegistrationCollege(Model model, College college) {
        if (college == null) {
            return;
        }
        model.addAttribute("registrationCollegeCode", college.getCode());
        model.addAttribute("registrationCollegeName", firstNonBlank(college.getName(), "College"));
        model.addAttribute("registrationCollegeAddress", firstNonBlank(college.getAddress(), ""));
        model.addAttribute("registrationCollegeLogoPath", normalizePublicLogoPath(college.getLogoPath()));
    }

    private College resolveCollegeForRegistration(String collegeIdentifier) {
        String cleanIdentifier = firstNonBlank(collegeIdentifier);
        if (cleanIdentifier == null) {
            return null;
        }

        College byCode = collegeRepository.findByCodeIgnoreCase(cleanIdentifier);
        if (byCode != null) {
            return byCode;
        }

        College byName = collegeRepository.findByNameIgnoreCase(cleanIdentifier);
        if (byName != null) {
            return byName;
        }

        String normalizedIdentifier = normalizeLookupKey(cleanIdentifier);
        if (normalizedIdentifier == null) {
            return null;
        }

        College fuzzyMatch = collegeRepository.findAllByOrderByNameAsc().stream()
                .filter(College::isActive)
                .filter(college -> adminRepository.findFirstByCollege_Id(college.getId()) != null)
                .filter(college -> matchesRegistrationIdentifier(college, normalizedIdentifier))
                .findFirst()
                .orElse(null);
        if (fuzzyMatch != null) {
            return fuzzyMatch;
        }

        try {
            Long collegeId = Long.valueOf(cleanIdentifier);
            return collegeRepository.findById(collegeId).orElse(null);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean matchesRegistrationIdentifier(College college, String normalizedIdentifier) {
        if (college == null || normalizedIdentifier == null) {
            return false;
        }

        String collegeCode = normalizeLookupKey(college.getCode());
        if (collegeCode != null) {
            if (collegeCode.equals(normalizedIdentifier)
                    || collegeCode.startsWith(normalizedIdentifier)
                    || normalizedIdentifier.startsWith(collegeCode)
                    || collegeCode.contains(normalizedIdentifier)) {
                return true;
            }
        }

        String collegeName = normalizeLookupKey(college.getName());
        if (collegeName != null) {
            if (collegeName.equals(normalizedIdentifier)
                    || collegeName.startsWith(normalizedIdentifier)
                    || normalizedIdentifier.startsWith(collegeName)
                    || collegeName.contains(normalizedIdentifier)) {
                return true;
            }
        }

        String compactCollegeCode = compactLookupKey(college.getCode());
        String compactCollegeName = compactLookupKey(college.getName());
        String compactIdentifier = compactLookupKey(normalizedIdentifier);

        return (compactCollegeCode != null && compactIdentifier != null
                && (compactCollegeCode.equals(compactIdentifier)
                || compactCollegeCode.startsWith(compactIdentifier)
                || compactIdentifier.startsWith(compactCollegeCode)))
                || (compactCollegeName != null && compactIdentifier != null
                && (compactCollegeName.equals(compactIdentifier)
                || compactCollegeName.startsWith(compactIdentifier)
                || compactIdentifier.startsWith(compactCollegeName)));
    }

    private String normalizeLookupKey(String value) {
        String cleanValue = firstNonBlank(value);
        return cleanValue == null ? null : cleanValue.toLowerCase(Locale.ROOT);
    }

    private String compactLookupKey(String value) {
        String normalized = normalizeLookupKey(value);
        return normalized == null ? null : normalized.replaceAll("[^a-z0-9]+", "");
    }

    private String normalizePublicLogoPath(String logoPath) {
        String cleanPath = firstNonBlank(logoPath);
        if (cleanPath == null) {
            return null;
        }
        if (cleanPath.startsWith("http://") || cleanPath.startsWith("https://")) {
            return cleanPath;
        }
        if (cleanPath.startsWith("/")) {
            return cleanPath;
        }
        if (cleanPath.startsWith("uploads/")) {
            return "/" + cleanPath;
        }
        if (cleanPath.contains("uploads/")) {
            int uploadsIndex = cleanPath.indexOf("uploads/");
            return "/" + cleanPath.substring(uploadsIndex);
        }
        return "/" + cleanPath;
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

    private String resolveStudentCollegeCode(Student student) {
        if (student == null) {
            return null;
        }
        if (student.getAdmin() != null && student.getAdmin().getCollegeCode() != null && !student.getAdmin().getCollegeCode().isBlank()) {
            return student.getAdmin().getCollegeCode().trim();
        }
        if (student.getAdmin() != null && student.getAdmin().getCollege() != null && student.getAdmin().getCollege().getCode() != null) {
            return student.getAdmin().getCollege().getCode().trim();
        }
        return null;
    }

    private String resolveAdminCollegeCode(Admin admin) {
        if (admin == null) {
            return null;
        }
        if (admin.getCollegeCode() != null && !admin.getCollegeCode().isBlank()) {
            return admin.getCollegeCode().trim();
        }
        if (admin.getCollege() != null && admin.getCollege().getCode() != null && !admin.getCollege().getCode().isBlank()) {
            return admin.getCollege().getCode().trim();
        }
        return null;
    }

    private String resolveAdminCollegeName(Admin admin) {
        if (admin == null) {
            return null;
        }
        if (admin.getCollege() != null && admin.getCollege().getName() != null && !admin.getCollege().getName().isBlank()) {
            return admin.getCollege().getName().trim();
        }
        if (admin.getCollegeName() != null && !admin.getCollegeName().isBlank()) {
            return admin.getCollegeName().trim();
        }
        return null;
    }

    private String resolveTeacherCollegeCode(Teacher teacher) {
        if (teacher == null || teacher.getAdmin() == null) {
            return null;
        }
        return resolveAdminCollegeCode(teacher.getAdmin());
    }

    private String resolveTeacherCollegeName(Teacher teacher) {
        if (teacher == null || teacher.getAdmin() == null) {
            return null;
        }
        return resolveAdminCollegeName(teacher.getAdmin());
    }

    private String resolvePlacementTpoCollegeCode(PlacementTpo placementTpo) {
        if (placementTpo == null) {
            return null;
        }
        if (placementTpo.getCollege() != null
                && placementTpo.getCollege().getCode() != null
                && !placementTpo.getCollege().getCode().isBlank()) {
            return placementTpo.getCollege().getCode().trim();
        }
        if (placementTpo.getAdmin() != null) {
            String adminCollegeCode = resolveAdminCollegeCode(placementTpo.getAdmin());
            if (adminCollegeCode != null) {
                return adminCollegeCode;
            }
        }
        String collegeName = firstNonBlank(placementTpo.getCollegeName());
        if (collegeName == null) {
            return null;
        }
        College college = collegeRepository.findByNameIgnoreCase(collegeName);
        if (college != null && college.getCode() != null && !college.getCode().isBlank()) {
            return college.getCode().trim();
        }
        return null;
    }

    private String resolvePlacementTpoCollegeName(PlacementTpo placementTpo) {
        if (placementTpo == null) {
            return null;
        }
        if (placementTpo.getCollege() != null
                && placementTpo.getCollege().getName() != null
                && !placementTpo.getCollege().getName().isBlank()) {
            return placementTpo.getCollege().getName().trim();
        }
        if (placementTpo.getAdmin() != null) {
            String adminCollegeName = resolveAdminCollegeName(placementTpo.getAdmin());
            if (adminCollegeName != null) {
                return adminCollegeName;
            }
        }
        String collegeName = firstNonBlank(placementTpo.getCollegeName());
        if (collegeName != null) {
            return collegeName;
        }
        return null;
    }

    private String resolveStudentCollegeName(Student student) {
        if (student == null || student.getAdmin() == null) {
            return null;
        }
        if (student.getAdmin().getCollege() != null && student.getAdmin().getCollege().getName() != null) {
            return student.getAdmin().getCollege().getName().trim();
        }
        if (student.getAdmin().getCollegeName() != null && !student.getAdmin().getCollegeName().isBlank()) {
            return student.getAdmin().getCollegeName().trim();
        }
        return null;
    }

    private String resolveCollegeCodeForLogout(HttpSession session) {
        if (session == null) {
            return null;
        }

        String collegeCode = firstNonBlank((String) session.getAttribute("collegeCode"));
        if (collegeCode != null) {
            return collegeCode;
        }

        String userRole = firstNonBlank((String) session.getAttribute("userRole"));
        String loggedInUser = firstNonBlank((String) session.getAttribute("loggedInUser"));
        if (userRole == null || loggedInUser == null) {
            return null;
        }

        return switch (userRole.toUpperCase(Locale.ROOT)) {
            case "STUDENT" -> resolveStudentCollegeCode(studentRepository.findByEmail(loggedInUser));
            case "TEACHER" -> resolveTeacherCollegeCode(teacherRepository.findByEmail(loggedInUser));
            case "ADMIN" -> resolveAdminCollegeCode(adminRepository.findByEmail(loggedInUser));
            case "PLACEMENT_TPO" -> {
                List<PlacementTpo> placementTpos = placementTpoRepository.findByEmail(loggedInUser);
                yield placementTpos == null || placementTpos.isEmpty()
                        ? null
                        : resolvePlacementTpoCollegeCode(placementTpos.get(0));
            }
            default -> null;
        };
    }

    private boolean isSameCollegePortal(String requestedCollegeCode, String studentCollegeCode) {
        String cleanRequested = firstNonBlank(requestedCollegeCode);
        String cleanStudent = firstNonBlank(studentCollegeCode);
        if (cleanRequested == null || cleanStudent == null) {
            return cleanRequested == null || cleanStudent == null;
        }
        return cleanRequested.equalsIgnoreCase(cleanStudent);
    }

    private String redirectToCollegePortalOrHome(String collegeCode, String mode) {
        String cleanCollegeCode = firstNonBlank(collegeCode);
        if (cleanCollegeCode == null) {
            return "redirect:/";
        }
        String cleanMode = firstNonBlank(mode, "register");
        return "redirect:/Registration/Apply/" + cleanCollegeCode + "?mode=" + cleanMode;
    }

    public static class CollegeOption {
        private final String code;
        private final String displayName;

        public CollegeOption(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }

        public String getCode() {
            return code;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

}
