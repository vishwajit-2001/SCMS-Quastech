package com.scms.scms.repository;

import com.scms.scms.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Teacher findByEmail(String email);
    Optional<Teacher> findByIdAndEmailIgnoreCase(Long id, String email);
    Teacher findByAdminAndEmailIgnoreCase(Admin admin, String email);
    List<Teacher> findByAdmin(Admin admin);
    List<Teacher> findByAdminAndStatusIgnoreCaseOrderByNameAsc(Admin admin, String status);
    boolean existsByAdminAndEmailIgnoreCase(Admin admin, String email);
    boolean existsByEmployeeIdIgnoreCase(String employeeId);
    boolean existsByAdminAndEmployeeIdIgnoreCase(Admin admin, String employeeId);
}
