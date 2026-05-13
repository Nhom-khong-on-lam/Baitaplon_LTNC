package server.repository;

import server.common.model.User_SessionDTO;
import server.database.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserSessionDAO {
    private static final Logger LOGGER = Logger.getLogger(UserSessionDAO.class.getName());

    public long insert(User_SessionDTO session) {
        String sql = "INSERT INTO user_session (user_id, token, expires_at, created_at) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setLong(1, session.getUserId());
            ps.setString(2, session.getToken());
            ps.setTimestamp(3, Timestamp.valueOf(session.getExpiresAt()));
            
            LocalDateTime createdAt = (session.getCreatedAt() != null) ? session.getCreatedAt() : LocalDateTime.now();
            ps.setTimestamp(4, Timestamp.valueOf(createdAt));

            if (ps.executeUpdate() > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        long id = rs.getLong(1);
                        LOGGER.info("INSERT SUCCESS: User ID=" + session.getUserId() + " đã tạo session mới ID=" + id);
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "INSERT ERROR: Lỗi khi tạo phiên làm việc cho User ID=" + session.getUserId(), e); 
        }
        return -1;
    }

    public User_SessionDTO findByToken(String token) {
        String sql = "SELECT * FROM user_session WHERE token = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User_SessionDTO session = new User_SessionDTO();
                    session.setId(rs.getLong("id"));
                    session.setUserId(rs.getLong("user_id"));
                    session.setToken(rs.getString("token"));
                    session.setExpiresAt(rs.getTimestamp("expires_at").toLocalDateTime());
                    session.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

                    LOGGER.info("READ SUCCESS: Đã tìm thấy session hợp lệ cho token.");
                    return session;
                } else {
                    LOGGER.warning("READ WARNING: Không tìm thấy session nào với token này.");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "SESSION ERROR: Lỗi truy vấn token", e);
        }
        return null;
    }

    public boolean deleteByToken(String token) {
        String sql = "DELETE FROM user_session WHERE token = ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setString(1, token);
            boolean deleted = ps.executeUpdate() > 0;
            if (deleted) {
                LOGGER.info("DELETE SUCCESS: Phiên làm việc (token) đã được thu hồi thành công."); [cite: 286]
            } else {
                LOGGER.warning("DELETE WARNING: Không có session nào để xóa (Token không tồn tại).");
            }
            return deleted;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "DELETE ERROR: Lỗi khi thu hồi session", e); [cite: 287]
        }
        return false;
    }

    public int deleteExpiredSessions() {
        String sql = "DELETE FROM user_session WHERE expires_at < ?";
        
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            int deletedCount = ps.executeUpdate();
            if (deletedCount > 0) {
                LOGGER.info("CLEANUP SUCCESS: Đã xóa " + deletedCount + " phiên làm việc hết hạn."); [cite: 291]
            } else {
                LOGGER.info("CLEANUP INFO: Không có phiên làm việc nào hết hạn để dọn dẹp.");
            }
            return deletedCount;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "CLEANUP ERROR: Lỗi trong quá trình dọn dẹp session tự động", e); [cite: 293]
        }
        return 0;
    }
}
