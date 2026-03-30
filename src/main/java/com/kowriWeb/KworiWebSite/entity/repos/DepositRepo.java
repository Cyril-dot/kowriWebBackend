package com.kowriWeb.KworiWebSite.entity.repos;

import com.kowriWeb.KworiWebSite.entity.Deposit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DepositRepo extends JpaRepository<Deposit, UUID> {

    // All deposits for a specific user, newest first
    List<Deposit> findByUserIdOrderByCreatedAtDesc(UUID userId);

    // All deposits across all users, newest first (for admin)
    List<Deposit> findAllByOrderByCreatedAtDesc();
}