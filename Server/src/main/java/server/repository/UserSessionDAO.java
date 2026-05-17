package server.repository;

import com.auction.common.dto.User_SessionDTO;
import server.database.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserSessionDAO {
    private static final Logger LOGGER = Logger.getLogger(UserSessionDAO.class.getName());

    // ── MAP ROW: Khớp chính xác 100% với các thuộc tính trong User_SessionDTO ──
    private User_SessionDTO mapRow(ResultSet rs) throws SQLException {
        User_SessionDTO session = new User_SessionDTO();
        session.setId(rs.getLong("id"));
        session.setUserId(rs.getLong("user_id"));

        // Khớp với private String token;
        session.setToken(rs.getString("token"));

        // Khớp với private LocalDateTime expiresAt;
        Timestamp expTs = rs.getTimestamp("expires_at");
        if (expTs != null) {
            session.setExpiresAt(expTs.toLocalDateTime());
        }

        // Khớp với private LocalDateTime createdAt;
        Timestamp creTs = rs.getTimestamp("created_at");
        if (creTs != null) {
            session.setCreatedAt(creTs.toLocalDateTime());
        }

        return session;
    }

    // ── INSERT: Khớp hàm get và bọc try-with-resources xử lý SQLException ──
    public boolean insert(User_SessionDTO session) {
        String sql = "INSERT INTO user_session (user_id, token, expires_at, created_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, session.getUserId());
            ps.setString(2, session.getToken()); // .getToken() chuẩn DTO

            if (session.getExpiresAt() != null) {
                ps.setTimestamp(3, Timestamp.valueOf(session.getExpiresAt()));
            } else {
                ps.setNull(3, Types.TIMESTAMP);
            }

            // Sử dụng thuộc tính .getCreatedAt() từ DTO của bạn
            LocalDateTime createdTime = (session.getCreatedAt() != null) ? session.getCreatedAt() : LocalDateTime.now();
            ps.setTimestamp(4, Timestamp.valueOf(createdTime));

            int rows = ps.executeUpdate();
            if (rows > 0) {
                LOGGER.info("INSERT SESSION SUCCESS: Token = " + session.getToken());
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "INSERT SESSION ERROR: Không thể thêm session mới", e);
        }
        return false;
    }

    // ── FIND BY TOKEN ──
    public User_SessionDTO findByToken(String token) {
        String sql = "SELECT id, user_id, token, expires_at, created_at FROM user_session WHERE token = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "FIND SESSION ERROR: Lỗi tìm kiếm session với token=" + token, e);
        }
        return null;
    }

    // ── DELETE BY TOKEN (Log out) ──
    public boolean deleteByToken(String token) {
        String sql = "DELETE FROM user_session WHERE token = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, token);
            boolean deleted = ps.executeUpdate() > 0;
            if (deleted) {
                LOGGER.info("DELETE SESSION SUCCESS: Đã xóa session của token = " + token);
            }
            return deleted;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "DELETE SESSION ERROR: Không thể xóa session", e);
        }
        return false;
    }
}