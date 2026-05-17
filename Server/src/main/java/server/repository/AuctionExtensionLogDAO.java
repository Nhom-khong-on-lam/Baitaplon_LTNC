package server.repository;

import com.auction.common.dto.Auction_extension_logDTO;
import server.database.DBConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuctionExtensionLogDAO {
    private static final Logger LOGGER = Logger.getLogger(AuctionExtensionLogDAO.class.getName());

    public boolean insertLog(Auction_extension_logDTO log) {
        String sql = "INSERT INTO auction_extension_log (auction_id, original_end_time, new_end_time) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, log.getAuctionId());
            ps.setTimestamp(2, Timestamp.valueOf(log.getOriginalEndTime()));
            ps.setTimestamp(3, Timestamp.valueOf(log.getNewEndTime()));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi ghi log gia hạn", e);
            return false;
        }
    }

    public List<Auction_extension_logDTO> getLogsByAuctionId(long auctionId) {
        List<Auction_extension_logDTO> logs = new ArrayList<>();
        String sql = "SELECT id, auction_id, original_end_time, new_end_time FROM auction_extension_log WHERE auction_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    logs.add(new Auction_extension_logDTO(
                            rs.getLong("id"),
                            rs.getLong("auction_id"),
                            rs.getTimestamp("original_end_time") != null ? rs.getTimestamp("original_end_time").toLocalDateTime() : null,
                            rs.getTimestamp("new_end_time") != null ? rs.getTimestamp("new_end_time").toLocalDateTime() : null
                    ));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi lấy log lịch sử gia hạn", e);
        }
        return logs;
    }

    public long getTotalExtendedMinutes(long auctionId) {
        String sql = "SELECT SUM(TIMESTAMPDIFF(MINUTE, original_end_time, new_end_time)) FROM auction_extension_log WHERE auction_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi tính tổng phút gia hạn", e);
        }
        return 0;
    }
}