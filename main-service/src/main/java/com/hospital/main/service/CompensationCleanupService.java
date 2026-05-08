package com.hospital.main.service;

import com.hospital.main.domain.Doctor;
import com.hospital.main.domain.Receptionist;
import com.hospital.main.repo.AppointmentRepository;
import com.hospital.main.repo.DoctorRepository;
import com.hospital.main.repo.ReceptionistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class CompensationCleanupService {
    private static final Logger log = LoggerFactory.getLogger(CompensationCleanupService.class);
    private final Queue<CleanupTask> tasks = new ConcurrentLinkedQueue<>();
    private final DoctorRepository doctorRepository;
    private final ReceptionistRepository receptionistRepository;
    private final AppointmentRepository appointmentRepository;

    public CompensationCleanupService(DoctorRepository doctorRepository,
                                      ReceptionistRepository receptionistRepository,
                                      AppointmentRepository appointmentRepository) {
        this.doctorRepository = doctorRepository;
        this.receptionistRepository = receptionistRepository;
        this.appointmentRepository = appointmentRepository;
    }

    public void queueDoctorCleanup(Long doctorId, Long userId, String reason) {
        log.warn("Compensation queued for doctor cleanup: doctorId={}, orphanUserId={}, reason={}", doctorId, userId, reason);
        tasks.add(new CleanupTask("DOCTOR", doctorId, userId, 0));
    }

    public void queueReceptionistCleanup(Long receptionistId, Long userId, String reason) {
        log.warn("Compensation queued for receptionist cleanup: receptionistId={}, orphanUserId={}, reason={}",
            receptionistId, userId, reason);
        tasks.add(new CleanupTask("RECEPTIONIST", receptionistId, userId, 0));
    }

    @Scheduled(fixedDelay = 15000)
    public void processRetries() {
        CleanupTask task;
        while ((task = tasks.poll()) != null) {
            try {
                if ("DOCTOR".equals(task.type())) {
                    Doctor doctor = doctorRepository.findById(task.entityId()).orElse(null);
                    if (doctor != null) {
                        appointmentRepository.deleteByDoctorId(task.entityId());
                        doctorRepository.delete(doctor);
                    }
                } else {
                    Receptionist receptionist = receptionistRepository.findById(task.entityId()).orElse(null);
                    if (receptionist != null) {
                        receptionistRepository.delete(receptionist);
                    }
                }
                log.info("Compensation cleanup success: type={}, entityId={}, orphanUserId={}",
                    task.type(), task.entityId(), task.orphanUserId());
            } catch (Exception ex) {
                int retries = task.retries() + 1;
                if (retries <= 5) {
                    tasks.add(new CleanupTask(task.type(), task.entityId(), task.orphanUserId(), retries));
                    log.warn("Compensation retry {} failed for type={}, entityId={}, orphanUserId={}, error={}",
                        retries, task.type(), task.entityId(), task.orphanUserId(), ex.getMessage());
                } else {
                    log.error("Compensation permanently failed for type={}, entityId={}, orphanUserId={}",
                        task.type(), task.entityId(), task.orphanUserId(), ex);
                }
            }
        }
    }

    private record CleanupTask(String type, Long entityId, Long orphanUserId, int retries) {
    }
}
