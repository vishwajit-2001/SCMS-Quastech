from reportlab.lib import colors
from reportlab.lib.pagesizes import LETTER
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import inch
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak


OUT = "SCMS_Interview_Preparation_Guide.pdf"


styles = getSampleStyleSheet()
styles.add(ParagraphStyle("TitleMain", parent=styles["Title"], fontSize=21, leading=26, textColor=colors.HexColor("#0B2545"), spaceAfter=10))
styles.add(ParagraphStyle("H1x", parent=styles["Heading1"], fontSize=15, leading=19, textColor=colors.HexColor("#2E74B5"), spaceBefore=14, spaceAfter=7))
styles.add(ParagraphStyle("H2x", parent=styles["Heading2"], fontSize=12, leading=15, textColor=colors.HexColor("#1F4D78"), spaceBefore=10, spaceAfter=5))
styles.add(ParagraphStyle("Bodyx", parent=styles["BodyText"], fontSize=9.4, leading=12.4, spaceAfter=5))
styles.add(ParagraphStyle("Bulletx", parent=styles["BodyText"], fontSize=9.2, leading=12, leftIndent=14, firstLineIndent=-8, spaceAfter=3))
styles.add(ParagraphStyle("Qx", parent=styles["BodyText"], fontSize=9.5, leading=12.5, textColor=colors.HexColor("#1F4D78"), spaceBefore=6, spaceAfter=2))


def p(text):
    return Paragraph(text, styles["Bodyx"])


def h1(text):
    return Paragraph(text, styles["H1x"])


def h2(text):
    return Paragraph(text, styles["H2x"])


def b(text):
    return Paragraph("• " + text, styles["Bulletx"])


def q(question, answer):
    return [Paragraph("<b>Q. " + question + "</b>", styles["Qx"]), p("<b>A.</b> " + answer)]


def table(headers, rows, widths):
    data = [[Paragraph("<b>" + h + "</b>", styles["Bodyx"]) for h in headers]]
    for row in rows:
        data.append([Paragraph(str(cell), styles["Bodyx"]) for cell in row])
    t = Table(data, colWidths=widths, repeatRows=1)
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#E8EEF5")),
        ("GRID", (0, 0), (-1, -1), 0.35, colors.HexColor("#B8C2CC")),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 5),
        ("RIGHTPADDING", (0, 0), (-1, -1), 5),
        ("TOPPADDING", (0, 0), (-1, -1), 4),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 4),
    ]))
    return t


story = []
story.append(Paragraph("SCMS Project Interview Preparation Guide", styles["TitleMain"]))
story.append(p("<i>Student College Management System | Technical Architecture, Database, APIs, Schema, Security, and Google-style Interview Q&amp;A</i>"))
story.append(Spacer(1, 0.12 * inch))

story += [
    h1("1. Project Overview"),
    p("SCMS is a Java Spring Boot based Student College Management System for managing college operations across super admin, institute admin, teacher, student, and placement/TPO roles. The system covers admission/onboarding, academic structure, student/teacher management, courses, batches, subjects, timetable, fees, attendance, assignments, exams/quizzes, study materials, campus events, placements, notifications, reports, and document verification."),
    b("Architecture style: server-side MVC web application with selected REST/JSON endpoints."),
    b("Backend: Java 17 with Spring Boot 3.2.3."),
    b("Frontend: Thymeleaf templates, HTML, CSS, JavaScript, SVG assets, and role-specific dashboards."),
    b("Database: MySQL relational database accessed through Spring Data JPA/Hibernate."),
    b("Build and packaging: Maven project packaged as a Spring Boot JAR."),
]

story += [h1("2. Technology Stack")]
story.append(table(["Layer", "Technology", "Purpose"], [
    ("Language", "Java 17", "Backend implementation language."),
    ("Framework", "Spring Boot 3.2.3", "Application bootstrap, dependency management, MVC, configuration."),
    ("Web Layer", "Spring MVC", "Controllers, routes, forms, uploads, downloads."),
    ("View Layer", "Thymeleaf", "Server-rendered role dashboards and pages."),
    ("Persistence", "Spring Data JPA + Hibernate", "Entity mapping, repositories, JPQL, ORM."),
    ("Database", "MySQL", "Relational storage with constraints and joins."),
    ("Security", "Spring Security, BCrypt, custom interceptor", "Password hashing and session-role checks."),
    ("Files/Reports", "Multipart, Apache POI, PDFBox", "Uploads, Excel imports/exports, PDF/download features."),
    ("Frontend Assets", "CSS/JS/SVG", "Portal styling, onboarding scripts, chatbot widget."),
], [1.05 * inch, 1.9 * inch, 3.25 * inch]))

