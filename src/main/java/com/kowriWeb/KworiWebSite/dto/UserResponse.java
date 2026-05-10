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
    private Role role;
    private BigDecimal balance;
    private LocalDateTime createdAt;
    // tokens only populated on register/login
    private String token;
    private String refreshToken;
}
