package server.repository;

import server.common.model.BidDTO;
import server.common.model.UserDTO;
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
            LOGGER.log(Level.SEVERE, "INSERT ERROR: Lỗi khi lưu BidDTO", e);
        }
        return -1;
    }

    public List<BidDTO> getBidsByAuctionId(long auctionId) {
        List<BidDTO> bidHistory = new ArrayList<>();
        String sql = "SELECT * FROM bid WHERE auction_id = ? ORDER BY bid_time DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {                
                UserDAO userDAO = new UserDAO();

                while (rs.next()) {
                    BidDTO bid = new BidDTO();                    
                    bid.setId(rs.getLong("id"));
                    bid.setAuctionId(rs.getLong("auction_id"));
                    long bidderId = rs.getLong("bidder_id");
                    bid.setBidderId(bidderId);
                    bid.setAmount(rs.getDouble("amount"));
                    bid.setBidTime(rs.getTimestamp("bid_time").toLocalDateTime());
                    bid.setAutoBid(rs.getBoolean("auto_bid"));

                    UserDTO user = userDAO.findById(bidderId);
                    
                    if (user != null) {
                        bid.setBidderName(user.getUsername());
                    } else {
                        bid.setBidderName("Unknown User");
                    }

                    bidHistory.add(bid);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "READ ERROR: Lỗi lấy danh sách bid của auction_id=" + auctionId, e);
        }
        return bidHistory;
    }
}
