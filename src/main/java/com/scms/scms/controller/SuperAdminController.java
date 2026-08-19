package com.scms.scms.controller;

import com.scms.scms.model.Admin;
import com.scms.scms.model.College;
import com.scms.scms.model.PortalNotification;
import com.scms.scms.model.SuperAdminAuditLog;
import com.scms.scms.repository.AdminRepository;
import com.scms.scms.repository.AcademicStructureRepository;
import com.scms.scms.repository.CollegeRepository;
import com.scms.scms.repository.PortalNotificationRepository;
import com.scms.scms.repository.SuperAdminAuditLogRepository;
import com.scms.scms.service.PasswordProtectionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Controller
public class SuperAdminController {

    @Autowired private AdminRepository adminRepository;
    @Autowired private AcademicStructureRepository academicStructureRepository;
    @Autowired private CollegeRepository collegeRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PortalNotificationRepository portalNotificationRepository;
    @Autowired private SuperAdminAuditLogRepository auditLogRepository;
    @Autowired private PasswordProtectionService passwordProtectionService;

    @GetMapping("/super-admin-dashboard")
    public String dashboard(HttpSession session,
                            Model model,
                            @RequestParam(value = "q", required = false) String query,
                            @RequestParam(value = "status", required = false) String statusFilter,
                            @RequestParam(value = "sort", required = false, defaultValue = "name") String sort,
                            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
                            @RequestParam(value = "size", required = false, defaultValue = "6") int size) {
        Admin superAdmin = getLoggedSuperAdmin(session);
        if (superAdmin == null) return "redirect:/superadmin";

        List<CollegeSummary> allColleges = collegeSummaries();
        List<CollegeSummary> filtered = applyFilters(allColleges, query, statusFilter, sort);
        PageSlice<CollegeSummary> pageSlice = paginate(filtered, page, size);

        model.addAttribute("superAdminName", superAdmin.getName());
        model.addAttribute("superAdminEmail", superAdmin.getEmail());
        model.addAttribute("activeModule", "dashboard");
        model.addAttribute("query", normalize(query));
        model.addAttribute("statusFilter", normalize(statusFilter));
        model.addAttribute("sort", normalize(sort, "name"));
        model.addAttribute("collegesPage", pageSlice.items);
        model.addAttribute("pageSlice", pageSlice);
        model.addAttribute("collegeSummaries", filtered);
        model.addAttribute("totalColleges", allColleges.size());
        model.addAttribute("totalAdmins", adminSummaries().size());
        model.addAttribute("activeColleges", allColleges.stream().filter(c -> "ACTIVE".equalsIgnoreCase(c.getCollegeStatus())).count());
        model.addAttribute("inactiveColleges", allColleges.stream().filter(c -> "INACTIVE".equalsIgnoreCase(c.getCollegeStatus())).count());
        model.addAttribute("setupPendingColleges", allColleges.stream().filter(c -> "SETUP_PENDING".equalsIgnoreCase(c.getCollegeStatus())).count());
        model.addAttribute("statusOptions", List.of("All", "Active", "Inactive", "Setup Pending"));
        model.addAttribute("sortOptions", List.of("name", "status", "code"));
        model.addAttribute("chartLabels", List.of("Active", "Inactive", "Pending"));
        model.addAttribute("chartValues", List.of(
                (int) allColleges.stream().filter(c -> "ACTIVE".equalsIgnoreCase(c.getCollegeStatus())).count(),
                (int) allColleges.stream().filter(c -> "INACTIVE".equalsIgnoreCase(c.getCollegeStatus())).count(),
                (int) allColleges.stream().filter(c -> "SETUP_PENDING".equalsIgnoreCase(c.getCollegeStatus())).count()
        ));
        model.addAttribute("recentActivities", recentActivities(allColleges));
        model.addAttribute("notifications", buildNotifications(allColleges));
        return "super-admin/super-admin-dashboard";
    }

    @GetMapping("/super-admin/colleges")
    public String colleges(HttpSession session, Model model,
                           @RequestParam(value = "q", required = false) String query,
                           @RequestParam(value = "status", required = false) String statusFilter,
                           @RequestParam(value = "sort", required = false, defaultValue = "name") String sort,
                           @RequestParam(value = "page", required = false, defaultValue = "1") int page,
                           @RequestParam(value = "size", required = false, defaultValue = "10") int size) {
        Admin superAdmin = getLoggedSuperAdmin(session);
        if (superAdmin == null) return "redirect:/superadmin";

        List<CollegeSummary> filtered = applyFilters(collegeSummaries(), query, statusFilter, sort);
        PageSlice<CollegeSummary> pageSlice = paginate(filtered, page, size);
        baseModel(model, superAdmin, "colleges");
        model.addAttribute("query", normalize(query));
        model.addAttribute("statusFilter", normalize(statusFilter));
        model.addAttribute("sort", normalize(sort, "name"));
        model.addAttribute("statusOptions", List.of("All", "Active", "Inactive", "Setup Pending"));
        model.addAttribute("sortOptions", List.of("name", "status", "code"));
        model.addAttribute("pageSlice", pageSlice);
        model.addAttribute("colleges", pageSlice.items);
        model.addAttribute("filteredCount", filtered.size());
        model.addAttribute("newCollege", new College());
        return "super-admin/colleges";
    }

