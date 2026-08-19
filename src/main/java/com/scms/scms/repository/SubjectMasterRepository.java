package com.scms.scms.repository;

import com.scms.scms.model.Admin;
import com.scms.scms.model.Course;
import com.scms.scms.model.SubjectMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubjectMasterRepository extends JpaRepository<SubjectMaster, Long> {
    List<SubjectMaster> findByAdminAndCourseOrderBySemesterNumberAscNameAsc(Admin admin, Course course);
    List<SubjectMaster> findByAdminAndCourse(Admin admin, Course course);
    List<SubjectMaster> findByAdminAndCourseAndStatusIgnoreCaseOrderBySemesterNumberAscNameAsc(Admin admin, Course course, String status);
    SubjectMaster findByAdminAndCourseAndSemesterNumberAndCodeIgnoreCase(Admin admin, Course course, Integer semesterNumber, String code);
    SubjectMaster findByAdminAndCourseAndSemesterNumberAndCodeIgnoreCaseAndIdNot(Admin admin, Course course, Integer semesterNumber, String code, Long id);
}
