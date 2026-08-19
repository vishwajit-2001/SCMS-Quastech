-- ============================================================================
-- SCMS (Student College Management System) - UPDATED CLEAN DATABASE SCRIPT
-- Database: scms_db
-- Matches the current Java entities and dynamic timetable flow.
-- ============================================================================

CREATE DATABASE IF NOT EXISTS scms_db;
USE scms_db;

SET FOREIGN_KEY_CHECKS = 0;
SET SQL_SAFE_UPDATES = 0;

-- ----------------------------------------------------------------------------
-- DROP TABLES (clean slate)
-- ----------------------------------------------------------------------------
DROP TABLE IF EXISTS timetable;
DROP TABLE IF EXISTS student;
DROP TABLE IF EXISTS teacher;
DROP TABLE IF EXISTS classroom;
DROP TABLE IF EXISTS fees;
DROP TABLE IF EXISTS admin;

-- ----------------------------------------------------------------------------
-- TABLE: admin
-- ----------------------------------------------------------------------------
CREATE TABLE admin (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100) DEFAULT 'Admin',
    email        VARCHAR(100) UNIQUE NOT NULL,
    password     VARCHAR(100) NOT NULL,
    college_name VARCHAR(200) DEFAULT 'AI Campus Institute',
    phone        VARCHAR(512) DEFAULT '',
    role         VARCHAR(50) DEFAULT 'Institute Admin'
);

-- ----------------------------------------------------------------------------
-- TABLE: classroom
-- Source of truth for class setup
-- ----------------------------------------------------------------------------
CREATE TABLE classroom (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    name             VARCHAR(100),
    course           VARCHAR(100),
    course_code      VARCHAR(50),
    department       VARCHAR(100),
    course_type      VARCHAR(50),
    duration         VARCHAR(50),
    total_semesters  INT DEFAULT NULL,
    year             VARCHAR(20) DEFAULT NULL,
    semester         INT DEFAULT NULL,
    section          VARCHAR(20) DEFAULT NULL,
    room             VARCHAR(50),
    batch            VARCHAR(100) DEFAULT NULL,
    batch_name       VARCHAR(100) DEFAULT NULL,
    batch_start_year INT DEFAULT NULL,
    batch_end_year   INT DEFAULT NULL,
    academic_year    VARCHAR(20) DEFAULT NULL,
    intake_capacity  INT DEFAULT NULL,
    shift            VARCHAR(20) DEFAULT NULL,
    total_fees       DOUBLE DEFAULT NULL,
    description      VARCHAR(2000) DEFAULT NULL,
    status           VARCHAR(20) DEFAULT 'Active',
    remarks          VARCHAR(2000) DEFAULT NULL,
    teacher_id       BIGINT DEFAULT NULL,
    admin_id         BIGINT DEFAULT NULL
);