    @GetMapping("/super-admin/admins")
    public String admins(HttpSession session, Model model,
                         @RequestParam(value = "q", required = false) String query,
                         @RequestParam(value = "status", required = false) String statusFilter,
                         @RequestParam(value = "page", required = false, defaultValue = "1") int page,
                         @RequestParam(value = "size", required = false, defaultValue = "10") int size) {
        Admin superAdmin = getLoggedSuperAdmin(session);
        if (superAdmin == null) return "redirect:/superadmin";

        List<AdminSummary> filtered = applyAdminFilters(adminSummaries(), query, statusFilter);
        PageSlice<AdminSummary> pageSlice = paginate(filtered, page, size);
        baseModel(model, superAdmin, "admins");
        model.addAttribute("query", normalize(query));
        model.addAttribute("statusFilter", normalize(statusFilter));
        model.addAttribute("statusOptions", List.of("All", "Active", "Inactive", "Setup Pending"));
        model.addAttribute("colleges", collegeRepository.findAllByOrderByNameAsc());
        model.addAttribute("pageSlice", pageSlice);
        model.addAttribute("admins", pageSlice.items);
        model.addAttribute("filteredCount", filtered.size());
        return "super-admin/college-admins";
    }

    @GetMapping("/super-admin/communications")
    public String communications(HttpSession session, Model model) {
        Admin superAdmin = getLoggedSuperAdmin(session);
        if (superAdmin == null) return "redirect:/superadmin";

        baseModel(model, superAdmin, "communications");
        model.addAttribute("collegeAdmins", adminSummaries());
        model.addAttribute("sentMessages", communicationFeed());
        model.addAttribute("collegeSummaries", collegeSummaries());
        return "super-admin/communications";
    }

    @PostMapping("/super-admin/communications/send")
    public String sendCommunication(HttpSession session,
                                    @RequestParam String recipientEmail,
                                    @RequestParam String title,
                                    @RequestParam String message,
                                    RedirectAttributes redirectAttributes) {
        Admin superAdmin = getLoggedSuperAdmin(session);
        if (superAdmin == null) return "redirect:/superadmin";

        Admin recipient = adminRepository.findByEmail(recipientEmail == null ? null : recipientEmail.trim());
        if (recipient == null || recipient.isSuperAdmin()) {
            redirectAttributes.addFlashAttribute("superAdminError", "Select a valid college admin.");
            return "redirect:/super-admin/communications";
        }

        PortalNotification notification = new PortalNotification();
        notification.setRecipientRole("ADMIN");
        notification.setRecipientEmail(recipient.getEmail());
        notification.setTitle(blankToNull(title));
        notification.setMessage(blankToNull(message));
        notification.setCategory("SUPER_ADMIN_MESSAGE");
        notification.setSourceType("SUPER_ADMIN");
        notification.setSourceId(superAdmin.getId());
        notification.setCreatedAt(LocalDateTime.now());
        notification.setScheduledFor(LocalDateTime.now());
        portalNotificationRepository.save(notification);

        logAudit(session, "MESSAGE_SENT", "COLLEGE_ADMIN", recipient.getCollegeName(),
                "Message sent to " + recipient.getEmail());

        redirectAttributes.addFlashAttribute("superAdminSuccess", "Message sent to the college admin.");
        return "redirect:/super-admin/communications";
    }

    @GetMapping("/super-admin/contact-requests")
    public String contactRequests(HttpSession session, Model model) {
        Admin superAdmin = getLoggedSuperAdmin(session);
        if (superAdmin == null) return "redirect:/superadmin";

        baseModel(model, superAdmin, "communications");
        model.addAttribute("collegeAdmins", adminSummaries());
        model.addAttribute("messages", communicationFeed());
        return "super-admin/contact-requests";
    }

    @GetMapping("/super-admin/audit-logs")
    public String auditLogs(HttpSession session, Model model) {
        Admin superAdmin = getLoggedSuperAdmin(session);
        if (superAdmin == null) return "redirect:/superadmin";

        baseModel(model, superAdmin, "audit");
        model.addAttribute("auditLogs", auditLogRepository.findAllByOrderByCreatedAtDesc());
        return "super-admin/audit-logs";
    }

    @GetMapping("/super-admin/settings")
    public String settings(HttpSession session, Model model) {
        Admin superAdmin = getLoggedSuperAdmin(session);
        if (superAdmin == null) return "redirect:/superadmin";

        baseModel(model, superAdmin, "settings");
        model.addAttribute("settingsCards", List.of(
                new SimpleMetric("Tenant Mode", "College Wise Isolation"),
                new SimpleMetric("Security", "BCrypt + Session Controls"),
                new SimpleMetric("Communication", "Super Admin to College Admin"),
                new SimpleMetric("Status Control", "Activate / Deactivate Colleges")
        ));
        return "super-admin/settings";
    }

    @GetMapping("/super-admin/notifications")
    public String notifications(HttpSession session,
                                Model model,
                                @RequestParam(value = "notificationId", required = false) Long notificationId) {
        Admin superAdmin = getLoggedSuperAdmin(session);
        if (superAdmin == null) return "redirect:/superadmin";

        List<PortalNotification> sentMessages = portalNotificationRepository
                .findByRecipientRoleAndCategoryOrderByCreatedAtDesc("ADMIN", "SUPER_ADMIN_MESSAGE");

        PortalNotification selectedNotification = null;
        if (notificationId != null) {
            selectedNotification = sentMessages.stream()
                    .filter(notification -> notification.getId() != null && notification.getId().equals(notificationId))
                    .findFirst()
                    .orElse(null);
        }
        if (selectedNotification == null && !sentMessages.isEmpty()) {
            selectedNotification = sentMessages.get(0);
        }

        model.addAttribute("notificationCount", sentMessages.size());
        model.addAttribute("unreadCount", sentMessages.stream().filter(PortalNotification::isUnread).count());
        model.addAttribute("sentMessages", sentMessages);
        model.addAttribute("selectedNotification", selectedNotification);
        model.addAttribute("recentActivities", recentActivities(collegeSummaries()));
        baseModel(model, superAdmin, "notifications");
        return "super-admin/notifications";
    }

