package com.kowriWeb.KworiWebSite.controllers;

import com.kowriWeb.KworiWebSite.dto.*;
import com.kowriWeb.KworiWebSite.services.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // ── Registration (protected by secret key inside service) ──────────
    @PostMapping("/register")
    public ResponseEntity<AdminResponse> register(@RequestBody CreateAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminService.registerAdmin(request));
    }

    // ── Login ───────────────────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<AdminLoginResponse> login(@RequestBody AdminLoginRequest request) {
        return ResponseEntity.ok(adminService.adminLogin(request));
    }

    // ── View all users (ADMIN role required) ───────────────────────────
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserSummaryResponse>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    // ── View single user details (ADMIN role required) ─────────────────
    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserDetails(@PathVariable UUID userId) {
        return ResponseEntity.ok(adminService.getUserDetails(userId));
    }
}