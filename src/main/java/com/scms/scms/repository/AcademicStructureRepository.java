package com.scms.scms.repository;

import com.scms.scms.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AcademicStructureRepository extends JpaRepository<AcademicStructure, Long> {
    List<AcademicStructure> findByAdminOrderByBatch_DisplayNameAscYearLabelAscSemesterNumberAscSectionAsc(Admin admin);

    List<AcademicStructure> findByAdminAndCourseAndBatchOrderByYearLabelAscSemesterNumberAscSectionAsc(
            Admin admin, Course course, Batch batch);
    List<AcademicStructure> findByAdminAndCourse(Admin admin, Course course);
    List<AcademicStructure> findByAdminAndBatch(Admin admin, Batch batch);

    @Query("select distinct a.yearLabel from AcademicStructure a where a.admin = :admin and a.course = :course and a.batch = :batch order by a.yearLabel")
    List<String> findYearLabels(@Param("admin") Admin admin, @Param("course") Course course, @Param("batch") Batch batch);

    @Query("select distinct a.semesterNumber from AcademicStructure a where a.admin = :admin and a.course = :course and a.batch = :batch and a.yearLabel = :yearLabel order by a.semesterNumber")
    List<Integer> findSemesters(@Param("admin") Admin admin, @Param("course") Course course, @Param("batch") Batch batch, @Param("yearLabel") String yearLabel);

    @Query("select distinct a.section from AcademicStructure a where a.admin = :admin and a.course = :course and a.batch = :batch and a.yearLabel = :yearLabel and a.semesterNumber = :semester order by a.section")
    List<String> findSections(@Param("admin") Admin admin, @Param("course") Course course, @Param("batch") Batch batch,
                              @Param("yearLabel") String yearLabel, @Param("semester") Integer semester);

    @Query("select distinct a.semesterNumber from AcademicStructure a where a.admin = :admin order by a.semesterNumber")
    List<Integer> findDistinctSemesterNumbersByAdmin(@Param("admin") Admin admin);

    @Query("select distinct a.semesterNumber from AcademicStructure a where a.admin = :admin and a.batch.id = :batchId order by a.semesterNumber")
    List<Integer> findDistinctSemesterNumbersByAdminAndBatchId(@Param("admin") Admin admin, @Param("batchId") Long batchId);

    @Query("""
            select distinct a.section
            from AcademicStructure a
            where a.admin = :admin
              and a.batch.id = :batchId
              and a.semesterNumber = :semester
            order by a.section
            """)
    List<String> findDistinctSectionsByAdminAndBatchIdAndSemesterNumber(@Param("admin") Admin admin,
                                                                        @Param("batchId") Long batchId,
                                                                        @Param("semester") Integer semester);

    AcademicStructure findByAdminAndCourseAndBatchAndYearLabelAndSemesterNumberAndSection(
            Admin admin, Course course, Batch batch, String yearLabel, Integer semesterNumber, String section);
}
