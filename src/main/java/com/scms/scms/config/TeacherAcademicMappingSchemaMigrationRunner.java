package com.scms.scms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TeacherAcademicMappingSchemaMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TeacherAcademicMappingSchemaMigrationRunner.class);

    private final JdbcTemplate jdbcTemplate;

    public TeacherAcademicMappingSchemaMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists("teacher_academic_mapping")) {
            return;
        }
        relaxNullableColumn("semester", "INT");
        relaxNullableColumn("section", "VARCHAR(20)");
        relaxNullableColumn("subject_id", "BIGINT");
        dropIndexIfExists("uq_teacher_academic_subject_scope");
        collapseDuplicateScopeRows();
        addIndexIfMissing();
    }

    private boolean tableExists(String table) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = database() and table_name = ?",
                Integer.class,
                table
        );
        return count != null && count > 0;
    }

    private void relaxNullableColumn(String column, String type) {
        try {
            jdbcTemplate.execute("alter table teacher_academic_mapping modify column " + column + " " + type + " null");
        } catch (Exception ex) {
            log.debug("Teacher mapping column {} already relaxed or unavailable: {}", column, ex.getMessage());
        }
    }

    private void dropIndexIfExists(String indexName) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.statistics where table_schema = database() and table_name = 'teacher_academic_mapping' and index_name = ?",
                Integer.class,
                indexName
        );
        if (count != null && count > 0) {
            jdbcTemplate.execute("alter table teacher_academic_mapping drop index " + indexName);
        }
    }

    private void addIndexIfMissing() {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.statistics where table_schema = database() and table_name = 'teacher_academic_mapping' and index_name = 'uq_teacher_academic_scope'",
                Integer.class
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute("""
                    alter table teacher_academic_mapping
                    add unique key uq_teacher_academic_scope (teacher_id, course_id, batch_id, academic_year)
                    """);
        }
    }

    private void collapseDuplicateScopeRows() {
        try {
            jdbcTemplate.execute("""
                    delete duplicate_row from teacher_academic_mapping duplicate_row
                    join teacher_academic_mapping keep_row
                      on duplicate_row.teacher_id = keep_row.teacher_id
                     and duplicate_row.course_id = keep_row.course_id
                     and duplicate_row.batch_id = keep_row.batch_id
                     and duplicate_row.academic_year = keep_row.academic_year
                     and duplicate_row.id > keep_row.id
                    """);
        } catch (Exception ex) {
            log.debug("Teacher mapping duplicate collapse skipped: {}", ex.getMessage());
        }
    }
}
