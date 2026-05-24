package com.auction.common.dto;

import com.auction.common.enums.PaymentStatus;

import java.time.LocalDateTime;

public class AuctionDTO {
    private Long id;
    private Long itemId;
    private Long sellerId;
    private Long highestBidderId;
    private Double currentPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private PaymentStatus paymentStatus;
    private String itemName;
    private String category;
    private String sellerUsername;

    public AuctionDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }

    public Long getHighestBidderId() { return highestBidderId; }
    public void setHighestBidderId(Long highestBidderId) { this.highestBidderId = highestBidderId; }

    public Double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(Double currentPrice) { this.currentPrice = currentPrice; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(com.auction.common.enums.PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getSellerUsername() { return sellerUsername; }
    public void setSellerUsername(String sellerUsername) { this.sellerUsername = sellerUsername; }
}