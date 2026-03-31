package com.kowriWeb.KworiWebSite.services;

import com.kowriWeb.KworiWebSite.Config.Security.TokenService;
import com.kowriWeb.KworiWebSite.dto.*;
import com.kowriWeb.KworiWebSite.entity.User;
import com.kowriWeb.KworiWebSite.entity.Role;          // ← explicit import
import com.kowriWeb.KworiWebSite.entity.repos.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import com.kowriWeb.KworiWebSite.dto.UserFinancialSummaryResponse;
import com.kowriWeb.KworiWebSite.entity.repos.DepositRepo;
import com.kowriWeb.KworiWebSite.entity.repos.WithdrawalRepo;
import com.kowriWeb.KworiWebSite.entity.DepositStatus;
import com.kowriWeb.KworiWebSite.entity.WithdrawalStatus;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRegistrationService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepo userRepo;
    private final TokenService tokenService;
    private final DepositRepo depositRepo;
    private final WithdrawalRepo withdrawalRepo;


    public UserResponse createUser(CreateUserRequest request) {
        // Check for duplicate email (keep this one for data integrity)
        if (userRepo.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        // REMOVED: phone number duplicate check
        // REMOVED: password confirmation check

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        User savedUser = userRepo.save(user);
        log.info("New user registered: {}", savedUser.getEmail());

        String accessToken  = tokenService.generateAccessToken(savedUser);
        String refreshToken = tokenService.generateRefreshToken(savedUser).getToken();

        return UserResponse.builder()
                .id(savedUser.getId())
                .fullName(savedUser.getFullName())
                .email(savedUser.getEmail())
                .phoneNumber(savedUser.getPhoneNumber())
                .createdAt(savedUser.getCreatedAt())
                .role(savedUser.getRole())
                .token(accessToken)
                .refreshToken(refreshToken)
                .build();
    }


    public LoginResponse userLogin(LoginRequest request) {
        User user = userRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        log.info("User logged in: {}", user.getEmail());

        String accessToken  = tokenService.generateAccessToken(user);
        String refreshToken = tokenService.generateRefreshToken(user).getToken();

        return LoginResponse.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .token(accessToken)
                .refreshToken(refreshToken)
                .build();
    }


    public UserResponse getUserDetails(UUID id) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("Fetching details for user: {}", user.getEmail());

        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .createdAt(user.getCreatedAt())
                .role(user.getRole())
                .balance(user.getBalance())   // ✅ ADD THIS
                .build();
    }

    public UserFinancialSummaryResponse getUserFinancialSummary(UUID userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        BigDecimal totalDeposited = depositRepo
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(d -> d.getStatus() == DepositStatus.APPROVED)
                .map(d -> d.getAmount() != null ? d.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRewards = depositRepo
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(d -> d.getStatus() == DepositStatus.APPROVED)
                .map(d -> d.getRewardAmount() != null ? d.getRewardAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalWithdrawn = withdrawalRepo
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .filter(w -> w.getStatus() == WithdrawalStatus.APPROVED)
                .map(w -> w.getAmount() != null ? w.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Financial summary fetched for user: {}", user.getEmail());

        return UserFinancialSummaryResponse.builder()
                .balance(user.getBalance())
                .totalDeposited(totalDeposited)
                .totalRewardsEarned(totalRewards)
                .totalWithdrawn(totalWithdrawn)
                .build();
    }


    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        User user = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepo.findByEmail(request.getEmail()).isPresent()) {
                throw new RuntimeException("Email already in use");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().equals(user.getPhoneNumber())) {
            if (userRepo.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
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
        User updatedUser = userRepo.save(user);
        log.info("User updated: {}", updatedUser.getEmail());

        return UserResponse.builder()
                .id(updatedUser.getId())
                .fullName(updatedUser.getFullName())
                .email(updatedUser.getEmail())
                .phoneNumber(updatedUser.getPhoneNumber())
                .createdAt(updatedUser.getCreatedAt())
                .role(updatedUser.getRole())
                .build();
    }
}