package com.kowriWeb.KworiWebSite.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "admin_table")
public class Admin {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private java.util.UUID id;

    private String fullName;

    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.ADMIN;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;
}
