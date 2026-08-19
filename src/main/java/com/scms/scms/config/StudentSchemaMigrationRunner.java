package com.scms.scms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Order(1)
public class StudentSchemaMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(StudentSchemaMigrationRunner.class);

    private final JdbcTemplate jdbcTemplate;

    public StudentSchemaMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        String schemaName = jdbcTemplate.queryForObject("select database()", String.class);
        if (schemaName == null || schemaName.isBlank()) {
            log.warn("Skipping student schema migration because no active database schema was detected.");
            return;
        }

        studentColumns().forEach((columnName, definition) -> ensureColumn(schemaName, columnName, definition));
    }

    private void ensureColumn(String schemaName, String columnName, String definition) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select column_name
                from information_schema.columns
                where table_schema = ?
                  and table_name = 'student'
                  and column_name = ?
                """, schemaName, columnName);

        if (!rows.isEmpty()) {
            return;
        }

        jdbcTemplate.execute("alter table student add column " + columnName + " " + definition);
        log.info("Added missing student column {}.", columnName);
    }

    private Map<String, String> studentColumns() {
        Map<String, String> columns = new LinkedHashMap<>();
        columns.put("mobile_number", "varchar(512) null");
        columns.put("title", "text null");
        columns.put("first_name", "text null");
        columns.put("middle_name", "text null");
        columns.put("last_name", "text null");
        columns.put("name_as_per_aadhaar", "text null");
        columns.put("lname_as_12th_std", "text null");
        columns.put("fname_as_12th_std", "text null");
        columns.put("mname_as_12th_std", "text null");
        columns.put("phone_number", "text null");
        columns.put("marital_status", "text null");
        columns.put("mother_tongue", "text null");
        columns.put("native_place", "text null");
        columns.put("birth_place", "text null");
        columns.put("birth_country", "text null");
        columns.put("region", "text null");
        columns.put("nationality", "text null");
        columns.put("category_type", "text null");
        columns.put("caste_category", "text null");
        columns.put("sub_caste", "text null");
        columns.put("father_occupation", "text null");
        columns.put("father_qualification", "text null");
        columns.put("mother_qualification", "text null");
        columns.put("total_family_member", "int null");
        columns.put("family_annual_income", "decimal(12,2) null");
        columns.put("differently_abled", "bit(1) null");
        columns.put("sports_person", "bit(1) null");
        columns.put("sports_achievement", "text null");
        columns.put("hobbies", "text null");
        columns.put("university_pre_adm_reg_no", "text null");
        columns.put("no_of_attempt", "int null");
        columns.put("inhouse", "text null");
        columns.put("medium_of_instruction", "text null");
        columns.put("social_reservation", "text null");
        columns.put("academic_bank_of_credits", "text null");
        columns.put("signature", "text null");
        columns.put("current_address", "text null");
        columns.put("permanent_address", "text null");
        columns.put("program_level", "varchar(255) null");
        columns.put("onboarding_completed", "bit(1) null");
        return columns;
    }
}
