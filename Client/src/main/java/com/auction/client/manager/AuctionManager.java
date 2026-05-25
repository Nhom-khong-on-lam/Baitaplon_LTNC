package com.auction.client.manager;

import com.auction.common.model.Auction;
import com.auction.common.model.User;

import java.util.Map;

public class AuctionManager {
    private static AuctionManager instance;

    private Map<Long, Auction> activeAuction;

    private User currentUser;

    private AuctionManager() {}

    public static AuctionManager getInstance() {
        return null;
    }

    public void setCurrentUser(User user) {}

    public User getCurrentUser() {
        return null;
    }

    public Auction createAuction(Auction newAuction) {
        return newAuction;
    }

    public Auction getAuctionById(Long id) {
        return null;
    }

    public void placeBid(User user, Long auctionId, double amount) {}
}