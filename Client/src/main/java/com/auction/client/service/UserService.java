package com.auction.client.service;

import com.auction.client.controller.SessionManager;

import java.io.IOException;

/**
 * Service quản lý thông tin người dùng:
 * - Xem / cập nhật profile (màn Profile)
 * - Admin: lấy danh sách / cập nhật user (màn AdminUsers)
 */
public class UserService {

    private final ServerConnection connection = ServerConnection.getInstance();

    // ── GET PROFILE ──────────────────────────────────────────────────────────
    /**
     * Lấy thông tin profile của user đang đăng nhập.
     * @return Response chứa UserDTO
     */
    public Response getProfile() {
        return send(new Request(Request.GET_PROFILE));
    }

    // ── UPDATE PROFILE ───────────────────────────────────────────────────────
    /**
     * Cập nhật thông tin profile.
     * @param userDTO UserDTO chứa thông tin mới (không cần password nếu không đổi)
     */
    public Response updateProfile(Object userDTO) {
        return send(new Request(Request.UPDATE_PROFILE, userDTO));
    }

    // ── ADMIN: GET ALL USERS ─────────────────────────────────────────────────
    /**
     * [Admin only] Lấy danh sách tất cả user (màn AdminUsers).
     * @return Response chứa List<UserDTO>
     */
    public Response adminGetAllUsers() {
        return send(new Request(Request.ADMIN_GET_USERS));
    }

    // ── ADMIN: UPDATE USER ───────────────────────────────────────────────────
    /**
     * [Admin only] Cập nhật thông tin / trạng thái user (khoá, mở khoá, đổi role).
     * @param userDTO UserDTO đã chỉnh sửa (phải có ID)
     */
    public Response adminUpdateUser(Object userDTO) {
        return send(new Request(Request.ADMIN_UPDATE_USER, userDTO));
    }

    // ── Helper ───────────────────────────────────────────────────────────────
    private Response send(Request request) {
        try {
            attachToken(request);
            return (Response) connection.sendRequest(request);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return Response.error("Lỗi kết nối server: " + e.getMessage());
        }
    }

    private void attachToken(Request request) {
        String token = SessionManager.get().getToken();
        if (token != null) request.setToken(token);
    }
}