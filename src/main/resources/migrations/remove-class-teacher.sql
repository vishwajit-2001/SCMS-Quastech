USE scms_db1;

UPDATE classroom
SET teacher_id = NULL
WHERE teacher_id IS NOT NULL;

SET @fk_name := (
    SELECT CONSTRAINT_NAME
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'classroom'
      AND COLUMN_NAME = 'teacher_id'
      AND REFERENCED_TABLE_NAME IS NOT NULL
    LIMIT 1
);

SET @drop_fk_sql := IF(
    @fk_name IS NOT NULL,
    CONCAT('ALTER TABLE classroom DROP FOREIGN KEY ', @fk_name),
    'SELECT 1'
);
PREPARE drop_fk_stmt FROM @drop_fk_sql;
EXECUTE drop_fk_stmt;
DEALLOCATE PREPARE drop_fk_stmt;

SET @idx_name := (
    SELECT INDEX_NAME
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'classroom'
      AND COLUMN_NAME = 'teacher_id'
      AND INDEX_NAME <> 'PRIMARY'
    LIMIT 1
);

SET @drop_idx_sql := IF(
    @idx_name IS NOT NULL,
    CONCAT('ALTER TABLE classroom DROP INDEX ', @idx_name),
    'SELECT 1'
);
PREPARE drop_idx_stmt FROM @drop_idx_sql;
EXECUTE drop_idx_stmt;
DEALLOCATE PREPARE drop_idx_stmt;

SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'classroom'
      AND COLUMN_NAME = 'teacher_id'
);

SET @drop_column_sql := IF(
    @column_exists > 0,
    'ALTER TABLE classroom DROP COLUMN teacher_id',
    'SELECT 1'
);
PREPARE drop_column_stmt FROM @drop_column_sql;
EXECUTE drop_column_stmt;
DEALLOCATE PREPARE drop_column_stmt;
