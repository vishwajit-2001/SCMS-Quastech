package com.scms.scms.repository;

import com.scms.scms.model.Student;
import com.scms.scms.model.TeacherAttendanceEntry;
import com.scms.scms.model.TeacherAttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeacherAttendanceEntryRepository extends JpaRepository<TeacherAttendanceEntry, Long> {
    List<TeacherAttendanceEntry> findBySessionOrderByStudent_NameAsc(TeacherAttendanceSession session);
    List<TeacherAttendanceEntry> findByStudentOrderByMarkedAtDesc(Student student);
    List<TeacherAttendanceEntry> findBySession_TeacherOrderByMarkedAtDesc(com.scms.scms.model.Teacher teacher);
    long deleteBySession(TeacherAttendanceSession session);
    long deleteByStudent(Student student);
}
