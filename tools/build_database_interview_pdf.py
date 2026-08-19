from reportlab.lib import colors
from reportlab.lib.pagesizes import LETTER, landscape
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import inch
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, Preformatted


OUT = "SCMS_Database_Interview_QA_and_SQL_Guide.pdf"

styles = getSampleStyleSheet()
styles.add(ParagraphStyle("TitleMain", parent=styles["Title"], fontSize=20, leading=25, textColor=colors.HexColor("#0B2545"), spaceAfter=8))
styles.add(ParagraphStyle("H1x", parent=styles["Heading1"], fontSize=14, leading=18, textColor=colors.HexColor("#2E74B5"), spaceBefore=12, spaceAfter=6))
styles.add(ParagraphStyle("H2x", parent=styles["Heading2"], fontSize=11.5, leading=14, textColor=colors.HexColor("#1F4D78"), spaceBefore=8, spaceAfter=4))
styles.add(ParagraphStyle("Bodyx", parent=styles["BodyText"], fontSize=8.7, leading=11.2, spaceAfter=4))
styles.add(ParagraphStyle("Bulletx", parent=styles["BodyText"], fontSize=8.7, leading=11, leftIndent=14, firstLineIndent=-8, spaceAfter=2))
styles.add(ParagraphStyle("Qx", parent=styles["BodyText"], fontSize=9, leading=11.5, textColor=colors.HexColor("#1F4D78"), spaceBefore=5, spaceAfter=2))
styles.add(ParagraphStyle("Codex", fontName="Courier", fontSize=7.2, leading=8.8, leftIndent=8, rightIndent=4, spaceBefore=3, spaceAfter=6))


def p(text):
    return Paragraph(text, styles["Bodyx"])


def h1(text):
    return Paragraph(text, styles["H1x"])


def h2(text):
    return Paragraph(text, styles["H2x"])


def b(text):
    return Paragraph("• " + text, styles["Bulletx"])


def code(text):
    return Preformatted(text.strip(), styles["Codex"])


def qa(question, answer, sql=None):
    items = [Paragraph("<b>Q. " + question + "</b>", styles["Qx"]), p("<b>A.</b> " + answer)]
    if sql:
        items.append(code(sql))
    return items


def table(headers, rows, widths):
    data = [[Paragraph("<b>" + h + "</b>", styles["Bodyx"]) for h in headers]]
    for row in rows:
        data.append([Paragraph(str(cell), styles["Bodyx"]) for cell in row])
    t = Table(data, colWidths=widths, repeatRows=1)
    t.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#E8EEF5")),
        ("GRID", (0, 0), (-1, -1), 0.35, colors.HexColor("#B8C2CC")),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 4),
        ("RIGHTPADDING", (0, 0), (-1, -1), 4),
        ("TOPPADDING", (0, 0), (-1, -1), 3),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 3),
    ]))
    return t


story = []
story.append(Paragraph("SCMS Database Interview Q&A and SQL Query Guide", styles["TitleMain"]))
story.append(p("<i>Database-only explanation for Student College Management System: MySQL schema, keys, joins, constraints, normalization, repository query patterns, and practical SQL.</i>"))
story.append(Spacer(1, 0.08 * inch))

story += [
    h1("1. Database Overview"),
    p("SCMS uses MySQL, which is a relational SQL database. The system is relational because the main data is connected through well-defined business relationships: college to admin, admin to course, course to batch, batch to student, teacher to class, timetable to subject, placement drive to applications, and exam to quiz attempts."),
    b("Database type: Relational SQL database."),
    b("Database engine: MySQL with InnoDB-style foreign-key relationships."),
    b("ORM layer: Spring Data JPA and Hibernate."),
    b("Primary key pattern: BIGINT auto-increment id on most tables."),
    b("Relationship pattern: foreign keys such as admin_id, course_id, batch_id, class_id, teacher_id, student_id."),
    b("Schema evolution: Hibernate ddl-auto=update plus Java migration runners."),
]

