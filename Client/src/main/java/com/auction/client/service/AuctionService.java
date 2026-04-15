package com.auction.client.service;

import com.auction.client.model.Auction;
import com.auction.client.model.AutoBidConfig;
import com.auction.client.model.BidTransaction;
import com.auction.client.model.User;
import com.auction.client.Enum.AuctionStatus;

import java.time.LocalDateTime;

public class AuctionService {


    public boolean placeBid(Auction auction, User bidder, double amount) {

        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(auction.getStartTime())) {
            System.out.println(" Buổi đấu giá chưa bắt đầu!");
            return false;
        }


        if (now.isAfter(auction.getEndTime())) {
            System.out.println(" Buổi đấu giá đã kết thúc!");
            return false;
        }

        if (auction.getStatus() != AuctionStatus.OPEN) {

            System.out.println("Buổi đấu giá không cho phép bid!");
            return false;
        }


        if (amount <= auction.getCurrentPrice()) {
            System.out.println(" Giá phải cao hơn giá hiện tại!");
            return false;
        }

        BidTransaction bid = new BidTransaction(bidder, amount, false);

        auction.addBid(bid);
        handleAutoBid(auction);
        System.out.println(" Đặt giá thành công!");
        return true;
    }


    public void startAuction(Auction auction) {
        auction.setStatus(AuctionStatus.OPEN);
    }


    public void finishAuction(Auction auction) {
        auction.setStatus(AuctionStatus.CLOSED);
    }


    public void cancelAuction(Auction auction) {
        auction.setStatus(AuctionStatus.CANCELLED);
    }
    private void handleAutoBid(Auction auction) {

        boolean updated;

        do {
            updated = false;

            for (AutoBidConfig config : auction.getAutoBidConfigs()) {

                if (!config.isActive()) continue;

                if (config.getBidder().equals(auction.getHighestBidder())) continue;

                double nextPrice = auction.getCurrentPrice() + config.getIncrement();

                if (nextPrice <= config.getMaxPrice()) {

                    BidTransaction bid = new BidTransaction(config.getBidder(), nextPrice, true);

                    auction.addBid(bid);
                    updated = true;
                }
            }

        } while (updated);
    }

}