-- ----------------------------------------------------------------------------
-- TABLE: teacher
-- ----------------------------------------------------------------------------
CREATE TABLE teacher (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    name                    VARCHAR(100),
    email                   VARCHAR(100) UNIQUE,
    password                VARCHAR(100),
    subject                 VARCHAR(100),
    photo                   VARCHAR(500) DEFAULT NULL,

    gender                  VARCHAR(20) DEFAULT NULL,
    dob                     DATE DEFAULT NULL,
    blood_group             VARCHAR(10) DEFAULT NULL,
    phone                   VARCHAR(512) DEFAULT NULL,
    alt_phone               VARCHAR(512) DEFAULT NULL,
    marital_status          VARCHAR(30) DEFAULT NULL,
    religion                VARCHAR(50) DEFAULT NULL,
    caste_name              VARCHAR(100) DEFAULT NULL,
    category                VARCHAR(50) DEFAULT NULL,

    address                 VARCHAR(500) DEFAULT NULL,
    permanent_address       VARCHAR(500) DEFAULT NULL,
    city                    VARCHAR(100) DEFAULT NULL,
    state                   VARCHAR(100) DEFAULT NULL,
    pin_code                VARCHAR(10) DEFAULT NULL,

    designation             VARCHAR(100) DEFAULT NULL,
    employee_id             VARCHAR(50) DEFAULT NULL,
    joining_date            DATE DEFAULT NULL,
    employment_type         VARCHAR(50) DEFAULT NULL,
    experience              VARCHAR(20) DEFAULT NULL,
    salary                  DOUBLE DEFAULT NULL,
    specialization          VARCHAR(150) DEFAULT NULL,
    department              VARCHAR(100) DEFAULT NULL,
    status                  VARCHAR(20) DEFAULT 'Active',
    academic_year           VARCHAR(20) DEFAULT NULL,

    qualification           VARCHAR(100) DEFAULT NULL,
    degree_specialization   VARCHAR(255) DEFAULT NULL,
    university              VARCHAR(200) DEFAULT NULL,
    year_of_passing         INT DEFAULT NULL,
    publications            INT DEFAULT NULL,

    aadhar_number           VARCHAR(512) DEFAULT NULL,
    pan_card_number         VARCHAR(512) DEFAULT NULL,
    voter_id                VARCHAR(512) DEFAULT NULL,
    passport_number         VARCHAR(512) DEFAULT NULL,

    bank_name               VARCHAR(150) DEFAULT NULL,
    bank_acc_no             VARCHAR(512) DEFAULT NULL,
    ifsc_code               VARCHAR(512) DEFAULT NULL,
    pf_number               VARCHAR(512) DEFAULT NULL,
    uan_number              VARCHAR(512) DEFAULT NULL,
    micr_number             VARCHAR(512) DEFAULT NULL,

    emergency_contact_name  VARCHAR(150) DEFAULT NULL,
    emergency_phone         VARCHAR(512) DEFAULT NULL,
    emergency_relation      VARCHAR(50) DEFAULT NULL,

    admin_id                BIGINT DEFAULT NULL,
    class_room_id           BIGINT DEFAULT NULL
);

-- ----------------------------------------------------------------------------
-- TABLE: student
-- ----------------------------------------------------------------------------
CREATE TABLE student (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    name              VARCHAR(100),
    email             VARCHAR(100) UNIQUE,
    password          VARCHAR(100),

    course            VARCHAR(100) DEFAULT NULL,
    semester          VARCHAR(20) DEFAULT NULL,
    academic_year     VARCHAR(20) DEFAULT NULL,
    degree            VARCHAR(50) DEFAULT NULL,
    section_name      VARCHAR(20) DEFAULT NULL,
    medium            VARCHAR(50) DEFAULT NULL,

    roll_no           VARCHAR(50) DEFAULT NULL,
    enrollment_no     VARCHAR(100) DEFAULT NULL,
    registration_no   VARCHAR(100) DEFAULT NULL,
    prn_number        VARCHAR(100) DEFAULT NULL,
    abc_number        VARCHAR(100) DEFAULT NULL,

    gender            VARCHAR(20) DEFAULT NULL,
    dob               DATE DEFAULT NULL,
    blood_group       VARCHAR(10) DEFAULT NULL,
    religion          VARCHAR(50) DEFAULT NULL,
    admission_date    DATE DEFAULT NULL,
    caste_name        VARCHAR(100) DEFAULT NULL,
    category          VARCHAR(50) DEFAULT NULL,
    photo             VARCHAR(500) DEFAULT NULL,

    father_name       VARCHAR(150) DEFAULT NULL,
    mother_name       VARCHAR(150) DEFAULT NULL,
    guardian_name     VARCHAR(150) DEFAULT NULL,

    aadhar_number     VARCHAR(512) DEFAULT NULL,
    pan_card_number   VARCHAR(512) DEFAULT NULL,
    voter_id          VARCHAR(512) DEFAULT NULL,
    eid_number        VARCHAR(512) DEFAULT NULL,

    bank_name         VARCHAR(150) DEFAULT NULL,
    bank_acc_no       VARCHAR(512) DEFAULT NULL,
    ifsc_code         VARCHAR(512) DEFAULT NULL,
    micr_number       VARCHAR(512) DEFAULT NULL,

    total_fees        DOUBLE DEFAULT 0,
    paid_fees         DOUBLE DEFAULT 0,
    pending_fees      DOUBLE DEFAULT 0,

    class_id          BIGINT DEFAULT NULL,
    admin_id          BIGINT DEFAULT NULL,
    UNIQUE KEY uq_student_admin_enrollment (admin_id, enrollment_no)
);

