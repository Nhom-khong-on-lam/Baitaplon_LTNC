package server.common.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Auction_extension_logDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private long id;
    private long auctionId;
    private LocalDateTime originalEndTime; // Khớp với ERD: original_end_time
    private LocalDateTime newEndTime; // Khớp với ERD: new_end_time

    public Auction_extension_logDTO() {}
    // Constructor dùng khi thực hiện gia hạn
    public Auction_extension_logDTO(long auctionId, LocalDateTime originalEndTime, LocalDateTime newEndTime) {
        this.auctionId = auctionId;
        this.originalEndTime = originalEndTime;
        this.newEndTime = newEndTime;
    }
    //Constructor đầy đủ cho DAO
    public Auction_extension_logDTO(long id, long auctionId, LocalDateTime originalEndTime, LocalDateTime newEndTime) {
        this.id= id;
        this.auctionId = auctionId;
        this.originalEndTime = originalEndTime;
        this.newEndTime = newEndTime;
    }
    // --- Getters and Setters
    public long getId() {return id;}
    public void setId(long id) {this.id=id;}

    public long getAuctionId() { return auctionId; }
    public void setAuctionId(long auctionId) { this.auctionId = auctionId; }

    public LocalDateTime getOriginalEndTime() { return originalEndTime; }
    public void setOriginalEndTime(LocalDateTime originalEndTime) { this.originalEndTime = originalEndTime; }

    public LocalDateTime getNewEndTime() { return newEndTime; }
    public void setNewEndTime(LocalDateTime newEndTime) { this.newEndTime = newEndTime; }

    @Override
    public String toString() {
        return "AuctionExtensionLogDTO{" +
                "auctionId=" + auctionId +
                ", originalEndTime=" + originalEndTime +
                ", newEndTime=" + newEndTime + '}';
    }
}
