package com.scms.scms.repository;

import com.scms.scms.model.Admin;
import com.scms.scms.model.Assignment;
import com.scms.scms.model.AssignmentSubmission;
import com.scms.scms.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, Long> {
    List<AssignmentSubmission> findByAssignmentOrderBySubmittedAtDesc(Assignment assignment);
    List<AssignmentSubmission> findByStudentOrderBySubmittedAtDesc(Student student);
    Optional<AssignmentSubmission> findByAssignmentAndStudent(Assignment assignment, Student student);
    List<AssignmentSubmission> findByAdminOrderBySubmittedAtDesc(Admin admin);
}
