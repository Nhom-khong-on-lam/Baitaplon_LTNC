package com.auction.client.service;

import com.auction.client.controller.SessionManager;

import java.io.IOException;

/**
 * Service quản lý Item / Product của người dùng (màn MyProducts).
 * Hỗ trợ Art, Electronics, Vehicle thông qua ItemDTO chung.
 */
public class ItemService {

    private final ServerConnection connection = ServerConnection.getInstance();

    // ── GET MY PRODUCTS ──────────────────────────────────────────────────────
    /**
     * Lấy danh sách sản phẩm của user đang đăng nhập (màn MyProducts).
     * @return Response chứa List<ItemDTO>
     */
    public Response getMyProducts() {
        return send(new Request(Request.GET_MY_PRODUCTS));
    }

    // ── CREATE ITEM ──────────────────────────────────────────────────────────
    /**
     * Tạo sản phẩm mới.
     * Server sẽ dùng ItemFactory để tạo đúng loại (Art/Electronics/Vehicle).
     * @param itemDTO object ItemDTO (có field "type" để server phân loại)
     */
    public Response createItem(Object itemDTO) {
        return send(new Request(Request.CREATE_ITEM, itemDTO));
    }

    // ── UPDATE ITEM ──────────────────────────────────────────────────────────
    /**
     * Cập nhật thông tin sản phẩm.
     * @param itemDTO ItemDTO đã chỉnh sửa (phải có ID)
     */
    public Response updateItem(Object itemDTO) {
        return send(new Request(Request.UPDATE_ITEM, itemDTO));
    }

    // ── DELETE ITEM ──────────────────────────────────────────────────────────
    /**
     * Xoá sản phẩm theo ID.
     * @param itemId ID của item cần xoá
     */
    public Response deleteItem(int itemId) {
        return send(new Request(Request.DELETE_ITEM, itemId));
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