    @GetMapping("/super-admin/reports")
    public String reports(HttpSession session, Model model) {
        Admin superAdmin = getLoggedSuperAdmin(session);
        if (superAdmin == null) return "redirect:/superadmin";

        List<CollegeSummary> all = collegeSummaries();
        baseModel(model, superAdmin, "reports");
        model.addAttribute("colleges", all);
        model.addAttribute("reportCards", reportCards(all));
        model.addAttribute("chartLabels", List.of("Active", "Inactive", "Pending"));
        model.addAttribute("chartValues", List.of(
                (int) all.stream().filter(c -> "ACTIVE".equalsIgnoreCase(c.getCollegeStatus())).count(),
                (int) all.stream().filter(c -> "INACTIVE".equalsIgnoreCase(c.getCollegeStatus())).count(),
                (int) all.stream().filter(c -> "SETUP_PENDING".equalsIgnoreCase(c.getCollegeStatus())).count()
        ));
        return "super-admin/reports";
    }

    @PostMapping("/super-admin/colleges/create")
    public String createCollege(HttpSession session,
                                @RequestParam String name,
                                @RequestParam String code,
                                @RequestParam(value = "address", required = false) String address,
                                @RequestParam(value = "status", required = false) String status,
                                @RequestParam(value = "logo", required = false) MultipartFile logoFile,
                                RedirectAttributes redirectAttributes) {
        Admin superAdmin = getLoggedSuperAdmin(session);
        if (superAdmin == null) return "redirect:/superadmin";

        String cleanName = blankToNull(name);
        String cleanCode = blankToNull(code);
        if (cleanName == null || cleanCode == null) {
            redirectAttributes.addFlashAttribute("superAdminError", "College name and code are required.");
            return "redirect:/super-admin/colleges";
        }
        if (collegeRepository.findByNameIgnoreCase(cleanName) != null) {
            redirectAttributes.addFlashAttribute("superAdminError", "College name already exists.");
            return "redirect:/super-admin/colleges";
        }
        if (collegeRepository.findByCodeIgnoreCase(cleanCode) != null) {
            redirectAttributes.addFlashAttribute("superAdminError", "College code already exists.");
            return "redirect:/super-admin/colleges";
        }

        College college = new College();
        college.setName(cleanName);
        college.setCode(cleanCode);
        college.setAddress(blankToNull(address));
        college.setStatus("INACTIVE".equalsIgnoreCase(status) ? "INACTIVE" : "ACTIVE");
        if (logoFile != null && !logoFile.isEmpty()) {
            college.setLogoPath(saveCollegeLogo(logoFile));
        }
        collegeRepository.save(college);

        logAudit(session, "COLLEGE_CREATED", "COLLEGE", college.getName(),
                "Created college record with code " + college.getCode());
        redirectAttributes.addFlashAttribute("superAdminSuccess", "College created successfully.");
        return "redirect:/super-admin/colleges";
    }

    @GetMapping("/super-admin/colleges/{collegeId}")
    public String collegeDetails(HttpSession session, Model model, @PathVariable Long collegeId) {
        Admin superAdmin = getLoggedSuperAdmin(session);
        if (superAdmin == null) return "redirect:/superadmin";

        College college = resolveCollege(collegeId);
        if (college == null) return "redirect:/super-admin/colleges";

        baseModel(model, superAdmin, "colleges");
        model.addAttribute("college", toCollegeSummary(college));
        model.addAttribute("collegeAdmin", adminRepository.findFirstByCollege_Id(college.getId()));
        return "super-admin/college-detail";
    }

    @GetMapping("/super-admin/colleges/{collegeId}/edit")
    public String editCollege(HttpSession session, Model model, @PathVariable Long collegeId) {
        Admin superAdmin = getLoggedSuperAdmin(session);
        if (superAdmin == null) return "redirect:/superadmin";

        College college = resolveCollege(collegeId);
        if (college == null) return "redirect:/super-admin/colleges";

        baseModel(model, superAdmin, "colleges");
        model.addAttribute("college", college);
        return "super-admin/college-edit";
    }

    @PostMapping("/super-admin/colleges/{collegeId}/edit")
    public String updateCollege(HttpSession session,
                                @PathVariable Long collegeId,
                                @RequestParam String name,
                                @RequestParam String code,
                                @RequestParam(value = "address", required = false) String address,
                                @RequestParam(value = "status", required = false) String status,
                                @RequestParam(value = "logo", required = false) MultipartFile logoFile,
                                RedirectAttributes redirectAttributes) {
        Admin superAdmin = getLoggedSuperAdmin(session);
        if (superAdmin == null) return "redirect:/superadmin";

        College college = resolveCollege(collegeId);
        if (college == null) {
            redirectAttributes.addFlashAttribute("superAdminError", "College not found.");
            return "redirect:/super-admin/colleges";
        }

        String cleanName = blankToNull(name);
        String cleanCode = blankToNull(code);
        if (cleanName == null || cleanCode == null) {
            redirectAttributes.addFlashAttribute("superAdminError", "All fields are required.");
            return "redirect:/super-admin/colleges/" + collegeId + "/edit";
        }

        College codeOwner = collegeRepository.findByCodeIgnoreCase(cleanCode);
        if (codeOwner != null && !Objects.equals(codeOwner.getId(), collegeId)) {
            redirectAttributes.addFlashAttribute("superAdminError", "College code already exists.");
            return "redirect:/super-admin/colleges/" + collegeId + "/edit";
        }

        college.setName(cleanName);
        college.setCode(cleanCode);
        college.setAddress(blankToNull(address));
        college.setStatus("INACTIVE".equalsIgnoreCase(status) ? "INACTIVE" : "ACTIVE");
        if (logoFile != null && !logoFile.isEmpty()) {
            college.setLogoPath(saveCollegeLogo(logoFile));
        }
        collegeRepository.save(college);
        syncLinkedAdminFromCollege(college);
        logAudit(session, "COLLEGE_UPDATED", "COLLEGE", college.getName(),
                "Updated college profile");
        redirectAttributes.addFlashAttribute("superAdminSuccess", "College updated successfully.");
        return "redirect:/super-admin/colleges/" + collegeId;
    }

