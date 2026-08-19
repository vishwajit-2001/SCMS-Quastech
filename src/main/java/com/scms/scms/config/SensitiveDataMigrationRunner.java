package com.scms.scms.config;

import com.scms.scms.repository.AdminRepository;
import com.scms.scms.repository.StudentRepository;
import com.scms.scms.repository.TeacherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(1)
public class SensitiveDataMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SensitiveDataMigrationRunner.class);
    private static final String ENCRYPTED_PATTERN = "ENC::%";

    private final JdbcTemplate jdbcTemplate;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final AdminRepository adminRepository;

    public SensitiveDataMigrationRunner(
            JdbcTemplate jdbcTemplate,
            StudentRepository studentRepository,
            TeacherRepository teacherRepository,
            AdminRepository adminRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.adminRepository = adminRepository;
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
                where (coalesce(aadhar_number, '') <> '' and aadhar_number not like ?)
                   or (coalesce(pan_card_number, '') <> '' and pan_card_number not like ?)
                   or (coalesce(voter_id, '') <> '' and voter_id not like ?)
                   or (coalesce(eid_number, '') <> '' and eid_number not like ?)
                   or (coalesce(bank_acc_no, '') <> '' and bank_acc_no not like ?)
                   or (coalesce(ifsc_code, '') <> '' and ifsc_code not like ?)
                   or (coalesce(micr_number, '') <> '' and micr_number not like ?)
                """, Long.class,
                ENCRYPTED_PATTERN, ENCRYPTED_PATTERN, ENCRYPTED_PATTERN, ENCRYPTED_PATTERN,
                ENCRYPTED_PATTERN, ENCRYPTED_PATTERN, ENCRYPTED_PATTERN);

        ids.forEach(id -> studentRepository.findById(id).ifPresent(studentRepository::save));
        if (!ids.isEmpty()) {
            log.info("Encrypted legacy sensitive fields for {} student record(s).", ids.size());
        }
    }

    private void migrateTeachers() {
        List<Long> ids = jdbcTemplate.queryForList("""
                select id from teacher
                where (coalesce(phone, '') <> '' and phone not like ?)
                   or (coalesce(alt_phone, '') <> '' and alt_phone not like ?)
                   or (coalesce(aadhar_number, '') <> '' and aadhar_number not like ?)
                   or (coalesce(pan_card_number, '') <> '' and pan_card_number not like ?)
                   or (coalesce(voter_id, '') <> '' and voter_id not like ?)
                   or (coalesce(passport_number, '') <> '' and passport_number not like ?)
                   or (coalesce(bank_acc_no, '') <> '' and bank_acc_no not like ?)
                   or (coalesce(ifsc_code, '') <> '' and ifsc_code not like ?)
                   or (coalesce(pf_number, '') <> '' and pf_number not like ?)
                   or (coalesce(uan_number, '') <> '' and uan_number not like ?)
                   or (coalesce(micr_number, '') <> '' and micr_number not like ?)
                   or (coalesce(emergency_phone, '') <> '' and emergency_phone not like ?)
                """, Long.class,
                ENCRYPTED_PATTERN, ENCRYPTED_PATTERN, ENCRYPTED_PATTERN, ENCRYPTED_PATTERN,
                ENCRYPTED_PATTERN, ENCRYPTED_PATTERN, ENCRYPTED_PATTERN, ENCRYPTED_PATTERN,
                ENCRYPTED_PATTERN, ENCRYPTED_PATTERN, ENCRYPTED_PATTERN, ENCRYPTED_PATTERN);

        ids.forEach(id -> teacherRepository.findById(id).ifPresent(teacherRepository::save));
        if (!ids.isEmpty()) {
            log.info("Encrypted legacy sensitive fields for {} teacher record(s).", ids.size());
        }
    }

    private void migrateAdmins() {
        List<Long> ids = jdbcTemplate.queryForList("""
                select id from admin
                where coalesce(phone, '') <> '' and phone not like ?
                """, Long.class, ENCRYPTED_PATTERN);

        ids.forEach(id -> adminRepository.findById(id).ifPresent(adminRepository::save));
        if (!ids.isEmpty()) {
            log.info("Encrypted legacy sensitive fields for {} admin record(s).", ids.size());
        }
    }
}
