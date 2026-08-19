from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import ListFlowable, ListItem, Paragraph, SimpleDocTemplate, Spacer


OUTPUT_PATH = r"D:\internship\scms-project\scms-project\updated_project_details.pdf"


def bullet_list(items, style):
    return ListFlowable(
        [ListItem(Paragraph(item, style)) for item in items],
        bulletType="bullet",
        start="-",
        leftIndent=16,
        bulletFontName="Helvetica",
        bulletFontSize=9,
    )


styles = getSampleStyleSheet()
styles.add(
    ParagraphStyle(
        name="TitleCenter",
        parent=styles["Title"],
        alignment=TA_CENTER,
        fontName="Helvetica-Bold",
        fontSize=17,
        leading=20,
        textColor=colors.HexColor("#12355B"),
        spaceAfter=8,
    )
)
styles.add(
    ParagraphStyle(
        name="SubTitleCenter",
        parent=styles["Normal"],
        alignment=TA_CENTER,
        fontName="Helvetica",
        fontSize=10,
        leading=13,
        textColor=colors.HexColor("#465A69"),
        spaceAfter=8,
    )
)
styles.add(
    ParagraphStyle(
        name="SectionHeading",
        parent=styles["Heading1"],
        fontName="Helvetica-Bold",
        fontSize=13,
        leading=16,
        textColor=colors.HexColor("#12355B"),
        spaceBefore=8,
        spaceAfter=6,
    )
)
styles.add(
    ParagraphStyle(
        name="SubHeading",
        parent=styles["Heading2"],
        fontName="Helvetica-Bold",
        fontSize=10.5,
        leading=13,
        textColor=colors.HexColor("#214E6E"),
        spaceBefore=6,
        spaceAfter=4,
    )
)
styles.add(
    ParagraphStyle(
        name="Body",
        parent=styles["Normal"],
        fontName="Helvetica",
        fontSize=9.5,
        leading=13,
        spaceAfter=4,
    )
)


