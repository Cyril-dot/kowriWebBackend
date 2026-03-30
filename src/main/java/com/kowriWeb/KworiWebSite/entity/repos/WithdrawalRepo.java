package com.kowriWeb.KworiWebSite.entity.repos;

import com.kowriWeb.KworiWebSite.entity.Withdrawal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WithdrawalRepo extends JpaRepository<Withdrawal, UUID> {

    // All withdrawals for a specific user, newest first
    List<Withdrawal> findByUserIdOrderByCreatedAtDesc(UUID userId);

    // All withdrawals across all users, newest first (admin)
    List<Withdrawal> findAllByOrderByCreatedAtDesc();
}