package com.scms.scms.config;

/**
 * The legacy classroom.teacher_id column has been removed from the schema,
 * so the old startup cleanup is no longer needed.
 */
public final class ClassTeacherCleanupRunner {

    private ClassTeacherCleanupRunner() {
    }
}
