package com.scms.scms.repository;

import com.scms.scms.model.Admin;
import com.scms.scms.model.Batch;
import com.scms.scms.model.Course;
import com.scms.scms.model.Subject;
import com.scms.scms.model.SubjectMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findByAdminOrderByCourseRef_CodeAscBatchRef_DisplayNameAscSemesterAscNameAsc(Admin admin);
    List<Subject> findByAdminAndCourseRefAndBatchRefAndSemesterOrderByNameAsc(Admin admin, Course courseRef, Batch batchRef, Integer semester);
    List<Subject> findByAdminAndCourseRefAndBatchRefOrderBySemesterAscNameAsc(Admin admin, Course courseRef, Batch batchRef);
    Subject findByAdminAndCourseRefAndBatchRefAndSemesterAndCodeIgnoreCase(Admin admin, Course courseRef, Batch batchRef, Integer semester, String code);
    List<Subject> findByAdminAndBatchRefOrderBySemesterAscNameAsc(Admin admin, Batch batchRef);
    List<Subject> findByAdminAndBatchRefAndStatusIgnoreCaseOrderBySemesterAscNameAsc(Admin admin, Batch batchRef, String status);
    List<Subject> findByAdminAndBatchRefAndCourseRefAndSemesterAndStatusIgnoreCaseOrderByNameAscCodeAsc(
            Admin admin,
            Batch batchRef,
            Course courseRef,
            Integer semester,
            String status
    );
    List<Subject> findByAdminAndCourseRefAndStatusIgnoreCaseOrderByBatchRef_DisplayNameAscSemesterAscNameAsc(Admin admin, Course courseRef, String status);
    Subject findByAdminAndBatchRefAndSemesterAndCodeIgnoreCase(Admin admin, Batch batchRef, Integer semester, String code);
    Subject findByAdminAndBatchRefAndSemesterAndCodeIgnoreCaseAndIdNot(Admin admin, Batch batchRef, Integer semester, String code, Long id);
    List<Subject> findByAdminAndSubjectMasterRefOrderByBatchRef_DisplayNameAscSemesterAscNameAsc(Admin admin, SubjectMaster subjectMasterRef);
    List<Subject> findByAdminAndCourseRef(Admin admin, Course courseRef);
    List<Subject> findByAdminAndBatchRef(Admin admin, Batch batchRef);

    @Query("select count(distinct s.batchRef.id) from Subject s where s.admin = :admin and s.subjectMasterRef = :subjectMasterRef and lower(coalesce(s.status, 'active')) = lower(:status)")
    long countDistinctBatchesByAdminAndSubjectMasterRefAndStatus(@Param("admin") Admin admin,
                                                                 @Param("subjectMasterRef") SubjectMaster subjectMasterRef,
                                                                 @Param("status") String status);
}
