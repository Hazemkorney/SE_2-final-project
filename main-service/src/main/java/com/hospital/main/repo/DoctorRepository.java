package com.hospital.main.repo;

import com.hospital.main.domain.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    List<Doctor> findByDepartmentId(Long departmentId);
    Optional<Doctor> findByUserId(Long userId);
    boolean existsByDepartmentId(Long departmentId);
}
