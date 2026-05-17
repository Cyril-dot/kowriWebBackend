package com.kowriWeb.KworiWebSite.Config.Security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Service
public class TokenEncryptionService {

    private static final String ALGORITHM      = "AES/GCM/NoPadding";
    private static final int    GCM_IV_LENGTH  = 12;  // 96 bits
    private static final int    GCM_TAG_LENGTH = 128; // 128 bits

    private final SecretKey encryptionKey;

    public TokenEncryptionService(
            @Value("${security.token.encryption-key}") String encryptionKeyBase64) {
        // ✅ Key is Base64-encoded — decode to get exactly 32 bytes for AES-256
        byte[] keyBytes = Base64.getDecoder().decode(encryptionKeyBase64);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "Encryption key must be 32 bytes (256 bits). Got: " + keyBytes.length + " bytes.");
        }
        this.encryptionKey = new SecretKeySpec(keyBytes, "AES");
        log.info("✅ Token encryption service initialized (AES-256-GCM)");
    }

    public String encryptToken(String plainToken) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] encryptedBytes = cipher.doFinal(plainToken.getBytes("UTF-8"));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encryptedBytes.length);
            buffer.put(iv);
            buffer.put(encryptedBytes);

            return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer.array());

        } catch (Exception e) {
            log.error("❌ Token encryption failed", e);
            throw new RuntimeException("Failed to encrypt token", e);
        }
    }

    public String decryptToken(String encryptedToken) {
        try {
            byte[] encryptedBytes = Base64.getUrlDecoder().decode(encryptedToken);

            ByteBuffer buffer = ByteBuffer.wrap(encryptedBytes);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            return new String(cipher.doFinal(cipherText), "UTF-8");

        } catch (Exception e) {
            log.error("❌ Token decryption failed", e);
            throw new RuntimeException("Invalid or corrupted token", e);
        }
    }

    /** Run once to generate a new Base64-encoded AES-256 key */
    public static void main(String[] args) throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        String key = Base64.getEncoder().encodeToString(keyGen.generateKey().getEncoded());
        System.out.println("Generated AES-256 key (Base64): " + key);
    }
}