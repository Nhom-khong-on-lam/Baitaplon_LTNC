package com.auction.common.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class PaymentDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long auctionId;
    private Long buyerId;
    private Long sellerId;
    private double amount;
    private String status;
    private LocalDateTime createdAt;

    // 🚀 Các trường bổ trợ hiển thị thông tin ngân hàng đích của Người Bán lên UI Dialog
    private String sellerBankName;
    private String sellerAccountNumber;
    private String sellerCardholderName;

    public PaymentDTO() {}

    // --- GETTERS & SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAuctionId() { return auctionId; }
    public void setAuctionId(Long auctionId) { this.auctionId = auctionId; }

    public Long getBuyerId() { return buyerId; }
    public void setBuyerId(Long buyerId) { this.buyerId = buyerId; }

    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getSellerBankName() { return sellerBankName; }
    public void setSellerBankName(String sellerBankName) { this.sellerBankName = sellerBankName; }

    public String getSellerAccountNumber() { return sellerAccountNumber; }
    public void setSellerAccountNumber(String sellerAccountNumber) { this.sellerAccountNumber = sellerAccountNumber; }

    public String getSellerCardholderName() { return sellerCardholderName; }
    public void setSellerCardholderName(String sellerCardholderName) { this.sellerCardholderName = sellerCardholderName; }
}