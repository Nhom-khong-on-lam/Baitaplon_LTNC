package com.auction.client.service;

import com.auction.client.model.User;
import server.repository.UserDAO;
import org.mindrot.jbcrypt.BCrypt;

public class UserService {
    private final UserDAO userDAO = new UserDAO();

    /**
     * Đăng ký người dùng mới với mật khẩu được mã hóa
     */
    public boolean register(String username, String password, String email) {
        // 1. Kiểm tra username đã tồn tại chưa
        if (userDAO.isExisted("username", username)) {
            System.out.println("LỖI: Username đã tồn tại!");
            return false;
        }

        // 2. Mã hóa mật khẩu bằng BCrypt
        // gensalt() tạo ra một chuỗi ngẫu nhiên để trộn vào mật khẩu
        String hashedParams = BCrypt.hashpw(password, BCrypt.gensalt());

        // 3. Tạo đối tượng User và lưu vào DB
        User newUser = new User(username, hashedParams, email);
        newUser.setSystemRole("USER");
        newUser.setAccountStatus("ACTIVE");

        return userDAO.insert(newUser) > 0;
    }

    /**
     * Xác thực người dùng khi đăng nhập
     */
    public User login(String username, String password) {
        // 1. Tìm user theo username
        User user = userDAO.findByUsername(username);

        // 2. Kiểm tra nếu user tồn tại và mật khẩu khớp
        if (user != null) {
            // BCrypt.checkpw tự động trích xuất Salt từ mật khẩu đã băm trong DB
            // để so sánh với mật khẩu nhập vào
            if (BCrypt.checkpw(password, user.getPassword())) {

                // Kiểm tra trạng thái tài khoản
                if ("BANNED".equalsIgnoreCase(user.getAccountStatus())) {
                    System.out.println("Tài khoản của bạn đã bị khóa!");
                    return null;
                }

                return user; // Đăng nhập thành công
            }
        }

        System.out.println("Sai tài khoản hoặc mật khẩu!");
        return null;
    }
}