    @PostMapping("/super-admin/colleges/{collegeId}/status")
    public String toggleCollegeStatus(HttpSession session,
                                      @PathVariable Long collegeId,
                                      @RequestParam String status,
                                      RedirectAttributes redirectAttributes) {
        Admin superAdmin = getLoggedSuperAdmin(session);
        if (superAdmin == null) return "redirect:/superadmin";

        College college = resolveCollege(collegeId);
        if (college == null) {
            redirectAttributes.addFlashAttribute("superAdminError", "College not found.");
            return "redirect:/super-admin/colleges";
        }

        String normalized = "INACTIVE".equalsIgnoreCase(status) ? "INACTIVE" : "ACTIVE";
        college.setStatus(normalized);
        collegeRepository.save(college);
        syncLinkedAdminFromCollege(college);
        logAudit(session, "COLLEGE_STATUS_CHANGED", "COLLEGE", college.getName(),
                "Status changed to " + normalized);
        redirectAttributes.addFlashAttribute("superAdminSuccess", "College status updated.");
        return "redirect:/super-admin/colleges/" + collegeId;
    }

    @PostMapping("/super-admin/colleges/{collegeId}/delete")
    @Transactional
    public String deleteCollege(HttpSession session,
                                @PathVariable Long collegeId,
                                RedirectAttributes redirectAttributes) {
        Admin superAdmin = getLoggedSuperAdmin(session);
        if (superAdmin == null) return "redirect:/superadmin";

        College college = resolveCollege(collegeId);
        if (college == null) {
            redirectAttributes.addFlashAttribute("superAdminError", "College not found.");
            return "redirect:/super-admin/colleges";
        }

        int deletedAdmins = deleteCollegeAdmins(collegeId);
        collegeRepository.delete(college);
        logAudit(session, "COLLEGE_DELETED", "COLLEGE", college.getName(),
                "Deleted college record and " + deletedAdmins + " linked admin account(s)");
        redirectAttributes.addFlashAttribute("superAdminSuccess", "College removed from the control center.");
        return "redirect:/super-admin/colleges";
    }

    @GetMapping("/super-admin/colleges/{collegeId}/manage")
    public String manageCollege(HttpSession session, Model model, @PathVariable Long collegeId) {
        Admin superAdmin = getLoggedSuperAdmin(session);
        if (superAdmin == null) return "redirect:/superadmin";

        College college = resolveCollege(collegeId);
        if (college == null) return "redirect:/super-admin/colleges";

        baseModel(model, superAdmin, "colleges");
        CollegeSummary summary = toCollegeSummary(college);
        model.addAttribute("college", summary);
        model.addAttribute("collegeAdmin", adminRepository.findFirstByCollege_Id(college.getId()));
        model.addAttribute("recentActivities", recentActivities(List.of(summary)));
        return "super-admin/college-manage";
    }

    @PostMapping("/super-admin/admins/create")
    public String createCollegeAdmin(HttpSession session,
                                     @RequestParam String name,
                                     @RequestParam String email,
                                     @RequestParam String password,
                                     @RequestParam Long collegeId,
                                     @RequestParam(value = "phone", required = false) String phone,
                                     RedirectAttributes redirectAttributes) {
        Admin superAdmin = getLoggedSuperAdmin(session);
        if (superAdmin == null) return "redirect:/superadmin";

        String cleanName = blankToNull(name);
        String cleanEmail = blankToNull(email);
        String cleanPassword = blankToNull(password);
        College college = resolveCollege(collegeId);

        if (cleanName == null || cleanEmail == null || cleanPassword == null || college == null) {
            redirectAttributes.addFlashAttribute("superAdminError", "All college admin fields are required.");
            return "redirect:/super-admin/admins";
        }
        if (adminRepository.findByEmail(cleanEmail) != null) {
            redirectAttributes.addFlashAttribute("superAdminError", "Email already exists.");
            return "redirect:/super-admin/admins";
        }
        if (adminRepository.findFirstByCollege_Id(college.getId()) != null) {
            redirectAttributes.addFlashAttribute("superAdminError", "This college already has an admin.");
            return "redirect:/super-admin/admins";
        }

        Admin collegeAdmin = new Admin();
        collegeAdmin.setName(cleanName);
        collegeAdmin.setEmail(cleanEmail);
        collegeAdmin.setPassword(passwordProtectionService.encode(cleanPassword));
        collegeAdmin.setCollege(college);
        collegeAdmin.setCollegeName(college.getName());
        collegeAdmin.setCollegeCode(college.getCode());
        collegeAdmin.setCollegeStatus(college.getStatus());
        collegeAdmin.setPhone(phone == null ? null : phone.trim());
        collegeAdmin.setRole("ADMIN");
        adminRepository.save(collegeAdmin);
        logAudit(session, "COLLEGE_ADMIN_CREATED", "COLLEGE_ADMIN", collegeAdmin.getCollegeName(), "Created college admin account");
        redirectAttributes.addFlashAttribute("superAdminSuccess", "College admin created successfully.");
        return "redirect:/super-admin/admins";
    }

    @PostMapping("/super-admin/admins/{adminId}/delete")
    @Transactional
    public String deleteCollegeAdmin(HttpSession session,
                                     @PathVariable Long adminId,
                                     RedirectAttributes redirectAttributes) {
        Admin superAdmin = getLoggedSuperAdmin(session);
        if (superAdmin == null) return "redirect:/superadmin";

        Admin collegeAdmin = resolveCollegeAdmin(adminId);
        if (collegeAdmin == null) {
            redirectAttributes.addFlashAttribute("superAdminError", "Admin not found.");
            return "redirect:/super-admin/admins";
        }

        String adminName = blankToDash(collegeAdmin.getName());
        String adminEmail = blankToDash(collegeAdmin.getEmail());
        String collegeName = blankToDash(collegeAdmin.getCollegeName());

        deleteAdminScopedData(collegeAdmin);
        adminRepository.delete(collegeAdmin);
        logAudit(session, "COLLEGE_ADMIN_DELETED", "COLLEGE_ADMIN", collegeName,
                "Deleted admin " + adminName + " (" + adminEmail + ")");
        redirectAttributes.addFlashAttribute("superAdminSuccess", "College admin deleted successfully.");
        return "redirect:/super-admin/admins";
    }

