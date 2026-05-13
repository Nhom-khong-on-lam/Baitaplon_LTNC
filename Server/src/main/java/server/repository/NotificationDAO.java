package server.repository;

import server.common.model.NotificationDTO;
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

            if (notif.getRelatedAuctionId() != null) {
                ps.setLong(6, notif.getRelatedAuctionId());
            } else {
                ps.setNull(6, Types.BIGINT);
            }

            if (notif.getRelatedBidId() != null) {
                ps.setLong(7, notif.getRelatedBidId());
            } else {
                ps.setNull(7, Types.BIGINT);
            }

            if (notif.getExpiresAt() != null) {
                ps.setTimestamp(8, Timestamp.valueOf(notif.getExpiresAt()));
            } else {
                ps.setNull(8, Types.TIMESTAMP);
            }

            LocalDateTime createdAt = (notif.getCreatedAt() != null) ? notif.getCreatedAt() : LocalDateTime.now();
            ps.setTimestamp(9, Timestamp.valueOf(createdAt));

            if (ps.executeUpdate() > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        long id = rs.getLong(1);
                        LOGGER.info("INSERT SUCCESS: Đã tạo thông báo mới ID=" + id + " cho User ID=" + notif.getUserId());
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "INSERT ERROR: Lỗi khi tạo thông báo cho User ID=" + notif.getUserId(), e);
        }
        return -1;
    }

    public List<NotificationDTO> findByUserId(long userId) {
        List<NotificationDTO> list = new ArrayList<>();
        String sql = "SELECT * FROM notification WHERE user_id = ? ORDER BY created_at DESC";
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

                    long auctionId = rs.getLong("related_auction_id");
                    if (!rs.wasNull()) notif.setRelatedAuctionId(auctionId);

                    long bidId = rs.getLong("related_bid_id");
                    if (!rs.wasNull()) notif.setRelatedBidId(bidId);

                    Timestamp expTs = rs.getTimestamp("expires_at");
                    if (expTs != null) notif.setExpiresAt(expTs.toLocalDateTime());
                    Timestamp creTs = rs.getTimestamp("created_at");
                    if (creTs != null) notif.setCreatedAt(creTs.toLocalDateTime());

                    list.add(notif);
                }
                if (list.isEmpty()) {
                    LOGGER.info("READ INFO: User ID=" + userId + " hiện không có thông báo nào.");
                } else {
                    LOGGER.info("READ SUCCESS: Đã lấy " + list.size() + " thông báo cho User ID=" + userId);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "READ ERROR: Lỗi lấy danh sách thông báo của User ID=" + userId, e);
        }
        return list;
    }

    public boolean markAsRead(long notificationId) {
        String sql = "UPDATE notification SET is_read = true WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, notificationId);
            boolean updated = ps.executeUpdate() > 0;

            if (updated) {
                LOGGER.info("UPDATE SUCCESS: Đã đánh dấu đã đọc cho thông báo ID=" + notificationId);
            } else {
                LOGGER.warning("UPDATE WARNING: Không tìm thấy thông báo ID=" + notificationId + " để cập nhật.");
            }
            return updated;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "UPDATE ERROR: Lỗi đánh dấu đã đọc thông báo ID=" + notificationId, e);
        }
        return false;
    }

    public int markAllAsRead(long userId) {
        String sql = "UPDATE notification SET is_read = true WHERE user_id = ? AND is_read = false";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, userId);
            int count = ps.executeUpdate();

            if (count > 0) {
                LOGGER.info("UPDATE SUCCESS: Đã đánh dấu " + count + " thông báo là đã đọc cho User ID=" + userId);
            } else {
                LOGGER.info("UPDATE INFO: User ID=" + userId + " không có thông báo chưa đọc nào.");
            }
            return count;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "UPDATE ERROR: Lỗi đánh dấu đọc tất cả cho User ID=" + userId, e);
        }
        return 0;
    }
}
