package com.scms.scms.repository;

import com.scms.scms.model.PlacementDrive;
import com.scms.scms.model.PlacementTpo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlacementDriveRepository extends JpaRepository<PlacementDrive, Long> {
    List<PlacementDrive> findByPlacementTpoOrderByCreatedAtDesc(PlacementTpo placementTpo);
    List<PlacementDrive> findByPublishedTrueOrderByCreatedAtDesc();
    List<PlacementDrive> findByPublishedTrueAndEligibilityCourseOrderByCreatedAtDesc(String eligibilityCourse);
}
