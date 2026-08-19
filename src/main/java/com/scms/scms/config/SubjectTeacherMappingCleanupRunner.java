package com.scms.scms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SubjectTeacherMappingCleanupRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SubjectTeacherMappingCleanupRunner.class);

    private final JdbcTemplate jdbcTemplate;

    public SubjectTeacherMappingCleanupRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        clearTeacherColumn("subject_master");
        clearTeacherColumn("subject");
    }

    private void clearTeacherColumn(String tableName) {
        try {
            jdbcTemplate.update("UPDATE " + tableName + " SET teacher_id = NULL WHERE teacher_id IS NOT NULL");
        } catch (Exception ex) {
            log.debug("Skipping {} teacher mapping cleanup: {}", tableName, ex.getMessage());
        }
    }
}
