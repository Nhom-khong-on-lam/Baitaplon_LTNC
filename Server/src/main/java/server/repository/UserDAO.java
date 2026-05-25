package server.repository;

import server.database.DBConnection;
import com.auction.common.dto.UserDTO;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserDAO {
    private static final Logger LOGGER = Logger.getLogger(UserDAO.class.getName());

    // Helper: Map data từ SQL sang Object UserDTO
    private UserDTO mapRow(ResultSet rs) throws SQLException {
        UserDTO user = new UserDTO();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setEmail(rs.getString("email"));

        // CHUẨN HÓA CHỮ HOA: Tránh lỗi nếu trong DB lỡ lưu chữ thường 'admin' hoặc 'user'
        String role = rs.getString("systemRole");
        user.setSystemRole(role != null ? role.toUpperCase().trim() : "USER");

        String status = rs.getString("accountStatus");
        user.setAccountStatus(status != null ? status.toUpperCase().trim() : "ACTIVE");

        Timestamp ts = rs.getTimestamp("created_at");
        if(ts != null) {
            user.setCreatedAt(ts.toLocalDateTime());
        }

        try {
            user.setBidCount(rs.getInt("bid_count"));
        } catch (SQLException ignore) {}

        // Banking fields (nullable — wrap in try/catch in case column absent)
        try { user.setAccountNumber(rs.getString("account_number")); } catch (SQLException ignore) {}
        try { user.setBankName(rs.getString("bank_name")); } catch (SQLException ignore) {}
        try { user.setCardholderName(rs.getString("cardholder_name")); } catch (SQLException ignore) {}

        // Đọc dữ liệu số dư từ Database lên RAM
        try { user.setBalance(rs.getDouble("balance")); } catch (SQLException ignore) {}

        return user;
    }

    // --- CREATE ---
    public long insert(UserDTO user) {
        // Mặc định nạp tiền lúc tạo tài khoản mới nếu có
        String sql = "INSERT INTO user (username, password, email, systemRole, accountStatus, created_at, balance) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getSystemRole() != null ? user.getSystemRole() : "USER");
            ps.setString(5, user.getAccountStatus() != null ? user.getAccountStatus() : "ACTIVE");

            LocalDateTime createdAt = (user.getCreatedAt() != null) ? user.getCreatedAt() : LocalDateTime.now();
            ps.setTimestamp(6, Timestamp.valueOf(createdAt));
            ps.setDouble(7, user.getBalance()); // Thêm balance cho insert công bằng

            if (ps.executeUpdate() > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        long generatedId = rs.getLong(1);
                        LOGGER.info("INSERT SUCCESS: Đã tạo User mới ID=" + generatedId);
                        return generatedId;
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "INSERT ERROR: Không thể thêm User " + user.getUsername(), e);
        }
        return -1;
    }

    public boolean deleteUserById(long userId) {
        String sql = "DELETE FROM user WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi SQL khi xóa User: " + e.getMessage());
            return false;
        }
    }

    public boolean updateStatus(long userId, String status) {
        String sql = "UPDATE user SET accountStatus = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.toUpperCase().trim());
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi SQL khi đổi trạng thái User: " + e.getMessage());
            return false;
        }
    }

    public UserDTO findByField(String fieldName, Object value) {
        String sql = "SELECT u.*, (SELECT COUNT(DISTINCT auction_id) FROM bid b WHERE b.bidder_id = u.id) AS bid_count " +
                "FROM user u WHERE u." + fieldName + " = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "READ ERROR: Lỗi tìm user theo field " + fieldName, e);
        }
        return null;
    }

    public UserDTO findById(long id) {
        return findByField("id", id);
    }

    public UserDTO findByUsername(String username) {
        return findByField("username", username);
    }

    public UserDTO findByEmail(String email) {
        return findByField("email", email);
    }

    public List<UserDTO> findAll() {
        List<UserDTO> users = new ArrayList<>();
        String sql = "SELECT u.*, (SELECT COUNT(DISTINCT auction_id) FROM bid b WHERE b.bidder_id = u.id) AS bid_count FROM user u";
        try (Connection conn = DBConnection.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) users.add(mapRow(rs));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "READ ALL ERROR: Lỗi lấy danh sách user", e);
        }
        return users;
    }

    public boolean isExisted(String fieldName, String value) {
        if ("username".equalsIgnoreCase(fieldName)) {
            return findByUsername(value) != null;
        } else if ("email".equalsIgnoreCase(fieldName)) {
            return findByEmail(value) != null;
        }
        return false;
    }

    // --- UPDATE (BỔ SUNG QUYẾT ĐỊNH CHO BIẾN BALANCE ĐỂ LƯU ĐƯỢC TIỀN) ---
    public boolean update(UserDTO user) {
        // Đã thêm "balance = ?" vào trước khối WHERE và đẩy tham số ID xuống vị trí số 10
        String sql = "UPDATE user SET username = ?, email = ?, systemRole = ?, accountStatus = ?, password = ?," +
                " account_number = ?, bank_name = ?, cardholder_name = ?, balance = ? WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getSystemRole());
            ps.setString(4, user.getAccountStatus());
            ps.setString(5, user.getPassword());
            ps.setString(6, user.getAccountNumber());
            ps.setString(7, user.getBankName());
            ps.setString(8, user.getCardholderName());
            ps.setDouble(9, user.getBalance()); // 🚀 Ghi số dư mới vào DB
            ps.setLong(10, user.getId());       // Vị trí id đổi từ số 9 sang số 10

            boolean updated = ps.executeUpdate() > 0;
            if (updated) LOGGER.info("UPDATE SUCCESS: Đã cập nhật User ID=" + user.getId() + " | Số dư mới: " + user.getBalance());
            return updated;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "UPDATE ERROR: Lỗi cập nhật User ID=" + user.getId(), e);
        }
        return false;
    }

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

    public UserDTO findByIdLight(long id) {
        // Bổ sung balance vào luôn cho hàm tìm kiếm nhanh này tránh lỗi không đồng bộ số dư
        String sql = "SELECT id, username, password, email, systemRole, accountStatus, created_at, balance FROM user WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    UserDTO user = new UserDTO();
                    user.setId(rs.getLong("id"));
                    user.setUsername(rs.getString("username"));
                    user.setPassword(rs.getString("password"));
                    user.setEmail(rs.getString("email"));
                    user.setSystemRole(rs.getString("systemRole"));
                    user.setAccountStatus(rs.getString("accountStatus"));
                    user.setBalance(rs.getDouble("balance"));

                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) {
                        user.setCreatedAt(ts.toLocalDateTime());
                    }
                    user.setBidCount(0);
                    return user;
                }
            }
        } catch (SQLException e) {
            System.err.println("LỖI trong UserDAO.findByIdLight(): " + e.getMessage());
        }
        return null;
    }
}