package com.auction.common.observer;


import com.auction.common.model.Auction;

public interface Observer {
    void update(Auction auction);
}