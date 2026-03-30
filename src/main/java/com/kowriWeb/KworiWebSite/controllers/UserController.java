package com.kowriWeb.KworiWebSite.controllers;

import com.kowriWeb.KworiWebSite.dto.*;
import com.kowriWeb.KworiWebSite.services.UserRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRegistrationService userRegistrationService;

    // ── Register ────────────────────────────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userRegistrationService.createUser(request));
    }

    // ── Login ───────────────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(userRegistrationService.userLogin(request));
    }

    // ── Get own profile ─────────────────────────────────────────────────
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userRegistrationService.getUserDetails(id));
    }

    // ── Update own profile ──────────────────────────────────────────────
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID id,
            @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userRegistrationService.updateUser(id, request));
    }
}