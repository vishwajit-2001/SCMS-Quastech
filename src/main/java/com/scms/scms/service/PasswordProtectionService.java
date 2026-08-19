package com.scms.scms.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
public class PasswordProtectionService {

    private static final Pattern BCRYPT_PATTERN =
            Pattern.compile("^\\$2[aby]?\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    @Autowired
    private PasswordEncoder passwordEncoder;

    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null || storedPassword.isBlank()) {
            return false;
        }

        if (isEncoded(storedPassword)) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }

        return storedPassword.equals(rawPassword);
    }

    public boolean needsUpgrade(String storedPassword) {
        return storedPassword != null && !storedPassword.isBlank() && !isEncoded(storedPassword);
    }

    public boolean isSamePassword(String rawPassword, String storedPassword) {
        return matches(rawPassword, storedPassword);
    }

    private boolean isEncoded(String storedPassword) {
        return BCRYPT_PATTERN.matcher(storedPassword).matches();
    }
}
