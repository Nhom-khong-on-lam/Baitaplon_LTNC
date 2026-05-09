package server.common.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Auction_watchDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private long id;
    private long userId;
    private long auctionId;
    private LocalDateTime createdAt;

    // Các trường bổ sung để hiển thị lên giao diện mà không cần query nhiều lần
    private String auctionTitle; // Tên phiên đấu giá
    private String status; // Trạng thái phiên hiện tại

    public Auction_watchDTO() {}

    // Constructor dùng khi User nhấn nút "Theo dõi"
    public Auction_watchDTO(long userId, long auctionId) {
        this.userId = userId;
        this.auctionId = auctionId;
        this.createdAt = LocalDateTime.now();
    }
    // Constructor đầy đủ cho DAO
    public Auction_watchDTO(long id, long userId, long auctionId, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.auctionId = auctionId;
        this.createdAt = createdAt;
    }
    // ---  Getters and Setters ---
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public long getAuctionId() { return auctionId; }
    public void setAuctionId(long auctionId) { this.auctionId = auctionId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getAuctionTitle() { return auctionTitle; }
    public void setAuctionTitle(String auctionTitle) { this.auctionTitle = auctionTitle; }

    @Override
    public String toString() {
        return "AuctionWatchDTO{" + "userId=" + userId + ", auctionId=" + auctionId + '}';
    }
}
