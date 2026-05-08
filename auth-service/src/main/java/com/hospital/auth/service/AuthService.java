package com.hospital.auth.service;

import com.hospital.auth.api.dto.*;
import com.hospital.auth.domain.Role;
import com.hospital.auth.domain.User;
import com.hospital.auth.repo.UserRepository;
import com.hospital.auth.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String token = jwtService.generateToken(user.getId(), user.getRole().name(), user.getEmail());
        return new LoginResponse(token, user.getRole().name());
    }

    public InternalRegisterResponse registerInternal(InternalRegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }
        User user = new User();
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.valueOf(request.role()));
        user = userRepository.save(user);
        return new InternalRegisterResponse(user.getId());
    }

    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }
}
