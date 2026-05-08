package com.hospital.auth.api.dto;

public record LoginResponse(String token, String role) {
}
