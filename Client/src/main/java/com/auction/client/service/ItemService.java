package com.auction.client.service;

import com.auction.client.controller.SessionManager;
import com.auction.common.network.Request;
import com.auction.common.network.Response;

import java.io.IOException;

/**
 * Service for managing user items / products (MyProducts screen).
 * Supports Art, Electronics, Vehicle via a shared ItemDTO.
 */
public class ItemService {

    private final ServerConnection connection = ServerConnection.getInstance();

    // ── GET MY PRODUCTS ──────────────────────────────────────────────────────
    public Response getMyProducts() {
        return send(new Request(Request.GET_MY_PRODUCTS));
    }

    // ── CREATE ITEM ──────────────────────────────────────────────────────────
    public Response createItem(Object itemDTO) {
        return send(new Request(Request.CREATE_ITEM, itemDTO));
    }

    // ── UPDATE ITEM ──────────────────────────────────────────────────────────
    public Response updateItem(Object itemDTO) {
        return send(new Request(Request.UPDATE_ITEM, itemDTO));
    }

    // ── DELETE ITEM ──────────────────────────────────────────────────────────
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
            return Response.error("Server connection error: " + e.getMessage());
        }
    }

    private void attachToken(Request request) {
        String token = SessionManager.get().getToken();
        if (token != null) request.setToken(token);
    }
}