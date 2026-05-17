package com.kowriWeb.KworiWebSite.Config.Security;

import com.kowriWeb.KworiWebSite.entity.User;

import com.kowriWeb.KworiWebSite.entity.repos.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepo userRepo;

    @Override
    public UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {

        log.debug("🔍 Loading account by email: {}", email);

        return userRepo.findByEmail(email)
                .map(user -> {
                    log.debug("✅ User found: {}, Role: {}", user.getEmail(), user.getRole());
                    return (UserDetails) new UserPrincipal(user);
                })
                .orElseThrow(() -> {
                    log.warn("❌ No account found with email: {}", email);
                    return new UsernameNotFoundException("No user found with email: " + email);
                });
    }
}