story += [h1("2. Main Tables and Keys")]
story.append(table(["Table", "Primary Key", "Foreign Keys / Unique Keys", "Meaning"], [
    ("college", "id", "code unique, name unique", "Institution or tenant record."),
    ("admin", "id", "college_id, email unique", "Institute admin for one college context."),
    ("course", "id", "admin_id FK, unique(admin_id, code)", "Course/program under one admin."),
    ("batch", "id", "course_id FK, admin_id FK, unique(admin_id, course_id, display_name)", "Academic batch/intake."),
    ("academic_structure", "id", "course_id, batch_id, admin_id; unique admin/course/batch/year/semester/section", "Semester-section structure."),
    ("subject_master", "id", "course_id, admin_id; unique(admin_id, course_id, semester_number, code)", "Reusable course curriculum subject."),
    ("subject", "id", "subject_master_id, course_id, batch_id, admin_id; unique admin/course/batch/semester/code", "Actual batch subject offering."),
    ("classroom", "id", "admin_id, teacher_id optional", "Classroom/section/room metadata."),
    ("student", "id", "class_id, batch_id, admin_id; email unique; unique(admin_id, enrollment_no)", "Student profile and academic state."),
    ("teacher", "id", "admin_id, class_room_id", "Teacher profile and employment data."),
    ("teacher_academic_mapping", "id", "teacher_id, course_id, batch_id, subject_id, admin_id; composite unique", "Teacher assignment to academic context."),
    ("timetable", "id", "class_id, course_id, batch_id, subject_id, teacher_id, admin_id", "Scheduled class slots."),
    ("fees", "id", "batch_id, admin_id; lookup index(course, academic_year, semester)", "Fee rules and amounts."),
    ("class_assignment", "id", "teacher_id, class_room_id, admin_id", "Teacher assignment upload/workflow."),
    ("assignment_submission", "id", "assignment_id, student_id, admin_id; unique assignment/student", "Student submission."),
    ("exam_session", "id", "teacher_id, class_room_id, admin_id", "Exam or quiz session."),
    ("exam_attendance", "id", "exam_id, student_id", "Exam attendance record."),
    ("exam_quiz_attempt", "id", "exam_session_id, student_id, admin_id; unique exam/student", "Student quiz attempt."),
    ("exam_quiz_response", "id", "attempt_id, question_id; unique attempt/question", "Answer per quiz question."),
    ("placement_tpo", "id", "college_id, admin_id, email unique", "Placement officer account."),
    ("placement_drive", "id", "tpo_id", "Job/placement drive."),
    ("placement_application", "id", "drive_id, student_id", "Student application to drive."),
    ("placement_interview_student", "interview_id + student_id", "FK to placement_interview and student", "Many-to-many join table."),
], [1.35 * inch, 0.85 * inch, 2.4 * inch, 1.8 * inch]))

story += [
    h1("3. Interview Q&A: Keys, Constraints, and Relationships"),
]

qas = [
    ("What is a primary key in this project?", "A primary key uniquely identifies each row. Most SCMS tables use id BIGINT AUTO_INCREMENT PRIMARY KEY. Example: student.id uniquely identifies one student.", "SELECT id, name, email FROM student WHERE id = 1;"),
    ("What is a foreign key in this project?", "A foreign key stores the id of a related row from another table. For example student.admin_id references admin.id, student.batch_id references batch.id, and student.class_id references classroom.id.", "SELECT s.name, a.name AS admin_name\nFROM student s\nJOIN admin a ON s.admin_id = a.id;"),
    ("What is a composite key?", "A composite key uses more than one column to identify a row. The placement_interview_student join table uses PRIMARY KEY(interview_id, student_id), so the same student cannot be added twice to the same interview.", "SELECT interview_id, student_id\nFROM placement_interview_student\nWHERE interview_id = 10;"),
    ("What is a unique key?", "A unique key prevents duplicate values for a business rule. Example: course has unique(admin_id, code), so one admin cannot create two courses with the same code.", "SELECT admin_id, code, COUNT(*) AS duplicate_count\nFROM course\nGROUP BY admin_id, code\nHAVING COUNT(*) > 1;"),
    ("What is an index and why is it used?", "An index speeds up lookup, filtering, and joins. For example student has indexes on class_id, batch_id, and admin_id because dashboards often filter students by class, batch, or admin.", "EXPLAIN SELECT * FROM student WHERE admin_id = 1 AND batch_id = 3;"),
    ("What is referential integrity?", "Referential integrity means child rows must point to valid parent rows. A student batch_id should exist in batch.id, and a subject course_id should exist in course.id. Foreign keys enforce this rule.", "SELECT s.id, s.name\nFROM student s\nLEFT JOIN batch b ON s.batch_id = b.id\nWHERE s.batch_id IS NOT NULL AND b.id IS NULL;"),
    ("What is a many-to-one relationship?", "Many rows in one table point to one row in another table. Many students belong to one admin; many batches belong to one course; many subjects belong to one batch.", "SELECT b.display_name, c.name AS course_name\nFROM batch b\nJOIN course c ON b.course_id = c.id;"),
    ("What is a one-to-many relationship?", "It is the reverse view of many-to-one. One course can have many batches, one admin can have many students, and one assignment can have many submissions.", "SELECT c.name, COUNT(b.id) AS batch_count\nFROM course c\nLEFT JOIN batch b ON b.course_id = c.id\nGROUP BY c.id, c.name;"),
    ("What is a many-to-many relationship in this system?", "Placement interviews and students use many-to-many. One interview can include many students, and one student can appear in many interviews. The link table is placement_interview_student.", "SELECT pi.id AS interview_id, s.name AS student_name\nFROM placement_interview pi\nJOIN placement_interview_student pis ON pis.interview_id = pi.id\nJOIN student s ON s.id = pis.student_id;"),
]
for item in qas:
    story.extend(qa(*item))

