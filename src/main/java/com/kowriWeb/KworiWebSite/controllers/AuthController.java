// ═══════════════════════════════════════════════════════════
// FILE: controller/AuthController.java
// Public endpoints — no auth required
// ═══════════════════════════════════════════════════════════
package com.kowriWeb.KworiWebSite.controllers;

import com.kowriWeb.KworiWebSite.dto.*;
import com.kowriWeb.KworiWebSite.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /** POST /api/auth/register  → creates a USER */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(userService.registerUser(request));
    }

    /** POST /api/auth/login  → works for USER, ADMIN, SUPER_ADMIN */
    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }
}