    @PostMapping("/super-admin/admins/{adminId}/status")
    public String toggleAdminStatus(HttpSession session,
                                    @PathVariable Long adminId,
                                    @RequestParam String status,
                                    RedirectAttributes redirectAttributes) {
        Admin superAdmin = getLoggedSuperAdmin(session);
        if (superAdmin == null) return "redirect:/superadmin";

        Admin collegeAdmin = resolveCollegeAdmin(adminId);
        if (collegeAdmin == null) {
            redirectAttributes.addFlashAttribute("superAdminError", "Admin not found.");
            return "redirect:/super-admin/admins";
        }

        collegeAdmin.setCollegeStatus("INACTIVE".equalsIgnoreCase(status) ? "INACTIVE" : "ACTIVE");
        adminRepository.save(collegeAdmin);
        logAudit(session, "COLLEGE_ADMIN_STATUS", "COLLEGE_ADMIN", collegeAdmin.getCollegeName(),
                "Status changed to " + collegeAdmin.getCollegeStatus());
        redirectAttributes.addFlashAttribute("superAdminSuccess", "College admin status updated.");
        return "redirect:/super-admin/admins";
    }

    @PostMapping("/super-admin/admins/message")
    public String messageAdmin(HttpSession session,
                               @RequestParam String recipientEmail,
                               @RequestParam String title,
                               @RequestParam String message,
                               RedirectAttributes redirectAttributes) {
        Admin superAdmin = getLoggedSuperAdmin(session);
        if (superAdmin == null) return "redirect:/superadmin";

        Admin recipient = adminRepository.findByEmail(recipientEmail == null ? null : recipientEmail.trim());
        if (recipient == null || recipient.isSuperAdmin()) {
            redirectAttributes.addFlashAttribute("superAdminError", "Select a valid college admin.");
            return "redirect:/super-admin/communications";
        }

        PortalNotification notification = new PortalNotification();
        notification.setRecipientRole("ADMIN");
        notification.setRecipientEmail(recipient.getEmail());
        notification.setTitle(blankToNull(title));
        notification.setMessage(blankToNull(message));
        notification.setCategory("SUPER_ADMIN_MESSAGE");
        notification.setSourceType("SUPER_ADMIN");
        notification.setSourceId(superAdmin.getId());
        notification.setCreatedAt(LocalDateTime.now());
        notification.setScheduledFor(LocalDateTime.now());
        portalNotificationRepository.save(notification);

        logAudit(session, "MESSAGE_SENT", "COLLEGE_ADMIN", recipient.getCollegeName(),
                "Message sent to " + recipient.getEmail());

        redirectAttributes.addFlashAttribute("superAdminSuccess", "Message sent to the college admin.");
        return "redirect:/super-admin/communications";
    }

    private void baseModel(Model model, Admin superAdmin, String activeModule) {
        model.addAttribute("superAdminName", superAdmin.getName());
        model.addAttribute("superAdminEmail", superAdmin.getEmail());
        model.addAttribute("activeModule", activeModule);
    }

    private Admin getLoggedSuperAdmin(HttpSession session) {
        String email = (String) session.getAttribute("loggedInUser");
        String role = (String) session.getAttribute("userRole");
        if (email == null || role == null || !"SUPER_ADMIN".equalsIgnoreCase(role)) return null;
        Admin admin = adminRepository.findByEmail(email);
        return admin != null && admin.isSuperAdmin() ? admin : null;
    }

    private Admin resolveCollegeAdmin(Long adminId) {
        if (adminId == null) return null;
        Admin admin = adminRepository.findById(adminId).orElse(null);
        return admin != null && !admin.isSuperAdmin() ? admin : null;
    }

    private College resolveCollege(Long collegeId) {
        if (collegeId == null) return null;
        return collegeRepository.findById(collegeId).orElse(null);
    }

    private void syncLinkedAdminFromCollege(College college) {
        if (college == null) {
            return;
        }
        Admin linkedAdmin = adminRepository.findFirstByCollege_Id(college.getId());
        if (linkedAdmin == null) {
            return;
        }
        linkedAdmin.setCollege(college);
        linkedAdmin.setCollegeName(college.getName());
        linkedAdmin.setCollegeCode(college.getCode());
        linkedAdmin.setCollegeStatus(college.getStatus());
        adminRepository.save(linkedAdmin);
    }

    private int deleteCollegeAdmins(Long collegeId) {
        List<Admin> linkedAdmins = adminRepository.findByCollege_Id(collegeId);
        int deleted = 0;
        for (Admin linkedAdmin : linkedAdmins) {
            if (linkedAdmin == null) {
                continue;
            }
            deleteAdminScopedData(linkedAdmin);
            adminRepository.delete(linkedAdmin);
            deleted++;
        }
        return deleted;
    }

