package com.hospital.main.api;

import com.hospital.main.api.dto.DoctorAppointmentResponse;
import com.hospital.main.service.DoctorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/doctor")
public class DoctorController {
    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @GetMapping("/my-appointments")
    public List<DoctorAppointmentResponse> myAppointments() {
        return doctorService.myAppointments();
    }
}
