package com.hospital.main.api;

import com.hospital.main.domain.Department;
import com.hospital.main.domain.Doctor;
import com.hospital.main.domain.Receptionist;
import com.hospital.main.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/departments")
    public List<Department> departments() { return adminService.departments(); }
    @PostMapping("/departments")
    public Department createDepartment(@RequestBody Map<String, String> req) { return adminService.createDepartment(req); }
    @DeleteMapping("/departments/{id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) { adminService.deleteDepartment(id); return ResponseEntity.noContent().build(); }

    @GetMapping("/doctors")
    public List<Doctor> doctors() { return adminService.doctors(); }
    @PostMapping("/doctors")
    public Doctor createDoctor(@RequestBody Map<String, Object> req) { return adminService.createDoctor(req); }
    @DeleteMapping("/doctors/{id}")
    public ResponseEntity<Void> deleteDoctor(@PathVariable Long id) { adminService.deleteDoctor(id); return ResponseEntity.noContent().build(); }

    @GetMapping("/receptionists")
    public List<Receptionist> receptionists() { return adminService.receptionists(); }
    @PostMapping("/receptionists")
    public Receptionist createReceptionist(@RequestBody Map<String, String> req) { return adminService.createReceptionist(req); }
    @DeleteMapping("/receptionists/{id}")
    public ResponseEntity<Void> deleteReceptionist(@PathVariable Long id) { adminService.deleteReceptionist(id); return ResponseEntity.noContent().build(); }
}
