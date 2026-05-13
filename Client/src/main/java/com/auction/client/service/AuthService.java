package com.auction.client.service;

import com.auction.client.controller.SessionManager;
import com.auction.client.model.User;
import java.io.IOException;
import java.util.List;

import static jakarta.mail.Transport.send;

/**
 * Service xử lý Authentication: Login, Register, Logout.
 * Tự động lưu User + token vào SessionManager sau khi login thành công.
 */
public class AuthService {

    private final ServerConnection connection = ServerConnection.getInstance();

    // ── LOGIN ────────────────────────────────────────────────────────────────
    /**
     * Gửi username + password lên server để đăng nhập.
     * @return Response chứa User nếu thành công
     */
    public Response login(String username, String password) {
        try {
            String[] credentials = {username, password};
            Request  request     = new Request(Request.LOGIN, credentials);

            Response response = (Response) connection.sendRequest(request);

            if (response.isSuccess()) {
                SessionManager session = SessionManager.get();
                session.login((User) response.getData()); // lưu User vào session
                session.setToken(response.getToken());    // lưu token
            }

            return response;

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return Response.error("Không thể kết nối tới server: " + e.getMessage());
        }
    }

    // ── REGISTER ─────────────────────────────────────────────────────────────
    /**
     * Đăng ký tài khoản mới.
     * @param user object User chứa thông tin đăng ký
     */
    public Response register(User user) {
        try {
            Request request = new Request(Request.REGISTER, user);
            return (Response) connection.sendRequest(request);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return Response.error("Không thể kết nối tới server: " + e.getMessage());
        }
    }

    // ── LOGOUT ───────────────────────────────────────────────────────────────
    /**
     * Đăng xuất: xoá session cục bộ và thông báo server.
     */
    public Response logout() {
        try {
            Request request = new Request(Request.LOGOUT);
            attachToken(request);
            Response response = (Response) connection.sendRequest(request);
            SessionManager.get().logout();
            return response;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            SessionManager.get().logout(); // vẫn xoá local dù lỗi mạng
            return Response.error("Đã đăng xuất cục bộ (lỗi server): " + e.getMessage());
        }
    }

    public boolean updateUserStatus(Long userId, Object status) {
        try {
            // Gom dữ liệu vào mảng để gửi đi
            Object[] data = { userId, status };
            Request request = new Request(Request.ADMIN_UPDATE_USER_STATUS, data);
            attachToken(request); // Đính kèm token Admin để xác thực quyền

            Response res = (Response) connection.sendRequest(request);
            return res != null && res.isSuccess();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public User authenticate(String username, String password) {
        Object[] credentials = {username, password};
        Request req = new Request(Request.LOGIN, credentials);
        try {
            Response res = (Response) connection.sendRequest(req);
            if (res != null && res.isSuccess() && res.getData() instanceof User) {
                User user = (User) res.getData();
                SessionManager.get().setToken(res.getToken()); // Lưu token
                return user;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; // Trả về null để khớp check (currentUser == null)
    }

    // ── Helper ───────────────────────────────────────────────────────────────
    private void attachToken(Request request) {
        String token = SessionManager.get().getToken();
        if (token != null) request.setToken(token);
    }
    public List<User> getAllUsers() {
        try {
            // Gửi request với action ADMIN_GET_USERS
            Request request = new Request(Request.ADMIN_GET_USERS);
            attachToken(request); // Quan trọng: Đính kèm token Admin

            Response res = (Response) connection.sendRequest(request);

            if (res != null && res.isSuccess() && res.getData() instanceof List) {
                // Ép kiểu an toàn dữ liệu từ server sang List<User>
                return (List<User>) res.getData();
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy danh sách người dùng: " + e.getMessage());
            e.printStackTrace();
        }
        // Trả về danh sách rỗng nếu có lỗi để tránh lỗi NullPointerException
        return new java.util.ArrayList<>();
    }
    public boolean deleteUser(Long userId) {
        try {
            Request request = new Request(Request.ADMIN_DELETE_USER, userId);
            attachToken(request); // Gửi kèm token Admin để xác thực quyền xóa

            Response res = (Response) connection.sendRequest(request);
            return res != null && res.isSuccess();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public boolean isValidEmail(String email) {
        if (email == null) return false;
        return email.matches("^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$");
    }

    public boolean isEmailExists(String email) {
        try {
            Request req = new Request("CHECK_EMAIL_EXISTS", email);
            Response res = (Response) connection.sendRequest(req);
            return res != null && res.isSuccess();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean checkPassword(User user, String oldPass) {
        try {
            Object[] data = { user.getId(), oldPass };
            Request req = new Request("CHECK_PASSWORD", data);
            attachToken(req); // Đính kèm token để xác thực

            Response res = (Response) connection.sendRequest(req);
            return res != null && res.isSuccess();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public void updatePassword(String email, String newPass) {
        try {
            Object[] data = { email, newPass };
            Request req = new Request("UPDATE_PASSWORD", data);
            attachToken(req);

            connection.sendRequest(req); // Gửi đi, không nhất thiết phải đợi trả về nếu chỉ update
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean saveUser(String username, String email, String password) {
        try {
            // 1. Tạo đối tượng User thay vì gửi mảng Object[]
            com.auction.client.model.User newUser = new com.auction.client.model.User("");
            newUser.setUsername(username);
            newUser.setEmail(email);
            newUser.setPasswordHash(password); // Hoặc setPassword tùy theo model của bạn

            // 2. Gửi đối tượng User đi với đúng lệnh REGISTER
            Request req = new Request(Request.REGISTER, newUser);

            // 3. Nhận phản hồi thực sự từ server
            Response res = (Response) connection.sendRequest(req);

            // Trả về kết quả thành công/thất bại từ Server
            return res != null && res.isSuccess();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public void updateUser(User user) {
        try {
            Request req = new Request("UPDATE_USER", user);
            attachToken(req);

            connection.sendRequest(req);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public boolean isUsernameExists(String username) {
        try {
            // Tạo Request với action đã định nghĩa trong Request.java
            Request req = new Request(Request.CHECK_USER_EXISTS, username);

            // Đính kèm token (nếu hàm này cần xác thực, thường thì check tồn tại ko cần)
            attachToken(req);

            // Gửi request và nhận phản hồi từ Server
            // Ép kiểu (Response) vì sendRequest trả về Object
            Response res = (Response) connection.sendRequest(req);

            // Trả về true nếu success (username đã tồn tại), false nếu ngược lại
            return res != null && res.isSuccess();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}