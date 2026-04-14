package com.auction.client.observer;

import com.auction.client.model.Auction;

public interface Observer {
    void update(Auction auction);
}