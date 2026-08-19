package com.scms.scms.config;

import com.scms.scms.model.Admin;
import com.scms.scms.repository.AdminRepository;
import com.scms.scms.service.PasswordProtectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
@Order(4)
public class SuperAdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminBootstrapRunner.class);
    private static final String SUPER_ADMIN_EMAIL = "superadmin@scms.local";
    private static final String SUPER_ADMIN_PASSWORD = "Admin@123";

    private final AdminRepository adminRepository;
    private final PasswordProtectionService passwordProtectionService;

    public SuperAdminBootstrapRunner(AdminRepository adminRepository,
                                     PasswordProtectionService passwordProtectionService) {
        this.adminRepository = adminRepository;
        this.passwordProtectionService = passwordProtectionService;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedSuperAdmin();
        backfillCollegeCodes();
    }

    private void seedSuperAdmin() {
        Optional<Admin> existing = adminRepository.findByRoleIgnoreCase("SUPER_ADMIN").stream().findFirst();
        if (existing.isPresent()) {
            return;
        }

        Admin superAdmin = new Admin();
        superAdmin.setName("Super Admin");
        superAdmin.setEmail(SUPER_ADMIN_EMAIL);
        superAdmin.setPassword(passwordProtectionService.encode(SUPER_ADMIN_PASSWORD));
        superAdmin.setCollegeName("System Administration");
        superAdmin.setCollegeCode("SUPER-ADMIN");
        superAdmin.setCollegeStatus("ACTIVE");
        superAdmin.setRole("SUPER_ADMIN");
        adminRepository.save(superAdmin);

        log.info("Seeded default super admin account {} with password {}.", SUPER_ADMIN_EMAIL, SUPER_ADMIN_PASSWORD);
    }

    private void backfillCollegeCodes() {
        List<Admin> admins = adminRepository.findAll().stream()
                .filter(admin -> admin != null && !admin.isSuperAdmin())
                .toList();

        boolean updated = false;
        for (Admin admin : admins) {
            if (hasText(admin.getCollegeCode())) {
                if (!hasText(admin.getCollegeStatus())) {
                    admin.setCollegeStatus("ACTIVE");
                    adminRepository.save(admin);
                }
                continue;
            }
            admin.setCollegeCode(generateCollegeCode(admin));
            admin.setCollegeStatus("ACTIVE");
            adminRepository.save(admin);
            updated = true;
        }

        if (updated) {
            log.info("Backfilled college codes for existing college admins.");
        }
    }

    private String generateCollegeCode(Admin admin) {
        String base = firstNonBlank(admin.getCollegeName(), admin.getName(), "COLLEGE");
        base = base.toUpperCase(Locale.ENGLISH).replaceAll("[^A-Z0-9]+", "-");
        base = base.replaceAll("(^-+|-+$)", "");
        if (base.isBlank()) {
            base = "COLLEGE";
        }
        return base + "-" + admin.getId();
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
