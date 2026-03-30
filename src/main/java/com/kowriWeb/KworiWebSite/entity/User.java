package com.kowriWeb.KworiWebSite.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "user_table")
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String fullName;

    @Column(unique = true)
    private String email;

    private String password;

    @Column(unique = true)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.USER;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;
}
