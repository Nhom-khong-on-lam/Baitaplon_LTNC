package server.common.model;

import server.common.enums.AuctionStatus;
import java.time.LocalDateTime;

public class AuctionDTO {
    private Long id;
    private Long itemId;
    private UserDTO seller;
    private UserDTO highestBidder;
    private double currentPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;

    // Constructor không tham số (Bắt buộc phải có để DAO khởi tạo)
    public AuctionDTO() {
    }
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public UserDTO getSeller() {
        return seller;
    }

    public void setSeller(UserDTO seller) {
        this.seller = seller;
    }

    public UserDTO getHighestBidder() {
        return highestBidder;
    }

    public void setHighestBidder(UserDTO highestBidder) {
        this.highestBidder = highestBidder;
    }
    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public AuctionStatus getStatus() {
        return status;
    }
    public void setStatus(AuctionStatus status) {
        this.status = status;
    }
}