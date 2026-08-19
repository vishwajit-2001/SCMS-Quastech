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
@Order(3)
public class PlacementTpoSchemaMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PlacementTpoSchemaMigrationRunner.class);

    private final JdbcTemplate jdbcTemplate;

    public PlacementTpoSchemaMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        String schemaName = jdbcTemplate.queryForObject("select database()", String.class);
        if (schemaName == null || schemaName.isBlank()) {
            log.warn("Skipping placement TPO schema migration because no active database schema was detected.");
            return;
        }

        tpoColumns().forEach((columnName, definition) -> ensureColumn(schemaName, columnName, definition));
        ensureIndex(schemaName, "idx_placement_tpo_college", "alter table placement_tpo add index idx_placement_tpo_college (college_id)");
        ensureIndex(schemaName, "idx_placement_tpo_admin", "alter table placement_tpo add index idx_placement_tpo_admin (admin_id)");
    }

    private void ensureColumn(String schemaName, String columnName, String definition) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select column_name
                from information_schema.columns
                where table_schema = ?
                  and table_name = 'placement_tpo'
                  and column_name = ?
                """, schemaName, columnName);

        if (!rows.isEmpty()) {
            return;
        }

        jdbcTemplate.execute("alter table placement_tpo add column " + columnName + " " + definition);
        log.info("Added missing placement_tpo column {}.", columnName);
    }

    private void ensureIndex(String schemaName, String indexName, String ddl) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select index_name
                from information_schema.statistics
                where table_schema = ?
                  and table_name = 'placement_tpo'
                  and index_name = ?
                """, schemaName, indexName);

        if (!rows.isEmpty()) {
            return;
        }

        jdbcTemplate.execute(ddl);
        log.info("Added missing placement_tpo index {}.", indexName);
    }

    private Map<String, String> tpoColumns() {
        Map<String, String> columns = new LinkedHashMap<>();
        columns.put("college_id", "bigint null");
        columns.put("admin_id", "bigint null");
        columns.put("employee_id", "varchar(255) null");
        return columns;
    }
}
