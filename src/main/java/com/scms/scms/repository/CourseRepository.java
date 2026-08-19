package com.scms.scms.repository;

import com.scms.scms.model.Admin;
import com.scms.scms.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByAdminOrderByCodeAsc(Admin admin);
    Course findByAdminAndCodeIgnoreCase(Admin admin, String code);
    Course findByAdminAndNameIgnoreCase(Admin admin, String name);
}
