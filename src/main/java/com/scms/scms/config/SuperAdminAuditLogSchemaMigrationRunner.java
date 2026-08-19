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
@Order(3)
public class SuperAdminAuditLogSchemaMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminAuditLogSchemaMigrationRunner.class);

    private final JdbcTemplate jdbcTemplate;

    public SuperAdminAuditLogSchemaMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        String schemaName = jdbcTemplate.queryForObject("select database()", String.class);
        if (schemaName == null || schemaName.isBlank()) {
            log.warn("Skipping audit log schema migration because no active database schema was detected.");
            return;
        }

        List<?> rows = jdbcTemplate.queryForList("""
                select table_name
                from information_schema.tables
                where table_schema = ?
                  and table_name = 'super_admin_audit_log'
                """, schemaName);

        if (!rows.isEmpty()) {
            return;
        }

        jdbcTemplate.execute("""
                create table super_admin_audit_log (
                    id bigint not null auto_increment,
                    actor_email varchar(255),
                    action_type varchar(100),
                    entity_type varchar(100),
                    entity_name varchar(255),
                    details varchar(2000),
                    created_at datetime,
                    primary key (id)
                ) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci
                """);
        log.info("Created super_admin_audit_log table.");
    }
}
