package server.repository;

import server.database.DBConnection;

import server.common.model.UserDTO;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserDAO {
    // Khởi tạo Logger cho class
    private static final Logger LOGGER = Logger.getLogger(UserDAO.class.getName());

    // Helper: Map data từ SQL sang Object UserDTO
    private UserDTO mapRow(ResultSet rs) throws SQLException {
        UserDTO user = new UserDTO();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setEmail(rs.getString("email"));
        user.setSystemRole(rs.getString("systemRole"));
        user.setAccountStatus(rs.getString("accountStatus"));

        Timestamp ts = rs.getTimestamp("created_at");
        if(ts != null) {
            user.setCreatedAt(ts.toLocalDateTime());
        }
        return user;
    }
    // --- CREATE ---
    public long insert(UserDTO user) {
        String sql = "INSERT INTO user (username, password, email, systemRole, accountStatus, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getSystemRole() != null ? user.getSystemRole() : "USER");
            ps.setString(5, user.getAccountStatus() != null ? user.getAccountStatus() : "ACTIVE");

            LocalDateTime createdAt = (user.getCreatedAt() != null) ? user.getCreatedAt() : LocalDateTime.now();

            if (ps.executeUpdate() > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        long id = rs.getLong(1);
                        LOGGER.info("INSERT SUCCESS: Đã tạo User mới ID=" + id + " [" + user.getUsername() + "]");
                        return id;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "INSERT ERROR: Không thể thêm User " + user.getUsername(), e);
        }
        return -1;
    }
    // --- READ (Dùng chung field name để tối ưu) ---
    public UserDTO findByField(String fieldName, Object value) {
        String sql = "SELECT * FROM user WHERE " + fieldName + " = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "READ ERROR: Lỗi truy vấn field " + fieldname + " với giá trị " + value, e);
        }
        return null;
    }
    public UserDTO findById(long id) { return findByField("id", id); }
    public UserDTO findByUsername(String username) { return findByField("username", username); }

    public List<UserDTO> findAll() {
        List<UserDTO> users = new ArrayList<>();
        String sql = "SELECT * FROM user";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) users.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "READ ALL ERROR: Lỗi lấy danh sách user", e);
        }
        return users;
    }
    // --- UPDATE ---
    public boolean update(UserDTO user) {
        String sql = "UPDATE user SET email = ?, systemRole = ?, accountStatus = ?, password = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getSystemRole());
            ps.setString(3, user.getAccountStatus());
            ps.setString(4, user.getPassword());
            ps.setLong(5, user.getId());
            boolean updated = ps.executeUpdate() > 0;
            if (updated) LOGGER.info("UPDATE SUCCESS: Đã cập nhật User ID=" + user.getId());
            return updated;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "UPDATE ERROR: Lỗi cập nhật User ID=" + user.getId(), e);
        }
        return false;
    }
    // --- DELETE ---
    public boolean delete(long id) {
        String sql = "DELETE FROM user WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            boolean deleted = ps.executeUpdate() > 0;
            if (deleted) LOGGER.info("DELETE SUCCESS: Đã xóa User ID=" + id);
            return deleted;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "DELETE ERROR: Không thể xóa User ID=" + id, e);
        }
        return false;
    }
    // --- UTILITY ---
    public boolean isExisted(String fieldName, String value) {
        return findByField(fieldName, value) != null;
    }
}