// ═══════════════════════════════════════════════════════════
// FILE: controller/UserController.java
// Authenticated user endpoints
// ═══════════════════════════════════════════════════════════
package com.kowriWeb.KworiWebSite.controllers;

import com.kowriWeb.KworiWebSite.dto.*;
import com.kowriWeb.KworiWebSite.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** GET /api/users/{id}  → own profile (or admin lookup) */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    /** PUT /api/users/{id}  → update own profile */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID id,
            @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    /** GET /api/users/{id}/financial-summary */
    @GetMapping("/{id}/financial-summary")
    @PreAuthorize("hasAnyRole('USER','ADMIN','SUPER_ADMIN')")
    public ResponseEntity<UserFinancialSummaryResponse> financialSummary(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getFinancialSummary(id));
    }
}