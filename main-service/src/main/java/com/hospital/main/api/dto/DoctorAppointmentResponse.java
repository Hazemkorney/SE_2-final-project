package com.hospital.main.api.dto;

public record DoctorAppointmentResponse(String slotStart, String slotEnd, String patientName) {
}
