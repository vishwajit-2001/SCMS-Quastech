package com.scms.scms.repository;

import com.scms.scms.model.College;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CollegeRepository extends JpaRepository<College, Long> {

    College findByCodeIgnoreCase(String code);

    College findByNameIgnoreCase(String name);

    List<College> findAllByOrderByNameAsc();
}
