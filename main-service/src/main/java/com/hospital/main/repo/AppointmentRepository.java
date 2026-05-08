package com.hospital.main.repo;

import com.hospital.main.domain.Appointment;
import com.hospital.main.domain.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    Optional<Appointment> findByDoctorIdAndAppointmentDateAndSlotStartAndStatus(
        Long doctorId, LocalDate date, LocalDateTime slotStart, AppointmentStatus status);

    List<Appointment> findByDoctorIdAndAppointmentDateAndStatusOrderBySlotStart(
        Long doctorId, LocalDate date, AppointmentStatus status);

    List<Appointment> findByAppointmentDateAndStatusOrderBySlotStart(LocalDate date, AppointmentStatus status);

    void deleteByDoctorId(Long doctorId);
}
