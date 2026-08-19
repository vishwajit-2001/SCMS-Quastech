package com.scms.scms.repository;

import com.scms.scms.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {

    Admin findByEmail(String email);
    Admin findByCollegeCodeIgnoreCase(String collegeCode);
    List<Admin> findByCollege_Id(Long collegeId);
    Admin findFirstByCollege_Id(Long collegeId);
    List<Admin> findByRoleIgnoreCase(String role);

    Admin findFirstByOrderByIdAsc();

    Admin findByEmailAndPassword(String email, String password);
}
