package com.auction.common.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Auction_watchDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private long id;
    private long userId;
    private long auctionId;
    private LocalDateTime watchedAt;

    public Auction_watchDTO() {}

    // Constructor dùng khi User nhấn nút "Theo dõi"
    public Auction_watchDTO(long userId, long auctionId) {
        this.userId = userId;
        this.auctionId = auctionId;
        this.watchedAt = LocalDateTime.now();
    }
    // Constructor đầy đủ cho DAO
    public Auction_watchDTO(long id, long userId, long auctionId, LocalDateTime watchedAt) {
        this.id = id;
        this.userId = userId;
        this.auctionId = auctionId;
        this.watchedAt = watchedAt;
    }
    // ---  Getters and Setters ---
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }

    public long getAuctionId() { return auctionId; }
    public void setAuctionId(long auctionId) { this.auctionId = auctionId; }

    public LocalDateTime getWatchedAt() { return watchedAt; }
    public void setWatchedAt(LocalDateTime watchedAt) { this.watchedAt = watchedAt; }

    @Override
    public String toString() {
        return "Auction_watchDTO{" +
                "id=" + id +
                ", userId=" + userId +
                ", auctionId=" + auctionId +
                ", watchedAt=" + watchedAt +
                '}';
    }
}