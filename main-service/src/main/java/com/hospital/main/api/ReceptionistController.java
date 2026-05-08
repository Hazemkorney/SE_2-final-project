package com.hospital.main.api;

import com.hospital.main.domain.Appointment;
import com.hospital.main.domain.Department;
import com.hospital.main.domain.Doctor;
import com.hospital.main.service.ReceptionistService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/receptionist")
public class ReceptionistController {
    private final ReceptionistService receptionistService;

    public ReceptionistController(ReceptionistService receptionistService) {
        this.receptionistService = receptionistService;
    }

    @GetMapping("/departments")
    public List<Department> departments() { return receptionistService.departments(); }

    @GetMapping("/departments/{deptId}/doctors")
    public List<Doctor> doctors(@PathVariable Long deptId) { return receptionistService.doctorsByDepartment(deptId); }

    @GetMapping("/doctors/{doctorId}/slots")
    public List<Map<String, Object>> slots(@PathVariable Long doctorId) { return receptionistService.slots(doctorId); }

    @GetMapping("/appointments")
    public List<Map<String, Object>> todayAppointments() { return receptionistService.todayAppointments(); }

    @PostMapping("/appointments")
    public Appointment book(@RequestBody Map<String, String> req) { return receptionistService.book(req); }

    @DeleteMapping("/appointments/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id) { receptionistService.cancel(id); return ResponseEntity.noContent().build(); }

    @PutMapping("/appointments/{id}/reschedule")
    public Appointment reschedule(@PathVariable Long id, @RequestBody Map<String, String> req) {
        return receptionistService.reschedule(id, req);
    }
}
