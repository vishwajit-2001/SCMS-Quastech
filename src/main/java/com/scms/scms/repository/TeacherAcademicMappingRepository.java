package com.scms.scms.repository;

import com.scms.scms.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeacherAcademicMappingRepository extends JpaRepository<TeacherAcademicMapping, Long> {
    List<TeacherAcademicMapping> findByAdminOrderByTeacher_NameAscCourse_CodeAscBatch_DisplayNameAscAcademicYearAsc(Admin admin);
    List<TeacherAcademicMapping> findByTeacherOrderByCourse_CodeAscBatch_DisplayNameAscAcademicYearAsc(Teacher teacher);
    List<TeacherAcademicMapping> findByAdminAndTeacherOrderByCourse_CodeAscBatch_DisplayNameAscAcademicYearAsc(Admin admin, Teacher teacher);
    boolean existsByTeacherAndCourseAndBatchAndAcademicYearIgnoreCase(
            Teacher teacher, Course course, Batch batch, String academicYear);
    void deleteByTeacher(Teacher teacher);
    void deleteByAdminAndCourse(Admin admin, Course course);
    void deleteByAdminAndBatch(Admin admin, Batch batch);
}
