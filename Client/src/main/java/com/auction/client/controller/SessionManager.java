package com.auction.client.controller;


import com.auction.common.model.User;

/**
 * SessionManager — lưu trạng thái đăng nhập toàn cục.
 */
public class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();
    public  static SessionManager get() { return INSTANCE; }
    private SessionManager() {}

    private User currentUser;
    private String token;
    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
    public void   login(User user)  { this.currentUser = user; }
    public void   logout()          { this.currentUser = null; }
    public User   getUser()         { return currentUser; }
    public boolean isLoggedIn()     { return currentUser != null; }
    public boolean isAdmin()        {
        return currentUser != null && currentUser.isAdmin();
    }
}