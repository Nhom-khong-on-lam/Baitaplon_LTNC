package com.auction.client.model;

import java.time.LocalDateTime;

public class AutoBidConfig {
    private User bidder;
    private double maxPrice;
    private double stepIncrement;
    private boolean active;
    private LocalDateTime registeredAt;

    public AutoBidConfig(User bidder, double maxPrice, double stepIncrement) {
        this.bidder = bidder;
        this.maxPrice = maxPrice;
        this.stepIncrement = stepIncrement;
        this.active = true;
        this.registeredAt = LocalDateTime.now();
    }
    public User getBidder() {
        return bidder;
    }
    public double getMaxPrice() {
        return maxPrice;
    }
    public double getStepIncrement() {
        return stepIncrement;
    }
    public boolean isActive() {
        return active;
    }
    public void deactivate() {
        this.active = false;
    }
    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }
    public double getNextBid(double currentPrice) {
        if (!active) return -1;
        double next = currentPrice + stepIncrement;
        if (next <= maxPrice) {
            return next;
        }
        return -1;
    }
    @Override
    public String toString() {
        return "AutoBidConfig{" +
                "bidder=" + bidder.getUsername() +
                ", maxPrice=" + maxPrice +
                ", step=" + stepIncrement +
                ", active=" + active +
                '}';
    }

    public double getIncrement() {
        return stepIncrement;
    }

}