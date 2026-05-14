package com.auction.common.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class BidDTO implements Serializable {
    private static final long serialVersionUID = 1L; 

    private Long id;
    private Long auctionId;
    private Long bidderId;
    private String bidderName; 
    private double amount;
    private LocalDateTime bidTime;
    private boolean autoBid;

    public BidDTO() {
    }

    public BidDTO(Long id, Long auctionId, Long bidderId, String bidderName, double amount, LocalDateTime bidTime, boolean autoBid) {
        this.id = id;
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidderName = bidderName;
        this.amount = amount;
        this.bidTime = bidTime;
        this.autoBid = autoBid;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getAuctionId() { return auctionId; }
    public void setAuctionId(Long auctionId) { this.auctionId = auctionId; }

    public Long getBidderId() { return bidderId; }
    public void setBidderId(Long bidderId) { this.bidderId = bidderId; }

    public String getBidderName() { return bidderName; }
    public void setBidderName(String bidderName) { this.bidderName = bidderName; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public LocalDateTime getBidTime() { return bidTime; }
    public void setBidTime(LocalDateTime bidTime) { this.bidTime = bidTime; }

    public boolean isAutoBid() { return autoBid; }
    public void setAutoBid(boolean autoBid) { this.autoBid = autoBid; }
}
