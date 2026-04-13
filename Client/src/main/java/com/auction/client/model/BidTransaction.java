package com.auction.client.model;

import java.time.LocalDateTime;

public class BidTransaction {
    private User bidder;
    private double amount;
    private LocalDateTime bidTime;
    private boolean autoBid;

    public BidTransaction(User bidder, double amount, boolean autoBid) {
        this.bidder = bidder;
        this.amount = amount;
        this.autoBid = autoBid;
        this.bidTime = LocalDateTime.now();
    }
    public User getBidder() {
        return bidder;
    }
    public double getAmount() {
        return amount;
    }
    public LocalDateTime getBidTime() {
        return bidTime;
    }
    public boolean isAutoBid() {
        return autoBid;
    }
    public boolean isValid(double currentPrice, double step) {
        return amount >= currentPrice + step;
    }
    @Override
    public String toString() {
        return "BidTransaction{" +
                "bidder=" + bidder.getUsername() +
                ", amount=" + amount +
                ", time=" + bidTime +
                ", autoBid=" + autoBid +
                '}';
    }
}