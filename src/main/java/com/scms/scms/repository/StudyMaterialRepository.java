package com.scms.scms.repository;

import com.scms.scms.model.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyMaterialRepository extends JpaRepository<StudyMaterial, Long> {
    List<StudyMaterial> findByClassRoomOrderByUploadedAtDesc(ClassRoom classRoom);
    List<StudyMaterial> findByTeacherOrderByUploadedAtDesc(Teacher teacher);
    List<StudyMaterial> findByAdminOrderByUploadedAtDesc(Admin admin);
    List<StudyMaterial> findAllByOrderByUploadedAtDesc();
}
