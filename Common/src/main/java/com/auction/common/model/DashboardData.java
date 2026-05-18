package com.auction.common.model;

import java.io.Serializable;
import java.util.List;

public class DashboardData implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<Auction> allAuctions;
    private List<Auction> myBids;
    private List<Auction> myWinning;
    private List<Auction> myProducts;
    private List<Auction> liveAuctions;

    // Constructor mặc định bắt buộc
    public DashboardData() {}

    public DashboardData(List<Auction> allAuctions, List<Auction> myBids,
                         List<Auction> myWinning, List<Auction> myProducts, List<Auction> liveAuctions) {
        this.allAuctions = allAuctions;
        this.myBids = myBids;
        this.myWinning = myWinning;
        this.myProducts = myProducts;
        this.liveAuctions = liveAuctions;
    }

    // Các hàm Getter và Setter
    public List<Auction> getAllAuctions() { return allAuctions; }
    public void setAllAuctions(List<Auction> allAuctions) { this.allAuctions = allAuctions; }

    public List<Auction> getMyBids() { return myBids; }
    public void setMyBids(List<Auction> myBids) { this.myBids = myBids; }

    public List<Auction> getMyWinning() { return myWinning; }
    public void setMyWinning(List<Auction> myWinning) { this.myWinning = myWinning; }

    public List<Auction> getMyProducts() { return myProducts; }
    public void setMyProducts(List<Auction> myProducts) { this.myProducts = myProducts; }

    public List<Auction> getLiveAuctions() { return liveAuctions; }
    public void setLiveAuctions(List<Auction> liveAuctions) { this.liveAuctions = liveAuctions; }
}