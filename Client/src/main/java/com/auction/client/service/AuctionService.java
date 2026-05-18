package com.auction.client.service;



import com.auction.client.controller.SessionManager;
import com.auction.common.model.Auction;
import com.auction.common.model.BidTransaction;
import com.auction.common.model.DashboardData;
import com.auction.common.model.User;
import com.auction.common.network.Request;
import com.auction.common.network.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service handling all auction-related operations:
 * list, detail, create, update, delete, place bid.
 */
public class AuctionService {

    private final ServerConnection connection = ServerConnection.getInstance();
    private static List<Auction> cachedActiveAuctions = new ArrayList<>();
    private static boolean isFirstLoadDone = false;
    private static DashboardData cachedDashboardData = null;
    // ── GET ALL AUCTIONS ─────────────────────────────────────────────────────
    public List<Auction> getAllAuctions() {
        Response res = send(new Request(Request.GET_AUCTIONS));
        if (res.isSuccess() && res.getData() instanceof List) {
            return (List<Auction>) res.getData();
        }
        return new ArrayList<>();
    }
    public List<Auction> getActiveAuctionsCached() {
        return cachedActiveAuctions; // Bốc trực tiếp từ RAM trả về ngay lập tức (0ms)
    }
    public DashboardData getDashboardDataCached() {
        return cachedDashboardData;
    }


