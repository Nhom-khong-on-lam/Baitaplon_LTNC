package server.repository;

import server.common.model.Auction_extension_logDTO;
import server.database.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuctionExtensionLogDAO {
    private static final Logger LOGGER = Logger.getLogger(AuctionExtensionLogDAO.class.getName());

    // Ghi lại một lần gia hạn đấu giá
    public boolean insertLog(Auction_extension_logDTO log) {
        String sql = "INSERT INTO auction_extension_log (auction_id, original_end_time, new_end_time) VALUES (?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, log.getAuctionId());
            ps.setTimestamp(2, Timestamp.valueOf(log.getOriginalEndTime()));
            ps.setTimestamp(3, Timestamp.valueOf(log.getNewEndTime()));

            boolean success = ps.executeUpdate() >0;
            if(success) {
                LOGGER.info("EXTENSION_LOG: Phiên đấu giá ID " + log.getAuctionId() + " được gia hạn từ " + log.getOriginalEndTime() + " đến " + log.getNewEndTime());
            }
            return success;
        } catch (SQLException e ) {
            LOGGER.log(Level.SEVERE, "EXTENSION_ERROR: Lỗi khi ghi log gia hạn cho Auction ID " + log.getAuctionId(), e);
        }
        return false;
    }
    // Lấy lịch sử gia hạn của một phiên đấu giá cụ thể
    public List<Auction_extension_logDTO> findByAuctionId(long auctionId) {
        List<Auction_extension_logDTO> logs = new ArrayList<>();
        String sql = "SELECT * FROM auction_extension_log WHERE auction_id =? ORDER BY created_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                while(rs.next()) {
                    logs.add(new Auction_extension_logDTO(
                            rs.getLong("id"),
                            rs.getLong("auction_id"),
                            rs.getInt("extended_minutes"),
                            rs.getString("reason"),
                            rs.getTimestamp("created_at").toLocalDateTime()
                    ));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "EXTENSION_READ_ERROR: Lỗi truy vấn lịch sử gia hạn cho ID " + auctionId, e);
        }
        return logs;
    }
    // Tính tổng thời gian đã bị gia hạn của một phiên(Hữu ích để phân tích)
    public int getTotalExtendedMinutes(long auctionId) {
        String sql = "SELECT SUM(extended_minutes) FROM auction_extension_log WHERE auction_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                if(rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "EXTENSION_SUM_ERROR", e);
        }
        return 0;
    }
}
