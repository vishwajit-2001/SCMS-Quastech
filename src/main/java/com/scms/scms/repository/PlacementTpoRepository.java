package com.scms.scms.repository;

import com.scms.scms.model.Admin;
import com.scms.scms.model.PlacementTpo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlacementTpoRepository extends JpaRepository<PlacementTpo, Long> {
    List<PlacementTpo> findByEmail(String email);
    List<PlacementTpo> findByAdminOrderByNameAsc(Admin admin);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmployeeIdIgnoreCase(String employeeId);
    boolean existsByAdminAndEmployeeIdIgnoreCase(Admin admin, String employeeId);
    PlacementTpo findByEmailAndPassword(String email, String password);
}
