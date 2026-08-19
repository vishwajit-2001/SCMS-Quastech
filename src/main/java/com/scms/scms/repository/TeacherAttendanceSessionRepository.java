package com.scms.scms.repository;

import com.scms.scms.model.ClassRoom;
import com.scms.scms.model.Teacher;
import com.scms.scms.model.TeacherAttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TeacherAttendanceSessionRepository extends JpaRepository<TeacherAttendanceSession, Long> {
    List<TeacherAttendanceSession> findByTeacherOrderByAttendanceDateDescCreatedAtDesc(Teacher teacher);
    List<TeacherAttendanceSession> findByClassRoomOrderByAttendanceDateDescCreatedAtDesc(ClassRoom classRoom);
    List<TeacherAttendanceSession> findByTeacherAndAttendanceDateOrderByCreatedAtDesc(Teacher teacher, LocalDate attendanceDate);
    TeacherAttendanceSession findFirstByTeacherAndClassRoomAndSubjectAndAttendanceDateAndLectureNoOrderByCreatedAtDesc(Teacher teacher, ClassRoom classRoom, String subject, LocalDate attendanceDate, String lectureNo);
}
