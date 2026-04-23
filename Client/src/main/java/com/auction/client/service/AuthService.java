package com.auction.client.service;

import com.auction.client.Enum.SystemRole;
import com.auction.client.model.User;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class AuthService {

    private static final Set<String> existingUsers = new HashSet<>();
    private static final Set<String> existingEmails = new HashSet<>();

    static {
        existingUsers.add("admin");
        existingUsers.add("user1");
        existingEmails.add("test@gmail.com");
        existingEmails.add("user1@example.com");
    }

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

    // Phương thức authenticate giả lập
    public User authenticate(String usernameOrEmail, String password) {
        // Giả lập: admin/admin -> ADMIN, user1/password -> USER
        if (("admin".equals(usernameOrEmail) || "admin@example.com".equals(usernameOrEmail)) && "admin".equals(password)) {
            return new User(1L, "admin", hashPassword("admin"), "admin@example.com", SystemRole.ADMIN);
        } else if (("user1".equals(usernameOrEmail) || "user1@example.com".equals(usernameOrEmail)) && "password".equals(password)) {
            return new User(2L, "user1", hashPassword("password"), "user1@example.com", SystemRole.USER);
        }
        return null;
    }

    // Giả lập hash (trong thực tế dùng BCrypt...)
    private String hashPassword(String plain) {
        return plain;
    }
}