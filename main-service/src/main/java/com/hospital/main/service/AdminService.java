package com.hospital.main.service;

import com.hospital.main.aop.RequiresRole;
import com.hospital.main.domain.*;
import com.hospital.main.repo.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class AdminService {
    private final DepartmentRepository departmentRepository;
    private final DoctorRepository doctorRepository;
    private final ReceptionistRepository receptionistRepository;
    private final AppointmentRepository appointmentRepository;
    private final WebClient webClient;
    private final String authServiceUrl;
    private final String internalToken;
    private final CompensationCleanupService compensationCleanupService;

    public AdminService(DepartmentRepository departmentRepository, DoctorRepository doctorRepository,
                        ReceptionistRepository receptionistRepository, AppointmentRepository appointmentRepository,
                        WebClient webClient, @Value("${auth-service.url}") String authServiceUrl,
                        @Value("${internal.token}") String internalToken,
                        CompensationCleanupService compensationCleanupService) {
        this.departmentRepository = departmentRepository;
        this.doctorRepository = doctorRepository;
        this.receptionistRepository = receptionistRepository;
        this.appointmentRepository = appointmentRepository;
        this.webClient = webClient;
        this.authServiceUrl = authServiceUrl;
        this.internalToken = internalToken;
        this.compensationCleanupService = compensationCleanupService;
    }

    @RequiresRole({"ADMIN"})
    public List<Department> departments() { return departmentRepository.findAll(); }
    @RequiresRole({"ADMIN"})
    public List<Doctor> doctors() { return doctorRepository.findAll(); }
    @RequiresRole({"ADMIN"})
    public List<Receptionist> receptionists() { return receptionistRepository.findAll(); }

    @RequiresRole({"ADMIN"})
    public Department createDepartment(Map<String, String> request) {
        Department department = new Department();
        department.setName(request.get("name"));
        return departmentRepository.save(department);
    }

    @RequiresRole({"ADMIN"})
    public void deleteDepartment(Long id) {
        if (doctorRepository.existsByDepartmentId(id)) {
            throw new IllegalStateException("Cannot delete department with existing doctors.");
        }
        departmentRepository.deleteById(id);
    }

    @RequiresRole({"ADMIN"})
    @Transactional
    public Doctor createDoctor(Map<String, Object> request) {
        Long userId = registerAuthUser((String) request.get("email"), (String) request.get("password"), "DOCTOR");
        try {
            Department department = departmentRepository.findById(Long.valueOf(request.get("departmentId").toString()))
                .orElseThrow();
            Doctor doctor = new Doctor();
            doctor.setName((String) request.get("name"));
            doctor.setSpecialization((String) request.get("specialization"));
            doctor.setDepartment(department);
            doctor.setUserId(userId);
            return doctorRepository.save(doctor);
        } catch (Exception ex) {
            deleteAuthUser(userId);
            throw ex;
        }
    }

    @RequiresRole({"ADMIN"})
    @Transactional
    public Receptionist createReceptionist(Map<String, String> request) {
        Long userId = registerAuthUser(request.get("email"), request.get("password"), "RECEPTIONIST");
        try {
            Receptionist receptionist = new Receptionist();
            receptionist.setName(request.get("name"));
            receptionist.setUserId(userId);
            return receptionistRepository.save(receptionist);
        } catch (Exception ex) {
            deleteAuthUser(userId);
            throw ex;
        }
    }

    @RequiresRole({"ADMIN"})
    @Transactional
    public void deleteDoctor(Long id) {
        Doctor doctor = doctorRepository.findById(id).orElseThrow();
        deleteAuthUser(doctor.getUserId());
        try {
            appointmentRepository.deleteByDoctorId(id);
            doctorRepository.delete(doctor);
        } catch (Exception ex) {
            compensationCleanupService.queueDoctorCleanup(id, doctor.getUserId(), ex.getMessage());
            throw new IllegalStateException("Doctor auth account deleted; local cleanup queued for retry");
        }
    }

    @RequiresRole({"ADMIN"})
    @Transactional
    public void deleteReceptionist(Long id) {
        Receptionist receptionist = receptionistRepository.findById(id).orElseThrow();
        deleteAuthUser(receptionist.getUserId());
        try {
            receptionistRepository.delete(receptionist);
        } catch (Exception ex) {
            compensationCleanupService.queueReceptionistCleanup(id, receptionist.getUserId(), ex.getMessage());
            throw new IllegalStateException("Receptionist auth account deleted; local cleanup queued for retry");
        }
    }

    private Long registerAuthUser(String email, String password, String role) {
        Map<?, ?> response = webClient.post()
            .uri(authServiceUrl + "/auth/register-internal")
            .header("X-Internal-Token", internalToken)
            .bodyValue(Map.of("email", email, "password", password, "role", role))
            .retrieve()
            .bodyToMono(Map.class)
            .block();
        return Long.valueOf(response.get("userId").toString());
    }

    private void deleteAuthUser(Long userId) {
        webClient.delete()
            .uri(authServiceUrl + "/auth/users/" + userId)
            .header("X-Internal-Token", internalToken)
            .retrieve()
            .toBodilessEntity()
            .block();
    }
}