story.append(PageBreak())
story += [h1("4. Interview Q&A: Joins")]
join_qas = [
    ("What is INNER JOIN?", "INNER JOIN returns only matching rows from both tables. Use it when the relationship must exist, such as students with a valid admin.", "SELECT s.name, a.email AS admin_email\nFROM student s\nINNER JOIN admin a ON s.admin_id = a.id;"),
    ("What is LEFT JOIN?", "LEFT JOIN returns all rows from the left table and matching rows from the right table. Use it to find missing optional relationships, such as students without a classroom.", "SELECT s.name, c.name AS class_name\nFROM student s\nLEFT JOIN classroom c ON s.class_id = c.id\nWHERE c.id IS NULL;"),
    ("What is RIGHT JOIN?", "RIGHT JOIN returns all rows from the right table and matching rows from the left. It is less common because the same query can usually be rewritten as LEFT JOIN.", "SELECT s.name, b.display_name\nFROM student s\nRIGHT JOIN batch b ON s.batch_id = b.id;"),
    ("What is SELF JOIN?", "SELF JOIN joins a table to itself. It is not central in SCMS, but can compare records in the same table, such as duplicate student emails ignoring case.", "SELECT s1.id, s2.id, s1.email\nFROM student s1\nJOIN student s2 ON LOWER(s1.email) = LOWER(s2.email)\nWHERE s1.id < s2.id;"),
    ("What is a join table?", "A join table stores relationships between two tables in a many-to-many design. placement_interview_student contains interview_id and student_id.", "SELECT pi.interview_round, s.name\nFROM placement_interview_student pis\nJOIN placement_interview pi ON pi.id = pis.interview_id\nJOIN student s ON s.id = pis.student_id;"),
    ("How do you join course, batch, and student?", "Join student.batch_id to batch.id and batch.course_id to course.id. This gives each student's course and batch.", "SELECT s.name AS student, c.code AS course, b.display_name AS batch\nFROM student s\nJOIN batch b ON s.batch_id = b.id\nJOIN course c ON b.course_id = c.id\nORDER BY c.code, b.display_name, s.name;"),
    ("How do you join timetable with subject and teacher?", "Timetable has subject_id and teacher_id, so join timetable to subject and teacher to show readable schedule rows.", "SELECT tt.day, tt.start_time, tt.end_time, sub.name AS subject, t.name AS teacher\nFROM timetable tt\nLEFT JOIN subject sub ON sub.id = tt.subject_id\nLEFT JOIN teacher t ON t.id = tt.teacher_id\nORDER BY tt.day, tt.start_time;"),
    ("How do you join assignment and submission?", "Assignment submission has assignment_id and student_id. Join assignment_submission to class_assignment and student.", "SELECT a.title, s.name AS student, sub.submitted_at, sub.original_file_name\nFROM assignment_submission sub\nJOIN class_assignment a ON a.id = sub.assignment_id\nJOIN student s ON s.id = sub.student_id\nORDER BY sub.submitted_at DESC;"),
]
for item in join_qas:
    story.extend(qa(*item))

