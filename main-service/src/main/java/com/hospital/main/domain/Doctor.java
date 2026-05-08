package com.hospital.main.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "doctors")
public class Doctor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    private String specialization;
    @ManyToOne(optional = false)
    @JoinColumn(name = "department_id")
    private Department department;
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }
    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