story += [
    h1("3. Database Type and Design"),
    p("The project uses a relational database, not a non-relational/NoSQL database. MySQL is a good fit because college data has strong relationships and integrity rules: admins own courses, courses contain batches, batches contain students, subjects belong to batches/semesters, teachers map to academic structures, placement drives receive applications, and interviews select students."),
    b("Configured datasource: jdbc:mysql://localhost:3306/scms."),
    b("Runtime driver: mysql-connector-j."),
    b("Schema management: Hibernate ddl-auto=update plus custom schema migration runners."),
    b("Relational features used: primary keys, foreign keys, unique constraints, indexes, joins, and transaction-friendly workflows."),
    b("Sensitive values are encrypted using a JPA AttributeConverter backed by AES/GCM/NoPadding."),
]

story += [h1("4. Main Schema and Relationships")]
story.append(table(["Entity/Table", "Purpose", "Relationship"], [
    ("college", "Institution/tenant master.", "One college can have many admins/TPOs."),
    ("admin", "Institute admin account.", "Owns courses, batches, students, teachers, fees, timetable."),
    ("course", "Program such as BCA/MCA.", "Belongs to admin; unique code per admin."),
    ("batch", "Academic intake/batch.", "Belongs to course and admin."),
    ("academic_structure", "Year, semester, section.", "Links admin, course, batch."),
    ("subject_master", "Course-level subject registry.", "Belongs to course/admin."),
    ("subject", "Batch-semester subject offering.", "Links course, batch, admin, optional subject master."),
    ("classroom", "Class/section/room.", "Belongs to admin; students/timetable connect to it."),
    ("student", "Student profile and academic state.", "Belongs to admin, batch, class."),
    ("teacher", "Teacher employment/profile data.", "Belongs to admin and optional class."),
    ("teacher_academic_mapping", "Teacher allocation.", "Links teacher, course, batch, subject, admin."),
    ("timetable", "Scheduled slots.", "Links class/course/batch/subject/teacher/admin."),
    ("fees", "Fee rules.", "Belongs to admin and optional batch."),
    ("teacher_attendance_session/entry", "Attendance records.", "Session links teacher/class; entry links student."),
    ("class_assignment/submission", "Assignment workflow.", "Assignment links teacher/class/admin; submission links student."),
    ("exam_session/attendance/quiz_*", "Exams and quizzes.", "Exam links teacher/class/admin; attempt/response links student/question."),
    ("study_material", "Learning material uploads.", "Links teacher/class/admin and file metadata."),
    ("campus_event/application", "Events and applications.", "Event links teacher; application links event/student."),
    ("placement_tpo/drive/application/interview", "Placement workflow.", "TPO creates drives; students apply; interviews select students."),
    ("student_document", "Admission documents.", "Links student and reviewing admin."),
    ("portal_notification", "Portal notifications.", "Targets role/email/category/schedule/read state."),
    ("super_admin_audit_log", "Audit trail.", "Stores actor, action, target, timestamp."),
], [1.55 * inch, 2.05 * inch, 2.6 * inch]))

story += [
    h1("5. Modules and Features"),
    b("Public/home module: home, overview, courses, contact, registration and login pages."),
    b("Super admin module: colleges, college admins, communications, audit logs, notifications, reports, settings."),
    b("Admin module: dashboard, courses, batches, academic structure, subject registry, students, teachers, classes, timetable, fees, imports, reports."),
    b("Student module: onboarding, dashboard, profile, subjects, timetable, attendance, assignments, exams, fees, notifications, placements, events, study materials."),
    b("Teacher module: dashboard, students, subjects, attendance, marks, assignments, timetable, profile, notifications, study materials, events, exams."),
    b("Placement module: TPO login, dashboard, drives, applications, students, interviews, analytics, reports, Excel export, resume zip export."),
    b("Chatbot module: JSON endpoints for chatbot context and message handling."),
]

