package com.kowriWeb.KworiWebSite.Config.Security;

import com.kowriWeb.KworiWebSite.Config.Security.entity.RefreshToken;
import com.kowriWeb.KworiWebSite.Config.Security.entity.RefreshTokenRepo;
import com.kowriWeb.KworiWebSite.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenService {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-expiration-ms}")
    private long accessExpirationMs;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private final RefreshTokenRepo refreshTokenRepo;
    private final TokenEncryptionService encryptionService;

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new RuntimeException("JWT secret key must be at least 32 bytes for HS256");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ===================== ACCESS TOKEN =====================

    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessExpirationMs);

        log.info("🔐 Generating access token for user: {}", user.getEmail());

        String plainToken = Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();

        String encryptedToken = encryptionService.encryptToken(plainToken);

        log.info("🔒 Generated encrypted access token for user: {}", user.getEmail());
        return encryptedToken;
    }

    // ===================== REFRESH TOKEN =====================

    public RefreshToken generateRefreshToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshExpirationMs);

        log.info("🔐 Generating refresh token for user: {}", user.getEmail());

        String plainRefreshToken = Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("tokenType", "REFRESH")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();

        String encryptedRefreshToken = encryptionService.encryptToken(plainRefreshToken);

        Instant expiryInstant = Instant.now().plusMillis(refreshExpirationMs);
        refreshTokenRepo.upsertUserRefreshToken(user.getId(), encryptedRefreshToken, expiryInstant);

        log.info("🔒 Generated encrypted refresh token for user: {}", user.getEmail());

        return refreshTokenRepo.findByUser(user)
                .orElseThrow(() -> new RuntimeException("Failed to create refresh token for user"));
    }

    // ===================== VALIDATION =====================

    public boolean validateAccessToken(String encryptedToken) {
        try {
            String plainToken = encryptionService.decryptToken(encryptedToken);

            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(plainToken)
                    .getPayload();

            log.debug("✅ Token validated for user: {}", claims.getSubject());
            return true;

        } catch (ExpiredJwtException ex) {
            log.warn("⚠️ Token expired for user: {}", ex.getClaims().getSubject());
            throw new RuntimeException("Token expired", ex);
        } catch (JwtException ex) {
            log.error("❌ Invalid JWT token: {}", ex.getMessage());
            throw new RuntimeException("Invalid token", ex);
        } catch (Exception ex) {
            log.error("❌ Token decryption failed: {}", ex.getMessage(), ex);
            throw new RuntimeException("Invalid or corrupted token", ex);
        }
    }

    public boolean validateRefreshToken(String encryptedRefreshToken) {
        try {
            RefreshToken refreshToken = refreshTokenRepo.findByToken(encryptedRefreshToken)
                    .orElseThrow(() -> new RuntimeException("Refresh token not found"));

            if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
                log.warn("⚠️ Refresh token expired for user: {}", refreshToken.getUser().getEmail());
                refreshTokenRepo.delete(refreshToken);
                throw new RuntimeException("Refresh token expired");
            }

            String plainToken = encryptionService.decryptToken(encryptedRefreshToken);

            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(plainToken);

            log.debug("✅ Refresh token validated for user: {}", refreshToken.getUser().getEmail());
            return true;

        } catch (ExpiredJwtException ex) {
            log.warn("⚠️ Refresh token expired");
            throw new RuntimeException("Refresh token expired", ex);
        } catch (JwtException ex) {
            log.error("❌ Invalid refresh token: {}", ex.getMessage());
            throw new RuntimeException("Invalid refresh token", ex);
        } catch (Exception ex) {
            log.error("❌ Refresh token validation failed: {}", ex.getMessage());
            throw new RuntimeException("Invalid or corrupted refresh token", ex);
        }
    }

    // ===================== EXTRACT CLAIMS =====================

    public String getEmailFromAccessToken(String encryptedToken) {
        try {
            String plainToken = encryptionService.decryptToken(encryptedToken);

            String email = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(plainToken)
                    .getPayload()
                    .getSubject();

            log.debug("✅ Email extracted: {}", email);
            return email;

        } catch (ExpiredJwtException ex) {
            log.error("❌ Token expired while extracting email");
            throw new RuntimeException("Token expired", ex);
        } catch (JwtException ex) {
            log.error("❌ Invalid token while extracting email: {}", ex.getMessage());
            throw new RuntimeException("Invalid token", ex);
        } catch (Exception ex) {
            log.error("❌ Failed to decrypt token for email extraction: {}", ex.getMessage(), ex);
            throw new RuntimeException("Invalid or corrupted token", ex);
        }
    }

    public Long getUserIdFromAccessToken(String encryptedToken) {
        try {
            String plainToken = encryptionService.decryptToken(encryptedToken);

            Long userId = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(plainToken)
                    .getPayload()
                    .get("userId", Long.class);

            log.debug("✅ UserId extracted: {}", userId);
            return userId;

        } catch (ExpiredJwtException ex) {
            log.error("❌ Token expired while extracting userId");
            throw new RuntimeException("Token expired", ex);
        } catch (JwtException ex) {
            log.error("❌ Invalid token while extracting userId: {}", ex.getMessage());
            throw new RuntimeException("Invalid token", ex);
        } catch (Exception ex) {
            log.error("❌ Failed to decrypt token for userId extraction: {}", ex.getMessage(), ex);
            throw new RuntimeException("Invalid or corrupted token", ex);
        }
    }

    public String getRoleFromAccessToken(String encryptedToken) {
        try {
            String plainToken = encryptionService.decryptToken(encryptedToken);

            String role = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(plainToken)
                    .getPayload()
                    .get("role", String.class);

            log.debug("✅ Role extracted: {}", role);
            return role;

        } catch (ExpiredJwtException ex) {
            log.error("❌ Token expired while extracting role");
            throw new RuntimeException("Token expired", ex);
        } catch (JwtException ex) {
            log.error("❌ Invalid token while extracting role: {}", ex.getMessage());
            throw new RuntimeException("Invalid token", ex);
        } catch (Exception ex) {
            log.error("❌ Failed to decrypt token for role extraction: {}", ex.getMessage(), ex);
            throw new RuntimeException("Invalid or corrupted token", ex);
        }
    }

    // ===================== TOKEN REFRESH =====================

    public String refreshAccessToken(String encryptedRefreshToken) {
        try {
            log.info("🔄 Refreshing access token");

            validateRefreshToken(encryptedRefreshToken);

            RefreshToken refreshToken = refreshTokenRepo.findByToken(encryptedRefreshToken)
                    .orElseThrow(() -> new RuntimeException("Refresh token not found"));

            User user = refreshToken.getUser();
            String newAccessToken = generateAccessToken(user);

            log.info("🔄 Access token refreshed for user: {}", user.getEmail());
            return newAccessToken;

        } catch (Exception ex) {
            log.error("❌ Failed to refresh access token: {}", ex.getMessage());
            throw new RuntimeException("Failed to refresh token", ex);
        }
    }

    // ===================== TOKEN REVOCATION =====================

    public void revokeRefreshToken(String encryptedRefreshToken) {
        try {
            refreshTokenRepo.findByToken(encryptedRefreshToken)
                    .ifPresent(token -> {
                        refreshTokenRepo.delete(token);
                        log.info("🗑️ Refresh token revoked for user: {}", token.getUser().getEmail());
                    });
        } catch (Exception ex) {
            log.error("❌ Failed to revoke refresh token: {}", ex.getMessage());
            throw new RuntimeException("Failed to revoke token", ex);
        }
    }

    public void revokeAllUserRefreshTokens(User user) {
        try {
            log.info("🗑️ Revoking all refresh tokens for user: {}", user.getEmail());
            refreshTokenRepo.findByUser(user).ifPresent(token -> {
                refreshTokenRepo.delete(token);
                log.info("🗑️ All refresh tokens revoked for user: {}", user.getEmail());
            });
        } catch (Exception ex) {
            log.error("❌ Failed to revoke user refresh tokens: {}", ex.getMessage());
            throw new RuntimeException("Failed to revoke tokens", ex);
        }
    }

    // ===================== HELPER METHODS =====================

    public boolean isTokenExpired(String encryptedToken) {
        try {
            String plainToken = encryptionService.decryptToken(encryptedToken);

            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(plainToken)
                    .getPayload();

            return claims.getExpiration().before(new Date());

        } catch (ExpiredJwtException ex) {
            return true;
        } catch (Exception ex) {
            log.error("❌ Failed to check token expiration: {}", ex.getMessage());
            return true;
        }
    }

    public long getTokenExpirationTime(String encryptedToken) {
        try {
            String plainToken = encryptionService.decryptToken(encryptedToken);

            Date expiration = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(plainToken)
                    .getPayload()
                    .getExpiration();

            return Math.max(0, expiration.getTime() - System.currentTimeMillis());

        } catch (Exception ex) {
            log.error("❌ Failed to get token expiration time: {}", ex.getMessage());
            return 0;
        }
    }
}