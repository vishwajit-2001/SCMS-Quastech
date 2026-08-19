package com.scms.scms.repository;

import com.scms.scms.model.Student;
import com.scms.scms.model.StudentDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentDocumentRepository extends JpaRepository<StudentDocument, Long> {
    List<StudentDocument> findByStudentIdOrderByDocumentNameAsc(Long studentId);
    List<StudentDocument> findByStudentIdAndStatusIgnoreCaseOrderByDocumentNameAsc(Long studentId, String status);
    boolean existsByStudentIdAndStatusIgnoreCase(Long studentId, String status);
    Optional<StudentDocument> findByStudentIdAndDocumentNameIgnoreCase(Long studentId, String documentName);
    void deleteByStudentId(Long studentId);
}