    private void deleteAdminScopedData(Admin admin) {
        if (admin == null || admin.getId() == null) {
            return;
        }

        Long adminId = admin.getId();

        // Remove child rows first, then the tenant-owned parents.
        jdbcTemplate.update("""
                delete ea
                from exam_attendance ea
                join exam_session es on ea.exam_id = es.id
                where es.admin_id = ?
                """, adminId);

        jdbcTemplate.update("""
                delete tea
                from teacher_attendance_entry tea
                join teacher_attendance_session tas on tea.session_id = tas.id
                join teacher t on tas.teacher_id = t.id
                where t.admin_id = ?
                """, adminId);

        jdbcTemplate.update("""
                delete ca
                from campus_event_application ca
                join student s on ca.student_id = s.id
                where s.admin_id = ?
                """, adminId);

        jdbcTemplate.update("""
                delete pa
                from placement_application pa
                join student s on pa.student_id = s.id
                where s.admin_id = ?
                """, adminId);

        jdbcTemplate.update("""
                delete pis
                from placement_interview_student pis
                join student s on pis.student_id = s.id
                where s.admin_id = ?
                """, adminId);

        jdbcTemplate.update("""
                delete ca
                from class_assignment ca
                join teacher t on ca.teacher_id = t.id
                where t.admin_id = ?
                """, adminId);

        jdbcTemplate.update("""
                delete ce
                from campus_event ce
                join teacher t on ce.teacher_id = t.id
                where t.admin_id = ?
                """, adminId);

        jdbcTemplate.update("delete from exam_session where admin_id = ?", adminId);
        jdbcTemplate.update("""
                delete tas
                from teacher_attendance_session tas
                join teacher t on tas.teacher_id = t.id
                where t.admin_id = ?
                """, adminId);

        jdbcTemplate.update("delete from study_material where admin_id = ?", adminId);
        jdbcTemplate.update("delete from timetable where admin_id = ?", adminId);
        jdbcTemplate.update("delete from fees where admin_id = ?", adminId);
        jdbcTemplate.update("delete from student where admin_id = ?", adminId);
        jdbcTemplate.update("delete from academic_structure where admin_id = ?", adminId);
        jdbcTemplate.update("delete from subject where admin_id = ?", adminId);
        jdbcTemplate.update("delete from subject_master where admin_id = ?", adminId);
        jdbcTemplate.update("delete from batch where admin_id = ?", adminId);
        jdbcTemplate.update("delete from course where admin_id = ?", adminId);
        jdbcTemplate.update("update teacher set class_room_id = null where admin_id = ?", adminId);
        jdbcTemplate.update("delete from teacher where admin_id = ?", adminId);
        jdbcTemplate.update("delete from classroom where admin_id = ?", adminId);
    }

    private String saveCollegeLogo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String contentType = file.getContentType();
        if (contentType == null || !(contentType.equals("image/jpeg")
                || contentType.equals("image/png")
                || contentType.equals("image/webp")
                || contentType.equals("image/gif"))) {
            return null;
        }

        if (file.getSize() > 2 * 1024 * 1024) {
            return null;
        }