story = [
    Spacer(1, 6 * mm),
    Paragraph("AI-Powered Smart Campus Management System", styles["TitleCenter"]),
    Paragraph("Updated Project Details Based on Current Implementation", styles["SubTitleCenter"]),
    Paragraph("Project: SCMS", styles["SubTitleCenter"]),
    Paragraph("Updated on: 23 May 2026", styles["SubTitleCenter"]),
    Spacer(1, 2 * mm),
    Paragraph("Group Members (Group A)", styles["SubHeading"]),
    bullet_list(
        [
            "Vishwajit Gadale",
            "Amar Bharati",
            "Aakash Vishwakarma",
        ],
        styles["Body"],
    ),
    Spacer(1, 2 * mm),
    Paragraph("Project Overview", styles["SectionHeading"]),
    Paragraph(
        "SCMS is a centralized web-based campus management platform for multi-college academic operations. "
        "The current implementation is a monolithic Spring Boot application with separate role-based portals "
        "for Super Admin, College Admin, Teacher, Student, and Placement TPO users.",
        styles["Body"],
    ),
    Paragraph(
        "The system manages student onboarding, academic structure, classroom operations, attendance, "
        "assignments, study materials, fees, exams, events, placement workflows, notifications, and "
        "tenant-level administration from a single application.",
        styles["Body"],
    ),
    Paragraph("Key Feature Modules", styles["SectionHeading"]),
    Paragraph("Student Module", styles["SubHeading"]),
    bullet_list(
        [
            "College-wise registration and login",
            "Detailed onboarding workflow with personal, academic, address, and document sections",
            "Timetable, subjects, assignments, study materials, fees, placements, events, and notifications",
            "Assignment submission and downloadable academic resources",
            "Exam and quiz participation with result visibility",
        ],
        styles["Body"],
    ),
    Paragraph("Teacher Module", styles["SubHeading"]),
    bullet_list(
        [
            "Teacher dashboard with assigned students and timetable visibility",
            "Attendance session creation and status updates",
            "Assignment publishing with file upload and submission review",
            "Study material upload and class resource management",
            "Standard exam scheduling and PDF-based auto-generated quiz creation",
        ],
        styles["Body"],
    ),
    Paragraph("Admin Module", styles["SubHeading"]),
    bullet_list(
        [
            "Student, teacher, course, batch, class, subject, and academic structure management",
            "Fee setup and operational reports",
            "Placement drive, TPO, application, and interview management",
            "Import/export support for operational data and spreadsheets",
            "Portal notifications and document review handling",
        ],
        styles["Body"],
    ),
    Paragraph("Super Admin and Platform Services", styles["SubHeading"]),
    bullet_list(
        [
            "College lifecycle management with active, inactive, and setup-pending states",
            "College-admin provisioning, communication center, notifications, and audit logs",
            "College-wise portal isolation using college codes",
            "Role-aware chatbot support for student and teacher self-service queries",
            "Sensitive field encryption support for Aadhaar, bank, and related data",
        ],
        styles["Body"],
    ),
    Spacer(1, 2 * mm),
    Paragraph("Updated Tech Architecture", styles["SectionHeading"]),
    bullet_list(
        [
            "Backend framework: Spring Boot 3.2.3 on Java 17",
            "Server-side web layer: Spring MVC with Thymeleaf templates",
            "Persistence: Spring Data JPA with MySQL",
            "Security model: BCrypt password hashing with session-based role access",
            "Utilities: Apache POI for Excel export/import, PDFBox for PDF text extraction, JavaMail for email support",
            "Frontend delivery: HTML, CSS, JavaScript, Thymeleaf-rendered portals",
            "Static file handling for photos, signatures, resumes, exam files, assignments, and student documents",
        ],
        styles["Body"],
    ),
    Paragraph("BRS - Business Requirement Specification", styles["SectionHeading"]),
    Paragraph("1.1 Purpose", styles["SubHeading"]),
    Paragraph(
        "To provide a centralized campus platform that reduces manual coordination across colleges and improves "
        "student, academic, and administrative visibility through one integrated web system.",
        styles["Body"],
    ),
    Paragraph("1.2 Business Objectives", styles["SubHeading"]),
    bullet_list(
        [
            "Digitize student admission-to-portal onboarding workflows",
            "Reduce operational work for admins, teachers, and placement teams",
            "Standardize class, course, batch, and subject management",
            "Improve communication using in-app notifications and college-level portals",
            "Support multi-college oversight through a super admin control layer",
        ],
        styles["Body"],
    ),
    Paragraph("1.3 Stakeholders", styles["SubHeading"]),
    bullet_list(
        [
            "Super Admin",
            "College Management",
            "College Admin Staff",
            "Teachers",
            "Students",
            "Placement TPO",
        ],
        styles["Body"],
    ),
    Paragraph("1.4 Business Scope", styles["SubHeading"]),
    Paragraph("In Scope", styles["SubHeading"]),
    bullet_list(
        [
            "College onboarding and status control",
            "Student, teacher, TPO, course, batch, subject, and classroom management",
            "Attendance, assignments, study materials, exams, fees, events, and placements",
            "Student document collection and onboarding verification",
            "Notifications, chatbot assistance, audit logs, and operational exports",
        ],
        styles["Body"],
    ),
    Paragraph("Out of Scope", styles["SubHeading"]),
    bullet_list(
        [
            "Biometric device integration",
            "Native mobile applications",
            "External government or university API integrations",
            "Dedicated microservice deployment in the current version",
        ],
        styles["Body"],
    ),
    Paragraph("SRS - Software Requirement Specification", styles["SectionHeading"]),
    Paragraph("2.1 Introduction", styles["SubHeading"]),
    Paragraph(
        "This document captures the current implemented scope and aligned requirements for the SCMS platform.",
        styles["Body"],
    ),
    Paragraph("2.2 Overall System Description", styles["SubHeading"]),
    bullet_list(
        [
            "Product type: browser-based enterprise web application",
            "Deployment style: single Spring Boot application",
            "User classes: Super Admin, Admin, Teacher, Student, Placement TPO",
            "Access model: college-code-aware portal entry with session-backed role routing",
        ],
        styles["Body"],
    ),
    Paragraph("2.3 System Features (High Level)", styles["SubHeading"]),
    bullet_list(
        [
            "Authentication, password protection, and role-aware session access",
            "Student onboarding and student lifecycle management",
            "Academic structure and timetable operations",
            "Assignment, material, exam, and quiz workflows",
            "Fee, placement, event, notification, and reporting modules",
            "Super admin oversight and chatbot support",
        ],
        styles["Body"],
    ),
    Spacer(1, 2 * mm),
    Paragraph("Functional Requirements (FRS)", styles["SectionHeading"]),
    Paragraph("3.1 User and Access Management", styles["SubHeading"]),
    bullet_list(
        [
            "FR1: System shall allow student self-registration through college-specific registration links.",
            "FR2: System shall support login for Admin, Teacher, Student, Super Admin, and Placement TPO users.",
            "FR3: System shall maintain session-based access control for protected routes.",
            "FR4: System shall store passwords using BCrypt hashing and support legacy password migration.",
        ],
        styles["Body"],
    ),
    Paragraph("3.2 College and Administration Management", styles["SubHeading"]),
    bullet_list(
        [
            "FR5: Super Admin shall manage colleges and college activation status.",
            "FR6: Super Admin shall manage college-admin communication and audit logs.",
            "FR7: Admin shall manage courses, batches, subjects, classrooms, and academic structure.",
            "FR8: Admin shall manage students, teachers, and placement TPO accounts.",
        ],
        styles["Body"],
    ),
    Paragraph("3.3 Student Lifecycle and Onboarding", styles["SubHeading"]),
    bullet_list(
        [
            "FR9: Students shall complete a multi-section onboarding form after registration.",
            "FR10: System shall store student profile, address, academic, and uploaded document data.",
            "FR11: Admin shall review or request re-upload of student documents.",
            "FR12: Students shall view profile, subjects, timetable, and notifications from their portal.",
        ],
        styles["Body"],
    ),
    Paragraph("3.4 Teaching and Academic Operations", styles["SubHeading"]),
    bullet_list(
        [
            "FR13: Teachers shall mark attendance for timetable-linked sessions.",
            "FR14: Teachers shall upload assignments and study materials.",
            "FR15: Students shall submit assignments and access study materials.",
            "FR16: System shall maintain class-wise timetable visibility for students and teachers.",
        ],
        styles["Body"],
    ),
    Paragraph("3.5 Exam and Quiz Management", styles["SubHeading"]),
    bullet_list(
        [
            "FR17: Teachers shall schedule standard exams with instructions, topics, files, and room details.",
            "FR18: System shall create quiz exams by extracting text from uploaded PDF notes.",
            "FR19: System shall store quiz questions, attempts, and responses.",
            "FR20: Students shall view upcoming exams, attempt quizzes, and review results where available.",
        ],
        styles["Body"],
    ),
    Paragraph("3.6 Fees, Placements, and Events", styles["SubHeading"]),
    bullet_list(
        [
            "FR21: Admin shall define fee records and students shall view fee summaries.",
            "FR22: Placement TPO shall manage drives, applications, interviews, analytics, and reports.",
            "FR23: System shall export placement applications to Excel and selected resumes to ZIP.",
            "FR24: Students shall access event and placement information from the portal.",
        ],
        styles["Body"],
    ),
    Paragraph("3.7 Notifications and Assistance", styles["SubHeading"]),
    bullet_list(
        [
            "FR25: System shall support in-app notifications across roles.",
            "FR26: Super Admin shall send messages to college admins.",
            "FR27: Chatbot shall answer role-aware questions for timetable, fees, assignments, and profile context.",
        ],
        styles["Body"],
    ),
    Spacer(1, 2 * mm),
    Paragraph("Non-Functional Requirements (NFR)", styles["SectionHeading"]),
    Paragraph("4.1 Performance", styles["SubHeading"]),
    bullet_list(
        [
            "NFR1: Common dashboard and list views should load within reasonable web response times under normal campus usage.",
            "NFR2: File upload and export operations shall handle moderate academic-office workloads.",
        ],
        styles["Body"],
    ),
    Paragraph("4.2 Security", styles["SubHeading"]),
    bullet_list(
        [
            "NFR3: Passwords must be encrypted using BCrypt.",
            "NFR4: Sensitive fields shall support encryption using the configured application secret.",
            "NFR5: Protected routes shall enforce session-aware role validation.",
            "NFR6: File access paths shall be normalized to avoid unsafe path traversal.",
        ],
        styles["Body"],
    ),
    Paragraph("4.3 Maintainability", styles["SubHeading"]),
    bullet_list(
        [
            "NFR7: System shall remain modular through controllers, services, repositories, and model entities.",
            "NFR8: Schema evolution shall be supportable through runner-based migration utilities and normalized entities.",
            "NFR9: UI templates shall remain separated by role for maintainable portal updates.",
        ],
        styles["Body"],
    ),
    Paragraph("4.4 Reliability and Operations", styles["SubHeading"]),
    bullet_list(
        [
            "NFR10: Uploaded academic files and portal assets shall remain accessible through managed static storage.",
            "NFR11: Audit and notification records shall preserve administrative activity history.",
            "NFR12: The application shall support development-time hot reload for faster maintenance cycles.",
        ],
        styles["Body"],
    ),
    Paragraph("Technical Requirements", styles["SectionHeading"]),
    Paragraph("Backend", styles["SubHeading"]),
    bullet_list(
        [
            "Java 17",
            "Spring Boot",
            "Spring MVC",
            "Spring Data JPA / Hibernate",
            "Spring Security",
            "Maven",
        ],
        styles["Body"],
    ),
    Paragraph("Database", styles["SubHeading"]),
    bullet_list(
        [
            "MySQL",
            "Normalized relational schema",
            "Entity relationships for colleges, admins, teachers, students, academics, exams, and placements",
        ],
        styles["Body"],
    ),
    Paragraph("Frontend", styles["SubHeading"]),
    bullet_list(
        [
            "Thymeleaf templates",
            "HTML/CSS/JavaScript",
            "Role-specific dashboards and portal screens",
        ],
        styles["Body"],
    ),
    Paragraph("Document and File Utilities", styles["SubHeading"]),
    bullet_list(
        [
            "Apache POI",
            "Apache PDFBox",
            "Multipart file handling for uploads and exports",
        ],
        styles["Body"],
    ),
    Spacer(1, 2 * mm),
    Paragraph("High-Level Architecture", styles["SectionHeading"]),
    Paragraph(
        "The current system follows a single-application modular architecture. Role-specific controllers handle "
        "web requests, services implement core business logic, repositories manage persistence, and Thymeleaf "
        "templates deliver the role portals.",
        styles["Body"],
    ),
    bullet_list(
        [
            "Presentation layer: Thymeleaf pages, static CSS, and JavaScript",
            "Controller layer: Main, Admin, Student, Teacher, Placement, Super Admin, Event, and Chatbot controllers",
            "Service layer: academic structure, password protection, employee IDs, chatbot, and exam automation",
            "Persistence layer: JPA repositories and MySQL-backed entities",
            "Storage layer: local static upload directories for academic and profile documents",
        ],
        styles["Body"],
    ),
    Paragraph("Core Database Tables / Entities", styles["SectionHeading"]),
    bullet_list(
        [
            "college",
            "admin",
            "student",
            "teacher",
            "placement_tpo",
            "course",
            "batch",
            "academic_structure",
            "subject and subject_master",
            "classroom and timetable",
            "teacher_attendance_session and teacher_attendance_entry",
            "assignment and assignment_submission",
            "study_material",
            "fees",
            "exam_session, exam_attendance, exam_quiz_question, exam_quiz_attempt, exam_quiz_response",
            "campus_event and campus_event_application",
            "placement_drive, placement_application, placement_interview",
            "portal_notification and super_admin_audit_log",
            "student_document",
        ],
        styles["Body"],
    ),
    Paragraph("Updated Team Division", styles["SectionHeading"]),
    bullet_list(
        [
            "Member 1 - Core backend, authentication flows, repositories, and security-related logic",
            "Member 2 - Academic operations, exams, fees, placements, and admin workflows",
            "Member 3 - Thymeleaf UI, dashboards, role portals, and integration/testing support",
        ],
        styles["Body"],
    ),
    Paragraph(
        "This revised project details document reflects the current implemented SCMS project rather than the earlier planned stack.",
        styles["Body"],
    ),
]


def add_page_number(canvas, doc):
    canvas.saveState()
    canvas.setFont("Helvetica", 8)
    canvas.setFillColor(colors.HexColor("#666666"))
    canvas.drawRightString(doc.pagesize[0] - 18 * mm, 10 * mm, f"Page {doc.page}")
    canvas.restoreState()


doc = SimpleDocTemplate(
    OUTPUT_PATH,
    pagesize=A4,
    leftMargin=18 * mm,
    rightMargin=18 * mm,
    topMargin=16 * mm,
    bottomMargin=16 * mm,
)
doc.build(story, onFirstPage=add_page_number, onLaterPages=add_page_number)
print(OUTPUT_PATH)
