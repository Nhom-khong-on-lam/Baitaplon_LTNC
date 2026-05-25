package server.repository;

import com.auction.common.dto.Auction_watchDTO;
import server.database.DBConnection;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuctionWatchDAO {
    private static final Logger LOGGER = Logger.getLogger(AuctionWatchDAO.class.getName());

    public boolean addWatch(Auction_watchDTO watch) {
        String sql = "INSERT INTO auction_watch (user_id, auction_id, watched_at) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, watch.getUserId());
            ps.setLong(2, watch.getAuctionId());
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error in addWatch", e);
            return false;
        }
    }

    public boolean removeWatch(long userId, long auctionId) {
        String sql = "DELETE FROM auction_watch WHERE user_id = ? AND auction_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, auctionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error in removeWatch", e);
            return false;
        }
    }

    public List<Auction_watchDTO> getWatchListByUserId(long userId) {
        List<Auction_watchDTO> list = new ArrayList<>();
        // Thay SELECT * bằng liệt kê cột tường minh để an toàn tuyệt đối
        String sql = "SELECT id, user_id, auction_id, watched_at FROM auction_watch WHERE user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Auction_watchDTO dto = new Auction_watchDTO(
                            rs.getLong("id"),
                            rs.getLong("user_id"),
                            rs.getLong("auction_id"),
                            rs.getTimestamp("watched_at") != null ? rs.getTimestamp("watched_at").toLocalDateTime() : LocalDateTime.now()
                    );
                    list.add(dto);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error in getWatchListByUserId", e);
        }
        return list;
    }

    public boolean isWatching(long userId, long auctionId) {
        String sql = "SELECT 1 FROM auction_watch WHERE user_id = ? AND auction_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, auctionId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }
}