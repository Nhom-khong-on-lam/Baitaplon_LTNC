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

    // Lưu lịch sử thông báo phiên làm việc hiện tại
    private final java.util.List<String> notifications = new java.util.ArrayList<>();

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
    public void   login(User user)  { this.currentUser = user; }
    public void   logout()          {
        this.currentUser = null;
        this.notifications.clear();
    }
    public User   getUser()         { return currentUser; }
    public boolean isLoggedIn()     { return currentUser != null; }
    public boolean isAdmin()        {
        return currentUser != null && currentUser.isAdmin();
    }

    public void addNotification(String msg) {
        // Thêm vào đầu danh sách (mới nhất lên trên)
        notifications.add(0, java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")) + " - " + msg);
        // Giữ tối đa 50 thông báo
        if (notifications.size() > 50) {
            notifications.remove(notifications.size() - 1);
        }
    }

    public java.util.List<String> getNotifications() {
        return notifications;
    }
}