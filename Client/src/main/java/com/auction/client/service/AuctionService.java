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
            System.out.println("Thất bại: Buổi đấu giá chưa đến giờ bắt đầu!");
            return false;
        }

        if (now.isAfter(auction.getEndTime())) {
            System.out.println("Thất bại: Buổi đấu giá đã kết thúc thời gian!");
            return false;
        }

        if (auction.getStatus() != AuctionStatus.RUNNING) {
            System.out.println("Thất bại: Trạng thái hiện tại (" + auction.getStatus() + ") không cho phép đặt giá!");
            return false;
        }


        if (amount <= auction.getCurrentPrice()) {
            System.out.println("Thất bại: Giá đặt phải cao hơn giá hiện tại (" + auction.getCurrentPrice() + ")!");
            return false;
        }


        BidTransaction bid = new BidTransaction(bidder, amount, false);
        auction.addBid(bid);


        handleAutoBid(auction);

        System.out.println("Thành công: Đã đặt giá " + amount + " cho người dùng " + bidder.getUsername());
        return true;
    }


    public void startAuction(Auction auction) {
        if (auction.getStatus() == AuctionStatus.OPEN) {
            auction.setStatus(AuctionStatus.RUNNING);
            System.out.println("Thông báo: Buổi đấu giá đã chính thức bắt đầu (RUNNING).");
        } else {
            System.out.println("Lỗi: Không thể bắt đầu buổi đấu giá từ trạng thái " + auction.getStatus());
        }
    }

    public void finishAuction(Auction auction) {
        auction.setStatus(AuctionStatus.FINISHED);
        System.out.println("Thông báo: Buổi đấu giá đã kết thúc thành công.");
    }


    public void processPayment(Auction auction) {
        if (auction.getStatus() == AuctionStatus.FINISHED) {
            auction.setStatus(AuctionStatus.PAID);
            System.out.println("Thông báo: Đơn hàng đã được thanh toán hoàn tất.");
        } else {
            System.out.println("Lỗi: Chỉ có thể thanh toán cho các buổi đấu giá đã FINISHED.");
        }
    }


    public void cancelAuction(Auction auction) {
        if (auction.getStatus() != AuctionStatus.PAID) {
            auction.setStatus(AuctionStatus.CANCELLED);
            System.out.println("Thông báo: Buổi đấu giá đã bị hủy.");
        } else {
            System.out.println("Lỗi: Không thể hủy buổi đấu giá đã thanh toán.");
        }
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