package com.auction.common.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BidTransaction implements Serializable {
    private static final long serialVersionUID = 1L;
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
    public String getFormattedTime() {
        // Định dạng theo kiểu Giờ:Phút:Giây (Ví dụ: 21:45:10)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return bidTime.format(formatter);
    }
}