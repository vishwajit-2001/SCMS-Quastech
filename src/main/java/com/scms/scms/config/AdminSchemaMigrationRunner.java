package com.scms.scms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(2)
public class AdminSchemaMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSchemaMigrationRunner.class);

    private final JdbcTemplate jdbcTemplate;

    public AdminSchemaMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        String schemaName = jdbcTemplate.queryForObject("select database()", String.class);
        if (schemaName == null || schemaName.isBlank()) {
            log.warn("Skipping admin schema migration because no active database schema was detected.");
            return;
        }

        ensureCollegeCodeColumn(schemaName);
    }

    private void ensureCollegeCodeColumn(String schemaName) {
        List<?> rows = jdbcTemplate.queryForList("""
                select column_name
                from information_schema.columns
                where table_schema = ?
                  and table_name = 'admin'
                  and column_name = 'college_code'
                """, schemaName);

        if (!rows.isEmpty()) {
            return;
        }

        jdbcTemplate.execute("alter table admin add column college_code varchar(100) null after college_name");
        log.info("Added missing admin column college_code.");
    }
}
