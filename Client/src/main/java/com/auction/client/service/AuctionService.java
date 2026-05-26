package com.auction.client.service;

import com.auction.client.controller.SessionManager;
import com.auction.common.dto.AutoBidDTO;
import com.auction.common.dto.PaymentDTO;
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
        return cachedActiveAuctions;
    }
    public DashboardData getDashboardDataCached() {
        return cachedDashboardData;
    }

    public void refreshActiveAuctionsFromServer() {
        try {
            Request req = new Request(Request.GET_AUCTIONS, null);
            Response res = send(req);

            if (res != null && res.isSuccess() && res.getData() != null) {
                synchronized (cachedActiveAuctions) {
                    cachedActiveAuctions = (List<Auction>) res.getData();
                    isFirstLoadDone = true;
                }
            }
        } catch (Exception e) {
            System.err.println("Error background loading auction list: " + e.getMessage());
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
                                   double increment,
                                  java.time.LocalDateTime startTime,
                                  java.time.LocalDateTime endTime,
                                  String imagePath) {
        try {
            // KHÔNG gửi cả cục Object User nữa để tránh lệch byte mạng Socket
            // Thay vào đó chỉ gửi ID của User (Kiểu Long cực kỳ an toàn)
            Object[] auctionData = {
                    owner.getId(), title, description, category, condition,
                    startPrice, increment,
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
                                  java.time.LocalDateTime endTime, String imagePath) {
        try {
            Object[] updateData = {auctionId, title, description, category, startPrice, endTime, imagePath};
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

    public Response setAutoBid(AutoBidDTO dto) {
        Request req = new Request(Request.SET_AUTO_BID, dto);
        return send(req);
    }

    // ── EXTEND / END EARLY ───────────────────────────────────────────────────
    public boolean extendAuction(Long auctionId, int hours) {
        Object[] data = {auctionId, hours};
        Response res  = send(new Request(Request.EXTEND_AUCTION, data));
        return res != null && res.isSuccess();
    }

    public boolean endAuctionEarly(Long auctionId) {
        Response res = send(new Request(Request.END_AUCTION_EARLY, auctionId));
        return res != null && res.isSuccess();
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
        Response res = send(req);

        if (res != null && res.isSuccess() && res.getData() != null) {
            cachedDashboardData = (com.auction.common.model.DashboardData) res.getData();
            return cachedDashboardData;
        }
        return null;
    }

    // ── 5 HÀM PHỤC VỤ CHỨC NĂNG DUYỆT ĐẤU GIÁ CỦA ADMIN ───────────────────────
    public List<Auction> getPendingAuctions() {
        Response res = send(new Request("GET_PENDING_AUCTIONS"));
        if (res != null && res.isSuccess() && res.getData() instanceof List) {
            return (List<Auction>) res.getData();
        }
        return new ArrayList<>();
    }

    public List<Auction> getApprovedAuctions() {
        Response res = send(new Request("GET_APPROVED_AUCTIONS"));
        if (res != null && res.isSuccess() && res.getData() instanceof List) {
            return (List<Auction>) res.getData();
        }
        return new ArrayList<>();
    }

    public List<Auction> getRejectedAuctions() {
        Response res = send(new Request("GET_REJECTED_AUCTIONS"));
        if (res != null && res.isSuccess() && res.getData() instanceof List) {
            return (List<Auction>) res.getData();
        }
        return new ArrayList<>();
    }

    public boolean approveAuction(Long auctionId) {
        Response res = send(new Request("ADMIN_APPROVE_AUCTION", auctionId));
        return res != null && res.isSuccess();
    }

    public boolean rejectAuction(Long auctionId, String reason) {
        Object[] data = {auctionId, reason};
        Response res = send(new Request("ADMIN_REJECT_AUCTION", data));
        return res != null && res.isSuccess();
    }

    public List<Auction> getAllApprovalAuctions() {
        Response res = send(new Request("GET_ALL_APPROVAL_AUCTIONS"));
        if (res != null && res.isSuccess() && res.getData() instanceof List) {
            return (List<Auction>) res.getData();
        }
        return new ArrayList<>();
    }

    // ── NOTIFICATIONS ────────────────────────────────────────────────────────
    public List<com.auction.common.dto.NotificationDTO> getNotifications(Long userId) {
        try {
            Response res = send(new Request(Request.GET_NOTIFICATIONS, userId));
            if (res != null && res.isSuccess() && res.getData() instanceof List) {
                return (List<com.auction.common.dto.NotificationDTO>) res.getData();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public PaymentDTO getPaymentByAuctionId(Long id) {
        try {
            Request req = new Request("GET_PAYMENT_DETAIL", id);
            Response res = send(req);
            if (res != null && res.isSuccess() && res.getData() instanceof PaymentDTO) {
                return (PaymentDTO) res.getData();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Response createPayment(PaymentDTO payment) {
        try {
            Request req = new Request("CREATE_PAYMENT", payment);
            return send(req);
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error("Connection error during payment:" + e.getMessage());
        }
    }

    public Response updateBankInfo(Long userId, String bankName, String accountNumber, String cardholderName) {
        try {
            Object[] data = {userId, bankName, accountNumber, cardholderName};
            return send(new Request("UPDATE_BANK_INFO", data));
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error("Connection error: " + e.getMessage());
        }
    }

    public Response topUpBalance(Long userId, double amount) {
        try {
            Object[] data = {userId, amount};
            return send(new Request("TOP_UP_BALANCE", data));
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error("Deposit connection error: " + e.getMessage());
        }
    }
}