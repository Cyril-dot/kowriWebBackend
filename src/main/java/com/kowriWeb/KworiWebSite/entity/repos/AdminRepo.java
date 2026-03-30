package com.kowriWeb.KworiWebSite.entity.repos;

import com.kowriWeb.KworiWebSite.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdminRepo extends JpaRepository<Admin, UUID> {

    // Auth lookups
    Optional<Admin> findByEmail(String email);

    // Existence checks (registration validation)
    boolean existsByEmail(String email);
}