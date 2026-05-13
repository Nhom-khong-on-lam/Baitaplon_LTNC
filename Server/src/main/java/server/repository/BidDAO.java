package server.repository;

import server.common.model.BidDTO;
import server.database.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BidDAO {
    private static final Logger LOGGER = Logger.getLogger(BidDAO.class.getName());

    public long insert(BidDTO bid) {
        String sql = "INSERT INTO bid (auction_id, bidder_id, amount, bid_time, auto_bid) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, bid.getAuctionId());
            ps.setLong(2, bid.getBidderId());
            ps.setDouble(3, bid.getAmount());

            LocalDateTime timeToSave = (bid.getBidTime() != null) ? bid.getBidTime() : LocalDateTime.now();
            ps.setTimestamp(4, Timestamp.valueOf(timeToSave));
            ps.setBoolean(5, bid.isAutoBid());

            if (ps.executeUpdate() > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        long id = rs.getLong(1);
                        LOGGER.info("INSERT SUCCESS: Đã lưu Bid mới ID=" + id + " cho Auction=" + bid.getAuctionId());
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "INSERT ERROR: Lỗi khi lưu BidDTO của Auction=" + bid.getAuctionId(), e);
        }
        return -1;
    }

    public List<BidDTO> getBidsByAuctionId(long auctionId) {
        List<BidDTO> bidHistory = new ArrayList<>();
        String sql = "SELECT b.*, u.username FROM bid b LEFT JOIN user u ON b.bidder_id = u.id WHERE b.auction_id = ? ORDER BY b.bid_time DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BidDTO bid = new BidDTO();
                    bid.setId(rs.getLong("id"));
                    bid.setAuctionId(rs.getLong("auction_id"));
                    bid.setBidderId(rs.getLong("bidder_id"));
                    bid.setAmount(rs.getDouble("amount"));
                    bid.setBidTime(rs.getTimestamp("bid_time").toLocalDateTime());
                    bid.setAutoBid(rs.getBoolean("auto_bid"));

                    String username = rs.getString("username");
                    bid.setBidderName(username != null ? username : "Unknown User");

                    bidHistory.add(bid);
                }

                if (bidHistory.isEmpty()) {
                    LOGGER.warning("READ WARNING: Không tìm thấy lượt đấu giá nào cho Auction ID=" + auctionId);
                } else {
                    LOGGER.info("READ SUCCESS: Đã lấy " + bidHistory.size() + " lượt đấu giá cho Auction ID=" + auctionId);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "READ ERROR: Lỗi lấy danh sách bid của auction_id=" + auctionId, e);
        }
        return bidHistory;
    }
}
