package server.repository;

import server.common.model.Auction_watchDTO;
import server.database.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuctionWatchDAO {
    private static final Logger LOGGER = Logger.getLogger(AuctionWatchDAO.class.getName());

    // Thêm một phiên đấu giá vào danh sách theo dõi
    public boolean addWatch(AuctionWatchDAO watch) {
        String sql = "INSERT INTO auction_watch (user_id, auction_id, created_at) VALUES (?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, watch.getUserId());
            ps.setLong(2, watch.getAuctionId());
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));

            boolean success = ps.executeUpdate() > 0;
            if (success) {
                LOGGER.info("WATCH_ADD: User ID " + watch.getUserId() + " đang theo dõi Auction ID " + watch.getAuctionId());
            }
            return success;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "WATCH_ERROR: Lỗi khi thêm theo dõi cho User " + watch.getUserId(), e);
        }
        return false;
    }
    // Hủy theo dõi (Xóa khỏi watchlist)
    public boolean removeWatch(long userId, long auctionId) {
        String sql = "DELETE FROM auction_watch WHERE user_id = ? AND auction_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setLong(2, auctionId);

            boolean success = ps.executeUpdate() > 0;
            if (success) {
                LOGGER.info("WATCH_REMOVE: User ID " + userId + " đã hủy theo dõi Auction ID " + auctionId);
            }
            return success;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "WATCH_ERROR: Lỗi khi xóa theo dõi", e);
        }
        return false;
    }
    // Lấy danh sách tất cả các phiên đấu giá mà một User đang theo dõi
    public List<AuctionWatchDTO> findByUserId(long userId) {
        List<AuctionWatchDTO> list = new ArrayList<>();
        // Query có Join với bảng auction để lấy thêm tiêu đề hiển thị
        String sql = "SELECT aw.*, a.title FROM auction_watch aw " +
                "JOIN auction a ON aw.auction_id = a.id " +
                "WHERE aw.user_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AuctionWatchDTO dto = new AuctionWatchDTO(
                            rs.getLong("id"),
                            rs.getLong("user_id"),
                            rs.getLong("auction_id"),
                            rs.getTimestamp("created_at").toLocalDateTime()
                    );
                    dto.setAuctionTitle(rs.getString("title"));
                    list.add(dto);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "WATCH_READ_ERROR: Lỗi lấy watchlist của User " + userId, e);
        }
        return list;
    }
    // Kiểm tra xem người dùng đã theo dõi phiên này chưa (để hiện thị nút Follow/Unfollow)
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
