package com.auction.client.observer;

import com.auction.client.model.Auction;

public class AuctionObserver implements Observer{
    private String name;

    public AuctionObserver(String name) {
        this.name = name;
    }

    @Override
    public void update(Auction auction) {
        System.out.println("Observer " + name + " notified: Auction updated!");
    }
}