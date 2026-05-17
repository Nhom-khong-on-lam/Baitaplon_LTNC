package server.repository;

import com.auction.common.dto.NotificationDTO;
import server.database.DBConnection;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotificationDAO {
    private static final Logger LOGGER = Logger.getLogger(NotificationDAO.class.getName());

    public long insert(NotificationDTO notif) {
        String sql = "INSERT INTO notification (user_id, title, message, type, is_read, related_auction_id, related_bid_id, expires_at, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setLong(1, notif.getUserId());
            ps.setString(2, notif.getTitle());
            ps.setString(3, notif.getMessage());
            ps.setString(4, notif.getType());
            ps.setBoolean(5, notif.isRead());

            if (notif.getRelatedAuctionId() != null) ps.setLong(6, notif.getRelatedAuctionId());
            else ps.setNull(6, Types.BIGINT);

            if (notif.getRelatedBidId() != null) ps.setLong(7, notif.getRelatedBidId());
            else ps.setNull(7, Types.BIGINT);

            if (notif.getExpiresAt() != null) ps.setTimestamp(8, Timestamp.valueOf(notif.getExpiresAt()));
            else ps.setNull(8, Types.TIMESTAMP);

            ps.setTimestamp(9, Timestamp.valueOf(notif.getCreatedAt() != null ? notif.getCreatedAt() : LocalDateTime.now()));

            if (ps.executeUpdate() > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi tạo thông báo", e);
        }
        return -1;
    }

    public List<NotificationDTO> getUnreadNotifications(long userId) {
        List<NotificationDTO> list = new ArrayList<>();
        String sql = "SELECT id, user_id, title, message, type, is_read, related_auction_id, related_bid_id, expires_at, created_at FROM notification WHERE user_id = ? AND is_read = false ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    NotificationDTO notif = new NotificationDTO();
                    notif.setId(rs.getLong("id"));
                    notif.setUserId(rs.getLong("user_id"));
                    notif.setTitle(rs.getString("title"));
                    notif.setMessage(rs.getString("message"));
                    notif.setType(rs.getString("type"));
                    notif.setRead(rs.getBoolean("is_read"));

                    long rAuctionId = rs.getLong("related_auction_id");
                    if (!rs.wasNull()) notif.setRelatedAuctionId(rAuctionId);

                    long rBidId = rs.getLong("related_bid_id");
                    if (!rs.wasNull()) notif.setRelatedBidId(rBidId);

                    Timestamp expTs = rs.getTimestamp("expires_at");
                    if (expTs != null) notif.setExpiresAt(expTs.toLocalDateTime());

                    Timestamp creTs = rs.getTimestamp("created_at");
                    if (creTs != null) notif.setCreatedAt(creTs.toLocalDateTime());

                    list.add(notif);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi đọc danh sách thông báo chưa đọc", e);
        }
        return list;
    }

    public boolean markAsRead(long notificationId) {
        String sql = "UPDATE notification SET is_read = true WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, notificationId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi cập nhật trạng thái đã đọc thông báo", e);
            return false;
        }
    }

    public int markAllAsRead(long userId) {
        String sql = "UPDATE notification SET is_read = true WHERE user_id = ? AND is_read = false";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            return ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi cập nhật đọc tất cả thông báo", e);
            return 0;
        }
    }
}