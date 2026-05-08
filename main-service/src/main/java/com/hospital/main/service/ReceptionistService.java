package com.hospital.main.service;

import com.hospital.main.aop.RequiresRole;
import com.hospital.main.domain.*;
import com.hospital.main.repo.*;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class ReceptionistService {
    private static final ZoneId EGYPT_ZONE = ZoneId.of("Africa/Cairo");
    private final DepartmentRepository departmentRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;

    public ReceptionistService(DepartmentRepository departmentRepository, DoctorRepository doctorRepository,
                               AppointmentRepository appointmentRepository, PatientRepository patientRepository) {
        this.departmentRepository = departmentRepository;
        this.doctorRepository = doctorRepository;
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
    }

    @RequiresRole({"RECEPTIONIST"})
    public List<Department> departments() { return departmentRepository.findAll(); }
    @RequiresRole({"RECEPTIONIST"})
    public List<Doctor> doctorsByDepartment(Long deptId) { return doctorRepository.findByDepartmentId(deptId); }

    @RequiresRole({"RECEPTIONIST"})
    public List<Map<String, Object>> slots(Long doctorId) {
        LocalDate today = LocalDate.now(EGYPT_ZONE);
        LocalTime now = LocalTime.now(EGYPT_ZONE);
        Map<LocalTime, Boolean> booked = new HashMap<>();
        appointmentRepository.findByDoctorIdAndAppointmentDateAndStatusOrderBySlotStart(doctorId, today, AppointmentStatus.BOOKED)
            .forEach(a -> booked.put(a.getSlotStart().toLocalTime(), true));

        List<Map<String, Object>> result = new ArrayList<>();
        LocalTime start = LocalTime.of(9, 0);
        for (int i = 0; i < 16; i++) {
            LocalTime slotStart = start.plusMinutes(30L * i);
            LocalTime slotEnd = slotStart.plusMinutes(30);
            result.add(Map.of(
                "slotStart", slotStart.toString(),
                "slotEnd", slotEnd.toString(),
                "available", !booked.containsKey(slotStart) && slotStart.isAfter(now),
                "isPast", !slotStart.isAfter(now),
                "booked", booked.containsKey(slotStart)
            ));
        }
        return result;
    }

    @RequiresRole({"RECEPTIONIST"})
    public List<Map<String, Object>> todayAppointments() {
        return appointmentRepository.findByAppointmentDateAndStatusOrderBySlotStart(LocalDate.now(EGYPT_ZONE), AppointmentStatus.BOOKED)
            .stream()
            .map(a -> {
                Map<String, Object> row = new HashMap<>();
                row.put("id", a.getId());
                row.put("doctorId", a.getDoctor().getId());
                row.put("doctorName", a.getDoctor().getName());
                row.put("slotStart", a.getSlotStart().toLocalTime().toString());
                row.put("slotEnd", a.getSlotEnd().toLocalTime().toString());
                row.put("patientName", a.getPatient().getName());
                row.put("patientPhone", a.getPatient().getPhone());
                return row;
            })
            .toList();
    }

    @RequiresRole({"RECEPTIONIST"})
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Appointment book(Map<String, String> request) {
        try {
            Long doctorId = Long.valueOf(request.get("doctorId"));
            LocalTime requestedTime = LocalTime.parse(request.get("slotStart"));
            LocalDate today = LocalDate.now(EGYPT_ZONE);
            LocalDateTime slotStart = LocalDateTime.of(today, requestedTime);
            LocalDateTime slotEnd = slotStart.plusMinutes(30);
            if (!slotStart.isAfter(LocalDateTime.now(EGYPT_ZONE))) {
                throw new IllegalStateException("Cannot book a past slot");
            }
            if (appointmentRepository.findByDoctorIdAndAppointmentDateAndSlotStartAndStatus(
                doctorId, today, slotStart, AppointmentStatus.BOOKED).isPresent()) {
                throw new IllegalStateException("Slot already booked");
            }
            Doctor doctor = doctorRepository.findById(doctorId).orElseThrow();
            Patient patient = patientRepository.findByPhone(request.get("patientPhone")).orElseGet(() -> {
                Patient p = new Patient();
                p.setName(request.get("patientName"));
                p.setPhone(request.get("patientPhone"));
                return patientRepository.save(p);
            });
            Appointment appointment = new Appointment();
            appointment.setDoctor(doctor);
            appointment.setPatient(patient);
            appointment.setAppointmentDate(today);
            appointment.setSlotStart(slotStart);
            appointment.setSlotEnd(slotEnd);
            appointment.setStatus(AppointmentStatus.BOOKED);
            return appointmentRepository.save(appointment);
        } catch (OptimisticLockingFailureException ex) {
            throw new IllegalStateException("Slot just got booked, please try another slot");
        }
    }

    @RequiresRole({"RECEPTIONIST"})
    @Transactional
    public void cancel(Long id) {
        Appointment appointment = appointmentRepository.findById(id).orElseThrow();
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);
    }

    @RequiresRole({"RECEPTIONIST"})
    @Transactional
    public Appointment reschedule(Long id, Map<String, String> request) {
        Appointment old = appointmentRepository.findById(id).orElseThrow();
        old.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(old);
        return book(Map.of(
            "doctorId", request.get("newDoctorId"),
            "slotStart", request.get("newSlotStart"),
            "patientName", old.getPatient().getName(),
            "patientPhone", old.getPatient().getPhone()
        ));
    }
}
