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
public class AdminStatusSchemaMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminStatusSchemaMigrationRunner.class);

    private final JdbcTemplate jdbcTemplate;

    public AdminStatusSchemaMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        String schemaName = jdbcTemplate.queryForObject("select database()", String.class);
        if (schemaName == null || schemaName.isBlank()) {
            log.warn("Skipping admin status migration because no active database schema was detected.");
            return;
        }

        List<?> rows = jdbcTemplate.queryForList("""
                select column_name
                from information_schema.columns
                where table_schema = ?
                  and table_name = 'admin'
                  and column_name = 'college_status'
                """, schemaName);

        if (!rows.isEmpty()) {
            return;
        }

        jdbcTemplate.execute("alter table admin add column college_status varchar(30) null after college_code");
        log.info("Added missing admin column college_status.");
    }
}
