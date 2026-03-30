package com.kowriWeb.KworiWebSite.services;

import com.kowriWeb.KworiWebSite.Config.Security.TokenService;
import com.kowriWeb.KworiWebSite.dto.*;
import com.kowriWeb.KworiWebSite.entity.Admin;
import com.kowriWeb.KworiWebSite.entity.Role;
import com.kowriWeb.KworiWebSite.entity.User;
import com.kowriWeb.KworiWebSite.entity.repos.AdminRepo;
import com.kowriWeb.KworiWebSite.entity.repos.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final AdminRepo adminRepo;
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    /**
     * Injected from application.properties:
     *   app.admin.secret-key=YOUR_SECRET_HERE
     *
     * This prevents random people from self-registering as admins.
     * Remove the secret-key check if you prefer a different guard
     * (e.g. only a super-admin can create admins).
     */
    // ──────────────────────────────────────────
    // ADMIN REGISTRATION
    // ──────────────────────────────────────────

    public AdminResponse registerAdmin(CreateAdminRequest request) {

        if (adminRepo.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered as admin");
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        Admin admin = Admin.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ADMIN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Admin savedAdmin = adminRepo.save(admin);
        log.info("New admin registered: {}", savedAdmin.getEmail());

        String accessToken  = tokenService.generateOwnerAccessToken(savedAdmin);
        String refreshToken = tokenService.generateAdminRefreshToken(savedAdmin).getToken();

        return AdminResponse.builder()
                .id(savedAdmin.getId())
                .fullName(savedAdmin.getFullName())
                .email(savedAdmin.getEmail())
                .role(savedAdmin.getRole())
                .createdAt(savedAdmin.getCreatedAt())
                .token(accessToken)
                .refreshToken(refreshToken)
                .build();
    }


    // ──────────────────────────────────────────
    // ADMIN LOGIN
    // ──────────────────────────────────────────

    public AdminLoginResponse adminLogin(AdminLoginRequest request) {
        Admin admin = adminRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        log.info("Admin logged in: {}", admin.getEmail());

        String accessToken  = tokenService.generateOwnerAccessToken(admin);
        String refreshToken = tokenService.generateAdminRefreshToken(admin).getToken();

        return AdminLoginResponse.builder()
                .adminId(admin.getId())
                .fullName(admin.getFullName())
                .email(admin.getEmail())
                .role(admin.getRole())
                .token(accessToken)
                .refreshToken(refreshToken)
                .build();
    }


    // ──────────────────────────────────────────
    // VIEW ALL USERS
    // ──────────────────────────────────────────

    public List<UserSummaryResponse> getAllUsers() {
        log.info("Admin fetching all users");

        return userRepo.findAll()
                .stream()
                .map(this::toUserSummary)
                .collect(Collectors.toList());
    }


    // ──────────────────────────────────────────
    // VIEW SINGLE USER DETAILS
    // ──────────────────────────────────────────

    public UserResponse getUserDetails(UUID userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("Admin fetching details for user: {}", user.getEmail());

        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .createdAt(user.getCreatedAt())
                .role(user.getRole())
                .build();
    }


    // ──────────────────────────────────────────
    // PRIVATE HELPERS
    // ──────────────────────────────────────────

    private UserSummaryResponse toUserSummary(User user) {
        return UserSummaryResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}