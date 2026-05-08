package com.hospital.auth.api.dto;

public record InternalRegisterRequest(String email, String password, String role) {
}
