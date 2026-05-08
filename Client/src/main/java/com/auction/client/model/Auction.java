package com.auction.client.model;

import com.auction.client.Enum.AuctionStatus;
import com.auction.client.observer.Observer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Auction extends BaseEntity {
    private Item item ;
    private User seller;
    private User highestBidder;
    private List<BidTransaction> bidHistory = new ArrayList<>();
    private List<Observer> observers = new ArrayList<>();
    private List<AutoBidConfig> autoBidConfigs = new ArrayList<>();
    private double currentPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;

    // Constructor
    public Auction(Item item, User seller, LocalDateTime endTime) {
        this.item = item;
        this.seller = seller;
        this.startTime = LocalDateTime.now();
        this.endTime = endTime;
        this.currentPrice = item.getStartingPrice();
        this.status = AuctionStatus.RUNNING;
    }

    public boolean addBid(BidTransaction bid) {
        if (status != AuctionStatus.RUNNING) return false;
        if (bid.getAmount() <= currentPrice) return false;
        if (bid.getAmount() < currentPrice + getMinIncrement()) return false;

        this.currentPrice = bid.getAmount();
        this.highestBidder = bid.getBidder();
        bidHistory.add(bid);


        processAutoBids();

        notifyObservers();
        return true;
    }

    private double getMinIncrement() {
        if (currentPrice < 100) return 5;
        else if (currentPrice < 500) return 10;
        else if (currentPrice < 1000) return 20;
        else return 50;
    }

    private void processAutoBids() {
        List<AutoBidConfig> activeConfigs = autoBidConfigs.stream()
                .filter(AutoBidConfig::isActive)
                .filter(c -> !c.getBidder().equals(highestBidder))
                .collect(Collectors.toList());

        boolean changed;
        do {
            changed = false;
            for (AutoBidConfig config : activeConfigs) {
                double nextBid = config.getNextBid(currentPrice);
                if (nextBid > 0 && nextBid > currentPrice) {
                    // Tạo bid tự động
                    BidTransaction autoBid = new BidTransaction(config.getBidder(), nextBid, true);
                    if (addBid(autoBid)) {
                        changed = true;
                        break;
                    }
                }
            }
        } while (changed);
    }

    public void closeAuction() {
        this.status = AuctionStatus.FINISHED;
        notifyObservers();
    }

    public void extendEndTime(int seconds) {
        this.endTime = this.endTime.plusSeconds(seconds);
        notifyObservers();
    }

    public void registerAutoBid(AutoBidConfig config) {
        autoBidConfigs.add(config);
    }

    public double getCurrentPrice() { return currentPrice; }
    public User getHighestBidder() { return highestBidder; }
    public List<BidTransaction> getBidHistory() { return bidHistory; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public AuctionStatus getStatus() { return status; }
    public void setStatus(AuctionStatus status) { this.status = status; }
    public Item getItem() { return item; }
    public User getSeller() { return seller; }

    // Observer
    public void registerObserver(Observer observer) { observers.add(observer); }
    public void removeObserver(Observer observer) { observers.remove(observer); }
    public void notifyObservers() {
        for (Observer o : observers) o.update(this);
    }
}