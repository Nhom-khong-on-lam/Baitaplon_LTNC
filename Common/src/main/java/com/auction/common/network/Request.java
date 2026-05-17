package com.auction.common.network;

import java.io.Serializable;

/**
 * Object được serialize gửi từ Client lên Server.
 *
 * Cấu trúc:
 *   - action : tên lệnh,  ví dụ "LOGIN", "GET_AUCTIONS", "PLACE_BID"
 *   - data   : payload đi kèm (DTO, String, null, ...)
 *   - token  : session token sau khi đăng nhập (gửi kèm mọi request cần auth)
 */
public class Request implements Serializable {
    private static final long serialVersionUID = 1L;

    // ── Danh sách action constants ──────────────────────────────────────────
    // Auth
    public static final String LOGIN           = "LOGIN";
    public static final String REGISTER        = "REGISTER";
    public static final String LOGOUT          = "LOGOUT";
    public static final String EXTEND_AUCTION = "EXTEND_AUCTION";
    public static final String END_AUCTION_EARLY = "END_AUCTION_EARLY";
    public static final String GET_AUCTIONS    = "GET_AUCTIONS";
    public static final String GET_AUCTION     = "GET_AUCTION";
    public static final String CREATE_AUCTION  = "CREATE_AUCTION";
    public static final String UPDATE_AUCTION  = "UPDATE_AUCTION";
    public static final String DELETE_AUCTION  = "DELETE_AUCTION";
    public static final String ADMIN_UPDATE_USER_STATUS = "ADMIN_UPDATE_USER_STATUS";
    public static final String ADMIN_DELETE_USER = "ADMIN_DELETE_USER";
    public static final String GET_BID_HISTORY = "GET_BID_HISTORY";
    public static final String CHECK_USER_EXISTS = "CHECK_USER_EXISTS";
    public static final String CHECK_EMAIL_EXISTS = "CHECK_EMAIL_EXISTS";
    public static final String CHECK_PASSWORD = "CHECK_PASSWORD";

    // Bid
    public static final String PLACE_BID       = "PLACE_BID";
    public static final String GET_MY_BIDS     = "GET_MY_BIDS";
    public static final String GET_AUTO_BID    = "GET_AUTO_BID";
    public static final String SET_AUTO_BID    = "SET_AUTO_BID";
    public static final String GET_ACTIVE_AUCTIONS = "GET_ACTIVE_AUCTIONS";
    public static final String GET_AUCTIONS_BY_SELLER = "GET_AUCTIONS_BY_SELLER";
    public static final String GET_WINNING_BIDS = "GET_WINNING_BIDS";

    // Item / Product
    public static final String GET_MY_PRODUCTS = "GET_MY_PRODUCTS";
    public static final String CREATE_ITEM     = "CREATE_ITEM";
    public static final String UPDATE_ITEM     = "UPDATE_ITEM";
    public static final String DELETE_ITEM     = "DELETE_ITEM";

    // User / Profile
    public static final String GET_PROFILE     = "GET_PROFILE";
    public static final String UPDATE_PROFILE  = "UPDATE_PROFILE";
    public static final String UPDATE_PASSWORD = "UPDATE_PASSWORD";
    public static final String UPDATE_USER = "UPDATE_USER";
    // Admin
    public static final String ADMIN_GET_USERS    = "ADMIN_GET_USERS";
    public static final String ADMIN_UPDATE_USER  = "ADMIN_UPDATE_USER";
    public static final String ADMIN_GET_AUCTIONS = "ADMIN_GET_AUCTIONS";
    // ────────────────────────────────────────────────────────────────────────

    private final String action;
    private final Object data;
    private String token; // set sau khi login

    public Request(String action, Object data) {
        this.action = action;
        this.data   = data;
    }

    public Request(String action) {
        this(action, null);
    }

    // Getters / Setters
    public String getAction() { return action; }
    public Object getData()   { return data;   }
    public String getToken()  { return token;  }
    public void setToken(String token) { this.token = token; }

    @Override
    public String toString() {
        return "Request{action='" + action + "', data=" + data + "}";
    }
}