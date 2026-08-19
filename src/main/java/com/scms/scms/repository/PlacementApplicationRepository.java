package com.scms.scms.repository;

import com.scms.scms.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlacementApplicationRepository extends JpaRepository<PlacementApplication, Long> {
    List<PlacementApplication> findByPlacementDriveOrderByAppliedAtDesc(PlacementDrive placementDrive);
    List<PlacementApplication> findByStudentOrderByAppliedAtDesc(Student student);
    long deleteByStudent(Student student);
    boolean existsByPlacementDriveAndEmail(PlacementDrive placementDrive, String email);
}
