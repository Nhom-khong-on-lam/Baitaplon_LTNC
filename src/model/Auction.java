package model;

import Enum.AuctionStatus;
import observer.Observer;

import java.time.LocalDateTime;
import java.time.chrono.ChronoLocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Auction extends BaseEntity {
    private User seller;
    private User highestBidder;

    private List<BidTransaction> bidHistory = new ArrayList<>();
    private List<Observer> observers = new ArrayList<>();
    private List<AutoBidConfig> autoBidConfigs = new ArrayList<>();
    private double currentPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;

    // ================= METHODS =================

    // Thêm bid mới
    public void addBid(BidTransaction bid) {
        // TODO:
        // 1. Kiểm tra bid hợp lệ (bid.getAmount() > currentPrice)
        // 2. Cập nhật currentPrice
        // 3. Cập nhật highestBidder
        // 4. Lưu vào bidHistory
        // 5. notifyObservers()
    }

    // Đóng auction
    public void closeAuction() {
        // TODO:
        // cập nhật status = CLOSED
    }

    // Gia hạn thời gian
    public void extendEndTime(int seconds) {
        // TODO:
        // endTime = endTime + seconds
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public User getHighestBidder() {
        return highestBidder;
    }

    // ================= OBSERVER =================

    public void registerObserver(Observer observer) {
        observers.add(observer);
    }

    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    public void notifyObservers() {
        for (Observer o : observers) {
            o.update(this);
        }
    }

    public LocalDateTime getStartTime() {
        return null;
    }

    public LocalDateTime getEndTime() {
        return null;
    }

    public AuctionStatus getStatus() {
        return null;
    }

    public void setStatus(AuctionStatus status) {
    }

    public List<AutoBidConfig> getAutoBidConfigs() {
        return autoBidConfigs;
    }
}
