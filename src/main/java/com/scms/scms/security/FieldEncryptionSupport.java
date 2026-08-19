package com.scms.scms.security;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class FieldEncryptionSupport {

    public static final String PREFIX = "ENC::";
    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static volatile SecretKeySpec secretKeySpec;

    private FieldEncryptionSupport() {
    }

    public static void configure(String secret) {
        try {
            if (secret == null || secret.isBlank()) {
                throw new IllegalStateException("Field encryption secret must not be blank.");
            }
            byte[] key = MessageDigest.getInstance("SHA-256")
                    .digest(secret.getBytes(StandardCharsets.UTF_8));
            secretKeySpec = new SecretKeySpec(key, "AES");
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to configure field encryption.", ex);
        }
    }

    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return plainText;
        }
        if (isEncrypted(plainText)) {
            return plainText;
        }

        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            byte[] iv = new byte[IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            cipher.init(Cipher.ENCRYPT_MODE, getSecretKeySpec(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + cipherBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherBytes, 0, combined, iv.length, cipherBytes.length);

            return PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt sensitive field.", ex);
        }
    }

    public static String decrypt(String dbValue) {
        if (dbValue == null || dbValue.isBlank()) {
            return dbValue;
        }
        if (!isEncrypted(dbValue)) {
            return dbValue;
        }

        try {
            byte[] combined = Base64.getDecoder().decode(dbValue.substring(PREFIX.length()));
            byte[] iv = new byte[IV_LENGTH];
            byte[] cipherBytes = new byte[combined.length - IV_LENGTH];

            System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
            System.arraycopy(combined, IV_LENGTH, cipherBytes, 0, cipherBytes.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getSecretKeySpec(), new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

            return new String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decrypt sensitive field.", ex);
        }
    }

    public static boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    private static SecretKeySpec getSecretKeySpec() {
        if (secretKeySpec == null) {
            throw new IllegalStateException("Field encryption secret is not configured.");
        }
        return secretKeySpec;
    }
}
