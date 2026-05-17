package com.kowriWeb.KworiWebSite.services;

import com.kowriWeb.KworiWebSite.Config.Security.TokenService;
import com.kowriWeb.KworiWebSite.dto.*;
import com.kowriWeb.KworiWebSite.entity.*;
import com.kowriWeb.KworiWebSite.entity.repos.DepositRepo;
import com.kowriWeb.KworiWebSite.entity.repos.UserRepo;
import com.kowriWeb.KworiWebSite.entity.repos.WithdrawalRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepo        userRepo;
    private final DepositRepo     depositRepo;
    private final WithdrawalRepo  withdrawalRepo;
    private final PasswordEncoder passwordEncoder;
    private final TokenService    tokenService;

    // ──────────────────────────────────────────
    // REGISTER
    // ──────────────────────────────────────────

    /** Public self-registration → role = USER */
    public UserResponse registerUser(RegisterRequest request) {
        return register(request, Role.USER);
    }

    /** Called by a SUPER_ADMIN endpoint to create an admin account */
    public UserResponse registerAdmin(RegisterRequest request) {
        return register(request, Role.ADMIN);
    }

    private UserResponse register(RegisterRequest request, Role role) {
        if (userRepo.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(role)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User saved = userRepo.save(user);
        log.info("✅ Registered {} → {}", role, saved.getEmail());

        return buildAuthResponse(saved);
    }

    // ──────────────────────────────────────────
    // LOGIN
    // ──────────────────────────────────────────

    public UserResponse login(LoginRequest request) {
        User user = userRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        log.info("✅ Login: {} [{}]", user.getEmail(), user.getRole());
        return buildAuthResponse(user);
    }

    // ──────────────────────────────────────────
    // GET SINGLE USER
    // ──────────────────────────────────────────

    public UserResponse getUserById(UUID id) {
        User user = getOrThrow(id);
        log.info("Fetching user: {}", user.getEmail());
        return toFullResponse(user);
    }

    // ──────────────────────────────────────────
    // GET ALL USERS
    // ──────────────────────────────────────────

    public List<UserSummaryResponse> getAllUsers() {
        return userRepo.findAll()
                .stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    public List<UserSummaryResponse> getUsersByRole(Role role) {
        return userRepo.findAllByRole(role)
                .stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────
    // FINANCIAL SUMMARY
    // ──────────────────────────────────────────

    public UserFinancialSummaryResponse getFinancialSummary(UUID userId) {
        User user = getOrThrow(userId);

        BigDecimal totalDeposited = depositRepo
                .findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(d -> d.getStatus() == DepositStatus.APPROVED)
                .map(d -> d.getAmount() != null ? d.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRewards = depositRepo
                .findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(d -> d.getStatus() == DepositStatus.APPROVED)
                .map(d -> d.getRewardAmount() != null ? d.getRewardAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalWithdrawn = withdrawalRepo
                .findByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(w -> w.getStatus() == WithdrawalStatus.APPROVED)
                .map(w -> w.getAmount() != null ? w.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Financial summary for: {}", user.getEmail());

        return UserFinancialSummaryResponse.builder()
                .balance(user.getBalance())
                .totalDeposited(totalDeposited)
                .totalRewardsEarned(totalRewards)
                .totalWithdrawn(totalWithdrawn)
                .build();
    }

    // ──────────────────────────────────────────
    // UPDATE PROFILE
    // ──────────────────────────────────────────

    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        User user = getOrThrow(id);

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepo.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email already in use");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().equals(user.getPhoneNumber())) {
            if (userRepo.existsByPhoneNumber(request.getPhoneNumber())) {
                throw new RuntimeException("Phone number already in use");
            }
            user.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }

        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            if (request.getCurrentPassword() == null ||
                    !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new RuntimeException("Current password is incorrect");
            }
            if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
                throw new RuntimeException("New passwords do not match");
            }
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        }

        user.setUpdatedAt(LocalDateTime.now());
        User updated = userRepo.save(user);
        log.info("✅ User updated: {}", updated.getEmail());
        return toFullResponse(updated);
    }

    // ──────────────────────────────────────────
    // CHANGE ROLE  (super-admin only)
    // ──────────────────────────────────────────

    public UserResponse changeRole(UUID id, ChangeRoleRequest request) {
        User user = getOrThrow(id);
        user.setRole(request.getRole());
        user.setUpdatedAt(LocalDateTime.now());
        User updated = userRepo.save(user);
        log.info("✅ Role changed → {} for {}", updated.getRole(), updated.getEmail());
        return toFullResponse(updated);
    }

    // ──────────────────────────────────────────
    // PRIVATE HELPERS
    // ──────────────────────────────────────────

    private User getOrThrow(UUID id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private UserResponse buildAuthResponse(User user) {
        String accessToken  = tokenService.generateAccessToken(user);
        String refreshToken = tokenService.generateRefreshToken(user).getToken();

        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .balance(user.getBalance())
                .createdAt(user.getCreatedAt())
                .token(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    private UserResponse toFullResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .balance(user.getBalance())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private UserSummaryResponse toSummary(User user) {
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