story += [h1("5. Practical SQL Queries for the Whole System")]
query_blocks = [
    ("List all students with admin, course, batch, and classroom", "SELECT s.id, s.name, s.email, a.name AS admin_name,\n       c.name AS course_name, b.display_name AS batch,\n       cr.name AS classroom\nFROM student s\nJOIN admin a ON a.id = s.admin_id\nLEFT JOIN batch b ON b.id = s.batch_id\nLEFT JOIN course c ON c.id = b.course_id\nLEFT JOIN classroom cr ON cr.id = s.class_id\nORDER BY a.name, c.name, b.display_name, s.name;"),
    ("Count students per course and batch", "SELECT c.code, c.name AS course, b.display_name AS batch,\n       COUNT(s.id) AS student_count\nFROM batch b\nJOIN course c ON c.id = b.course_id\nLEFT JOIN student s ON s.batch_id = b.id\nGROUP BY c.id, c.code, c.name, b.id, b.display_name\nORDER BY c.code, b.display_name;"),
    ("Find students with pending fees", "SELECT s.name, s.email, s.total_fees, s.paid_fees,\n       (COALESCE(s.total_fees,0) - COALESCE(s.paid_fees,0)) AS pending_amount\nFROM student s\nWHERE (COALESCE(s.total_fees,0) - COALESCE(s.paid_fees,0)) > 0\nORDER BY pending_amount DESC;"),
    ("Show teacher academic assignments", "SELECT t.name AS teacher, c.code AS course, b.display_name AS batch,\n       tam.academic_year, sub.name AS subject, tam.semester\nFROM teacher_academic_mapping tam\nJOIN teacher t ON t.id = tam.teacher_id\nJOIN course c ON c.id = tam.course_id\nJOIN batch b ON b.id = tam.batch_id\nLEFT JOIN subject sub ON sub.id = tam.subject_id\nORDER BY t.name, c.code, b.display_name;"),
    ("Show complete timetable", "SELECT cr.name AS class_name, tt.day, tt.start_time, tt.end_time,\n       sub.name AS subject, t.name AS teacher, tt.room\nFROM timetable tt\nLEFT JOIN classroom cr ON cr.id = tt.class_id\nLEFT JOIN subject sub ON sub.id = tt.subject_id\nLEFT JOIN teacher t ON t.id = tt.teacher_id\nORDER BY cr.name, tt.day, tt.start_time;"),
    ("Student attendance percentage", "SELECT s.id, s.name,\n       SUM(CASE WHEN tae.status = 'Present' THEN 1 ELSE 0 END) AS present_count,\n       COUNT(tae.id) AS total_count,\n       ROUND(100 * SUM(CASE WHEN tae.status = 'Present' THEN 1 ELSE 0 END) / NULLIF(COUNT(tae.id),0), 2) AS attendance_percent\nFROM student s\nLEFT JOIN teacher_attendance_entry tae ON tae.student_id = s.id\nGROUP BY s.id, s.name\nORDER BY attendance_percent DESC;"),
    ("Placement applications by drive", "SELECT pd.company_name, pd.job_title,\n       COUNT(pa.id) AS applications\nFROM placement_drive pd\nLEFT JOIN placement_application pa ON pa.drive_id = pd.id\nGROUP BY pd.id, pd.company_name, pd.job_title\nORDER BY applications DESC;"),
    ("Students selected for interviews", "SELECT pd.company_name, pi.interview_round, pi.interview_date_time,\n       s.name AS student, s.email\nFROM placement_interview pi\nJOIN placement_drive pd ON pd.id = pi.drive_id\nJOIN placement_interview_student pis ON pis.interview_id = pi.id\nJOIN student s ON s.id = pis.student_id\nORDER BY pi.interview_date_time, pd.company_name, s.name;"),
    ("Exam result summary", "SELECT es.title, s.name AS student, eqa.score,\n       eqa.correct_answers, eqa.total_questions, eqa.status\nFROM exam_quiz_attempt eqa\nJOIN exam_session es ON es.id = eqa.exam_session_id\nJOIN student s ON s.id = eqa.student_id\nORDER BY es.title, eqa.score DESC;"),
    ("Document verification status", "SELECT s.name, sd.document_name, sd.status,\n       sd.uploaded_at, a.name AS reviewed_by\nFROM student_document sd\nJOIN student s ON s.id = sd.student_id\nLEFT JOIN admin a ON a.id = sd.reviewed_by_admin_id\nORDER BY s.name, sd.document_name;"),
]
for title, sql in query_blocks:
    story += [h2(title), code(sql)]

