package com.auction.common.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class AutoBidDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long auctionId;
    private Long bidderId;
    private double maxPrice;
    private double stepIncrement;
    private boolean active;
    private LocalDateTime registeredAt;

    public AutoBidDTO() {
    }

    public AutoBidDTO(Long id, Long auctionId, Long bidderId, double maxPrice, double stepIncrement, boolean active, LocalDateTime registeredAt) {
        this.id = id;
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.maxPrice = maxPrice;
        this.stepIncrement = stepIncrement;
        this.active = active;
        this.registeredAt = registeredAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAuctionId() { return auctionId; }
    public void setAuctionId(Long auctionId) { this.auctionId = auctionId; }

    public Long getBidderId() { return bidderId; }
    public void setBidderId(Long bidderId) { this.bidderId = bidderId; }

    public double getMaxPrice() { return maxPrice; }
    public void setMaxPrice(double maxPrice) { this.maxPrice = maxPrice; }

    public double getStepIncrement() { return stepIncrement; }
    public void setStepIncrement(double stepIncrement) { this.stepIncrement = stepIncrement; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
}
