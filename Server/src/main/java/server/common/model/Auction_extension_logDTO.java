package server.common.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Auction_extension_logDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private long id;
    private long auctionId;
    private int extendedMinutes; //Số phút được cộng thêm
    private String reason; //Lý do (ví dụ: "Last minute bid")
    private LocalDateTime createdAt;

    public Auction_extension_logDTO() {}
    // Constructor dùng khi thực hiện gia hạn
    public Auction_extension_logDTO(long auctionId, int extendedMinutes, String reason) {
        this.auctionId = auctionId;
        this.extendedMinutes = extendedMinutes;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
    }
    //Constructor đầy đủ cho DAO
    public Auction_extension_logDTO(long id, long auctionId, int extendedMinutes, String reason, LocalDateTime createdAt) {
        this.id= id;
        this.auctionId = auctionId;
        this.extendedMinutes = extendedMinutes;
        this.reason = reason;
        this.createdAt = createdAt;
    }
    // --- Getters and Setters
    public long getId() {return id;}
    public void setId(long id) {this.id=id;}

    public long getAuctionId() { return auctionId; }
    public void setAuctionId(long auctionId) { this.auctionId = auctionId; }

    public int getExtendedMinutes() { return extendedMinutes; }
    public void setExtendedMinutes(int extendedMinutes) { this.extendedMinutes = extendedMinutes; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "AuctionExtensionLogDTO{" + "auctionId=" + auctionId + ", added=" + extendedMinutes + "m}";
    }
}
