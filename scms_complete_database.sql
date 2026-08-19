-- ============================================================================
-- SCMS (Student College Management System) - COMPLETE DATABASE SCRIPT
-- Database: scms_db
-- Matches the current Java entities in this project.
-- ============================================================================

CREATE DATABASE IF NOT EXISTS scms_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE scms_db;

SET FOREIGN_KEY_CHECKS = 0;
SET SQL_SAFE_UPDATES = 0;

-- ----------------------------------------------------------------------------
-- DROP TABLES (reverse dependency order)
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS placement_interview_student;
DROP TABLE IF EXISTS placement_interview;
DROP TABLE IF EXISTS placement_application;
DROP TABLE IF EXISTS placement_drive;
DROP TABLE IF EXISTS placement_tpo;
DROP TABLE IF EXISTS campus_event_application;
DROP TABLE IF EXISTS campus_event;
DROP TABLE IF EXISTS study_material;
DROP TABLE IF EXISTS timetable;
DROP TABLE IF EXISTS fees;
DROP TABLE IF EXISTS subject;
DROP TABLE IF EXISTS academic_structure;
DROP TABLE IF EXISTS student;
DROP TABLE IF EXISTS teacher;
DROP TABLE IF EXISTS classroom;
DROP TABLE IF EXISTS batch;
DROP TABLE IF EXISTS course;
DROP TABLE IF EXISTS admin;

-- ----------------------------------------------------------------------------
-- TABLE: admin
-- ----------------------------------------------------------------------------
CREATE TABLE admin (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) DEFAULT 'Admin',
    email         VARCHAR(100) NOT NULL UNIQUE,
    password      VARCHAR(100) NOT NULL,
    college_name  VARCHAR(200) DEFAULT 'AI Campus Institute',
    phone         VARCHAR(512) DEFAULT '',
    role          VARCHAR(50)  DEFAULT 'Institute Admin'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- TABLE: course
