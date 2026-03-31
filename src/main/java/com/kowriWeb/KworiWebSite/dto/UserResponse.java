package com.kowriWeb.KworiWebSite.dto;

import com.kowriWeb.KworiWebSite.entity.Role;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UserResponse {
    private UUID id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private LocalDateTime createdAt;
    private Role role;
    private String token;
    private String refreshToken;
    private BigDecimal balance;   // ✅ ADD THIS
}