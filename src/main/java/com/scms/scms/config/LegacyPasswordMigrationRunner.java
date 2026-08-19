package com.scms.scms.config;

import com.scms.scms.repository.AdminRepository;
import com.scms.scms.repository.StudentRepository;
import com.scms.scms.repository.TeacherRepository;
import com.scms.scms.service.PasswordProtectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(3)
public class LegacyPasswordMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LegacyPasswordMigrationRunner.class);

    private final JdbcTemplate jdbcTemplate;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final AdminRepository adminRepository;
    private final PasswordProtectionService passwordProtectionService;

    public LegacyPasswordMigrationRunner(
            JdbcTemplate jdbcTemplate,
            StudentRepository studentRepository,
            TeacherRepository teacherRepository,
            AdminRepository adminRepository,
            PasswordProtectionService passwordProtectionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.adminRepository = adminRepository;
        this.passwordProtectionService = passwordProtectionService;
    }

    @Override
    public void run(ApplicationArguments args) {
        migrateStudents();
        migrateTeachers();
        migrateAdmins();
    }

    private void migrateStudents() {
        List<Long> ids = jdbcTemplate.queryForList("""
                select id from student
                where coalesce(password, '') <> ''
                  and password not like '$2%'
                """, Long.class);

        ids.forEach(id -> studentRepository.findById(id).ifPresent(student -> {
            student.setPassword(passwordProtectionService.encode(student.getPassword()));
            studentRepository.save(student);
        }));

        if (!ids.isEmpty()) {
            log.info("Migrated {} legacy student password(s) to BCrypt.", ids.size());
        }
    }

    private void migrateTeachers() {
        List<Long> ids = jdbcTemplate.queryForList("""
                select id from teacher
                where coalesce(password, '') <> ''
                  and password not like '$2%'
                """, Long.class);

        ids.forEach(id -> teacherRepository.findById(id).ifPresent(teacher -> {
            teacher.setPassword(passwordProtectionService.encode(teacher.getPassword()));
            teacherRepository.save(teacher);
        }));

        if (!ids.isEmpty()) {
            log.info("Migrated {} legacy teacher password(s) to BCrypt.", ids.size());
        }
    }

    private void migrateAdmins() {
        List<Long> ids = jdbcTemplate.queryForList("""
                select id from admin
                where coalesce(password, '') <> ''
                  and password not like '$2%'
                """, Long.class);

        ids.forEach(id -> adminRepository.findById(id).ifPresent(admin -> {
            admin.setPassword(passwordProtectionService.encode(admin.getPassword()));
            adminRepository.save(admin);
        }));

        if (!ids.isEmpty()) {
            log.info("Migrated {} legacy admin password(s) to BCrypt.", ids.size());
        }
    }
}
