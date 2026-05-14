package com.auction.common.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class NotificationDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String title;
    private String message;
    private String type; // Lưu chuỗi ENUM: 'BID_PLACED', 'OUTBID', 'AUCTION_WON'...
    private boolean isRead;

    private Long relatedAuctionId;
    private Long relatedBidId;

    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    public NotificationDTO() {
    }

    public NotificationDTO(Long userId, String title, String message, String type, Long relatedAuctionId, Long relatedBidId) {
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.type = type;
        this.isRead = false; // Mặc định khi mới tạo là chưa đọc
        this.relatedAuctionId = relatedAuctionId;
        this.relatedBidId = relatedBidId;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public Long getRelatedAuctionId() { return relatedAuctionId; }
    public void setRelatedAuctionId(Long relatedAuctionId) { this.relatedAuctionId = relatedAuctionId; }

    public Long getRelatedBidId() { return relatedBidId; }
    public void setRelatedBidId(Long relatedBidId) { this.relatedBidId = relatedBidId; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}