story += [h1("6. Controllers and API Surface")]
story.append(table(["Controller", "Type", "Main Responsibility"], [
    ("MainController", "MVC", "/, /register, /login, role login pages, superadmin login, public pages."),
    ("AdminController", "MVC", "Admin dashboard, courses, batches, academic structure, subjects, imports, students, teachers, classes, timetable, fees, reports."),
    ("StudentController", "MVC", "Student dashboard, onboarding, assignments, profile, fees, notifications, marks, attendance."),
    ("TeacherController", "MVC", "Teacher dashboard, attendance, marks, assignments, timetable, profile, notifications."),
    ("TeacherExamController", "MVC", "Teacher exam creation, quiz creation, attendance, seating, student exam attempts, quiz results."),
    ("PlacementController", "MVC", "TPO login, drives, applications, placements, interviews, exports."),
    ("EventController", "MVC", "Campus events, event applications, teacher event creation and download."),
    ("StudyMaterialController", "MVC", "Study material upload, view, download."),
    ("SuperAdminController", "MVC", "College/admin management, communications, audit logs, notifications, reports."),
    ("ChatbotController", "REST", "GET /api/chatbot/context and POST /api/chatbot/message."),
], [1.45 * inch, 0.72 * inch, 4.03 * inch]))

story += [
    h1("7. Security"),
    p("Spring Security is present, but default form login, HTTP Basic, and CSRF are disabled and all requests are permitted at the Spring Security filter chain. Actual route protection is handled by SessionAccessInterceptor, which checks session attributes like loggedInUser and userRole and redirects unauthenticated or wrong-role users to the appropriate login page."),
    b("Roles handled: ADMIN, TEACHER, STUDENT, PLACEMENT_TPO, and generic AUTHENTICATED download routes."),
    b("Passwords use BCryptPasswordEncoder with strength 12."),
    b("Sensitive fields use AES-GCM encryption through SensitiveStringConverter and FieldEncryptionSupport."),
    b("FIELD_ENCRYPTION_SECRET should be provided from environment in production."),
    b("Production improvements: enable CSRF, use SecurityFilterChain role rules, secure cookies, stricter file authorization, external secret management, upload validation, and rate limits."),
]

story += [
    h1("8. Files, Imports, Exports, and Reports"),
    b("Multipart upload size is configured as 10 MB."),
    b("Upload folders include student documents, student photos, teacher photos, placement resumes, assignments, exams, and study materials."),
    b("Database stores file metadata such as original filename, stored filename, path, content type, size, status, and upload/review timestamps."),
    b("Apache POI supports Excel template/import/export flows."),
    b("PDFBox and HTTP response streaming support PDF/report/download style workflows."),
]

story += [
    h1("9. Key Interview Talking Points"),
    b("Relational DB is chosen because the domain needs joins, constraints, transactions, and reporting."),
    b("JPA repositories reduce boilerplate and use query derivation plus JPQL where needed."),
    b("Thymeleaf keeps the frontend simple for forms, dashboards, sessions, and role-specific pages."),
    b("The schema is mostly normalized, with some duplicated display fields for historical filtering/display convenience."),
    b("The biggest maintainability improvement is splitting very large controllers into smaller controllers and services."),
    b("The biggest production security improvement is moving authorization into Spring Security and enabling CSRF."),
    b("The biggest deployment improvement is storing uploads outside the source tree, ideally in object storage."),
]

story.append(PageBreak())
story += [h1("10. Google-Style Interview Questions and Answers")]

