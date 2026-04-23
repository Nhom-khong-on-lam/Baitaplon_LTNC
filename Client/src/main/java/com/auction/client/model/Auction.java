package com.auction.client.model;


import com.auction.client.Enum.AuctionStatus;
import com.auction.client.observer.Observer;

import java.time.LocalDateTime;
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

    public void addBid(BidTransaction bid) {

    }


    public void closeAuction() {

    }


    public void extendEndTime(int seconds) {

    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public User getHighestBidder() {
        return highestBidder;
    }



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
