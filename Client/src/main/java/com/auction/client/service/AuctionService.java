package com.auction.client.service;

import com.auction.client.controller.SessionManager;
import com.auction.client.model.Auction;
import com.auction.client.model.BidTransaction;
import com.auction.client.model.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service xử lý toàn bộ nghiệp vụ liên quan đến Auction:
 * lấy danh sách, xem chi tiết, tạo, cập nhật, xoá, đặt giá thầu.
 */
public class AuctionService {

    private final ServerConnection connection = ServerConnection.getInstance();

    // ── GET ALL AUCTIONS ─────────────────────────────────────────────────────
    public List<Auction> getAllAuctions() {
        Response res = send(new Request(Request.GET_AUCTIONS));
        if (res.isSuccess() && res.getData() instanceof List) {
            return (List<Auction>) res.getData();
        }
        return new ArrayList<>();
    }
    /**
     * Hàm lấy các phiên đang hoạt động (Active/Live)
     */
    public List<Auction> getActiveAuctions() {
        // Giả sử bạn có hằng số GET_ACTIVE_AUCTIONS trong Request
        Response res = send(new Request("GET_ACTIVE_AUCTIONS"));
        if (res.isSuccess() && res.getData() instanceof List) {
            return (List<Auction>) res.getData();
        }
        return new ArrayList<>();
    }
    // ── GET AUCTION DETAIL ───────────────────────────────────────────────────
    /**
     * Lấy chi tiết một phiên đấu giá theo ID.
     * @param auctionId ID của auction
     * @return Response chứa AuctionDTO
     */
    public Response getAuctionById(Long auctionId) {
        return send(new Request(Request.GET_AUCTION, auctionId));
    }

    // ── CREATE AUCTION ───────────────────────────────────────────────────────

    // ── UPDATE AUCTION ───────────────────────────────────────────────────────

    public Response updateAuction(Long auctionId, String title, String description,
                                  String category, double startPrice,
                                  java.time.LocalDateTime endTime) {
        try {
            // Đóng gói dữ liệu cần cập nhật
            Object[] updateData = {
                    auctionId, title, description, category, startPrice, endTime
            };

            // Tạo Request với action UPDATE_AUCTION
            Request request = new Request(Request.UPDATE_AUCTION, updateData);

            // Đính kèm token để xác thực quyền sở hữu/quyền Admin
            attachToken(request);

            // Gửi yêu cầu tới Server
            return (Response) connection.sendRequest(request);

        } catch (Exception e) {
            e.printStackTrace();
            return Response.error("Lỗi khi cập nhật phiên đấu giá: " + e.getMessage());
        }
    }
    public Response createAuction(User owner, String title, String description,
                                  String category, String condition, double startPrice,
                                  double reservePrice, double increment,
                                  java.time.LocalDateTime startTime,
                                  java.time.LocalDateTime endTime,
                                  String imagePath) {
        try {
            // Đóng gói tất cả tham số vào một mảng Object
            Object[] auctionData = {
                    owner, title, description, category, condition,
                    startPrice, reservePrice, increment,
                    startTime, endTime, imagePath
            };

            // Tạo Request với action CREATE_AUCTION (cần định nghĩa trong Request.java)
            Request request = new Request("CREATE_AUCTION", auctionData);

            // Đính kèm token xác thực của người dùng hiện tại
            attachToken(request);

            // Gửi lên server thông qua ServerConnection
            return (Response) connection.sendRequest(request);

        } catch (Exception e) {
            e.printStackTrace();
            return Response.error("Lỗi khi tạo phiên đấu giá: " + e.getMessage());
        }
    }

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
        return new java.util.ArrayList<>();
    }
    // ── DELETE AUCTION ───────────────────────────────────────────────────────
    /**
     * Xoá phiên đấu giá (Admin only).
     * @param auctionId ID của auction cần xoá
     */
    public Response deleteAuction(Long auctionId) {
        return send(new Request(Request.DELETE_AUCTION, auctionId));
    }

    // ── PLACE BID ────────────────────────────────────────────────────────────
    public Response placeBid(Long auctionId, User user, double amount) {
        try {
            // Tạo một mảng Object hoặc một DTO để đóng gói dữ liệu gửi lên server
            Object[] bidData = { auctionId, user, amount };

            // Gửi request PLACE_BID kèm dữ liệu
            Request request = new Request(Request.PLACE_BID, bidData);

            // Đính kèm token để server xác thực người dùng đang đặt giá
            String token = com.auction.client.controller.SessionManager.get().getToken();
            if (token != null) request.setToken(token);

            return (Response) ServerConnection.getInstance().sendRequest(request);
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error("Lỗi kết nối khi đặt giá: " + e.getMessage());
        }
    }

    // ── GET MY BIDS ──────────────────────────────────────────────────────────
    /**
     * Lấy lịch sử đặt giá của user hiện tại (màn MyBids).
     * @return Response chứa List<BidTransactionDTO>
     */
    public List<Auction> getMyBids(Long userId) { // Chuyển từ Long sang int
        try {
            // Gửi Request kèm userId
            Response res = send(new Request(Request.GET_MY_BIDS, userId));

            // Kiểm tra và ép kiểu sang List<Auction>
            if (res != null && res.isSuccess() && res.getData() instanceof List) {
                return (List<Auction>) res.getData();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Trả về danh sách rỗng để tránh lỗi NullPointerException
        return new ArrayList<>();
    }

    public List<BidTransaction> getBidHistory(Long auctionId) {
        try {
            Response res = send(new Request(Request.GET_BID_HISTORY, auctionId));
            if (res != null && res.isSuccess() && res.getData() instanceof List) {
                return (List<BidTransaction>) res.getData();
            }
        } catch (Exception e) { e.printStackTrace(); }
        return new java.util.ArrayList<>();
    }

    public List<Auction> getAuctionsBySeller(Long sellerId) {
        try {
            Request req = new Request("GET_AUCTIONS_BY_SELLER", sellerId);
            attachToken(req);
            Response res = send(req);
            if (res != null && res.isSuccess() && res.getData() instanceof List) {
                return (List<Auction>) res.getData();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new java.util.ArrayList<>();
    }

    // ── AUTO BID ─────────────────────────────────────────────────────────────
    /**
     * Lấy cấu hình AutoBid của user cho 1 auction.
     * @param auctionId ID auction
     */
    public Response getAutoBid(Long auctionId) {
        return send(new Request(Request.GET_AUTO_BID, auctionId));
    }

    /**
     * Thiết lập / cập nhật AutoBid.
     * @param autoBidDTO object AutoBidDTO (auctionId + maxAmount)
     */
    public Response setAutoBid(Object autoBidDTO) {
        return send(new Request(Request.SET_AUTO_BID, autoBidDTO));
    }

    // ── Helper ───────────────────────────────────────────────────────────────
    private Response send(Request request) {
        try {
            attachToken(request);
            return (Response) connection.sendRequest(request);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return Response.error("Lỗi kết nối server: " + e.getMessage());
        }
    }
    public boolean extendAuction(Long auctionId, int hours) {
        // Gom dữ liệu vào một mảng hoặc Map để gửi đi
        Object[] data = { auctionId, hours };
        Response res = send(new Request(Request.EXTEND_AUCTION, data));

        return res != null && res.isSuccess();
    }

    public void endAuctionEarly(Long auctionId) {
        send(new Request(Request.END_AUCTION_EARLY, auctionId));
    }

    private void attachToken(Request request) {
        String token = SessionManager.get().getToken();
        if (token != null) request.setToken(token);
    }
}