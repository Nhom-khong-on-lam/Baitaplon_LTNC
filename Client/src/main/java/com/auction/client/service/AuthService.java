package com.auction.client.service;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class AuthService {
    // Giả lập Database đơn giản
    private static final Set<String> existingUsers = new HashSet<>();
    private static final Set<String> existingEmails = new HashSet<>();

    static {
        existingUsers.add("admin");
        existingEmails.add("test@gmail.com");
    }

    // Regex kiểm tra định dạng email
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@(.+)$";

    public boolean isValidEmail(String email) {
        return Pattern.compile(EMAIL_PATTERN).matcher(email).matches();
    }

    public boolean isUsernameExists(String username) {
        return existingUsers.contains(username);
    }

    public boolean isEmailExists(String email) {
        return existingEmails.contains(email);
    }

    public void saveUser(String username, String email) {
        existingUsers.add(username);
        existingEmails.add(email);
    }
}