    public void refreshActiveAuctionsFromServer() {
        try {
            // 🚀 SỬA: Đổi từ "GET_ACTIVE_AUCTIONS" thành Request.GET_AUCTIONS (hoặc "GET_AUCTIONS")
            // Để luồng chạy ngầm đồng bộ lấy toàn bộ sản phẩm giống hệt như lúc bạn gõ Tìm kiếm
            Request req = new Request(Request.GET_AUCTIONS, null);
            Response res = send(req);

            if (res != null && res.isSuccess() && res.getData() != null) {
                synchronized (cachedActiveAuctions) {
                    cachedActiveAuctions = (List<Auction>) res.getData();
                    isFirstLoadDone = true;
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi tải ngầm danh sách đấu giá: " + e.getMessage());
        }
    }
    public boolean isFirstLoadDone() {
        return isFirstLoadDone;
    }

    // ── GET ACTIVE AUCTIONS ──────────────────────────────────────────────────
    public List<Auction> getActiveAuctions() {
        Response res = send(new Request("GET_ACTIVE_AUCTIONS"));
        if (res.isSuccess() && res.getData() instanceof List) {
            return (List<Auction>) res.getData();
        }
        return new ArrayList<>();
    }

    // ── GET AUCTION BY ID ────────────────────────────────────────────────────
    public Response getAuctionById(Long auctionId) {
        return send(new Request(Request.GET_AUCTION, auctionId));
    }

    // ── CREATE AUCTION ───────────────────────────────────────────────────────
    public Response createAuction(User owner, String title, String description,
                                  String category, String condition, double startPrice,
                                  double reservePrice, double increment,
                                  java.time.LocalDateTime startTime,
                                  java.time.LocalDateTime endTime,
                                  String imagePath) {
        try {
            Object[] auctionData = {
                    owner, title, description, category, condition,
                    startPrice, reservePrice, increment,
                    startTime, endTime, imagePath
            };
            Request request = new Request("CREATE_AUCTION", auctionData);
            attachToken(request);
            return (Response) connection.sendRequest(request);
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error("Failed to create auction: " + e.getMessage());
        }
    }

    // ── UPDATE AUCTION ───────────────────────────────────────────────────────
    public Response updateAuction(Long auctionId, String title, String description,
                                  String category, double startPrice,
                                  java.time.LocalDateTime endTime) {
        try {
            Object[] updateData = {auctionId, title, description, category, startPrice, endTime};
            Request  request    = new Request(Request.UPDATE_AUCTION, updateData);
            attachToken(request);
            return (Response) connection.sendRequest(request);
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error("Failed to update auction: " + e.getMessage());
        }
    }

    // ── DELETE AUCTION ───────────────────────────────────────────────────────
    public Response deleteAuction(Long auctionId) {
        return send(new Request(Request.DELETE_AUCTION, auctionId));
    }

    // ── PLACE BID ────────────────────────────────────────────────────────────
    public Response placeBid(Long auctionId, User user, double amount) {
        try {
            Object[] bidData = {auctionId, user, amount};
            Request  request = new Request(Request.PLACE_BID, bidData);
            String   token   = SessionManager.get().getToken();
            if (token != null) request.setToken(token);
            return (Response) ServerConnection.getInstance().sendRequest(request);
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error("Connection error while placing bid: " + e.getMessage());
        }
    }

    // ── GET MY BIDS ──────────────────────────────────────────────────────────
    public List<Auction> getMyBids(Long userId) {
        try {
            Response res = send(new Request(Request.GET_MY_BIDS, userId));
            if (res != null && res.isSuccess() && res.getData() instanceof List) {
                return (List<Auction>) res.getData();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    // ── GET BID HISTORY ──────────────────────────────────────────────────────
    public List<BidTransaction> getBidHistory(Long auctionId) {
        try {
            Response res = send(new Request(Request.GET_BID_HISTORY, auctionId));
            if (res != null && res.isSuccess() && res.getData() instanceof List) {
                return (List<BidTransaction>) res.getData();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    // ── GET WINNING BIDS ─────────────────────────────────────────────────────
    public List<Auction> getWinningBids(Long userId) {
        try {
            Request req = new Request("GET_WINNING_BIDS", userId);
            attachToken(req);
            Response res = send(req);
            if (res != null && res.isSuccess() && res.getData() instanceof List) {
                return (List<Auction>) res.getData();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    // ── GET AUCTIONS BY SELLER ───────────────────────────────────────────────
    public List<Auction> getAuctionsBySeller(Long sellerId) {
        try {
            Request  req = new Request("GET_AUCTIONS_BY_SELLER", sellerId);
            attachToken(req);
            Response res = send(req);
            if (res != null && res.isSuccess() && res.getData() instanceof List) {
                return (List<Auction>) res.getData();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    // ── AUTO BID ─────────────────────────────────────────────────────────────
    public Response getAutoBid(Long auctionId) {
        return send(new Request(Request.GET_AUTO_BID, auctionId));
    }

    public Response setAutoBid(Object autoBidDTO) {
        return send(new Request(Request.SET_AUTO_BID, autoBidDTO));
    }

    // ── EXTEND / END EARLY ───────────────────────────────────────────────────
    public boolean extendAuction(Long auctionId, int hours) {
        Object[] data = {auctionId, hours};
        Response res  = send(new Request(Request.EXTEND_AUCTION, data));
        return res != null && res.isSuccess();
    }

    public void endAuctionEarly(Long auctionId) {
        send(new Request(Request.END_AUCTION_EARLY, auctionId));
    }

    // ── Helper ───────────────────────────────────────────────────────────────
    private Response send(Request request) {
        try {
            attachToken(request);
            return (Response) connection.sendRequest(request);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return Response.error("Server connection error: " + e.getMessage());
        }
    }

    private void attachToken(Request request) {
        String token = SessionManager.get().getToken();
        if (token != null) request.setToken(token);
    }
    public com.auction.common.model.DashboardData getDashboardData(long userId) throws Exception {
        Request req = new Request("GET_DASHBOARD_DATA", userId);
        Response res = send(req); // Hàm gửi socket của bạn

        if (res != null && res.isSuccess() && res.getData() != null) {
            // Cất vào kho RAM trước khi trả về
            cachedDashboardData = (com.auction.common.model.DashboardData) res.getData();
            return cachedDashboardData;
        }
        return null;
    }
}