qas = [
    ("Explain this project in 60 seconds.", "SCMS is a Java Spring Boot MVC web application for managing college operations across super admin, admin, teacher, student, and placement/TPO roles. It uses Thymeleaf for server-rendered UI, MySQL for relational persistence, Spring Data JPA/Hibernate for ORM, BCrypt for password hashing, AES-GCM for sensitive field encryption, and file upload/export workflows for documents, assignments, resumes, timetables, and reports."),
    ("Which database did you use and why?", "We used MySQL, a relational database. The domain contains strongly related entities such as colleges, admins, courses, batches, students, teachers, subjects, exams, attendance, fees, events, and placements. Relational modeling gives referential integrity, joins, transactions, unique constraints, and better reporting."),
    ("Is this SQL or NoSQL?", "It is SQL/relational. The schema uses tables, primary keys, foreign keys, indexes, and constraints. A NoSQL database could store flexible documents, but it would make integrity and reporting across academic relationships harder."),
    ("What is the application architecture?", "It is a layered MVC architecture: controllers handle requests, repositories access data through JPA, services contain reusable domain logic, entities map to MySQL tables, Thymeleaf templates render pages, and static JS/CSS improves the UI."),
    ("Is it REST API based?", "Mostly it is a Spring MVC server-rendered application using GET/POST routes that return views or redirects. It also has REST-style JSON endpoints for chatbot context/message and utility endpoints like options/previews/live data."),
    ("How are relationships implemented?", "JPA annotations like @ManyToOne, @OneToMany, and @ManyToMany model relationships. Examples include Student to Admin/Batch/ClassRoom, Subject to Course/Batch/Admin, PlacementDrive to PlacementTpo, and PlacementInterview to selected Students."),
    ("How do you prevent duplicate data?", "The schema has unique constraints such as course code per admin, batch per admin/course/display name, subject code per admin/course/batch/semester, and student email. Repositories also use existsBy methods before saving."),
    ("How is authentication implemented?", "Users log in through role-specific routes. After successful login, session attributes store loggedInUser and userRole. SessionAccessInterceptor checks protected URL patterns and redirects users who are not logged in or have the wrong role."),
    ("Why use BCrypt?", "BCrypt is a salted, adaptive, one-way password hashing algorithm. The project uses strength 12, which makes brute force attacks much harder than plain hashes like MD5 or SHA."),
    ("How is sensitive data protected?", "Sensitive fields are annotated with @Convert using SensitiveStringConverter. Before database write, AES/GCM encrypts the value with a random IV and authentication tag. On read, the converter decrypts the value for the application."),
    ("What are the main security gaps?", "CSRF is disabled, SecurityFilterChain permits all requests, and role checks are implemented through a custom interceptor. For production, authorization should be enforced in Spring Security, CSRF should be enabled, and download endpoints should verify ownership."),
    ("How does file upload work?", "Controllers receive MultipartFile, store the physical file with a generated name, and persist metadata in MySQL. Download endpoints locate the stored file and stream it back with proper response headers."),
    ("How would you scale this system?", "Move uploaded files to object storage, externalize sessions or use stateless auth, add pagination, add database indexes for heavy filters, cache static lookups, split controllers/services, and deploy multiple app instances behind a load balancer."),
    ("What is normalization in this project?", "Instead of one large table, the system separates courses, batches, students, teachers, subjects, fees, timetable, assignments, exams, events, and placements. This reduces duplication and allows foreign-key relationships and cleaner reporting."),
    ("Where is denormalization used?", "Some operational tables keep display fields like course, semester, academic year, or batch name. This simplifies historical display/filtering but requires clear source-of-truth rules."),
    ("Why use Thymeleaf instead of React?", "Thymeleaf is simpler for a form-heavy academic portal where server-side sessions, redirects, and role-specific pages dominate. React would be useful for an SPA but would require a stronger REST API boundary."),
    ("What does ddl-auto=update mean?", "Hibernate updates the database schema based on entity mappings at startup. It is convenient for development but should be replaced by Flyway or Liquibase for production migrations."),
    ("How would you test it?", "Use unit tests for services, @DataJpaTest or Testcontainers for repositories, MockMvc for controllers, integration tests for login/session/role paths, and upload/download tests for file workflows."),
    ("What would you refactor first?", "AdminController is very large and should be split by bounded context: course, batch, student, teacher, timetable, fees, import, and report controllers. Business logic should move into services for maintainability and testing."),
    ("What is the strongest project explanation for interview?", "It is a real-world multi-role college ERP-style portal using Spring Boot, MySQL, JPA, Thymeleaf, security/session handling, encrypted sensitive fields, uploads, exports, and normalized relational schema design."),
]

for question, answer in qas:
    story.extend(q(question, answer))

story += [
    h1("11. Final Summary"),
    p("For interviews, present SCMS as a relational, multi-role, Spring Boot MVC system. Emphasize MySQL schema design, JPA relationships, role-based workflows, file upload/export handling, password hashing, sensitive-field encryption, and practical production improvements. Be ready to discuss why relational design fits the academic domain, how each user role interacts with the system, and how you would improve security, testing, scalability, and maintainability."),
]


def footer(canvas, doc):
    canvas.saveState()
    canvas.setFont("Helvetica", 8)
    canvas.setFillColor(colors.HexColor("#667085"))
    canvas.drawString(inch, 0.55 * inch, "SCMS Interview Preparation Guide")
    canvas.drawRightString(LETTER[0] - inch, 0.55 * inch, f"Page {doc.page}")
    canvas.restoreState()


pdf = SimpleDocTemplate(OUT, pagesize=LETTER, rightMargin=0.75 * inch, leftMargin=0.75 * inch, topMargin=0.75 * inch, bottomMargin=0.8 * inch)
pdf.build(story, onFirstPage=footer, onLaterPages=footer)
print(OUT)
