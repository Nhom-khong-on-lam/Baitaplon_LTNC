package com.auction.client.service;

import server.common.model.UserDTO;
import server.repository.UserDAO;
import org.mindrot.jbcrypt.BCrypt;
import java.util.logging.Logger;

public class UserService {
    private final UserDAO userDAO = new UserDAO();
    private static final Logger LOGGER = Logger.getLogger(UserService.class.getName());
    /**
     * Đăng ký người dùng mới với mật khẩu được mã hóa
     */
    public boolean register(String username, String password, String email) {
        // 1. Kiểm tra username đã tồn tại chưa
        if (userDAO.isExisted("username", username)) {
            return false;
        }

        // 2. Mã hóa mật khẩu bằng BCrypt
        // gensalt() tạo ra một chuỗi ngẫu nhiên để trộn vào mật khẩu
        String hashedParams = BCrypt.hashpw(password, BCrypt.gensalt());

        // 3. Sử dụng UserDTO (Sửa lỗi Expected 5 arguments but found 3)
        // Lưu ý: Dùng constructor mặc định rồi set hoặc constructor 3 tham số đã tạo trong UserDTO
        UserDTO newUser = new UserDTO(username, hashedParams, email);
        newUser.setSystemRole("USER");
        newUser.setAccountStatus("ACTIVE");

        return userDAO.insert(newUser) > 0;
    }

    /**
     * Xác thực người dùng khi đăng nhập
     */
    public UserDTO login(String username, String password) {
        // 1. Tìm user theo username
        UserDTO user = userDAO.findByUsername(username);

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