        try {
            Path uploadDir = Paths.get("uploads", "colleges");
            Files.createDirectories(uploadDir);
            String original = file.getOriginalFilename();
            String ext = (original != null && original.contains("."))
                    ? original.substring(original.lastIndexOf('.'))
                    : ".jpg";
            String fileName = UUID.randomUUID() + ext;
            Files.copy(file.getInputStream(), uploadDir.resolve(fileName));
            return "/uploads/colleges/" + fileName;
        } catch (IOException ex) {
            return null;
        }
    }

    private List<CollegeSummary> collegeSummaries() {
        return collegeRepository.findAllByOrderByNameAsc().stream()
                .filter(Objects::nonNull)
                .map(this::toCollegeSummary)
                .sorted(Comparator.comparing(CollegeSummary::getCollegeName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    private List<AdminSummary> adminSummaries() {
        return adminRepository.findAll().stream()
                .filter(Objects::nonNull)
                .filter(admin -> !admin.isSuperAdmin())
                .map(this::toAdminSummary)
                .sorted(Comparator.comparing(AdminSummary::getCollegeName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
    }

    private CollegeSummary toCollegeSummary(College college) {
        Admin linkedAdmin = college == null ? null : adminRepository.findFirstByCollege_Id(college.getId());
        return new CollegeSummary(
                college == null ? null : college.getId(),
                blankToDash(college == null ? null : college.getName()),
                blankToDash(college == null ? null : college.getCode()),
                blankToDash(college == null ? null : college.getAddress()),
                blankToDash(college == null ? null : college.getLogoPath()),
                blankToDash(college == null ? null : college.getStatus()).toUpperCase(Locale.ENGLISH),
                statusTone(college == null ? null : college.getStatus()),
                linkedAdmin == null ? null : linkedAdmin.getId(),
                blankToDash(linkedAdmin == null ? null : linkedAdmin.getName()),
                blankToDash(linkedAdmin == null ? null : linkedAdmin.getEmail()),
                linkedAdmin == null
                        ? "No admin created yet"
                        : "College admin: " + blankToDash(linkedAdmin.getName()) + " Ã‚Â -  " + blankToDash(linkedAdmin.getEmail())
        );
    }

    private CollegeSummary toCollegeSummary(Admin admin) {
        College college = admin == null ? null : admin.getCollege();
        if (college != null) {
            return toCollegeSummary(college);
        }
        return new CollegeSummary(
                admin == null ? null : admin.getId(),
                blankToDash(admin == null ? null : admin.getCollegeName()),
                blankToDash(admin == null ? null : admin.getCollegeCode()),
                blankToDash(admin == null ? null : null),
                blankToDash(admin == null ? null : null),
                blankToDash(admin == null ? null : admin.getCollegeStatus()).toUpperCase(Locale.ENGLISH),
                statusTone(admin == null ? null : admin.getCollegeStatus()),
                admin == null ? null : admin.getId(),
                blankToDash(admin == null ? null : admin.getName()),
                blankToDash(admin == null ? null : admin.getEmail()),
                admin == null
                        ? "No admin created yet"
                        : "College admin: " + blankToDash(admin.getName()) + "  -  " + blankToDash(admin.getEmail())
        );
    }

    private AdminSummary toAdminSummary(Admin admin) {
        return new AdminSummary(
                admin.getId(),
                blankToDash(admin.getName()),
                blankToDash(admin.getEmail()),
                admin.getCollege() == null ? null : admin.getCollege().getId(),
                blankToDash(admin.getCollegeName()),
                blankToDash(admin.getCollegeCode()),
                blankToDash(admin.getCollege() == null ? null : admin.getCollege().getAddress()),
                blankToDash(admin.getCollegeStatus()).toUpperCase(Locale.ENGLISH),
                statusTone(admin.getCollegeStatus())
        );
    }

    private String statusTone(String status) {
        if ("ACTIVE".equalsIgnoreCase(status)) return "success";
        if ("INACTIVE".equalsIgnoreCase(status)) return "danger";
        return "warning";
    }

    private List<CollegeSummary> applyFilters(List<CollegeSummary> colleges, String query, String statusFilter, String sort) {
        String q = normalize(query);
        String status = normalize(statusFilter);
        String sortKey = normalize(sort, "name").toLowerCase(Locale.ENGLISH);

        List<CollegeSummary> filtered = colleges.stream()
                .filter(college -> q == null || q.isBlank()
                        || containsIgnoreCase(college.getCollegeName(), q)
                        || containsIgnoreCase(college.getCollegeCode(), q)
                        || containsIgnoreCase(college.getAdminName(), q))
                .filter(college -> status == null || status.isBlank() || "all".equalsIgnoreCase(status)
                        || containsIgnoreCase(college.getStatus(), status))
                .collect(Collectors.toList());

        Comparator<CollegeSummary> comparator = switch (sortKey) {
            case "status" -> Comparator.comparing(CollegeSummary::getStatus, String.CASE_INSENSITIVE_ORDER);
            case "code" -> Comparator.comparing(CollegeSummary::getCollegeCode, String.CASE_INSENSITIVE_ORDER);
            default -> Comparator.comparing(CollegeSummary::getCollegeName, String.CASE_INSENSITIVE_ORDER);
        };
        return filtered.stream().sorted(comparator).collect(Collectors.toList());
    }

    private List<AdminSummary> applyAdminFilters(List<AdminSummary> admins, String query, String statusFilter) {
        String q = normalize(query);
        String status = normalize(statusFilter);
        return admins.stream()
                .filter(admin -> q == null || q.isBlank()
                        || containsIgnoreCase(admin.getAdminName(), q)
                        || containsIgnoreCase(admin.getEmail(), q)
                        || containsIgnoreCase(admin.getCollegeName(), q)
                        || containsIgnoreCase(admin.getCollegeCode(), q))
                .filter(admin -> status == null || status.isBlank() || "all".equalsIgnoreCase(status)
                        || containsIgnoreCase(admin.getStatus(), status))
                .collect(Collectors.toList());
    }

    private PageSlice paginate(List<?> items, int page, int size) {
        int safeSize = Math.max(1, size);
        int totalItems = items.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / safeSize));
        int safePage = Math.min(Math.max(1, page), totalPages);
        int fromIndex = Math.min((safePage - 1) * safeSize, totalItems);
        int toIndex = Math.min(fromIndex + safeSize, totalItems);
        List<?> slice = items.subList(fromIndex, toIndex);
        List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages).boxed().collect(Collectors.toList());
        return new PageSlice(slice, safePage, totalPages, totalItems, safePage > 1, safePage < totalPages, pageNumbers, safeSize);
    }

    private List<ActivityItem> recentActivities(List<CollegeSummary> colleges) {
        List<ActivityItem> items = new ArrayList<>();
        colleges.stream().limit(5).forEach(college ->
                items.add(new ActivityItem(
                        college.getCollegeName(),
                        "Admin: " + college.getAdminName() + " Â -  " + college.getLatestSummary(),
                        statusTone(college.getStatus()),
                        "fa-school"
                )));
        if (items.isEmpty()) {
            items.add(new ActivityItem("No activity yet", "Create a college admin to start onboarding colleges.", "warning", "fa-circle-info"));
        }
        return items;
    }

    private List<ActivityItem> buildNotifications(List<CollegeSummary> colleges) {
        List<ActivityItem> items = new ArrayList<>();
        items.add(new ActivityItem("Platform heartbeat", "Only college and admin records are managed here.", "info", "fa-bell"));
        items.add(new ActivityItem("College coverage", colleges.size() + " colleges linked to the ERP control center.", "success", "fa-school"));
        colleges.stream().limit(4).forEach(college ->
                items.add(new ActivityItem(college.getCollegeName(), "Admin: " + college.getAdminName() + " Â -  " + college.getStatus(), statusTone(college.getStatus()), "fa-circle-dot")));
        return items;
    }

    private List<SimpleMetric> reportCards(List<CollegeSummary> colleges) {
        return List.of(
                new SimpleMetric("Active Colleges", String.valueOf(colleges.stream().filter(c -> "ACTIVE".equalsIgnoreCase(c.getStatus())).count())),
                new SimpleMetric("Inactive Colleges", String.valueOf(colleges.stream().filter(c -> "INACTIVE".equalsIgnoreCase(c.getStatus())).count())),
                new SimpleMetric("Pending Setup", String.valueOf(colleges.stream().filter(c -> "SETUP_PENDING".equalsIgnoreCase(c.getStatus())).count())),
                new SimpleMetric("College Admins", String.valueOf(colleges.size()))
        );
    }

    private List<CommunicationItem> communicationFeed() {
        return portalNotificationRepository.findAll().stream()
                .filter(notification -> notification != null && "SUPER_ADMIN_MESSAGE".equalsIgnoreCase(notification.getCategory()))
                .sorted(Comparator.comparing(PortalNotification::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .limit(20)
                .map(this::toCommunicationItem)
                .collect(Collectors.toList());
    }

    private CommunicationItem toCommunicationItem(PortalNotification notification) {
        return new CommunicationItem(
                blankToDash(notification.getRecipientEmail()),
                blankToDash(notification.getTitle()),
                blankToDash(notification.getMessage()),
                blankToDash(notification.getCreatedAtLabel()),
                "success"
        );
    }

    private void logAudit(HttpSession session, String actionType, String entityType, String entityName, String details) {
        Admin superAdmin = getLoggedSuperAdmin(session);
        if (superAdmin == null) return;
        SuperAdminAuditLog log = new SuperAdminAuditLog();
        log.setActorEmail(superAdmin.getEmail());
        log.setActionType(actionType);
        log.setEntityType(entityType);
        log.setEntityName(entityName);
        log.setDetails(details);
        log.setCreatedAt(LocalDateTime.now());
        auditLogRepository.save(log);
    }

    private boolean containsIgnoreCase(String value, String query) {
        return value != null && query != null && value.toLowerCase(Locale.ENGLISH).contains(query.toLowerCase(Locale.ENGLISH));
    }

    private String blankToDash(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalize(String value) {
        return normalize(value, null);
    }

    private String normalize(String value, String fallback) {
        String normalized = blankToNull(value);
        return normalized == null ? fallback : normalized;
    }

    public static class CollegeSummary {
        private final Long collegeId;
        private final String collegeName;
        private final String collegeCode;
        private final String address;
        private final String logoPath;
        private final String status;
        private final String statusTone;
        private final Long adminId;
        private final String adminName;
        private final String adminEmail;
        private final String latestSummary;

        public CollegeSummary(Long collegeId, String collegeName, String collegeCode, String address, String logoPath, String status, String statusTone, Long adminId, String adminName, String adminEmail, String latestSummary) {
            this.collegeId = collegeId;
            this.collegeName = collegeName;
            this.collegeCode = collegeCode;
            this.address = address;
            this.logoPath = logoPath;
            this.status = status;
            this.statusTone = statusTone;
            this.adminId = adminId;
            this.adminName = adminName;
            this.adminEmail = adminEmail;
            this.latestSummary = latestSummary;
        }

        public Long getCollegeId() { return collegeId; }
        public String getCollegeName() { return collegeName; }
        public String getCollegeCode() { return collegeCode; }
        public String getAddress() { return address; }
        public String getLogoPath() { return logoPath; }
        public String getAdminName() { return adminName; }
        public String getAdminEmail() { return adminEmail; }
        public String getStatus() { return status; }
        public String getCollegeStatus() { return status; }
        public String getStatusTone() { return statusTone; }
        public Long getAdminId() { return adminId; }
        public String getLatestSummary() { return latestSummary; }
    }

    public static class AdminSummary {
        private final Long adminId;
        private final String adminName;
        private final String email;
        private final Long collegeId;
        private final String collegeName;
        private final String collegeCode;
        private final String collegeAddress;
        private final String status;
        private final String statusTone;

        public AdminSummary(Long adminId, String adminName, String email, Long collegeId, String collegeName, String collegeCode, String collegeAddress, String status, String statusTone) {
            this.adminId = adminId;
            this.adminName = adminName;
            this.email = email;
            this.collegeId = collegeId;
            this.collegeName = collegeName;
            this.collegeCode = collegeCode;
            this.collegeAddress = collegeAddress;
            this.status = status;
            this.statusTone = statusTone;
        }

        public Long getAdminId() { return adminId; }
        public String getAdminName() { return adminName; }
        public String getEmail() { return email; }
        public Long getCollegeId() { return collegeId; }
        public String getCollegeName() { return collegeName; }
        public String getCollegeCode() { return collegeCode; }
        public String getCollegeAddress() { return collegeAddress; }
        public String getStatus() { return status; }
        public String getStatusTone() { return statusTone; }
    }

    public static class CommunicationItem {
        private final String recipient;
        private final String title;
        private final String message;
        private final String createdAt;
        private final String tone;

        public CommunicationItem(String recipient, String title, String message, String createdAt, String tone) {
            this.recipient = recipient;
            this.title = title;
            this.message = message;
            this.createdAt = createdAt;
            this.tone = tone;
        }

        public String getRecipient() { return recipient; }
        public String getTitle() { return title; }
        public String getMessage() { return message; }
        public String getCreatedAt() { return createdAt; }
        public String getTone() { return tone; }
    }

    public static class ActivityItem {
        private final String title;
        private final String subtitle;
        private final String tone;
        private final String icon;

        public ActivityItem(String title, String subtitle, String tone, String icon) {
            this.title = title;
            this.subtitle = subtitle;
            this.tone = tone;
            this.icon = icon;
        }

        public String getTitle() { return title; }
        public String getSubtitle() { return subtitle; }
        public String getTone() { return tone; }
        public String getIcon() { return icon; }
    }

    public static class PageSlice<T> {
        private final List<T> items;
        private final int currentPage;
        private final int totalPages;
        private final int totalItems;
        private final boolean hasPrev;
        private final boolean hasNext;
        private final List<Integer> pageNumbers;
        private final int size;

        public PageSlice(List<T> items, int currentPage, int totalPages, int totalItems, boolean hasPrev, boolean hasNext, List<Integer> pageNumbers, int size) {
            this.items = items;
            this.currentPage = currentPage;
            this.totalPages = totalPages;
            this.totalItems = totalItems;
            this.hasPrev = hasPrev;
            this.hasNext = hasNext;
            this.pageNumbers = pageNumbers;
            this.size = size;
        }

        public List<T> getItems() { return items; }
        public int getCurrentPage() { return currentPage; }
        public int getTotalPages() { return totalPages; }
        public int getTotalItems() { return totalItems; }
        public boolean isHasPrev() { return hasPrev; }
        public boolean isHasNext() { return hasNext; }
        public List<Integer> getPageNumbers() { return pageNumbers; }
        public int getSize() { return size; }
    }

    public static class SimpleMetric {
        private final String label;
        private final String value;

        public SimpleMetric(String label, String value) {
            this.label = label;
            this.value = value;
        }

        public String getLabel() { return label; }
        public String getValue() { return value; }
    }
}


