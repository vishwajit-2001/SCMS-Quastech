package com.scms.scms.config;

import com.scms.scms.model.Admin;
import com.scms.scms.model.College;
import com.scms.scms.repository.AdminRepository;
import com.scms.scms.repository.CollegeRepository;
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
public class CollegeSchemaMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CollegeSchemaMigrationRunner.class);

    private final JdbcTemplate jdbcTemplate;
    private final CollegeRepository collegeRepository;
    private final AdminRepository adminRepository;

    public CollegeSchemaMigrationRunner(JdbcTemplate jdbcTemplate,
                                        CollegeRepository collegeRepository,
                                        AdminRepository adminRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.collegeRepository = collegeRepository;
        this.adminRepository = adminRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        String schemaName = jdbcTemplate.queryForObject("select database()", String.class);
        if (schemaName == null || schemaName.isBlank()) {
            log.warn("Skipping college schema migration because no active database schema was detected.");
            return;
        }

        ensureCollegeTable(schemaName);
        ensureCollegeIdColumn(schemaName);
        backfillCollegeRecords();
    }

    private void ensureCollegeTable(String schemaName) {
        jdbcTemplate.execute("""
                create table if not exists college (
                    id bigint not null auto_increment,
                    name varchar(255) not null,
                    code varchar(100) not null,
                    address text,
                    logo_path text,
                    status varchar(30),
                    primary key (id),
                    unique key uq_college_name (name),
                    unique key uq_college_code (code)
                ) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci
                """);
        log.info("Ensured college table exists.");
    }

    private void ensureCollegeIdColumn(String schemaName) {
        List<?> rows = jdbcTemplate.queryForList("""
                select column_name
                from information_schema.columns
                where table_schema = ?
                  and table_name = 'admin'
                  and column_name = 'college_id'
                """, schemaName);

        if (!rows.isEmpty()) {
            return;
        }

        jdbcTemplate.execute("alter table admin add column college_id bigint null after role");
        jdbcTemplate.execute("""
                alter table admin
                add constraint fk_admin_college
                foreign key (college_id) references college(id)
                on delete set null
                """);
        log.info("Added missing admin column college_id.");
    }

    private void backfillCollegeRecords() {
        List<Admin> admins = adminRepository.findAll();
        for (Admin admin : admins) {
            if (admin == null || admin.isSuperAdmin()) {
                continue;
            }

            College college = resolveCollege(admin);
            if (college == null) {
                college = new College();
                college.setName(admin.getCollegeName());
                college.setCode(admin.getCollegeCode());
                college.setStatus(admin.getCollegeStatus());
                collegeRepository.save(college);
            }

            if (admin.getCollege() == null || admin.getCollege().getId() == null || !admin.getCollege().getId().equals(college.getId())) {
                admin.setCollege(college);
                admin.setCollegeName(college.getName());
                admin.setCollegeCode(college.getCode());
                admin.setCollegeStatus(college.getStatus());
                adminRepository.save(admin);
            }
        }
    }

    private College resolveCollege(Admin admin) {
        College byCode = null;
        String code = admin.getCollegeCode();
        if (code != null && !code.isBlank()) {
            byCode = collegeRepository.findByCodeIgnoreCase(code.trim());
        }
        if (byCode != null) {
            return byCode;
        }

        String name = admin.getCollegeName();
        if (name != null && !name.isBlank()) {
            return collegeRepository.findByNameIgnoreCase(name.trim());
        }
        return null;
    }
}
