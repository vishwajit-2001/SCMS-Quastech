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
@Order(0)
public class SensitiveColumnSchemaMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SensitiveColumnSchemaMigrationRunner.class);
    private static final int TARGET_LENGTH = 512;

    private final JdbcTemplate jdbcTemplate;

    public SensitiveColumnSchemaMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        String schemaName = jdbcTemplate.queryForObject("select database()", String.class);
        if (schemaName == null || schemaName.isBlank()) {
            log.warn("Skipping sensitive column schema migration because no active database schema was detected.");
            return;
        }

        sensitiveColumns().forEach((tableName, columns) -> ensureColumnLengths(schemaName, tableName, columns));
    }

    private void ensureColumnLengths(String schemaName, String tableName, List<String> columns) {
        for (String columnName : columns) {
            Map<String, Object> metadata = loadColumnMetadata(schemaName, tableName, columnName);
            if (metadata == null) {
                continue;
            }

            String dataType = stringValue(metadata.get("data_type"));
            Integer currentLength = integerValue(metadata.get("character_maximum_length"));
            boolean alreadyWideEnough = "varchar".equalsIgnoreCase(dataType)
                    && currentLength != null
                    && currentLength >= TARGET_LENGTH;

            if (alreadyWideEnough) {
                continue;
            }

            boolean nullable = !"NO".equalsIgnoreCase(stringValue(metadata.get("is_nullable")));
            String nullability = nullable ? "NULL" : "NOT NULL";

            jdbcTemplate.execute(String.format(
                    "alter table `%s` modify column `%s` varchar(%d) %s",
                    tableName, columnName, TARGET_LENGTH, nullability));

            log.info("Adjusted {}.{} to VARCHAR({}) for encrypted storage.", tableName, columnName, TARGET_LENGTH);
        }
    }

    private Map<String, Object> loadColumnMetadata(String schemaName, String tableName, String columnName) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                select data_type, character_maximum_length, is_nullable
                from information_schema.columns
                where table_schema = ?
                  and table_name = ?
                  and column_name = ?
                """, schemaName, tableName, columnName);

        if (rows.isEmpty()) {
            log.debug("Skipping schema adjustment for missing column {}.{}", tableName, columnName);
            return null;
        }
        return rows.get(0);
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private Integer integerValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return value == null ? null : Integer.valueOf(value.toString());
    }

    private Map<String, List<String>> sensitiveColumns() {
        Map<String, List<String>> columns = new LinkedHashMap<>();
        columns.put("student", List.of(
                "aadhar_number",
                "pan_card_number",
                "voter_id",
                "eid_number",
                "bank_acc_no",
                "ifsc_code",
                "micr_number"
        ));
        columns.put("teacher", List.of(
                "phone",
                "alt_phone",
                "aadhar_number",
                "pan_card_number",
                "voter_id",
                "passport_number",
                "bank_acc_no",
                "ifsc_code",
                "pf_number",
                "uan_number",
                "micr_number",
                "emergency_phone"
        ));
        columns.put("admin", List.of("phone"));
        return columns;
    }
}
