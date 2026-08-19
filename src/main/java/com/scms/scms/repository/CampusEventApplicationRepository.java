package com.scms.scms.repository;

import com.scms.scms.model.CampusEvent;
import com.scms.scms.model.CampusEventApplication;
import com.scms.scms.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampusEventApplicationRepository extends JpaRepository<CampusEventApplication, Long> {
    List<CampusEventApplication> findByCampusEventOrderByAppliedAtDesc(CampusEvent campusEvent);
    List<CampusEventApplication> findByStudentOrderByAppliedAtDesc(Student student);
    long deleteByStudent(Student student);
    boolean existsByCampusEventAndEmail(CampusEvent campusEvent, String email);
}
