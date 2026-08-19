package com.scms.scms.repository;

import com.scms.scms.model.Admin;
import com.scms.scms.model.Batch;
import com.scms.scms.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BatchRepository extends JpaRepository<Batch, Long> {
    List<Batch> findByAdminOrderByDisplayNameAsc(Admin admin);
    List<Batch> findByAdminAndCourseOrderByDisplayNameAsc(Admin admin, Course course);
    Batch findByAdminAndCourseAndDisplayNameIgnoreCase(Admin admin, Course course, String displayName);
    Batch findByAdminAndCourseAndStartYearAndEndYear(Admin admin, Course course, Integer startYear, Integer endYear);
}
