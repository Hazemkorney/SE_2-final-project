package com.hospital.main.service;

import com.hospital.main.aop.RequiresRole;
import com.hospital.main.api.dto.DoctorAppointmentResponse;
import com.hospital.main.domain.Appointment;
import com.hospital.main.domain.AppointmentStatus;
import com.hospital.main.domain.Doctor;
import com.hospital.main.repo.AppointmentRepository;
import com.hospital.main.repo.DoctorRepository;
import com.hospital.main.security.RequestUserHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class DoctorService {
    private static final ZoneId EGYPT_ZONE = ZoneId.of("Africa/Cairo");
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;

    public DoctorService(DoctorRepository doctorRepository, AppointmentRepository appointmentRepository) {
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @RequiresRole({"DOCTOR"})
    public List<DoctorAppointmentResponse> myAppointments() {
        Long userId = RequestUserHolder.get().userId();
        Doctor doctor = doctorRepository.findByUserId(userId).orElseThrow();
        List<Appointment> appointments = appointmentRepository.findByDoctorIdAndAppointmentDateAndStatusOrderBySlotStart(
            doctor.getId(), LocalDate.now(EGYPT_ZONE), AppointmentStatus.BOOKED);
        return appointments.stream()
            .map(a -> new DoctorAppointmentResponse(
                a.getSlotStart().toLocalTime().toString(),
                a.getSlotEnd().toLocalTime().toString(),
                a.getPatient().getName()
            ))
            .toList();
    }
}