-- ----------------------------------------------------------------------------
CREATE TABLE course (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    name             VARCHAR(100) NOT NULL,
    code             VARCHAR(50)  NOT NULL,
    duration_years   INT DEFAULT NULL,
    total_semesters  INT DEFAULT NULL,
    status           VARCHAR(50) DEFAULT 'Active',
    admin_id         BIGINT NOT NULL,
    CONSTRAINT uq_course_admin_code UNIQUE (admin_id, code),
    CONSTRAINT fk_course_admin
        FOREIGN KEY (admin_id) REFERENCES admin(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- TABLE: batch
-- ----------------------------------------------------------------------------
CREATE TABLE batch (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    display_name    VARCHAR(120) NOT NULL,
    start_year      INT DEFAULT NULL,
    end_year        INT DEFAULT NULL,
    course_id       BIGINT NOT NULL,
    admin_id        BIGINT NOT NULL,
    CONSTRAINT uq_batch_admin_course_display UNIQUE (admin_id, course_id, display_name),
    CONSTRAINT fk_batch_course
        FOREIGN KEY (course_id) REFERENCES course(id),
    CONSTRAINT fk_batch_admin
        FOREIGN KEY (admin_id) REFERENCES admin(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- TABLE: academic_structure
-- ----------------------------------------------------------------------------
CREATE TABLE academic_structure (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    year_label       VARCHAR(20) NOT NULL,
    semester_number  INT NOT NULL,
    section          VARCHAR(10) NOT NULL,
    course_id        BIGINT NOT NULL,
    batch_id         BIGINT NOT NULL,
    admin_id         BIGINT NOT NULL,
    CONSTRAINT uq_academic_structure_unique
        UNIQUE (admin_id, course_id, batch_id, year_label, semester_number, section),
    CONSTRAINT fk_academic_structure_course
        FOREIGN KEY (course_id) REFERENCES course(id),
    CONSTRAINT fk_academic_structure_batch
        FOREIGN KEY (batch_id) REFERENCES batch(id),
    CONSTRAINT fk_academic_structure_admin
        FOREIGN KEY (admin_id) REFERENCES admin(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- TABLE: subject_master
-- ----------------------------------------------------------------------------
CREATE TABLE subject_master (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    name             VARCHAR(255) NOT NULL,
    code             VARCHAR(50) NOT NULL,
    semester_number  INT NOT NULL,
    credits          DECIMAL(8,2) DEFAULT 4.00,
    category         VARCHAR(255) DEFAULT NULL,
    status           VARCHAR(50) DEFAULT 'active',
    created_at       DATETIME DEFAULT NULL,
    course_id        BIGINT NOT NULL,
    admin_id         BIGINT NOT NULL,
    CONSTRAINT uq_subject_master_admin_course_sem_code
        UNIQUE (admin_id, course_id, semester_number, code),
    CONSTRAINT fk_subject_master_course
        FOREIGN KEY (course_id) REFERENCES course(id),
    CONSTRAINT fk_subject_master_admin
        FOREIGN KEY (admin_id) REFERENCES admin(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- TABLE: classroom
-- ----------------------------------------------------------------------------
CREATE TABLE classroom (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    name             VARCHAR(100) DEFAULT NULL,
    course           VARCHAR(100) DEFAULT NULL,
    course_code      VARCHAR(50)  DEFAULT NULL,
    department       VARCHAR(100) DEFAULT NULL,
    course_type      VARCHAR(50)  DEFAULT NULL,
    duration         VARCHAR(50)  DEFAULT NULL,
    total_semesters  INT DEFAULT NULL,
    `year`           VARCHAR(20)  DEFAULT NULL,
    semester         INT DEFAULT NULL,
    section          VARCHAR(20)  DEFAULT NULL,
    room             VARCHAR(50)  DEFAULT NULL,
    batch            VARCHAR(100) DEFAULT NULL,
    batch_name       VARCHAR(100) DEFAULT NULL,
    batch_start_year INT DEFAULT NULL,
    batch_end_year   INT DEFAULT NULL,
    academic_year    VARCHAR(20)  DEFAULT NULL,
    intake_capacity  INT DEFAULT NULL,
    shift            VARCHAR(20)  DEFAULT NULL,
    total_fees       DOUBLE DEFAULT NULL,
    description      VARCHAR(2000) DEFAULT NULL,
    status           VARCHAR(20) DEFAULT 'Active',
    remarks          VARCHAR(2000) DEFAULT NULL,
    teacher_id       BIGINT DEFAULT NULL,
    admin_id         BIGINT DEFAULT NULL,
    INDEX idx_classroom_teacher (teacher_id),
    INDEX idx_classroom_admin (admin_id),
    INDEX idx_classroom_course (course),
    CONSTRAINT fk_classroom_teacher
        FOREIGN KEY (teacher_id) REFERENCES teacher(id),
    CONSTRAINT fk_classroom_admin
        FOREIGN KEY (admin_id) REFERENCES admin(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- TABLE: teacher
-- ----------------------------------------------------------------------------
CREATE TABLE teacher (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                    VARCHAR(100) DEFAULT NULL,
    email                   VARCHAR(100) DEFAULT NULL,
    password                VARCHAR(100) DEFAULT NULL,
    subject                 VARCHAR(100) DEFAULT NULL,
    photo                   VARCHAR(500) DEFAULT NULL,

    gender                  VARCHAR(20) DEFAULT NULL,
    dob                     DATE DEFAULT NULL,
    blood_group             VARCHAR(50) DEFAULT NULL,
    phone                   VARCHAR(512) DEFAULT NULL,
    alt_phone               VARCHAR(512) DEFAULT NULL,
    marital_status          VARCHAR(50) DEFAULT NULL,
    religion                VARCHAR(50) DEFAULT NULL,
    caste_name              VARCHAR(100) DEFAULT NULL,
    category                VARCHAR(50) DEFAULT NULL,

    address                 VARCHAR(255) DEFAULT NULL,
    permanent_address       VARCHAR(255) DEFAULT NULL,
    city                    VARCHAR(100) DEFAULT NULL,
    state                   VARCHAR(100) DEFAULT NULL,
    pin_code                VARCHAR(20) DEFAULT NULL,

    designation             VARCHAR(100) DEFAULT NULL,
    employee_id             VARCHAR(100) DEFAULT NULL,
    joining_date            DATE DEFAULT NULL,
    experience              VARCHAR(50) DEFAULT NULL,
    employment_type         VARCHAR(50) DEFAULT NULL,
    salary                  DOUBLE DEFAULT NULL,
    specialization          VARCHAR(255) DEFAULT NULL,
    department              VARCHAR(100) DEFAULT NULL,
    status                  VARCHAR(20) DEFAULT NULL,
    academic_year           VARCHAR(20) DEFAULT NULL,

    qualification           VARCHAR(100) DEFAULT NULL,
    degree_specialization    VARCHAR(100) DEFAULT NULL,
    university              VARCHAR(255) DEFAULT NULL,
    year_of_passing         INT DEFAULT NULL,
    publications            INT DEFAULT NULL,

    aadhar_number           VARCHAR(512) DEFAULT NULL,
    pan_card_number         VARCHAR(512) DEFAULT NULL,
    voter_id                VARCHAR(512) DEFAULT NULL,
    passport_number         VARCHAR(512) DEFAULT NULL,

    bank_name               VARCHAR(100) DEFAULT NULL,
    bank_acc_no             VARCHAR(512) DEFAULT NULL,
    ifsc_code               VARCHAR(512) DEFAULT NULL,
    pf_number               VARCHAR(512) DEFAULT NULL,
    uan_number              VARCHAR(512) DEFAULT NULL,
    micr_number             VARCHAR(512) DEFAULT NULL,

    emergency_contact_name  VARCHAR(100) DEFAULT NULL,
    emergency_phone         VARCHAR(512) DEFAULT NULL,
    emergency_relation      VARCHAR(100) DEFAULT NULL,

    admin_id                BIGINT NOT NULL,
    class_room_id           BIGINT DEFAULT NULL,
    INDEX idx_teacher_admin (admin_id),
    INDEX idx_teacher_class_room (class_room_id),
    CONSTRAINT fk_teacher_admin
        FOREIGN KEY (admin_id) REFERENCES admin(id),
    CONSTRAINT fk_teacher_class_room
        FOREIGN KEY (class_room_id) REFERENCES classroom(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Add classroom -> teacher FK after teacher table exists.
ALTER TABLE classroom
    ADD CONSTRAINT fk_classroom_teacher_ref
        FOREIGN KEY (teacher_id) REFERENCES teacher(id);

-- ----------------------------------------------------------------------------
-- TABLE: student
-- ----------------------------------------------------------------------------
CREATE TABLE student (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                VARCHAR(100) DEFAULT NULL,
    email               VARCHAR(150) DEFAULT NULL,
    password            VARCHAR(100) DEFAULT NULL,

    course              VARCHAR(100) DEFAULT NULL,
    semester            VARCHAR(20) DEFAULT NULL,
    academic_year       VARCHAR(20) DEFAULT NULL,
    degree              VARCHAR(50) DEFAULT NULL,
    section_name        VARCHAR(20) DEFAULT NULL,
    medium              VARCHAR(50) DEFAULT NULL,

    roll_no             VARCHAR(50) DEFAULT NULL,
    enrollment_no       VARCHAR(50) DEFAULT NULL,
    registration_no     VARCHAR(50) DEFAULT NULL,
    prn_number          VARCHAR(50) DEFAULT NULL,
    abc_number          VARCHAR(50) DEFAULT NULL,

    gender              VARCHAR(20) DEFAULT NULL,
    dob                 DATE DEFAULT NULL,
    blood_group         VARCHAR(20) DEFAULT NULL,
    religion            VARCHAR(50) DEFAULT NULL,
    admission_date      DATE DEFAULT NULL,
    caste_name          VARCHAR(100) DEFAULT NULL,
    category            VARCHAR(50) DEFAULT NULL,

    father_name         VARCHAR(100) DEFAULT NULL,
    mother_name         VARCHAR(100) DEFAULT NULL,
    guardian_name       VARCHAR(100) DEFAULT NULL,

    aadhar_number       VARCHAR(512) DEFAULT NULL,
    pan_card_number     VARCHAR(512) DEFAULT NULL,
    voter_id            VARCHAR(512) DEFAULT NULL,
    eid_number          VARCHAR(512) DEFAULT NULL,

    bank_name           VARCHAR(100) DEFAULT NULL,
    bank_acc_no         VARCHAR(512) DEFAULT NULL,
    ifsc_code           VARCHAR(512) DEFAULT NULL,
    micr_number         VARCHAR(512) DEFAULT NULL,

    photo               VARCHAR(500) DEFAULT NULL,

    total_fees          DOUBLE DEFAULT NULL,
    paid_fees           DOUBLE DEFAULT NULL,
    pending_fees        DOUBLE DEFAULT NULL,

    class_id            BIGINT DEFAULT NULL,
    batch_id            BIGINT DEFAULT NULL,
    admin_id            BIGINT NOT NULL,
    UNIQUE KEY uq_student_email (email),
    UNIQUE KEY uq_student_admin_enrollment (admin_id, enrollment_no),
    INDEX idx_student_class (class_id),
    INDEX idx_student_batch (batch_id),
    INDEX idx_student_admin (admin_id),
    CONSTRAINT fk_student_class
        FOREIGN KEY (class_id) REFERENCES classroom(id),
    CONSTRAINT fk_student_batch
        FOREIGN KEY (batch_id) REFERENCES batch(id),
    CONSTRAINT fk_student_admin
        FOREIGN KEY (admin_id) REFERENCES admin(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- TABLE: subject
-- ----------------------------------------------------------------------------
CREATE TABLE subject (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    code            VARCHAR(50) NOT NULL,
    semester        INT NOT NULL,
    course_category VARCHAR(100) DEFAULT NULL,
    credits         DOUBLE DEFAULT NULL,
    term            VARCHAR(50) DEFAULT NULL,
    cycle           VARCHAR(50) DEFAULT NULL,
    subject_master_id BIGINT DEFAULT NULL,
    course_id       BIGINT NOT NULL,
    batch_id        BIGINT NOT NULL,
    teacher_id      BIGINT DEFAULT NULL,
    is_override     BIT(1) DEFAULT b'0',
    status          VARCHAR(50) DEFAULT 'active',
    created_at      DATETIME DEFAULT NULL,
    admin_id        BIGINT NOT NULL,
    CONSTRAINT uq_subject_admin_course_batch_sem_code
        UNIQUE (admin_id, course_id, batch_id, semester, code),
    CONSTRAINT fk_subject_master_ref
        FOREIGN KEY (subject_master_id) REFERENCES subject_master(id),
    CONSTRAINT fk_subject_course
        FOREIGN KEY (course_id) REFERENCES course(id),
    CONSTRAINT fk_subject_batch
        FOREIGN KEY (batch_id) REFERENCES batch(id),
    CONSTRAINT fk_subject_teacher
        FOREIGN KEY (teacher_id) REFERENCES teacher(id),
    CONSTRAINT fk_subject_admin
        FOREIGN KEY (admin_id) REFERENCES admin(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- TABLE: study_material
-- ----------------------------------------------------------------------------
CREATE TABLE study_material (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    title               VARCHAR(255) DEFAULT NULL,
    description         VARCHAR(1000) DEFAULT NULL,
    subject             VARCHAR(255) DEFAULT NULL,
    course              VARCHAR(255) DEFAULT NULL,
    semester            VARCHAR(50) DEFAULT NULL,
    section             VARCHAR(50) DEFAULT NULL,
    academic_year       VARCHAR(20) DEFAULT NULL,
    original_file_name  VARCHAR(255) DEFAULT NULL,
    stored_file_name    VARCHAR(255) DEFAULT NULL,
    file_path           VARCHAR(500) DEFAULT NULL,
    content_type        VARCHAR(100) DEFAULT NULL,
    file_size           BIGINT DEFAULT NULL,
    uploaded_at         DATETIME DEFAULT CURRENT_TIMESTAMP,
    teacher_id          BIGINT DEFAULT NULL,
    class_room_id       BIGINT DEFAULT NULL,
    admin_id            BIGINT NOT NULL,
    INDEX idx_study_material_teacher (teacher_id),
    INDEX idx_study_material_class (class_room_id),
    INDEX idx_study_material_admin (admin_id),
    CONSTRAINT fk_study_material_teacher
        FOREIGN KEY (teacher_id) REFERENCES teacher(id),
    CONSTRAINT fk_study_material_class
        FOREIGN KEY (class_room_id) REFERENCES classroom(id),
    CONSTRAINT fk_study_material_admin
        FOREIGN KEY (admin_id) REFERENCES admin(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- TABLE: campus_event
-- ----------------------------------------------------------------------------
CREATE TABLE campus_event (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    title                   VARCHAR(255) DEFAULT NULL,
    organizer_name          VARCHAR(255) DEFAULT NULL,
    organization_name       VARCHAR(255) DEFAULT NULL,
    venue                   VARCHAR(255) DEFAULT NULL,
    category                VARCHAR(100) DEFAULT NULL,
    description             VARCHAR(2000) DEFAULT NULL,
    event_date_time         DATETIME DEFAULT NULL,
    registration_deadline   DATETIME DEFAULT NULL,
    target_audience         VARCHAR(255) DEFAULT NULL,
    published               BOOLEAN DEFAULT FALSE,
    created_at              DATETIME DEFAULT CURRENT_TIMESTAMP,
    teacher_id              BIGINT DEFAULT NULL,
    INDEX idx_campus_event_teacher (teacher_id),
    CONSTRAINT fk_campus_event_teacher
        FOREIGN KEY (teacher_id) REFERENCES teacher(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- TABLE: campus_event_application
-- ----------------------------------------------------------------------------
CREATE TABLE campus_event_application (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id        BIGINT DEFAULT NULL,
    student_id      BIGINT DEFAULT NULL,
    full_name       VARCHAR(255) DEFAULT NULL,
    email           VARCHAR(255) DEFAULT NULL,
    phone           VARCHAR(50) DEFAULT NULL,
    course          VARCHAR(255) DEFAULT NULL,
    semester        VARCHAR(50) DEFAULT NULL,
    specialization  VARCHAR(255) DEFAULT NULL,
    section_name    VARCHAR(50) DEFAULT NULL,
    notes           VARCHAR(1500) DEFAULT NULL,
    applied_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_campus_event_application_event (event_id),
    INDEX idx_campus_event_application_student (student_id),
    CONSTRAINT fk_campus_event_application_event
        FOREIGN KEY (event_id) REFERENCES campus_event(id),
    CONSTRAINT fk_campus_event_application_student
        FOREIGN KEY (student_id) REFERENCES student(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- TABLE: placement_tpo
-- ----------------------------------------------------------------------------
CREATE TABLE placement_tpo (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(255) DEFAULT NULL,
    email         VARCHAR(255) DEFAULT NULL UNIQUE,
    password      VARCHAR(255) DEFAULT NULL,
    college_name  VARCHAR(255) DEFAULT NULL,
    phone         VARCHAR(50) DEFAULT NULL,
    department    VARCHAR(255) DEFAULT NULL,
    designation   VARCHAR(255) DEFAULT NULL,
    status        VARCHAR(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- TABLE: placement_drive
-- ----------------------------------------------------------------------------
CREATE TABLE placement_drive (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_name            VARCHAR(255) DEFAULT NULL,
    job_title               VARCHAR(255) DEFAULT NULL,
    package_offered         VARCHAR(255) DEFAULT NULL,
    location                VARCHAR(255) DEFAULT NULL,
    drive_type              VARCHAR(100) DEFAULT NULL,
    experience              VARCHAR(100) DEFAULT NULL,
    salary_range            VARCHAR(255) DEFAULT NULL,
    openings                INT DEFAULT NULL,
    eligibility_course      VARCHAR(255) DEFAULT NULL,
    eligibility_semester    VARCHAR(255) DEFAULT NULL,
    eligibility_degree      VARCHAR(255) DEFAULT NULL,
    skills_required         VARCHAR(1000) DEFAULT NULL,
    description             VARCHAR(2000) DEFAULT NULL,
    drive_date              DATE DEFAULT NULL,
    last_apply_date         DATE DEFAULT NULL,
    application_deadline    DATETIME DEFAULT NULL,
    published               BOOLEAN DEFAULT FALSE,
    created_at              DATETIME DEFAULT CURRENT_TIMESTAMP,
    tpo_id                  BIGINT DEFAULT NULL,
    INDEX idx_placement_drive_tpo (tpo_id),
    CONSTRAINT fk_placement_drive_tpo
        FOREIGN KEY (tpo_id) REFERENCES placement_tpo(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- TABLE: placement_application
-- ----------------------------------------------------------------------------
CREATE TABLE placement_application (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    drive_id            BIGINT DEFAULT NULL,
    student_id          BIGINT DEFAULT NULL,
    full_name           VARCHAR(255) DEFAULT NULL,
    email               VARCHAR(255) DEFAULT NULL,
    phone               VARCHAR(50) DEFAULT NULL,
    course              VARCHAR(255) DEFAULT NULL,
    semester            VARCHAR(50) DEFAULT NULL,
    specialization      VARCHAR(255) DEFAULT NULL,
    current_location    VARCHAR(255) DEFAULT NULL,
    skills              VARCHAR(1000) DEFAULT NULL,
    cover_note          VARCHAR(2000) DEFAULT NULL,
    resume_path         VARCHAR(500) DEFAULT NULL,
    resume_name         VARCHAR(255) DEFAULT NULL,
    status              VARCHAR(50) DEFAULT NULL,
    applied_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_placement_application_drive (drive_id),
    INDEX idx_placement_application_student (student_id),
    CONSTRAINT fk_placement_application_drive
        FOREIGN KEY (drive_id) REFERENCES placement_drive(id),
    CONSTRAINT fk_placement_application_student
        FOREIGN KEY (student_id) REFERENCES student(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- TABLE: placement_interview
-- ----------------------------------------------------------------------------
CREATE TABLE placement_interview (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    drive_id            BIGINT DEFAULT NULL,
    tpo_id              BIGINT DEFAULT NULL,
    interview_round     VARCHAR(255) DEFAULT NULL,
    interview_date_time DATETIME DEFAULT NULL,
    venue               VARCHAR(255) DEFAULT NULL,
    meeting_link        VARCHAR(500) DEFAULT NULL,
    notes               VARCHAR(2000) DEFAULT NULL,
    status              VARCHAR(50) DEFAULT NULL,
    created_at          DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_placement_interview_drive (drive_id),
    INDEX idx_placement_interview_tpo (tpo_id),
    CONSTRAINT fk_placement_interview_drive
        FOREIGN KEY (drive_id) REFERENCES placement_drive(id),
    CONSTRAINT fk_placement_interview_tpo
        FOREIGN KEY (tpo_id) REFERENCES placement_tpo(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- TABLE: placement_interview_student
-- Join table for placement_interview <-> student
-- ----------------------------------------------------------------------------
CREATE TABLE placement_interview_student (
    interview_id    BIGINT NOT NULL,
    student_id      BIGINT NOT NULL,
    PRIMARY KEY (interview_id, student_id),
    CONSTRAINT fk_pis_interview
        FOREIGN KEY (interview_id) REFERENCES placement_interview(id),
    CONSTRAINT fk_pis_student
        FOREIGN KEY (student_id) REFERENCES student(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- TABLE: fees
-- ----------------------------------------------------------------------------
CREATE TABLE fees (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    course          VARCHAR(100) DEFAULT NULL,
    batch_name      VARCHAR(120) DEFAULT NULL,
    batch_id        BIGINT DEFAULT NULL,
    academic_year   VARCHAR(20) DEFAULT NULL,
    fee_scope       VARCHAR(50) DEFAULT NULL,
    semester        VARCHAR(50) DEFAULT NULL,
    total_amount    DOUBLE NOT NULL,
    admin_id        BIGINT NOT NULL,
    INDEX idx_fees_batch (batch_id),
    INDEX idx_fees_admin (admin_id),
    INDEX idx_fees_lookup (course, academic_year, semester),
    CONSTRAINT fk_fees_batch
        FOREIGN KEY (batch_id) REFERENCES batch(id),
    CONSTRAINT fk_fees_admin
        FOREIGN KEY (admin_id) REFERENCES admin(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- TABLE: timetable
-- ----------------------------------------------------------------------------
CREATE TABLE timetable (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    day             VARCHAR(20) DEFAULT NULL,
    start_time      VARCHAR(20) DEFAULT NULL,
    end_time        VARCHAR(20) DEFAULT NULL,
    subject         VARCHAR(255) DEFAULT NULL,
    room            VARCHAR(100) DEFAULT NULL,
    entry_type      VARCHAR(50) DEFAULT NULL,
    academic_year   VARCHAR(20) DEFAULT NULL,
    slot            INT DEFAULT NULL,
    class_id        BIGINT DEFAULT NULL,
    teacher_id      BIGINT DEFAULT NULL,
    admin_id        BIGINT DEFAULT NULL,
    INDEX idx_timetable_class (class_id),
    INDEX idx_timetable_teacher (teacher_id),
    INDEX idx_timetable_admin (admin_id),
    INDEX idx_timetable_scope (academic_year, day, slot),
    CONSTRAINT fk_timetable_class
        FOREIGN KEY (class_id) REFERENCES classroom(id),
    CONSTRAINT fk_timetable_teacher
        FOREIGN KEY (teacher_id) REFERENCES teacher(id),
    CONSTRAINT fk_timetable_admin
        FOREIGN KEY (admin_id) REFERENCES admin(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- Optional starter seed data
-- Add your own rows after importing this schema.
-- ============================================================================
