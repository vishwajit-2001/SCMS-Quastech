CREATE DATABASE IF NOT EXISTS scms_db1
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE scms_db1;

CREATE TABLE IF NOT EXISTS college (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(100) NOT NULL,
    address TEXT,
    logo_path TEXT,
    status VARCHAR(30),
    PRIMARY KEY (id),
    UNIQUE KEY uq_college_name (name),
    UNIQUE KEY uq_college_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS admin (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255),
    email VARCHAR(255),
    password VARCHAR(255),
    college_name VARCHAR(255),
    college_code VARCHAR(100),
    college_status VARCHAR(30),
    college_id BIGINT,
    phone VARCHAR(512),
    role VARCHAR(255),
    PRIMARY KEY (id),
    UNIQUE KEY uq_admin_email (email),
    KEY idx_admin_college_id (college_id),
    CONSTRAINT fk_admin_college
        FOREIGN KEY (college_id) REFERENCES college (id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS course (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    duration_years INT,
    total_semesters INT,
    department VARCHAR(255),
    course_type VARCHAR(255),
    status VARCHAR(50),
    admin_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_course_admin_code (admin_id, code),
    KEY idx_course_admin (admin_id),
    CONSTRAINT fk_course_admin
        FOREIGN KEY (admin_id) REFERENCES admin (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS batch (
    id BIGINT NOT NULL AUTO_INCREMENT,
    display_name VARCHAR(120) NOT NULL,
    start_year INT,
    end_year INT,
    course_id BIGINT NOT NULL,
    admin_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_batch_admin_course_display (admin_id, course_id, display_name),
    KEY idx_batch_course (course_id),
    KEY idx_batch_admin (admin_id),
    CONSTRAINT fk_batch_course
        FOREIGN KEY (course_id) REFERENCES course (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_batch_admin
        FOREIGN KEY (admin_id) REFERENCES admin (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS classroom (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255),
    course VARCHAR(255),
    course_code VARCHAR(255),
    department VARCHAR(255),
    course_type VARCHAR(255),
    duration VARCHAR(255),
    total_semesters INT,
    year VARCHAR(255),
    semester INT,
    section VARCHAR(255),
    room VARCHAR(255),
    batch VARCHAR(255),
    batch_name VARCHAR(255),
    batch_start_year INT,
    batch_end_year INT,
    academic_year VARCHAR(255),
    intake_capacity INT,
    shift VARCHAR(255),
    total_fees DECIMAL(12,2),
    description TEXT,
    status VARCHAR(255),
    remarks TEXT,
    admin_id BIGINT,
    PRIMARY KEY (id),
    KEY idx_classroom_admin (admin_id),
    CONSTRAINT fk_classroom_admin
        FOREIGN KEY (admin_id) REFERENCES admin (id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS teacher (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255),
    email VARCHAR(255),
    password VARCHAR(255),
    subject VARCHAR(255),
    photo VARCHAR(255),
    gender VARCHAR(255),
    dob DATE,
    blood_group VARCHAR(255),
    phone VARCHAR(512),
    alt_phone VARCHAR(512),
    marital_status VARCHAR(255),
    religion VARCHAR(255),
    caste_name VARCHAR(255),
    category VARCHAR(255),
    address VARCHAR(255),
    permanent_address VARCHAR(255),
    city VARCHAR(255),
    state VARCHAR(255),
    pin_code VARCHAR(255),
    designation VARCHAR(255),
    employee_id VARCHAR(255),
    joining_date DATE,
    experience VARCHAR(255),
    employment_type VARCHAR(255),
    salary DECIMAL(12,2),
    specialization VARCHAR(255),
    department VARCHAR(255),
    status VARCHAR(255),
    academic_year VARCHAR(255),
    qualification VARCHAR(255),
    degree_specialization VARCHAR(255),
    university VARCHAR(255),
    year_of_passing INT,
    publications INT,
    aadhar_number VARCHAR(512),
    pan_card_number VARCHAR(512),
    voter_id VARCHAR(512),
    passport_number VARCHAR(512),
    bank_name VARCHAR(255),
    bank_acc_no VARCHAR(512),
    ifsc_code VARCHAR(512),
    pf_number VARCHAR(512),
    uan_number VARCHAR(512),
    micr_number VARCHAR(512),
    emergency_contact_name VARCHAR(255),
    emergency_phone VARCHAR(512),
    emergency_relation VARCHAR(255),
    admin_id BIGINT,
    class_room_id BIGINT,
    PRIMARY KEY (id),
    UNIQUE KEY uq_teacher_email (email),
    KEY idx_teacher_admin (admin_id),
    KEY idx_teacher_class_room (class_room_id),
    CONSTRAINT fk_teacher_admin
        FOREIGN KEY (admin_id) REFERENCES admin (id)
        ON DELETE SET NULL,
    CONSTRAINT fk_teacher_class_room
        FOREIGN KEY (class_room_id) REFERENCES classroom (id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS academic_structure (
    id BIGINT NOT NULL AUTO_INCREMENT,
    year_label VARCHAR(20) NOT NULL,
    semester_number INT NOT NULL,
    section VARCHAR(10) NOT NULL,
    course_id BIGINT NOT NULL,
    batch_id BIGINT NOT NULL,
    admin_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_academic_structure_scope (
        admin_id, course_id, batch_id, year_label, semester_number, section
    ),
    KEY idx_academic_structure_course (course_id),
    KEY idx_academic_structure_batch (batch_id),
    KEY idx_academic_structure_admin (admin_id),
    CONSTRAINT fk_academic_structure_course
        FOREIGN KEY (course_id) REFERENCES course (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_academic_structure_batch
        FOREIGN KEY (batch_id) REFERENCES batch (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_academic_structure_admin
        FOREIGN KEY (admin_id) REFERENCES admin (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS subject_master (
    id BIGINT NOT NULL AUTO_INCREMENT,
    course_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL,
    semester_number INT NOT NULL,
    year_label VARCHAR(20),
    credits DECIMAL(8,2) DEFAULT 4.00,
    category VARCHAR(255),
    status VARCHAR(50),
    created_at DATETIME,
    teacher_id BIGINT,
    admin_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_subject_master_admin_course_sem_code (admin_id, course_id, semester_number, code),
    KEY idx_subject_master_course (course_id),
    KEY idx_subject_master_teacher (teacher_id),
    KEY idx_subject_master_admin (admin_id),
    CONSTRAINT fk_subject_master_course
        FOREIGN KEY (course_id) REFERENCES course (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_subject_master_teacher
        FOREIGN KEY (teacher_id) REFERENCES teacher (id)
        ON DELETE SET NULL,
    CONSTRAINT fk_subject_master_admin
        FOREIGN KEY (admin_id) REFERENCES admin (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS subject (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL,
    semester INT NOT NULL,
    course_category VARCHAR(255),
    credits DECIMAL(8,2),
    term VARCHAR(255),
    cycle VARCHAR(255),
    subject_master_id BIGINT,
    course_id BIGINT NOT NULL,
    batch_id BIGINT NOT NULL,
    teacher_id BIGINT,
    is_override BIT(1) DEFAULT b'0',
    status VARCHAR(50),
    created_at DATETIME,
    admin_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_subject_admin_course_batch_sem_code (
        admin_id, course_id, batch_id, semester, code
    ),
    KEY idx_subject_master (subject_master_id),
    KEY idx_subject_course (course_id),
    KEY idx_subject_batch (batch_id),
    KEY idx_subject_teacher (teacher_id),
    KEY idx_subject_admin (admin_id),
    CONSTRAINT fk_subject_master_ref
        FOREIGN KEY (subject_master_id) REFERENCES subject_master (id)
        ON DELETE SET NULL,
    CONSTRAINT fk_subject_course
        FOREIGN KEY (course_id) REFERENCES course (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_subject_batch
        FOREIGN KEY (batch_id) REFERENCES batch (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_subject_teacher
        FOREIGN KEY (teacher_id) REFERENCES teacher (id)
        ON DELETE SET NULL,
    CONSTRAINT fk_subject_admin
        FOREIGN KEY (admin_id) REFERENCES admin (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS teacher_academic_mapping (
    id BIGINT NOT NULL AUTO_INCREMENT,
    teacher_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    batch_id BIGINT NOT NULL,
    academic_year VARCHAR(20) NOT NULL,
    semester INT,
    section VARCHAR(20),
    subject_id BIGINT,
    room_number VARCHAR(255),
    admin_id BIGINT NOT NULL,
    created_at DATETIME,
    updated_at DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_teacher_academic_scope (
        teacher_id, course_id, batch_id, academic_year
    ),
    KEY idx_teacher_mapping_teacher (teacher_id),
    KEY idx_teacher_mapping_course (course_id),
    KEY idx_teacher_mapping_batch (batch_id),
    KEY idx_teacher_mapping_subject (subject_id),
    KEY idx_teacher_mapping_admin (admin_id),
    CONSTRAINT fk_teacher_mapping_teacher
        FOREIGN KEY (teacher_id) REFERENCES teacher (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_teacher_mapping_course
        FOREIGN KEY (course_id) REFERENCES course (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_teacher_mapping_batch
        FOREIGN KEY (batch_id) REFERENCES batch (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_teacher_mapping_subject
        FOREIGN KEY (subject_id) REFERENCES subject (id)
        ON DELETE SET NULL,
    CONSTRAINT fk_teacher_mapping_admin
        FOREIGN KEY (admin_id) REFERENCES admin (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS fees (
    id BIGINT NOT NULL AUTO_INCREMENT,
    course VARCHAR(255),
    batch_name VARCHAR(255),
    batch_id BIGINT,
    academic_year VARCHAR(255),
    fee_scope VARCHAR(255),
    semester VARCHAR(255),
    total_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    admin_id BIGINT,
    PRIMARY KEY (id),
    KEY idx_fees_batch (batch_id),
    KEY idx_fees_admin (admin_id),
    CONSTRAINT fk_fees_batch
        FOREIGN KEY (batch_id) REFERENCES batch (id)
        ON DELETE SET NULL,
    CONSTRAINT fk_fees_admin
        FOREIGN KEY (admin_id) REFERENCES admin (id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS student (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255),
    email VARCHAR(255),
    password VARCHAR(255),
    mobile_number VARCHAR(512),
    title TEXT,
    first_name TEXT,
    middle_name TEXT,
    last_name TEXT,
    name_as_per_aadhaar TEXT,
    lname_as_12th_std TEXT,
    fname_as_12th_std TEXT,
    mname_as_12th_std TEXT,
    phone_number TEXT,
    marital_status TEXT,
    mother_tongue TEXT,
    native_place TEXT,
    birth_place TEXT,
    birth_country TEXT,
    region TEXT,
    nationality TEXT,
    category_type TEXT,
    caste_category TEXT,
    sub_caste TEXT,
    father_occupation TEXT,
    father_qualification TEXT,
    mother_qualification TEXT,
    total_family_member INT,
    family_annual_income DECIMAL(12,2),
    differently_abled BIT(1),
    sports_person BIT(1),
    sports_achievement TEXT,
    hobbies TEXT,
    university_pre_adm_reg_no TEXT,
    no_of_attempt INT,
    inhouse TEXT,
    medium_of_instruction TEXT,
    social_reservation TEXT,
    academic_bank_of_credits TEXT,
    course VARCHAR(255),
    semester VARCHAR(255),
    academic_year VARCHAR(255),
    degree VARCHAR(255),
    section_name VARCHAR(255),
    medium VARCHAR(255),
    roll_no VARCHAR(255),
    enrollment_no VARCHAR(255),
    registration_no VARCHAR(255),
    prn_number VARCHAR(255),
    abc_number VARCHAR(255),
    gender VARCHAR(255),
    dob DATE,
    blood_group VARCHAR(255),
    religion VARCHAR(255),
    admission_date DATE,
    caste_name VARCHAR(255),
    category VARCHAR(255),
    father_name VARCHAR(255),
    mother_name VARCHAR(255),
    guardian_name VARCHAR(255),
    aadhar_number VARCHAR(512),
    pan_card_number VARCHAR(512),
    voter_id VARCHAR(512),
    eid_number VARCHAR(512),
    bank_name VARCHAR(255),
    bank_acc_no VARCHAR(512),
    ifsc_code VARCHAR(512),
    micr_number VARCHAR(512),
    photo VARCHAR(255),
    signature TEXT,
    current_address TEXT,
    permanent_address TEXT,
    total_fees DECIMAL(12,2),
    paid_fees DECIMAL(12,2),
    pending_fees DECIMAL(12,2),
    admission_status VARCHAR(255),
    program_level VARCHAR(255),
    onboarding_completed BIT(1),
    class_id BIGINT,
    batch_id BIGINT,
    admin_id BIGINT,
    PRIMARY KEY (id),
    UNIQUE KEY uq_student_email (email),
    UNIQUE KEY uq_student_mobile_number (mobile_number),
    UNIQUE KEY uq_student_admin_enrollment (admin_id, enrollment_no),
    KEY idx_student_class (class_id),
    KEY idx_student_batch (batch_id),
    KEY idx_student_admin (admin_id),
    CONSTRAINT fk_student_class
        FOREIGN KEY (class_id) REFERENCES classroom (id)
        ON DELETE SET NULL,
    CONSTRAINT fk_student_batch
        FOREIGN KEY (batch_id) REFERENCES batch (id)
        ON DELETE SET NULL,
    CONSTRAINT fk_student_admin
        FOREIGN KEY (admin_id) REFERENCES admin (id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS timetable (
    id BIGINT NOT NULL AUTO_INCREMENT,
    day VARCHAR(255),
    start_time VARCHAR(255),
    end_time VARCHAR(255),
    subject VARCHAR(255),
    room VARCHAR(255),
    entry_type VARCHAR(255),
    academic_year VARCHAR(255),
    slot INT,
    class_id BIGINT,
    teacher_id BIGINT,
    admin_id BIGINT,
    PRIMARY KEY (id),
    KEY idx_timetable_class (class_id),
    KEY idx_timetable_teacher (teacher_id),
    KEY idx_timetable_admin (admin_id),
    CONSTRAINT fk_timetable_class
        FOREIGN KEY (class_id) REFERENCES classroom (id)
        ON DELETE SET NULL,
    CONSTRAINT fk_timetable_teacher
        FOREIGN KEY (teacher_id) REFERENCES teacher (id)
        ON DELETE SET NULL,
    CONSTRAINT fk_timetable_admin
        FOREIGN KEY (admin_id) REFERENCES admin (id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS study_material (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255),
    description VARCHAR(1000),
    subject VARCHAR(255),
    course VARCHAR(255),
    semester VARCHAR(255),
    section VARCHAR(255),
    academic_year VARCHAR(255),
    original_file_name VARCHAR(255),
    stored_file_name VARCHAR(255),
    file_path VARCHAR(255),
    content_type VARCHAR(255),
    file_size BIGINT,
    uploaded_at DATETIME,
    teacher_id BIGINT,
    class_room_id BIGINT,
    admin_id BIGINT,
    PRIMARY KEY (id),
    KEY idx_study_material_teacher (teacher_id),
    KEY idx_study_material_class_room (class_room_id),
    KEY idx_study_material_admin (admin_id),
    CONSTRAINT fk_study_material_teacher
        FOREIGN KEY (teacher_id) REFERENCES teacher (id)
        ON DELETE SET NULL,
    CONSTRAINT fk_study_material_class_room
        FOREIGN KEY (class_room_id) REFERENCES classroom (id)
        ON DELETE SET NULL,
    CONSTRAINT fk_study_material_admin
        FOREIGN KEY (admin_id) REFERENCES admin (id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS campus_event (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255),
    organizer_name VARCHAR(255),
    organization_name VARCHAR(255),
    venue VARCHAR(255),
    category VARCHAR(255),
    description TEXT,
    event_date_time DATETIME,
    registration_deadline DATETIME,
    target_audience VARCHAR(255),
    published BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME,
    teacher_id BIGINT,
    PRIMARY KEY (id),
    KEY idx_campus_event_teacher (teacher_id),
    CONSTRAINT fk_campus_event_teacher
        FOREIGN KEY (teacher_id) REFERENCES teacher (id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS campus_event_application (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id BIGINT,
    student_id BIGINT,
    full_name VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(255),
    course VARCHAR(255),
    semester VARCHAR(255),
    specialization VARCHAR(255),
    section_name VARCHAR(255),
    notes VARCHAR(1500),
    applied_at DATETIME,
    PRIMARY KEY (id),
    KEY idx_campus_event_application_event (event_id),
    KEY idx_campus_event_application_student (student_id),
    CONSTRAINT fk_campus_event_application_event
        FOREIGN KEY (event_id) REFERENCES campus_event (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_campus_event_application_student
        FOREIGN KEY (student_id) REFERENCES student (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS placement_tpo (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255),
    email VARCHAR(255),
    password VARCHAR(255),
    college_name VARCHAR(255),
    college_id BIGINT,
    admin_id BIGINT,
    phone VARCHAR(255),
    department VARCHAR(255),
    employee_id VARCHAR(255),
    designation VARCHAR(255),
    status VARCHAR(255),
    PRIMARY KEY (id),
    UNIQUE KEY uq_placement_tpo_email (email),
    KEY idx_placement_tpo_college (college_id),
    KEY idx_placement_tpo_admin (admin_id),
    CONSTRAINT fk_placement_tpo_college
        FOREIGN KEY (college_id) REFERENCES college (id),
    CONSTRAINT fk_placement_tpo_admin
        FOREIGN KEY (admin_id) REFERENCES admin (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS placement_drive (
    id BIGINT NOT NULL AUTO_INCREMENT,
    company_name VARCHAR(255),
    job_title VARCHAR(255),
    package_offered VARCHAR(255),
    location VARCHAR(255),
    drive_type VARCHAR(255),
    experience VARCHAR(255),
    salary_range VARCHAR(255),
    openings INT,
    eligibility_course VARCHAR(255),
    eligibility_semester VARCHAR(255),
    eligibility_degree VARCHAR(255),
    skills_required VARCHAR(255),
    description TEXT,
    drive_date DATE,
    last_apply_date DATE,
    application_deadline DATETIME,
    published BIT(1) NOT NULL DEFAULT b'0',
    created_at DATETIME,
    tpo_id BIGINT,
    PRIMARY KEY (id),
    KEY idx_placement_drive_tpo (tpo_id),
    CONSTRAINT fk_placement_drive_tpo
        FOREIGN KEY (tpo_id) REFERENCES placement_tpo (id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS placement_application (
    id BIGINT NOT NULL AUTO_INCREMENT,
    drive_id BIGINT,
    student_id BIGINT,
    full_name VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(255),
    course VARCHAR(255),
    semester VARCHAR(255),
    specialization VARCHAR(255),
    current_location VARCHAR(255),
    skills VARCHAR(1000),
    cover_note TEXT,
    resume_path VARCHAR(255),
    resume_name VARCHAR(255),
    status VARCHAR(255),
    applied_at DATETIME,
    PRIMARY KEY (id),
    KEY idx_placement_application_drive (drive_id),
    KEY idx_placement_application_student (student_id),
    CONSTRAINT fk_placement_application_drive
        FOREIGN KEY (drive_id) REFERENCES placement_drive (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_placement_application_student
        FOREIGN KEY (student_id) REFERENCES student (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS placement_interview (
    id BIGINT NOT NULL AUTO_INCREMENT,
    drive_id BIGINT,
    tpo_id BIGINT,
    interview_round VARCHAR(255),
    interview_date_time DATETIME,
    venue VARCHAR(255),
    meeting_link VARCHAR(255),
    notes TEXT,
    status VARCHAR(255),
    created_at DATETIME,
    PRIMARY KEY (id),
    KEY idx_placement_interview_drive (drive_id),
    KEY idx_placement_interview_tpo (tpo_id),
    CONSTRAINT fk_placement_interview_drive
        FOREIGN KEY (drive_id) REFERENCES placement_drive (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_placement_interview_tpo
        FOREIGN KEY (tpo_id) REFERENCES placement_tpo (id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS placement_interview_student (
    interview_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    PRIMARY KEY (interview_id, student_id),
    KEY idx_placement_interview_student_student (student_id),
    CONSTRAINT fk_placement_interview_student_interview
        FOREIGN KEY (interview_id) REFERENCES placement_interview (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_placement_interview_student_student
        FOREIGN KEY (student_id) REFERENCES student (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
