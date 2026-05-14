package com.auction.common.model;

import java.time.LocalDateTime;

public abstract class Item extends BaseEntity {
    private String name;
    private String description;
    private double startingPrice;
    private double currentPrice;
    private Long sellerId;
    private String sellerName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;

    public Item(){}
    public Item(Long id, String name, String description, double startingPrice,
                Long sellerId, String sellerName, LocalDateTime endTime) {
        super(id);
        this.id = id;
        this.name = name;
        this.description = description;
        this.startingPrice = (startingPrice < 0) ? 0 : startingPrice;
        this.currentPrice = this.startingPrice;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.startTime = LocalDateTime.now();
        this.endTime = endTime;
        this.status = "ACTIVE";
    }

    public Item(String name, String description, double startingPrice) {
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        this.startTime = LocalDateTime.now();
        this.status = "ACTIVE";
    }

    public abstract String getCategory();


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }

    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