-- ----------------------------------------------------------------------------
-- TABLE: timetable
-- Dynamic custom start/end times
-- ----------------------------------------------------------------------------
CREATE TABLE timetable (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    day            VARCHAR(20),
    start_time     VARCHAR(20),
    end_time       VARCHAR(20),
    subject        VARCHAR(100),
    room           VARCHAR(50),
    entry_type     VARCHAR(20) DEFAULT 'LECTURE',
    academic_year  VARCHAR(20) DEFAULT NULL,
    slot           INT DEFAULT NULL,
    class_id       BIGINT DEFAULT NULL,
    teacher_id     BIGINT DEFAULT NULL,
    admin_id       BIGINT DEFAULT NULL
);

-- ----------------------------------------------------------------------------
-- TABLE: fees
-- ----------------------------------------------------------------------------
CREATE TABLE fees (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    course       VARCHAR(100),
    total_amount DOUBLE
);

CREATE TABLE course (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    duration_years INT DEFAULT NULL,
    total_semesters INT DEFAULT NULL,
    department VARCHAR(100) DEFAULT NULL,
    course_type VARCHAR(50) DEFAULT NULL,
    status VARCHAR(20) DEFAULT 'Active',
    admin_id BIGINT DEFAULT NULL
);

CREATE TABLE batch (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    display_name VARCHAR(120) NOT NULL,
    start_year INT DEFAULT NULL,
    end_year INT DEFAULT NULL,
    course_id BIGINT NOT NULL,
    admin_id BIGINT DEFAULT NULL
);

CREATE TABLE subject_master (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL,
    semester_number INT NOT NULL,
    credits DECIMAL(8,2) DEFAULT 4.00,
    category VARCHAR(255) DEFAULT NULL,
    status VARCHAR(50) DEFAULT 'active',
    created_at DATETIME DEFAULT NULL,
    admin_id BIGINT DEFAULT NULL
);

CREATE TABLE subject (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL,
    semester INT NOT NULL,
    course_category VARCHAR(255) DEFAULT NULL,
    credits DECIMAL(8,2) DEFAULT NULL,
    term VARCHAR(255) DEFAULT NULL,
    cycle VARCHAR(255) DEFAULT NULL,
    subject_master_id BIGINT DEFAULT NULL,
    course_id BIGINT NOT NULL,
    batch_id BIGINT NOT NULL,
    teacher_id BIGINT DEFAULT NULL,
    is_override BIT(1) DEFAULT b'0',
    status VARCHAR(50) DEFAULT 'active',
    created_at DATETIME DEFAULT NULL,
    admin_id BIGINT DEFAULT NULL
);

