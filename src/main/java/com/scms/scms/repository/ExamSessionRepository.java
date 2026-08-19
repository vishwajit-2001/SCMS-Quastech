package com.scms.scms.repository;

import com.scms.scms.model.Admin;
import com.scms.scms.model.ClassRoom;
import com.scms.scms.model.ExamSession;
import com.scms.scms.model.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ExamSessionRepository extends JpaRepository<ExamSession, Long> {
    List<ExamSession> findByTeacherOrderByExamDateTimeDesc(Teacher teacher);
    List<ExamSession> findByClassRoomOrderByExamDateTimeAsc(ClassRoom classRoom);
    List<ExamSession> findByAdminOrderByExamDateTimeDesc(Admin admin);
    List<ExamSession> findByStatusInOrderByExamDateTimeAsc(List<String> status);
    List<ExamSession> findByExamDateTimeBeforeAndStatusNotIn(LocalDateTime cutoff, List<String> status);
}
