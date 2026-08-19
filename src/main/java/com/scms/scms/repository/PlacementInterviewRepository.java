package com.scms.scms.repository;

import com.scms.scms.model.PlacementDrive;
import com.scms.scms.model.PlacementInterview;
import com.scms.scms.model.PlacementTpo;
import com.scms.scms.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PlacementInterviewRepository extends JpaRepository<PlacementInterview, Long> {
    List<PlacementInterview> findByPlacementTpoOrderByInterviewDateTimeDesc(PlacementTpo placementTpo);
    List<PlacementInterview> findByPlacementDriveOrderByInterviewDateTimeDesc(PlacementDrive placementDrive);
    List<PlacementInterview> findByPlacementDriveInOrderByInterviewDateTimeAsc(List<PlacementDrive> placementDrives);

    @Query("select distinct i from PlacementInterview i join i.selectedStudents s where s = :student order by i.interviewDateTime asc")
    List<PlacementInterview> findForSelectedStudent(@Param("student") Student student);
}