-- ----------------------------------------------------------------------------
-- FOREIGN KEYS
-- Added separately so the classroom <-> teacher circular relation works cleanly.
-- ----------------------------------------------------------------------------
ALTER TABLE classroom
    ADD CONSTRAINT fk_classroom_admin
        FOREIGN KEY (admin_id) REFERENCES admin(id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_classroom_teacher
        FOREIGN KEY (teacher_id) REFERENCES teacher(id)
        ON DELETE SET NULL;

ALTER TABLE teacher
    ADD CONSTRAINT fk_teacher_admin
        FOREIGN KEY (admin_id) REFERENCES admin(id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_teacher_classroom
        FOREIGN KEY (class_room_id) REFERENCES classroom(id)
        ON DELETE SET NULL;

ALTER TABLE student
    ADD CONSTRAINT fk_student_classroom
        FOREIGN KEY (class_id) REFERENCES classroom(id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_student_admin
        FOREIGN KEY (admin_id) REFERENCES admin(id)
        ON DELETE SET NULL;

ALTER TABLE timetable
    ADD CONSTRAINT fk_timetable_classroom
        FOREIGN KEY (class_id) REFERENCES classroom(id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_timetable_teacher
        FOREIGN KEY (teacher_id) REFERENCES teacher(id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_timetable_admin
        FOREIGN KEY (admin_id) REFERENCES admin(id)
        ON DELETE SET NULL;

-- ----------------------------------------------------------------------------
-- SAMPLE DATA
-- Keep this minimal because timetable is now fully dynamic.
-- ----------------------------------------------------------------------------
INSERT INTO admin (name, email, password, college_name, phone, role) VALUES
('Rohit', 'admin@gmail.com', 'admin123', 'ABC College of Technology', '9000000001', 'Institute Admin');

INSERT INTO fees (course, total_amount) VALUES
('MCA', 80000),
('BCA', 60000),
('BSc CS', 50000),
('MSc CS', 70000);

INSERT INTO teacher (
    name, email, password, subject, photo,
    gender, dob, blood_group, phone, alt_phone, marital_status, religion, caste_name, category,
    address, permanent_address, city, state, pin_code,
    designation, employee_id, joining_date, employment_type, experience, salary, specialization, department, status, academic_year,
    qualification, degree_specialization, university, year_of_passing, publications,
    aadhar_number, pan_card_number, voter_id, passport_number,
    bank_name, bank_acc_no, ifsc_code, pf_number, uan_number, micr_number,
    emergency_contact_name, emergency_phone, emergency_relation,
    admin_id
) VALUES
('Dr. Rajesh Kumar', 'rajesh.kumar@college.edu', 'teacher123', 'Java Programming', NULL,
 'Male', '1985-03-15', 'B+', '9876543201', '9876543210', 'Married', 'Hindu', 'Brahmin', 'General',
 '12, Shivaji Nagar, Pune', '12, Shivaji Nagar, Pune', 'Pune', 'Maharashtra', '411001',
 'Assistant Professor', 'EMP001', '2015-07-01', 'Permanent', '9 years', 65000, 'Software Engineering', 'Computer Science', 'Active', '2025-26',
 'M.Tech', 'Software Engineering', 'Mumbai University', 2012, 5,
 '234567890123', 'ABCPK1234R', 'MH/01/123456', 'M1234567',
 'SBI', '30012345678', 'SBIN0001234', 'PF001234', 'UAN001234', '123456789',
 'Sunita Kumar', '9876500001', 'Wife',
 1),

('Prof. Sneha Sharma', 'sneha.sharma@college.edu', 'teacher123', 'Database Management', NULL,
 'Female', '1988-07-22', 'O+', '9876543202', '9876543211', 'Married', 'Hindu', 'Kayastha', 'General',
 '45, Koregaon Park, Pune', '45, Koregaon Park, Pune', 'Pune', 'Maharashtra', '411002',
 'Associate Professor', 'EMP002', '2013-06-15', 'Permanent', '11 years', 75000, 'Database Systems', 'Information Technology', 'Active', '2025-26',
 'M.Tech', 'Database Systems', 'Pune University', 2010, 8,
 '345678901234', 'BCDPS2345S', 'MH/01/234567', 'M2345678',
 'HDFC', '50023456789', 'HDFC0001234', 'PF002345', 'UAN002345', '234567890',
 'Amit Sharma', '9876500002', 'Husband',
 1);

INSERT INTO classroom (
    name, course, course_code, department, course_type, duration, total_semesters,
    year, semester, section, room, batch, batch_name, batch_start_year, batch_end_year,
    academic_year, intake_capacity, shift, total_fees, description, status, remarks,
    teacher_id, admin_id
) VALUES
('MCA-FY-Sem1-A', 'MCA', 'MCA101', 'Computer Science', 'Postgraduate', '2 Years', 4,
 'FY', 1, 'A', '101', 'FYMCA', 'MCA 2025-2027', 2025, 2027,
 '2025-26', 60, 'Morning', 80000, 'First year MCA class', 'Active', 'Coordinator assigned', 1, 1),

('BCA-FY-Sem1-A', 'BCA', 'BCA101', 'Computer Science', 'Undergraduate', '3 Years', 6,
 'FY', 1, 'A', '201', 'FYBCA', 'BCA 2025-2028', 2025, 2028,
 '2025-26', 80, 'Morning', 60000, 'First year BCA class', 'Active', 'Coordinator assigned', 2, 1);

UPDATE teacher
SET class_room_id = (
    SELECT id
    FROM classroom c
    WHERE c.teacher_id = teacher.id
    LIMIT 1
);

INSERT INTO student (
    name, email, password, course, semester, academic_year, degree, section_name, medium,
    roll_no, enrollment_no, registration_no, prn_number, abc_number,
    gender, dob, blood_group, religion, admission_date, caste_name, category, photo,
    father_name, mother_name, guardian_name,
    aadhar_number, pan_card_number, voter_id, eid_number,
    bank_name, bank_acc_no, ifsc_code, micr_number,
    total_fees, paid_fees, pending_fees,
    class_id, admin_id
) VALUES
('Aman Sharma', 'aman@gmail.com', '123', 'MCA', '1', '2025-26', 'PG', 'A', 'English',
 'MCA2025001', 'ENR20250001', 'REG20250001', 'PRN00000001', 'ABC0000000001',
 'Male', '2002-05-14', 'B+', 'Hindu', '2025-07-15', 'Sharma', 'General', NULL,
 'Rajesh Sharma', 'Sunita Sharma', 'Rajesh Sharma',
 '234567891234', 'ABCPS1234A', 'MH/01/2345678', 'EID202500001',
 'SBI', '30012345600001', 'SBIN0001234', '400002001',
 80000, 0, 80000,
 1, 1),

('Riya Desai', 'riya@gmail.com', '123', 'BCA', '1', '2025-26', 'UG', 'A', 'English',
 'BCA2025002', 'ENR20250002', 'REG20250002', 'PRN00000002', 'ABC0000000002',
 'Female', '2004-07-19', 'AB+', 'Hindu', '2025-07-15', 'Desai', 'General', NULL,
 'Ramesh Desai', 'Meena Desai', 'Ramesh Desai',
 '567891234567', 'DEFPD4567D', 'MH/01/5678901', 'EID202500002',
 'ICICI', '70045678900004', 'ICIC0001234', '400002004',
 60000, 0, 60000,
 2, 1);

INSERT INTO timetable (
    day, start_time, end_time, subject, room, entry_type, academic_year, slot,
    class_id, teacher_id, admin_id
) VALUES
('Monday', '10:00', '11:00', 'Java Programming', '101', 'LECTURE', '2025-26', 1, 1, 1, 1),
('Monday', '11:00', '12:00', 'Database Management', '201', 'LECTURE', '2025-26', 2, 2, 2, 1),
('Monday', '13:00', '14:00', 'Lunch Break', NULL, 'BREAK', '2025-26', NULL, NULL, NULL, 1);

SET FOREIGN_KEY_CHECKS = 1;
SET SQL_SAFE_UPDATES = 1;

-- ----------------------------------------------------------------------------
-- QUICK CHECKS
-- ----------------------------------------------------------------------------
SELECT 'admin' AS tbl, COUNT(*) AS rows_count FROM admin
UNION ALL SELECT 'teacher', COUNT(*) FROM teacher
UNION ALL SELECT 'classroom', COUNT(*) FROM classroom
UNION ALL SELECT 'student', COUNT(*) FROM student
UNION ALL SELECT 'timetable', COUNT(*) FROM timetable
UNION ALL SELECT 'fees', COUNT(*) FROM fees;
