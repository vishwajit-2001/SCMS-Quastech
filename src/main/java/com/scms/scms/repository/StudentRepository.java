package com.scms.scms.repository;

import com.scms.scms.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Student findByEmail(String email);
    Student findByEmailIgnoreCase(String email);
    @Query("select s from Student s where lower(trim(s.email)) = lower(trim(:email))")
    Student findByEmailNormalized(@Param("email") String email);
    List<Student> findByAdmin(Admin admin);
    List<Student> findByAdminAndBatch(Admin admin, Batch batch);
    List<Student> findByClassRoom(ClassRoom classRoom);
    List<Student> findByAdminAndClassRoom(Admin admin, ClassRoom classRoom);
    List<Student> findByAdminAndAdmissionStatusIgnoreCase(Admin admin, String admissionStatus);
    List<Student> findByAdmissionStatusIgnoreCase(String admissionStatus);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByMobileNumberIgnoreCase(String mobileNumber);
    boolean existsByAdminAndEmailIgnoreCase(Admin admin, String email);
    boolean existsByAdminAndEmailIgnoreCaseAndIdNot(Admin admin, String email, Long id);
    boolean existsByAdminAndEnrollmentNoIgnoreCase(Admin admin, String enrollmentNo);
    boolean existsByAdminAndEnrollmentNoIgnoreCaseAndIdNot(Admin admin, String enrollmentNo, Long id);
}
