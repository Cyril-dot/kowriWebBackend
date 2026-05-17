package com.kowriWeb.KworiWebSite.Config.Security;

import com.kowriWeb.KworiWebSite.entity.repos.UserRepo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UserRepo userRepo;

    private static final List<String> PUBLIC_ENDPOINTS = Arrays.asList(
            "/api/auth/register",
            "/api/auth/login",
            "/api/v1/payment/webhook",
            "/ping"
    );

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        boolean isPublic = PUBLIC_ENDPOINTS.stream().anyMatch(path::startsWith);

        if (isPublic) {
            log.info("🟢 Skipping JWT filter for public endpoint: {}", path);
            return true;
        }

        return path.startsWith("/actuator/")
                || path.startsWith("/ws/")
                || path.startsWith("/ws-meeting/")
                || path.equals("/favicon.ico")
                || path.startsWith("/.well-known/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        log.info("🔐 Processing JWT for: {}", path);

        var existingAuth = SecurityContextHolder.getContext().getAuthentication();
        if (existingAuth != null
                && existingAuth.isAuthenticated()
                && !(existingAuth instanceof AnonymousAuthenticationToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String header = request.getHeader("Authorization");

            if (header == null || !header.startsWith("Bearer ")) {
                log.warn("⚠️ No JWT token found for request: {}", path);
                filterChain.doFilter(request, response);
                return;
            }

            String token = header.substring(7);
            String email = tokenService.getEmailFromAccessToken(token);

            log.info("🔍 Token parsed → Email: {}", email);

            var userOptional = userRepo.findByEmail(email);

            if (userOptional.isEmpty()) {
                log.warn("❌ User not found: {}", email);
                filterChain.doFilter(request, response);
                return;
            }

            UserDetails userDetails = new UserPrincipal(userOptional.get());

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

            SecurityContextHolder.getContext().setAuthentication(authToken);

            log.info("✅ Authenticated successfully: {}", email);

        } catch (Exception e) {
            log.error("💥 JWT authentication error: {}", e.getMessage(), e);
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}