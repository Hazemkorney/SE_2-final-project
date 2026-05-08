package com.hospital.auth.api;

import com.hospital.auth.api.dto.*;
import com.hospital.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final String internalToken;

    public AuthController(AuthService authService, @Value("${internal.token}") String internalToken) {
        this.authService = authService;
        this.internalToken = internalToken;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register-internal")
    public ResponseEntity<InternalRegisterResponse> registerInternal(
        @RequestHeader(value = "X-Internal-Token", required = false) String providedToken,
        @RequestBody InternalRegisterRequest request) {
        assertInternalToken(providedToken);
        return ResponseEntity.ok(authService.registerInternal(request));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(
        @RequestHeader(value = "X-Internal-Token", required = false) String providedToken,
        @PathVariable Long userId) {
        assertInternalToken(providedToken);
        authService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    private void assertInternalToken(String providedToken) {
        if (providedToken == null || !internalToken.equals(providedToken)) {
            throw new AccessDeniedException("Internal endpoint is not publicly accessible");
        }
    }
}
