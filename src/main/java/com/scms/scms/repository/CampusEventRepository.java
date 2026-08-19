package com.scms.scms.repository;

import com.scms.scms.model.CampusEvent;
import com.scms.scms.model.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampusEventRepository extends JpaRepository<CampusEvent, Long> {
    List<CampusEvent> findByPublishedTrueOrderByCreatedAtDesc();
    List<CampusEvent> findByTeacherOrderByCreatedAtDesc(Teacher teacher);
}
