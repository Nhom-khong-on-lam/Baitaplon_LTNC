package com.auction.client.service;

import com.auction.client.controller.SessionManager;

import java.io.IOException;

/**
 * Service for managing user information:
 * - View / update profile (Profile screen)
 * - Admin: list / update users (AdminUsers screen)
 */
public class UserService {

    private final ServerConnection connection = ServerConnection.getInstance();

    // ── GET PROFILE ──────────────────────────────────────────────────────────
    public Response getProfile() {
        return send(new Request(Request.GET_PROFILE));
    }

    // ── UPDATE PROFILE ───────────────────────────────────────────────────────
    public Response updateProfile(Object userDTO) {
        return send(new Request(Request.UPDATE_PROFILE, userDTO));
    }

    // ── ADMIN: GET ALL USERS ─────────────────────────────────────────────────
    public Response adminGetAllUsers() {
        return send(new Request(Request.ADMIN_GET_USERS));
    }

    // ── ADMIN: UPDATE USER ───────────────────────────────────────────────────
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
            return Response.error("Server connection error: " + e.getMessage());
        }
    }

    private void attachToken(Request request) {
        String token = SessionManager.get().getToken();
        if (token != null) request.setToken(token);
    }
}