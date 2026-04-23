package com.auction.client.service;

import com.auction.client.Enum.AuctionStatus;
import com.auction.client.model.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class AuctionService {
    private static final Map<Long, Auction> auctions = new HashMap<>();
    private static final AtomicLong auctionIdGen = new AtomicLong(1);
    private static final AtomicLong itemIdGen = new AtomicLong(1);
    private final AuthService authService = new AuthService();


    public AuctionService() {
        initSampleData();
    }

    private void initSampleData() {
        if (!auctions.isEmpty()) return;
        User user1 = authService.getUserByUsername("user1");
        User user2 = authService.getUserByUsername("user2");

        if (user1 == null || user2 == null) {
            System.err.println("Không tìm thấy user1/user2 trong AuthService. Đảm bảo AuthService có dữ liệu.");
            return;
        }

        Art art = new Art("Starry Night", "Van Gogh painting", 1000.0, "Van Gogh", 1889);
        art.setId(itemIdGen.getAndIncrement());
        art.setSellerId(user1.getId());
        art.setSellerName(user1.getUsername());

        Electronics electronics = new Electronics("iPhone 15", "Latest model", 800.0, "Apple", "15 Pro");
        electronics.setId(itemIdGen.getAndIncrement());
        electronics.setSellerId(user2.getId());
        electronics.setSellerName(user2.getUsername());

        Vehicle vehicle = new Vehicle("Tesla Model S", "Electric car", 50000.0, "Tesla", "Model S", 2023);
        vehicle.setId(itemIdGen.getAndIncrement());
        vehicle.setSellerId(user1.getId());
        vehicle.setSellerName(user1.getUsername());

        // Tạo phiên đấu giá (endTime là tương lai)
        Auction a1 = new Auction(art, user1, LocalDateTime.now().plusDays(3));
        a1.setId(auctionIdGen.getAndIncrement());
        Auction a2 = new Auction(electronics, user2, LocalDateTime.now().plusDays(5));
        a2.setId(auctionIdGen.getAndIncrement());
        Auction a3 = new Auction(vehicle, user1, LocalDateTime.now().plusDays(7));
        a3.setId(auctionIdGen.getAndIncrement());

        auctions.put(a1.getId(), a1);
        auctions.put(a2.getId(), a2);
        auctions.put(a3.getId(), a3);

        System.out.println("Đã khởi tạo " + auctions.size() + " phiên đấu giá mẫu.");
    }

    public ObservableList<Auction> getActiveAuctions() {
        List<Auction> active = new ArrayList<>();
        for (Auction a : auctions.values()) {
            if (a.getStatus() == AuctionStatus.RUNNING && a.getEndTime().isAfter(LocalDateTime.now())) {
                active.add(a);
            }
        }
        return FXCollections.observableArrayList(active);
    }

    public ObservableList<Auction> getAllAuctions() {
        return FXCollections.observableArrayList(auctions.values());
    }

    public boolean placeBid(Long auctionId, User bidder, double amount) {
        if (auctionId == null || bidder == null) return false;
        Auction auction = auctions.get(auctionId);
        if (auction == null) return false;
        try {
            if (auction.getStatus() != AuctionStatus.RUNNING || auction.getEndTime().isBefore(LocalDateTime.now())) {
                return false;
            }
            if (amount <= auction.getCurrentPrice()) return false;
            BidTransaction bid = new BidTransaction(bidder, amount, false);
            return auction.addBid(bid);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public void registerAutoBid(Long auctionId, User bidder, double maxPrice, double step) {
        if (auctionId == null || bidder == null) return;
        Auction auction = auctions.get(auctionId);
        if (auction != null) {
            auction.registerAutoBid(new AutoBidConfig(bidder, maxPrice, step));
        }
    }


    public Auction createAuction(Item item, User seller, LocalDateTime endTime) {
        if (item == null || seller == null || endTime == null) return null;
        Auction auction = new Auction(item, seller, endTime);
        auction.setId(auctionIdGen.getAndIncrement());
        auctions.put(auction.getId(), auction);
        return auction;
    }

    public void endAuctionEarly(Long auctionId) {
        if (auctionId == null) return;
        Auction auction = auctions.get(auctionId);
        if (auction != null) auction.closeAuction();
    }


    public List<BidTransaction> getBidsByUser(Long userId) {
        if (userId == null) return Collections.emptyList();
        List<BidTransaction> result = new ArrayList<>();
        for (Auction a : auctions.values()) {
            for (BidTransaction bid : a.getBidHistory()) {
                if (bid.getBidder().getId().equals(userId)) {
                    result.add(bid);
                }
            }
        }
        return result;
    }

    public List<Auction> getAuctionsBySeller(Long sellerId) {
        if (sellerId == null) return Collections.emptyList();
        List<Auction> result = new ArrayList<>();
        for (Auction a : auctions.values()) {
            if (a.getSeller().getId().equals(sellerId)) {
                result.add(a);
            }
        }
        return result;
    }

    public List<User> getAllUsers() {
        return authService.getAllUsers();
    }
}