story.append(PageBreak())
story += [h1("6. Advanced Database Interview Q&A")]
advanced = [
    ("What is normalization and how is it applied?", "Normalization means splitting data into related tables to reduce duplication and update anomalies. SCMS separates admin, course, batch, student, teacher, subject, timetable, fees, placements, exams, and documents instead of storing everything in one table."),
    ("What normal form does this system mostly follow?", "The design mostly follows 3NF for core academic data: each table has a primary key, non-key fields describe that table, and relationships are handled by foreign keys. Some fields are denormalized for display/history, such as course or batch name in operational tables."),
    ("Why keep both subject_master and subject?", "subject_master is the course-level curriculum definition. subject is the batch-level/semester-level offering. This lets the system define common subjects once and apply or override them per batch."),
    ("What is cascading and should it be used here?", "Cascading automatically propagates deletes/updates to child rows. In SCMS, it should be used carefully because deleting a student can affect applications, attendance, submissions, exam attempts, and documents. Explicit deletes or soft delete are safer for auditability."),
    ("How would you handle soft delete?", "Add status or deleted_at columns instead of physically deleting rows. Queries filter active records. This is useful for students, teachers, courses, placement drives, and audit-sensitive records."),
    ("What is transaction management?", "A transaction groups multiple database changes so they either all succeed or all fail. Example: creating an exam with questions should be one transaction; if question insertion fails, the exam should not remain partially created."),
    ("What are indexes you would add?", "Add indexes on frequent filters such as student(admin_id, batch_id), timetable(admin_id, class_id, day), placement_application(drive_id, student_id), teacher_attendance_entry(student_id), exam_quiz_attempt(exam_session_id, student_id), and portal_notification(recipient_role, recipient_email, scheduled_for)."),
    ("How would you find slow queries?", "Use EXPLAIN, check missing indexes, inspect join order, avoid SELECT *, paginate large result sets, and measure query time. In production, enable slow query logs and use application metrics."),
    ("What is an N+1 query problem?", "N+1 happens when the app fetches a list and then runs one extra query for each item. In JPA, it can happen with lazy relationships. Use fetch joins, entity graphs, DTO queries, or batch fetching."),
    ("What is the difference between JPQL and SQL?", "SQL queries database tables directly. JPQL queries Java entity names and fields. Example: select s from Student s where s.admin = :admin is JPQL; SELECT * FROM student WHERE admin_id = ? is SQL."),
]
for question, answer in advanced:
    story.extend(qa(question, answer))

story += [h1("7. Interview-Ready Database Summary")]
story += [
    p("Use this summary in interviews: SCMS uses a MySQL relational schema because the domain is relationship-heavy and needs referential integrity. Most tables have BIGINT auto-increment primary keys. Relationships are implemented using foreign keys and JPA @ManyToOne/@ManyToMany mappings. Unique constraints protect business rules like one course code per admin and one subject code per batch-semester. Indexes support frequent filters on admin, batch, class, teacher, timetable scope, fees lookup, and placement/exam relationships. Complex screens are powered by joins across student, batch, course, teacher, timetable, assignment, exam, and placement tables."),
    b("Most important join path: admin -> course -> batch -> student."),
    b("Most important academic path: course -> batch -> subject -> timetable -> teacher."),
    b("Most important student activity path: student -> assignment_submission, exam_quiz_attempt, placement_application, teacher_attendance_entry."),
    b("Most important many-to-many example: placement_interview <-> student through placement_interview_student."),
]


def footer(canvas, doc):
    canvas.saveState()
    canvas.setFont("Helvetica", 8)
    canvas.setFillColor(colors.HexColor("#667085"))
    canvas.drawString(0.55 * inch, 0.38 * inch, "SCMS Database Interview Q&A and SQL Guide")
    canvas.drawRightString(LETTER[0] - 0.55 * inch, 0.38 * inch, f"Page {doc.page}")
    canvas.restoreState()


pdf = SimpleDocTemplate(
    OUT,
    pagesize=LETTER,
    rightMargin=0.55 * inch,
    leftMargin=0.55 * inch,
    topMargin=0.55 * inch,
    bottomMargin=0.6 * inch,
)
pdf.build(story, onFirstPage=footer, onLaterPages=footer)
print(OUT)
