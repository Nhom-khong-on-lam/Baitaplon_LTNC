package com.auction.client.service;


import com.auction.client.controller.SessionManager;
import com.auction.common.model.User;
import com.auction.common.network.Request;
import com.auction.common.network.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AuthService {

    private final ServerConnection connection = ServerConnection.getInstance();

    // ── LOGIN ────────────────────────────────────────────────────────────────
    public Response login(String username, String password) {
        try {
            String[] credentials = {username, password};
            Request request     = new Request(Request.LOGIN, credentials);
            Response response    = (Response) connection.sendRequest(request);

            if (response.isSuccess()) {
                SessionManager session = SessionManager.get();
                session.login((User) response.getData());
                session.setToken(response.getToken());
            }
            return response;

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return Response.error("Unable to connect to server: " + e.getMessage());
        }
    }

    // ── REGISTER ─────────────────────────────────────────────────────────────
    public Response register(User user) {
        try {
            Request request = new Request(Request.REGISTER, user);
            return (Response) connection.sendRequest(request);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return Response.error("Unable to connect to server: " + e.getMessage());
        }
    }

    // ── LOGOUT ───────────────────────────────────────────────────────────────
    public Response logout() {
        try {
            Request  request  = new Request(Request.LOGOUT);
            attachToken(request);
            Response response = (Response) connection.sendRequest(request);
            SessionManager.get().logout();
            return response;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            SessionManager.get().logout();
            return Response.error("Logged out locally (server error): " + e.getMessage());
        }
    }

    // ── AUTHENTICATE ─────────────────────────────────────────────────────────
    public User authenticate(String username, String password) {
        String[] credentials = {username, password};
        Request  req         = new Request(Request.LOGIN, credentials);
        try {
            Response res = (Response) connection.sendRequest(req);
            if (res != null && res.isSuccess() && res.getData() instanceof User) {
                User user = (User) res.getData();
                SessionManager.get().setToken(res.getToken());
                return user;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // ── ADMIN ────────────────────────────────────────────────────────────────
    public boolean updateUserStatus(Long userId, Object status) {
        try {
            Object[] data    = {userId, status};
            Request  request = new Request(Request.ADMIN_UPDATE_USER_STATUS, data);
            attachToken(request);

            // 🚀 Nhận gói tin phản hồi chuẩn từ Server và ép kiểu về Response
            Response res = (Response) connection.sendRequest(request);
            return res != null && res.isSuccess();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<User> getAllUsers() {
        try {
            Request  request = new Request(Request.ADMIN_GET_USERS);
            attachToken(request);
            Response res = (Response) connection.sendRequest(request);
            if (res != null && res.isSuccess() && res.getData() instanceof List) {
                return (List<User>) res.getData();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }


    // ── VALIDATION ───────────────────────────────────────────────────────────
    public boolean isValidEmail(String email) {
        if (email == null) return false;
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$");
    }

    public boolean isEmailExists(String email) {
        try {
            Request  req = new Request("CHECK_EMAIL_EXISTS", email);
            Response res = (Response) connection.sendRequest(req);
            return res != null && res.isSuccess();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isUsernameExists(String username) {
        try {
            Request  req = new Request(Request.CHECK_USER_EXISTS, username);
            Response res = (Response) connection.sendRequest(req);
            return res != null && res.isSuccess();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean checkPassword(User user, String oldPass) {
        try {
            Object[] data = {user.getId(), oldPass};
            Request  req  = new Request("CHECK_PASSWORD", data);
            attachToken(req);
            Response res = (Response) connection.sendRequest(req);
            return res != null && res.isSuccess();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void updatePassword(String email, String newPass) {
        try {
            Object[] data = {email, newPass};
            Request  req  = new Request("UPDATE_PASSWORD", data);
            attachToken(req);
            connection.sendRequest(req);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean saveUser(String username, String email, String password) {
        try {
            User newUser = new User("");
            newUser.setUsername(username);
            newUser.setEmail(email);

            // Đảm bảo truyền chuỗi thô sang, không đi qua bộ lọc băm nào ở Client cả
            newUser.setPasswordHash(password);

            Request  req = new Request(Request.REGISTER, newUser);
            Response res = (Response) connection.sendRequest(req);

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

    // ── Helper ───────────────────────────────────────────────────────────────
    private void attachToken(Request request) {
        String token = SessionManager.get().getToken();
        if (token != null) request.setToken(token);
    }
}