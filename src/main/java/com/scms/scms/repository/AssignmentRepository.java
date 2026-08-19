package com.scms.scms.repository;

import com.scms.scms.model.Admin;
import com.scms.scms.model.Assignment;
import com.scms.scms.model.ClassRoom;
import com.scms.scms.model.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByTeacherOrderByCreatedAtDesc(Teacher teacher);
    List<Assignment> findByClassRoomOrderByDueDateAscCreatedAtDesc(ClassRoom classRoom);
    List<Assignment> findByClassRoomAndPublishedTrueOrderByDueDateAscCreatedAtDesc(ClassRoom classRoom);
    List<Assignment> findByAdminOrderByCreatedAtDesc(Admin admin);
}
