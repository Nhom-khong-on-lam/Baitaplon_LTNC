package com.auction.client.service;

import com.auction.client.Enum.AccountStatus;
import com.auction.client.Enum.SystemRole;
import com.auction.client.model.User;
import org.mindrot.jbcrypt.BCrypt;

import java.util.*;
import java.util.regex.Pattern;



public class AuthService {
    private static final Map<String, User> userDB = new HashMap<>();
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@(.+)$";
    private static long nextId;

    static {
        String adminHash = BCrypt.hashpw("admin", BCrypt.gensalt());
        String userHash = BCrypt.hashpw("password", BCrypt.gensalt());
        User admin = new User(1L, "admin", adminHash, "admin@example.com", SystemRole.ADMIN);
        User user1 = new User(2L, "user1", userHash, "user1@example.com", SystemRole.USER);
        User user2 = new User(3L, "user2", userHash, "user1@example.com", SystemRole.USER);
        admin.setAccountStatus(AccountStatus.ACTIVE);
        user1.setAccountStatus(AccountStatus.ACTIVE);
        user2.setAccountStatus(AccountStatus.ACTIVE);
        userDB.put(admin.getUsername(), admin);
        userDB.put(user1.getUsername(), user1);
        userDB.put(user2.getUsername(), user2);
        nextId = 4;
    }

    public boolean isValidEmail(String email) {
        return email != null && Pattern.compile(EMAIL_PATTERN).matcher(email).matches();
    }

    public boolean isUsernameExists(String username) {
        return username != null && userDB.containsKey(username);
    }

    public boolean isEmailExists(String email) {
        return email != null && userDB.values().stream().anyMatch(u -> email.equals(u.getEmail()));
    }

    public User authenticate(String usernameOrEmail, String plainPassword) {
        if (usernameOrEmail == null || plainPassword == null) return null;
        User user = userDB.values().stream()
                .filter(u -> usernameOrEmail.equals(u.getUsername()) || usernameOrEmail.equals(u.getEmail()))
                .findFirst()
                .orElse(null);
        if (user == null) return null;
        try {
            if (BCrypt.checkpw(plainPassword, user.getPasswordHash())) {
                return user;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void saveUser(String username, String email, String plainPassword) {
        if (username == null || email == null || plainPassword == null) return;
        if (isUsernameExists(username) || isEmailExists(email)) return;
        String hash = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
        User newUser = new User(nextId++, username, hash, email, SystemRole.USER);
        userDB.put(username, newUser);
    }

    public void updatePassword(String email, String newPlainPassword) {
        if (email == null || newPlainPassword == null) return;
        User user = userDB.values().stream()
                .filter(u -> email.equals(u.getEmail()))
                .findFirst()
                .orElse(null);
        if (user != null) {
            String newHash = BCrypt.hashpw(newPlainPassword, BCrypt.gensalt());
            user.setPasswordHash(newHash);
        }
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(userDB.values());
    }


    public void updateUserStatus(Long userId, AccountStatus newStatus) {
        User user = getUserById(userId);
        if (user != null) {
            user.setAccountStatus(newStatus);
        }
    }
    public User getUserById(Long id) {
        if (id == null) return null;
        return userDB.values().stream().filter(u -> id.equals(u.getId())).findFirst().orElse(null);
    }

    public User getUserByUsername(String username) {
        return username == null ? null : userDB.get(username);
    }
    public void banUser(Long userId) {
        // Logic thực tế ở đây (gọi API server hoặc cập nhật Database)
        System.out.println("User with ID " + userId + " has been banned in database.");

        // Nếu bạn quản lý danh sách user tập trung trong service,
        // hãy tìm user đó và cập nhật status của họ tại đây.
    }
    public void deleteUser(Long userId) {
        // 1. Thực tế: Bạn sẽ gọi Repository hoặc API để xóa trong DB
        // userRepository.deleteById(userId);

        // 2. Tạm thời để test logic:
        System.out.println("Đã xóa User có ID: " + userId + " khỏi hệ thống.");
    }
    public boolean checkPassword(User user, String plainPassword) {
        if (user == null || plainPassword == null || user.getPasswordHash() == null) {
            return false;
        }
        try {
            // Sử dụng BCrypt để so sánh mật khẩu nhập vào với mã hash trong DB
            return BCrypt.checkpw(plainPassword, user.getPasswordHash());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public void updateUser(User updatedUser) {
        if (updatedUser != null && updatedUser.getUsername() != null) {
            userDB.put(updatedUser.getUsername(), updatedUser);
        }
    }
}