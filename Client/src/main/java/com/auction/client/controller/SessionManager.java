package com.auction.client.controller;

import com.auction.common.model.User;
import java.util.HashSet;
import java.util.Set;

/**
 * SessionManager — lưu trạng thái đăng nhập toàn cục.
 * ĐÃ TÍCH HỢP BỘ NHỚ ĐỒNG BỘ TRẠNG THÁI THANH TOÁN TOÀN HỆ THỐNG GIỮA CÁC CONTROLLER.
 */
public class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();
    public  static SessionManager get() { return INSTANCE; }
    private SessionManager() {}

    private User currentUser;
    private String token;

    // 🚀 BỘ NHỚ TOÀN CỤC: Đánh dấu các ID phiên đấu giá đã được bấm thanh toán thành công trên Client
    private final Set<Long> globallyPaidAuctionIds = new HashSet<>();

    // Lưu lịch sử thông báo phiên làm việc hiện tại
    private final java.util.List<String> notifications = new java.util.ArrayList<>();

    /**
     * Ghi nhận ID phiên đấu giá đã thanh toán thành công
     */
    public void markAsPaid(Long auctionId) {
        if (auctionId != null) {
            globallyPaidAuctionIds.add(auctionId);
        }
    }

    /**
     * Kiểm tra xem ID phiên này đã được thanh toán trên RAM Client chưa
     */
    public boolean isAuctionPaidLocally(Long auctionId) {
        return auctionId != null && globallyPaidAuctionIds.contains(auctionId);
    }

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
        this.globallyPaidAuctionIds.clear(); // Xóa cờ thanh toán khi user đăng xuất
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