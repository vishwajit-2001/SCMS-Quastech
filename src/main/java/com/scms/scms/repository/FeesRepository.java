package com.scms.scms.repository;

import com.scms.scms.model.Admin;
import com.scms.scms.model.Batch;
import com.scms.scms.model.Fees;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeesRepository extends JpaRepository<Fees, Long> {

    List<Fees> findByAdmin(Admin admin);
    List<Fees> findByAdminAndBatch(Admin admin, Batch batch);
    Fees findByAdminAndCourseIgnoreCaseAndAcademicYearIgnoreCaseAndSemesterIgnoreCase(Admin admin, String course, String academicYear, String semester);
    Fees findByAdminAndCourseIgnoreCaseAndAcademicYearIgnoreCaseAndSemesterIsNull(Admin admin, String course, String academicYear);
    Fees findByAdminAndCourseIgnoreCaseAndAcademicYearIsNullAndSemesterIsNull(Admin admin, String course);
}
